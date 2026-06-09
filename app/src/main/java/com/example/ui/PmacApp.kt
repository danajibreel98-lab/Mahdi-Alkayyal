package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.network.GeminiClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PmacApp(viewModel: PmacViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // --- Collect States ---
    val currentSection by viewModel.currentSection.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val pendingSyncs by viewModel.pendingSyncCount.collectAsStateWithLifecycle()
    val isSyncing by viewModel.syncing.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Global User Authentication State (React Context Provider equivalent)
    val authSession by viewModel.authSession.collectAsStateWithLifecycle()
    val currentUser = (authSession as? AuthSessionState.Authenticated)?.user
    val selectedRole = currentUser?.role ?: ""
    val userPermissions = currentUser?.permissions ?: ""
    
    // Active dialog hooks 
    var showSecurityInfo by remember { mutableStateOf(false) }

    // Forces Right-To-Left Layout direction for native Arabic feel.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        when (val session = authSession) {
            is AuthSessionState.Unauthenticated -> {
                OfflineLoginView(viewModel)
            }
            is AuthSessionState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "جاري التحقق من الهوية وصلاحيات الأمن محلياً (Room SQlite)...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            is AuthSessionState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.padding(24.dp).widthIn(max = 450.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "عذرًا، فشل التحقق من الهوية الميدانية",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                session.message,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { viewModel.clearAuthError() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("العودة لشاشة الدخول والتحقق")
                            }
                        }
                    }
                }
            }
            is AuthSessionState.Authenticated -> {
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        
        // Navigation side menu items in Arabic
        val navigationItems = listOf(
            Triple("Dashboard", "لوحة المؤشرات العامة", Icons.Default.Home),
            Triple("Operations", "العمليات وحوادث الألغام", Icons.Default.Warning),
            Triple("ReportDiscovery", "الإبلاغ عن لغم جديد (Pouch)", Icons.Default.Add),
            Triple("Finance", "الدائرة المالية والموازنة", Icons.Default.AccountBalance),
            Triple("Administration", "القرارات وتكليفات الإدارة", Icons.Default.Gavel),
            Triple("Relations", "العلاقات العامة والشركاء", Icons.Default.Group),
            Triple("HR", "الموارد البشرية والحضور", Icons.Default.Person),
            Triple("Assets", "الأصول والخدمات اللوجستية", Icons.Default.Build),
            Triple("Audit", "سجل التدقيق والمراقبة", Icons.Default.List),
            Triple("Knowledge", "مكتبة معايير الألغام SOP", Icons.Default.Book),
            Triple("Documents", "إدارة المستندات والوثائق", Icons.Default.Folder),
            Triple("QuickGuide", "بروتوكولات الطوارئ والدليل", Icons.Default.Info),
            Triple("PouchSync", "مزامنة البيانات ولوحة الاتصال", Icons.Default.Sync),
            Triple("AI Assistant", "المساعد الذكي لبحث البيانات", Icons.Default.SmartToy),
            Triple("Settings", "الصلاحيات وإعدادات النظام", Icons.Default.Settings)
        )

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.width(310.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(20.dp)
                    ) {
                        Column {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "المركز الفلسطيني للأعمال المتعلقة بالألغام",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Palestine Mine Action Center (PMAC)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            if (currentUser != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = currentUser.fullName,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp)
                    ) {
                        items(navigationItems) { item ->
                            val selected = currentSection == item.first
                            NavigationDrawerItem(
                                icon = { Icon(item.third, contentDescription = null) },
                                label = { Text(item.second, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                                selected = selected,
                                onClick = {
                                    viewModel.setSection(item.first)
                                    coroutineScope.launch { drawerState.close() }
                                },
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .testTag("nav_item_${item.first.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(
                                        text = "بوابة PMAC الإدارية",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                    Text(
                                        text = navigationItems.firstOrNull { it.first == currentSection }?.second ?: "",
                                        fontSize = 12.sp,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = { coroutineScope.launch { drawerState.open() } },
                                modifier = Modifier.testTag("menu_button")
                            ) {
                                Icon(Icons.Default.Menu, contentDescription = "القائمة")
                            }
                        },
                        actions = {
                            // Security / Prototype path banner popup toggle page
                            IconButton(onClick = { showSecurityInfo = true }) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "التحذير الأمني",
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }

                            // Interactive Online/Offline Simulation Toggle Badge
                            Card(
                                onClick = { 
                                    viewModel.toggleOnlineMode() 
                                    val modeStateText = if (isOnline) "الأوفلاين (المحلي)" else "أونلاين (المزامنة)"
                                    Toast.makeText(context, "تم التبديل إلى وضع $modeStateText", Toast.LENGTH_SHORT).show()
                                },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isOnline) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .testTag("network_toggle_badge")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (isOnline) Color(0xFF4CAF50) else Color(0xFFF44336))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isOnline) "متصل (أونلاين)" else "أوفلاين - محلي",
                                        fontSize = 11.sp,
                                        color = if (isOnline) Color(0xFF2E7D32) else Color(0xFFC62828),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // If offline modifications pending, show direct manual sync action trigger!
                            if (pendingSyncs > 0) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (!isOnline) {
                                                Toast.makeText(context, "يرجى تشغيل الشبكة أولاً بالضغط على وضع أوفلاين لتفعيل المزامنة!", Toast.LENGTH_LONG).show()
                                            } else {
                                                viewModel.toggleOnlineMode() // triggers simulated sync
                                                viewModel.toggleOnlineMode()
                                            }
                                        },
                                        modifier = Modifier.testTag("sync_action_button")
                                    ) {
                                        if (isSyncing) {
                                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "مزامنة التعديلات",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.testTag("refresh_sync_icon")
                                            )
                                        }
                                    }
                                    Text(
                                        text = "($pendingSyncs)",
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    )
                },
                bottomBar = {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .border(width = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Dashboard
                            val isDash = currentSection == "Dashboard"
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable { viewModel.setSection("Dashboard") }
                                    .weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(48.dp)
                                        .height(32.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isDash) MaterialTheme.colorScheme.primaryContainer else Color.Transparent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Dashboard,
                                        contentDescription = "لوحة المؤشرات",
                                        tint = if (isDash) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "لوحة المؤشرات",
                                    fontSize = 11.sp,
                                    fontWeight = if (isDash) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isDash) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }

                            // Operations
                            val isOps = currentSection == "Operations"
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable { viewModel.setSection("Operations") }
                                    .weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(48.dp)
                                        .height(32.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isOps) MaterialTheme.colorScheme.primaryContainer else Color.Transparent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "العمليات وحوادث الألغام",
                                        tint = if (isOps) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "العمليات",
                                    fontSize = 11.sp,
                                    fontWeight = if (isOps) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isOps) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }

                            // Finance
                            val isFin = currentSection == "Finance"
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable { viewModel.setSection("Finance") }
                                    .weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(48.dp)
                                        .height(32.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isFin) MaterialTheme.colorScheme.primaryContainer else Color.Transparent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = "المالية والموازنات",
                                        tint = if (isFin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "المالية",
                                    fontSize = 11.sp,
                                    fontWeight = if (isFin) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isFin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }

                            // More (opens Drawer)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable { coroutineScope.launch { drawerState.open() } }
                                    .weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(48.dp)
                                        .height(32.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.Transparent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "المزيد",
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "المزيد",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    GlobalNetworkIndicator(viewModel)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        if (checkRouteAccess(selectedRole, currentSection)) {
                            when (currentSection) {
                                "Dashboard" -> DashboardView(viewModel)
                                "Operations" -> OperationsView(viewModel, selectedRole)
                                "ReportDiscovery" -> ReportDiscoveryView(viewModel)
                                "Finance" -> FinanceView(viewModel, selectedRole)
                                "Administration" -> AdministrationView(viewModel, selectedRole)
                                "Relations" -> RelationsView(viewModel, selectedRole)
                                "HR" -> HrView(viewModel, selectedRole)
                                "Assets" -> AssetsView(viewModel, selectedRole)
                                "Audit" -> AuditLogsView(viewModel, selectedRole)
                                "Knowledge" -> KnowledgeView(viewModel, selectedRole)
                                "Documents" -> DocumentsView(viewModel, selectedRole)
                                "QuickGuide" -> QuickGuideView(viewModel)
                                "PouchSync" -> PouchSyncDashboardView(viewModel)
                                "AI Assistant" -> AiChatView(viewModel)
                                "Settings" -> SettingsView(viewModel)
                                else -> DashboardView(viewModel)
                            }
                        } else {
                            AccessDeniedView(
                                currentRole = selectedRole,
                                attemptedSection = currentSection,
                                onGoToDashboard = { viewModel.setSection("Dashboard") },
                                onGoToSettings = { viewModel.setSection("Settings") }
                            )
                        }

                        // --- Security and Prototype Info Dialogue ---
                        if (showSecurityInfo) {
                            AlertDialog(
                                onDismissRequest = { showSecurityInfo = false },
                                confirmButton = {
                                    Button(onClick = { showSecurityInfo = false }) {
                                        Text("حسناً وفهمت")
                                    }
                                },
                                title = { Text("🔒 نظام الحوكمة وحماية البيانات") },
                                text = {
                                    Column {
                                        Text(
                                            text = "⚠️ تنبيه أمني (Prototype Warning):",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        Text(
                                            text = "هذا الإصدار التقني مهيأ بـ Direct REST API للاتصال اللحظي بـ Gemini لأغراض المحاكاة السريعة دون اتصال خادم خلفي. في بيئات الإنتاج الفعلية، يتم تأمين مفاتيح الاستدعاء بالكامل ولا تُحقن في التطبيق بل يعتمد على خواديم موثقة و Firebase App Check لمنع المخترقين من تفكيك النواتج.",
                                            fontSize = 13.sp,
                                            textAlign = TextAlign.Justify,
                                            lineHeight = 18.sp
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "معماريات الأوفلاين (Offline Architectures):",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "يعمل النظام بالكامل في بيئة محلية تامة معتمدًا على Room database. أي مدخلات خلال انقطاع الإنترنت تُسجل أوتوماتيكياً وتظل معلقة، لتتم فلترتها ومزامنتها لحظة استعادة الربط بدون أي فقد يذكر في المعطيات الميدانية أو سجلات الضحايا والذخيرة.",
                                            fontSize = 13.sp,
                                            textAlign = TextAlign.Justify,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            )
                        }

                        // Floating alert if success/error states updated
                        LaunchedEffect(uiState) {
                            if (uiState is PmacUiState.Success) {
                                Toast.makeText(context, (uiState as PmacUiState.Success).message, Toast.LENGTH_LONG).show()
                                viewModel.clearUiState()
                            } else if (uiState is PmacUiState.Error) {
                                Toast.makeText(context, (uiState as PmacUiState.Error).error, Toast.LENGTH_LONG).show()
                                viewModel.clearUiState()
                            }
                        }
                    }
                }
            }
        }
    }
}
}
}

// ======================== SUB-VIEWS REPRESENTING SCREENS ========================

