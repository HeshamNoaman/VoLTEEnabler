# دليل شامل لطرق تفعيل ميزة VoLTE وأوامر ADB البرمجية المصاحبة

[English](METHODS_AND_ADB_COMMANDS.md) | **العربية**

يقدم هذا المرجع التقني شرحاً مفصلاً لجميع طرق تفعيل ميزة المكالمات الصوتية عبر الجيل الرابع (VoLTE) المدمجة في تطبيق **مفعل VoLTE**، مع توضيح آليات نظام أندرويد الداخلية، وحيل تجاوز حظر سامسونج، وأوامر شيل ADB البرمجية، وكود لغة Kotlin المرتبط بها في المشروع.

---

## 📑 جدول المحتويات
1. [مشكلة VoLTE في واجهات سامسونج One UI وأندرويد 13 و 14](#1-مشكلة-volte-في-واجهات-سامسونج-one-ui-وأندرويد-13-و-14)
2. [الطريقة الأولى: اختصار شاشة القفل (طريقة التجاوز الموصى بها - بدون شيزوكو)](#2-الطريقة-الأولى-اختصار-شاشة-القفل-طريقة-التجاوز-الموصى-بها---بدون-شيزوكو)
3. [الطريقة الثانية: المفتاح الجانبي / الضغط المزدوج على زر البور (أندرويد 10-12 فقط)](#3-الطريقة-الثانية-المفتاح-الجانبي--الضغط-المزدوج-على-زر-البور-أندرويد-10-12-فقط)
4. [الطريقة الثالثة: المساعد الرقمي / الضغط المطول على زر الهوم (تعمل 100% عبر شيزوكو)](#4-الطريقة-الثالثة-المساعد-الرقمي--الضغط-المطول-على-زر-الهوم-تعمل-100-عبر-شيزوكو)
5. [الطريقة الرابعة: فحص وإصلاح وتفعيل حزم IMS بضغطة زر](#5-الطريقة-الرابعة-فحص-وإصلاح-وتفعيل-حزم-ims-بضغطة-زر)
6. [أدوات الشبكة، القوائم السرية، وإعدادات نقاط الوصول APN](#6-أدوات-الشبكة-القوائم-السرية-وإعدادات-نقاط-الوصول-apn)
7. [معمارية التكامل مع شيزوكو وصلاحيات UID 2000](#7-معمارية-التكامل-مع-شيزوكو-وصلاحيات-uid-2000)

---

## 1. مشكلة VoLTE في واجهات سامسونج One UI وأندرويد 13 و 14

### نظرة عامة
تتيح ميزة VoLTE إجراء المكالمات الصوتية عالية الدقة عبر شبكة الجيل الرابع LTE والجيل الخامس 5G مباشرة دون التحويل إلى شبكات الجيل الثاني والثالث القديمة. في أجهزة سامسونج، تعتمد هذه الميزة على:
1. **حزم نظام IMS**: وتتضمن (`com.sec.ims`, `com.sec.imsservice`, `com.samsung.advp.imssettings`).
2. **ملفات تعريف المشغل (CSC)**: إعدادات مشغلي الاتصالات المضمنة في النظام. إذا كانت الشريحة تتبع مشغلاً غير مدرج رسمياً من قِبل سامسونج (مثل يمن موبايل)، يتم إيقاف VoLTE افتراضياً.
3. **وضع الاختبار الشامل (GCF MODE)**: خيار اختبار داخلي ضمن واجهة إعدادات IMS يجبر مودم الجهاز على تجاهل قوائم الحظر وتفعيل VoLTE فوراً مع أي شبكة تدعم المعايير العالمية.

### الحاجز الأمني في One UI 5 و 6
في أندرويد 13 و 14، قامت سامسونج بإلغاء تصدير واجهة إعدادات IMS:
- **المكون المستهدف**: `com.samsung.advp.imssettings/.MainActivity`
- **النتيجة**: عند محاولة فتح الواجهة عبر أمر `am start` في ADB يرفض النظام ذلك بالخطأ:
  ```text
  java.lang.SecurityException: Permission Denial: starting Intent { cmp=com.samsung.advp.imssettings/.MainActivity } not exported from uid 10146
  ```

---

## 2. الطريقة الأولى: اختصار شاشة القفل (طريقة التجاوز الموصى بها - بدون شيزوكو)

### كيف تعمل؟ (حيلة SystemUI)
تتيح واجهة قفل الشاشة في سامسونج تعيين تطبيقين في الزاويتين السفلية اليمنى واليسرى (الهاتف والكاميرا افتراضياً):
- عند سحب أيقونة الاختصار من شاشة القفل، يقوم نظام واجهة المستخدم لسامسونج (`SystemUI` بصلاحيات `UID=1000`) بتشغيل الواجهة المطلوبة.
- نظراً لأن `SystemUI` يحمل صلاحيات النظام القصوى، فإنه يستطيع فتح أي واجهة غير مصدرة (Unexported).
- بمجرد حقن إعدادات IMS في الاختصار، يسحب المستخدم الأيقونة لتفتح واجهة IMS فوراً.
- **الأهمية**: هذه الطريقة لا تتطلب شيزوكو ولا روت ولا كمبيوتر، بل تتطلب فقط إذن تعديل إعدادات النظام العادي (`WRITE_SETTINGS`) الذي يمنحه المستخدم من شاشة الإعدادات.

### أوامر ADB البرمجية:

#### 1. قراءة الاختصارات الحالية:
```bash
settings get system lock_application_shortcut
```
*مثال للناتج:*
```text
1;com.samsung.android.dialer/com.samsung.android.dialer.DialtactsActivity;1;com.sec.android.app.camera/com.sec.android.app.camera.Camera;
```

#### 2. تعيين إعدادات IMS للاختصار الأيمن:
```bash
settings put system lock_application_shortcut '1;com.samsung.android.dialer/com.samsung.android.dialer.DialtactsActivity;1;com.samsung.advp.imssettings/.MainActivity;'
```

#### 3. استعادة اختصار الكاميرا الافتراضي:
```bash
settings put system lock_application_shortcut '1;com.samsung.android.dialer/com.samsung.android.dialer.DialtactsActivity;1;com.sec.android.app.camera/com.sec.android.app.camera.Camera;'
```

### الكود المرتبط في التطبيق:
- [`LockscreenShortcutManager.kt`](file:///d:/github_project/enable-volte-app/VoLTEEnabler/app/src/main/java/com/volteenabler/app/methods/LockscreenShortcutManager.kt)

### خطوات تشغيل المستخدم:
1. الضغط على زر **"تعيين إعدادات IMS لشاشة القفل"** داخل التطبيق.
2. قفل الشاشة بالضغط على زر البور، ثم إضاءتها مجدداً.
3. ستظهر أيقونة إعدادات IMS في الزاوية اليمنى السفلية؛ قم بسحبها وإلغاء القفل.
4. داخل إعدادات IMS، قم بتفعيل خيار **"GCF MODE"** ثم اضغط **"OK"**. ستتفعل ميزة VoLTE فوراً.
5. عد إلى التطبيق واضغط على **"استعادة اختصار الكاميرا الافتراضي"**.

---

## 3. الطريقة الثانية: المفتاح الجانبي / الضغط المزدوج على زر البور (أندرويد 10-12 فقط)

### حالة الطريقة وسبب حظرها في One UI 5 و 6 (أندرويد 13/14)
على هواتف **أندرويد 10 إلى 12 (One UI 2–4)**، كان تعديل قيمة `function_key_config_doublepress_value` ينجح في فتح إعدادات IMS عند الضغط مرتين على زر البور.

لكن الهندسة العكسية لحزم One UI 6.1 أثبتت وجود حظر مزدوج من سامسونج في التحديثات الحديثة:
1. **قائمة حظر قاعدة البيانات (`SettingsProvider.apk -> AdbCommandUtils`)**:
   أضافت سامسونج مصفوفة `ADB_CHANGE_NOT_ALLOWED_GLOBAL_SETTINGS` وتتضمن مفاتيح المفتاح الجانبي. عند محاولة التعديل عبر ADB أو شيزوكو يرفض النظام ذلك برسالة:
   ```text
   Invalid command; no put performed
   ```
2. **شرط أيقونة اللانشر (`services.jar -> KeyCustomizationInfoManager`)**:
   عند معالجة الضغط، يتحقق النظام من وجود `android.intent.category.LAUNCHER`. وبما أن إعدادات IMS واجهة مخفية بدون أيقونة لانشر، يتم استبعادها تلقائياً ولا تظهر في قائمة اختيار التطبيقات للمستخدم.

> تفاصيل التفكيك الكاملة متوفرة في ملف: [`decompiled_apks/samsung_sidekey_analysis/README.md`](../../decompiled_apks/samsung_sidekey_analysis/README.md).

### أوامر ADB للأجهزة القديمة (أندرويد 10–12):
```bash
# 1. إيقاف تداخل إيماءة الكاميرا الافتراضية في أندرويد:
adb shell settings put secure camera_double_tap_power_gesture_disabled 1

# 2. تعيين إعدادات IMS للضغط المزدوج:
adb shell settings put global function_key_config_doublepress_value 'com.samsung.advp.imssettings/com.samsung.advp.imssettings.MainActivity'

# 3. تفعيل الضغط المزدوج (النوع 2 = فتح تطبيق مخصص):
adb shell "settings put global function_key_config_doublepress 1; settings put global function_key_config_doublepress_type 2"

# 4. محاكاة الضغط المزدوج:
adb shell input keyevent 26 26

# 5. استعادة الكاميرا:
adb shell "settings put secure camera_double_tap_power_gesture_disabled 0; settings put global function_key_config_doublepress_type 0; settings put global function_key_config_doublepress_value 'com.sec.android.app.camera/com.sec.android.app.camera.Camera'"
```

---

## 4. الطريقة الثالثة: المساعد الرقمي / الضغط المطول على زر الهوم (تعمل 100% عبر شيزوكو)

### كيف تعمل؟
يخزن نظام أندرويد المساعد الرقمي الافتراضي في `Settings.Secure.assistant`. عند تعيين هذا الحقل إلى `com.samsung.advp.imssettings/.MainActivity`، تقوم خدمات المساعد في النظام (`AssistUtils` بصلاحيات `UID=1000`) بتشغيل الواجهة مباشرة.

في واجهة سامسونج One UI، إرسال رمز المفتاح البرمجي `KEYCODE_ASSIST` (219) أو الضغط المطول على زر الشاشة الرئيسية (الهوم) يفتح إعدادات IMS فوراً!

### أوامر ADB وشيزوكو البرمجية:

#### 1. تعيين إعدادات IMS كمساعد رقمي:
```bash
adb shell settings put secure assistant 'com.samsung.advp.imssettings/com.samsung.advp.imssettings.MainActivity'
```

#### 2. إرسال أمر المحاكاة لفتح إعدادات IMS فوراً:
```bash
adb shell input keyevent 219
```

#### 3. استعادة مساعد جوجل الافتراضي:
```bash
adb shell settings put secure assistant 'com.google.android.googlequicksearchbox/com.google.android.apps.gsa.staticplugins.opa.omnihost.OpaRootHostActivity'
```

### الكود المرتبط في التطبيق:
- [`AssistantManager.kt`](file:///d:/github_project/enable-volte-app/VoLTEEnabler/app/src/main/java/com/volteenabler/app/methods/AssistantManager.kt)

---

## 5. الطريقة الرابعة: فحص وإصلاح وتفعيل حزم IMS بضغطة زر

### كيف تعمل؟
في بعض الأجهزة المخصصة لشركات اتصالات معينة أو المعدلة بحذف التطبيقات (Debloated)، قد تقوم برامج إزالة التطبيقات بتعطيل أو حذف خدمات سامسونج الأساسية الخاصة بـ VoLTE (`com.sec.ims`, `com.sec.imsservice`).

باستخدام صلاحيات شيزوكو، يقوم التطبيق بإعادة تثبيت الحزم المحذوفة وتفعيل الحزم المعطلة دون الحاجة لروت عبر أوامر:
```bash
# إعادة تثبيت الحزم المحذوفة للمستخدم الحالي:
cmd package install-existing com.sec.ims
cmd package install-existing com.sec.imsservice
cmd package install-existing com.samsung.advp.imssettings
cmd package install-existing com.sec.imslogger
cmd package install-existing com.sec.epdg

# تفعيل الحزم المعطلة:
pm enable com.sec.ims
pm enable com.sec.imsservice
pm enable com.samsung.advp.imssettings
pm enable com.sec.imslogger
pm enable com.sec.epdg
```

### الكود المرتبط في التطبيق:
- [`ImsRepairManager.kt`](file:///d:/github_project/enable-volte-app/VoLTEEnabler/app/src/main/java/com/volteenabler/app/methods/ImsRepairManager.kt)
- [`ImsPackageInfo.kt`](file:///d:/github_project/enable-volte-app/VoLTEEnabler/app/src/main/java/com/volteenabler/app/data/ImsPackageInfo.kt)

---

## 6. أدوات الشبكة، القوائم السرية، وإعدادات نقاط الوصول APN

### 1. واجهة اختبار الهاتف (الرمز السري `*#*#4636#*#*`)
تفتح واجهة اختبار الهاتف الرسمية في أندرويد لتفعيل خيارات *VoLTE Provisioned*، ومكالمات الواي فاي، وتحديد نمط الشبكة:
```bash
am start -n com.android.settings/.RadioInfo
```

### 2. اختصار نقاط الوصول (APN) وحل مشكلة أندرويد 14
في أندرويد 14 و One UI 6، تقوم واجهة نقاط الوصول في سامسونج بالإغلاق الفوري مع رسالة "Due to SIM absent" إذا لم يتم تمرير معرف الشريحة `sub_id`.
لذلك يجب فتحها مع تمرير المعرف بالشكل التالي:
```bash
am start -a android.settings.APN_SETTINGS --ei sub_id <ACTIVE_SUB_ID>
```
*يقوم التطبيق برمجياً بالاستعلام عن `SubscriptionManager.getDefaultDataSubscriptionId()` وتمريره تلقائياً لمنع إغلاق الشاشة.*

### 3. استعادة زر بيانات الهاتف في الإعدادات السريعة
تقوم بعض ملفات تعريف المشغل بإخفاء مفتاح تشغيل البيانات من ستارة الإشعارات. يمكن إعادته عبر إضافة `MobileData` لقائمة المربعات:
```bash
settings put secure sysui_qs_tiles '<existing_tiles>,MobileData,Hotspot'
```

### 4. إعدادات نقاط الوصول المسبقة (يمن موبايل و VoLTE العالمي)
- **بيانات يمن موبايل (4G)**: اسم ونقطة الوصول `ymdata` مع النوع `default,mms,supl`.
- **مكالمات VoLTE (IMS)**: اسم ونقطة الوصول `ims` مع النوع `ims,default`.
- **خدمات VoLTE الإضافية (XCAP)**: اسم ونقطة الوصول `xcap` مع النوع `xcap`.

---

## 7. معمارية التكامل مع شيزوكو وصلاحيات UID 2000

يعتمد التطبيق على مكتبة Shizuku الرسمية للتواصل مع خدمة النظام:
- فحص الاتصال: `Shizuku.pingBinder()`
- فحص الصلاحية: `Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED`
- تنفيذ الأوامر: `Shizuku.newProcess(cmd, null, null)` عبر خيط معالجة خلفي (`Dispatchers.IO`).
- توفير آلية بديلة كاملة: في حال عدم توفر شيزوكو، ترشد الواجهة المستخدم للاعتماد على **الطريقة الأولى (شاشة القفل)** التي تعمل بكفاءة 100% بدون أي متطلبات إضافية.
