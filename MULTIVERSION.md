# Multi-version support (WIP)

This branch adds [Stonecutter](https://stonecutter.kikugie.dev/) to target multiple
Minecraft versions from one codebase.

## Status

| Version  | Obfuscation | Loom needed | Status |
|----------|-------------|-------------|--------|
| 26.1.2   | unobfuscated | 1.15 | ✅ builds through Stonecutter (`versions/26.1.2/build/libs/...`) |
| 1.21.11  | obfuscated   | **1.14** | ⛔ blocked — see below |
| 1.21.10  | obfuscated   | **1.14** | ⛔ blocked — see below |

Registered in `settings.gradle`; per-version dependency matrix (minecraft / loader /
fabric-api / java level / mappings) is in `build.gradle`.

## The blocker: obfuscation boundary needs a per-node Loom version

26.1 is the first **unobfuscated** Minecraft release and uses **Loom 1.15**.
1.21.11 and 1.21.10 are **obfuscated** and need **Loom 1.14** + Mojang/Yarn mappings.

A single Loom version can't do both: Loom 1.15 rejects the obfuscated 1.21.x nodes
("Cannot use Mojang mappings in a non-obfuscated environment" / access-widener setup
failure). The Loom plugin is applied once in the `plugins {}` block, which is evaluated
before Stonecutter's per-node context is available, so the version can't yet vary per node.

### Next step
Make the Loom plugin version per-node (1.14 for `<26.1`, 1.15 for `>=26.1`), e.g. via a
per-version `loom_version` property resolved in the `plugins {}` block, then add the
Yarn/Mojmap mappings for 1.21.x and the Stonecutter source swaps for the API deltas
(`Identifier`↔`ResourceLocation`, `GuiGraphicsExtractor`↔`GuiGraphics`,
`extractRenderState(`↔`render(`, input event classes, `KeyMapping.Category`, and the
mixin `compatibilityLevel` JAVA_25↔JAVA_21).

## Building
```
# active version is 26.1.2 (vcsVersion)
./gradlew build

# switch active version (once 1.21.x is wired):
./gradlew "Set active project to 1.21.11"
```