@Composable
fun DashboardView(viewModel: PmacViewModel) {
    // Collect database stats
    val opList by viewModel.operations.collectAsStateWithLifecycle()
    val decList by viewModel.decisions.collectAsStateWithLifecycle()
    val finList by viewModel.financeRecords.collectAsStateWithLifecycle()
    val prList by viewModel.partners.collectAsStateWithLifecycle()

    val pendingTasksSize = decList.count { it.status != "مكتمل" }
    val totalBudget = finList.filter { it.type.contains("موازنة") }.sumOf { it.amount }
    val totalExpenses = finList.filter { it.type.contains("مصروف") }.sumOf { it.amount }
    val revenueBudget = finList.filter { it.type.contains("إيراد") }.sumOf { it.amount }
    val netCapital = totalBudget + revenueBudget - totalExpenses

    val casualtiesCount = opList.sumOf { it.casualties }
    val incidentsCount = opList.count { it.type.contains("حوادث") }
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("dashboard_scroll_column"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Banner Welcome
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "المركز الفلسطيني للأعمال المتعلقة بالألغام (PMAC)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "مرحباً بكم في بوابة السيطرة والقيادة. يعتمد هذا النظام على بروتوكول المزامنة والأوفلاين لحفظ سلامة الفنيين في الميدان.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                        modifier = Modifier.padding(top = 4.dp),
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // --- Budget Alert Card if expenses exceed 80% ---
        if (totalBudget > 0 && totalExpenses / totalBudget >= 0.80) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "تنبيه تخطي الموازنة المحددة! (Over-Budget Alert)",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "تخطت المصاريف التشغيلية للمشاريع نسبة ٨٠٪ من الموازنة السنوية المرصودة. يرجى توخي التقنين في النفقات الميدانية.",
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f),
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // 1. Management KPI Row (Safety Index 94%, Budget Burn 62%, Ops Rate 8.2km2)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Safety Index Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "SAFETY INDEX",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "94%",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), shape = CircleShape)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.94f)
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                            )
                        }
                    }
                }

                // Budget Burn Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "BUDGET BURN",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "62%",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), shape = CircleShape)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.62f)
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                            )
                        }
                    }
                }

                // Ops Rate Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "OPS RATE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "8.2 km²",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .background(Color.White.copy(alpha = 0.25f), shape = CircleShape)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.75f)
                                    .fillMaxHeight()
                                    .background(Color.White, shape = CircleShape)
                            )
                        }
                    }
                }
            }
        }

        // Analytics / Core KPI Cards Block (Traditional Stats)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    KpiCard(
                        title = "مهمات وتكاليف معلقة",
                        value = "$pendingTasksSize مهمة",
                        icon = Icons.Default.Assignment,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    KpiCard(
                        title = "إجمالي الموازنة العامة",
                        value = "$${String.format("%,.0f", totalBudget)}",
                        icon = Icons.Default.AccountBalanceWallet,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    KpiCard(
                        title = "الرصيد المالي الصافي",
                        value = "$${String.format("%,.0f", netCapital)}",
                        icon = Icons.Default.TrendingUp,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    KpiCard(
                        title = "ضحايا وحوادث الألغام",
                        value = "$incidentsCount حوادث ($casualtiesCount إصابة)",
                        icon = Icons.Default.CrisisAlert,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 2. Field Operations Center Map Card with active alerts signal
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Field Operations Center",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Gaza - Northern Sector",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFAD8D8))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "4 ACTIVE ALERTS",
                                color = Color(0xFF3F0001),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Stylized map coordinates mockup container
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                    ) {
                        // Horizontal Coordinate Guideline Lines
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceEvenly
                        ) {
                            repeat(5) {
                                Divider(
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.07f),
                                    thickness = 1.dp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        // Vertical Coordinate Guideline Lines
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            repeat(7) {
                                Divider(
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.07f),
                                    thickness = 1.dp,
                                    modifier = Modifier.fillMaxHeight().width(1.dp)
                                )
                            }
                        }

                        // Coordinates Overlay Label Mockup
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                                .background(Color.White.copy(alpha = 0.85f), shape = RoundedCornerShape(4.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "LAT: 31.501 | LON: 34.466",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        // Pulsing beacon visual elements
                        Box(
                            modifier = Modifier.align(Alignment.Center)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), shape = CircleShape)
                                    .align(Alignment.Center)
                            )
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(Color.White, shape = CircleShape)
                                    .align(Alignment.Center)
                            )
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                                    .align(Alignment.Center)
                            )
                        }
                    }

                    // Bottom field team specs labels
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text(
                                    text = "Teams Deployment",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "12 Units Fielded",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text(
                                    text = "Clearance Status",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "Active Phase II",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)

                    // Unified list of hotspots mapped inside the card boundaries
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "📍 بؤر ومسارات الألغام والمخاطر الموثقة:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        if (opList.isEmpty()) {
                            Text("لا يوجد سجلات عملياتية داتا لعرض النقاط الميدانية.")
                        } else {
                            opList.forEach { report ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (report.type.contains("حوادث")) Icons.Default.Error else Icons.Default.Navigation,
                                        contentDescription = null,
                                        tint = if (report.type.contains("حوادث")) Color(0xFFBA1A1A) else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(report.title, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text(
                                            text = "المنطقة: ${report.area} | الإحداثيات: ${report.latitude}° شمالاً / ${report.longitude}° شرقاً",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (report.riskScore >= 4) Color(0xFFFAD8D8) else MaterialTheme.colorScheme.primaryContainer,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "خطورة: ${report.riskScore}/5",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (report.riskScore >= 4) Color(0xFF3F0001) else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. AI Finance Predictor / Insights Card (Dark Forest Theme #111F0E)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111F0E))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(MaterialTheme.colorScheme.primary, shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI FINANCE PREDICTOR",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.7f),
                            letterSpacing = 1.sp
                        )
                    }

                    Text(
                        text = "برنامج التحليل والذكاء الاصطناعي يتوقع تسييل فائض مالي بقيمة $12,400 من موازنة المشروع ER-24. توصية: يفضل إعادة توجيه المخصص لرفع جهوزية صيانة المعدات الوقائية في الميدان قبل نهاية الربع الحالي.",
                        fontSize = 12.sp,
                        color = Color.White,
                        lineHeight = 17.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { 
                                Toast.makeText(context, "تم رفع التقرير الفني المالي للإدارة التنفيذية", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إصدار التقرير", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { 
                                Toast.makeText(context, "تم حفظ التوصية للمراجعة اللاحقة", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("تجاهل", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Budget ratio visual indicators
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📊 موازنة المشاريع واستهلاك المخصصات",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "نسب الإنجاز ومعدلات الإنفاق الفعلي للمشاريع والأقسام الميدانية النشطة",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    ProjectRatioItem(name = "تطهير ومسح مناطق غزة المستعصية", spent = 112000.0, budgeted = 130000.0)
                    ProjectRatioItem(name = "برنامج التثقيف والتوعية المدرسية (أريحا)", spent = 18000.0, budgeted = 45000.0)
                    ProjectRatioItem(name = "مسح وحصر القذائف والذخائر في جنين", spent = 14500.0, budgeted = 55000.0)
                    ProjectRatioItem(name = "التشغيل والرواتب الفنية للمركز", spent = 45000.0, budgeted = 120000.0)
                }
            }
        }

        // Recent activity styled exactly like the high-density HTML mockup
        item {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "السجل الأخير للأنشطة والمدفوعات",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "عرض الكل",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            viewModel.setSection("Finance")
                        }
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "دعم وقود الآليات والمعدات الثقيلة",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "دائرة النقل الميداني • 14:20 مساءً",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "-$420.00",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFBA1A1A)
                            )
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "#882A",
                                    fontSize = 9.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KpiCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, containerColor: Color, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.height(110.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
            Text(value, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun ProjectRatioItem(name: String, spent: Double, budgeted: Double) {
    val progress = if (budgeted > 0) spent / budgeted else 0.0
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "أنفق $${String.format("%,.0f", spent)} من $${String.format("%,.0f", budgeted)} (${String.format("%.0f", progress * 100)}%)",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = progress.toFloat(),
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = if (progress >= 0.85) Color.Red else MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

// ======================== OPERATIONS VIEW (العمليات وحوادث الألغام) ========================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperationsView(viewModel: PmacViewModel, currentRole: String) {
    val operations by viewModel.operations.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    
    var activeTabState by remember { mutableStateOf(0) }
    
    // Form States
    var opTitle by remember { mutableStateOf("") }
    var opType by remember { mutableStateOf("مسح ميداني") } // "مسح ميداني", "حملة توعية", "إدارة حوادث", "سجل ذخائر"
    var opTeam by remember { mutableStateOf("فريق مسح أ") }
    var opArea by remember { mutableStateOf("جنين") }
    var opDetails by remember { mutableStateOf("") }
    var casualties by remember { mutableStateOf("0") }
    var riskScoreSelected by remember { mutableStateOf(3f) }
    
    val optTypes = listOf("مسح ميداني", "حملة توعية", "إدارة حوادث", "سجل ذخائر")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = activeTabState) {
            Tab(selected = activeTabState == 0, onClick = { activeTabState = 0 }) {
                Text("📋 تقارير وحوادث الميدان", modifier = Modifier.padding(12.dp))
            }
            Tab(selected = activeTabState == 1, onClick = { activeTabState = 1 }) {
                Text("➕ تسجيل تقرير جديد", modifier = Modifier.padding(12.dp))
            }
        }

        if (activeTabState == 0) {
            LazyColumn(modifier = Modifier.padding(16.dp)) {
                if (operations.isEmpty()) {
                    item { Text("لا يوجد تقارير ميدانية مسجلة حتى الآن. تفضل بتسجيل أول بند في تيب الإضافة.") }
                } else {
                    items(operations) { report ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = when (report.type) {
                                                    "إدارة حوادث" -> Color(0xFFFFEBEE)
                                                    "مسح ميداني" -> Color(0xFFE3F2FD)
                                                    "حملة توعية" -> Color(0xFFE8F5E9)
                                                    else -> Color(0xFFECEFF1)
                                                },
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = report.type,
                                            color = when (report.type) {
                                                "إدارة حوادث" -> Color(0xFFC62828)
                                                "مسح ميداني" -> Color(0xFF1565C0)
                                                "حملة توعية" -> Color(0xFF2E7D32)
                                                else -> Color(0xFF37474F)
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    
                                    // Delete action button if Role permits (not Guest Viewer!)
                                    IconButton(
                                        onClick = {
                                            if (currentRole == "Viewer") {
                                                // Restricted
                                            } else {
                                                viewModel.deleteOperation(report)
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(report.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("المنطقة الجغرافية: ${report.area} | المكلف: ${report.fieldTeam}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(report.details, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("الإصابات/الضحايا الحادثية: ${report.casualties} | نقاط الخطر الفيلية: ${report.riskScore}/5", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)

                                // Smart AI Summarization Panel inside each operation item
                                Spacer(modifier = Modifier.height(10.dp))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "🧠 تلخيص الحادث وتحليل الخطورة (AI)",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Button(
                                                onClick = { viewModel.runAISummarizeOperation(report) },
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                modifier = Modifier.height(30.dp)
                                            ) {
                                                Text("توليد بالذكاء الإصطناعي", fontSize = 10.sp)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        if (report.aiSummary.isNotEmpty()) {
                                            Text(
                                                text = report.aiSummary,
                                                fontSize = 12.sp,
                                                lineHeight = 16.sp,
                                                textAlign = TextAlign.Justify
                                            )
                                        } else {
                                            Text(
                                                text = "اضغط على زر التوليد أعلى لصياغة مخرجات ذكية للتقرير واقتراح التصليحات الأمنية فورا من خادم غاميني.",
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Addition Form View
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("إضافة نشاط أو حصر لغم/حادثة جديدة بالميدان", fontWeight = FontWeight.Bold)
                    Text("يتم حفط هذه البيانات محلياً مباشرة فورية ومزامنتها تبريراً لقوانين الأوفلاين.", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                }

                item {
                    OutlinedTextField(
                        value = opTitle,
                        onValueChange = { opTitle = it },
                        label = { Text("عنوان التقرير / الحادث الميداني") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Text("تصنيف الفحص المیداني", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        optTypes.forEach { type ->
                            val isSel = opType == type
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { opType = type }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(type, fontSize = 10.sp, color = if (isSel) Color.White else Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = opTeam,
                        onValueChange = { opTeam = it },
                        label = { Text("اسم الفريق الميداني المكلف") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = opArea,
                        onValueChange = { opArea = it },
                        label = { Text("الموقع / المحافظة (جنين، غزة، الخليل..)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = casualties,
                        onValueChange = { casualties = it },
                        label = { Text("إصابات المدنيين / الضحايا (إن وجد)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Column {
                        Text("مؤشر الخطر ودرجة الصعوبة: ${riskScoreSelected.toInt()}/5")
                        Slider(
                            value = riskScoreSelected,
                            onValueChange = { riskScoreSelected = it },
                            valueRange = 1f..5f,
                            steps = 3
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = opDetails,
                        onValueChange = { opDetails = it },
                        label = { Text("شرح تفصيلي للحادث و العثور والمعاينات الميدانية") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Button(
                        onClick = {
                            if (currentRole == "Viewer") {
                                // Block
                            } else if (opTitle.isBlank() || opDetails.isBlank()) {
                                // Show state
                            } else {
                                viewModel.addOperation(
                                    title = opTitle,
                                    type = opType,
                                    fieldTeam = opTeam,
                                    area = opArea,
                                    date = "2026-06-08",
                                    status = "نشط",
                                    details = opDetails,
                                    casualties = casualties.toIntOrNull() ?: 0,
                                    riskScore = riskScoreSelected.toInt()
                                )
                                // Clear
                                opTitle = ""
                                opDetails = ""
                                casualties = "0"
                                activeTabState = 0
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("حفظ التقرير المیداني بقاعدة البيانات", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ======================== FINANCE VIEW ========================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceView(viewModel: PmacViewModel, currentRole: String) {
    val financeRecords by viewModel.financeRecords.collectAsStateWithLifecycle()
    val aiForecast by viewModel.aiForecastState.collectAsStateWithLifecycle()
    val scannedResult by viewModel.scannedInvoiceResult.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()

    var tabSelectedState by remember { mutableStateOf(0) }
    
    // Form Inputs
    var finTitle by remember { mutableStateOf("") }
    var finType by remember { mutableStateOf("مصروفات") }
    var finProject by remember { mutableStateOf("التشغيل العام") }
    var finAmount by remember { mutableStateOf("") }
    var finCategory by remember { mutableStateOf("رواتب وجدولة") }
    var finDonor by remember { mutableStateOf("UNMAS") }
    var finNotes by remember { mutableStateOf("") }

    // Invoice optical scanner text mock input 
    var rawTextInvoiceInput by remember { mutableStateOf("") }

    val sampleInvoiceText1 = "FATOORAH COMPANY FOR SECURITY DEV - SPECIAL ORDER.\nITEMS: 4 MINE METAL DETECTOR COILS\nTOTAL COST: \$3500.00\nPROJECT: مسح جنين\nDONOR: EU\nDATE: 2026-06-05"
    val sampleInvoiceText2 = "مبيعات شركة الهنا للسيارات والمحروقات المحدودة.\nالبيان: محروقات ديزل لتسيير قوافل المسح الميداني للقطاع\nالسعر الكلي للفاتورة: \$1200.00\nالمشروع المستهدف: تطهير شمال غزة\nالدعم المبرم من: UNMAS\nالسلطة الوطنية 2026"

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tabSelectedState) {
            Tab(selected = tabSelectedState == 0, onClick = { tabSelectedState = 0 }) {
                Text("💰 كشف الموازنات والنفقات")
            }
            Tab(selected = tabSelectedState == 1, onClick = { tabSelectedState = 1 }) {
                Text("💵 إدخال بند مالي")
            }
            Tab(selected = tabSelectedState == 2, onClick = { tabSelectedState = 2 }) {
                Text("📷 استخراج الفواتير (AI)")
            }
        }

        if (tabSelectedState == 0) {
            LazyColumn(modifier = Modifier.padding(16.dp)) {
                // AI Predictor widget block
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "💡 مستشار التنبؤ المالي وتوقعات نفاد الميزانية (AI Forecast)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = aiForecast.ifEmpty { "اضغط على زر استشارة غاميني للحوسبة الذكية وتوقع الموازنات ومحدد استنفاد الرصيد المادي للمركز." },
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.runFinanceBurnRateForecast() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                            ) {
                                Text("توقع واستنتاج نفاد السيولة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                items(financeRecords) { record ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(record.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("النوع: ${record.type} | المانح الممول: ${record.donor}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("بند: ${record.category} | مشروع: ${record.project}", fontSize = 12.sp)
                                if (record.notes.isNotEmpty()) {
                                    Text("ملاحظة: ${record.notes}", fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                            
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "$${String.format("%,.0f", record.amount)}",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (record.type.contains("مصروف") || record.type.contains("سلف")) Color(0xFFC62828) else Color(0xFF2E7D32)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                IconButton(
                                    onClick = {
                                        if (currentRole == "Viewer") {
                                            // Restricted
                                        } else {
                                            viewModel.deleteFinance(record)
                                        }
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف بند", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        } else if (tabSelectedState == 1) {
            // Form इضافة بند مالي
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text("تسجيل معاملة مالية جديدة في السجلات", fontWeight = FontWeight.Bold)
                    Text("يدعم السجل حفظ بيانات السلف والعهد للفرق الميدانية في أوفلاين تام.", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                }

                item {
                    OutlinedTextField(
                        value = finTitle,
                        onValueChange = { finTitle = it },
                        label = { Text("اسم المعاملة المالية / الغرض") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = finAmount,
                        onValueChange = { finAmount = it },
                        label = { Text("المقدار المالي الفعلي (دولار أمريكي)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = finType,
                        onValueChange = { finType = it },
                        label = { Text("نوع المعاملة (موازنة، مصروفات، إيرادات، سلف، عهد)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = finProject,
                        onValueChange = { finProject = it },
                        label = { Text("المشروع الملحق به المصرف") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = finCategory,
                        onValueChange = { finCategory = it },
                        label = { Text("فئة الصرف (رواتب، مواصلات، معدات، مطبوعات)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = finDonor,
                        onValueChange = { finDonor = it },
                        label = { Text("المانح الرئيسي الممضي للحساب") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = finNotes,
                        onValueChange = { finNotes = it },
                        label = { Text("ملاحظات المتابعة الإضافية") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Button(
                        onClick = {
                            if (currentRole == "Viewer") {
                                // Block
                            } else if (finTitle.isBlank() || finAmount.isBlank()) {
                                // Empty state
                            } else {
                                viewModel.addFinance(
                                    title = finTitle,
                                    type = finType,
                                    project = finProject,
                                    amount = finAmount.toDoubleOrNull() ?: 100.0,
                                    category = finCategory,
                                    date = "2026-06-08",
                                    donor = finDonor,
                                    notes = finNotes
                                )
                                // Reset
                                finTitle = ""
                                finAmount = ""
                                finNotes = ""
                                tabSelectedState = 0
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("حفظ البند المالي بالخزينة", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // Invoice Extractor Tab
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("📷 قارئ ومحلل الفواتير الذكي بالذكاء الاصطناعي", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("الصق بيانات نصية مستخرجة من صورة فاتورة حارس أو جهاز طبي، وسيقوم غاميني بتحليلها وملء استمارات المالية بشكل آلي.", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { rawTextInvoiceInput = sampleInvoiceText1 },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                        ) {
                            Text("نموذج كواشف (EU)", fontSize = 10.sp)
                        }
                        Button(
                            onClick = { rawTextInvoiceInput = sampleInvoiceText2 },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                        ) {
                            Text("نموذج وقود (UNMAS)", fontSize = 10.sp)
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = rawTextInvoiceInput,
                        onValueChange = { rawTextInvoiceInput = it },
                        label = { Text("نص الفاتورة المطلوب استقرائه و تحليله") },
                        minLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Button(
                        onClick = { viewModel.runInvoiceDataExtraction(rawTextInvoiceInput) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🚀 استخراج وتصنيف البيانات الفاتورية", fontWeight = FontWeight.Bold)
                    }
                }

                // Extracted result box
                if (scannedResult != null) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                            modifier = Modifier.fillMaxWidth(),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("✅ البيانات المستخرجة أوتوماتيكياً بقوة AI:", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("اسم البند المستنبط: ${scannedResult!!.title}")
                                Text("المشروع المرتبط: ${scannedResult!!.project}")
                                Text("المقدار المستخرج الكلي: $${scannedResult!!.amount}")
                                Text("الجهة الممولة: ${scannedResult!!.donor}")
                                Text("الفئة المصنفة: ${scannedResult!!.category}")
                                Spacer(modifier = Modifier.height(10.dp))
                                
                                Button(
                                    onClick = {
                                        viewModel.addFinance(
                                            title = scannedResult!!.title,
                                            type = scannedResult!!.type,
                                            project = scannedResult!!.project,
                                            amount = scannedResult!!.amount,
                                            category = scannedResult!!.category,
                                            date = scannedResult!!.date,
                                            donor = scannedResult!!.donor,
                                            notes = scannedResult!!.notes
                                        )
                                        viewModel.clearScannedInvoice()
                                        rawTextInvoiceInput = ""
                                        tabSelectedState = 0
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("تأكيد واعتماد الإدخال بالخزنة المجمعة")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ======================== ADMINISTRATION VIEW ========================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdministrationView(viewModel: PmacViewModel, currentRole: String) {
    val decisions by viewModel.decisions.collectAsStateWithLifecycle()
    
    var showAddFormState by remember { mutableStateOf(false) }
    
    // Quick Add Form
    var decTitle by remember { mutableStateOf("") }
    var decDept by remember { mutableStateOf("العمليات") }
    var decAssignedTo by remember { mutableStateOf("") }
    var decRisk by remember { mutableStateOf("متوسط") }
    var decDetails by remember { mutableStateOf("") }

    // Meeting notes compiler states
    var meetingTitle by remember { mutableStateOf("") }
    var rawMeetingNotes by remember { mutableStateOf("") }

    val sampleNotesExample = "اجتماع لمناقشة خطة الطوارئ.\nأوصى المدير التنفيذي بضرورة تكليف ريم جابر بقسم المالية بصرف مخصصات شراء بطاريات استبدال فوري لكاشفات الألغام قبل الأسبوع المقبل والمخاطرة تعتبر مرتفعة."

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("⚖️ قرارات وتكليفات الإدارة العليا للمركز", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text("مراقبة التكليفات والتنفيذ ومؤشرات كفاءة إنجاز المهام KPI وحوكمة المخاطر.", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
        }

        // Active Tasks Dashboard
        items(decisions) { decision ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                border = if (decision.riskLevel == "مرتفع") CardDefaults.outlinedCardBorder() else null
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(decision.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Box(
                            modifier = Modifier
                                .background(
                                    color = when (decision.status) {
                                        "مكتمل" -> Color(0xFFE8F5E9)
                                        "قيد العمل" -> Color(0xFFE3F2FD)
                                        else -> Color(0xFFECEFF1)
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(decision.status, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (decision.status == "مكتمل") Color(0xFF2E7D32) else Color.DarkGray)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text("الجهة الفيلية: ${decision.department} | المكلف بالمتابعة: ${decision.assignedTo}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(decision.details, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("المهلة الزمنية: ${decision.dueDate} | مستوى الخطر: ${decision.riskLevel}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        
                        // Toggle Status checkmark action
                        if (decision.status != "مكتمل") {
                            Button(
                                onClick = {
                                    if (currentRole == "Viewer") {
                                        // Blocked
                                    } else {
                                        val nextSt = if (decision.status == "بانتظار البدء") "قيد العمل" else "مكتمل"
                                        viewModel.updateDecision(decision.copy(status = nextSt))
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text(if (decision.status == "بانتظار البدء") "البدء بالعمل" else "تأكيد الاكتمال", fontSize = 9.sp)
                            }
                        }
                    }
                }
            }
        }

        // Section for AI Generator meeting builder
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("💡 مولد التكليفات والخطوات التنفيذية من الاجتماعات (AI Minutes)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("اكتب محضر نقاش سريع طارئ، وسينتج الذكاء الاصطناعي تكاليف قانونية ويسجلها في مصفوفة القرار التفاعلية فوراً.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha=0.8f))
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = meetingTitle,
                        onValueChange = { meetingTitle = it },
                        label = { Text("عنوان الاجتماع (مثال: طوارئ وقود جنين)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rawMeetingNotes,
                        onValueChange = { rawMeetingNotes = it },
                        label = { Text("الملاحظات والنقاشات العشوائية المطروحة") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { rawMeetingNotes = sampleNotesExample },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.outline),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("محاكاة مذكر الإدارة", fontSize = 10.sp)
                        }
                        Button(
                            onClick = {
                                if (meetingTitle.isNotBlank() && rawMeetingNotes.isNotBlank()) {
                                    viewModel.generateMeetingMinutesAndTasks(meetingTitle, rawMeetingNotes)
                                    meetingTitle = ""
                                    rawMeetingNotes = ""
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("استخراج القرار وزرعه (AI)", fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // Manual Add Panel trigger
        item {
            if (!showAddFormState) {
                Button(
                    onClick = { showAddFormState = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("➕ تكليف إداري يدوي فوري")
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("إسناد مهمة إدارية رسمية", fontWeight = FontWeight.Bold)

                        OutlinedTextField(
                            value = decTitle,
                            onValueChange = { decTitle = it },
                            label = { Text("اسم المهمة الإدارية القرار") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = decAssignedTo,
                            onValueChange = { decAssignedTo = it },
                            label = { Text("المسؤول المكلف بالخطوات") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = decDept,
                            onValueChange = { decDept = it },
                            label = { Text("القسم الموجه له (العمليات، العلاقات..)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = decDetails,
                            onValueChange = { decDetails = it },
                            label = { Text("شرح التعليمات التنفيذية") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showAddFormState = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("إلغاء الأمر")
                            }
                            Button(
                                onClick = {
                                    if (currentRole == "Viewer") {
                                        // Restricted
                                    } else if (decTitle.isNotBlank()) {
                                        viewModel.addDecision(
                                            title = decTitle,
                                            details = decDetails,
                                            department = decDept,
                                            assignedTo = decAssignedTo,
                                            dueDate = "2026-06-25",
                                            riskLevel = decRisk
                                        )
                                        // Clear
                                        decTitle = ""
                                        decDetails = ""
                                        decAssignedTo = ""
                                        showAddFormState = false
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("تأخير وزرع الخطوة")
                            }
                        }
                    }
                }
            }
        }
    }
}

// ======================== RELATIONS VIEW ========================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelationsView(viewModel: PmacViewModel, currentRole: String) {
    val partners by viewModel.partners.collectAsStateWithLifecycle()
    
    var partnerName by remember { mutableStateOf("") }
    var partnerType by remember { mutableStateOf("مانح رئيسي") }
    var partnerOrg by remember { mutableStateOf("") }
    var partnerContact by remember { mutableStateOf("") }
    var partnerContribution by remember { mutableStateOf("") }
    var partnerProject by remember { mutableStateOf("") }

    var showFormState by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("🤝 دائرة العلاقات العامة وحوكمة الشركاء الداعمين", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text("إدارة ملف المانحين، عقود المعاهدات، سجلات الإعلام والتواصل والدعم التطوعي.", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
        }

        items(partners) { partner ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(partner.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(partner.type, fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("الهيئة التابعة: ${partner.organization} | حالة العقد: ${partner.status}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("المشاريع النشطة الممولة: ${partner.activeProjects}", fontSize = 12.sp)
                    Text("وسائل الربط: ${partner.contactInfo}", fontSize = 11.sp, color = Color.Gray)

                    if (partner.contributions > 0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "إجمالي المساهمات التراكمية: $${String.format("%,.0f", partner.contributions)} دولار",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFF2E7D32)
                        )
                    }

                    // Delete partner button if not Viewer
                    if (currentRole != "Viewer") {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { viewModel.deletePartner(partner) }) {
                                Text("إلغاء الشراكة", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Simple manual partner entry
        item {
            if (!showFormState) {
                Button(onClick = { showFormState = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("➕ تسجيل شريك / اتحاد مانحين جديد")
                }
            } else {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("سجل قيد مانحين PMAC", fontWeight = FontWeight.Bold)

                        OutlinedTextField(value = partnerName, onValueChange = { partnerName = it }, label = { Text("اسم الجهة أو الداعم") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = partnerType, onValueChange = { partnerType = it }, label = { Text("نوع الشراكة (مانح رئيسي، شريك محلي...)") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = partnerOrg, onValueChange = { partnerOrg = it }, label = { Text("التبعية والمؤسسة التابع لها") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = partnerContact, onValueChange = { partnerContact = it }, label = { Text("بريد / تواصل المسؤول المكلف") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = partnerProject, onValueChange = { partnerProject = it }, label = { Text("اسم المشروع الممول المخصص") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(
                            value = partnerContribution,
                            onValueChange = { partnerContribution = it },
                            label = { Text("المقدار المالي الفعلي للتبرع (إن وجد)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { showFormState = false }, modifier = Modifier.weight(1f)) {
                                Text("إبطال الإدخال")
                            }
                            Button(
                                onClick = {
                                    if (currentRole == "Viewer") {
                                        // Blocked
                                    } else if (partnerName.isNotBlank()) {
                                        viewModel.addPartner(
                                            name = partnerName,
                                            type = partnerType,
                                            organization = partnerOrg,
                                            contactInfo = partnerContact,
                                            activeProjects = partnerProject,
                                            contributions = partnerContribution.toDoubleOrNull() ?: 0.0
                                        )
                                        partnerName = ""
                                        partnerOrg = ""
                                        partnerContribution = ""
                                        partnerProject = ""
                                        showFormState = false
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("اعتماد الشراكة")
                            }
                        }
                    }
                }
            }
        }
    }
}

// ======================== HR VIEW ========================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HrView(viewModel: PmacViewModel, currentRole: String) {
    val employees by viewModel.employees.collectAsStateWithLifecycle()
    
    var empName by remember { mutableStateOf("") }
    var empRole by remember { mutableStateOf("") }
    var empDept by remember { mutableStateOf("العمليات") }
    var empPhone by remember { mutableStateOf("") }
    
    var showAddEmployee by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("👥 إدارة الموارد البشرية وشؤون العاملين بالميدان", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text("مراقبة حضور كادر المسح، طلبات الأذونات، الفلو التشغيلي، وتقييم الدورات والمهمات.", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
        }

        // Attendance Quick Sheet Section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("💡 كشف الحضور والانصراف السريع اليومي للفرق", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("اضغط على حالة الحضور تحت كل إطار موظف لتعديل حالته للتو في قاعدة البيانات أوفلاين فورا.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha=0.8f))
                }
            }
        }

        items(employees) { emp ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(emp.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("${emp.role} | إدارة: ${emp.department}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        }

                        // Attendance status toggle click handler (real-time offline update!)
                        Box(
                            modifier = Modifier
                                .background(
                                    color = when (emp.attendanceStatus) {
                                        "حاضر" -> Color(0xFFE8F5E9)
                                        "غائب" -> Color(0xFFFFEBEE)
                                        else -> Color(0xFFFFF3E0)
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    if (currentRole != "Viewer") {
                                        val nextAtState = when (emp.attendanceStatus) {
                                            "حاضر" -> "غائب"
                                            "غائب" -> "إجازة"
                                            else -> "حاضر"
                                        }
                                        viewModel.updateEmployee(emp.copy(attendanceStatus = nextAtState))
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = emp.attendanceStatus,
                                color = when (emp.attendanceStatus) {
                                    "حاضر" -> Color(0xFF2E7D32)
                                    "غائب" -> Color(0xFFC62828)
                                    else -> Color(0xFFE65100)
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("رقم الجوال: ${emp.phone} | رصيد الإجازات المتبقي: ${emp.leaveBalance} يوم", fontSize = 12.sp)
                    Text("التقييم السنوي الأخير المعتمد: ${emp.lastAppraisal}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary)

                    if (currentRole != "Viewer") {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { viewModel.deleteEmployee(emp) }) {
                                Text("إنهاء الخدمة", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Form Addition
        item {
            if (!showAddEmployee) {
                Button(onClick = { showAddEmployee = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("➕ تعيين وتوظيف فرد جديد بالمركز")
                }
            } else {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("عقد تعيين كادر PMAC", fontWeight = FontWeight.Bold)

                        OutlinedTextField(value = empName, onValueChange = { empName = it }, label = { Text("الاسم الكامل للموظف") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = empRole, onValueChange = { empRole = it }, label = { Text("المسمى الوظيفي المعتمد") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = empDept, onValueChange = { empDept = it }, label = { Text("القسم الإلحاقي (العمليات، العلاقات العامة..)") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = empPhone, onValueChange = { empPhone = it }, label = { Text("رقم جوال تواصل") }, modifier = Modifier.fillMaxWidth())

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { showAddEmployee = false }, modifier = Modifier.weight(1f)) {
                                Text("إلغاء التعيين")
                            }
                            Button(
                                onClick = {
                                    if (currentRole == "Viewer") {
                                        // Block
                                    } else if (empName.isNotBlank()) {
                                        viewModel.addEmployee(
                                            name = empName,
                                            role = empRole,
                                            department = empDept,
                                            phone = empPhone
                                        )
                                        empName = ""
                                        empRole = ""
                                        empPhone = ""
                                        showAddEmployee = false
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("توقيع العقد الرسمي")
                            }
                        }
                    }
                }
            }
        }
    }
}

// ======================== KNOWLEDGE VIEW & STAFF QUIZ (المعرفة والتعلم SOP) ========================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KnowledgeView(viewModel: PmacViewModel, currentRole: String) {
    val knowledgeItems by viewModel.knowledgeItems.collectAsStateWithLifecycle()
    
    var showQuizState by remember { mutableStateOf(false) }
    var currentQuestionIdx by remember { mutableStateOf(0) }
    var quizScore by remember { mutableStateOf(0) }
    var selectedOptionIdx by remember { mutableStateOf<Int?>(null) }
    var quizCompleted by remember { mutableStateOf(false) }

    // Hardcoded test questions on Mine Safety (الامتحانات الفنية للكوادر E-Quiz)
    val quizQuestions = listOf(
        Triple(
            "ما هي الإجراء الفني القانوني الفوري عند العثور على لغم أرضي يدوي صدء؟",
            listOf(
                "محاولة سحبه بسلسلة حديدية لتأمين المكان المجموع.",
                "تفجير الجسم في مكانه بحشوة موجهة عازلة بالاتفاق مع SOP-04.",
                "تفكييك الفيوز والصاعق المدمج يدوياً بمفتاح كاشف.",
                "إغلاقه بالرمال الطينية فقط دون إبلاغ الدائرة."
            ),
            1
        ),
        Triple(
            "ما هو معنى وضع أكوام من الحجارة الملونة باللون الأحمر بمسارات الفحص؟",
            listOf(
                "وجود منطقة غنية بالمعادن الطينية الخصبة.",
                "علامة معتمدة دولياً لوجود بؤرة حوادث وخطر ألغام داهم ولا يجب العبور.",
                "مكان مخصص لاستراحة الفرق الهندسية الميدانية.",
                "طريق آمن تماماً لعبور المدنيين و الحراثة الزراعية."
            ),
            1
        ),
        Triple(
            "ماذا يجب على الباحث فعله إثر حصول فيضان أو انجراف طيني شديد للتربة؟",
            listOf(
                "اعتبار السهول خالية من الأخطار لإتمام التطهير الفصلي مسبقاً.",
                "تعطيل المسح نهائياً ونقل الفريق إلى المحرك الرئيسي.",
                "إعادة إخضاع السهول والمسيلات للفحص والمسح باعتبارها بؤر مجددة لخطورة الألغام المنجرفة.",
                "اكتفاء بوضع لوحة تحذيرية قديمة بالمكان."
            ),
            2
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("📚 بنك المعلومات ومكتبة المعايير الفنية (IMAS & SOP)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text("قاعدة وثائق مكافحة المتفجرات في فلسطين، دليل الإجراءات التشغيلية SOP وحالات الحوادث القياسية.", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
        }

        // Sub-tabs / action buttons
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { showQuizState = !showQuizState },
                    colors = ButtonDefaults.buttonColors(containerColor = if (showQuizState) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Star, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (showQuizState) "إغلاق الاختبار" else "التدريب والاختبار الإلكتروني (E-Quiz)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // --- Interactive Staff E-Quiz Panel ---
        if (showQuizState) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "✍️ اختبار كفاءة السلامة وأمان مكافحة الألغام للموظفين",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (!quizCompleted) {
                            val question = quizQuestions[currentQuestionIdx]
                            Text(
                                text = "سؤال ${currentQuestionIdx + 1}/3: ${question.first}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )

                            question.second.forEachIndexed { idx, option ->
                                val isSelected = selectedOptionIdx == idx
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable { selectedOptionIdx = idx }
                                ) {
                                    Text(
                                        text = option,
                                        modifier = Modifier.padding(12.dp),
                                        fontSize = 12.sp,
                                        color = if (isSelected) Color.White else Color.Black
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    if (selectedOptionIdx != null) {
                                        if (selectedOptionIdx == question.third) {
                                            quizScore += 1
                                        }
                                        selectedOptionIdx = null
                                        if (currentQuestionIdx < quizQuestions.size - 1) {
                                            currentQuestionIdx += 1
                                        } else {
                                            quizCompleted = true
                                        }
                                    }
                                },
                                modifier = Modifier.align(Alignment.End),
                                enabled = selectedOptionIdx != null
                            ) {
                                Text(if (currentQuestionIdx == quizQuestions.size - 1) "إنهاء الإجابات" else "السؤال التالي")
                            }
                        } else {
                            // Quiz Completed Screen
                            Text(
                                text = "🎉 اكتمل اختبار السلامة التقنية للمركز!",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF2E7D32)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "درجة تقييم الكفاءة المحرزة: $quizScore من أصل 3 أسئلة (${String.format("%.0f", (quizScore.toFloat()/3)*100)}%)",
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        currentQuestionIdx = 0
                                        quizScore = 0
                                        selectedOptionIdx = null
                                        quizCompleted = false
                                    }
                                ) {
                                    Text("إعادة الاختبار")
                                }
                                Button(
                                    onClick = { showQuizState = false },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.outline)
                                ) {
                                    Text("إنهاء والمتابعة")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Standard references guidelines lists (IMAS / SOPs)
        items(knowledgeItems) { item ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(item.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(item.category, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                    if (item.subTitle.isNotEmpty()) {
                        Text(item.subTitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = item.content,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Justify,
                        lineHeight = 17.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("المسؤول الناشر: ${item.addedBy}", fontSize = 10.sp, color = Color.Gray)

                    if (currentRole != "Viewer") {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { viewModel.deleteKnowledge(item) }) {
                                Text("إسقاط الوثيقة", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ======================== AI CHAT ASSISTANT VIEW (المساعد الذكي) ========================

@Composable
fun AiChatView(viewModel: PmacViewModel) {
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isLoading by viewModel.chatLoading.collectAsStateWithLifecycle()

    var textInput by remember { mutableStateOf("") }

    val quickPrompts = listOf(
        "كم عدد الحوادث والوفيات المسجلة بالداتا؟",
        "هل الميزانية المالية للمشاريع في خطر؟",
        "ما هو معيار SOP لإبطال وتفجير الذخائر؟",
        "أعطني ملخص للقرارات والتكليفات الحالية؟"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("🧠 المساعد ومستشار الذكاء الاصطناعي التقني (RAG-AI Command)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Text("يقوم المساعد بقراءة معطيات قاعدة البيانات الأوفلاين المسجلة للمركز لحظيا ودمجها ليجيبك على كافة استفسارات التخطيط والمالية والعمليات.", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
        Spacer(modifier = Modifier.height(10.dp))

        // Dialogue history lazy box
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), shape = RoundedCornerShape(12.dp))
                .padding(8.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { msg ->
                    val isBot = msg.sender == "bot"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isBot) Arrangement.Start else Arrangement.End
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isBot) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer
                            ),
                            shape = RoundedCornerShape(
                                topStart = 12.dp,
                                topEnd = 12.dp,
                                bottomStart = if (isBot) 0.dp else 12.dp,
                                bottomEnd = if (isBot) 12.dp else 0.dp
                            ),
                            modifier = Modifier.widthIn(max = 260.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = if (isBot) "🤖 مستشار الـ PMAC" else "👤 الموظف المسؤول",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (isBot) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = msg.message,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                if (isLoading) {
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("المساعد الذكي يحلل أرقام السجلات ويصيغ الرد...", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick prompts helper cards scroll bar row
        Box(modifier = Modifier.fillMaxWidth().height(40.dp)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.Start
            ) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        quickPrompts.forEach { qPrompt ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .clickable {
                                        viewModel.sendChatMessage(qPrompt)
                                    }
                            ) {
                                Text(
                                    text = qPrompt,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Input chat edit box
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = { Text("اطرح استفسار أو أمر إداري..", fontSize = 12.sp) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_chat_input"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                maxLines = 2
            )
            IconButton(
                onClick = {
                    if (textInput.isNotBlank()) {
                        viewModel.sendChatMessage(textInput)
                        textInput = ""
                    }
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .size(48.dp)
                    .testTag("submit_chat_button")
            ) {
                Icon(Icons.Default.Send, contentDescription = "إرسال", tint = Color.White)
            }
        }
    }
}

// ======================== SETTINGS & PERMISSIONS VIEW ========================

@Composable
fun SettingsView(viewModel: PmacViewModel) {
    val authSession by viewModel.authSession.collectAsStateWithLifecycle()
    val currentUser = (authSession as? AuthSessionState.Authenticated)?.user
    val currentRole = currentUser?.role ?: ""
    val permissions = currentUser?.permissions ?: ""

    val rolesInArabic = mapOf(
        "Administrator" to "المدير العام والمسؤول التقني (Administrator)",
        "Executive Director" to "المدير التنفيذي (Executive Director)",
        "Finance Manager" to "رئيس الدائرة المالية (Finance Manager)",
        "Operations Manager" to "مدير العمليات الفنية (Operations Manager)",
        "EORE Manager" to "منسق برامج التوعية والتثقيف (EORE Manager)",
        "Public Relations Officer" to "مسؤول العلاقات العامة (PR Officer)",
        "HR Officer" to "مسؤول الموارد البشرية (HR Officer)",
        "Field Supervisor" to "مشرف الفحص الميداني (Field Supervisor)",
        "Data Entry" to "مدخل بيانات السجلات (Data Entry)"
    )

    var activeSubTab by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TabRow(
            selectedTabIndex = activeSubTab,
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
        ) {
            Tab(selected = activeSubTab == 0, onClick = { activeSubTab = 0 }) {
                Text("👮 الصلاحيات والتحكم", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
            Tab(selected = activeSubTab == 1, onClick = { activeSubTab = 1 }) {
                Text("📊 المعمار المرجعي و DDL", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }

        if (activeSubTab == 0) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text("⚙️ الصلاحيات وإدارة مستخدمي ومتحكمي النظام", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("التحقق من تفويض الصلاحيات محلياً (Offline Identity Management) المرتبط بقاعدة بيانات Room.", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("👤 الحساب الميداني النشط حالياً", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))

                    if (currentUser != null) {
                        Text("الاسم الميداني الكامل: ${currentUser.fullName}", fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("الدور الوظيفي بالمنفذ: ${rolesInArabic[currentUser.role] ?: currentUser.role}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text("🔑 الصلاحيات والتفويضات النشطة (Comma Separated):", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline, fontWeight = FontWeight.Bold)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            Text(
                                text = permissions,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(8.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = { viewModel.logout() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth().testTag("logout_button")
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("تسجيل الخروج الآمن (Log Out)", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text("لا يوجد مستخدم نشط حالياً.", fontSize = 13.sp)
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🔒 سياسات صلاحيات الدور النشط:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    val policies = when (currentRole) {
                        "Administrator" -> listOf(
                            "✅ كامل الصلاحيات المطلقة بالنسف والتعديل وقبول المدخلات.",
                            "✅ تفويض وتوقيع قرارات وتكليفات الإدارة.",
                            "✅ تعديل وتحصيل الموازنات والنفقات وسجلات المانحين.",
                            "✅ إبطال وثائق معايير الألغام وإدارة المعرفة وعزل الكوادر."
                        )
                        "Finance Manager" -> listOf(
                            "✅ التعديل والحذف الكامل بالمالية، المصروفات، السلف والعهد الميدانية.",
                            "✅ إرسال تنبيهات الموازنات وتوقع الصرف ومقارنة الدونر.",
                            "❌ لا يمتلك الإذن بتعديل سجل التكليفات العلوية أو الحضور البشري."
                        )
                        "Operations Manager" -> listOf(
                            "✅ تفويض مهام وقنوات المسح الميداني وسجل الذخائر والحوادث المكتشفة.",
                            "✅ إدارة كاشفات ومعدات الحقل وتحديث إحداثيات الخريطة الميدانية.",
                            "❌ محجوب عنه تفويض صرف السلف المالية المباشرة دون إيعاز المالية."
                        )
                        else -> listOf(
                            "✅ قراءة وسحب البيانات الملحقة والمتابعة الفنية الميدانية.",
                            "❌ يمنع تماماً تعديل أو حذف أي سجلات مالية أو عمليات أو حذف شركاء المركز إلا بطلب مكتوب ورسمي من المدير العام."
                        )
                    }

                    policies.forEach { policy ->
                        Text(text = policy, fontSize = 12.sp, modifier = Modifier.padding(vertical = 2.dp), lineHeight = 16.sp)
                    }
                }
            }
        }
    }
} else {
    Box(modifier = Modifier.weight(1f)) {
        ArchitecturalBlueprintDashboard()
    }
}
    }
}

@Composable
fun OfflineLoginView(viewModel: PmacViewModel) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showRegister by remember { mutableStateOf(false) }

    // Registration inputs
    var regUsername by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regFullName by remember { mutableStateOf("") }
    var regRole by remember { mutableStateOf("Field Supervisor") }
    var regPermissions by remember { mutableStateOf("read_reports,write_reports") }

    val rolesList = listOf("Administrator", "Finance Manager", "Operations Manager", "Field Supervisor")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .widthIn(max = 480.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(28.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Logo / Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (showRegister) "تسجيل مستخدم ميداني جديد" else "البوابة الأمنية الميدانية (Offline Identification)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = if (showRegister) "إدراج سجل مستخدم جديد محلياً في قاعدة بيانات Room" else "المرجع الفلسطيني للأعمال المتعلقة بالألغام ومكافحة المخاطر",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (!showRegister) {
                    // Username input
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("اسم المستخدم (Username)") },
                        modifier = Modifier.fillMaxWidth().testTag("username_input"),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Password input
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("كلمة المرور (Password)") },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().testTag("password_input"),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Seed accounts assist info
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "💡 الحسابات الافتراضية للتجربة (Offline Verification):",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("• المدير العام: admin / admin123", fontSize = 10.sp)
                            Text("• العمليات الميدانية: ops / ops123", fontSize = 10.sp)
                            Text("• الدائرة المالية: finance / finance123", fontSize = 10.sp)
                            Text("• مشرف الحقل الميداني: field / field123", fontSize = 10.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Login Button
                    Button(
                        onClick = {
                            if (username.isNotBlank() && password.isNotBlank()) {
                                viewModel.loginOffline(username, password)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("login_submit_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("تسجيل الدخول والتحقق الآمن", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(onClick = { showRegister = true }) {
                        Text("إنتاج حساب مستخدم ميداني جديد محلياً (Register)", fontSize = 12.sp)
                    }
                } else {
                    // Registration form
                    OutlinedTextField(
                        value = regUsername,
                        onValueChange = { regUsername = it },
                        label = { Text("اسم المستخدم (Username)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = regPassword,
                        onValueChange = { regPassword = it },
                        label = { Text("كلمة المرور") },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = regFullName,
                        onValueChange = { regFullName = it },
                        label = { Text("الاسم الكامل الميداني") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Role indicator
                    Text("اختر الموقف والوظيفة (Role):", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        rolesList.forEach { role ->
                            val isSelected = regRole == role
                            val textInArabic = when (role) {
                                "Administrator" -> "مدير عام"
                                "Operations Manager" -> "عمليات"
                                "Finance Manager" -> "مالية"
                                else -> "مشرف"
                            }
                            OutlinedButton(
                                onClick = {
                                    regRole = role
                                    regPermissions = when (role) {
                                        "Administrator" -> "all_permissions,create_report,delete_report,approve_finance"
                                        "Operations Manager" -> "read_reports,write_reports,create_report"
                                        "Finance Manager" -> "read_finance,write_finance,approve_finance"
                                        else -> "read_reports,write_reports"
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(textInArabic, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (regUsername.isNotBlank() && regPassword.isNotBlank() && regFullName.isNotBlank()) {
                                viewModel.registerUserOffline(regUsername, regPassword, regFullName, regRole, regPermissions)
                                showRegister = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("حفظ وتسجيل الموظف", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(onClick = { showRegister = false }) {
                        Text("العودة إلى تسجيل الدخول الميداني", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DocumentsView(viewModel: PmacViewModel, currentRole: String) {
    val context = LocalContext.current
    val documents by viewModel.documents.collectAsStateWithLifecycle()
    val documentVersions by viewModel.documentVersions.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("الكل") }

    // Dialog state for adding a document
    var showAddDialog by remember { mutableStateOf(false) }
    var newDocTitle by remember { mutableStateOf("") }
    var newDocCategory by remember { mutableStateOf("خرائط ميدانية") }
    var newDocFileType by remember { mutableStateOf("PDF") }
    var newDocKeywords by remember { mutableStateOf("") }
    var newDocRequiredRole by remember { mutableStateOf("Field Supervisor") }
    var newDocNotes by remember { mutableStateOf("") }

    // Dialog state for viewing versions (version control timeline)
    var showVersionsDoc by remember { mutableStateOf<PmacDocument?>(null) }
    var showNewVersionDialog by remember { mutableStateOf<PmacDocument?>(null) }
    
    // New Version fields
    var newVerTitle by remember { mutableStateOf("") }
    var newVerNotes by remember { mutableStateOf("") }

    // Access restricted warning state
    var restrictedDocAttempt by remember { mutableStateOf<PmacDocument?>(null) }

    val categoriesList = listOf("الكل", "خرائط ميدانية", "تقارير مالية", "معايير SOP", "اتفاقيات مانحين", "تقارير أخرى")
    
    // Filtering based on searches + categories
    val filteredDocs = documents.filter { doc ->
        val matchesCategory = selectedCategory == "الكل" || doc.category == selectedCategory
        val matchesQuery = searchQuery.isEmpty() || 
                doc.title.contains(searchQuery, ignoreCase = true) || 
                doc.keywords.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesQuery
    }

    // Role hierarchies
    val roleHierarchy = listOf(
        "Data Entry",
        "Field Supervisor",
        "Public Relations Officer",
        "HR Officer",
        "EORE Manager",
        "Operations Manager",
        "Finance Manager",
        "Executive Director",
        "Administrator"
    )

    fun hasRoleAccess(role: String, reqRole: String): Boolean {
        // Administrator always bypassed
        if (role == "Administrator") return true
        val userIndex = roleHierarchy.indexOf(role)
        val reqIndex = roleHierarchy.indexOf(reqRole)
        if (userIndex == -1 || reqIndex == -1) {
            return role == reqRole
        }
        return userIndex >= reqIndex
    }

    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Header Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "📁 صندوق الوثائق وإدارة المستندات للمركز (DMS)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "نظام أرشفة حوكمي مدمج بأحدث بروتوكولات الأمان والتحكم بالإصدارات لتتبع خرائط الذخيرة الخطرة وتقارير ميزانية المانحين.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            lineHeight = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("add_document_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إدراج مستند")
                    }
                }
            }

            // Search and Category Chips Row
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth().testTag("document_search_field"),
                        placeholder = { Text("بحث كامل في العنوان أو الكلمات المفتاحية...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "مسح")
                                }
                            }
                        },
                        singleLine = true
                    )

                    // Categories Scroll Row
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categoriesList.forEach { cat ->
                            val selected = selectedCategory == cat
                            FilterChip(
                                selected = selected,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat, fontSize = 12.sp) },
                                modifier = Modifier.testTag("cat_chip_$cat")
                            )
                        }
                    }
                }
            }

            // Documents List Scroll
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            
            if (filteredDocs.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "لا توجد مستندات مطابقة للبحث أو التصنيف الحالي.", color = MaterialTheme.colorScheme.outline, fontSize = 14.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredDocs) { doc ->
                        val accessible = hasRoleAccess(currentRole, doc.requiredRole)
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("doc_card_${doc.id}"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (accessible) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.05f)
                            ),
                            border = BorderStroke(
                                1.dp, 
                                if (accessible) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        // Colored File Type Avatar
                                        val bgTypeColor = when(doc.fileType.uppercase()) {
                                            "PDF" -> Color(0xFFFFEBEE)
                                            "DOCX" -> Color(0xFFE3F2FD)
                                            "PNG", "JPG" -> Color(0xFFEDE7F6)
                                            else -> Color(0xFFF5F5F5)
                                        }
                                        val textTypeColor = when(doc.fileType.uppercase()) {
                                            "PDF" -> Color(0xFFC62828)
                                            "DOCX" -> Color(0xFF1565C0)
                                            "PNG", "JPG" -> Color(0xFF651FFF)
                                            else -> Color(0xFF616161)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(bgTypeColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = doc.fileType,
                                                fontWeight = FontWeight.Bold,
                                                color = textTypeColor,
                                                fontSize = 11.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        
                                        Column {
                                            Text(
                                                text = doc.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (accessible) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = doc.category,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "إصدار: v${doc.currentVersion}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.secondary,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = "صلاحية: ${doc.requiredRole}",
                                                    fontSize = 11.sp,
                                                    color = if (accessible) Color(0xFF2E7D32) else Color(0xFFC62828),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                    
                                    // Lock icon if inaccessible
                                    if (!accessible) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "مغلق",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "متاح",
                                            tint = Color(0xFF4CAF50),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                
                                // Lower description and keywords Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "الكلمات الدالة: " + doc.keywords.split(",").joinToString(" | "),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "بواسطة: ${doc.uploadedBy}  |  تعديل: ${doc.uploadDate}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }

                                    // Action Buttons Row (Interactive Timeline & Revision Upload check)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        IconButton(
                                            onClick = {
                                                if (accessible) {
                                                    viewModel.setSelectedDocumentForHistory(doc.id)
                                                    showVersionsDoc = doc
                                                } else {
                                                    restrictedDocAttempt = doc
                                                }
                                            },
                                            modifier = Modifier.testTag("view_doc_history_btn_${doc.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.List, 
                                                contentDescription = "جدول الإصدارات",
                                                tint = if (accessible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                            )
                                        }

                                        if (accessible) {
                                            IconButton(
                                                onClick = { showNewVersionDialog = doc },
                                                modifier = Modifier.testTag("add_new_version_btn_${doc.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit, 
                                                    contentDescription = "شحن إصدار جديد",
                                                    tint = MaterialTheme.colorScheme.secondary
                                                )
                                            }

                                            IconButton(
                                                onClick = { viewModel.deleteDocument(doc) },
                                                modifier = Modifier.testTag("delete_doc_btn_${doc.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete, 
                                                    contentDescription = "حذف الوثيقة",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- ADD DIALOG FORM (CREATE NEW DOCUMENT AND V1) ---
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newDocTitle.isNotEmpty()) {
                                viewModel.addDocument(
                                    title = newDocTitle,
                                    category = newDocCategory,
                                    fileType = newDocFileType,
                                    keywords = newDocKeywords,
                                    uploadedBy = if (currentRole == "Guest Viewer" || currentRole == "Viewer") "الدائرة الإلكترونية" else currentRole,
                                    requiredRole = newDocRequiredRole,
                                    notes = newDocNotes
                                )
                                showAddDialog = false
                                newDocTitle = ""
                                newDocKeywords = ""
                                newDocNotes = ""
                                Toast.makeText(context, "تم إرسال وجدولة مستند جديد وحفظ إصدار رقم V1", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "الرجاء اكمال الحقول الأساسية وتسمية الملف", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.testTag("submit_doc_btn")
                    ) {
                        Text("موافق وحفظ")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showAddDialog = false }) {
                        Text("إلغاء")
                    }
                },
                title = { Text("📝 جدولة وإدراج مستند فني جديد للمركز") },
                text = {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        item {
                            OutlinedTextField(
                                value = newDocTitle,
                                onValueChange = { newDocTitle = it },
                                label = { Text("عنوان الوثيقة الفنية (مثلاً: محضر مؤازرة UN)") },
                                modifier = Modifier.fillMaxWidth().testTag("add_title_field")
                            )
                        }
                        item {
                            Text("قسم التصنيف المعتمد:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("خرائط ميدانية", "تقارير مالية", "معايير SOP", "اتفاقيات مانحين", "تقارير أخرى").forEach { item ->
                                    FilterChip(
                                        selected = newDocCategory == item,
                                        onClick = { newDocCategory = item },
                                        label = { Text(item, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                        item {
                            Text("امتداد الملف (النوع الإلكتروني):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                listOf("PDF", "DOCX", "PNG", "JPG").forEach { ext ->
                                    val isSel = newDocFileType == ext
                                    ElevatedCard(
                                        onClick = { newDocFileType = ext },
                                        colors = CardDefaults.elevatedCardColors(
                                            containerColor = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                        ),
                                        modifier = Modifier.padding(horizontal = 2.dp)
                                    ) {
                                        Box(modifier = Modifier.padding(8.dp)) {
                                            Text(ext, fontSize = 12.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            OutlinedTextField(
                                value = newDocKeywords,
                                onValueChange = { newDocKeywords = it },
                                label = { Text("الكلمات المفتاحية (مثال: غزة, UNMAS, لغم)") },
                                modifier = Modifier.fillMaxWidth().testTag("add_keywords_field")
                            )
                        }
                        item {
                            Text("دور صاحب القراءة الأدنى المسموح به (التحكم بالصلاحيات):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("Field Supervisor", "Finance Manager", "Operations Manager", "Public Relations Officer", "Executive Director", "Administrator").forEach { roleKey ->
                                    FilterChip(
                                        selected = newDocRequiredRole == roleKey,
                                        onClick = { newDocRequiredRole = roleKey },
                                        label = { Text(roleKey, fontSize = 10.sp) }
                                    )
                                }
                            }
                        }
                        item {
                            OutlinedTextField(
                                value = newDocNotes,
                                onValueChange = { newDocNotes = it },
                                label = { Text("ملاحظات المراجعة وتعديلات الإصدار الأول") },
                                modifier = Modifier.fillMaxWidth().testTag("add_notes_field")
                            )
                        }
                    }
                }
            )
        }

        // --- REVISIONS / TIMELINE DIALOG (VERSION CONTROL) ---
        if (showVersionsDoc != null) {
            val doc = showVersionsDoc!!
            AlertDialog(
                onDismissRequest = { 
                    showVersionsDoc = null
                    viewModel.setSelectedDocumentForHistory(null)
                },
                confirmButton = {
                    Button(onClick = { 
                        showVersionsDoc = null
                        viewModel.setSelectedDocumentForHistory(null)
                    }) {
                        Text("إغلاق السجل")
                    }
                },
                title = { 
                    Column {
                        Text("⏳ جدول أعمال المراجعة والتحكم بالإصدارات", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(doc.title, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, maxLines = 1)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Text("الخط الزمني (Timeline) لجميع نسخ المستند المسجلة:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        
                        if (documentVersions.isEmpty()) {
                            Text("حدث خطأ ما أو لا تتوفر أي تعديلات للملف حالياً.", color = MaterialTheme.colorScheme.outline)
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.heightIn(max = 280.dp).fillMaxWidth()
                            ) {
                                items(documentVersions) { ver ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "نسخة الإصدار v${ver.versionNumber} - ${ver.title}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(MaterialTheme.colorScheme.primary)
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text("مؤرشف", color = Color.White, fontSize = 9.sp)
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "ملاحظات التغيير: " + ver.updateNotes,
                                                fontSize = 11.sp,
                                                lineHeight = 15.sp,
                                                color = Color.Black.copy(alpha = 0.8f)
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(text = "التعديل: ${ver.modifiedDate}", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                                Text(text = "المدقق: ${ver.modifiedBy}", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }

        // --- ADD REVISION VERSION DIALOG ---
        if (showNewVersionDialog != null) {
            val doc = showNewVersionDialog!!
            AlertDialog(
                onDismissRequest = { showNewVersionDialog = null },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newVerTitle.isNotEmpty() && newVerNotes.isNotEmpty()) {
                                viewModel.uploadNewVersion(
                                    document = doc,
                                    versionTitle = newVerTitle,
                                    updateNotes = newVerNotes,
                                    modifiedBy = if (currentRole == "Guest Viewer" || currentRole == "Viewer") "الدائرة الإلكترونية" else currentRole
                                )
                                showNewVersionDialog = null
                                newVerTitle = ""
                                newVerNotes = ""
                                Toast.makeText(context, "تم رفع وحفظ المراجعة المكتوبة الجديدة v${doc.currentVersion + 1} بنجاح!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "الرجاء تحديد التبديل وتلخيص المراجعة", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.testTag("submit_new_version_btn")
                    ) {
                        Text("تثبيت المسودة الجديدة (V+)")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showNewVersionDialog = null }) {
                        Text("إلغاء")
                    }
                },
                title = { Text("📥 رفع مراجعة جديدة فنية (Version Upper)") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "أنت بصدد ترفيع وحفظ الملف: ${doc.title} من الإصدار v${doc.currentVersion} إلى الإصدار المحدث v${doc.currentVersion + 1}.",
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                        OutlinedTextField(
                            value = newVerTitle,
                            onValueChange = { newVerTitle = it },
                            label = { Text("مسمى المراجعة والتعديل (مثل: تعديلات الإحداثيات)") },
                            modifier = Modifier.fillMaxWidth().testTag("new_version_title_field")
                        )
                        OutlinedTextField(
                            value = newVerNotes,
                            onValueChange = { newVerNotes = it },
                            label = { Text("وصف دقيق للمصادقة وتعديلات بنود السند") },
                            modifier = Modifier.fillMaxWidth().testTag("new_version_notes_field")
                        )
                    }
                }
            )
        }

        // --- ACCESS SHIELD WARNING (RBAC POLICE) ---
        if (restrictedDocAttempt != null) {
            val doc = restrictedDocAttempt!!
            AlertDialog(
                onDismissRequest = { restrictedDocAttempt = null },
                confirmButton = {
                    Button(onClick = { restrictedDocAttempt = null }) {
                        Text("عودة")
                    }
                },
                title = { Text("🚨 تنبيه: محجوب لدواعي سرية وأمنية") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "لا تملك الصلاحية الكافية لقراءة أو تبديل بيانات هذا المستند الإداري الحرج.",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "تفاصيل الملف المحظور:\n• العنوان: ${doc.title}\n• الصلاحية المستهدفة: ${doc.requiredRole} فما فوق.\n• دور حسابك الحالي: $currentRole",
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = Color.Black.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "حوكمة البيانات بفلسطين تلزم حظر الوصول الميداني العشوائي لمعالجة الصواعق وتتبع تمويل الموازنات إلا لمن يتمتع بتفويض مباشر من الإدارة العليا. توجه لقسم 'الصلاحيات' بالجانب لتعديل صلاحياتك إن دعت الحاجة القانونية.",
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AssetsView(viewModel: PmacViewModel, currentRole: String) {
    val assets by viewModel.filteredAssets.collectAsStateWithLifecycle(emptyList())
    val conditionFilter by viewModel.assetConditionFilter.collectAsStateWithLifecycle("")
    val searchQuery by viewModel.assetSearchQuery.collectAsStateWithLifecycle("")

    var showAddDialog by remember { mutableStateOf(false) }
    var assetName by remember { mutableStateOf("") }
    var assetSerial by remember { mutableStateOf("") }
    var assetType by remember { mutableStateOf("كاشف ألغام (Metal Detector)") }
    var assetCondition by remember { mutableStateOf("ممتازة") }
    var assetAssigned by remember { mutableStateOf("فريق مسح أ") }
    var assetLocation by remember { mutableStateOf("مستودع جنين الرئيسي") }
    var assetSupplier by remember { mutableStateOf("MineLab Australia") }

    var editAssetConditionFor by remember { mutableStateOf<AssetItem?>(null) }

    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Header
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "📦 وحدة الأصول والعهد اللوجستية الميدانية",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "تتبع وحصر أجهزة نزع الألغام (Mine Detector)، المركبات والمعدات مع سجل الصيانة الفورية (Offline Vault).",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            // Statistics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val total = assets.size
                val excellent = assets.count { it.condition == "ممتازة" }
                val maintenance = assets.count { it.condition == "تحت الصيانة" }

                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("مجموع العهد", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                        Text("$total أصول", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("حالة ممتازة", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                        Text("$excellent أجهزة", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF2E7D32))
                    }
                }
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("قيد الصيانة", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                        Text("$maintenance أصول", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFEF6C00))
                    }
                }
            }

            // Filters Box
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateAssetSearchQuery(it) },
                    placeholder = { Text("بحث عن أصل، رقم تسلسلي، فريق...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Button(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("أصل جديد", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Condition filter chips row
            val conditionsList = listOf("الكل", "ممتازة", "تحت الصيانة", "تالفة", "مفقودة في الميدان")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                conditionsList.forEach { cond ->
                    val filterVal = if (cond == "الكل") "" else cond
                    val isSelected = conditionFilter == filterVal
                    OutlinedButton(
                        onClick = { viewModel.updateAssetConditionFilter(filterVal) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(cond, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }

            // List
            if (assets.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                            Text("لا توجد أصول مطابقة لخيارات الفلترة الحالية.", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    items(assets.size) { index ->
                        val asset = assets[index]
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(asset.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("رقم تسلسلي موحد: ${asset.serialNumber}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                    }
                                    
                                    val (badgeBg, badgeText) = when (asset.condition) {
                                        "ممتازة" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
                                        "تحت الصيانة" -> Color(0xFFFFF3E0) to Color(0xFFE65100)
                                        "تالفة" -> Color(0xFFFFEBEE) to Color(0xFFC62828)
                                        else -> Color(0xFFEDE7F6) to Color(0xFF5E35B1)
                                    }
                                    Surface(
                                        color = badgeBg,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            asset.condition,
                                            color = badgeText,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("النوع والجهاز:", fontSize = 10.sp, color = Color.Gray)
                                        Text(asset.type, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("الجهة المستفيدة / العهدة:", fontSize = 10.sp, color = Color.Gray)
                                        Text(asset.assignedTo, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("الموقع والمخزن والمورد:", fontSize = 10.sp, color = Color.Gray)
                                        Text("${asset.storeLocation} | ${asset.supplier}", fontSize = 11.sp)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("تاريخ التوريد المعتمد:", fontSize = 10.sp, color = Color.Gray)
                                        Text(asset.purchaseDate, fontSize = 11.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "تحديث الصيانة الوقائية مؤخراً: ${asset.lastServiceDate}",
                                        fontSize = 10.sp,
                                        color = Color.Gray,
                                        modifier = Modifier.weight(1f)
                                    )

                                    OutlinedButton(
                                        onClick = { editAssetConditionFor = asset },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(30.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("تبديل الحالة الميدانية", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // New Asset dialog
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("📝 تسجيل وتوريد أصل فني جديد وقيده") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.registerAsset(assetName, assetSerial, assetType, assetCondition, assetAssigned, assetLocation, assetSupplier)
                            showAddDialog = false
                            assetName = ""
                            assetSerial = ""
                        }
                    ) {
                        Text("إدراج بسلسلة التوريد")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("إلغاء")
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = assetName,
                            onValueChange = { assetName = it },
                            label = { Text("اسم الأصل (مثال: MineLab F3)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = assetSerial,
                            onValueChange = { assetSerial = it },
                            label = { Text("رقم تسلسلي موحد فريد (Serial)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = assetType,
                            onValueChange = { assetType = it },
                            label = { Text("تصنيف الجهاز/المركبة") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = assetAssigned,
                            onValueChange = { assetAssigned = it },
                            label = { Text("الفرقة المستلمة / العهدة") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = assetLocation,
                            onValueChange = { assetLocation = it },
                            label = { Text("الموقع الجغرافي / المستودع") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = assetSupplier,
                            onValueChange = { assetSupplier = it },
                            label = { Text("الشركة الموردة والمنشأ") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            )
        }

        // Edit Asset Condition Dialog
        editAssetConditionFor?.let { asset ->
            AlertDialog(
                onDismissRequest = { editAssetConditionFor = null },
                title = { Text("⚙️ تبديل الحالة التشغيلية للمعدات") },
                confirmButton = {
                    Button(onClick = { editAssetConditionFor = null }) {
                        Text("إغلاق")
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("تبديل الحالة للعهدة: ${asset.name} (SN: ${asset.serialNumber})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        val states = listOf("ممتازة", "تحت الصيانة", "تالفة", "مفقودة في الميدان")
                        states.forEach { st ->
                            val isCurrent = asset.condition == st
                            OutlinedButton(
                                onClick = {
                                    viewModel.updateAssetCondition(asset.id, st)
                                    editAssetConditionFor = null
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(st, fontWeight = FontWeight.Bold, color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else Color.Black)
                            }
                        }
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AuditLogsView(viewModel: PmacViewModel, currentRole: String) {
    val auditLogs by viewModel.filteredAuditLogs.collectAsStateWithLifecycle(emptyList())
    val moduleFilter by viewModel.auditModuleFilter.collectAsStateWithLifecycle("")
    val searchQuery by viewModel.auditSearchQuery.collectAsStateWithLifecycle("")

    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Header
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "🔒 مركز تدقيق الحسابات والامتثال الأمني الموحد (Audit Trails)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        "السجل غير القابل للتعديل المنشأ برعاية نظام إدارة الهوية والمزامنة لـ PMAC لتوثيق العمليات أوفلاين.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            // Stat Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.weight(1.5f)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("إجمالي العمليات المسجلة", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                        Text("${auditLogs.size} حركات تدقيق", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("نسبة السلامة الفنية", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                        Text("100% متطابق", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF2E7D32))
                    }
                }
            }

            // Search Filter
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateAuditSearchQuery(it) },
                placeholder = { Text("بحث بالعملية، التفصيل، الموظف المشرف...", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Category filters - Modules
            val moduleCategories = listOf("الكل", "الصلاحيات", "العمليات", "المالية", "الأصول والخدمات اللوجستية")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                moduleCategories.forEach { cat ->
                    val filterVal = if (cat == "الكل") "" else cat
                    val isSelected = moduleFilter == filterVal
                    OutlinedButton(
                        onClick = { viewModel.updateAuditModuleFilter(filterVal) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(cat, fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }

            // Timber-line of logs
            if (auditLogs.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("لا توجد أحداث ومحاولات مسجلة للخيارات الحالية.", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    items(auditLogs.size) { index ->
                        val log = auditLogs[index]
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                log.systemModule,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        Text(log.action, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }

                                    val isSuccess = log.status.contains("success", true) || log.status.contains("نجاح", true)
                                    Text(
                                        text = log.status,
                                        color = if (isSuccess) Color(0xFF2E7D32) else Color(0xFFC62828),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                
                                Text(
                                    log.details,
                                    fontSize = 12.sp,
                                    color = Color.DarkGray,
                                    lineHeight = 18.sp
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                                        Text(
                                            "المسؤول: ${log.fullName} (${log.role})",
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )
                                    }

                                    Text(
                                        log.timestamp,
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ArchitecturalBlueprintDashboard() {
    var showDdlConsole by remember { mutableStateOf(false) }
    var selectSchemaTable by remember { mutableStateOf("users") }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "📄 لوحة قيادة وتوثيق الهيكل المعماري (Enterprise Core Blueprint)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "توثيق مخطط النظام الشامل لـ PMAC لربط خوادم الويب وقاعدة بيانات PostgreSQL المركزية عبر Drizzle ORM.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Section 1: Database ORM Models
        item {
            Text("🗄️ 1. نماذج مخطط قواعد البيانات (PostgreSQL / Drizzle ORM Model)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("اختر الجدول لقراءة نموذج هيكل Drizzle الخاص به:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val tables = listOf("users", "assets", "audit_logs", "operation_reports", "finance_records")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        tables.forEach { tb ->
                            val isSelected = selectSchemaTable == tb
                            OutlinedButton(
                                onClick = { selectSchemaTable = tb },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(tb, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val modelContent = when (selectSchemaTable) {
                        "users" -> """
                            // Schema definition for 'users'
                            export const users = pgTable('users', {
                              id: serial('id').primaryKey(),
                              username: varchar('username', { length: 50 }).notNull().unique(),
                              passwordHash: varchar('password_hash', { length: 255 }).notNull(),
                              fullName: varchar('full_name', { length: 150 }).notNull(),
                              role: roleEnum('role').default('Field Supervisor').notNull(),
                              permissions: text('permissions').notNull()
                            });
                        """.trimIndent()
                        "assets" -> """
                            // Schema definition for 'assets' (Logistics & Inventory)
                            export const assets = pgTable('assets', {
                              id: serial('id').primaryKey(),
                              name: varchar('name', { length: 200 }).notNull(),
                              serialNumber: varchar('serial_number', { length: 100 }).notNull().unique(),
                              type: varchar('type', { length: 100 }).notNull(),
                              condition: assetConditionEnum('condition').default('ممتازة').notNull(),
                              assignedTo: varchar('assigned_to', { length: 150 }).notNull(),
                              storeLocation: varchar('store_location', { length: 150 }).notNull(),
                              purchaseDate: timestamp('purchase_date').notNull(),
                              lastServiceDate: timestamp('last_service_date').defaultNow().notNull(),
                              supplier: varchar('supplier', { length: 150 })
                            });
                        """.trimIndent()
                        "audit_logs" -> """
                            // Schema definition for 'audit_logs'
                            export const auditLogs = pgTable('audit_logs', {
                              id: serial('id').primaryKey(),
                              username: varchar('username', { length: 50 }).notNull(),
                              fullName: varchar('full_name', { length: 150 }).notNull(),
                              role: varchar('role', { length: 50 }).notNull(),
                              action: varchar('action', { length: 200 }).notNull(),
                              details: text('details').notNull(),
                              timestamp: timestamp('timestamp').defaultNow().notNull(),
                              systemModule: varchar('system_module', { length: 100 }).notNull()
                            });
                        """.trimIndent()
                        "operation_reports" -> """
                            // Schema definition for 'operation_reports'
                            export const operationReports = pgTable('operation_reports', {
                              id: serial('id').primaryKey(),
                              title: varchar('title', { length: 255 }).notNull(),
                              type: varchar('type', { length: 100 }).notNull(),
                              fieldTeam: varchar('field_team', { length: 100 }).notNull(),
                              area: varchar('area', { length: 100 }).notNull(),
                              date: timestamp('date').notNull(),
                              status: varchar('status', { length: 50 }).notNull(),
                              casualties: integer('casualties').default(0),
                              riskScore: integer('risk_score').default(1),
                              aiSummary: text('ai_summary')
                            });
                        """.trimIndent()
                        else -> """
                            // Schema definition for 'finance_records'
                            export const financeRecords = pgTable('finance_records', {
                              id: serial('id').primaryKey(),
                              title: varchar('title', { length: 255 }).notNull(),
                              type: varchar('type', { length: 100 }).notNull(),
                              project: varchar('project', { length: 150 }).notNull(),
                              amount: doublePrecision('amount').notNull(),
                              category: varchar('category', { length: 100 }).notNull(),
                              donor: varchar('donor', { length: 150 }).notNull(),
                              isSynced: boolean('is_synced').default(true)
                            });
                        """.trimIndent()
                    }

                    // Monospace Box
                    Surface(
                        color = Color(0xFFF5F5F5),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            modelContent,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { showDdlConsole = true },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("توليد نص DDL الشامل لـ PostgreSQL", fontSize = 11.sp)
                    }
                }
            }
        }

        // Section 2: REST Endpoints
        item {
            Text("🔗 2. هيكلية منافذ الاتصال والموديول (REST API Endpoints Structure)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val apis = listOf(
                        Triple("POST", "/api/v1/assets", "تسجيل عهد ميداني جديد. (متاح لـ: Administrator, Operations Manager)"),
                        Triple("GET", "/api/v1/assets", "استعلام وبحث الأصول والعهد الفعالة صيانتها. (متاح للجميع)"),
                        Triple("PATCH", "/api/v1/assets/:id/condition", "تحديث الحالة التشغيلية من الميدان. (متاح لـ: Supervisors, Managers)"),
                        Triple("GET", "/api/v1/audit-logs", "مراجعة سلسلة حركات التدقيق وبلاغات التعديل. (متاح لـ: Admin, Exec-Director)"),
                        Triple("POST", "/api/v1/auth/login", "تسجيل الدخول وإصدار توثيق التشفير والتحقق JWT Token.")
                    )

                    apis.forEach { (method, route, desc) ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val methodColor = when (method) {
                                "POST" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
                                "PATCH" -> Color(0xFFFFF3E0) to Color(0xFFE65100)
                                else -> Color(0xFFE3F2FD) to Color(0xFF1565C0)
                            }
                            Surface(color = methodColor.first, shape = RoundedCornerShape(4.dp)) {
                                Text(
                                    method,
                                    color = methodColor.second,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(route, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black)
                                Text(desc, fontSize = 11.sp, color = Color.Gray, lineHeight = 16.sp)
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }
            }
        }

        // Section 3: RBAC Grid
        item {
            Text("🛡️ 3. لوحة مصفوفة الصلاحيات وقواعد التحقق (RBAC Rule Engine)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("الصلاحيات الفنية الصارمة للأدوار الميدانية النشطة بقاعدة البيانات:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))

                    val rbacRules = listOf(
                        "Administrator" to "جميع الصلاحيات (all_permissions)، صيانة الخوادم وإشراف تام لكافة الأبواب والسجلات.",
                        "Executive Director" to "قراءة وكتابة وتوريد القرارات الإدارية، مراقبة التدقيق والحسابات والتقارير المالية والشركاء.",
                        "Operations Manager" to "إصدار وإدارة الخطط والتقارير الميدانية والمخازن اللوجستية، الفحص الميداني، وجرد الأسلحة والأصول.",
                        "Finance Manager" to "إثبات المقبوضات وصرف النفقات الميدانية والموازنة، صياغة المانحين والشركاء.",
                        "Field Supervisor" to "تسجيل حالات حظر الألغام، تحديث سلامة العهد، تنفيذ بروتوكولات SOP وحملات التوعية."
                    )

                    rbacRules.forEach { (role, rule) ->
                        Column {
                            Text(role, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            Text(rule, fontSize = 11.sp, color = Color.DarkGray, lineHeight = 16.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }
    }

    if (showDdlConsole) {
        AlertDialog(
            onDismissRequest = { showDdlConsole = false },
            title = { Text("💾 نصوص DDL المخصصة لمحرك PostgreSQL") },
            confirmButton = {
                Button(onClick = { showDdlConsole = false }) {
                    Text("أغلق الكونسول")
                }
            },
            text = {
                val ddlString = """
                    -- Dynamic PostgreSQL DDL Output Compiled under Drizzle Schema
                    CREATE TYPE user_role AS ENUM (
                      'Administrator',
                      'Executive Director',
                      'Finance Manager',
                      'Operations Manager',
                      'EORE Manager',
                      'Public Relations Officer',
                      'HR Officer',
                      'Field Supervisor',
                      'Data Entry'
                    );

                    CREATE TYPE asset_condition AS ENUM (
                      'ممتازة',
                      'تحت الصيانة',
                      'تالفة',
                      'مفقودة في الميدان'
                    );

                    CREATE TABLE users (
                      id SERIAL PRIMARY KEY,
                      username VARCHAR(50) NOT NULL UNIQUE,
                      password_hash VARCHAR(255) NOT NULL,
                      full_name VARCHAR(150) NOT NULL,
                      role user_role DEFAULT 'Field Supervisor' NOT NULL,
                      permissions TEXT NOT NULL
                    );

                    CREATE TABLE assets (
                      id SERIAL PRIMARY KEY,
                      name VARCHAR(200) NOT NULL,
                      serial_number VARCHAR(100) NOT NULL UNIQUE,
                      type VARCHAR(100) NOT NULL,
                      condition asset_condition DEFAULT 'ممتازة' NOT NULL,
                      assigned_to VARCHAR(150) NOT NULL,
                      store_location VARCHAR(150) NOT NULL,
                      purchase_date TIMESTAMP NOT NULL DEFAULT NOW(),
                      last_service_date TIMESTAMP NOT NULL DEFAULT NOW(),
                      supplier VARCHAR(150)
                    );

                    CREATE TABLE audit_logs (
                      id SERIAL PRIMARY KEY,
                      userId INT REFERENCES users(id),
                      username VARCHAR(50) NOT NULL,
                      full_name VARCHAR(150) NOT NULL,
                      role VARCHAR(50) NOT NULL,
                      action VARCHAR(200) NOT NULL,
                      details TEXT NOT NULL,
                      timestamp TIMESTAMP NOT NULL DEFAULT NOW(),
                      system_module VARCHAR(100) NOT NULL,
                      status VARCHAR(50) DEFAULT 'نجاح (Success)' NOT NULL
                    );
                """.trimIndent()

                Surface(
                    color = Color.Black,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        ddlString,
                        color = Color(0xFF00FF00),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        )
    }
}

// ======================== SECURITY ROUTE MIDDLEWARE (RBAC) ========================

/**
 * Checks if the specified simulated role has route-level permission to access the section.
 */
fun checkRouteAccess(role: String, section: String): Boolean {
    // Administrator can access everything
    if (role == "Administrator") return true
    
    return when (section) {
        "Dashboard", "AI Assistant", "Settings", "Documents" -> true
        
        "Operations" -> role in listOf("Operations Manager", "Field Supervisor", "Executive Director")
        "Finance" -> role in listOf("Finance Manager", "Executive Director")
        "Administration" -> role in listOf("Executive Director")
        "Relations" -> role in listOf("Public Relations Officer", "Executive Director")
        "HR" -> role in listOf("HR Officer", "Executive Director")
        "Knowledge" -> role in listOf("EORE Manager", "Operations Manager", "Field Supervisor", "Executive Director")
        "Assets" -> role in listOf("Administrator", "Operations Manager", "Executive Director", "Finance Manager", "Field Supervisor")
        "Audit" -> role in listOf("Administrator", "Executive Director")
        
        else -> true
    }
}

/**
 * An exceptionally polished, secure material 3 screen shown when a route is blocked by the RBAC middleware.
 */
@Composable
fun AccessDeniedView(
    currentRole: String,
    attemptedSection: String,
    onGoToDashboard: () -> Unit,
    onGoToSettings: () -> Unit
) {
    val sectionNamesInArabic = mapOf(
        "Dashboard" to "لوحة المؤشرات العامة",
        "Operations" to "العمليات وحوادث الألغام",
        "Finance" to "الدائرة المالية والموازنة",
        "Administration" to "القرارات وتكليفات الإدارة",
        "Relations" to "العلاقات العامة والشركاء",
        "HR" to "الموارد البشرية والحضور",
        "Knowledge" to "مكتبة معايير الألغام SOP",
        "Documents" to "إدارة المستندات والوثائق",
        "AI Assistant" to "المساعد الذكي لبحث البيانات",
        "Settings" to "الصلاحيات وإعدادات النظام"
    )

    val sectionAuthorizedRoles = mapOf(
        "Operations" to listOf("Operations Manager", "Field Supervisor", "Executive Director", "Administrator"),
        "Finance" to listOf("Finance Manager", "Executive Director", "Administrator"),
        "Administration" to listOf("Executive Director", "Administrator"),
        "Relations" to listOf("Public Relations Officer", "Executive Director", "Administrator"),
        "HR" to listOf("HR Officer", "Executive Director", "Administrator"),
        "Knowledge" to listOf("EORE Manager", "Operations Manager", "Field Supervisor", "Executive Director", "Administrator")
    )

    val rolesInArabicMap = mapOf(
        "Administrator" to "المدير العام والمسؤول التقني (Administrator)",
        "Executive Director" to "المدير التنفيذي (Executive Director)",
        "Finance Manager" to "رئيس الدائرة المالية (Finance Manager)",
        "Operations Manager" to "مدير العمليات الفنية (Operations Manager)",
        "EORE Manager" to "منسق برامج التوعية والتثقيف (EORE Manager)",
        "Public Relations Officer" to "مسؤول العلاقات العامة (PR Officer)",
        "HR Officer" to "مسؤول الموارد البشرية (HR Officer)",
        "Field Supervisor" to "مشرف الفحص الميداني (Field Supervisor)",
        "Data Entry" to "مدخل بيانات السجلات (Data Entry)"
    )

    val arabicSectionName = sectionNamesInArabic[attemptedSection] ?: attemptedSection
    val arabicCurrentRole = rolesInArabicMap[currentRole] ?: currentRole
    val allowedRoles = sectionAuthorizedRoles[attemptedSection] ?: emptyList()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.08f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 500.dp)
                .testTag("access_denied_card"),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Large secure shield / lock icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "محظور أمنياً",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Text(
                    text = "🚨 حاجز الأمان: المسار غير مصرح",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "حوكمة البيانات بفلسطين ومركز PMAC تمنع حسابك الحالي من الدخول الفوري إلى هذا القسم تفادياً للثغرات الحقلية والتقريرية الميدانية.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Metadata details
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "🛣️ القسم المستهدف:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                        Text(text = arabicSectionName, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.error)
                    }

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "👤 دور حسابك الحالي:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                        Text(text = arabicCurrentRole, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }

                    if (allowedRoles.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "🔑 تصريح العبور الممنوح للأدوار التالية فقط:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            allowedRoles.forEach { roleId ->
                                val arabicRole = rolesInArabicMap[roleId] ?: roleId
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = arabicRole, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Dual actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onGoToDashboard,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("access_denied_dashboard_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Home, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("الرئيسية", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onGoToSettings,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("access_denied_settings_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("لوحة الصلاحيات", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun GlobalNetworkIndicator(viewModel: PmacViewModel) {
    val context = LocalContext.current
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val isDeviceOnline by viewModel.isDeviceOnline.collectAsStateWithLifecycle()
    val pendingSyncs by viewModel.pendingSyncCount.collectAsStateWithLifecycle()
    val isSyncing by viewModel.syncing.collectAsStateWithLifecycle()

    var isExpanded by remember { mutableStateOf(false) }

    // Logic to determine overall effective connection
    // Standard web browsers look at navigator.onLine (equivalent to isDeviceOnline)
    // The PMAC app allows simulating offline toggle on top of actual device state.
    val effectiveOnline = isOnline && isDeviceOnline

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("global_network_status_banner")
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSyncing -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                !effectiveOnline -> Color(0xFFFFF9C4) // Beautiful warning light amber
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) // Standard clean state
            }
        ),
        border = BorderStroke(
            width = 1.dp,
            color = when {
                isSyncing -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                !effectiveOnline -> Color(0xFFFBC02D).copy(alpha = 0.6f)
                else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .clickable { isExpanded = !isExpanded }
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Left dynamic icon based on status
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isSyncing -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    !effectiveOnline -> Color(0xFFFFF59D)
                                    else -> Color(0xFFC8E6C9)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = when {
                                    !isDeviceOnline -> Icons.Default.CloudOff
                                    !isOnline -> Icons.Default.WifiOff
                                    else -> Icons.Default.CloudQueue
                                },
                                contentDescription = null,
                                tint = when {
                                    !effectiveOnline -> Color(0xFF8D6E63)
                                    else -> Color(0xFF2E7D32)
                                },
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    // Main title showing browser connectivity simulator state
                    Column {
                        Text(
                            text = when {
                                isSyncing -> "⏳ جاري مزامنة بيانات المركز الفورية..."
                                !isDeviceOnline -> "🌐 انقطع اتصال الشبكة (Navigator Offline)"
                                !isOnline -> "📴 وضع العمل دون اتصال النشط (Forced Offline)"
                                else -> "☁️ اتصال آمن ومستمر بخوادم PMAC السحابية"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isSyncing -> MaterialTheme.colorScheme.primary
                                !effectiveOnline -> Color(0xFF5D4037)
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                        
                        // Small status label
                        Text(
                            text = when {
                                !isDeviceOnline -> "استشعار المتصفح (navigator.onLine = false): شبكة الجهاز الحقيقية مقطوعة."
                                !isOnline -> "التطبيق يحمي السجلات ويحفظ التعديلات محلياً في قاعدة بيانات Room."
                                isSyncing -> "تفريغ وتوليد حوكمة السندات والقرارات على السحابة المركزية."
                                else -> "كامل مؤشرات الألغام والذخائر خطرة متزامنة مع السلطة."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                }

                // Synchronization summary or actions
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (pendingSyncs > 0) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.testTag("network_sync_status_text")
                        ) {
                            Text(text = "$pendingSyncs تعديل محلي", fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp))
                        }
                    } else if (effectiveOnline) {
                        Badge(
                            containerColor = Color(0xFFE8F5E9),
                            contentColor = Color(0xFF2E7D32)
                        ) {
                            Text("مؤرشف بالكامل", fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp))
                        }
                    }

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "تفاصيل",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Expanded details block
            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "حوكمة الأوفلاين وإمكانية العمل دون اتصال:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "يدعم نظام PMAC أسلوب الـ (Offline-First) المتكامل. " +
                           "عند انقطاع شبكة الجهاز بالكامل (استشعار navigator.onLine) أو تغيير وضع الاتصال يدوياً، " +
                           "يقوم محرّك البيانات بتوجيه جميع عمليات الإضافة، التعديل، والحذف في مجالات المالية والعمليات والقرارات مباشرة إلى قاعدة بيانات Room المحلية والآمنة. " +
                           "فور استعادة الاتصال واستشعار الشبكة، تظهر المزامنة التلقائية ويتاح ترحيل النسخ فوراً.",
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Detail specifications
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ElevatedCard(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("شبكة المتصفح", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                            Text(
                                text = if (isDeviceOnline) "متاح (Online)" else "معطل (Offline)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = if (isDeviceOnline) Color(0xFF2E7D32) else Color(0xFFC62828),
                                modifier = Modifier.testTag("network_device_status_text")
                            )
                        }
                    }

                    ElevatedCard(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("محاكي PMAC", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                            Text(
                                text = if (isOnline) "نشط (Cloud Enabled)" else "مطفأ (Local Mode)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = if (isOnline) Color(0xFF2E7D32) else Color(0xFF8D6E63)
                            )
                        }
                    }

                    ElevatedCard(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("قاعدة البيانات", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                            Text(
                                text = "SQLite Room Active",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action area
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "يمكنك النقر على الزر للمحاكاة أو المزامنة:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Toggle simulate offline
                        OutlinedButton(
                            onClick = { 
                                viewModel.toggleOnlineMode()
                                val msg = if (isOnline) "تم تفعيل محاكي الأوفلاين" else "تم الاتصال بسيرفر المزامنة"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.height(36.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                imageVector = if (isOnline) Icons.Default.WifiOff else Icons.Default.Wifi,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isOnline) "قطع الاتصال" else "تفعيل الاتصال", fontSize = 10.sp)
                        }

                        // Force sync if we have pending changes and connection is available
                        Button(
                            onClick = {
                                if (!isOnline || !isDeviceOnline) {
                                    Toast.makeText(context, "يرجى تشغيل الشبكة أولاً بالضغط على قطع / تفعيل للاتصال بالسيرفر السحابي!", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.triggerManualSync()
                                    Toast.makeText(context, "بدء مزامنة السجلات الفورية مع الحوكمة المركزية...", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = pendingSyncs > 0 && isOnline && isDeviceOnline && !isSyncing,
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("manual_sync_trigger_button"),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("مزامنة الآن", fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun QuickGuideView(viewModel: PmacViewModel) {
    val protocols by viewModel.searchedEmergencyProtocols.collectAsStateWithLifecycle()
    val searchQuery by viewModel.protocolSearchQuery.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var activeCallNo by remember { mutableStateOf<String?>(null) }

    var newTitle by remember { mutableStateOf("") }
    var newPriority by remember { mutableStateOf("🔴 حرجة جداً") }
    var newSteps by remember { mutableStateOf("") }
    var newContactNo by remember { mutableStateOf("") }

    val priorities = listOf("🔴 حرجة جداً", "🟠 أولوية مرتفعة", "🟡 إرشاد عام")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "🚑 دليل بروتوكولات الطوارئ والسلامة الفورية",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "مرجع فلسطيني آمن محلي ومؤرشف بقاعدة بيانات Room للعمل الذاتي الكامل (Offline Index)",
                            color = MaterialTheme.colorScheme.outline,
                            fontSize = 11.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "دليل سلامة معتمد للتعامل المباشر والسريع مع الحوادث الميدانية أو الأجسام المشبوهة والاتصال بفرق الدفاع المدني والهلال الأحمر في كافة المحافظات.",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                )
            }
        }

        // Actions & Search Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateProtocolSearchQuery(it) },
                placeholder = { Text("بحث محلي فوري بالبروتوكولات...", fontSize = 12.sp) },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(max = 56.dp)
                    .testTag("protocol_search_input"),
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { viewModel.updateProtocolSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear Search",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                } else null,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )

            // Add Protocol Button
            Button(
                onClick = { showAddDialog = true },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .height(52.dp)
                    .testTag("add_protocol_btn"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("إضافة", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        // Protocols list
        if (protocols.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "لا توجد بروتوكولات مطابقة للبحث أو مؤرشفة حالياً.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(protocols) { protocol ->
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("protocol_item_card"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column {
                            // Top edge with gold accent line representing visual identity
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = protocol.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    
                                    Spacer(modifier = Modifier.width(8.dp))
                                    
                                    // Priority Badge
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text(protocol.priority, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(10.dp))
                                
                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
                                
                                Spacer(modifier = Modifier.height(10.dp))
                                
                                // Steps details
                                Text(
                                    text = "خطوات الاستجابة الفورية:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = protocol.steps,
                                    fontSize = 12.sp,
                                    lineHeight = 20.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                // Emergency Contact Clickable Card
                                Card(
                                    onClick = { activeCallNo = protocol.contactNo },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Phone,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "جهة التواصل للطوارئ (انقر للطلب):",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                        Text(
                                            text = protocol.contactNo,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Text(
                                        text = "آخر تحديث محلي: ${protocol.lastUpdated}",
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialling SIMULATION Dialogue
    if (activeCallNo != null) {
        AlertDialog(
            onDismissRequest = { activeCallNo = null },
            confirmButton = {
                TextButton(
                    onClick = { activeCallNo = null },
                    modifier = Modifier.testTag("end_sim_call_btn")
                ) {
                    Text("إنهاء الاتصال الطارئ", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("قناة اتصال طوارئ لاسلكي")
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "جاري إجراء اتصال طارئ بقناة العمليات وإدارة المتفجرات:",
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = activeCallNo ?: "",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "الاتصال الميداني آمن ومشفر بالكامل محلياً",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        )
    }

    // Add Protocol Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotBlank() && newSteps.isNotBlank()) {
                            viewModel.addEmergencyProtocol(
                                title = newTitle,
                                priority = newPriority,
                                steps = newSteps,
                                contactNo = if (newContactNo.isBlank()) "عمليات الطوارئ والشرطة المشتركة" else newContactNo
                            )
                            showAddDialog = false
                        }
                    },
                    modifier = Modifier.testTag("submit_protocol_btn")
                ) {
                    Text("حفظ محلياً في الروم")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("إلغاء")
                }
            },
            title = { Text("إدراج بروتوكول طوارئ أوفلاين جديد") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "سيتم حفظ البيانات مباشرة وتأريشها في محرك البحث أوفلاين كمرجع ثابت.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("أدخل عنوان وعنصر الطارئ") },
                        modifier = Modifier.fillMaxWidth().testTag("new_protocol_title_input"),
                        singleLine = true
                    )
                    
                    // Priority selection
                    Column {
                        Text("مستوى الأهمية والخطورة:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            priorities.forEach { prio ->
                                val selected = newPriority == prio
                                FilterChip(
                                    selected = selected,
                                    onClick = { newPriority = prio },
                                    label = { Text(prio, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                                )
                            }
                        }
                    }
                    
                    OutlinedTextField(
                        value = newSteps,
                        onValueChange = { newSteps = it },
                        label = { Text("تفاصيل وإجراءات التجنب والإنقاذ (مرقمة)") },
                        modifier = Modifier.fillMaxWidth().height(120.dp).testTag("new_protocol_steps_input"),
                        maxLines = 6
                    )
                    
                    OutlinedTextField(
                        value = newContactNo,
                        onValueChange = { newContactNo = it },
                        label = { Text("قنوات الاتصال الميدانية السريعة") },
                        modifier = Modifier.fillMaxWidth().testTag("new_protocol_contact_input"),
                        singleLine = true
                    )
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PouchSyncDashboardView(viewModel: PmacViewModel) {
    // Collect Room Live Tables Data
    val decisions by viewModel.decisions.collectAsStateWithLifecycle()
    val financeRecords by viewModel.financeRecords.collectAsStateWithLifecycle()
    val operations by viewModel.operations.collectAsStateWithLifecycle()
    val partners by viewModel.partners.collectAsStateWithLifecycle()
    val employees by viewModel.employees.collectAsStateWithLifecycle()
    val knowledgeItems by viewModel.knowledgeItems.collectAsStateWithLifecycle()
    val documents by viewModel.documents.collectAsStateWithLifecycle()
    val emergencyProtocols by viewModel.emergencyProtocols.collectAsStateWithLifecycle()

    // Collect Sync & Connectivity States
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val isDeviceOnline by viewModel.isDeviceOnline.collectAsStateWithLifecycle()
    val pendingSyncs by viewModel.pendingSyncCount.collectAsStateWithLifecycle()
    val isSyncing by viewModel.syncing.collectAsStateWithLifecycle()

    // CouchDB / PouchDB Specific Metadata
    val pouchUpdateSeq by viewModel.pouchUpdateSeq.collectAsStateWithLifecycle()
    val pouchConflictCount by viewModel.pouchConflictCount.collectAsStateWithLifecycle()
    val replicationDirection by viewModel.replicationDirection.collectAsStateWithLifecycle()
    val lastSyncTime by viewModel.lastSyncTime.collectAsStateWithLifecycle()
    val pouchSyncLog by viewModel.pouchSyncLog.collectAsStateWithLifecycle()

    // Calculate database offline stats and byte representation
    val totalRecords = decisions.size + financeRecords.size + operations.size + 
                       partners.size + employees.size + knowledgeItems.size + 
                       documents.size + emergencyProtocols.size
    
    val calculatedDbSizeKb = totalRecords * 1.15 + 412.0
    val effectiveOnline = isOnline && isDeviceOnline
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Graphic Header block
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "🔗 مرآة مزامنة المعطيات ومحاكاة PouchDB أوفلاين",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "إدارة ومراقبة دورة حياة المزامنة ثنائية الاتجاه مع قواعد السحابة وأوفلاين",
                            color = MaterialTheme.colorScheme.outline,
                            fontSize = 11.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                
                Text(
                    text = "يدمج التطبيق قاعدة بيانات Room محلياً على الهاتف لتعمل كقاعدة بيانات محلية متزامنة (Offline-First Local Database Store). " +
                           "نظام المزامنة هنا يحاكي بروتوكول PouchDB/CouchDB متتبعاً أرقام التعديلات والـ Sequence من أجل تبادل البيانات وتمريرها فور التوصيل بالانترنت والحد من الفقدان.",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                )
            }
        }

        // Live METRICS cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. Update sequence
            Card(
                modifier = Modifier.weight(1.5f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("متسلسلة التعديل المحلية", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "#$pouchUpdateSeq",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("Seq ID", fontSize = 8.sp, color = MaterialTheme.colorScheme.outline)
                }
            }

            // 2. Queue changes
            Card(
                modifier = Modifier.weight(1.5f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("التغييرات المحلية المعلقة", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$pendingSyncs",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (pendingSyncs > 0) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                    )
                    Text("حزم غير مرحلة", fontSize = 8.sp, color = MaterialTheme.colorScheme.outline)
                }
            }

            // 3. Conflicts
            Card(
                modifier = Modifier.weight(1.5f),
                colors = CardDefaults.cardColors(
                    containerColor = if (pouchConflictCount > 0) Color(0xFFFFF3CD) else MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    0.8.dp, 
                    if (pouchConflictCount > 0) Color(0xFFE0A800) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("تعارضات التموج", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$pouchConflictCount",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (pouchConflictCount > 0) Color(0xFFD63031) else Color(0xFF2E7D32)
                    )
                    Text("Couch-Conflicts", fontSize = 8.sp, color = MaterialTheme.colorScheme.outline)
                }
            }
        }

        // WARNING ALERT FOR ACTIVE CONFLICTS
        if (pouchConflictCount > 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF2F2)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF3B2B2)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFC62828),
                        modifier = Modifier.size(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "تم رصد تعارضات في النسخ المستديمة!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFFC62828)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "يوجد تعارض في التعديلات المتزامنة على بعض الاستمارات. انقر أسفله لفض النزاعات وتحديث السحابة تلقائياً.",
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = Color(0xFF424242)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.resolvePouchConflicts() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("resolve_conflicts_btn").height(36.dp)
                        ) {
                            Text("محاذاة وفض التعارضات (Client-Win)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        // Title Section 2: Online Status Control Panel
        Text(
            text = "⚙️ لوحة التحكم بقنوات وطرق الاتصال والمزامنة",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.secondary
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                
                // Hardware Switch and Status indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isOnline) "🟢 وضع تشغيل متصل بالانترنت" else "🔴 وضع تشغيل أوفلاين (Local-Only)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "يتنقل النظام بتدرج عند إيقاف وضع الاتصال السحابي لمحاكاة التخزين في الذاكرة",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Switch(
                        checked = isOnline,
                        onCheckedChange = { viewModel.toggleOnlineMode() },
                        modifier = Modifier.testTag("online_mode_switch")
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)

                // Replication setting selector
                Column {
                    Text(
                        text = "اتجاه تزامن البيانات النشط (Replication Direction):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val directions = listOf(
                            "Bidirectional" to "مزامنة ثنائية",
                            "PushOnly" to "رفع فقط (Push)",
                            "PullOnly" to "تحميل فقط (Pull)"
                        )
                        directions.forEach { (dir, label) ->
                            val selected = replicationDirection == dir
                            FilterChip(
                                selected = selected,
                                onClick = { viewModel.updateReplicationDirection(dir) },
                                label = { Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)

                // Active interactive push buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Manual replication sync trigger
                    Button(
                        onClick = { viewModel.triggerManualSync() },
                        enabled = isOnline && isDeviceOnline && !isSyncing,
                        modifier = Modifier.weight(1f).testTag("trigger_pouch_sync_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ترحيل النسخ الآن", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Simulated modification creator button
                    OutlinedButton(
                        onClick = { viewModel.simulateNewLocalChange() },
                        modifier = Modifier.weight(1f).testTag("simulate_local_mod_btn"),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إجراء تعديل محلي", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Database reset / restore default replication
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { viewModel.simulateCloudDatabaseReset() },
                        modifier = Modifier.testTag("reset_sync_demo_btn")
                    ) {
                        Text("إعادة تصفير وضبط مؤشرات المزامنة", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }

        // Title Section 3: Room Database Offline Registry
        Text(
            text = "📲 السجلات المتوفرة في قاعدة بيانات Room الأوفلاين",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.secondary
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "مؤشرات تغطية الذاكرة الفائقة للمحافظة على العمل (Offline Index):",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                val stores = listOf(
                    Triple("الاستمارات والقرارات الإدارية", "${decisions.size} سجل", Icons.Default.Gavel),
                    Triple("المعاملات المالية والفواتير المشفّرة", "${financeRecords.size} دفعة", Icons.Default.AccountBalance),
                    Triple("تقارير مسح حوادث وبلاغات الألغام", "${operations.size} تقرير", Icons.Default.Warning),
                    Triple("شراكات المانحين والمنظمات التوثيقية", "${partners.size} شريك", Icons.Default.Group),
                    Triple("الحضور وسجلات الموارد البشرية", "${employees.size} موظف", Icons.Default.Person),
                    Triple("SOP معايير مكافحة الألغام ومؤشراتها", "${knowledgeItems.size} بند", Icons.Default.Book),
                    Triple("الوثائق وبطاقات المهام الميدانية", "${documents.size} وثيقة", Icons.Default.Folder),
                    Triple("بروتوكولات الطوارئ والسلامة الفورية", "${emergencyProtocols.size} بروتوكول", Icons.Default.Info)
                )

                stores.forEachIndexed { idx, pair ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = pair.third,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(text = pair.first, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Text(text = pair.second, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp))
                        }
                    }
                    if (idx < stores.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 0.5.dp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.5.dp)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("تقدير حجم الذاكرة المستخدمة (IndexedDB/Couch):", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    Text(
                        text = "${String.format(java.util.Locale.US, "%.2f", calculatedDbSizeKb)} KB",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Title Section 4: Live Sync Console Logs
        Text(
            text = "💾 سير عمليات المزامنة التاريخية (Sync Event Logs)",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.secondary
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)), // dark slate background
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "💻 CouchDB / Pouch Replication Event Tracker",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                    Badge(
                        containerColor = Color(0xFF1E293B),
                        contentColor = Color(0xFF38BDF8)
                    ) {
                        Text("Active Daemon", fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    pouchSyncLog.takeLast(7).forEach { log ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = ">",
                                color = Color(0xFF38BDF8),
                                fontSize = 11.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                            Text(
                                text = log,
                                color = Color(0xFF34D399), // soft responsive green
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "آخر مزامنة ناجحة: $lastSyncTime",
                        fontSize = 9.sp,
                        color = Color(0xFF64748B),
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ReportDiscoveryView(viewModel: PmacViewModel) {
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val isDeviceOnline by viewModel.isDeviceOnline.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var currentStep by remember { mutableStateOf(1) }

    // Form inputs state
    var titleState by remember { mutableStateOf("") }
    var selectedArea by remember { mutableStateOf("جنين") }
    var latitudeState by remember { mutableStateOf("31.950000") }
    var longitudeState by remember { mutableStateOf("35.200000") }

    var selectedType by remember { mutableStateOf("ذخيرة غير منفجرة (UXO)") }
    var riskScoreSelected by remember { mutableStateOf(3f) }
    var selectedTeam by remember { mutableStateOf("فريق مسح أ") }

    var descriptionState by remember { mutableStateOf("") }
    var casualtiesValue by remember { mutableStateOf(0) }
    var isAiGenerating by remember { mutableStateOf(false) }

    val areas = listOf("جنين", "الخليل", "غزة", "نابلس", "أريحا")
    val hazardTypes = listOf(
        "لغم مضاد للأفراد",
        "لغم مضاد للدروع",
        "ذخيرة غير منفجرة (UXO)",
        "عبوة ناسفة مصنعة محلياً (IED)",
        "مخلفات حربية أخرى"
    )
    val teams = listOf("فريق مسح أ", "فريق تثقيف ب", "فريق تدخل ج")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Form Title Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "📝 بلاغ الكشف الميداني الجديد (PouchDB Client)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "تسجيل فوري للألغام والمخلفات غير المنفجرة مع مزامنة أوفلاين تلقائية",
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Stepper Progress Indicator Row (Step 1 to 4, step 5 is final success)
        if (currentStep <= 4) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(
                            1 to "الموقع",
                            2 to "الجسم",
                            3 to "البيئة",
                            4 to "التأكيد"
                        ).forEach { (step, label) ->
                            val isCurrent = currentStep == step
                            val isPassed = currentStep > step
                            
                            val labelColor = when {
                                isCurrent -> MaterialTheme.colorScheme.primary
                                isPassed -> MaterialTheme.colorScheme.onSurface
                                else -> MaterialTheme.colorScheme.outline
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.testTag("step_indicator_$step")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(
                                            color = when {
                                                isCurrent -> MaterialTheme.colorScheme.primary
                                                isPassed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                else -> MaterialTheme.colorScheme.surfaceVariant
                                            },
                                            shape = CircleShape
                                        )
                                        .border(
                                            1.5.dp, 
                                            if (isCurrent) MaterialTheme.colorScheme.primary else Color.Transparent, 
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isPassed) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    } else {
                                        Text(
                                            text = step.toString(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = labelColor)
                            }

                            if (step < 4) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(2.dp)
                                        .background(
                                            if (currentStep > step) MaterialTheme.colorScheme.primary 
                                            else MaterialTheme.colorScheme.outlineVariant
                                        )
                                        .padding(horizontal = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Steps Content Switcher
        when (currentStep) {
            1 -> {
                // Step 1: Basic & Location Inputs
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "📍 الخطوة 1: البيانات الأساسية للبلدة والإحداثيات",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Title field
                        OutlinedTextField(
                            value = titleState,
                            onValueChange = { titleState = it },
                            label = { Text("عنوان كشف البلاغ (مثال: العثور على لغم في حجر يعبد)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("discovery_title_input"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        // Geographic Area selector
                        Column {
                            Text(
                                text = "المنطقة الجغرافية العامة:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                areas.forEach { area ->
                                    val isSelected = selectedArea == area
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedArea = area },
                                        label = { Text(area, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

                        // Coordinates inputs
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = latitudeState,
                                onValueChange = { latitudeState = it },
                                label = { Text("خط العرض (Latitude)") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_lat"),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            OutlinedTextField(
                                value = longitudeState,
                                onValueChange = { longitudeState = it },
                                label = { Text("خط الطول (Longitude)") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_lng"),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }

                        // GPS Generator Button
                        Button(
                            onClick = {
                                val randomLat = 31.3 + (0.01 * (1..100).random())
                                val randomLng = 34.2 + (0.01 * (1..100).random())
                                latitudeState = String.format(java.util.Locale.US, "%.6f", randomLat)
                                longitudeState = String.format(java.util.Locale.US, "%.6f", randomLng)
                                Toast.makeText(context, "تم تحديد إحداثيات GPS تلقائية للبلاغ!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("generate_gps_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("📍 توليد إحداثيات GPS تلقائياً للموقع الميداني", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            2 -> {
                // Step 2: Hazard details
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "💣 الخطوة 2: صنف الجسم المتفجر ورمز الخطر",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Body type selection chips (Vertical or scrolling flow)
                        Column {
                            Text(
                                text = "نوع المقذوف/الخطورة المكتشفة:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            // Let's draw chips in a scrollable view
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                hazardTypes.forEach { hType ->
                                    val isSelected = selectedType == hType
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedType = hType },
                                        label = { Text(hType, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

                        // Threat Priority score
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "مستوى الخطورة الفورية المقدر:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Badge(
                                    containerColor = if (riskScoreSelected >= 4) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = if (riskScoreSelected >= 4) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                                ) {
                                    Text(
                                        text = "${riskScoreSelected.toInt()} / 5 (${if (riskScoreSelected >= 4) "خطر داهم" else "مستقر نسبياً"})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Slider(
                                value = riskScoreSelected,
                                onValueChange = { riskScoreSelected = it },
                                valueRange = 1f..5f,
                                steps = 3,
                                modifier = Modifier.testTag("risk_level_slider")
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

                        // Field Team assignment selection
                        Column {
                            Text(
                                text = "الفريق الميداني المفوّض للتعامل مع هذا البلاغ:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                teams.forEach { team ->
                                    val isSelected = selectedTeam == team
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedTeam = team },
                                        label = { Text(team, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            3 -> {
                // Step 3: Description, AI assist and Casualties
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "🗒️ الخطوة 3: الوصف الفني والتوليد بالذكاء الاصطناعي",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Casualties counter
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "عدد الإصابات/الضحايا الحالية (إن وجد):",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text("اترك القيمة صفر في حال عدم وجود حوادث لحظية", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                            }
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedIconButton(
                                    onClick = { if (casualtiesValue > 0) casualtiesValue-- },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Text("-", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                                Text("$casualtiesValue", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                OutlinedIconButton(
                                    onClick = { casualtiesValue++ },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Text("+", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

                        // Intelligent Gemini assistant integration
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "وصف البلاغ والتفاصيل الميدانية الفنية:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                
                                Button(
                                    onClick = {
                                        if (titleState.isBlank()) {
                                            Toast.makeText(context, "الرجاء كتابة عنوان للبلاغ أولاً لتسهيل صياغته!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            isAiGenerating = true
                                            viewModel.generateAiDescriptionForDiscovery(
                                                title = titleState,
                                                type = selectedType,
                                                riskScore = riskScoreSelected.toInt()
                                            ) { result ->
                                                descriptionState = result
                                                isAiGenerating = false
                                            }
                                        }
                                    },
                                    enabled = !isAiGenerating,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    modifier = Modifier
                                        .height(34.dp)
                                        .testTag("ai_generate_details_button"),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    if (isAiGenerating) {
                                        CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp)
                                    } else {
                                        Icon(imageVector = Icons.Default.SmartToy, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("توليد ذكي (Gemini)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = descriptionState,
                                onValueChange = { descriptionState = it },
                                label = { Text("التفاصيل الميدانية أو الوصف الفني للجسم المتفجر...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                                    .testTag("details_input"),
                                maxLines = 4,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }

            4 -> {
                // Step 4: Preview & PouchDB database action
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "🔍 الخطوة 4: معاينة بلاغ الكشف وحوسبة المزامنة",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Simulated PouchDB database sync state banner
                        val actualOnline = isOnline && isDeviceOnline
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (actualOnline) Color(0xFFE8F5E9) else Color(0xFFFFF3CD)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp, 
                                if (actualOnline) Color(0xFFA5D6A7) else Color(0xFFFFE082)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (actualOnline) Icons.Default.CloudSync else Icons.Default.CloudOff,
                                    contentDescription = null,
                                    tint = if (actualOnline) Color(0xFF2E7D32) else Color(0xFFD84315)
                                )
                                Column {
                                    Text(
                                        text = if (actualOnline) "🟢 وضع المزامنة المباشرة نشط (Online)" else "⚠️ تشغيل محلي أوفلاين (Offline Cache)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (actualOnline) Color(0xFF2E7D32) else Color(0xFFD84315)
                                    )
                                    Text(
                                        text = if (actualOnline) 
                                            "سيتم حفظ البلاغ محلياً في الذاكرة ورفعه فوراً لخوادم السحابة لتحديث Sequence المزامنة." 
                                            else "سيتم تخزين البلاغ فوراً في كاش ومؤشر PouchDB (Room DB) وسترتفع التعديلات التراكمية (+1 حزمة معلقة) تلقائياً عند عودة الاتصال بكفاءة.",
                                        fontSize = 10.sp,
                                        lineHeight = 14.sp,
                                        color = if (actualOnline) Color(0xFF1B5E20) else Color(0xFFE65100)
                                    )
                                }
                            }
                        }

                        // Summary list card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("📝 عنوان بلاغ الكشف:", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                    Text(titleState, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                                }
                                HorizontalDivider(thickness = 0.5.dp)
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("📍 النطاق والمنطقة:", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                    Text(selectedArea, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                HorizontalDivider(thickness = 0.5.dp)
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("🛰️ الإحداثيات التقريبية:", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                    Text("$latitudeState , $longitudeState", fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                }
                                HorizontalDivider(thickness = 0.5.dp)
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("💣 نوع المقذوف وجسم الخطر:", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                    Text(selectedType, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.tertiary)
                                }
                                HorizontalDivider(thickness = 0.5.dp)
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("🛑 درجة الخطورة الفورية:", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                    Text("${riskScoreSelected.toInt()} / 5", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                                }
                                HorizontalDivider(thickness = 0.5.dp)
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("🛡️ الفريق المكلّف بالتحقق:", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                    Text(selectedTeam, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                HorizontalDivider(thickness = 0.5.dp)
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("👤 الإصابات المرصودة:", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                    Text("$casualtiesValue إصابة", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                HorizontalDivider(thickness = 0.5.dp)
                                Column {
                                    Text("📋 تفاصيل التحقيق الميداني الموصوفة:", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = descriptionState.ifBlank { "لم يتم سرد تفاصيل مخصصة." },
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Horizontal bottom navigation buttons for Multi-step wizard
        if (currentStep <= 4) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button
                if (currentStep > 1) {
                    OutlinedButton(
                        onClick = { currentStep-- },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("arrow_prev_step")
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("السابق", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Next / Submit Button
                Button(
                    onClick = {
                        if (currentStep == 1) {
                            if (titleState.isBlank() || latitudeState.isBlank() || longitudeState.isBlank()) {
                                Toast.makeText(context, "الرجاء ملء حقول البلاغ وتثبيت الإحداثيات أولاً!", Toast.LENGTH_SHORT).show()
                            } else {
                                currentStep++
                            }
                        } else if (currentStep < 4) {
                            currentStep++
                        } else {
                            // Step 4 hit: Save to simulated PouchDB Database
                            viewModel.addOperation(
                                title = titleState,
                                type = selectedType,
                                fieldTeam = selectedTeam,
                                area = selectedArea,
                                date = "2026-06-09",
                                status = if (riskScoreSelected >= 4) "خطر مرتفع" else "نشط",
                                details = descriptionState.ifBlank { "تم توليد وقيد بلاغ الكشف وفقاً للإحداثيات $latitudeState , $longitudeState." },
                                casualties = casualtiesValue,
                                riskScore = riskScoreSelected.toInt(),
                                aiSummary = "تم التأكيد والتحقق وتأمين دائرة الخطر في النطاق."
                            )
                            currentStep = 5 // Go to Success View
                            Toast.makeText(context, "تم حفظ البلاغ بنجاح في PouchDB!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag(if (currentStep == 4) "save_discovery_pouch_button" else "arrow_next_step")
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (currentStep == 4) "💾 اعتماد وحفظ البلاغ" else "التالي",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            // STEP 5: Success & Confirmation View block
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF4CAF50)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(Color(0xFFE8F5E9), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(54.dp)
                        )
                    }

                    Text(
                        text = "🎉 تم تلقي البلاغ بنجاح في PouchDB Store!",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF2E7D32),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = if (isOnline && isDeviceOnline) 
                            "المستند تم رفعه ومزامنته للشبكة السحابية فورياً وتعديل متسلسلة CouchDB Sequence." 
                            else "أنت الآن في وضع أوفلاين. تم حفظ البلاغ بأمان بمتسلسلة محلية Sequence ID. سيظهر لك التعديل معلق (+1 حزمة) في لوحة المزامنة وسيرفع تلقائياً فور عودة الاتصال بكفاءة.",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Secondary action buttons (Go to database or file new discovery)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.setSection("Operations")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("go_to_operations_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("📋 سجلات العمليات", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                // Reset form state
                                titleState = ""
                                selectedArea = "جنين"
                                latitudeState = "31.950000"
                                longitudeState = "35.200000"
                                selectedType = "ذخيرة غير منفجرة (UXO)"
                                riskScoreSelected = 3f
                                selectedTeam = "فريق مسح أ"
                                descriptionState = ""
                                casualtiesValue = 0
                                currentStep = 1
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("reset_form_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("➕ بلاغ جديد", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

