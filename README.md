# IPTV Player TV

تطبيق مشغل IPTV لأجهزة Android TV، بيدعم استيراد القنوات عن طريق **رابط M3U** أو **Xtream Codes API**، مع مفضلة وتصنيفات وواجهة مخصصة للريموت كنترول (Leanback).

> ⚠️ التطبيق ده مجرد **مشغل/عميل** (زي VLC أو Kodi) — مبيوفرش أي محتوى أو قنوات بنفسه. أنت اللي بتدخل رابط أو بيانات مصدر البث بتاعك، والقانونية بتاعة الاستخدام بترجع لمصدر البث نفسه اللي هتستخدمه.

## المميزات في النسخة دي
- استيراد قوائم M3U (رابط مباشر)
- تسجيل دخول عبر Xtream Codes API (host / port / username / password)
- مفضلة للقنوات + تصنيفها تلقائياً حسب الفئات (group-title)
- واجهة Leanback مخصصة تشتغل بالريموت
- تشغيل الفيديو عبر ExoPlayer (يدعم HLS و MPEG-TS)

## طريقة عمل Build وتحميل الـ APK (أونلاين، من غير تثبيت أي حاجة)

### 1. ارفع المشروع على GitHub
1. اعمل حساب على [github.com](https://github.com) لو مالكش واحد.
2. اعمل Repository جديد (خليه Private لو عايز).
3. ارفع كل ملفات المشروع ده (اسحبها وأفلتها في صفحة الـ repo، أو استخدم git):
   ```bash
   git init
   git add .
   git commit -m "IPTV Player TV - initial version"
   git branch -M main
   git remote add origin https://github.com/USERNAME/REPO_NAME.git
   git push -u origin main
   ```

### 2. الـ build هيشتغل تلقائي
بمجرد الرفع على branch `main`، هيشتغل GitHub Actions تلقائياً (الملف موجود في `.github/workflows/build-apk.yml`) ويعمل build للمشروع.

### 3. تحميل الـ APK
1. روح لتبويب **Actions** في صفحة الـ repo بتاعتك.
2. هتلاقي الـ workflow اسمه "Build APK" شغال أو خلص.
3. افتحه، وهتلاقي تحت في قسم **Artifacts** ملف اسمه `iptv-player-tv-debug-apk`.
4. حمله (هيجيلك كـ zip فيه ملف الـ APK جواه).

### 4. تثبيت الـ APK على تلفازك
- **الطريقة الأسهل:** حمّل تطبيق [Downloader](https://play.google.com/store/apps/details?id=com.esaba.downloader) من متجر Google TV، وارفع الـ APK بتاعك على أي خدمة استضافة ملفات (زي Google Drive برابط مباشر) وحمله من خلاله على التلفاز مباشرة.
- **أو عن طريق USB:** انسخ ملف الـ APK على فلاشة USB، وثبته من خلال أي File Manager على التلفاز.
- **أو عن طريق ADB** (لو التلفاز والكمبيوتر على نفس الشبكة):
  ```bash
  adb connect TV_IP_ADDRESS
  adb install app-debug.apk
  ```

## طريقة استخدام التطبيق
أول ما تفتح التطبيق هيوديك لشاشة الإعداد، هتختار:
- **Xtream Codes:** هتحط الـ Host، الـ Port، الـ Username، والـ Password اللي بتوصلك من مزود الخدمة.
- **رابط M3U:** هتحط رابط ملف الـ M3U مباشرة.

بعد التحميل، هتلاقي القنوات متقسمة لصفوف حسب التصنيف، وتقدر تضغط على أي قناة تشوف خيارين: تشغيل، أو إضافة/إزالة من المفضلة.

## تطوير المشروع لاحقاً (أفكار لو عايز تضيف)
- دعم دليل البرامج EPG (XMLTV)
- البحث عن قناة بالاسم
- دعم أفلام ومسلسلات VOD من Xtream Codes (get_vod_streams / get_series)
- شاشة تسجيل دخول تحفظ أكتر من حساب/مصدر بيانات
- توقيع release APK موقّع (بدل نسخة debug) — ممكن نضيف خطوة signing لملف الـ workflow

## هيكل المشروع
```
IPTVPlayerTV/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/iptvtv/player/
│       │   ├── MainActivity.kt
│       │   ├── SetupActivity.kt
│       │   ├── PlaybackActivity.kt
│       │   ├── ChannelsBrowseFragment.kt
│       │   ├── model/Channel.kt
│       │   ├── data/PlaylistRepository.kt
│       │   ├── parser/M3UParser.kt
│       │   ├── api/XtreamApiClient.kt
│       │   └── presenter/ChannelCardPresenter.kt
│       └── res/ (layouts, values, drawables)
├── .github/workflows/build-apk.yml
├── build.gradle.kts
└── settings.gradle.kts
```
