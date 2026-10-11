# Dashboard 112 — پروژه کامل Android Studio (Kotlin & Jetpack Compose)

- **نام برنامه**: Dashboard 112
- **شناسه بسته (applicationId)**: ir.dashboard112.app
- **نسخه**: 1.2.0 (versionCode = 2)
- **حداقل نسخه اندروید**: Android 8.0 (API 26) — پوشش بیش از ۹۷٪ دستگاه‌های فعال
- **نسخه هدف و کامپایل**: Android 15 (API 35)
- **معماری**: MVVM + Repository + Room Database (با MIGRATION_1_2) + DataStore + AlarmManager

## نحوه ساخت (Build) و تولید APK در Android Studio یا خط فرمان

1. پوشه پروژه را در **Android Studio Ladybug (2024.2.1)** یا بالاتر باز کنید.
2. برای تولید نسخه Debug APK دستور زیر را در ترمینال اجرا کنید:
```bash
./gradlew clean assembleDebug
```
مسیر خروجی فایل APK قابل نصب:
`app/build/outputs/apk/debug/app-debug.apk`

3. برای اجرای تست‌های واحد (Unit Tests) و بررسی Lint:
```bash
./gradlew testDebugUnitTest lintDebug
```

4. برای نصب مستقیم روی گوشی یا شبیه‌ساز متصل از طریق ADB:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
