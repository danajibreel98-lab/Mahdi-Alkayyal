package com.example.data

import kotlinx.coroutines.flow.Flow

class PmacRepository(private val db: PmacDatabase) {

    // 1. Decisions & Tasks
    val allDecisions: Flow<List<DecisionTask>> = db.decisionTaskDao().getAllDecisions()
    suspend fun insertDecision(decision: DecisionTask) = db.decisionTaskDao().insertDecision(decision)
    suspend fun updateDecision(decision: DecisionTask) = db.decisionTaskDao().updateDecision(decision)
    suspend fun deleteDecision(decision: DecisionTask) = db.decisionTaskDao().deleteDecision(decision)

    // 2. Finance
    val allFinanceRecords: Flow<List<FinanceRecord>> = db.financeRecordDao().getAllFinanceRecords()
    suspend fun insertFinance(record: FinanceRecord) = db.financeRecordDao().insertFinance(record)
    suspend fun updateFinance(record: FinanceRecord) = db.financeRecordDao().updateFinance(record)
    suspend fun deleteFinance(record: FinanceRecord) = db.financeRecordDao().deleteFinance(record)

    // 3. Operations
    val allOperations: Flow<List<OperationReport>> = db.operationReportDao().getAllOperations()
    suspend fun insertOperation(report: OperationReport) = db.operationReportDao().insertOperation(report)
    suspend fun updateOperation(report: OperationReport) = db.operationReportDao().updateOperation(report)
    suspend fun deleteOperation(report: OperationReport) = db.operationReportDao().deleteOperation(report)

    // 4. PR & Partners
    val allPartners: Flow<List<PartnerDonor>> = db.partnerDonorDao().getAllPartners()
    suspend fun insertPartner(partner: PartnerDonor) = db.partnerDonorDao().insertPartner(partner)
    suspend fun updatePartner(partner: PartnerDonor) = db.partnerDonorDao().updatePartner(partner)
    suspend fun deletePartner(partner: PartnerDonor) = db.partnerDonorDao().deletePartner(partner)

    // 5. HR Employees
    val allEmployees: Flow<List<EmployeeRecord>> = db.employeeRecordDao().getAllEmployees()
    suspend fun insertEmployee(employee: EmployeeRecord) = db.employeeRecordDao().insertEmployee(employee)
    suspend fun updateEmployee(employee: EmployeeRecord) = db.employeeRecordDao().updateEmployee(employee)
    suspend fun deleteEmployee(employee: EmployeeRecord) = db.employeeRecordDao().deleteEmployee(employee)

    // 6. Knowledge
    val allKnowledgeItems: Flow<List<KnowledgeItem>> = db.knowledgeItemDao().getAllKnowledgeItems()
    suspend fun insertKnowledgeItem(item: KnowledgeItem) = db.knowledgeItemDao().insertKnowledgeItem(item)
    suspend fun deleteKnowledgeItem(item: KnowledgeItem) = db.knowledgeItemDao().deleteKnowledgeItem(item)

    // 7. Documents
    val allDocuments: Flow<List<PmacDocument>> = db.pmacDocumentDao().getAllDocuments()
    fun searchDocuments(query: String): Flow<List<PmacDocument>> = db.pmacDocumentDao().searchDocuments(query)
    suspend fun insertDocument(document: PmacDocument): Long = db.pmacDocumentDao().insertDocument(document)
    suspend fun updateDocument(document: PmacDocument) = db.pmacDocumentDao().updateDocument(document)
    suspend fun deleteDocument(document: PmacDocument) = db.pmacDocumentDao().deleteDocument(document)

    // 8. Document Versions
    fun getVersionsForDocument(docId: Int): Flow<List<DocumentVersion>> = db.documentVersionDao().getVersionsForDocument(docId)
    suspend fun insertDocumentVersion(version: DocumentVersion) = db.documentVersionDao().insertVersion(version)

    // 9. Users Offline Authentication (Dexie equivalent 'users' store)
    suspend fun getUserByUsername(username: String): User? = db.userDao().getUserByUsername(username)
    suspend fun insertUser(user: User) = db.userDao().insertUser(user)
    suspend fun getUserCount(): Int = db.userDao().getUserCount()

    // 10. Emergency Protocols (Local Indexed & Always Accessible Offline)
    val allEmergencyProtocols: Flow<List<EmergencyProtocol>> = db.emergencyProtocolDao().getAllProtocols()
    suspend fun insertEmergencyProtocol(protocol: EmergencyProtocol) = db.emergencyProtocolDao().insertProtocol(protocol)
    suspend fun getEmergencyProtocolCount(): Int = db.emergencyProtocolDao().getProtocolCount()
    fun searchEmergencyProtocols(query: String): Flow<List<EmergencyProtocol>> = db.emergencyProtocolDao().searchProtocols(query)
}
