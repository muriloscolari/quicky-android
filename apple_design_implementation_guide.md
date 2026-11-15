# Guia Técnico: Design Estilo Apple (Liquid Glass) — Problemas Resolvidos & Como Replicar em Outros Apps

Este documento registra **todos os problemas reais enfrentados** durante a implementação do design Apple Liquid Glass no **UNICEP Portal** (Compose Multiplatform para Android + Web Wasm com `dev.chrisbanes.haze`) e apresenta um **guia prático e agnóstico** para aplicar a mesma estética e arquitetura em qualquer outro aplicativo.

---

## Parte 1 — Problemas Enfrentados e Como Foram Resolvidos

### 1. Crash na inicialização no Android por fontes ausentes (`NotFoundException`)
- **Sintoma:** O app compilava em Release, mas fechava imediatamente ao abrir no celular com erro ao carregar a família tipográfica `Inter`.
- **Causa raiz:** Ao usar o plugin `com.android.kotlin.multiplatform.library` no módulo compartilhado (`composeApp`), os arquivos `.ttf` em `commonMain/composeResources/font/` não são empacotados automaticamente em `assets/composeResources/...` do APK final se os Android resources não estiverem habilitados no target Android da biblioteca KMP.
- **Solução:**
  1. Habilitar explicitamente `androidResources.enable = true` dentro de `extensions.configure<KotlinMultiplatformAndroidLibraryExtension>("android")` em `composeApp/build.gradle.kts`.
  2. Sempre validar o APK release gerado conferindo a presença dos arquivos de fonte:
     ```bash
     unzip -l app/androidApp/build/outputs/apk/release/androidApp-release.apk | grep inter_
     ```

---

### 2. Crash no `hazeGlass`: `IllegalArgumentException: lightingIntensity must be finite and in 0f..1f`
- **Sintoma:** O app abria, mas fechava sozinho logo ao renderizar a primeira superfície de vidro (`GlassSurface`).
- **Causa raiz:** A API `GlassStyle` da biblioteca Haze exige rigorosamente que todos os floats de iluminação (`specularIntensity`, `ambientResponse`, `lightingIntensity`) sejam finitos e estejam dentro de `0f..1f`. Valores acima de `1f` (como `1.35f` no estado `pressed` e `1.15f` no estado `hovered`) disparam `IllegalArgumentException` em runtime.
- **Solução:** Manter todos os parâmetros dentro de `0f..1f` e validar que nenhum valor calculado possa ser `NaN` ou `Infinity`.

---

### 3. Brilho indesejado seguindo o cursor do mouse no Desktop / Web
- **Sintoma:** Ao passar o mouse sobre sheets ou barras em vidro no navegador/desktop, aparecia um foco de luz circular brilhante acompanhando o ponteiro.
- **Causa raiz:** `GlassStyle.regular` / estado `hovered` do `hazeGlass` aplica `lightingIntensity` interativo no ponto do ponteiro.
- **Solução:** Sobrescrever explicitamente `lightingIntensity(0f)` nos blocos `hovered { ... }` e `pressed { ... }` do `GlassStyle` (mantendo apenas `whitePointDelta` sutil no `pressed`). Isso remove o holofote do cursor preservando o blur, a refração e a borda especular.

---

### 4. Crash ao fechar menus e sheets: `Corner size in Px can't be negative`
- **Sintoma:** Ao fechar um modal/sheet (`AppleSheet`), o app fechava com:
  `java.lang.IllegalArgumentException: Corner size in Px can't be negative(topStart = -0.009661192, ...)`
- **Causa raiz:** As molas de animação (`AppleMotion`) usavam `dampingRatio < 1.0f` (`0.86f`, `0.72f`, `0.90f`). Molas subamortecidas (*underdamped*) oscilam levemente além do alvo (*overshoot*). Quando `bgCorner` (ou `cornerBottom` / `sideInset` / `bottomInset`) animava de `28.dp` ou `34.dp` de volta para `0.dp`, o valor passava brevemente para `-0.003.dp`, o que faz `RoundedCornerShape` e `Modifier.padding` lançarem `IllegalArgumentException`.
- **Solução:**
  1. Alterar todas as molas que animam dimensões, cantos ou preenchimentos para **amortecimento crítico** (`dampingRatio = 1.0f` / `Spring.DampingRatioNoBouncy`), eliminando *overshoot* negativo.
  2. Aplicar blindagem defensiva em todos os consumidores animados:
     - `RoundedCornerShape(corner.coerceAtLeast(0.dp))`
     - `Modifier.padding(...)` com `.coerceAtLeast(0.dp)`
     - `Modifier.height(...)` / `width(...)` com `.coerceAtLeast(1.dp)` ou `0.dp`
     - `Color.copy(alpha = alpha.coerceIn(0f, 1f))`
     - `fillMaxWidth(ratio.coerceIn(0f, 1f))` (verificando `isFinite()`).

