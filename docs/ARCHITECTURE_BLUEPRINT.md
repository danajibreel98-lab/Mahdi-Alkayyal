# مخطط المعمار المرجعي وإدارة الصلاحيات الشاملة للأنظمة المؤسسية
## PMAC INTEGRATED ENTERPRISE PORTAL - ARCHITECTURE BLUEPRINT

يقدم هذا المستند الصياغة المعمارية الشاملة واحترافية بالكامل لجميع الأنظمة الفرعية لمركز PMAC للعمليات والمؤشرات الكبرى. تم تصميم هذا المخطط ليكون دليلاً مرجعياً كاملاً وموثوقاً لمطوري الويب وأنظمة الـ Backend والذكاء الاصطناعي لتهيئة النظم ومطابقة هياكل المزامنة.

---

## 1. بنية النظام المعمارية وهيكلية المجلدات المستهدفة (Folder Structure)

```text
project/
│
├── apps/
│   ├── web/                 # React Frontend (Vite / Jetpack Client)
│   └── api/                 # Node.JS Express / NestJS Backend
│
├── modules/
│   ├── finance/             # الدائرة المالية، الموازنات والعهود المنقولة
│   ├── hr/                  # الموارد البشرية، الرواتب والتقييمات الميدانية
│   ├── operations/          # العمليات والمناطق المشتبه بها وحوادث الألغام
│   ├── procurement/         # المشتريات ومزودي الخدمات من المانحين الدوليين
│   ├── assets/              # إدارة الأصول، مخزون الأسلحة والعهد المادية (مثل كاشفات المعادن MineLab F3)
│   ├── crm/                 # الشركاء وإدارة حوارات المانحين والمجتمع المحلي
│   ├── reports/             # محركات الفلترة الذكية وإحصائيات KPI وتصدير التقارير
│   └── notifications/       # مركز إدارة التنبيهات وإشعارات الطوارئ
│
├── shared/
│   ├── types/               # القوالب المشتركة وأطر الصلاحيات (RBAC Types)
│   ├── utils/               # محركات الحسابات والإسقاطات الجغرافية
│   ├── constants/           # الثوابت ومعايير الأمان الوطنية والدولية IMAS
│   └── permissions/         # قاموس فك التشفير والتحقق من التواقيع الإلكترونية
│
├── database/
│   ├── migrations/          # نصوص تتبع نسخ الهياكل الزمنية
│   ├── seeds/               # المكونات المبدئية والبيانات الدورية للشركة
│   └── schema/              # تعريفات الجداول والعلاقات الثنائية Drizzle/Postgres
│
├── docs/                    # ملفات التوثيق الفني والبروتوكولات
└── infrastructure/          # إعدادات Docker ومحاكاة النظم الموزعة Offline-First
```

---

## 2. هيكلة قاعدة البيانات بقوانين PostgreSQL & Drizzle ORM Schema

فيما يلي توصيف الجداول الرئيسية والعلاقات بقالب Drizzle ORM المتوافق مع محرك PostgreSQL:

