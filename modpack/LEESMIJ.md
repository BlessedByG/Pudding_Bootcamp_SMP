# Modpack voor de streamers

Een Modrinth-pack (`.mrpack`) met alles wat een streamer voor de Pudding Bootcamp nodig heeft,
voor Minecraft 26.2 met Fabric Loader 0.19.5:

| Wat | Versie | Waarom |
|---|---|---|
| Fabric API | 0.161.0+26.2 | nodig voor de mods hieronder |
| Simple Voice Chat | 2.6.24 | de voice, verplicht ([docs/07-voice.md](../docs/07-voice.md)) |
| Sodium | 0.9.2 | sneller, en nodig voor Iris |
| Iris | 1.11.4 | om shaders te laden |
| Complementary Reimagined | r5.9.3 | shader, staat bij de eerste start aan |
| Complementary Unbound | r5.9.3 | shader, zelf te kiezen onder Opties > Video > Shader Packs |

Verder zet het pack klaar:

- **GUI-schaal 3** (`options.txt`), zodat de bossbar en de titles van de mod goed in beeld passen;
- **shaders aan** met Complementary Reimagined (`config/iris.properties`);
- **de bootcamp-server** in de Multiplayer-lijst (`servers.dat`), met het resource pack op
  automatisch aan, zodat er bij het joinen geen vraag komt.

Het pack bevat zelf geen mods: alleen een lijst met de vaste versies, met de download-url en de
hashes van Modrinth. De launcher haalt ze bij het importeren zelf op, dus het bestand is een paar kB
en iedereen krijgt precies dezelfde, officiële bestanden.

## Bouwen

```
powershell -ExecutionPolicy Bypass -File modpack/BouwModpack.ps1 -Server "<adres>:<poort>"
```

Dat maakt `modpack/Puddings-Bootcamp-1.0.0.mrpack`. Het serveradres staat bewust niet in de repo
(die is publiek); geef het mee met `-Server`. Een nieuwe versie van het pack: pas de versies in
`$Bestanden` aan (het versienummer zoals op Modrinth) en bouw met `-Versie 1.0.1`. De Simple Voice
Chat in het pack moet bij die op de server passen (dezelfde 2.6.x).

## Installeren (voor de streamers)

Het `.mrpack`-bestand werkt in de **Modrinth App**, **Prism Launcher** en **ATLauncher**:

- **Modrinth App:** links op de **+** (Create instance) > **Import from file** > het `.mrpack`.
- **Prism Launcher:** **Add Instance** > **Import** > het `.mrpack` kiezen > OK.
- **ATLauncher:** **Import** (links) > het `.mrpack` kiezen.

Daarna de instance starten en in Multiplayer op **Pudding's Bootcamp** klikken. De eerste start
duurt even: de shaders worden geladen.

Niet in MultiMC, de CurseForge-app of de gewone Minecraft-launcher. Wie die gebruikt, installeert
het makkelijkst de Modrinth App of Prism, of zet de mods hierboven zelf in een Fabric-installatie.
Lunar en Feather kunnen dit pack niet laden.
