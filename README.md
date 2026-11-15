# Sky Color (Minecraft 1.8.9 / Forge 11.15.1.2318)

Altera **somente a cor do ceu** (cor unica ou gradiente). Sem texturas, sem skybox.

## Compilar
Requisitos: **JDK 8** e **Gradle 2.14** (exigido pelo ForgeGradle 2.1). Com internet:

    gradle setupDecompWorkspace
    gradle build

O JAR sai em `build/libs/SkyColor-1.8.9-1.0.0.jar`. Copie para `.minecraft/mods/` (perfil Forge 1.8.9).
Para testar direto: `gradle runClient`.

## Uso
RIGHT SHIFT abre o menu (configuravel em Controles > Sky Color).
Configuracao salva em `.minecraft/config/skycolor.cfg` ao clicar em "Salvar".

## CI
O workflow `.github/workflows/build.yml` compila a cada push e publica o JAR como artifact (aba Actions).

## OptiFine HD_U_M5
Com "Sky: OFF" o disco colorido e desenhado via EntityViewRenderEvent.RenderFogEvent (ver ClientEvents.onRenderFog).