```typescript
import { pgTable, serial, varchar, text, doublePrecision, integer, boolean, timestamp, pgEnum } from 'drizzle-orm/pg-core';
import { relations } from 'drizzle-orm';

// 1. تحديد تصنيفات الصلاحيات والأدوار عبر PG ENUMS
export const roleEnum = pgEnum('user_role', [
  'Administrator',
  'Executive Director',
  'Finance Manager',
  'Operations Manager',
  'EORE Manager',
  'Public Relations Officer',
  'HR Officer',
  'Field Supervisor',
  'Data Entry'
]);

export const assetConditionEnum = pgEnum('asset_condition', [
  'ممتازة',
  'تحت الصيانة',
  'تالفة',
  'مفقودة في الميدان'
]);

// 2. جدول المستخدمين والصلاحيات الفرعية
export const users = pgTable('users', {
  id: serial('id').primaryKey(),
  username: varchar('username', { length: 50 }).notNull().unique(),
  passwordHash: varchar('password_hash', { length: 255 }).notNull(),
  fullName: varchar('full_name', { length: 150 }).notNull(),
  role: roleEnum('role').default('Field Supervisor').notNull(),
  permissions: text('permissions').notNull(), // Comma-separated list for custom policy overrides
  createdAt: timestamp('created_at').defaultNow().notNull(),
  updatedAt: timestamp('updated_at').defaultNow().notNull()
});

// 3. جدول إدارة الأصول و العهد (Assets & Logistics)
export const assets = pgTable('assets', {
  id: serial('id').primaryKey(),
  name: varchar('name', { length: 200 }).notNull(), // e.g. "MineLab F3"
  serialNumber: varchar('serial_number', { length: 100 }).notNull().unique(),
  type: varchar('type', { length: 100 }).notNull(), // "كاشف ألغام (Metal Detector)", "سيارة دفع رباعي 4x4", "سترة واقية"
  condition: assetConditionEnum('condition').default('ممتازة').notNull(),
  assignedTo: varchar('assigned_to', { length: 150 }).notNull(), // Allocated Team/Supervisor
  storeLocation: varchar('store_location', { length: 150 }).notNull(), // e.g. "مستودع جنين الرئيسي"
  purchaseDate: timestamp('purchase_date').notNull(),
  lastServiceDate: timestamp('last_service_date').defaultNow().notNull(),
  supplier: varchar('supplier', { length: 150 }).default('MineLab Australia')
});

// 4. سجل حركات التدقيق والمراقبة الذاتي (Audit Logs)
export const auditLogs = pgTable('audit_logs', {
  id: serial('id').primaryKey(),
  userId: integer('user_id').references(() => users.id),
  username: varchar('username', { length: 50 }).notNull(),
  fullName: varchar('full_name', { length: 150 }).notNull(),
  role: varchar('role', { length: 50 }).notNull(),
  action: varchar('action', { length: 200 }).notNull(), // Description of change, e.g. "تعديل حالة الأصل"
  details: text('details').notNull(), // Extended payload differences (diff JSON / text)
  timestamp: timestamp('timestamp').defaultNow().notNull(),
  systemModule: varchar('system_module', { length: 100 }).notNull(), // "الصلاحيات", "العمليات", "المالية", "الأصول"
  status: varchar('status', { length: 50 }).default('نجاح (Success)').notNull()
});

// 5. جدول العمليات وحوادث الألغام (Operations Reports)
export const operationReports = pgTable('operation_reports', {
  id: serial('id').primaryKey(),
  title: varchar('title', { length: 255 }).notNull(),
  type: varchar('type', { length: 100 }).notNull(), // "مسح ميداني", "حملة توعية", "إدارة حوادث"
  fieldTeam: varchar('field_team', { length: 100 }).notNull(),
  area: varchar('area', { length: 100 }).notNull(),
  date: timestamp('date').notNull(),
  status: varchar('status', { length: 50 }).notNull(), // "نشط", "مكتمل", "تحت المراجعة"
  details: text('details').notNull(),
  latitude: doublePrecision('latitude').notNull(),
  longitude: doublePrecision('longitude').notNull(),
  casualties: integer('casualties').default(0).notNull(),
  riskScore: integer('risk_score').default(1).notNull(),
  aiSummary: text('ai_summary')
});

// 6. جدول السجلات المالية وحساب المانحين (Finance Records)
export const financeRecords = pgTable('finance_records', {
  id: serial('id').primaryKey(),
  title: varchar('title', { length: 255 }).notNull(),
  type: varchar('type', { length: 100 }).notNull(), // "موازنة سنوية", "مصروفات", "إيرادات"
  project: varchar('project', { length: 150 }).notNull(),
  amount: doublePrecision('amount').notNull(),
  category: varchar('category', { length: 100 }).notNull(),
  date: timestamp('date').notNull(),
  donor: varchar('donor', { length: 150 }).notNull(),
  isSynced: boolean('is_synced').default(true).notNull(),
  notes: text('notes'),
  isAiForecasted: boolean('is_ai_forecasted').default(false).notNull()
});
```

---

## 3. معمارية واجهات برمجة التطبيقات ونهايات الربط المستهدفة (API Endpoint Blueprint)

يتخذ النظام بنية RESTful موحدة لتمرير حزم البيانات، وتتم الحماية على خطوط الربط باستخدام الـ Middleware الشهير للـ JWT Role Guard.

### أ. مجموعة إدارة الأصول لوجستياً (Procurement & Assets)
* **`POST /api/v1/assets`**
  * *الوصف:* تسجيل عهد ميداني جديد وجدولته تحت فرع معين بسلسلة التوريد.
  * *الأدوار المخولة:* `Administrator`, `Executive Director`, `Operations Manager`
  * *جسم الطلب (Request Body):*
    ```json
    {
      "name": "MineLab F3 Detector",
      "serialNumber": "ML-F3-01",
      "type": "كاشف ألغام (Metal Detector)",
      "condition": "ممتازة",
      "assignedTo": "فريق مسح أ",
      "storeLocation": "مستودع جنين الرئيسي",
      "supplier": "MineLab Australia"
    }
    ```
