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

### Toolchain investigation (attempt to build 1.21.10)
Getting an obfuscated Loom to work for 1.21.10 is the current wall:

- **Loom 1.14.10** (resolvable plugin marker): `loom.officialMojangMappings()` throws
  `Cannot use Mojang mappings in a non-obfuscated environment` for 1.21.10.
- **Loom 1.11.8 / 1.13.6** (Fabric's blog recommends Loom ~1.11 for the 1.21.9/1.21.10 era):
  the Gradle **plugin marker** `net.fabricmc.fabric-loom.gradle.plugin:<v>` is **not
  resolvable** from `maven.fabricmc.net` (only ~1.14.10 and 1.15.x resolve via the plugins DSL).

So the obfuscated 1.21.x toolchain likely needs: an older Loom applied via a `buildscript {}`
classpath + `apply plugin` (not the `plugins {}` DSL), a **matched older Gradle** (probably 8.x,
which conflicts with 26.1's Gradle 9.4 requirement) and **JDK 21**, plus a mappings decision
(native **Yarn** — which would require porting the code from Mojmap names — or Parchment/mojmap
if a supporting Loom is found). Because the Gradle/JDK/Loom requirements differ across the
obfuscation boundary, the old versions may be more practical as a **separate Gradle wrapper /
sub-build or a separate branch** rather than one Stonecutter project spanning 1.21.x↔26.1.

### Remaining code work (after toolchain)
Stonecutter source swaps for the API deltas: `Identifier`↔`ResourceLocation`,
`GuiGraphicsExtractor`↔`GuiGraphics`, `extractRenderState(`↔`render(`, input event classes,
`KeyMapping.Category`, and the mixin `compatibilityLevel` JAVA_25↔JAVA_21.

## Building
```
# active version is 26.1.2 (vcsVersion)
./gradlew build

# switch active version (once 1.21.x is wired):
./gradlew "Set active project to 1.21.11"
```