---

### 5. Conflito de `hazeGlass` dentro de `Popup` (`GlassMenu`)
- **Sintoma:** Menus suspensos (`Popup`) compartilhando o `HazeState` da tela principal podem falhar ou renderizar artefatos no Android porque `Popup` cria uma `Window` separada da `Activity` onde o `hazeSource` vive.
- **Solução:** Adicionar uma flag `useHaze: Boolean = true` em `GlassSurface` e passar `useHaze = false` em componentes renderizados dentro de `Popup`, usando o fallback translúcido sólido com borda linear sutil.

---

### 6. `RadialGradient` com raio zero no primeiro frame de layout
- **Sintoma:** Potencial crash no Android (`android.graphics.RadialGradient: radius must be > 0`) caso um `Canvas` seja desenhado antes de receber dimensões finais (`size.width == 0f`).
- **Solução:** Em todo `Canvas` com gradientes radiais (como `AppleMeshBackdrop`), fazer retorno antecipado (`if (w <= 0f || h <= 0f) return@Canvas`) e garantir `radiusBase.coerceAtLeast(1f)`.

---

### 7. Tela preta na Web (Wasm) após deploy por cache de CDN + SPA Fallback
- **Sintoma:** Após publicar uma nova versão do container atrás da Cloudflare, o navegador ficava em tela preta (`canvasCount = 0`) com `Uncaught (in promise)`.
- **Causa raiz combinada:**
  1. O servidor Go só enviava `Cache-Control` para `.wasm`. Como `.js` e `.html` não tinham política explícita, a Cloudflare cacheava `unicep-portal.js` por 4 horas (`max-age=14400`), enquanto o `index.html` já vinha dinâmico.
  2. O `unicep-portal.js` antigo em cache tentava baixar o arquivo `.wasm` com o hash antigo (que não existia mais no container novo).
  3. O fallback SPA do backend respondia qualquer rota não encontrada com `index.html` (`200 OK`, `text/html`) em vez de `404`, fazendo o loader Wasm tentar instanciar HTML como binário WebAssembly.
- **Solução:**
  1. No servidor estático (`router.go`): definir `Cache-Control: no-cache, must-revalidate` para `.html` e `.js`, mantendo `public, max-age=31536000, immutable` apenas para `.wasm` com hash no nome.
  2. Retornar `404 Not Found` para qualquer URL com extensão de arquivo (`filepath.Ext(clean) != ""`) que não exista no disco, nunca caindo no fallback SPA de `index.html`.
  3. Adicionar query string de versão nos scripts do `index.html` (`unicep-portal.js?v=...`) a cada release para invalidar caches de borda imediatamente.

---

## Parte 2 — Como Implementar o Design Estilo Apple em Outro Aplicativo

Para deixar qualquer app com a mesma identidade visual limpa, tátil e fluida vista no UNICEP Portal, siga esta estrutura de **5 pilares**:

### 1. Arquitetura Visual em 3 Camadas
Organize a raiz da tela em um `Box` com 3 camadas sobrepostas:
1. **Camada 1 — Conteúdo Edge-to-Edge (`hazeSource`):**
   - O `LazyColumn` / `ScrollView` ocupa 100% da tela, rolando **por baixo** das barras superior e inferior.
   - Os espaçamentos das barras entram como `contentPadding` (`top` e `bottom`), nunca comprimindo a área visível do scroll.
2. **Camada 2 — Navegação Flutuante em Vidro (`hazeGlass`):**
   - **Mobile (`< 640.dp`):** Barra superior transparente com fade/blur progressivo conforme a rolagem (`collapseProgress`), e **Tab Bar flutuante em cápsula (`CircleShape`)** na base com uma lente deslizante atrás da aba ativa. Ao rolar para baixo, a barra pode recolher suavemente (*minimize*).
   - **Desktop / Tablet (`>= 640.dp`):** Sidebar lateral fixa (`~248.dp`) com itens em pílula arredondada e rodapé com avatar/perfil.
3. **Camada 3 — Overlays, Sheets e Alertas:**
   - Quando um `AppleSheet` abre no mobile, a Camada 1+2 sofre um leve recuo de profundidade (`scale = 0.94f`, `cornerRadius = 28.dp`) sobre fundo preto.
   - No desktop, o mesmo `AppleSheet` vira um diálogo centralizado em vidro (`maxWidth = 560.dp`, `RoundedCornerShape(28.dp)`).

---

### 2. Paleta de Cores Semânticas (Light & Dark)
Evite cinzas pesados ou bordas grossas. Use contraste por **elevação tonal de superfície**:

