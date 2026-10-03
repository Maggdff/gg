# English for IT 1 — تطبيق مراجعة (أوفلاين)

تطبيق أندرويد (Kotlin + Jetpack Compose) للمراجعة على كتاب *English for Information Technology 1* (الوحدتان 1-2) والملازم الإضافية.
بدون إنترنت، بدون تسجيل دخول، بدون إعلانات.

## البناء على GitHub (بدون Android Studio)
1. أنشئ مستودع جديد على GitHub (Repository جديد، Public أو Private).
2. ارفع **كل محتويات** هذا المجلد (Add file → Upload files)، وتأكد أن المجلد المخفي `.github/workflows/build.yml` اتضاف.
   - لو المجلد المخفي ما اترفعش: Add file → Create new file → اكتب الاسم `.github/workflows/build.yml` والصق محتوى الملف.
3. ادخل تبويب **Actions** — هيبدأ بناء "Build APK" تلقائياً (3-6 دقائق).
4. بعد النجاح: ادخل **Releases** (يمين صفحة المستودع) ونزّل `app-debug.apk` من المتصفح على الموبايل مباشرة.
   (أو من Actions → آخر تشغيل → Artifacts → EnglishIT-apk).
5. ثبّت الـ APK (فعّل "التثبيت من مصادر غير معروفة" لو طلب).

## البناء على Android Studio
افتح المجلد → Sync → Run. (JDK 17، Android SDK 35).

## إضافة محتوى
كل المحتوى في `app/src/main/assets/` بصيغة JSON:
- `u1.json`, `u2.json`, `extra.json`: `{"units":[{"id","title","titleAr","lessons":[{"id","title","titleAr","text"?,"boxes"?,"auto"?,"ex":[...]}]}]}`
- أنواع التمارين (`t`):
  - `mcq`: `{"t":"mcq","q":"..","opts":[..],"a":<index الصحيح>,"exp":"شرح عربي","g":"grammarId"}`
  - `fill`: `{"t":"fill","q":"... ___ ...","a":["إجابة","بديل"],"b":"d"?,"exp":".."}` (`b` = اسم صندوق كلمات من `boxes`)
  - `tf`: `{"t":"tf","q":"..","a":true}`
  - `order`: `{"t":"order","q":"..","a":"الجملة الصحيحة","extra":["كلمة مشتتة"]}`
  - `match`: `{"t":"match","q":"..","pairs":[["a","b"],...]}`
  - `write`: `{"t":"write","q":"..","model":"نموذج الإجابة"}` (تقييم ذاتي)
- `auto`: `{"cats":["vocab","term"],"n":18,"match":2}` يولّد أسئلة تلقائياً من `vocab.json`.
- `vocab.json`: `{"items":[{"en","ar","def","cat"}]}` — `cat` ∈ vocab, term, acronym, hw, adj, def
- `grammar.json`: `{"items":[{"id","title","titleAr","notesAr","rules":[],"examples":[]}]}`

## اختبارات الوحدة
`gradle :app:testDebugUnitTest` (مدقق الإجابات + جدولة التكرار المتباعد).
