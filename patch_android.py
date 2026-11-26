"""`npx cap add android` dan keyin Android loyihasini KUNIM uchun sozlaydi.
Ishga tushirish: python3 scripts/patch_android.py  (repozitoriy ildizidan)
"""
import glob, json, os, re, shutil, sys
from PIL import Image

TEAL = '#0E7C86'
A = 'android/app/src/main'
PKG_DIR = f'{A}/java/uz/kunim/app'
RES = f'{A}/res'


def read(p):
    return open(p, encoding='utf8').read()


def write(p, s):
    os.makedirs(os.path.dirname(p), exist_ok=True)
    open(p, 'w', encoding='utf8').write(s)


def need(cond, msg):
    if not cond:
        sys.exit('XATO: ' + msg)


# ---------- 1) AndroidManifest: ruxsatlar va komponentlar ----------
m = read(f'{A}/AndroidManifest.xml')
perms = ['POST_NOTIFICATIONS', 'VIBRATE', 'WAKE_LOCK', 'USE_EXACT_ALARM', 'SCHEDULE_EXACT_ALARM',
         'RECEIVE_BOOT_COMPLETED', 'USE_FULL_SCREEN_INTENT', 'FOREGROUND_SERVICE',
         'FOREGROUND_SERVICE_MEDIA_PLAYBACK']
add = ''.join(f'    <uses-permission android:name="android.permission.{p}"/>\n'
              for p in perms if f'android.permission.{p}"' not in m)
comps = '''
        <service android:name=".AlarmService" android:exported="false"
            android:foregroundServiceType="mediaPlayback" />
        <receiver android:name=".AlarmReceiver" android:exported="false" />
        <receiver android:name=".BootReceiver" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
                <action android:name="android.intent.action.MY_PACKAGE_REPLACED" />
            </intent-filter>
        </receiver>
        <activity android:name=".AlarmActivity" android:exported="false"
            android:launchMode="singleTop" android:excludeFromRecents="true"
            android:showWhenLocked="true" android:turnScreenOn="true"
            android:theme="@android:style/Theme.Material.NoActionBar" />
'''
if '.AlarmService' not in m:
    need('</application>' in m, 'manifestda </application> topilmadi')
    m = m.replace('</application>', comps + '    </application>')
if add:
    m = m.replace('</manifest>', add + '</manifest>')
write(f'{A}/AndroidManifest.xml', m)

# ---------- 2) Java: native budilnik ----------
need(os.path.isdir(PKG_DIR), 'java/uz/kunim/app papkasi yo\'q (appId o\'zgargan?)')
for f in glob.glob('native/*.java'):
    shutil.copy(f, os.path.join(PKG_DIR, os.path.basename(f)))

# ---------- 3) resurslar: ovoz, bildirishnoma ikonkasi, ranglar ----------
os.makedirs(f'{RES}/raw', exist_ok=True)
shutil.copy('resources/alarm.wav', f'{RES}/raw/alarm.wav')
os.makedirs(f'{RES}/drawable', exist_ok=True)
shutil.copy('resources/ic_stat_kunim.png', f'{RES}/drawable/ic_stat_kunim.png')
write(f'{RES}/values/kunim_colors.xml',
      f'<?xml version="1.0" encoding="utf-8"?>\n<resources>\n    <color name="kunim_teal">{TEAL}</color>\n</resources>\n')

# ---------- 4) mavzu: status bar + splash ----------
st = read(f'{RES}/values/styles.xml')
need('AppTheme.NoActionBar"' in st and 'AppTheme.NoActionBarLaunch' in st, 'styles.xml shabloni kutilgandan farq qiladi')
launch_parent = re.search(r'name="AppTheme\.NoActionBarLaunch"\s+parent="([^"]+)"', st)
launch_parent = launch_parent.group(1) if launch_parent else 'Theme.SplashScreen'
splash_items = ''
if 'SplashScreen' in launch_parent:
    splash_items = '''
        <item name="windowSplashScreenBackground">@color/kunim_teal</item>
        <item name="windowSplashScreenAnimatedIcon">@drawable/ic_splash</item>
        <item name="postSplashScreenTheme">@style/AppTheme.NoActionBar</item>'''
