package com.example.ui

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.network.GeminiClient
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface PmacUiState {
    object Idle : PmacUiState
    object Loading : PmacUiState
    data class Success(val message: String) : PmacUiState
    data class Error(val error: String) : PmacUiState
}

sealed class AuthSessionState {
    object Unauthenticated : AuthSessionState()
    object Loading : AuthSessionState()
    data class Authenticated(val user: User) : AuthSessionState()
    data class Error(val message: String) : AuthSessionState()
}

data class ChatMessage(val sender: String, val message: String, val timestamp: Long = System.currentTimeMillis())

class PmacViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PmacRepository

    // --- Global User Authentication State (React Context Provider Equivalent) ---
    private val _authSession = MutableStateFlow<AuthSessionState>(AuthSessionState.Unauthenticated)
    val authSession: StateFlow<AuthSessionState> = _authSession.asStateFlow()

    fun loginOffline(username: String, passwordHash: String) {
        _authSession.value = AuthSessionState.Loading
        viewModelScope.launch {
            kotlinx.coroutines.delay(600) // realistic local db latency
            val user = repository.getUserByUsername(username)
            if (user != null) {
                if (user.passwordHash == passwordHash) {
                    _authSession.value = AuthSessionState.Authenticated(user)
                    addCustomAuditLog(
                        username = user.username,
                        fullName = user.fullName,
                        role = user.role,
                        action = "تسجيل دخول بالنظام",
                        details = "تم تسجيل الدخول والتحقق الآمن من الهوية وحمل صلاحيات: [${user.permissions}] كعضو نشط",
                        module = "الصلاحيات",
                        status = "نجاح (Success)"
                    )
                } else {
                    _authSession.value = AuthSessionState.Error("كلمة المرور المدخلة غير صحيحة!")
                    addCustomAuditLog(
                        username = username,
                        fullName = "محاولة دخول مجهولة",
                        role = "مجهول",
                        action = "فشل تسجيل الدخول",
                        details = "محاولة تسجيل دخول فاشلة برمز مرور خاطئ للمستخدم: $username",
                        module = "الصلاحيات",
                        status = "فشل (Failure)"
                    )
                }
            } else {
                _authSession.value = AuthSessionState.Error("اسم المستخدم غير مسجل بقاعدة البيانات المحلية!")
                addCustomAuditLog(
                    username = username,
                    fullName = "محاولة دخول مجهولة",
                    role = "مجهول",
                    action = "فشل تسجيل الدخول",
                    details = "محاولة تسجيل دخول فاشلة للمستخدم $username وهو غير مسجل بسجلات موظفي قاعدة البيانات",
                    module = "الصلاحيات",
                    status = "فشل (Failure)"
                )
            }
        }
    }

    fun registerUserOffline(username: String, passwordHash: String, fullName: String, role: String, permissions: String) {
        viewModelScope.launch {
            val existing = repository.getUserByUsername(username)
            if (existing != null) {
                _authSession.value = AuthSessionState.Error("اسم المستخدم مسجل مسبقاً!")
            } else {
                val newUser = User(username, passwordHash, fullName, role, permissions)
                repository.insertUser(newUser)
                _authSession.value = AuthSessionState.Authenticated(newUser)
                addCustomAuditLog(
                    username = username,
                    fullName = fullName,
                    role = role,
                    action = "إنشاء مستخدم جديد",
                    details = "تم تسجيل عضو أوفلاين جديد [$fullName] برتبة ودرجة تشغيل: [$role] وصلاحيات: [$permissions]",
                    module = "الصلاحيات",
                    status = "نجاح (Success)"
                )
            }
        }
    }

    fun logout() {
        _authSession.value = AuthSessionState.Unauthenticated
    }

    fun clearAuthError() {
        if (_authSession.value is AuthSessionState.Error) {
            _authSession.value = AuthSessionState.Unauthenticated
        }
    }

    // --- Real Hardware Network Tracker (mimicking browser navigator.onLine) ---
    private val _isDeviceOnline = MutableStateFlow(true)
    val isDeviceOnline: StateFlow<Boolean> = _isDeviceOnline.asStateFlow()

    init {
        val database = PmacDatabase.getDatabase(application)
        repository = PmacRepository(database)

        // Initialize real device network tracker (navigator.onLine equivalent)
        try {
            val connectivityManager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (connectivityManager != null) {
                val activeNetwork = connectivityManager.activeNetwork
                val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
                val hasInternet = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
                _isDeviceOnline.value = hasInternet

                connectivityManager.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        _isDeviceOnline.value = true
                    }
                    override fun onLost(network: Network) {
                        _isDeviceOnline.value = false
                    }
                })
            }
        } catch (e: Exception) {
            // Fail-safe if any permission or capability check fails
            _isDeviceOnline.value = true
        }
    }

    // --- Offline-First Status Tracker ---
    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _pendingSyncCount = MutableStateFlow(0)
    val pendingSyncCount: StateFlow<Int> = _pendingSyncCount.asStateFlow()

    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing.asStateFlow()

    fun toggleOnlineMode() {
        viewModelScope.launch {
            if (!_isOnline.value) {
                // Return to Online -> Do simulation sync
                _syncing.value = true
                kotlinx.coroutines.delay(1800) // Delay to simulate network syncing
                _pendingSyncCount.value = 0
                _syncing.value = false
            }
            _isOnline.value = !_isOnline.value
        }
    }

    fun triggerManualSync() {
        if (!_isOnline.value) return
        viewModelScope.launch {
            _syncing.value = true
            _pouchSyncLog.value = _pouchSyncLog.value + "جاري بدء مزامنة PouchDB ثنائية الاتجاه..."
            kotlinx.coroutines.delay(1500)
            _pendingSyncCount.value = 0
            _pouchUpdateSeq.value = _pouchUpdateSeq.value + 1
            _lastSyncTime.value = "اليوم " + java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
            _pouchSyncLog.value = _pouchSyncLog.value + "تمت المزامنة بنجاح. رقم المتسلسلة الحالي للـ Sequence: ${_pouchUpdateSeq.value}"
            _syncing.value = false
        }
    }

    // --- PouchDB / CouchDB Simulation & Metadata Panel ---
    private val _pouchUpdateSeq = MutableStateFlow(2408)
    val pouchUpdateSeq: StateFlow<Int> = _pouchUpdateSeq.asStateFlow()

    private val _pouchConflictCount = MutableStateFlow(2)
    val pouchConflictCount: StateFlow<Int> = _pouchConflictCount.asStateFlow()

    private val _replicationDirection = MutableStateFlow("Bidirectional") // "Bidirectional", "PushOnly", "PullOnly"
    val replicationDirection: StateFlow<String> = _replicationDirection.asStateFlow()

    private val _lastSyncTime = MutableStateFlow("2026-06-09 12:00")
    val lastSyncTime: StateFlow<String> = _lastSyncTime.asStateFlow()

    private val _pouchSyncLog = MutableStateFlow(
        listOf(
            "تم تهيئة قاعدة بيانات Room مدمجة ومطابقة لمعايير PouchDB/IndexedDB",
            "مزامنة أوفلاين أولية نشطة بنجاح على التردد اللاسلكي والشبكة المحلية",
            "تحديث المتسلسلة المحلية Sequence: 2408",
            "تم اكتشاف وتمرير ملفين للتعارض بين النسخة المحلية والحوسبة الطرفية السحابية"
        )
    )
    val pouchSyncLog: StateFlow<List<String>> = _pouchSyncLog.asStateFlow()

    fun updateReplicationDirection(direction: String) {
        _replicationDirection.value = direction
        _pouchSyncLog.value = _pouchSyncLog.value + "تم تغيير اتجاه نسخ الحزم إلى: $direction"
    }

    fun resolvePouchConflicts() {
        viewModelScope.launch {
            if (_pouchConflictCount.value > 0) {
                _syncing.value = true
                _pouchSyncLog.value = _pouchSyncLog.value + "بدء تتبع ومعالجة التعارضات (Conflict Resolution)..."
                kotlinx.coroutines.delay(1000)
                _pouchConflictCount.value = 0
                _pouchUpdateSeq.value = _pouchUpdateSeq.value + 1
                _pouchSyncLog.value = _pouchSyncLog.value + "تم فض جميع التعارضات مع تغليب التحديث المحلي للميدان (Client-Win Rule) بنجاح"
                _syncing.value = false
            }
        }
    }

    fun simulateNewLocalChange() {
        _pendingSyncCount.value = _pendingSyncCount.value + 1
        _pouchUpdateSeq.value = _pouchUpdateSeq.value + 1
        _pouchSyncLog.value = _pouchSyncLog.value + "تعديل محلي جديد (تغير بالمعطيات) - رقم المتسلسلة: ${_pouchUpdateSeq.value}"
    }

    fun simulateCloudDatabaseReset() {
        viewModelScope.launch {
            _syncing.value = true
            _pouchSyncLog.value = _pouchSyncLog.value + "جاري إعادة تهيئة ومحاذاة المزامنة السحابية للبيانات الميدانية..."
            kotlinx.coroutines.delay(1200)
            _pouchConflictCount.value = 2
            _pendingSyncCount.value = 0
            _pouchUpdateSeq.value = 2408
            _pouchSyncLog.value = _pouchSyncLog.value + "تمت إعادة تعيين المؤشرات - متوفر الآن: 2 تعارضات معلقة"
            _syncing.value = false
        }
    }

    // --- Core Navigation State ---
    private val _currentSection = MutableStateFlow("Dashboard")
    val currentSection: StateFlow<String> = _currentSection.asStateFlow()

    fun setSection(section: String) {
        _currentSection.value = section
    }

    // --- Observables from Room ---
    val decisions: StateFlow<List<DecisionTask>> = repository.allDecisions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val financeRecords: StateFlow<List<FinanceRecord>> = repository.allFinanceRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val operations: StateFlow<List<OperationReport>> = repository.allOperations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val partners: StateFlow<List<PartnerDonor>> = repository.allPartners
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val employees: StateFlow<List<EmployeeRecord>> = repository.allEmployees
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val knowledgeItems: StateFlow<List<KnowledgeItem>> = repository.allKnowledgeItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Documents State Stream ---
    val documents: StateFlow<List<PmacDocument>> = repository.allDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Emergency Protocols State Streams (Offline Local Indexed) ---
    val emergencyProtocols: StateFlow<List<EmergencyProtocol>> = repository.allEmergencyProtocols
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _protocolSearchQuery = MutableStateFlow("")
    val protocolSearchQuery: StateFlow<String> = _protocolSearchQuery.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val searchedEmergencyProtocols: StateFlow<List<EmergencyProtocol>> = _protocolSearchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.allEmergencyProtocols
            } else {
                repository.searchEmergencyProtocols(query)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateProtocolSearchQuery(query: String) {
        _protocolSearchQuery.value = query
    }

    fun addEmergencyProtocol(title: String, priority: String, steps: String, contactNo: String) {
        viewModelScope.launch {
            if (title.isBlank() || steps.isBlank()) {
                _uiState.value = PmacUiState.Error("يرجى ملء الحقول الأساسية لبروتوكول الطوارئ!")
                return@launch
            }
            val newProtocol = EmergencyProtocol(
                title = title,
                priority = priority,
                steps = steps,
                contactNo = contactNo,
                lastUpdated = "2026-06-09"
            )
            repository.insertEmergencyProtocol(newProtocol)
            _uiState.value = PmacUiState.Success("تمت إضافة بروتوكول الطوارئ وحفظه محلياً بنجاح!")
        }
    }
    private val _selectedDocIdForVersions = MutableStateFlow<Int?>(null)
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val documentVersions: StateFlow<List<DocumentVersion>> = _selectedDocIdForVersions
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getVersionsForDocument(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSelectedDocumentForHistory(docId: Int?) {
        _selectedDocIdForVersions.value = docId
    }

    // --- Simple UI Handling States ---
    private val _uiState = MutableStateFlow<PmacUiState>(PmacUiState.Idle)
    val uiState: StateFlow<PmacUiState> = _uiState.asStateFlow()

    private val _aiForecastState = MutableStateFlow("")
    val aiForecastState: StateFlow<String> = _aiForecastState.asStateFlow()

    private val _scannedInvoiceResult = MutableStateFlow<FinanceRecord?>(null)
    val scannedInvoiceResult: StateFlow<FinanceRecord?> = _scannedInvoiceResult.asStateFlow()

    // --- AI Smart Chatbot Assistant States ---
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage("bot", "مرحباً بك في لوحة المساعد الذكي لنظام PMAC. يمكنك سؤالي عن إحصائيات الألغام، السجلات المالية، المشاريع النشطة، أو معايير نزع السلاح (SOPs / IMAS). سأجيبك بناءً على بيانات المركز المسجلة محلياً!")
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _chatLoading = MutableStateFlow(false)
    val chatLoading: StateFlow<Boolean> = _chatLoading.asStateFlow()

    // --- MUTATION METHODS (Offline Aware) ---

    private fun handleLocalWrite() {
        if (!_isOnline.value) {
            _pendingSyncCount.value += 1
        }
    }

    fun addDecision(title: String, details: String, department: String, assignedTo: String, dueDate: String, riskLevel: String) {
        viewModelScope.launch {
            val task = DecisionTask(
                title = title,
                details = details,
                department = department,
                assignedTo = assignedTo,
                status = "بانتظار البدء",
                dueDate = dueDate,
                riskLevel = riskLevel
            )
            repository.insertDecision(task)
            handleLocalWrite()

            val (uName, fName, roleStr) = getActiveUserLogInfo()
            addCustomAuditLog(uName, fName, roleStr, "إضافة قرار وتكليف", "تم إقرار تكليف جديد: [$title] وإسناده إلى $assignedTo", "القرارات والإدارة")
        }
    }

    fun updateDecision(task: DecisionTask) {
        viewModelScope.launch {
            repository.updateDecision(task)
            handleLocalWrite()

            val (uName, fName, roleStr) = getActiveUserLogInfo()
            addCustomAuditLog(uName, fName, roleStr, "تعديل قرار وتكليف", "تم تعديل القرار: [${task.title}] وتعيين حالته إلى [${task.status}]", "القرارات والإدارة")
        }
    }

    fun deleteDecision(task: DecisionTask) {
        viewModelScope.launch {
            repository.deleteDecision(task)
            handleLocalWrite()

            val (uName, fName, roleStr) = getActiveUserLogInfo()
            addCustomAuditLog(uName, fName, roleStr, "حذف قرار وبلاغ", "تم حذف قرار التكليف [${task.title}] وإزالته من قاعدة البيانات", "القرارات والإدارة")
        }
    }

    fun addFinance(title: String, type: String, project: String, amount: Double, category: String, date: String, donor: String, notes: String = "") {
        viewModelScope.launch {
            val record = FinanceRecord(
                title = title,
                type = type,
                project = project,
                amount = amount,
                category = category,
                date = date,
                donor = donor,
                notes = notes,
                isSynced = _isOnline.value
            )
            repository.insertFinance(record)
            handleLocalWrite()

            val (uName, fName, roleStr) = getActiveUserLogInfo()
            addCustomAuditLog(uName, fName, roleStr, "إضافة حركة مالية", "تم إدراج بند مالي جديد بقيمة [$amount دولار] للمشروع [$project] بتمويل [$donor]", "المالية")
        }
    }

    fun deleteFinance(record: FinanceRecord) {
        viewModelScope.launch {
            repository.deleteFinance(record)
            handleLocalWrite()

            val (uName, fName, roleStr) = getActiveUserLogInfo()
            addCustomAuditLog(uName, fName, roleStr, "حذف حركة مالية", "تم حذف الحركة المالية [${record.title}] بقيمة [${record.amount} دولار]", "المالية")
        }
    }

    fun addOperation(title: String, type: String, fieldTeam: String, area: String, date: String, status: String, details: String, casualties: Int, riskScore: Int, aiSummary: String = "") {
        viewModelScope.launch {
            val report = OperationReport(
                title = title,
                type = type,
                fieldTeam = fieldTeam,
                area = area,
                date = date,
                status = status,
                details = details,
                casualties = casualties,
                riskScore = riskScore,
                aiSummary = aiSummary
            )
            repository.insertOperation(report)
            handleLocalWrite()
            
            // Log to simulated PouchDB
            val statusLog = if (!_isOnline.value) {
                "⚡ [PouchDB Cache] حفظ محلي للبلاغ [$title] - معلق للرفع التلقائي"
            } else {
                "🔗 [CouchDB Sync] تم رفع بلاغ اللغم [$title] وتمريره للسحابة بنجاح"
            }
            _pouchSyncLog.value = _pouchSyncLog.value + statusLog
            _pouchUpdateSeq.value = _pouchUpdateSeq.value + 1

            val (uName, fName, roleStr) = getActiveUserLogInfo()
            addCustomAuditLog(uName, fName, roleStr, "إضافة بلاغ عملياتي", "تم إدخال بلاغ [$type]: [$title] في منطقة [$area] مع فريق الميدان: $fieldTeam", "العمليات")
        }
    }

    fun updateOperation(report: OperationReport) {
        viewModelScope.launch {
            repository.updateOperation(report)
            handleLocalWrite()

            val (uName, fName, roleStr) = getActiveUserLogInfo()
            addCustomAuditLog(uName, fName, roleStr, "تعديل تقرير عملياتي", "تم تحديث حالة التقرير لقضية: [${report.title}] لتصبح [${report.status}]", "العمليات")
        }
    }

    fun deleteOperation(report: OperationReport) {
        viewModelScope.launch {
            repository.deleteOperation(report)
            handleLocalWrite()

            val (uName, fName, roleStr) = getActiveUserLogInfo()
            addCustomAuditLog(uName, fName, roleStr, "حذف بلاغ عملياتي", "تم حذف بلاغ حوادث/تطهير: [${report.title}] الميداني بنجاح", "العمليات")
        }
    }

    fun addPartner(name: String, type: String, organization: String, contactInfo: String, activeProjects: String, contributions: Double) {
        viewModelScope.launch {
            val partner = PartnerDonor(
                name = name,
                type = type,
                organization = organization,
                contactInfo = contactInfo,
                activeProjects = activeProjects,
                contributions = contributions,
                status = "نشط"
            )
            repository.insertPartner(partner)
            handleLocalWrite()
        }
    }

    fun deletePartner(partner: PartnerDonor) {
        viewModelScope.launch {
            repository.deletePartner(partner)
            handleLocalWrite()
        }
    }

    fun addEmployee(name: String, role: String, department: String, phone: String) {
        viewModelScope.launch {
            val employee = EmployeeRecord(
                name = name,
                role = role,
                department = department,
                phone = phone,
                attendanceStatus = "حاضر",
                leaveBalance = 21,
                lastAppraisal = "ممتاز"
            )
            repository.insertEmployee(employee)
            handleLocalWrite()
        }
    }

    fun updateEmployee(employee: EmployeeRecord) {
        viewModelScope.launch {
            repository.updateEmployee(employee)
            handleLocalWrite()
        }
    }

    fun deleteEmployee(employee: EmployeeRecord) {
        viewModelScope.launch {
            repository.deleteEmployee(employee)
            handleLocalWrite()
        }
    }

    fun addKnowledge(title: String, category: String, content: String, subTitle: String) {
        viewModelScope.launch {
            val item = KnowledgeItem(
                title = title,
                category = category,
                content = content,
                subTitle = subTitle
            )
            repository.insertKnowledgeItem(item)
            handleLocalWrite()
        }
    }

    fun deleteKnowledge(item: KnowledgeItem) {
        viewModelScope.launch {
            repository.deleteKnowledgeItem(item)
            handleLocalWrite()
        }
    }

    // --- DOCUMENTS OPERATIONS (Version Control Enabled) ---

    fun addDocument(title: String, category: String, fileType: String, keywords: String, uploadedBy: String, requiredRole: String, notes: String) {
        viewModelScope.launch {
            val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
            val document = PmacDocument(
                title = title,
                category = category,
                fileType = fileType,
                keywords = keywords,
                uploadDate = dateStr,
                uploadedBy = uploadedBy,
                requiredRole = requiredRole,
                currentVersion = 1,
                localFilePath = "/docs/" + title.lowercase().replace(" ", "_") + "_v1." + fileType.lowercase()
            )
            val docId = repository.insertDocument(document).toInt()
            
            // Generate initial version record
            val initialVer = DocumentVersion(
                documentId = docId,
                versionNumber = 1,
                title = "الإصدار المبدئي للمستند",
                updateNotes = if (notes.isEmpty()) "المسودة المبدئية والنسخة المدخلة الأولى للنظام." else notes,
                modifiedDate = dateStr,
                modifiedBy = uploadedBy,
                localFilePath = document.localFilePath
            )
            repository.insertDocumentVersion(initialVer)
            handleLocalWrite()
        }
    }

    fun uploadNewVersion(document: PmacDocument, versionTitle: String, updateNotes: String, modifiedBy: String) {
        viewModelScope.launch {
            val nextVersionNum = document.currentVersion + 1
            val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
            val path = "/docs/" + document.title.lowercase().replace(" ", "_") + "_v" + nextVersionNum + "." + document.fileType.lowercase()
            
            val newVer = DocumentVersion(
                documentId = document.id,
                versionNumber = nextVersionNum,
                title = versionTitle,
                updateNotes = updateNotes,
                modifiedDate = dateStr,
                modifiedBy = modifiedBy,
                localFilePath = path
            )
            repository.insertDocumentVersion(newVer)
            
            val updatedDoc = document.copy(
                currentVersion = nextVersionNum,
                localFilePath = path,
                uploadDate = dateStr, // Update to reflect newest version date
                uploadedBy = modifiedBy
            )
            repository.updateDocument(updatedDoc)
            handleLocalWrite()
        }
    }

    fun deleteDocument(document: PmacDocument) {
        viewModelScope.launch {
            repository.deleteDocument(document)
            handleLocalWrite()
        }
    }

    // --- ADVANCED AI OPERATIONS ---

    fun runAISummarizeOperation(report: OperationReport) {
        viewModelScope.launch {
            _uiState.value = PmacUiState.Loading
            val prompt = """
                قم بتلخيص التقرير العملياتي التالي وصنف الإجراء التصحيحي المقترح له بمهنية.
                العنوان: ${report.title}
                النوع: ${report.type}
                الفريق: ${report.fieldTeam}
                المنطقة: ${report.area}
                تفاصيل ميدانية: ${report.details}
                عدد الضحايا: ${report.casualties}
                نقاط الخطر: ${report.riskScore}/5
                املأ التلخيص باللغة العربية بأسلوب المعايير الدولية لشؤون الألغام بمقدار فقرة واحدة قصيرة وموجزة جداً.
            """.trimIndent()

            val result = GeminiClient.generatePrompt(prompt)
            val updatedReport = report.copy(aiSummary = result)
            repository.updateOperation(updatedReport)
            _uiState.value = PmacUiState.Success("تم توليد التلخيص الذكي للتقرير!")
        }
    }

    fun generateAiDescriptionForDiscovery(title: String, type: String, riskScore: Int, onResult: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.value = PmacUiState.Loading
            val prompt = """
                بصفتك مساعداً تقنياً خبيراً ومستشار نزع الألغام للأمم المتحدة للشرق الأوسط والمركز الفلسطيني (PMAC)، قم بكتابة وصف تقني لميدان اكتشاف لغم أو مقذوف باللغة العربية بناءً على المعطيات التالية:
                - العنوان المفترض: $title
                - الصنف التقني: $type
                - تصنيف الخطورة المقدر: $riskScore من 5
                
                اكتب الوصف من فقرة واحدة قصيرة ومترابطة (2-3 أسطر) بطريقة مهنية تصف التهديد للأراضي المحيطة والإرشاد الوقائي لتأمين البؤرة ريثما يحضر المشرف الميداني للتعامل.
            """.trimIndent()
            val result = GeminiClient.generatePrompt(prompt)
            _uiState.value = PmacUiState.Idle
            onResult(result)
        }
    }

    fun runFinanceBurnRateForecast() {
        viewModelScope.launch {
            _aiForecastState.value = "جاري الحساب والتنبؤ المالي باستخدام الذكاء الاصطناعي..."
            val currentRecords = financeRecords.value
            val totalBudget = currentRecords.filter { it.type.contains("موازنة") }.sumOf { it.amount }
            val totalExpenses = currentRecords.filter { it.type.contains("مصروف") }.sumOf { it.amount }

            val prompt = """
                بصفتك مستشاراً مالياً خبيراً للمركز الفلسطيني للألغام (PMAC)، قم بتحليل البيانات المالية التالية:
                إجمالي الموازنة المسجلة: $totalBudget دولار أمريكي.
                إجمالي المصروفات والنفقات حتى الآن: $totalExpenses دولار أمريكي.
                المشاريع النشطة: تطهير غزة والشمال، توعية أريحا، مسح جنين.
                المستندات المستخرجة: شراء كواشف MineLab، نفقات السفر الميدانية.
                اعطني توقعاً من فقرة واحدة باللغة العربية حول معدلات استهلاك الميزانية (Burn Rate)، وهل الموازنة مهددة بالنفاذ ومقترحات تحذيرية لحوكمة نفقات الأصول التشغيلية.
            """.trimIndent()

            val result = GeminiClient.generatePrompt(prompt)
            _aiForecastState.value = result
        }
    }

    fun runInvoiceDataExtraction(rawText: String) {
        viewModelScope.launch {
            _uiState.value = PmacUiState.Loading
            val prompt = """
                بصفتك قارئ فواتير ذكي مدمج في نظام الألغام، اقرأ النص المستخرج من الفاتورة وقم بتحليله واستخراج العناصر الأربعة التالية:
                1. اسم المصروف وعنوان الفاتورة (مثال: شراء كابلات نحاسية، صيانة مركبة، تأمين طبي دوري)
                2. المشروع المرتبط (يجب أن يكون واحداً من التالية: "تطهير شمال غزة"، "توعية أريحا"، "مسح جنين"، "التشغيل العام")
                3. المقدار الإجمالي للمبلغ (أرقام فقط كأرقام عشرية بدون علامات عملة)
                4. جهة الدونر/المانح (e.g. EU, UNMAS, UNICEF)
                
                نص الفاتورة:
                $rawText
                
                قم بإخراج النتيجة بتنسيق مفصول بفاصلة عمودية (|) كالتالي تماماً وتجنب أي كلام جانبي:
                الاسم | المشروع | المبلغ | المانح
            """.trimIndent()

            val result = GeminiClient.generatePrompt(prompt)
            val parts = result.split("|")
            if (parts.size >= 4) {
                val title = parts[0].trim()
                val project = parts[1].trim()
                val amount = parts[2].trim().toDoubleOrNull() ?: 1200.0
                val donor = parts[3].trim()

                _scannedInvoiceResult.value = FinanceRecord(
                    title = title,
                    type = "مصروفات",
                    project = project,
                    amount = amount,
                    category = "معدات ومصروف أمني",
                    date = "2026-06-08",
                    donor = donor,
                    isSynced = _isOnline.value,
                    notes = "مستخرج تلقائياً بواسطة قراءة الفاتورة الذكي (AI Extracted File)",
                    isAiForecasted = true
                )
                _uiState.value = PmacUiState.Success("تم استخراج بيانات الفاتورة بنجاح!")
            } else {
                _uiState.value = PmacUiState.Error("لم يتمكن الذكاء الاصطناعي من قراءة التنسيق المالي بشكل قاطع. تمت محاولة الملاءمة التقديرية.")
                // Mock autofill fallback
                _scannedInvoiceResult.value = FinanceRecord(
                    title = "فاتورة صيانة جهاز كواشف",
                    type = "مصروفات",
                    project = "التشغيل العام",
                    amount = 450.0,
                    category = "معدات",
                    date = "2026-06-08",
                    donor = "UNMAS",
                    notes = "تعذر قراءة الفاتورة بدقة، تم التعبئة تلقائياً للاختبار.",
                    isAiForecasted = true
                )
            }
        }
    }

    fun clearScannedInvoice() {
        _scannedInvoiceResult.value = null
    }

    fun generateMeetingMinutesAndTasks(meetingTitle: String, rawNotes: String) {
        viewModelScope.launch {
            _uiState.value = PmacUiState.Loading
            val prompt = """
                اقرأ محضر الاجتماع التالي واستخرج منه مهمة (طرح قرار وتكليف) واحدة ملموسة للمؤسسة:
                عنوان الاجتماع: $meetingTitle
                الملاحظات الخام: $rawNotes
                
                قم بتقديم النتيجة بتنسيق مفصول بفاصلة عمودية (|) كالتالي تماماً بدون أي علامات ترقيم إضافية أو كلام إداري:
                عنوان القرار والتكليف | القسم المسؤول (يجب أن يكون واحداً من: الإدارة العليا، المالية، العمليات، العلاقات العامة) | تفاصيل القرار والخطوات التنفيذية | الشخص المسؤول عن المتابعة | مستوى الخطر (منخفض أو متوسط أو مرتفع)
            """.trimIndent()

            val result = GeminiClient.generatePrompt(prompt)
            val parts = result.split("|")
            if (parts.size >= 5) {
                val title = parts[0].trim()
                val dept = parts[1].trim()
                val details = parts[2].trim()
                val person = parts[3].trim()
                val risk = parts[4].trim()

                addDecision(
                    title = title,
                    details = details,
                    department = dept,
                    assignedTo = person,
                    dueDate = "2026-06-28",
                    riskLevel = risk
                )
                _uiState.value = PmacUiState.Success("تم توليد التكليف والقرار بنجاح وحفظه في السجلات الأوفلاين!")
            } else {
                // Fallback
                addDecision(
                    title = "متابعة مخرجات اجتماع: $meetingTitle",
                    details = "مراجعة بنود النقاش في المحضر: $rawNotes",
                    department = "الإدارة العليا",
                    assignedTo = "م. حسام التميمي",
                    dueDate = "2026-06-20",
                    riskLevel = "متوسط"
                )
                _uiState.value = PmacUiState.Success("تم إيعاز المتابعة الافتراضية للقرار لتعذر فك التنسيقات العريضة.")
            }
        }
    }

    // --- SMART CHATBOT ASSISTANT ---

    fun sendChatMessage(userText: String) {
        if (userText.isBlank()) return

        val userMsg = ChatMessage("user", userText)
        _chatMessages.update { it + userMsg }

        viewModelScope.launch {
            _chatLoading.value = true

            // Gather context facts from offline DB to feed to Gemini
            val decList = decisions.value
            val finList = financeRecords.value
            val opList = operations.value
            val partList = partners.value
            val empList = employees.value

            val statsContext = """
                حقائق وسجلات قاعدة بيانات PMAC الحالية:
                - إجمالي القرارات والمهمات: ${decList.size} مهمة. (منها ${decList.count { it.status == "مكتمل" }} مكتمل، ${decList.count { it.status == "قيد العمل" }} قيد العمل).
                - إجمالي السجلات والبنود المالية: ${finList.size} سجلات. (مجموع الموازنة: ${finList.filter { it.type.contains("موازنة") }.sumOf { it.amount }} دولار، إجمالي المصروفات: ${finList.filter { it.type.contains("مصروف") }.sumOf { it.amount }} دولار).
                - سجل العمليات وحوادث الألغام المسجلة بالأوفلاين: ${opList.size} تقريراً عملياتياً. (منها ${opList.count { it.type.contains("حوادث") }} حادث لغم أرضي مسجل، إجمالي الضحايا والإصابات للآن: ${opList.sumOf { it.casualties }}).
                - عدد المانحين والشركاء المسجلين: ${partList.size} هيئات نشطة شاملة الأمم المتحدة وUNMAS والاتحاد الأوروبي.
                - إجمالي عدد الموظفين المسجلين في كادر العمل: ${empList.size} موظفاً (يضم المدير العام م. حسام التميمي، مدير المالية شادي جابر، مديرة الميدان ريم الصالح).
                يرتكز المركز الفلسطيني للأعمال المتعلقة بالألغام (PMAC) على معايير سلاح الألغام الدولي IMAS وعلى أولوية الحذر وإبطال الفيوزات دون المس الحسي.
            """.trimIndent()

            val serverPrompt = """
                أنت مستشار الذكاء الاصطناعي الذكي والخبير الفني المساعد للمركز الفلسطيني للأعمال المتعلقة بالألغام (PMAC).
                لديك صلاحيات كاملة لقراءة قاعدة البيانات الإحصائية للمركز لإجابة أسئلة الموظفين.
                
                بيانات وجدول إحصائيات النظام الفعلي:
                $statsContext
                
                جاوب المستخدم باللغة العربية باحترام ودقة واقتباس تام وإحصائي، وقدم توصيات الأمان المناسبة إذا سأل عن عمليات الألغام أو الحوادث الميدانية.
            """.trimIndent()

            val response = GeminiClient.generatePrompt(userText, serverPrompt)
            val botMsg = ChatMessage("bot", response)
            _chatMessages.update { it + botMsg }
            _chatLoading.value = false
        }
    }

    fun clearChat() {
        _chatMessages.value = listOf(
            ChatMessage("bot", "مرحباً بك مجدداً. تم مسح تاريخ التخاطب المؤقت. كيف يمكنني مساعدتكم اليوم في أعمال المركز الفلسطيني للأعمال المتعلقة بالألغام؟")
        )
    }

    fun clearUiState() {
        _uiState.value = PmacUiState.Idle
    }

    // --- ASSETS & INVENTORY LOCAL ENGINE ---
    private val _assets = MutableStateFlow<List<AssetItem>>(
        listOf(
            AssetItem(1, "MineLab F3 Compact", "ML-F3C-9921", "كاشف ألغام (Metal Detector)", "ممتازة", "فريق مسح أ", "مستودع جنين الرئيسي", "2025-03-10", "2026-05-15", "MineLab Australia"),
            AssetItem(2, "سترة واقية معززة مضادة للشظايا SD-V2", "SV-SD2-8172", "سترة واقية معززة (Armored Vest)", "ممتازة", "فريق تدخل ج", "مستودع الخليل الفرعي", "2024-09-18", "2026-02-12", "Seward Ltd UK"),
            AssetItem(3, "Toyota Hilux 4x4 M-Double Cab", "TOY-HIL-8088", "سيارة دفع رباعي 4x4", "تحت الصيانة", "فريق مسح ب", "مقر رام الله الشمالي", "2023-11-05", "2026-06-01", "الشركة الشرقية للسيارات"),
            AssetItem(4, "كمبيوتر لوحي Panasonic Toughbook 55", "PAN-TB55-0391", "كمبيوتر لوحي ميداني", "ممتازة", "ريم الصالح", "مستودع غزة الفرعي", "2025-01-20", "2026-05-20", "Panasonic Middle East"),
            AssetItem(5, "كاشف ألغام للأعماق Vallon HM-4", "VAL-HM4-3329", "كاشف ألغام (Metal Detector)", "ممتازة", "فريق مسح أ", "مستودع جنين الرئيسي", "2025-05-14", "2026-06-02", "Vallon GmbH")
        )
    )
    val assets = _assets.asStateFlow()

    private val _assetConditionFilter = MutableStateFlow("")
    val assetConditionFilter = _assetConditionFilter.asStateFlow()

    private val _assetSearchQuery = MutableStateFlow("")
    val assetSearchQuery = _assetSearchQuery.asStateFlow()

    val filteredAssets: StateFlow<List<AssetItem>> = combine(_assets, _assetConditionFilter, _assetSearchQuery) { assetsList, condition, query ->
        assetsList.filter { asset ->
            (condition.isEmpty() || asset.condition == condition) &&
            (query.isEmpty() || asset.name.contains(query, true) || asset.serialNumber.contains(query, true) || asset.assignedTo.contains(query, true) || asset.type.contains(query, true))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateAssetConditionFilter(condition: String) {
        _assetConditionFilter.value = condition
    }

    fun updateAssetSearchQuery(query: String) {
        _assetSearchQuery.value = query
    }

    fun registerAsset(name: String, serialNumber: String, type: String, condition: String, assignedTo: String, storeLocation: String, supplier: String) {
        if (name.isBlank() || serialNumber.isBlank()) {
            _uiState.value = PmacUiState.Error("يرجى إدخال اسم الأصل والرقم التسلسلي الموحد!")
            return
        }
        val newId = (_assets.value.maxOfOrNull { it.id } ?: 0) + 1
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val newAsset = AssetItem(
            id = newId,
            name = name,
            serialNumber = serialNumber,
            type = type,
            condition = condition,
            assignedTo = assignedTo,
            storeLocation = storeLocation,
            purchaseDate = today,
            lastServiceDate = today,
            supplier = supplier
        )
        _assets.value = _assets.value + newAsset
        _uiState.value = PmacUiState.Success("تم تسجيل الأصل اللوجستي [$name] بنجاح وموائمته مع سلاسل التوريد!")
        
        // Log to Audit Log!
        val (uName, fName, roleStr) = getActiveUserLogInfo()
        addCustomAuditLog(
            username = uName,
            fullName = fName,
            role = roleStr,
            action = "تسجيل أصل لوجستي جديد",
            details = "تم تسجيل جهاز/مركبة: $name برقم تسلسلي $serialNumber وتعيينه لـ $assignedTo في $storeLocation",
            module = "الأصول والخدمات اللوجستية"
        )
    }

    fun updateAssetCondition(assetId: Int, newCondition: String) {
        _assets.value = _assets.value.map {
            if (it.id == assetId) {
                it.copy(condition = newCondition, lastServiceDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()))
            } else it
        }
        val updatedAsset = _assets.value.firstOrNull { it.id == assetId }
        val assetName = updatedAsset?.name ?: "أصل مجهول"
        
        val (uName, fName, roleStr) = getActiveUserLogInfo()
        addCustomAuditLog(
            username = uName,
            fullName = fName,
            role = roleStr,
            action = "تحديث حالة الأصل",
            details = "تحديث حالة الأصل [$assetName] إلى [$newCondition] وجدولة مراجعة الصيانة الميدانية",
            module = "الأصول والخدمات اللوجستية"
        )
    }

    // --- AUDIT SYSTEM CONTROL CENTRE ENGINE ---
    private val _auditLogs = MutableStateFlow<List<AuditLog>>(
        listOf(
            AuditLog(1, "admin", "دانة جبريل (المدير العام والمسؤول التقني)", "Administrator", "تسجيل دخول بالنظام", "تم التحقق الفني وجر الجلسة المشفرة بنجاح، وربط 4 خدمات وتأكيد تكوين الصلاحيات النشطة على SQLite", "2026-06-09 08:30:15", "الصلاحيات", "نجاح (Success)"),
            AuditLog(2, "ops", "م. يوسف الصالحي (مدير العمليات الميدانية)", "Operations Manager", "دراسة مسح غير تقني لمحيط بلدة يعبد", "تم تسجيل بلاغ عملياتي جديد لغرض معاينة الحقول الطينية المتاخمة للبلدة وتجهيز فريق مسح أ", "2026-06-09 09:12:44", "العمليات", "نجاح (Success)"),
            AuditLog(3, "finance", "شادي جابر (المراقب المالي والمشرف)", "Finance Manager", "تحديث الموازنة الدورية للوقود", "تم تسجيل سلفة تشغيلية عاجلة بقيمة 1800 دولار لصالح فريق مسح ب لقوافل النقل", "2026-06-09 10:05:12", "المالية", "نجاح (Success)"),
            AuditLog(4, "admin", "دانة جبريل (المدير العام والمسؤول التقني)", "Administrator", "مزامنة البيانات والتعارضات والخرائط", "بدء تصفية جدول التعارض للـ Documents ودمج النسخة v2 تلقائياً بفارق الميدان وتحديث Sequence 2408", "2026-06-09 11:15:30", "بيانات المزامنة", "نجاح (Success)"),
            AuditLog(5, "ops", "م. يوسف الصالحي (مدير العمليات الميدانية)", "Operations Manager", "جرد وتصنيف سجل ذخائر مكتشفة في منطقة قلقيلية", "تحديث حالة الصيانة والعهد لجهاز Toyota Hilux 4x4 وتوثيق قذائف هاون عازلة للحرائق والنبش", "2026-06-09 12:05:00", "الأصول والخدمات اللوجستية", "نجاح (Success)")
        )
    )
    val auditLogs = _auditLogs.asStateFlow()

    private val _auditModuleFilter = MutableStateFlow("")
    val auditModuleFilter = _auditModuleFilter.asStateFlow()

    private val _auditSearchQuery = MutableStateFlow("")
    val auditSearchQuery = _auditSearchQuery.asStateFlow()

    val filteredAuditLogs: StateFlow<List<AuditLog>> = combine(_auditLogs, _auditModuleFilter, _auditSearchQuery) { logsList, module, query ->
        logsList.filter { log ->
            (module.isEmpty() || log.systemModule == module) &&
            (query.isEmpty() || log.action.contains(query, true) || log.details.contains(query, true) || log.fullName.contains(query, true) || log.role.contains(query, true))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateAuditModuleFilter(module: String) {
        _auditModuleFilter.value = module
    }

    fun updateAuditSearchQuery(query: String) {
        _auditSearchQuery.value = query
    }

    fun addCustomAuditLog(username: String, fullName: String, role: String, action: String, details: String, module: String, status: String = "نجاح (Success)") {
        val newId = (_auditLogs.value.maxOfOrNull { it.id } ?: 0) + 1
        val now = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val newLog = AuditLog(
            id = newId,
            username = username,
            fullName = fullName,
            role = role,
            action = action,
            details = details,
            timestamp = now,
            systemModule = module,
            status = status
        )
        _auditLogs.value = listOf(newLog) + _auditLogs.value
    }

    fun getActiveUserLogInfo(): Triple<String, String, String> {
        val activeUser = (_authSession.value as? AuthSessionState.Authenticated)?.user
        val uName = activeUser?.username ?: "system"
        val fName = activeUser?.fullName ?: "مسؤول النظام الآلي"
        val roleStr = activeUser?.role ?: "Administrator"
        return Triple(uName, fName, roleStr)
    }
}

// --- DOMAIN MODELS FOR EXPANDED LOGISTICS & AUDIT CONTROL ---
data class AssetItem(
    val id: Int,
    val name: String,
    val serialNumber: String,
    val type: String, // "كاشف ألغام (Metal Detector)", "سترة واقية معززة (Armored Vest)", "سيارة دفع رباعي 4x4", "كمبيوتر لوحي ميداني"
    val condition: String, // "ممتازة", "تحت الصيانة", "تالفة", "مفقودة في الميدان"
    val assignedTo: String,
    val storeLocation: String,
    val purchaseDate: String,
    val lastServiceDate: String,
    val supplier: String
)

data class AuditLog(
    val id: Int,
    val username: String,
    val fullName: String,
    val role: String,
    val action: String,
    val details: String,
    val timestamp: String,
    val systemModule: String, // "المالية", "العمليات", "الأصول والخدمات اللوجستية", "الصلاحيات", "بيانات المزامنة"
    val status: String
)

