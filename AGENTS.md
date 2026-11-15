# AGENTS.md — QuiCkY Android (`quicky-android`)

Guia operacional para manter consistência arquitetural, de protocolo BLE, UI, build, versionamento e releases entre todos os agentes que trabalharem neste repositório.

## 1. Visão Geral e Relação entre Repositórios
- **Este repositório (`/home/murilo/Documentos/QuickyAndroid`)**: app Android nativo em **Kotlin + Jetpack Compose** (`me.muriloscolari.quicky`, `minSdk = 26`, `compileSdk = 35`, `targetSdk = 35`) para controle via BLE GATT de fones QCY (foco no **QCY MeloBuds Pro**, `vendorId = 19786`).
- **Repositório irmão (`/home/murilo/Documentos/Quicky`)**: implementação Go + KDE Plasma 6 Plasmoid. Use `/home/murilo/Documentos/Quicky/internal/product/products.json` e `/home/murilo/Documentos/Quicky/internal/command/` como **referência oficial dos códigos de comando e pacotes BLE** do ecossistema QCY antes de inventar ou alterar qualquer comando.

## 2. Mapa de Arquitetura
- `app/src/main/java/me/muriloscolari/quicky/ble/QcyProtocol.kt`: constantes BLE (`COMPANY_ID_QCY = 0x521C`, `VENDOR_MELOBUDS_PRO = 19786`), enums (`AncCycleMode`, `AncSubScene`, `FindEarbudSide`, `QcyKeyId`, `QcyKeyAction`), modelos (`QcyBattery`, `QcyAdvertisement`), parsers e construtores de pacotes.
- `app/src/main/java/me/muriloscolari/quicky/ble/QcyBleManager.kt`: singleton central de estado (`QcyUiState` / `StateFlow`), persistência em `SharedPreferences`, fila sequencial GATT (`enqueueGattOp`), reconexão, auto-Modo Sono ao conectar e execução de comandos.
- `app/src/main/java/me/muriloscolari/quicky/ui/QuickyScreen.kt`: interface em Jetpack Compose com suporte a dois estilos (`UiStyle`: **Xiaomi HyperOS** com *glassmorphism/blur* e **Material 3** leve).
- `app/src/main/java/me/muriloscolari/quicky/MainActivity.kt`: permissões, ciclo de vida e fluxo de associação via `CompanionDeviceManager`.
- `app/src/main/java/me/muriloscolari/quicky/service/`:
  - `QcyAncTileService.kt`: Quick Settings Tile (`QCY Ruído`) para ciclar ANC / Ambiente / OFF com subtítulo dinâmico.
  - `QcyCompanionService.kt`: `CompanionDeviceService` para despertar o app/serviço quando o fone associado aparece.
  - `QcyBluetoothReceiver.kt` e `QcyControlService.kt`: recepção de eventos Bluetooth e serviço em primeiro plano/notificação.
- `app/src/main/java/me/muriloscolari/quicky/update/AppUpdater.kt`: auto-updater integrado que consulta releases no **Forgejo** (`murilo/quicky-android`) e no **GitLab** (`muriloscolari/quicky-android`) procurando assets `.apk`.
- `app/src/test/java/me/muriloscolari/quicky/QcyProtocolAndUpdaterTest.kt`: testes unitários JUnit4 para protocolo e updater.

