package com.example.data

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

// ======================== ENTITIES ========================

@Entity(tableName = "decisions_tasks")
data class DecisionTask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val details: String,
    val department: String, // "الإدارة العليا", "المالية", "العمليات", "الموارد البشرية", "العلاقات العامة", "المعرفة والتعلم"
    val assignedTo: String,
    val status: String, // "بانتظار البدء", "قيد العمل", "مكتمل"
    val dueDate: String,
    val riskLevel: String // "منخفض", "متوسط", "مرتفع"
)

@Entity(tableName = "finance_records")
data class FinanceRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val type: String, // "موازنة سنوية", "موازنة مشروع", "مصروفات", "إيرادات", "سلف", "عهد"
    val project: String, // "تطهير شمال غزة", "توعية أريحا", "مسح جنين", "التشغيل العام"
    val amount: Double,
    val category: String, // "رواتب", "معدات", "سفر ومواصلات", "مطبوعات وتوعية", "إيجار مقر"
    val date: String,
    val donor: String, // "UNMAS", "EU", "UNICEF", "موازنة السلطة"
    val isSynced: Boolean = true,
    val notes: String = "",
    val isAiForecasted: Boolean = false
)

@Entity(tableName = "operation_reports")
data class OperationReport(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val type: String, // "مسح ميداني", "حملة توعية", "إدارة حوادث", "سجل ذخائر"
    val fieldTeam: String, // "فريق مسح أ", "فريق تثقيف ب", "فريق تدخل ج"
    val area: String, // "جنين", "الخليل", "غزة", "نابلس", "أريحا"
    val date: String,
    val status: String, // "نشط", "مكتمل", "تحت المراجعة", "خطر مرتفع"
    val details: String,
    val latitude: Double = 31.95,
    val longitude: Double = 35.20,
    val casualties: Int = 0,
    val riskScore: Int = 1, // 1 (منخفض) to 5 (خطر داهم)
    val aiSummary: String = "" // Summary produced by Gemini helper
)

@Entity(tableName = "partners_donors")
data class PartnerDonor(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String, // "مانح رئيسي", "شريك محلي", "منظمة دولية", "متطوع مالي", "وسيلة إعلام"
    val organization: String,
    val contactInfo: String,
    val activeProjects: String,
    val contributions: Double = 0.0,
    val status: String = "نشط"
)

@Entity(tableName = "employee_records")
data class EmployeeRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val role: String, // "مدير عمليات", "مشرف ميداني", "باحث اجتماعي", "محاسب مالي", "منسق تدريب"
    val department: String, // "العمليات", "المالية", "العلاقات العامة", "الموارد البشرية"
    val phone: String,
    val attendanceStatus: String = "حاضر", // "حاضر", "غائب", "إجازة"
    val leaveBalance: Int = 21,
    val lastAppraisal: String = "ممتاز"
)

@Entity(tableName = "knowledge_items")
data class KnowledgeItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val category: String, // "IMAS", "SOP", "EORE Library", "درس مستفاد"
    val content: String,
    val subTitle: String = "",
    val addedBy: String = "الإدارة العليا"
)

@Entity(tableName = "pmac_documents")
data class PmacDocument(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val category: String, // "خرائط ميدانية", "تقارير مالية", "معايير SOP", "اتفاقيات مانحين"
    val fileType: String, // "PDF", "DOCX", "PNG", "JPG"
    val keywords: String, // Comma-separated keywords
    val uploadDate: String,
    val uploadedBy: String,
    val requiredRole: String, // "Administrator", "Executive Director", "Finance Manager", "Operations Manager", etc.
    val currentVersion: Int = 1,
    val localFilePath: String = ""
)

@Entity(tableName = "document_versions")
data class DocumentVersion(
    @PrimaryKey(autoGenerate = true) val versionId: Int = 0,
    val documentId: Int,
    val versionNumber: Int,
    val title: String,
    val updateNotes: String,
    val modifiedDate: String,
    val modifiedBy: String,
    val localFilePath: String = ""
)

// ======================== DAOS ========================

@Dao
interface DecisionTaskDao {
    @Query("SELECT * FROM decisions_tasks ORDER BY id DESC")
    fun getAllDecisions(): Flow<List<DecisionTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDecision(decision: DecisionTask)

