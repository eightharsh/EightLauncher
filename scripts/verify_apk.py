#!/usr/bin/env python3
"""Check the built APK identity and privacy-sensitive merged permissions."""
import os
from pathlib import Path
import shutil
import subprocess
import sys
import zipfile

if len(sys.argv) != 3:
    raise SystemExit('Usage: verify_apk.py APK EXPECTED_APPLICATION_ID')
apk, expected_id = sys.argv[1:]
analyzer = shutil.which('apkanalyzer')
if not analyzer:
    sdk = os.environ.get('ANDROID_HOME') or os.environ.get('ANDROID_SDK_ROOT')
    if sdk:
        candidate = Path(sdk) / 'cmdline-tools/latest/bin/apkanalyzer'
        if candidate.is_file():
            analyzer = str(candidate)
if not analyzer:
    raise SystemExit('apkanalyzer is required; install Android SDK command-line tools.')

def manifest_value(field):
    return subprocess.check_output(
        [analyzer, 'manifest', field, apk], text=True
    ).strip()

actual_id = manifest_value('application-id')
if actual_id != expected_id:
    raise SystemExit(f'Unexpected application ID: {actual_id!r}; expected {expected_id!r}')
permissions = set(manifest_value('permissions').splitlines())
forbidden = {
    'android.permission.ACCESS_COARSE_LOCATION',
    'android.permission.ACCESS_FINE_LOCATION',
    'android.permission.ACCESS_BACKGROUND_LOCATION',
}
unexpected = permissions & forbidden
if unexpected:
    raise SystemExit(f'Unexpected permissions in APK: {sorted(unexpected)}')
with zipfile.ZipFile(apk) as archive:
    for name in archive.namelist():
        if name.startswith('classes') and name.endswith('.dex'):
            dex = archive.read(name)
            for marker in (
                b'Lcom/google/firebase/FirebaseApp;',
                b'Lcom/google/firebase/analytics/',
                b'Lcom/google/firebase/crashlytics/',
                b'Lcom/google/firebase/perf/',
                b'Lcom/google/firebase/messaging/',
                b'Lcom/google/android/gms/measurement/',
            ):
                if marker in dex:
                    raise SystemExit(f'Telemetry SDK reference found in {name}: {marker!r}')
print(f'APK verified: {actual_id}; no Firebase initialization or telemetry SDK references, location permissions.')