## 3. Regras Críticas de BLE GATT, Protocolo e Compatibilidade
1. **Nunca execute operações GATT em paralelo**: o Android descarta silenciosamente escritas/leituras simultâneas. Toda leitura, escrita de característica (`WriteChar`) ou descritor deve passar pela fila sequencial (`enqueueGattOp`) em `QcyBleManager.kt`.
2. **Não sature a fila BLE com gestos contínuos**: componentes como `Slider` devem atualizar o estado visual localmente durante o arrasto e só despachar o comando BLE ao finalizar a interação (`onValueChangeFinished`).
3. **Bateria do estojo (`caseLevel`)**: nem sempre o fone/estojo envia leitura válida da case (por exemplo quando está fechada ou ambos os fones estão fora). Quando não houver leitura válida, **nunca exiba `0%` falso** — mantenha oculto/indisponível.
4. **Compatibilidade Xiaomi HyperOS vs Samsung OneUI**:
   - No `CompanionDeviceManager` (`MainActivity.kt`), não confie em filtros excessivamente restritivos que quebrem o pareamento em ROMs diferentes (Xiaomi HyperOS escaneia pacotes BLE ativos; Samsung OneUI pode listar dispositivos pareados de forma distinta). Preserve o suporte aos dois fluxos e garanta que o usuário veja nomes legíveis dos dispositivos, nunca apenas MAC bruto quando houver nome disponível.
5. **Quick Settings Tile e app devem compartilhar a mesma lógica de estado**: alterações em modos ANC/Ambiente, memória do último submodo/nível ou estado de conexão devem passar pelos helpers centralizados em `QcyBleManager.kt` para manter app, notificação e tile sincronizados.

## 4. Convenções de UI e Código
- **Idioma da UI**: todos os textos voltados ao usuário, status, labels de controles e notas de release devem estar em **Português (pt-BR)** claro e conciso.
- **Estilo Kotlin**: siga `kotlin.code.style=official`, JVM target 21, evite duplicação de código (extraia helpers/composables compartilhados) e mantenha `QcyProtocol` puro (testável na JVM sem dependências pesadas do Android sempre que possível).
- **Configuração de Build**: `release` usa `isMinifyEnabled = true` e `isShrinkResources = true` (R8 full mode em `gradle.properties`). Sempre valide o build de release antes de finalizar mudanças significativas.

## 5. Build, Testes e Controle de Memória (Obrigatório)
- Sempre execute testes unitários e compile o APK de release após alterações:
  ```bash
  ./gradlew testReleaseUnitTest assembleRelease
  ```
- **Obrigatório encerrar os daemons após o build** para não reter memória RAM (`-Xmx6g` configurado em `gradle.properties`):
  ```bash
  ./gradlew --stop && (pkill -f '[K]otlinCompileDaemon' 2>/dev/null || true)
  ```
- Nunca faça `git add` de arquivos `.apk` (eles já estão ignorados em `.gitignore`).

## 6. Versionamento, Git e Publicação de Releases (Forgejo + GitLab)
Este repositório usa **Forgejo** e **GitLab** em paralelo:
- `forgejo`: `https://git.muriloscolari.me/murilo/quicky-android.git`
- `gitlab`: `https://gitlab.com/muriloscolari/quicky-android.git`

Ao preparar uma nova versão `vX.Y.Z`:
1. Incremente `versionCode` e `versionName` em `app/build.gradle.kts`.
2. Rode `./gradlew testReleaseUnitTest assembleRelease` e pare os daemons.
3. Copie o APK gerado para o nome padronizado:
   ```bash
   cp app/build/outputs/apk/release/app-release.apk quicky-android-vX.Y.Z.apk
   ```
4. Faça o commit (mensagens claras estilo Conventional Commits), crie a tag anotada `vX.Y.Z` e envie **para ambos** os remotes:
   ```bash
   git push forgejo main vX.Y.Z
   git push gitlab main vX.Y.Z
   ```
5. **Publique a release com o `.apk` anexado em ambos os hosts** (o `AppUpdater.kt` depende de haver um asset `.apk` na release mais recente de cada plataforma):
   - Forgejo: `tea release create --repo murilo/quicky-android --tag vX.Y.Z --title "..." --note "..." --asset quicky-android-vX.Y.Z.apk`
   - GitLab: `glab release create vX.Y.Z quicky-android-vX.Y.Z.apk --repo muriloscolari/quicky-android --name "..." --notes "..."`
