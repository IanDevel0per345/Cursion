# Build e execução

1. JDK 17+ e Android SDK (`sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"`); `sdk.dir` em `local.properties` (não versionado).
2. `./gradlew :agent-core:test`
3. `./gradlew :app:assembleDebug :playground:assembleDebug`
4. `adb install -r app/build/outputs/apk/debug/app-debug.apk` e idem para o Playground.
5. Ative a acessibilidade manualmente (ou `scripts/e2e.sh`, que usa `adb shell settings put secure ...`, válido em emulador/aparelho de desenvolvimento).
6. Provedor: configure no app; **chaves não vão para o repositório** (ficam cifradas no aparelho).
7. Teste guiado: Home › Teste guiado. Ele abre o Playground e rola a lista.
8. Testes instrumentados: `scripts/e2e.sh`.
9. Logs: o app não emite logs próprios de conteúdo e não grava screenshots; use `adb logcat` apenas para crashes do sistema. Não há modo de exportação de diagnóstico ainda.
O APK é de **debug**; não há build de release assinado.