    @Update
    suspend fun updateDecision(decision: DecisionTask)

    @Delete
    suspend fun deleteDecision(decision: DecisionTask)
}

@Dao
interface FinanceRecordDao {
    @Query("SELECT * FROM finance_records ORDER BY date DESC, id DESC")
    fun getAllFinanceRecords(): Flow<List<FinanceRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFinance(record: FinanceRecord)

    @Update
    suspend fun updateFinance(record: FinanceRecord)

    @Delete
    suspend fun deleteFinance(record: FinanceRecord)
}

@Dao
interface OperationReportDao {
    @Query("SELECT * FROM operation_reports ORDER BY date DESC")
    fun getAllOperations(): Flow<List<OperationReport>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperation(report: OperationReport)

    @Update
    suspend fun updateOperation(report: OperationReport)

    @Delete
    suspend fun deleteOperation(report: OperationReport)
}

@Dao
interface PartnerDonorDao {
    @Query("SELECT * FROM partners_donors ORDER BY name ASC")
    fun getAllPartners(): Flow<List<PartnerDonor>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPartner(partner: PartnerDonor)

    @Update
    suspend fun updatePartner(partner: PartnerDonor)

    @Delete
    suspend fun deletePartner(partner: PartnerDonor)
}

@Dao
interface EmployeeRecordDao {
    @Query("SELECT * FROM employee_records ORDER BY name ASC")
    fun getAllEmployees(): Flow<List<EmployeeRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployee(employee: EmployeeRecord)

    @Update
    suspend fun updateEmployee(employee: EmployeeRecord)

    @Delete
    suspend fun deleteEmployee(employee: EmployeeRecord)
}

@Dao
interface KnowledgeItemDao {
    @Query("SELECT * FROM knowledge_items ORDER BY category ASC")
    fun getAllKnowledgeItems(): Flow<List<KnowledgeItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKnowledgeItem(item: KnowledgeItem)

    @Delete
    suspend fun deleteKnowledgeItem(item: KnowledgeItem)
}

@Dao
interface PmacDocumentDao {
    @Query("SELECT * FROM pmac_documents ORDER BY uploadDate DESC")
    fun getAllDocuments(): Flow<List<PmacDocument>>

    @Query("SELECT * FROM pmac_documents WHERE title LIKE '%' || :query || '%' OR keywords LIKE '%' || :query || '%' ORDER BY uploadDate DESC")
    fun searchDocuments(query: String): Flow<List<PmacDocument>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: PmacDocument): Long

    @Update
    suspend fun updateDocument(document: PmacDocument)

    @Delete
    suspend fun deleteDocument(document: PmacDocument)
}

@Dao
interface DocumentVersionDao {
    @Query("SELECT * FROM document_versions WHERE documentId = :docId ORDER BY versionNumber DESC")
    fun getVersionsForDocument(docId: Int): Flow<List<DocumentVersion>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVersion(version: DocumentVersion)
}

@Entity(tableName = "users")
data class User(
    @PrimaryKey val username: String,
    val passwordHash: String,
    val fullName: String,
    val role: String,
    val permissions: String
)

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE username = :username")
    suspend fun getUserByUsername(username: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int
}

@Entity(tableName = "emergency_protocols")
data class EmergencyProtocol(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val priority: String,
    val steps: String,
    val contactNo: String,
    val lastUpdated: String = "2026-06-09"
)

@Dao
interface EmergencyProtocolDao {
    @Query("SELECT * FROM emergency_protocols ORDER BY id ASC")
    fun getAllProtocols(): Flow<List<EmergencyProtocol>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProtocol(protocol: EmergencyProtocol)

    @Query("SELECT COUNT(*) FROM emergency_protocols")
    suspend fun getProtocolCount(): Int

    @Query("SELECT * FROM emergency_protocols WHERE title LIKE '%' || :query || '%' OR steps LIKE '%' || :query || '%'")
    fun searchProtocols(query: String): Flow<List<EmergencyProtocol>>
}

// ======================== DATABASE ========================

