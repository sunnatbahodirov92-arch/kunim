#!/usr/bin/env bash
# Release APK uchun imzo kaliti (keystore) yaratadi. Bir marta ishlating va kalitni YO'QOTMANG!
set -e
command -v keytool >/dev/null || { echo "keytool topilmadi: JDK o'rnating (https://adoptium.net)"; exit 1; }
keytool -genkeypair -v -keystore kunim.jks -alias kunim \
  -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=KUNIM, O=KUNIM, C=UZ"
base64 -w0 kunim.jks 2>/dev/null > kunim.jks.base64.txt || base64 kunim.jks | tr -d '\n' > kunim.jks.base64.txt
echo
echo "Tayyor. 2 ta fayl yaratildi:"
echo "  kunim.jks             — kalitning o'zi (zaxira nusxasini xavfsiz joyda saqlang, GitHub'ga YUKLAMANG)"
echo "  kunim.jks.base64.txt  — ichidagi matnni KUNIM_KEYSTORE_BASE64 secret'iga qo'ying"
echo "Boshqa secret'lar: KUNIM_STORE_PASS (keystore paroli), KUNIM_KEY_ALIAS = kunim, KUNIM_KEY_PASS (kalit paroli)"