write(f'{RES}/values/styles.xml', f'''<?xml version="1.0" encoding="utf-8"?>
<resources>

    <style name="AppTheme" parent="Theme.AppCompat.Light.DarkActionBar">
        <item name="colorPrimary">@color/colorPrimary</item>
        <item name="colorPrimaryDark">@color/colorPrimaryDark</item>
        <item name="colorAccent">@color/colorAccent</item>
    </style>

    <style name="AppTheme.NoActionBar" parent="Theme.AppCompat.DayNight.NoActionBar">
        <item name="windowActionBar">false</item>
        <item name="windowNoTitle">true</item>
        <item name="android:background">@null</item>
        <item name="android:windowBackground">@color/kunim_teal</item>
        <item name="android:statusBarColor">@color/kunim_teal</item>
        <item name="android:navigationBarColor">@color/kunim_teal</item>
        <item name="android:windowLightStatusBar">false</item>
    </style>

    <style name="AppTheme.NoActionBarLaunch" parent="{launch_parent}">{splash_items}
        <item name="android:background">@drawable/splash</item>
    </style>
</resources>
''')

# splash: bitta drawable (eski rasmli splash.png fayllarini o'chiramiz)
for f in glob.glob(f'{RES}/drawable*/splash.png'):
    os.remove(f)
write(f'{RES}/drawable/splash.xml', '''<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:drawable="@color/kunim_teal" />
    <item>
        <bitmap android:src="@drawable/ic_splash" android:gravity="center" />
    </item>
</layer-list>
''')
src = Image.open('resources/icon-512.png').convert('RGBA')
canvas = Image.new('RGBA', (960, 960), (0, 0, 0, 0))   # Android 12: ichki doira ~640px
canvas.paste(src.resize((452, 452), Image.LANCZOS), (254, 254), src.resize((452, 452), Image.LANCZOS))
canvas.save(f'{RES}/drawable/ic_splash.png')

# ---------- 5) ikonkalar: oddiy + adaptiv ----------
for d, sz in {'mdpi': 48, 'hdpi': 72, 'xhdpi': 96, 'xxhdpi': 144, 'xxxhdpi': 192}.items():
    os.makedirs(f'{RES}/mipmap-{d}', exist_ok=True)
    for n in ('ic_launcher.png', 'ic_launcher_round.png'):
        src.resize((sz, sz), Image.LANCZOS).save(f'{RES}/mipmap-{d}/{n}')
fg = Image.new('RGBA', (432, 432), (0, 0, 0, 0))      # adaptiv ikonka 108dp x4
ic = src.resize((300, 300), Image.LANCZOS)
fg.paste(ic, (66, 66), ic)
os.makedirs(f'{RES}/drawable-nodpi', exist_ok=True)
fg.save(f'{RES}/drawable-nodpi/ic_launcher_fg.png')
adaptive = '''<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/kunim_teal" />
    <foreground android:drawable="@drawable/ic_launcher_fg" />
</adaptive-icon>
'''
os.makedirs(f'{RES}/mipmap-anydpi-v26', exist_ok=True)
write(f'{RES}/mipmap-anydpi-v26/ic_launcher.xml', adaptive)
write(f'{RES}/mipmap-anydpi-v26/ic_launcher_round.xml', adaptive)

# ---------- 6) build.gradle: versiya + imzolash ----------
pj = json.load(open('package.json', encoding='utf8'))
vname = pj.get('version', '1.0.0')
vcode = 1000 + int(os.environ.get('GITHUB_RUN_NUMBER', '1'))
g = read('android/app/build.gradle')
g, n1 = re.subn(r'versionCode\s+\d+', f'versionCode {vcode}', g, count=1)
g, n2 = re.subn(r'versionName\s+"[^"]*"', f'versionName "{vname}"', g, count=1)
need(n1 and n2, 'build.gradle da versionCode/versionName topilmadi')
if 'KUNIM_KEYSTORE' not in g:
    g += '''

// --- KUNIM: release imzolash (GitHub Secrets orqali) va lint ---
android {
    lint {
        checkReleaseBuilds false
        abortOnError false
    }
}
def kunimStore = System.getenv("KUNIM_KEYSTORE")
if (kunimStore) {
    android {
        signingConfigs {
            release {
                storeFile file(kunimStore)
                storePassword System.getenv("KUNIM_STORE_PASS")
                keyAlias System.getenv("KUNIM_KEY_ALIAS")
                keyPassword System.getenv("KUNIM_KEY_PASS")
            }
        }
        buildTypes {
            release {
                signingConfig signingConfigs.release
            }
        }
    }
}
'''
write('android/app/build.gradle', g)
print(f'patched: v{vname} ({vcode}), splash={"SplashScreen" in launch_parent}')