@Database(
    entities = [
        DecisionTask::class,
        FinanceRecord::class,
        OperationReport::class,
        PartnerDonor::class,
        EmployeeRecord::class,
        KnowledgeItem::class,
        PmacDocument::class,
        DocumentVersion::class,
        User::class,
        EmergencyProtocol::class
    ],
    version = 4,
    exportSchema = false
)
abstract class PmacDatabase : RoomDatabase() {
    abstract fun decisionTaskDao(): DecisionTaskDao
    abstract fun financeRecordDao(): FinanceRecordDao
    abstract fun operationReportDao(): OperationReportDao
    abstract fun partnerDonorDao(): PartnerDonorDao
    abstract fun employeeRecordDao(): EmployeeRecordDao
    abstract fun knowledgeItemDao(): KnowledgeItemDao
    abstract fun pmacDocumentDao(): PmacDocumentDao
    abstract fun documentVersionDao(): DocumentVersionDao
    abstract fun userDao(): UserDao
    abstract fun emergencyProtocolDao(): EmergencyProtocolDao

    companion object {
        @Volatile
        private var INSTANCE: PmacDatabase? = null

        fun getDatabase(context: Context): PmacDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PmacDatabase::class.java,
                    "pmac_offline_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(PmacDatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class PmacDatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Prepopulate database in background
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateDatabase(database)
                    }
                }
            }
        }

        private suspend fun populateDatabase(db: PmacDatabase) {
            // 1. Prepopulate Decisions
            val initialDecisions = listOf(
                DecisionTask(
                    title = "توفير معدات وقاية جديدة لفريق المسح الأثري والشمالي",
                    details = "إتمام تزويد الفرق الميدانية بكاشفات الألغام اليدوية المتطورة وسترات واقية معززة قبل نهاية الشهر للتأهب لتمديد عمليات المسح الاستباقي.",
                    department = "العمليات",
                    assignedTo = "م. يوسف الصالحي (مشرف الميدان)",
                    status = "قيد العمل",
                    dueDate = "2026-06-25",
                    riskLevel = "مرتفع"
                ),
                DecisionTask(
                    title = "إعداد تقرير المانح النصف سنوي لعقد الشراكة مع المانح السويدي",
                    details = "تجميع التقارير الإحصائية والمالية لبرامج التوعية EORE في الضفة الغربية وتجهيز البيانات اللازمة لتطبيق الرؤى التحليلية لصندوق الدعم.",
                    department = "المالية",
                    assignedTo = "شادي جابر (المالية)",
                    status = "بانتظار البدء",
                    dueDate = "2026-06-30",
                    riskLevel = "متوسط"
                ),
                DecisionTask(
                    title = "مراجعة واعتماد النسخة المحدثة من معايير الإجراءات SOP للعمليات الميدانية",
                    details = "تثبيت معايير سحب الذخائر اليدوية وتحديث بروتوكولات الأمان بما يتوافق مع المعايير الدولية لشؤون الألغام IMAS.",
                    department = "الإدارة العليا",
                    assignedTo = "م. حسام التميمي (المدير التنفيذي)",
                    status = "مكتمل",
                    dueDate = "2026-06-05",
                    riskLevel = "منخفض"
                ),
                DecisionTask(
                    title = "تنظيم ندوة إعلامية حول سلامة المزارعين من مخلفات الأجسام الغريبة",
                    details = "تنسيق الفعالية بالتعاون مع وزارة الزراعة والبلديات في محافظة جنين لنشر وعي الحذر.",
                    department = "العلاقات العامة",
                    assignedTo = "دانة جبريل (العلاقات العامة)",
                    status = "قيد العمل",
                    dueDate = "2026-06-18",
                    riskLevel = "منخفض"
                )
            )
            initialDecisions.forEach { db.decisionTaskDao().insertDecision(it) }

            // 2. Prepopulate Finance Records
            val initialFinance = listOf(
                FinanceRecord(
                    title = "موازنة برامج التطهير والمسح المالي للشرق لعام 2026",
                    type = "موازنة سنوية",
                    project = "التشغيل العام",
                    amount = 230000.0,
                    category = "رواتب ومعدات",
                    date = "2026-01-01",
                    donor = "UNMAS",
                    notes = "الميزانية المعتمدة لبدء مشاريع الضفة الغربية."
                ),
                FinanceRecord(
                    title = "شراء أجهزة كشف معادن متطورة فئة MineLab F3",
                    type = "مصروفات",
                    project = "تطهير شمال جنين",
                    amount = 14500.0,
                    category = "معدات",
                    date = "2026-05-15",
                    donor = "EU",
                    notes = "تم توريد الأجهزة ودخلت السجل الفعلي لتشغيل الأصول."
                ),
                FinanceRecord(
                    title = "منحة تمويل التثقيف المدرسي EORE للأطفال بمحافظة الخليل",
                    type = "إيرادات",
                    project = "توعية أريحا والجنوب",
                    amount = 45000.0,
                    category = "مطبوعات وتوعية",
                    date = "2026-03-10",
                    donor = "UNICEF",
                    notes = "تحصيل الدفعة الأولى من التمويل الخاص بالمدارس الحكومية."
                ),
                FinanceRecord(
                    title = "سلفة تشغيلية عاجلة لنفقات النقل والوقود لفرقة المسح ب",
                    type = "سلف",
                    project = "مسح جنين",
                    amount = 1800.0,
                    category = "سفر ومواصلات",
                    date = "2026-06-02",
                    donor = "UNMAS",
                    notes = "مصروف تشغيلي دوري لتسريع فحص البؤر الحدودية المشتبهة."
                )
            )
            initialFinance.forEach { db.financeRecordDao().insertFinance(it) }

            // 3. Prepopulate Operations
            val initialOperations = listOf(
                OperationReport(
                    title = "دراسة مسح غير تقني لمحيط بلدة يعبد",
                    type = "مسح ميداني",
                    fieldTeam = "فريق مسح أ",
                    area = "جنين",
                    date = "2026-06-04",
                    status = "نشط",
                    details = "تم التواصل مع البلدية والمزارعين لتحديد إحداثيات المشتبه بها لمخلفات القصف التاريخي. غُطيت مساحة تقريبية 2500م٢ مهددة.",
                    latitude = 32.44,
                    longitude = 35.18,
                    riskScore = 4,
                    aiSummary = "المسح حدد بؤر حيوية مهددة للمزارعين، يوصى بالانتقال للمسح التقني وتطبيق طوق تحذيري فوري لعلو نقاط الخطورة الفيلية."
                ),
                OperationReport(
                    title = "انفجار لغم مضاد للأفراد قديم في حقول زراعية بالخليل",
                    type = "إدارة حوادث",
                    fieldTeam = "فريق تدخل ج",
                    area = "الخليل",
                    date = "2026-05-20",
                    status = "مكتمل",
                    details = "انفجار جسم مجهول تحت عجلات جرار زراعي. الحادث تسبب في بتر جزئي بالطرف السلفي لسائق الجرار وإصابته بشظايا من مخلف متراكم قديم.",
                    latitude = 31.52,
                    longitude = 35.09,
                    casualties = 1,
                    riskScore = 5,
                    aiSummary = "الحادث ناجم عن تحريك لغم مغطى بالرمال إثر عمليات الحراثة العملاقة. الإجراء المصحح: إخضاع الأرض لمسح تطهيري شامل وتعميد لوحات الخطر."
                ),
                OperationReport(
                    title = "أسبوع التوعية EORE الشامل بمخيم الأطفال في أريحا",
                    type = "حملة توعية",
                    fieldTeam = "فريق تثقيف ب",
                    area = "أريحا",
                    date = "2026-05-12",
                    status = "مكتمل",
                    details = "إعطاء 5 محاضرات تفاعلية وتوزيع كراسات تلوين للأطفال حول تصرف بسلامة مع علامات التحذير والمخاطر والمفرقعات العشوائية.",
                    latitude = 31.86,
                    longitude = 35.45,
                    riskScore = 1,
                    aiSummary = "تمت تغطية ما يزيد عن ١٤٠ طفلاً واختبار معارفهم المكتسبة بنسبة نجاح ٩٢٪ مع استبيان الرضا لأولياء الأمور."
                ),
                OperationReport(
                    title = "جرد وتصنيف سجل ذخائر مكتشفة في منطقة قلقيلية البلد",
                    type = "سجل ذخائر",
                    fieldTeam = "فريق تدخل ج",
                    area = "نابلس",
                    date = "2026-06-07",
                    status = "تحت المراجعة",
                    details = "تم العثور على ٣ قذائف هاون قديمة صدئة غير منفجرة في تسوية مبنى قديم قيد التشييد. تم تطويق المكان واستدعاء وحدة الهندسة للتعامل.",
                    latitude = 32.22,
                    longitude = 35.26,
                    riskScore = 5,
                    aiSummary = "القذائف تتمتع بحساسية بالغة للصدمات بسبب تلف صواعق التماس. تم تأجيل التحميل تمهيداً للإبطال والنسف في محجر آمن."
                )
            )
            initialOperations.forEach { db.operationReportDao().insertOperation(it) }

            // 4. Prepopulate Partners/Donors
            val initialPartners = listOf(
                PartnerDonor(
                    name = "دائرة الأمم المتحدة للأعمال المتعلقة بالألغام (UNMAS)",
                    type = "مانح رئيسي",
                    organization = "دائرة الأمم المتحدة للأعمال الإنسانية",
                    contactInfo = "unmas.pal@un.org | +970 2 240 5501",
                    activeProjects = "منحة دعم عمليات التطهير والمسح التقني بالضفة",
                    contributions = 580000.0,
                    status = "نشط"
                ),
                PartnerDonor(
                    name = "صندوق التنمية التابع للاتحاد الأوروبي (EU Co-op)",
                    type = "مانح رئيسي",
                    organization = "الاتحاد الأوروبي للشرق الأوسط",
                    contactInfo = "eu.representative@eeas.europa.eu",
                    activeProjects = "شراء كواشف متطورة وتدريب المهندسين الميدانيين",
                    contributions = 340000.0,
                    status = "نشط"
                ),
                PartnerDonor(
                    name = "الهلال الأحمر الفلسطيني",
                    type = "شريك محلي",
                    organization = "جمعية الهلال الأحمر",
                    contactInfo = "info@palestinercs.org | 101",
                    activeProjects = "إسعاف أولي طارئ بالحادث وتأهيل ضحايا الدروع والألغام الأثرية",
                    contributions = 0.0,
                    status = "نشط"
                ),
                PartnerDonor(
                    name = "شبكة متطوعي التوعية المجتمعية بيعبد",
                    type = "متطوع مالي",
                    organization = "منظومات شبابية أهلية مجتمعية",
                    contactInfo = "yaabad.youth@gmail.com",
                    activeProjects = "توزيع بوسترات وملصقات خطر بالقرب من الجدار الفاصل",
                    contributions = 1500.0,
                    status = "نشط"
                )
            )
            initialPartners.forEach { db.partnerDonorDao().insertPartner(it) }

            // 5. Prepopulate Employees
            val initialEmployees = listOf(
                EmployeeRecord(
                    name = "م. حسام التميمي",
                    role = "المدير التنفيذي العام",
                    department = "الإدارة العليا",
                    phone = "0599111222",
                    attendanceStatus = "حاضر",
                    leaveBalance = 24,
                    lastAppraisal = "امتياز"
                ),
                EmployeeRecord(
                    name = "شادي جابر",
                    role = "رئيس الدائرة المالية الموارد",
                    department = "المالية",
                    phone = "0599333444",
                    attendanceStatus = "حاضر",
                    leaveBalance = 18,
                    lastAppraisal = "امتياز"
                ),
                EmployeeRecord(
                    name = "ريم الصالح",
                    role = "مديرة العمليات والمسح الميداني",
                    department = "العمليات",
                    phone = "0599555666",
                    attendanceStatus = "حاضر",
                    leaveBalance = 15,
                    lastAppraisal = "ممتاز"
                ),
                EmployeeRecord(
                    name = "يوسف سلامة",
                    role = "مهندس مسح وقائد الفريق أ",
                    department = "العمليات",
                    phone = "0599777888",
                    attendanceStatus = "حاضر",
                    leaveBalance = 12,
                    lastAppraisal = "جيد جداً"
                ),
                EmployeeRecord(
                    name = "سهر الخالدي",
                    role = "منسق برامج التثقيف المدرسي EORE",
                    department = "العلاقات العامة",
                    phone = "0599000111",
                    attendanceStatus = "إجازة",
                    leaveBalance = 14,
                    lastAppraisal = "ممتاز"
                )
            )
            initialEmployees.forEach { db.employeeRecordDao().insertEmployee(it) }

            // 6. Prepopulate Knowledge Items
            val initialKnowledge = listOf(
                KnowledgeItem(
                    title = "المعايير الدولية للأعمال المتعلقة بالألغام (IMAS - الإصدار الخامس)",
                    category = "IMAS",
                    content = "تعتبر هذه المعايير هي الإطار المعمد رسمياً من مكتب الأمم المتحدة (UNMAS) لجميع الهيئات المؤسسية الوطنية والمكلفة بالأعمال. تنص القوانين على ضرورة التزام مشغل الميدان بحصر النطاق الجغرافي واستعمال وسائل الأمان القصوى كالدروع والخوذ والخرائط المصنفة وتفويض القيادة الموحدة.",
                    subTitle = "المعيار رقم 09.10 - عمليات المسح غير التقني والتقني"
                ),
                KnowledgeItem(
                    title = "إجراءات العمل القياسية (SOP) في التطهير والتدمير اليدوي للأجسام المنفجرة",
                    category = "SOP",
                    content = "١. تأمين محيط بقطر 500 متر على الأقل.\n٢. توظيف كاشفات رنين مغناطيسي عالية الحساسية.\n٣. استخدام المجارف غير المغناطيسية البلاستيكية للنبش الحذر بزاوية ٣٠ درجة.\n٤. يُمنع تحريك أو فك أي فيوز تفجير يدوي في الحقل؛ يتم إعداد حشوة نسف محلية متفجرة موجهة لإتلاف الجسم في مكانه.",
                    subTitle = "الدليل المعتمد رقم SOP-PMAC-04"
                ),
                KnowledgeItem(
                    title = "سلسلة التوعية بمخاطر المتفجرات للأطفال والمزارعين (EORE)",
                    category = "EORE Library",
                    content = "يتوجب التثقيف بناء على الرموز الثلاثة لسلامة التحذير: ١. الجمجمة مع العظمتين المتقاطعتين، ٢. المثلثات الحمراء المثبتة عمودياً، ٣. الأكوام الهندسية من الأحجار الملونة باللون الأحمر. يتوجب توعية المجتمعات بـ 'لا تلمس، لا تقترب، ابلغ فوراً على الرقم المجاني ١٠١ هاتفياً'.",
                    subTitle = "برامج التثقيف المدرسي والمجتمعي"
                ),
                KnowledgeItem(
                    title = "الدرس المستفاد: تأثير انجراف التربة الطينية على تحول مواقع الألغام",
                    category = "درس مستفاد",
                    content = "رصدت فرق فحص الوديان المتاخمة لقطاع أريحا تغير مواقع حقول حربية تم مسحها وتأكيدها مسبقاً بفعل فيضانات السيول الفصلية. الخلاصة: عند وقوع كوارث طبيعية أو انجراف سيول غزير، تسقط صفة التطهير المؤقت وتخضع السهول والمسيلات كبؤر مجددة لخطورة اللغم الأرضي المنجرف.",
                    subTitle = "تحرير: دائرة المتابعة والتقييم الفني"
                )
            )
            initialKnowledge.forEach { db.knowledgeItemDao().insertKnowledgeItem(it) }

            // 7. Prepopulate Documents
            val initialDocs = listOf(
                PmacDocument(
                    id = 1,
                    title = "خارطة توزيع الملوثات الخطرة والذخائر - غزة والشمال",
                    category = "خرائط ميدانية",
                    fileType = "PDF",
                    keywords = "غزة, شمال, خارطة, تلوث, حوض, مسح ميداني",
                    uploadDate = "2026-06-01",
                    uploadedBy = "ريم الصالح",
                    requiredRole = "Field Supervisor",
                    currentVersion = 2,
                    localFilePath = "/docs/gaza_hazard_map_v2.pdf"
                ),
                PmacDocument(
                    id = 2,
                    title = "المشروع التجريبي لموازنة التثقيف في مدارس أريحا",
                    category = "تقارير مالية",
                    fileType = "DOCX",
                    keywords = "موازنة, نفقات, أريحا, تمويل, يونيسيف, EORE",
                    uploadDate = "2026-04-15",
                    uploadedBy = "شادي جابر",
                    requiredRole = "Finance Manager",
                    currentVersion = 1,
                    localFilePath = "/docs/jericho_budget_2026.docx"
                ),
                PmacDocument(
                    id = 3,
                    title = "كتيب المعايير الوطنية لإبطال الأجسام المتفجرة الموضعية SOP-09",
                    category = "معايير SOP",
                    fileType = "PDF",
                    keywords = "SOP, إبطال, صواعق, سلامة, تفجير, معايير وطنية",
                    uploadDate = "2026-05-20",
                    uploadedBy = "ريم الصالح",
                    requiredRole = "Operations Manager",
                    currentVersion = 3,
                    localFilePath = "/docs/sop_09_disposal_v3.pdf"
                )
            )
            initialDocs.forEach { db.pmacDocumentDao().insertDocument(it) }

            val initialVersions = listOf(
                DocumentVersion(
                    documentId = 1,
                    versionNumber = 1,
                    title = "خارطة غزة والشمال المبدئية",
                    updateNotes = "مسودة أولى بناء على المشاهدات العامة والمسح البصري الأولي بالقطاع الشمالي.",
                    modifiedDate = "2026-05-10",
                    modifiedBy = "ريم الصالح",
                    localFilePath = "/docs/gaza_hazard_map_v1.pdf"
                ),
                DocumentVersion(
                    documentId = 1,
                    versionNumber = 2,
                    title = "تحديث خارطة غزة بعد رصد الدرونز التابع لوحدة العمليات الفنية",
                    updateNotes = "تم دمج إحداثيات GPS الدقيقة المستخلصة من طائرة الاستطلاع والمساحة الجوية والتحذيرات السكنية.",
                    modifiedDate = "2026-06-01",
                    modifiedBy = "ريم الصالح",
                    localFilePath = "/docs/gaza_hazard_map_v2.pdf"
                ),
                DocumentVersion(
                    documentId = 2,
                    versionNumber = 1,
                    title = "مخطط المظروف المالي لمدارس أريحا المعتمد",
                    updateNotes = "النسخة المالية النهائية المتفق عليها مع اليونيسيف لتمويل برنامج EORE التثقيفي الموحد.",
                    modifiedDate = "2026-04-15",
                    modifiedBy = "شادي جابر",
                    localFilePath = "/docs/jericho_budget_2026.docx"
                ),
                DocumentVersion(
                    documentId = 3,
                    versionNumber = 1,
                    title = "المشروع الأولي 2024 لبروتوكول التفجير الإمامي",
                    updateNotes = "صياغة المبادئ الأساسية لمسافة الأمان وسحب صواعق الفيوزات.",
                    modifiedDate = "2024-11-20",
                    modifiedBy = "حسام التميمي",
                    localFilePath = "/docs/sop_09_disposal_v1.pdf"
                ),
                DocumentVersion(
                    documentId = 3,
                    versionNumber = 2,
                    title = "تعديل الفقرة 4 الخاصة بالقذائف 155 ملم ثقيلة الوزن",
                    updateNotes = "إدراج شروط وقائية لتجنب ارتداد الشظايا وتحديد زوايا النبش الآمنة ٣٠ درجة.",
                    modifiedDate = "2025-08-01",
                    modifiedBy = "ريم الصالح",
                    localFilePath = "/docs/sop_09_disposal_v2.pdf"
                ),
                DocumentVersion(
                    documentId = 3,
                    versionNumber = 3,
                    title = "إتمام دراسة إبطال المفرقعات الحساسة الموضعية وتثبيت الإجراء",
                    updateNotes = "إضافة التعليمات النهائية لمعايير النسف الموقعي وتأمين طواقم الصيانة الميدانية.",
                    modifiedDate = "2026-05-20",
                    modifiedBy = "ريم الصالح",
                    localFilePath = "/docs/sop_09_disposal_v3.pdf"
                )
            )
            initialVersions.forEach { db.documentVersionDao().insertVersion(it) }

            // 9. Prepopulate Users for Offline Credentials Verification (Dexie equivalent 'users' store)
            val initialUsers = listOf(
                User(
                    username = "admin",
                    passwordHash = "admin123",
                    fullName = "دانة جبريل (المدير العام والمسؤول الأمني)",
                    role = "Administrator",
                    permissions = "all_permissions,create_report,delete_report,approve_finance,sync_data"
                ),
                User(
                    username = "ops",
                    passwordHash = "ops123",
                    fullName = "م. يوسف الصالحي (مدير العمليات الميدانية)",
                    role = "Operations Manager",
                    permissions = "read_reports,write_reports,create_report,execute_sync"
                ),
                User(
                    username = "finance",
                    passwordHash = "finance123",
                    fullName = "شادي جابر (المراقب المالي والمشرف)",
                    role = "Finance Manager",
                    permissions = "read_finance,write_finance,approve_finance"
                ),
                User(
                    username = "field",
                    passwordHash = "field123",
                    fullName = "ريم الصالح (مفتش ومرافق ميداني)",
                    role = "Field Supervisor",
                    permissions = "read_reports,write_reports"
                )
            )
            initialUsers.forEach { db.userDao().insertUser(it) }

            // 10. Seed Emergency Protocols
            val initialProtocols = listOf(
                EmergencyProtocol(
                    title = "بروتوكول العثور على لغم أرضي مكشوف أو جسم مشبوه مجهول",
                    priority = "🔴 حرجة جداً",
                    steps = "١. التوقف التام فوراً والوقوف بثبات وعدم محاولة الاقتراب.\n٢. منع أي فرد آخر في الفريق أو المارين من التحرك باتجاه الجسم.\n٣. تمييز المنطقة بعلامة واضحة من مسافة آمنة دون ملامسة الأرض المجاورة.\n٤. العودة خطوة بخطوة عبر المسار الآمن الذي جئت منه.\n٥. التبليغ اللحظي للعمليات الميدانية وتزويدهم بالإحداثيات بدقة.",
                    contactNo = "طوارئ الدفاع المدني: 102 | عمليات PMAC: +970-2-123456",
                    lastUpdated = "2026-06-09"
                ),
                EmergencyProtocol(
                    title = "بروتوكول إخلاء جريح من حقل ألغام نشط",
                    priority = "🔴 حرجة جداً",
                    steps = "١. عدم الاندفاع العشوائي لإنقاذ الجريح لتفادي الوقوع في انفجار ثانوي.\n٢. تحديد المسار الآمن المؤكد (أو المسار المتبع مسبقاً) والتواصل صوتياً مع المصاب لتثبيته.\n٣. استخدام أدوات فحص النبش التدريجي بزاوية ٣٠ درجة للوصول إلى الجريح عند الضرورة القصوى.\n٤. تقديم الإسعافات الأولية الأساسية للنزيف والصدمات في الموقع الآمن ومحاولة ربط النزيف بجبيرة وضغط مناسب.\n٥. طلب دعم مستعجل ومروحية الإخلاء إن أمكن عبر الهلال الأحمر والعمليات.",
                    contactNo = "الهلال الأحمر: 101 | عمليات الطوارئ المشتركة: 112",
                    lastUpdated = "2026-06-09"
                ),
                EmergencyProtocol(
                    title = "بروتوكول حدوث تماس فني أثناء تفكيك فيوز أو صاعق عبوة",
                    priority = "🔴 حرجة جداً",
                    steps = "١. التخلي الفوري اللحظي عن العملية والابتعاد بسرعة لساتر حماية أمني مسبق الصنع.\n٢. إطلاق صافرة الإنذار المخصصة بالموقع للتنبيه بوجود تفجير أو خطر وشيك.\n٣. عزل المنطقة لقطر لا يقل عن ٥٠٠ متر عن المدنيين وأفراد الدعم الفني.\n٤. انتظار مرور ٣٠ دقيقة على الأقل قبل الاقتراب مجدداً لتقييم الوضع إذا لم ينفجر الصاعق.\n٥. إجراء نقر فني متباعد وموجه والتحول لطريقة النسف الموقعي التدميري بدلاً من التفكيك اليدوي.",
                    contactNo = "دائرة تطهير المتفجرات: 105 | مشرف العمليات: م. يوسف الصالحي",
                    lastUpdated = "2026-06-09"
                )
            )
            initialProtocols.forEach { db.emergencyProtocolDao().insertProtocol(it) }
        }
    }
}