| Token | Modo Claro (Light) | Modo Escuro (Dark) | Uso |
| :--- | :--- | :--- | :--- |
| `background` | `#F2F2F7` | `#000000` | Fundo principal da tela |
| `surface` / `grouped` | `#FFFFFF` | `#1C1C1E` | Cards e células agrupadas |
| `tertiaryGrouped` | `#FFFFFF` | `#2C2C2E` | Cards dentro de sheets |
| `systemFill` | `#787880` (20% alpha) | `#787880` (36% alpha) | Trilhos, fundos de hover/press |
| `divider` | `#3C3C43` (18% alpha) | `#545458` (48% alpha) | Linhas de `0.5.dp` entre linhas |
| `accent` | `#007AFF` | `#0A84FF` | Ações principais, links, seleção |
| `green` / `orange` / `red` | `#34C759` / `#FF9500` / `#FF3B30` | `#30D158` / `#FF9F0A` / `#FF453A` | Status acadêmicos/financeiros |

---

### 3. Tipografia Limpa + Números Tabulares
- Use **Inter** (ou SF Pro / Plus Jakarta Sans) com pesos `Regular`, `Medium`, `SemiBold` e `Bold`.
- Hierarquia recomendada:
  - `largeTitle`: `34.sp` Bold (título principal no topo da lista)
  - `title3`: `20.sp` SemiBold (títulos de sheets e saudações)
  - `headline`: `17.sp` SemiBold (título de card/célula)
  - `body`: `17.sp` Regular (texto padrão de linha)
  - `subheadline`: `15.sp` Regular/Medium
  - `footnote`: `13.sp` Regular (metadados)
  - `caption1` / `caption2`: `12.sp` / `11.sp`
- **Regra de ouro:** Em qualquer nota, porcentagem, horário, contagem ou valor monetário, aplique `fontFeatureSettings = "tnum"` (números tabulares) para que colunas numéricas fiquem perfeitamente alinhadas.

---

### 4. Kit de Componentes Essenciais
1. **`GlassSurface` (com Fallback Automático):**
   - Usa `hazeGlass` quando habilitado (`GlassStyle.regular` / `clear`, `specularIntensity` ~ `0.42f–0.52f`, `lightingIntensity(0f)` no hover).
   - Quando o hardware não suportar ou dentro de `Popup`, cai automaticamente para fundo translúcido + gradiente vertical sutil no topo + borda fina de `0.75.dp`.
   - Sempre respeitar `Reduce Motion` do sistema operacional.
2. **`PortalCard` + `PortalListRow` (Estilo Ajustes do iOS):**
   - Cards agrupados com `RoundedCornerShape(20.dp)` e **sem borda** (no modo claro, sombra quase imperceptível de `2.dp` com 5% de alpha).
   - Múltiplas ações relacionadas ficam dentro do **mesmo card**, separadas por `PortalDivider(0.5.dp)` com recuo `start = 16.dp` (ou `58.dp` quando há ícone quadrado colorido de `30.dp` com cantos de `7.dp`).
3. **`PortalSegmentedControl`:**
   - Trilho em cápsula/retângulo arredondado (`10.dp`) com **um único thumb branco/elevado deslizante** por trás da opção ativa, e divisórias verticais sutis que desaparecem ao lado do item selecionado.
4. **`AppleSheet` e `AppleAlert`:**
   - `AppleSheet`: grabber no topo (`36.dp × 5.dp`), dois detents (`Medium` flutuante com inset de `8.dp` e cantos de `34.dp`, e `Large` encostando no topo), arrastável na vertical.
   - `AppleAlert`: card compacto (`maxWidth = 316.dp`), texto centralizado e botões em cápsula (`CircleShape`) lado a lado.
5. **Micro-feedbacks:**
   - Botões com escala `0.97f` ao pressionar (`animateFloatAsState` com `dampingRatio = 1.0f`).
   - `AppleActivityIndicator` (spinner radial de 8 traços arredondados) e `ProgressRing` (anel circular com transição suave).

---

### 5. Checklist Rápido Antes de Publicar
- [ ] Todas as molas (`spring`) que animam `Dp`, cantos ou `alpha` usam `dampingRatio = 1.0f`.
- [ ] Todos os cantos (`RoundedCornerShape`), `padding`, `width`/`height` têm `.coerceAtLeast(0.dp)` e alphas têm `.coerceIn(0f, 1f)`.
- [ ] Nenhum `GlassSurface` com `hazeGlass` está dentro de `Popup` em janela separada.
- [ ] Fontes customizadas estão empacotadas no APK (`androidResources.enable = true`).
- [ ] Backend Web envia `no-cache, must-revalidate` para `.html`/`.js` e `404` real para assets ausentes.
