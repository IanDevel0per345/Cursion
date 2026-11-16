#!/usr/bin/env bash
# Executa os testes instrumentados no aparelho/emulador conectado (adb). Não roda sem dispositivo.
set -euo pipefail
cd "$(dirname "$0")/.."
adb get-state >/dev/null 2>&1 || { echo "Nenhum dispositivo adb conectado."; exit 1; }
./gradlew :app:installDebug :playground:installDebug
SERVICE="com.opencell.app/com.opencell.app.device.OpenCellAccessibilityService"
adb shell pm grant com.opencell.app android.permission.POST_NOTIFICATIONS 2>/dev/null || true
adb shell settings put secure enabled_accessibility_services "$SERVICE"
adb shell settings put secure accessibility_enabled 1
sleep 2
./gradlew :app:connectedDebugAndroidTest
