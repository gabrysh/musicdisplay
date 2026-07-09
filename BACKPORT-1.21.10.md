# 1.21.10 backport — COMPILES ✅ (runtime untested)

`./gradlew build` with JDK 21 produces `build/libs/musicdisplay-2.4.jar` for **Minecraft 1.21.10**.
Runtime (mixin apply + rendering behaviour) has NOT been tested on a real 1.21.10 client yet —
test in a 1.21.10 + Fabric + Fabric API instance and report any crash.

Second wave of deltas done: `addGuiElement`→`submitGuiElement`, Screen `extractRenderState`→`render`,
`extractBackground`→`renderBackground`, `VertexFormatElement.register(..,boolean,..)`→`(..,Usage.GENERIC,..)`,
`Matrix3x2fc`→`Matrix3x2f`, removed dead `renderRounded`/`renderWorldBox` (used 26.1-only
`getGameRenderState()` and held an offensive debug string), `GuiMixin` target
`extractRenderState`→`render`, `fabric.mod.json` (mc 1.21.10-1.21.11 / java 21 / loader 0.17.2),
mixin `compatibilityLevel` JAVA_25→JAVA_21. All mixin target methods verified to exist in 1.21.10.

---

## Original notes

Standalone branch that ports the mod (written for 26.1) down to the **obfuscated**
Minecraft **1.21.10**.

## ✅ Toolchain solved (the blocker from the previous attempt)
The obfuscated toolchain now **configures** and sets up Minecraft 1.21.10:
- **Gradle 8.14** (wrapper) — Loom 1.11.8 requires ≥ 8.14.
- **JDK 21** (`JAVA_HOME=/usr/lib/jvm/java-21-openjdk`) — 1.21.x runs on Java 21.
- **Loom 1.11.8** applied via `buildscript { classpath 'net.fabricmc:fabric-loom:1.11.8' }`
  (the `plugins {}` DSL marker for old Loom isn't resolvable; the classpath artifact is).
- `mappings loom.officialMojangMappings()` + `modImplementation` fabric-api `0.138.4+1.21.10`,
  loader `0.17.2`, and `org.jspecify:jspecify:1.0.0` (26.1 bundles jspecify; 1.21.10 doesn't).

Build with: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew build`

## ✅ First wave of code deltas (done)
- `GuiGraphicsExtractor` → `GuiGraphics`
- `net.minecraft.resources.Identifier` → `ResourceLocation`
- `net.minecraft.client.renderer.state.gui.GuiElementRenderState`
  → `net.minecraft.client.gui.render.state.GuiElementRenderState`
- RenderPipeline builder: `.withColorTargetState(new ColorTargetState(...))` → `.withBlend(...)`;
  `.withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS,false))`
  → `.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withDepthWrite(false)`
- `GpuSampler` removed (doesn't exist in 1.21.10): filtering is set on the `GpuTexture`
  via `setTextureFilter(FilterMode,FilterMode,boolean)` + `setAddressMode(AddressMode,AddressMode)`,
  and `TextureSetup.singleTexture(view, sampler)` → `TextureSetup.singleTexture(view)`
  (ImageManager, MsdfFont, HaloFont, CaptureManager).

Errors went 200 → 16 → (render layer clean).

## ⏳ Remaining waves (not done)
- **`guiRenderState.addGuiElement(...)`** — ~96 call sites don't resolve; the 1.21.10
  GuiRenderState API differs (method name/signature TBD — needs inspecting the 1.21.10
  `GuiRenderState`/`GuiElementRenderState`; the custom `*RenderState` classes may need rework).
- **Screen render override**: `extractRenderState(GuiGraphics,int,int,float)` → `render(...)` (1.21.10).
- **The 10 mixins** target 26.1 class internals (`GameRenderer`, `Gui`, `Keyboard/Mouse`,
  `SharedConstants`, input event classes) — each needs adapting to 1.21.10 signatures.
  A compiling jar still won't run until the mixins match.
- **Runtime**: rendering behaviour on 1.21.10 is untested (can't run 1.21.10 here).

This is a multi-session port; the toolchain gate is now passed.
