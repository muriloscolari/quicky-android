# QuiCkY Android (QCY Earphones Controller)

Aplicativo Android nativo em **Kotlin + Jetpack Compose** para controle de fones Bluetooth QCY (com suporte completo ao **QCY MeloBuds Pro** via BLE GATT), compilado com otimização máxima do **R8**.

## Recursos Principais

- **Auto-Modo Sono ao Conectar (Bloquear Toques na Lateral)**:
  - Quando ativado, sempre que o fone conecta ao celular o app envia automaticamente o comando BLE `0x10` (`[0xFF, 0x03, 0x10, 0x01, 0x01]`) para desativar os controles de toque na lateral do fone.
- **Botão na Barra Rápida do Android (Quick Settings Tile)**:
  - Adicione o botão **QCY Ruído** no painel rápido do Android para ciclar entre **ANC → Ambiente → OFF** com 1 toque (também disponível como botão na notificação).
- **Dois Estilos de Interface**:
  - **Xiaomi HyperOS (Padrão)**: interface com *blur* translúcido (*glassmorphism*) e cápsulas inspiradas na Central de Controle HyperOS.
  - **Material 3 (Leve)**: interface em superfícies sólidas sem *blur* para máxima leveza em aparelhos mais simples.
- **Auto-Updater Integrado (Forgejo & GitLab)**:
  - Escolha nas configurações entre **Forgejo (`git.muriloscolari.me`)** e **GitLab (`gitlab.com`)** para buscar, baixar e instalar novas releases automaticamente.

## Compilação (R8 Release)

```bash
gradle test assembleRelease
```

O APK otimizado pelo R8 é gerado em:
`app/build/outputs/apk/release/app-release.apk`
