KUNIM v1.1 — Android ilova (APK) loyihasi
==========================================

1) APK olish (eng oson yo'l)
   a) GitHub'da yangi repozitoriy yarating (Private tavsiya etiladi).
   b) Shu papkadagi HAMMA narsani yuklang (www, native, scripts, resources, .github,
      package.json, package-lock.json, capacitor.config.json, .gitignore).
   c) Actions bo'limida "Build APK" tugashini kuting (yashil galochka, ~5-8 daqiqa).
   d) Pastdagi "KUNIM-apk" ni yuklab oling -> telefonga o'rnating.
   Imzo kaliti bo'lmasa, KUNIM-vX-debug.apk chiqadi (sinash uchun).

2) Imzolangan RELEASE APK (yangilash va Play Market uchun)
   a) Kompyuterda JDK o'rnating (https://adoptium.net), so'ng:
        bash scripts/make_keystore.sh
      (Windows PowerShell uchun: keytool -genkeypair -v -keystore kunim.jks -alias kunim
       -keyalg RSA -keysize 2048 -validity 10000, so'ng:
       [Convert]::ToBase64String([IO.File]::ReadAllBytes("kunim.jks")) | Set-Content kunim.jks.base64.txt)
   b) GitHub: Settings -> Secrets and variables -> Actions -> New repository secret. 4 ta secret:
        KUNIM_KEYSTORE_BASE64 = kunim.jks.base64.txt ichidagi matn
        KUNIM_STORE_PASS      = keystore paroli
        KUNIM_KEY_ALIAS       = kunim
        KUNIM_KEY_PASS        = kalit paroli
   c) Actions -> Build APK -> Run workflow. Natija: KUNIM-vX.apk va KUNIM-vX.aab (Play Market uchun).
   d) "v1.1.0" kabi teg qo'ysangiz, fayllar Releases bo'limiga ham chiqadi.
   MUHIM: kunim.jks faylini xavfsiz joyda saqlang va GitHub'ga yuklamang.
   Yo'qotsangiz, ilovani yangilab bo'lmaydi (qayta o'rnatish kerak bo'ladi).

3) Yangi versiya chiqarish
   package.json dagi "version" ni oshiring (va www/index.html dagi APP_VER ni ham). versionCode har build'da avtomatik oshadi.

4) Telefonda birinchi ishga tushirish
   Sozlamalar (⚙️) -> "Ruxsat berish" tugmasini bosing. Ilova ketma-ket so'raydi:
   bildirishnoma, aniq budilnik (Android 12+) va to'liq ekran (Android 14+) ruxsatlari.
   Ruxsatlar berilmaguncha tugma ko'rinib turadi.

Nima yangi (v1.1)
-----------------
- Haqiqiy budilnik: AlarmManager + 60 soniya takrorlanuvchi signal + tebranish + qulf ekranida
  to'liq ekranli oyna (Ochish / Keyinroq / To'xtatish). Telefon qayta yoqilsa ham tiklanadi.
- Ilova ochiqligida signalni ilovaning o'zi chaladi, yopiq bo'lsa native servis (ikki marta chalinmaydi).
- Rasmlar IndexedDB'da (localStorage ~5 MB chegarasi yo'q); eski rasmlar avtomatik ko'chadi.
- Shrift ilova ichida (offline), Google Fonts'ga bog'liqlik yo'q.
- Android "Orqaga" tugmasi: oyna yopadi -> jadvalga qaytadi -> ilovani minimallashtiradi.
- Splash ekran, adaptiv ikonka, status bar rangi, Android 15 edge-to-edge sozlamasi.
- 🔥 Ketma-ket kunlar (hamma ishlar bajarilgan kunlar seriyasi) hisobotda.
- Paket versiyalari package-lock.json bilan qotirilgan (build kutilmaganda buzilmaydi).

Loyiha tuzilishi
----------------
www/        — ilovaning o'zi (index.html, shriftlar, PWA fayllari)
native/     — Android budilnik kodi (Java), build paytida android/ ichiga ko'chiriladi
scripts/    — patch_android.py (manifest, mavzu, ikonka, imzo), make_keystore.sh
resources/  — budilnik ovozi va ikonkalar
