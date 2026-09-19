# bootcamp-mod

Server-side Fabric-mod voor de Pudding Bootcamp SMP op Minecraft 26.2. Spelers hebben alleen
Simple Voice Chat nodig. De spec staat in [../docs/04-technische-schets.md](../docs/04-technische-schets.md),
de voortgang in [BOUWLOG.md](BOUWLOG.md).

## Bouwen

Je hebt **JDK 25** nodig (Minecraft 26.2 vraagt Java 25). Gradle hoef je niet te installeren, de
wrapper regelt dat.

```
cd mod
./gradlew build
```

De jar staat daarna in `fabric/build/libs/bootcamp-<versie>.jar`. Op Windows zonder Git Bash:
`gradlew.bat build`.

Twee Gradle-projecten:

- `core`: pure Java, alle rekenwerk en spelregels, JUnit-tests, geen Minecraft.
- `fabric`: de lijm naar Minecraft (Fabric Loom). Neemt `core` mee in de jar.

Alleen de kernlogica bouwen en testen, zonder Loom en zonder Minecraft te downloaden:

```
./gradlew -PcoreOnly :core:test
```

`./check.sh` draait de vaste verificatie (build, jar-inhoud, grep-checks).

## Installeren

De testserver heeft ook **Java 25** nodig. In `mods/` van de Fabric-server (26.2):

- `bootcamp-<versie>.jar`
- Fabric API voor 26.2
- Simple Voice Chat (Fabric) voor 26.2

Jar vervangen betekent server herstarten.