* **`GET /api/v1/assets`**
  * *الوصف:* استعلام وبحث وفلترة لكافة الأصول المادية وحالات صيانتها الحالية.
  * *الأدوار المخولة:* الجميع لغايات القراءة
* **`PATCH /api/v1/assets/:id/condition`**
  * *الوصف:* تحديث حالة الأصل (مثلاً: من ممتازة إلى تحت الصيانة إثر تلف أو فحص دوري).
  * *الأدوار المخولة:* `Field Supervisor`, `Operations Manager`, `Administrator`

### ب. سجل حركات التدقيق الشامل (Enterprise Audit Trails)
* **`GET /api/v1/audit-logs`**
  * *الوصف:* جلب السجل الزمني الفعلي للأحداث في جميع الأنظمة الفرعية مضافاً إليه المرشحات.
  * *الأدوار المخولة:* `Administrator`, `Executive Director`
* **`POST /api/v1/audit-logs`**
  * *الوصف:* تسجيل حدث فني جديد (يتم التمرير داخلياً من قبل النظام كبروتوكول للمراقبة الذاتية).

### ج. حماية الصلاحيات وإصدار توثيقات الدخول (Security Auths)
* **`POST /api/v1/auth/login`**
  * *الوصف:* مصادقة الهوية الرقمية للمشرف واسترداد الـ JWTToken الحافل بمصفوفة الصلاحيات.
* **`POST /api/v1/auth/register-user`**
  * *الوصف:* تكوين مستخدم جديد محمي بواسطة المشرف وإلحاق الدور الوظيفي المناسب.

---

## 4. مصفوفة الصلاحيات وتفويضات الأدوار الفنية (RBAC Matrix)

الجدول التالي يوضح تفويض الأدوار بدقة عبر مختلف وحدات نظام PMAC:

| الوحدة الفرعية (System Module) | Administrator | Executive Director | Operations Manager | Finance Manager | Field Supervisor |
| :--- | :---: | :---: | :---: | :---: | :---: |
| **التحكم بالصلاحيات والمستخدمين** | 🟢 تعديل وقبول | 🟡 قراءة فقط | 🔴 غير مخول | 🔴 غير مخول | 🔴 غير مخول |
| **سجل التدقيق (Audit Logs)** | 🟢 تعديل وقبول | 🟢 قراءة المراقبة | 🔴 غير مخول | 🔴 غير مخول | 🔴 غير مخول |
| **المالية والتقارير الموازنة** | 🟢 إدارة كاملة | 🟢 إدارة كاملة | 🔴 غير مخول | 🟢 إدارة كاملة | 🔴 غير مخول |
| **العهد والأصول (Logistics Assets)** | 🟢 إدارة كاملة | 🟢 إدارة كاملة | 🟢 إدارة العمليات | 🟡 قراءة العهد | 🟡 قراءة وتحديث الحالة |
| **العمليات وحوادث الألغام** | 🟢 إدارة كاملة | 🟢 إدارة كاملة | 🟢 إدارة العمليات | 🔴 غير مخول | 🟢 إرسال وتعديل البلاغ |
| **بروتوكولات الطوارئ SOP** | 🟢 إنشاء وقبول | 🟢 إنشاء وقبول | 🟡 قراءة الإرشاد | 🔴 غير مخول | 🟡 قراءة الإرشاد |

---

## 5. محرك مزامنة البيانات دون اتصال بالإنترنت (Offline-to-Online Architecture)

يعتمد نظام PMAC على آليات حوسبية طرفية مبتكرة لإتاحة تجربة تشغيل مثالية للفرق الميدانية في المناطق عازلة التغطية والخالية من الإنترنت:
1. **PouchDB & SQLite Adapter:** يتم تهيئة قاعدة بيانات محلية صامتة داخل كاشف الأجهزة الميدانية.
2. **Sequential Revision Tracking (Sequence ID):** يعطى لكل تعديل مدته رقم تسلسلي متصاعد (مثلاً Sequence 2408).
3. **Client-Wins Conflict Rule:** عند معاودة الاتصال وبدء المزامنة مع سحابة PostgreSQL المركزية، يقرأ النظام التغييرات ويقارن المتسلسلات. وفي حالة حدوث تعارض بالمدخلات المباشرة، يتم تمرير تعديل المشرف الميداني الأحدث زمناً لضمان ثبات الحقائق على الأرض مع كتابة إشعار تعارض صامت بـ Audit Log.

تم اعتماد هذا المخطط رسمياً كمنظومة مستهدفة في 9 يونيو 2026.
