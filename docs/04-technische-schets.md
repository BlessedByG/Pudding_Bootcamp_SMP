# Technische schets: een server-side Fabric-mod op Minecraft 26.2

Eén eigen mod, `bootcamp`, op een Fabric-server. Server-side only: spelers hebben alleen Simple
Voice Chat nodig, plus een server resource pack dat ze bij het joinen automatisch krijgen. De mod
doet alles: regio's en punten zetten met een wand en commands, de rondes, de teams, de kroon, het
rad, de randomizer van de quiz, de tribune voor wie af is, jumpscares, bossbar en visuals. Voice
is puur proximity en gaat buiten de mod om.

De mod is gevibecode met Claude Code en staat in `mod/`. Hoe je hem bouwt, installeert en instelt
staat in [mod/README.md](../mod/README.md); wat er gebouwd en geverifieerd is, en wat er nog in-game
getest moet worden, in [mod/BOUWLOG.md](../mod/BOUWLOG.md).

> **Versie en status.** Minecraft 26.2; Fabric Loader, Fabric API en de Fabric-versie van Simple
> Voice Chat zijn er voor. **Deze doc beschrijft het rondeplan van 25 september 2026** (doolhof,
> Ei, mob arena, quiz, Clown vs All, FFA, en sinds 1 oktober de finale). De code in `mod/` is op 28 en 29 september 2026 naar dit
> plan omgebouwd (taakplan 2, [08-taakplan.md](08-taakplan.md)): hij bouwt, alle tests zijn groen,
> maar hij is nog niet in-game gedraaid. Wat er afwijkt van deze doc, staat hieronder bij het
> onderwerp en in [mod/BOUWLOG.md](../mod/BOUWLOG.md). Een naam uit 26.2 opzoeken:
> `./gradlew :fabric:genSources`.

## Stack

| Wat | Waarvoor |
|---|---|
| Fabric-server 26.2 + Fabric API | De server en de event/command-API. |
| `bootcamp`-mod (deze repo, map `mod/`) | Alles wat hieronder staat. |
| Server resource pack (deze repo, map `pack/`) | De vijf jumpscare-foto's, het schrikgeluid en de 8D-klop. |
| Simple Voice Chat (Fabric) | Voice. Puur proximity, de mod doet er niks mee. |
| WorldEdit (Fabric) | Bouwen. Niet voor de spellogica. |

Geen Skript, geen datapack, geen plugins, geen client-mod behalve voice.

## Project opzetten

1. Neem de Fabric-template voor 26.2 als basis (FabricMC/fabric-example-mod, branch master).
   Versies uit die template: `minecraft_version=26.2`, `loader_version=0.19.5`,
   `loom_version=1.17-SNAPSHOT`, `fabric_api_version=0.161.0+26.2`, Gradle-wrapper 9.5.1,
   plugin-id `net.fabricmc.fabric-loom`. **Java 25** is verplicht (`release 25`,
   `"java": ">=25"`), ook op de testserver. Er is geen mappings-regel meer: 26.2 is niet
   geobfusceerd, je werkt met de echte Mojang-namen.
2. `fabric.mod.json`: `"environment": "server"`, entrypoint `main`, depends `fabricloader >=0.19.5`,
   `minecraft ~26.2`, `java >=25`, `fabric-api *`. Geen mixins-config.
3. `build.gradle`: `implementation` voor `net.fabricmc:fabric-loader` en
   `net.fabricmc.fabric-api:fabric-api`, plus `implementation` én `include` van het
   `core`-project zodat de kernlogica in de jar komt. De voice-mod staat los van onze mod.
4. Dev-loop: `./gradlew build` maakt de jar in `fabric/build/libs/`; die kopieer je naar `mods/`
   van je eigen testserver en je herstart. Fixen tijdens het event betekent jar vervangen en
   herstarten, dus test vooraf.
5. De mod staat in deze repo onder `mod/`, in twee Gradle-projecten: `core` (pure Java, alle
   rekenwerk, JUnit-tests, geen Minecraft) en `fabric` (de lijm naar Minecraft, Fabric Loom).
   Met `-PcoreOnly` bouw en test je `core` zonder Loom. `mod/check.sh` draait de vaste
   verificatie. In het fabric-project draait één test (`KitsParseTest`) met de echte
   26.2-registries via `fabric-loader-junit`, zonder server: elk item uit de standaardkits gaat
   door de vanilla item-parser. 26.2-namen die onderweg anders bleken: `Identifier` in plaats van
   `ResourceLocation`, `EntityTypes` naast `EntityType`, `EntitySpawnReason`, permissies via
   `Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)`, teamkleur als `Optional<TeamColor>`,
   `lerpSizeBetween(van, naar, ticks, gameTime)`, en item-components die pas bij het laden van de
   registries aan items gebonden worden.

## Serverinstellingen

Gamerules die de mod bij het opstarten en bij `/bc reset` zet: `naturalRegeneration` aan, `pvp`
aan (de mod beslist zelf wie wie mag raken, zie PvP hieronder), `doMobSpawning` uit (mobs spawnen
we zelf), `mobGriefing` uit (creepers in de mob arena), `doDaylightCycle` uit,
`announceAdvancements` uit, `locatorBar` uit (aan in ronde 5), `randomTickSpeed` op 0 (geplakte bladeren
vergaan dan niet, gras en gewassen groeien niet door). In 26.2 heten de gamerules in code
anders (`GameRules.ADVANCE_TIME`, `SPAWN_MOBS`, ...) en zet je ze via
`level.getGameRules().set(...)`.

Zolang er geen ronde loopt (in de lobby en tussen de rondes) houdt de mod de honger van iedereen
die meedoet elke seconde vol.

Gamemode zet `/<ronde> start` per ronde: survival alleen in ronde 2 (het Ei), adventure in alle andere
rondes; kijkers altijd adventure.

**Staff is wie in creative of spectator staat.** Geen teleport, geen kit, telt niet mee, zit in
geen team. Er is geen apart command voor.

Op de server: difficulty niet op peaceful (de mob arena) en `spawn-protection=0`. Voor het
resource pack in `server.properties`:

```
resource-pack=<url van bootcamp-pack.zip>
resource-pack-sha1=<sha1 van dat bestand>
require-resource-pack=true
resource-pack-prompt=
```

De prompt is in 26.2 een JSON-tekst. Een host als Pterodactyl herschrijft `server.properties` bij
elke start en slikt dan het afsluitende aanhalingsteken en de regelovergang van
`"Nodig voor de bootcamp"` in; daarom leeg laten (dan toont Minecraft zijn eigen tekst).

## PvP

Eén regel in `ALLOW_DAMAGE`, niet via teams, want spelers zitten in verschillende teamkleuren en
mogen elkaar toch niet raken. Bron is de speler, ook via een pijl of andere projectile.

| Ronde | Speler raakt speler |
|---|---|
| 0 t/m 4 | nooit (de mob arena is PvE: alleen spelers tegen mobs) |
| 5 Clown vs All | alleen als aanvaller of slachtoffer de kroonhouder is |
| 6 FFA | altijd |
| 7 Finale | altijd (alleen de twee finalisten doen mee) |
| Tussen twee rondes, en tijdens elke opstelling of countdown | nooit |

## Teams

Teams zijn er voor de kleur van naam, Glowing-outline en locator-stip, en in ronde 3 en 4 om
te weten wie bij wie hoort. Friendly fire staat overal uit; de PvP-regel hierboven beslist.

| Team | Kleur | Wie |
|---|---|---|
| `spelers` | wit | Iedereen in het basiskamp en het doolhof, tot je een kleur kiest. |
| `rood`, `blauw`, `groen`, `geel` | rood, blauw, groen, geel | De vier teams van ronde 1 t/m 4, gekozen bij de uitgang van het doolhof. |
| `jagers` | aqua | Ronde 5, iedereen behalve de kroonhouder. |
| `kroon` | goud | De kroonhouder in ronde 5, de winnaar bij de kroning. |
| `out` | grijs | Kijkers. |

De teamkeuze wordt bewaard per speler (naam) in het geheugen van de mod en in `bootcamp.json`,
zodat een herstart tussen ronde 1 en 5 de teams niet kwijtraakt. `/bc team <speler> <kleur>`
zet iemand met de hand in een team; `/bc team <speler> weg` haalt de keuze weg. Bij ronde 5 gaat
iedereen uit zijn teamkleur naar `jagers`; de keuze blijft bewaard, maar wordt niet meer
gebruikt. In ronde 6 en de finale zit iedereen zonder team.

**Maximum per team**: 4, of meer als er meer dan 16 spelers zijn: `max(4, ceil(spelers / 4))`,
met spelers = iedereen die meedoet op het moment dat de eerste kiest.

**Het teammenu** is een vanilla kistmenu (`ChestMenu`, 4 rijen, `MenuType.GENERIC_9x4`) met een
`SimpleContainer`, een rij per kleur. Vooraan een gekleurd wolblok met de teamnaam en het aantal
(`Rood · 3/4`); een vol team is grijze wol. Daarachter de hoofden van wie er al in zit, met hun skin
(online met hun eigen profiel, offline op naam) en hun naam in de teamkleur; zijn het er meer dan
acht, dan zeven hoofden en `+3 meer`. Elke seconde bijgewerkt. Items verplaatsen kan niet (het menu
annuleert elke klik en handelt hem zelf af). **Alleen een klik op de wol** zet je in het team en
sluit het menu; een klik op een hoofd doet niets. Je blijft waar je bent (zie ronde 1). Sluit je het
menu zonder keuze terwijl je op de finishlijn (`doolhof_uit`) staat, dan opent het na 2 seconden
opnieuw. Allemaal server-side, geen client-mod nodig.

**De sidebar in het doolhof** (`Teams`) laat per kleur `Rood 3/4` zien (vet, in de teamkleur) en
daaronder wie erin zit, met het hoofd ervoor als hij online is. De sidebar heeft maar 15 regels
(`core`: `TeamOverzicht`): past het niet met één naam per regel, dan komen er twee, drie of meer naast
elkaar, zo weinig als kan. Elke seconde bekeken (ook `/bc team` en wie in- of uitlogt), alleen
opnieuw gestuurd als er iets veranderde.

## Resource pack en de jumpscare

Het pack staat in `pack/` in deze repo. **Aanleveren** gaat in `pack/aanleveren/`:

| Bestand | Eisen |
|---|---|
| `schrik_1.png` t/m `schrik_5.png` (ook `.jpg` of `.jpeg`) | De vijf jumpscare-foto's; in de mod heten ze foto 1 t/m 5. **Elk formaat en elke verhouding**: vierkant, liggend of staand, zo groot als je wilt. Een webp eerst omzetten naar png. |
| `schrik.ogg` | Het geluid bij elke jumpscare, ogg vorbis. **Zonder stilte aan het begin** (anders komt het geluid na de foto) en zo hard als je hem wilt: Minecraft speelt nooit harder dan het bestand zelf. Het huidige bestand is 0,362 s ingekort en 12 dB harder gemaakt, zie `pack/aanleveren/LEESMIJ.txt`. |
| `klop.ogg` | De 8D-klop uit de valkisten, ogg vorbis. **Stereo laten**: Minecraft speelt een stereogeluid zonder richting af, dus het 8D-effect in het bestand blijft. |
| `rad.ogg` | Het geluid van een draai van het quiz-rad (spinwheel, 11,1 s), ogg vorbis. Speelt op 70% (`"volume": 0.7` in `sounds.json`). **Niet inkorten**: de tijdlijn van het rad (`QuizDraai`) is op dit bestand gemeten; een ander geluid betekent de tikjes opnieuw meten. |
| `logo.png` | Het logo in het midden van het quiz-rad, vierkant. Het staat rechtop, ook als het rad draait, en zo groot dat het hele puddingkje binnen de ronde naaf valt (`LOGO_REIKWIJDTE`: het blaadje steekt tot 1,06 keer de halve breedte uit); de achtergrond van het logo vult de rest van de naaf, met een zachte overgang. Zonder logo een gewone dop. |

Minecraft speelt alleen ogg vorbis. Een wav of mp3 eerst omzetten, met Audacity (Bestand >
Exporteren > Exporteren als OGG) of `ffmpeg -i in.wav -c:a libvorbis -q:a 5 uit.ogg`. BouwPack
meldt het als er wel een `.wav` ligt maar geen `.ogg`.

**Bouwen:** `java pack/BouwPack.java`. Alleen de JDK 25 is nodig, die je voor de mod toch al hebt,
en het werkt ook op Windows zonder Git Bash. Het programma:

1. leest de vijf foto's (jpg of png) en schaalt ze naar 476 pixels hoog, met behoud van de
   verhouding (een heel brede foto wordt kleiner, tot 1428 pixels breed); een foto die er niet is,
   wordt een placeholder met "foto N volgt";
2. zet `schrik.ogg`, `klop.ogg` en `rad.ogg` erbij (de klop streamt, want die is lang; de jumpscare laadt al
   bij het laden van het pack, `preload`, anders laadt Minecraft hem pas bij de eerste keer en komt
   die te laat);
3. tekent **het quiz-rad** (Java2D, niets aan te leveren): 64 standen van 484 × 484, elk 5,625°
   verder gedraaid, met de 16 vakken in de vaste volgorde uit *Ronde 4* in alleen de
   teamkleuren (rood `#E24B4A`, blauw `#378ADD`, groen `#639922`, geel `#EF9F27`), een donkere
   rand en naad tussen de vakken, een naaf in het midden met daarop `logo.png` (rechtop, het draait
   niet mee; zonder logo een kleine dop), en het pijltje vast bovenin;
4. knipt de foto's en elke stand van het rad in **tegels** en schrijft de fonts (zie hieronder);
5. zipt het pack naar `bootcamp-pack.zip` en print de SHA-1 voor `server.properties`.

**Waarom tegels.** Minecraft 26.2 zet font-glyphs op vellen van 256 × 256 pixels; een glyph die
groter is, wordt zonder foutmelding een leeg vierkantje. Daarom is elk plaatje geknipt in twee
rijen tegels van hooguit 242 pixels, elk een eigen glyph. Twee rijen, omdat een glyph niet hoger
boven de basislijn mag staan dan hij hoog is (`ascent` ≤ `height`): de bovenste rij hangt boven de
basislijn, de onderste eronder, dus het plaatje staat in het midden van het scherm. De mod zet de
tegels weer aan elkaar met één tekst (`core`: `FontTegels`): na elke tegel een spatie van -1
(U+F801, want een bitmap-glyph schuift één eenheid meer op dan hij breed is), na de bovenste rij
een spatie terug over de hele breedte (U+F802). Elke tegel is een heel aantal font-eenheden breed
(7 pixels per eenheid voor de foto's, 11 voor het rad), anders ontstaat er een naad.
- Jumpscare: elke foto een eigen font `bootcamp:schrik_1` t/m `schrik_5`, zodat elke foto zijn
  eigen spatie terug (U+F802) heeft. In elke font is tegel (rij r, kolom c) U+E000 + 16r + c, tot
  zes kolommen; 34 eenheden per rij, dus 68 hoog. Kolommen die een smallere foto niet nodig heeft
  zijn spaties van +1. De mod stuurt dus voor elke foto dezelfde tekst, in een andere font.
- Quiz-rad: stand s is 2 × 2 tegels, U+E100 + 4s + 2r + c; 22 eenheden per rij, dus 44 hoog.
- `PackFontTest` in het fabric-project haalt elke provider van alle vijf foto's en het rad door
  de echte font-codec van 26.2 en controleert de maten van de tegels.

Online zetten als bijlage van een GitHub-release (de repo is public, dus de foto's in de zip zijn
dan openbaar), en die URL in `resource-pack=`. Na een nieuwe foto of geluid: opnieuw bouwen,
opnieuw uploaden, nieuwe SHA-1 invullen, server herstarten.

```
pack/
  aanleveren/schrik_1.png … schrik_5.png      wat je aanlevert (jpg of png, elk formaat)
  aanleveren/schrik.ogg, klop.ogg, rad.ogg, logo.png
  BouwPack.java                               bouwt het pack en de zip
  pack.mcmeta
  assets/bootcamp/font/schrik_N.json          per foto: per tegel een bitmap-provider, plus de spaties
  assets/bootcamp/textures/font/schrik_N_R_C.png de tegels van foto N (rij R, kolom C), gemaakt door BouwPack
  assets/bootcamp/font/rad.json               per tegel een bitmap-provider (64 standen × 4), plus de spaties
  assets/bootcamp/textures/font/rad_SS_R_C.png de tegels van stand SS van het quiz-rad
  assets/bootcamp/sounds.json                 bootcamp:schrik, bootcamp:klop en bootcamp:rad
  assets/bootcamp/sounds/*.ogg                gekopieerd uit aanleveren/
```

De foto's en geluiden in `aanleveren/` staan niet in git (de repo is public); wat BouwPack maakt
(`assets/bootcamp/textures/`, de geluiden, de zip) ook niet. De fonts en `pack.mcmeta` wel. Zonder
geluid is het stil, want een stil ogg-bestand kan BouwPack niet maken. Een foto die niet vierkant
is, blijft in de jumpscare in zijn eigen verhouding: de breedte volgt de hoogte.

**De jumpscare** (`Schrik.op(speler, foto)`): een title met de tegels van foto 1 t/m 5 in font
`bootcamp:schrik_N` (foto 0 = willekeurig, R1.6), fade-in 0, blijven 30 ticks, fade-out 10, plus
`bootcamp:schrik` op volle sterkte, alleen voor die speler. Hoe groot hij op het scherm staat,
bepalen `SCHRIK_EENHEDEN` en `SCHRIK_PX_PER_EENHEID` bovenin `BouwPack.java` (nu 68 eenheden hoog;
een title tekent vier keer zo groot); afstemmen in de eerste test. Heeft een speler het pack niet
(weigerde of downloadfout), dan ziet die lege vierkantjes en hoort niks; met
`require-resource-pack=true` kan dat niet. `/bc schrik <spelers> [<foto>]` doet een jumpscare met
de hand (zonder foto een willekeurige), voor het testen en voor de lol.

**De 8D-klop** (`Schrik.klop(speler)`): alleen het geluid `bootcamp:klop`, alleen voor die speler,
geen beeld. `/bc klop <spelers>` om te testen.

Waar een jumpscare vandaan komt:
- Doolhof: in een regio `schrik_1` t/m `schrik_n`, één keer per regio per speler. Per plek een
  vaste foto of willekeurig: `/doolhof schrik <nr> <1..5|random>`, standaard willekeurig.
- Doolhof: een valkist (trapped chest) openen, één keer per kist per speler: 25% de jumpscare met
  een willekeurige foto, 25% de 8D-klop, 50% mobs (R1.4).
- Het Ei: een emerald block, bij een willekeurige andere levende deelnemer, met een willekeurige
  foto; iedereen ziet groot wie naar wie.

## Modules

| Package | Doet | Belangrijkste API |
|---|---|---|
| `config` | Regio's, punten, teamkeuzes, grapjes opslaan en laden, JSON in `<wereld>/bootcamp.json`. | Gson, `ServerLifecycleEvents` |
| `kits` | Kits uit JSON-bestanden lezen en op spelers zetten; de loot-tabel van het doolhof. | `ItemParser` (dezelfde syntax als `/give`) |
| `commands` | `/bc` en de commando's per ronde (`/doolhof`, `/ei`, `/mobarena`, `/quiz`, `/clown`, `/ffa`, `/finale`). | Brigadier, `CommandRegistrationCallback` |
| `setup` | De wand, `region show` met particles. | `AttackBlockCallback`, `UseBlockCallback` |
| `game` | Spelstatus, timer, de zes rondes als klassen met `start/tick/onDeath/end`, de PvP-regel. | `ServerTickEvents.END_SERVER_TICK`, `ALLOW_DAMAGE` |
| `teams` | Vier teamkleuren, het teammenu, de maximumregel. | `ChestMenu`, `SimpleContainer` |
| `crown` | Kroonhouder, laatste hit, kroonwissel, opstelling, bevriezing. | `ServerLivingEntityEvents.ALLOW_DEATH`, `ALLOW_DAMAGE` |
| `rad` | Het Rad (een rij spelerskoppen met namen in beeld, rigged) en het quiz-rad (een rond rad als plaatjes uit het pack, 16 vakken, echt toeval): dezelfde rekenlogica. | tick-gestuurd, geen threads |
| `mobs` | Waves spawnen en tellen, per arena; kills toeschrijven; mobs laten kijkers met rust. | `EntityType.spawn`, entity-tags, `Mob.setTarget` |
| `schrik` | De jumpscare en de nep-uitgang. | title-packets, `playNotifySound` |
| `tribune` | Wie af is naar de tribune of de kooi, daar houden, geen schade, locator bar uit. | `ALLOW_DAMAGE`, tick-check op regio's |
| `visuals` | Bossbar, sidebar, titles, geluid, particles, vuurwerk, zweefkroon, labels. | `ServerBossEvent`, packets, `Display`-entities |

Vuistregel: alles draait op de server-tick. Geen `Thread.sleep`, geen eigen threads; een
wachttijd is een tick-teller in een state-object.

## Commands

Alles op op-level 2: spelers zonder op zien en gebruiken geen enkel commando. Elke ronde heeft
een eigen commando met de functies van die ronde, in de vorm `/<ronde> <functie>`. Wat voor de
hele avond geldt (setup, spelers, noodknoppen) staat onder `/bc`.

**Voor elke ronde hetzelfde:**

- `/<ronde> start`: teleport naar het startpunt, border, kits, countdown, timer. Breekt een
  lopende ronde eerst af. Weigert met één regel en verandert niets als er een regio, punt of kit
  mist.
- `/<ronde> stop`: breekt die ronde af: timer stil, mobs en border weg, bevriezing eraf, bossbar
  terug. Rollen en teams blijven. Loopt er een andere ronde, dan weigert hij met één regel
  (`nu loopt: het Ei`).

**Rondes met een timer** (doolhof, Ei) hebben er twee bij:

- `/<ronde> timer [<minuten>]`: hoe lang de ronde duurt. Een instelling, zie *Instellingen*
  hieronder.
- `/<ronde> resterend <seconden>`: alleen terwijl de ronde loopt: zet de klok op zoveel seconden.
  Om te testen of bij te sturen; wordt niet bewaard.

**Per ronde:**

| Command | Doet |
|---|---|
| `/doolhof start\|stop\|resterend` | Ronde 1. `start` zet iedereen klaar in de startruimte, zonder countdown. |
| `/doolhof go` | De countdown na `/doolhof start`; daarna gaat de startpoort open en loopt de timer. |
| `/doolhof wachttekst [<tekst>]` | De tekst in de actionbar zolang het doolhof op `/doolhof go` wacht, standaard `Wacht op het startsein`. Zonder tekst: de huidige laten zien. Bewaard in `bootcamp.json`. |
| `/doolhof einde` | Noodknop: het doolhof nu afsluiten: kleinste team voor wie er nog geen heeft, en wie nog in het doolhof loopt naar de finishruimte (met zijn spullen). Zonder deze knop wacht het doolhof tot niemand er meer in loopt. Bijvoorbeeld als iemand in de finishruimte blijft staan zonder te kiezen. |
| `/doolhof naarei` | Na het doolhof: iedereen die meedoet van de finishruimte naar `v2` bij het Ei, title `OP NAAR HET EI`. Weigert zolang er een ronde loopt. |
| `/doolhof timer [<minuten>]` | Hoe lang het doolhof duurt, standaard 15. |
| `/doolhof poort [<minuten>]` | Na hoeveel minuten de poort van de uitgang opengaat, standaard 4. |
| `/doolhof hint [<minuten>]` | Na hoeveel minuten de hint komt, standaard 10. |
| `/doolhof hinttekst [<tekst>]` | Wat er in de hint staat (subtitle onder `HINT`), bijvoorbeeld `De echte gang begint bij de lantaarn`. Bewaard in `bootcamp.json`. Zonder tekst: de huidige laten zien. Is er nooit een tekst gezet, dan rekent de mod een windrichting uit: `De uitgang ligt aan de noordkant`. `/doolhof hinttekst -` wist hem weer. |
| `/doolhof poort open\|dicht` | De poort met de hand bedienen. |
| `/doolhof startpoort open\|dicht` | De startpoort (`poort_start`) met de hand bedienen, om te testen. Open gaat stil, zoals bij de start van de timer. Dicht zet terug wat er stond. |
| `/doolhof poortmelding [aan\|uit]` | Of iedereen de raid-hoorn hoort en de title `DE UITGANG IS OPEN` ziet als de uitgang opengaat, standaard `aan`. Met `uit` gaat de poort stil open (de wolkjes in de poort komen er wel). Geldt ook voor `/doolhof poort open`. Zonder argument: de huidige stand. Bewaard in `bootcamp.json`. |
| `/doolhof schrik [<nr> [<foto>]]` | Welke foto schrikplek `schrik_<nr>` laat zien: `1` t/m `5` voor een vaste foto, `random` voor een willekeurige (standaard). Zonder foto: de huidige; zonder nummer: alle schrikplekken. Bewaard in `bootcamp.json`. |
| `/doolhof valmobs [<min> [<max>]]` | Hoeveel mobs (husks en silverfish door elkaar) er uit een valkist komen: elke keer willekeurig van `<min>` t/m `<max>`, standaard 3 t/m 10. Met één getal altijd zoveel; `0` is alleen de jumpscare of de klop (50/50). Bewaard in `bootcamp.json`. |
| `/ei start\|stop\|resterend` | Ronde 2. |
| `/ei timer [<minuten>]` | Hoe lang het Ei duurt, standaard 15. |
| `/ei blokken [<soort> <aantal>]` | Hoeveel blokken van een soort (`netherite`, `diamond`, `gold`, `iron`, `redstone`, `emerald`, `tnt`, `glowstone`, `slime`, `target`) de mod in het Ei strooit. Zonder argumenten: het overzicht, met het aantal deepslate-plekken in het Ei. |
| `/ei prijskader` | Het item frame waar je naar kijkt (binnen 5 blokken) wordt het frame voor het Warden-ei: daar verschijnt het als het Ei voorbij is. Slaat punt `ei_prijskader` op. |
| `/ei vastleggen` | Legt het Ei vast zoals het nu gebouwd is: alle blokken in regio `ei`. Eén keer na het bouwen, en opnieuw na elke bouwwijziging. Niet tijdens ronde 2. Antwoord: `Ei vastgelegd: 54.000 blokken, waarvan 27.812 deepslate.` |
| `/mobarena start` | Ronde 3: loot het schema (geheim, niet in de chat) en start beurt 1. |
| `/mobarena volgende` | Start de volgende beurt. Weigert zolang de huidige beurt nog loopt, ook tijdens de 10 seconden na de beurt. |
| `/mobarena schema` | Het schema, alleen voor wie het typt (de spelers zien het niet): per beurt wie in welke arena staat, wie af is. |
| `/mobarena startplek <rood\|blauw\|groen\|geel> <1\|2>` | Zet startplek 1 of 2 van dat team op de plek waar je staat, met je kijkrichting. Elk team heeft er twee, zodat de twee spelers van een team niet in elkaar spawnen. Hetzelfde als `/bc point set start_<kleur>_<nummer>`. |
| `/mobarena stop` | Breekt ronde 3 af. |
| `/mobarena wave volgende` | De huidige wave telt als klaar (overgebleven mobs weg), ook de warden. |
| `/mobarena warden [leven\|klap\|boom <hp>]` | De warden uit het Warden-ei, in HP (2 HP is één hartje): `leven` (standaard 200), `klap` (8) en `boom` voor de sonic boom (5). Zonder argumenten: de huidige waarden. Geldt voor de volgende warden; bewaard in `bootcamp.json`. Zijn punten: `/mobarena punten warden <n>`. |
| `/mobarena punten [<mob> <punten>]` | Hoeveel punten een mobtype waard is. Zonder argumenten: de tabel. |
| `/mobarena aftekst [<tekst>]` | De tekst die in de actionbar staat bij wie in de mob arena sneuvelt, standaard `Af · je speelt geen beurt meer`. Zonder tekst: de huidige laten zien. Bewaard in `bootcamp.json`. |
| `/mobarena veldhoogte [<blokken>]` | Tot hoeveel blokken boven de selectie een veld telt voor kijkers en wachtenden, standaard 3. Op regio `tribune_mob` telt het veld nooit, dus meestal hoef je hier niets aan te doen. Bij `0` telt alleen wie binnen de selectie zelf staat. Geldt meteen; bewaard in `bootcamp.json`. |
| `/quiz start\|stop` | Ronde 4. Weigert zonder presentator. |
| `/quiz presentator [<speler>]` | Wie presenteert (Pudding). Op naam, mag ook voor iemand die nog niet online is; bewaard in `bootcamp.json`. Die gaat bij de start naar het podium in plaats van naar zijn bank en krijgt de drie quiz-items. |
| `/quiz bank <rood\|blauw\|groen\|geel>` / `/quiz podium` | Zet de bank van dat team, of het podium, op de plek waar je staat, met je kijkrichting. Hetzelfde als `/bc point set quiz_<kleur>` en `quiz_podium`. |
| `/quiz lamp <kleur> [<nr>\|wis]` | Een lamp bij de bank van dat team: het blok waar je naar kijkt (tot 32 blokken). Zonder nummer komt er een lamp bij (`quizlamp_<kleur>_1`, `_2`, ...), met een nummer zet je die ene opnieuw, `wis` haalt alle lampen van dat team weg. Zoveel lampen als je wilt; ze gaan samen aan en uit. |
| `/quiz vuurwerk <kleur> <1\|2>` | Zet dispenser 1 of 2 bij de bank van dat team: het blok waar je naar kijkt (tot 32 blokken), en het moet een dispenser zijn. Bij een goed antwoord schiet hij een vuurpijl in de teamkleur, de kant op waar hij naartoe wijst. |
| `/quiz draai` | Het rad draaien. Hetzelfde als het rad-item van de presentator. |
| `/quiz goed` / `/quiz fout` | Het antwoord van het team dat aan de beurt is goedkeuren (+1 punt) of afkeuren. Hetzelfde als de groene en rode wol. |
| `/quiz punt <kleur> [<aantal>]` | Punten erbij (standaard 1, negatief mag): om een verkeerde klik recht te zetten. |
| `/quiz einde` | Het team met de meeste punten wint: titles en vuurwerk. Bij gelijke stand weigert hij en noemt de teams die gelijk staan. |
| `/quiz winnaar <kleur>` | Een winnaar aanwijzen, voor een gelijke stand. |
| `/quiz naararena` | Na de winnaar: iedereen naar de tribune van de Arena, net als de ender pearl van de presentator. Noodknop als Pudding er niet is. |
| `/clown rad` | Het Rad (in beeld), en daarna zet ronde 5 iedereen bevroren klaar op de vloer. Weigert zonder uitverkorene die online is en meedoet, en als ronde 5 daarna niet zou kunnen starten. |
| `/clown go` | Start de countdown van 10 seconden; daarna is iedereen los. Alleen nodig na het Rad (of `/clown start`); na een kroonwissel loopt de countdown vanzelf. Weigert als er niemand klaarstaat. |
| `/clown start` | Ronde 5 zonder het rad: de kroon gaat meteen naar de uitverkorene, iedereen bevroren klaar, dan `/clown go`. Noodknop, bijvoorbeeld na een crash. |
| `/clown stop` | Breekt ronde 5 af, of stopt een draaiend rad. |
| `/clown uitverkoren [<speler>]` | De verborgen rol. Op naam, mag ook voor iemand die nog niet online is. Het antwoord ziet alleen wie het typt: het rad blijft geheim. |
| `/clown troon` | Zet het podium in het midden (punt `troon`) op de plek waar je staat, met je kijkrichting. |
| `/clown jagerplek [<nummer>]` | Zet een startplek voor de jagers op de plek waar je staat, met je kijkrichting. Zonder nummer het volgende vrije nummer (`jagerplek 7 gezet`), met nummer overschrijf je die plek. Zoveel als je wilt; in de Arena zijn het er 20. |
| `/clown vloer <diameter>` | Maakt regio `vloer`: een cirkel met jou als midden, die doorsnede in blokken, 5 hoog vanaf je voeten. Tekent meteen tien seconden de rand in particles. Kijkers die erin komen, gaan terug naar de tribune. |
| `/clown tribune [<nummer>]` | Zet een tribuneplek (`tribune_n`) op de plek waar je staat, met je kijkrichting; nummert zelf door zoals `jagerplek`. Alleen op de onderste ring. |
| `/clown wachttekst [<tekst>]` | De tekst in de actionbar terwijl iedereen stil staat en wacht op `/clown go`, standaard `Wacht op het startsein`. Zonder tekst: de huidige laten zien. Bewaard in `bootcamp.json`. |
| `/clown kroon <speler>` | Kroonwissel forceren (de noodknop van de ref). Voor `/clown go` (het rad landde op de verkeerde naam) zonder reset: de oude kroonhouder wordt jager op de plek van de nieuwe en iedereen blijft wachten op `/clown go`. Daarna een gewone kroonwissel met reset. |
| `/clown krimp <grootte> [<seconden>]` | De border laten krimpen als het stilvalt. Standaard in 60 seconden. Iedereen ziet `DE BORDER KRIMPT`. |
| `/ffa start\|stop` | Ronde 6. `start` zet iedereen bevroren klaar. Geen timer. De winnaar gaat door naar de finale. |
| `/ffa go` | Start de countdown van 10 seconden; daarna is iedereen los. |
| `/ffa krimp <grootte> [<seconden>]` | De border laten krimpen als het stilvalt. Standaard in 60 seconden. Zonder dit commando blijft de hele vloer vrij. Iedereen ziet `DE BORDER KRIMPT`. |
| `/ffa wachttekst [<tekst>]` | De tekst in de actionbar terwijl iedereen wacht op `/ffa go`, standaard `Wacht op het startsein`. Zonder tekst: de huidige laten zien. Bewaard in `bootcamp.json`. |
| `/finale start\|stop` | Ronde 7: de winnaar van King of the Hill tegen de winnaar van de FFA, één tegen één, met de kroning aan het eind. `start` zet ze allebei bevroren klaar op `finale_1` en `finale_2`, de rest op de tribune. Geen timer. |
| `/finale go` | Start de countdown van 10 seconden; daarna zijn ze los. |
| `/finale combatlog` | Alleen als een finalist is uitgelogd (de finale pauzeert dan): het was een combat log, de ander wint en krijgt meteen de kroning. |
| `/finale crash` | Alleen als een finalist is uitgelogd: het was een crash, de finale stopt zonder winnaar. Is hij terug, dan `/finale start` en `/finale go`. |
| `/finale plek 1\|2` | Zet `finale_1` (de winnaar van King of the Hill) of `finale_2` (de winnaar van de FFA) op je positie, met je kijkrichting. |
| `/finale spelers [<speler1> <speler2>]` | Zonder namen: de uitslag (winnaar King of the Hill, winnaar en nummer twee van de FFA) en wie de finale speelt. Met namen: de twee finalisten met de hand, `speler1` op `finale_1`. Noodknop, en om te testen. |
| `/finale krimp <grootte> [<seconden>]` | De border laten krimpen als het stilvalt. Standaard in 60 seconden. |
| `/finale wachttekst [<tekst>]` | De tekst in de actionbar terwijl de finalisten wachten op `/finale go`, standaard `Wacht op het startsein`. Zonder tekst: de huidige laten zien. Bewaard in `bootcamp.json`. |

**Algemeen:**

| Command | Doet |
|---|---|
| `/bc wand` | Geeft de regio-wand (een stick met een custom data component). |
| `/bc region save\|add\|show\|list\|del <naam>` | `save` maakt de regio uit de wand-selectie (een bestaande regio begint opnieuw), `add` voegt de selectie toe als extra deel, `show` tekent tien seconden particles op de randen van alle delen, `list` toont de regio's met hun aantal delen, `del` haalt de hele regio weg. |
| `/bc point set\|block\|tp\|list\|del <naam>` | Punt op je positie (met kijkrichting) of op het blok waar je naar kijkt (tot 32 blokken). |
| `/bc label zet <tekst>` / `/bc label weg` | Een text display boven je hoofd plaatsen of het dichtstbijzijnde weghalen. |
| `/bc status` | Rollen, teams en vlaggen van alle spelers, huidige ronde, timer en de instellingen. |
| `/bc kit <naam> [<speler>]` | Zet een kit op iedereen die meedoet, of op één speler. |
| `/bc team <speler> <rood\|blauw\|groen\|geel\|weg>` | Noodknop: iemand in een team zetten of de keuze weghalen. Mag boven het maximum. |
| `/bc schrik <spelers> [<foto>]` | Een jumpscare, met de hand: foto 1 t/m 5, zonder foto voor ieder een willekeurige. Eén speler of een selector, bijvoorbeeld `/bc schrik @a 3`. |
| `/bc klop <spelers>` | De 8D-klop, met de hand (om te testen); ook met `@a`. |
| `/bc kijker <speler> aan\|uit` | Noodknop: iemand met de hand op de tribune zetten of eraf halen. |
| `/bc doodtekst lijst\|nieuw <tekst>\|weg <nr>\|standaard` | De doodteksten bekijken, er een bijzetten, er een weghalen (op nummer uit de lijst; er blijft er minstens één) of de standaard zes terugzetten. Wordt meteen opgeslagen. |
| `/bc reset` | Alles terug naar de basiskamp-staat via het reset-register; ook alle teamkeuzes weg en het Ei teruggezet. Instellingen blijven. |

**Instellingen.** De timers (`/doolhof timer`, `/ei timer`), de momenten in het doolhof
(`/doolhof poort <minuten>`, `/doolhof hint`), de aantallen van `/ei blokken` en de punten van
`/mobarena punten` worden bewaard in `bootcamp.json`, dus ze overleven een herstart en
`/bc reset`. Zonder getal laat het commando de huidige waarde zien. Tijden altijd in hele minuten, gerekend vanaf de start van de
ronde. Een nieuw aantal puntenblokken geldt vanaf de volgende `/ei start`.

| Instelling | Standaard | Grenzen |
|---|---|---|
| `/doolhof timer` | 15 | 5 tot 60, en later dan de poort en de hint |
| `/doolhof poort` | 4 | 0 (open na de countdown) tot de timer |
| `/doolhof hint` | 10 | 1 tot de timer |
| `/doolhof hinttekst` | geen (dan de windrichting) | tot 60 tekens, zodat hij op één regel past |
| `/doolhof poortmelding` | aan | aan of uit |
| `/doolhof wachttekst` | `Wacht op het startsein` | tot 60 tekens |
| `/doolhof valmobs` | 3 t/m 10 | 0 tot 20, min niet boven max |
| `/doolhof schrik` | per plek willekeurig | foto 1 t/m 5, of random |
| `/ei timer` | 15 | 5 tot 60 |
| `/ei blokken` | netherite 6, diamond 90, gold 120, iron 2000, redstone 10, emerald 10, tnt 10, glowstone 10, slime 10, target 5 (voorlopig; samen 2271) | 0 of meer; samen niet meer dan de deepslate-plekken in het Ei |
| `/mobarena punten` | zombie 1; skeleton, spider, cave spider 2; creeper 3; witch 4; vindicator 5; evoker 8; ravager 10; elk ander type 1 | 0 tot 100 |
| `/mobarena aftekst` | `Af · je speelt geen beurt meer` | tot 60 tekens |
| `/mobarena veldhoogte` | 3 blokken | 0 tot 10 |
| `/mobarena warden` | leven 200, klap 8, boom 5 (HP) | leven 20 tot 1000, klap 0 tot 60, boom 0 tot 40 |
| `/clown wachttekst` | `Wacht op het startsein` | tot 60 tekens |
| `/ffa wachttekst` | `Wacht op het startsein` | tot 60 tekens |
| `/finale wachttekst` | `Wacht op het startsein` | tot 60 tekens |

Buiten de grenzen weigert het commando met één regel (`de hint (16 min) valt na het einde
(15 min)`). Loopt de ronde al, dan geldt een nieuwe waarde meteen. Een poort of hint die al
geweest is, blijft geweest; ligt het nieuwe moment al achter je, dan gebeurt het nu. Een timer
korter dan wat er al gespeeld is, weigert hij.

## Regio's en punten

Eén keer zetten na het bouwen. Alles wordt opgeslagen in `<wereld>/bootcamp.json`. Geen
coördinaten in code.

**Regio's** met de wand: linksklik op een blok is hoek 1, rechtsklik hoek 2, dan
`/bc region save <naam>`. **Voor spelers is een regio een kolom**: alleen x en z tellen. Een
poort gebruikt wel de hele doos, net als de kistenscan van het doolhof en het Ei (`ei`). `veld`
telt als rechthoek. Startpunten horen binnen de border-regio van hun ronde te
liggen; anders weigert `/<ronde> start`.

**De vloer van de Arena is een cirkel**, gezet met een commando in plaats van de wand:
`/clown vloer <diameter>`. Ga midden in de Arena op de vloer staan (naast het podium mag, één
blok verschil maakt op een cirkel van 60 niets uit) en geef de doorsnede in blokken. De mod maakt
regio `vloer`: een cilinder met jouw positie als midden, die doorsnede, en **5 blokken hoog**
vanaf je voeten. Hier telt de hoogte wel: wie hoger staat, bijvoorbeeld op de tribune, is er niet
in. Meteen daarna tekent de mod tien seconden particles op de rand, zodat je ziet of hij goed
ligt; `/bc region show vloer` doet dat later nog eens. Opnieuw typen overschrijft hem.

**Een regio mag uit meerdere delen bestaan**, voor een veld dat geen rechthoek is (de T-vorm van
de mob-arenavelden). `/bc region save <naam>` begint de regio opnieuw met de huidige selectie;
`/bc region add <naam>` voegt de selectie toe als extra deel. Een plek hoort bij de regio als hij
in één van de delen ligt, en delen mogen elkaar overlappen. Een T is dus twee selecties: de
balk en de poot. `/bc region show` tekent alle delen, `/bc region list` noemt per regio het
aantal delen. Waar de mod één doos nodig heeft (worldborder, poort, kistenscan, het Ei), gebruikt
hij de doos om alle delen heen; bij de cirkel van `vloer` is dat het vierkant eromheen.

| Regio's | Waarvoor |
|---|---|
| `doolhof`, `eigebied`, `mobarena`, `quiz`, `vloer` | Worldborder per ronde. `quiz` is de quizhal. `eigebied` omvat het Ei, de kettingen en hun startplekken, `mobarena` het veld met de tribune en het plein. `vloer` is de vloer van de Arena: een cilinder van 5 hoog, gezet met `/clown vloer <diameter>`; kijkers die erin komen gaan terug naar de tribune. |
| `ei_plein` | De vloer van het plein bij de mob arena, vóór het podium (in delen als dat moet). Na het Ei komt iedereen behalve de winnaar hier op een willekeurige vrije plek te staan. |
| `ei` | Het Ei zelf, als doos: onderhoek en bovenhoek. Alleen hierbinnen kun je in ronde 2 breken; `/ei vastleggen` legt deze doos vast en de mod strooit de puntenblokken op de gewone deepslate erin. Kettingen (iron en copper) blijven ook binnen de doos heel: daar klim je over. |
| `colosseum` | *Optioneel.* De hele Arena inclusief tribunes: border van ronde 5 en 6. Anders `vloer`. |
| `doolhof_uit` | De finishlijn in de afgesloten ruimte achter de echte uitgang: wie erop staat en nog geen team heeft krijgt het teammenu. Een smalle lijn van één blok mag (de mod kijkt elke tick). Moet binnen regio `doolhof` liggen, anders weigert `/doolhof start`. |
| `nep_1` t/m `nep_3` | De vakken aan het eind van de nep-gangen. |
| `schrik_1` t/m `schrik_n` | Schrikplekken in het doolhof. Zoveel als je wilt, genummerd vanaf 1. Per plek een vaste foto of willekeurig met `/doolhof schrik`. |
| `poort_doolhof` | De poort voor de echte uitgang, opent na `/doolhof poort` minuten. |
| `poort_start` | De openingen van de startruimte, elk een deel (`save` + `add`). Zet er zelf barrier blocks in (of glas): tijdens de countdown kan niemand de startruimte uit. Bij de start van de timer haalt de mod ze stil weg (geen hoorn); bij de volgende `/doolhof start` zet hij ze terug, vóór iedereen de startruimte in gaat. Weet de mod na een herstart niet meer wat er stond, dan barrier. |
| `doolhof_gif` | Het doolhof zelf, zonder de finishruimte (in delen als dat moet: `save` + `add`). Na de timer is het hier giftig. Punt `doolhof_finish` mag er niet in liggen, anders weigert `/doolhof start`. |
| `veld` | Het veld van de mob arena, in delen als dat moet (`save` + `add`). Een kijker die erin komt wordt teruggezet, behalve wie in de kooi zit (de tralies houden die binnen); een speler die aan de beurt is en eruit komt ook. Voor kijkers telt een veld tot drie blokken boven de selectie (`/mobarena veldhoogte`), zodat het balkon erboven geen veld is; voor wie aan de beurt is telt alleen de kolom. |
| `tribune_mob` | De tribune van de mob arena: selecteer de vloer waar de kijkers op staan, in delen als dat moet (`save` + `add`). Daarop telt niemand als in het veld, ook waar de selectie over een veld hangt of ermee overlapt: tot drie blokken boven de selectie (springen telt mee), niet eronder. Wie aan de beurt is en de tribune op loopt, gaat terug naar zijn startplek. `/mobarena start` weigert als een `tribune_mob_n` er niet op ligt of een startplek er wel op ligt. |

**Punten** met `/bc point set <naam>` of `/bc point block <naam>`.

| Punten | Waarvoor |
|---|---|
| `basiskamp` | Spawn en reset. |
| `doolhof_start` | De startruimte in het midden van het doolhof; ook waar een nep-uitgang je neerzet. |
| `doolhof_finish` | In de finishruimte: waar wie tijdens het gif doodgaat neerkomt, en waar `/doolhof einde` wie nog in het doolhof liep heen zet. Binnen regio `doolhof`, niet in `doolhof_gif`. |
| `v2` | Verzamelpunt bij het Ei: na `/doolhof naarei`. |
| `ei_spawn_1` t/m `ei_spawn_n` | De startplekken aan het buiteneinde van de kettingen. Zoveel als je wilt, genummerd vanaf 1. Spelers worden er om en om over verdeeld; na een dodelijke klap kom je terug op je eigen startplek. |
| `v3` | Verzamelpunt bij de mob arena, op het plein: voor wie tussen het Ei en de mob arena inlogt. |
| `ei_podium` | Het podium op het plein, kijkend naar het plein: daar komt de winnaar van het Ei. |
| `ei_presentator` | Ook op het podium, naast de winnaar: daar komt de presentator (Pudding) om het Warden-ei uit te reiken, ook als hij staff is. |
| `ei_prijskader` | Het item frame voor het Warden-ei. Zetten met `/ei prijskader` terwijl je naar het frame kijkt. |
| `start_rood_1`, `start_rood_2`, en zo voor `blauw`, `groen`, `geel` | Twee startplekken per team in het veld, niet te dicht bij elkaar. Zetten met `/mobarena startplek <kleur> <1\|2>`. |
| `mob_1` t/m `mob_n` | Mob-spawns in het veld, op zelf gekozen plekken. Zoveel als je wilt; de mobs van een wave gaan er om en om over. |
| `kooi` | De kooi in het veld, waar wie af is de rest van de beurt zit. |
| `warden` | Waar de warden van het Warden-ei uit de grond komt: op de vloer van het veld. |
| `tribune_mob_1` t/m `tribune_mob_n` | De tribune (het balkon). Zoveel als je wilt, allemaal op regio `tribune_mob`. |
| `quiz_rood`, `quiz_blauw`, `quiz_groen`, `quiz_geel` | De vier banken in de quizhal. Zetten met `/quiz bank <kleur>`. |
| `quiz_podium` | Het podium boven aan de trap, waar de presentator staat. Zetten met `/quiz podium`. |
| `quizlamp_<kleur>_1` t/m `_n` (blokken) | De lampen bij elke bank, minstens één per team. Gedoofde redstone lampen, geen redstone ernaast. Zetten met `/quiz lamp <kleur>` (telkens een erbij). Een oude `quizlamp_<kleur>` (van voor 1 oktober) wordt bij het laden vanzelf `quizlamp_<kleur>_1`. |
| (decorlampen) | Geen punten: alle andere redstone lampen in regio `quiz` doen mee aan de lichtshow (R4.6). Bij `/quiz start` krijgen de ops in de chat hoeveel er gevonden zijn. |
| `quizdecor_rood`, `quizdecor_blauw`, `quizdecor_groen`, `quizdecor_geel` (regio's, mag) | De decorlampen achter elke bank: met de wand selecteren en `/bc region save quizdecor_<kleur>` (meer stukken met `/bc region add`). Is dat team aan de beurt, dan branden alleen deze lampen. Zonder regio blijft de lichtshow lopen als dat team aan de beurt is. |
| `quizvuurwerk_<kleur>_1`, `quizvuurwerk_<kleur>_2` (blokken) | Twee dispensers bij elke bank, 8 in totaal. Zetten met `/quiz vuurwerk <kleur> <1\|2>`; `/quiz start` weigert als er een mist of geen dispenser is. |
| `troon` | Het kleine podium in het midden van de Arena, waar de kroonhouder spawnt. Zetten met `/clown troon`. |
| `jager_1` t/m `jager_n` | De startplekken van de jagers: in de Arena de 20 redstone blocks in een cirkel. Zetten met `/clown jagerplek`, bovenop het blok en kijkend waar de speler heen moet kijken; de kijkrichting gaat mee met de teleport. |
| `finale_1`, `finale_2` | De startplekken van de finale in de Arena: 1 voor de winnaar van King of the Hill, 2 voor de winnaar van de FFA. Zetten met `/finale plek 1` en `/finale plek 2`, met de kijkrichting. |
| `tribune_1` t/m `tribune_n` | De plekken op de tribune van de Arena waar de mod iemand neerzet die af is (en iedereen voor het Rad), alleen op de **onderste ring** (de bovenste is decoratie). De mod verdeelt mensen om en om over deze plekken; daarna lopen ze vrij over de tribune. Er gaat meestal maar één tegelijk af, dus twee plekken is genoeg (minimaal één). Zetten met `/clown tribune`. |

Worldborder: `ServerLevel.getWorldBorder()`, center en grootte uit de regio, krimpen met
`lerpSizeBetween`. Altijd eerst teleporteren, dan de border zetten.

## Kits (JSON)

Elke kit is een JSON-bestand in `config/bootcamp/kits/`. De bestanden worden bij elk gebruik
opnieuw gelezen; een ronde weigert te starten als een kit die ze nodig heeft ontbreekt of een fout
bevat.

| Bestand | Wanneer | `clear` |
|---|---|---|
| `basis.json` | Start ronde 1, iedereen: iron armor, iron sword, 32 steak. | true |
| `ei.json` | Start ronde 2: diamond pickaxe met Efficiency II erbij. Alleen voor het Ei: de mod geeft alles uit deze kit `custom_data={bootcamp_ei:1b}` en haalt het aan het einde van ronde 2 weer weg. | false |
| `jager.json` | Iedereen behalve Clown bij de start van ronde 5: volledig diamond armor (Protection IV, Unbreaking III), diamond sword (Sharpness V, Unbreaking III), diamond axe (Sharpness V, Unbreaking III), bow (Power V, Unbreaking III), 32 pijlen, schild (Unbreaking III), 10 golden apples. | true |
| `boss.json` | Clown bij de start van ronde 5: precies de jagerskit, maar zonder helm (de kroon zit al op zijn hoofd). De twee bestanden zijn standaard gelijk; ze mogen later uit elkaar lopen. | true |
| `kroonpakket.json` | Bij elke kroonwissel erbij: 2 gapples, 2 pearls. | false |

**De kroon** is een diamond helm met dezelfde enchants als de kit (Protection IV, Unbreaking III)
plus Curse of Binding en de naam `Kroon` in goud, herkenbaar aan
`custom_data={bootcamp_kroon:1b}`. De uitrusting is gelijk; wel heeft de kroonhouder Strength II
(zie ronde 5). Dat hij de kroon heeft zie je aan Glowing en de zwevende gouden kroon boven zijn
hoofd.
| `arena.json` | Start ronde 6, iedereen: dezelfde kit als `jager.json` (met gewone diamond helm), maar met 32 golden apples in plaats van 10. | true |

Formaat: per slot een item in dezelfde syntax als `/give`, zodat enchantments en andere
components gewoon werken. Een getal achter het item is het aantal.

```json
{
  "clear": true,
  "armor": {
    "head":  "minecraft:iron_helmet",
    "chest": "minecraft:iron_chestplate",
    "legs":  "minecraft:iron_leggings",
    "feet":  "minecraft:iron_boots"
  },
  "offhand": "minecraft:shield",
  "hotbar": [
    "minecraft:iron_sword",
    "minecraft:cooked_beef 32"
  ],
  "inventory": []
}
```

Een kit schrijft nooit over de head-slot van de kroonhouder. Bij `clear: false` komt een item op
zijn slot als dat leeg is en anders ergens in de inventory. De mod herkent de kroon aan het item
zelf (`custom_data={bootcamp_kroon:1b}`).

**Loot-tabel van het doolhof**: `config/bootcamp/doolhof_loot.json`. Bij de start van ronde 1
zoekt de mod alle kisten in regio `doolhof` (de hele doos), maakt ze leeg en vult ze met
willekeurig gekozen items uit de tabel. Gewicht bepaalt hoe vaak een item valt. Een trapped chest
is een valkist: die blijft leeg (zie ronde 1).

```json
{
  "perKist": [2, 4],
  "items": [
    { "item": "minecraft:diamond_chestplate", "gewicht": 2 },
    { "item": "minecraft:diamond_helmet", "gewicht": 3 },
    { "item": "minecraft:iron_sword[minecraft:enchantments={\"minecraft:sharpness\":2}]", "gewicht": 4 },
    { "item": "minecraft:bow", "gewicht": 4 },
    { "item": "minecraft:arrow 16", "gewicht": 6 },
    { "item": "minecraft:golden_apple", "gewicht": 3 },
    { "item": "minecraft:cooked_beef 8", "gewicht": 6 }
  ]
}
```

**Waves** staan in `config/bootcamp/waves.json`: standaard 5 waves (de mod leest er ook meer of
minder, voor een test), per wave een lijst mobs met type, aantal en eventueel gear. Het aantal
geldt voor het hele veld (voor 8 spelers) en schaalt niet mee: een lege startplek maakt de wave
niet kleiner. De mobs gaan om en om over de spawns `mob_1..n`. De mod schrijft dit bestand alleen
als het er nog niet is; een nieuwe standaard krijg je door het bestand weg te gooien.

| Wave | Mobs |
|---|---|
| 1 Zombies en husks | 10 zombies, 4 husks |
| 2 Skeletons en spiders | 8 skeletons (boog), 8 spiders |
| 3 Zombies met iron gear, creepers en skeletons | 8 zombies (iron armor en zwaard), 4 creepers, 4 skeletons |
| 4 Zombies, witches, cave spiders en vindicators | 8 zombies (iron), 4 witches, 6 cave spiders, 2 vindicators (bijl) |
| 5 Ravagers, vindicators en evokers | 2 ravagers, 8 vindicators (bijl), 2 evokers |

## Spellogica

**Status.** Eén `GameState`: huidige ronde, timer, vlaggen, en per speler een rol (`SPELER`,
`JAGER`, `KROON`, `FFA`, `KIJKER`, `STAFF`), een team, en vlaggen (`dood`, `klaar`, `uitverkoren`,
slot, punten, schrikplekken gehad). De mod is de bron van waarheid; scoreboard-tags zijn read-only
spiegels die elke seconde worden bijgezet. `/bc status` toont alles.

**Ontbrekende config.** Elke ronde declareert welke regio's, punten en kits ze nodig heeft.
`/<ronde> start` weigert met één regel ("ontbreekt: kooi, start_geel_2, ...") en verandert
niets. Ronde 3 en 4 weigeren ook als niet iedereen een team heeft: `/bc team` lost dat op.

**Uitloggen en terugkomen.** In ronde 1 t/m 4 kom je terug waar je hoort: in het doolhof bij de
start, in het Ei op je startplek met je punten, in de mob arena op de tribune (wie tijdens zijn
beurt uitlogt telt als dood en speelt geen beurt meer), in de quiz bij je bank (de presentator op
het podium, met zijn items). In ronde 5: een jager die uitlogt is af; de kroonhouder krijgt 30
seconden (voor `/clown go` telt dat niet af: dan weigert `/clown go` tot hij terug is of de commander de
kroon met `/clown kroon` geeft). Wie terugkomt in ronde 5 of 6 wordt kijker op de tribune.

**Fouten.** Een fout in de tick van een ronde stopt de server niet: de mod logt hem, breekt de
ronde af en meldt het in de chat.

**Dood.** `ALLOW_DEATH`: de mod laat spelers nooit echt doodgaan. Bij een dodelijke klap wordt de
dood geannuleerd, de speler geheald en afgehandeld volgens de ronde:

| Ronde | Wat er gebeurt |
|---|---|
| 1 | Geheald terug naar `doolhof_start`. |
| 2 | Geheald terug naar zijn eigen `ei_spawn_n`, punten en spullen blijven. |
| 3 | Kijker in de kooi (`kooi`), na de beurt naar de tribune. Af voor de rest van de mob arena: een latere beurt op het schema blijft leeg. Punten blijven staan. Inventory leeg: wie af is speelt niet meer, en na ronde 3 levert iedereen toch alles in. Doodtekst. Het Warden-ei houdt hij. Zijn alle spelers in het veld af, dan is de beurt klaar. |
| 4 | Kan niet: geen schade. |
| 5, kroonhouder | Kroonwissel naar de killer, anders de laatste hit, anders een willekeurige levende jager. Ex-kroonhouder wordt kijker op de tribune. |
| 5, jager | Kijker op de tribune, af. Is er nog maar één speler over, dan is die de winnaar. |
| 6 | Kijker op de tribune; laatste over is King of the SMP Bootcamp. |

Geen death-screen, geen respawn. De laatste hit komt uit `ALLOW_DAMAGE`.

**Ronde 1, doolhof.** `/doolhof start`: poort en startpoort (`poort_start`) dicht, iedereen naar
`doolhof_start`, basiskit, kisten vullen, border `doolhof`. Dan wacht het doolhof, zoals Clown en
FFA: bossbar `Doolhof · wacht op de start`, elke seconde `/doolhof wachttekst` in de actionbar;
de startpoort houdt iedereen binnen. **`/doolhof go`** start de countdown; daarna gaat de
startpoort stil open en loopt de timer (`/doolhof timer`, standaard 15 minuten). Na
`/doolhof poort` minuten (standaard 4) gaat `poort_doolhof` open, met horn en title
`DE UITGANG IS OPEN` zolang `/doolhof poortmelding` aan staat. De bossbar laat alleen de totale tijd
zien, zodat niemand weet wanneer de uitgang opengaat. Elke 5 ticks: wie in `nep_n` staat krijgt
explosie-particles (`explosion_emitter`), `entity.creeper.primed` plus `entity.generic.explode`, een
willekeurig grapje als title (lijst in `bootcamp.json`) en gaat naar `doolhof_start`; wie in
`schrik_n` staat en die nog niet had krijgt de jumpscare; wie op de finishlijn (`doolhof_uit`, elke
tick gecontroleerd) staat zonder team krijgt het teammenu. Na `/doolhof hint` minuten (standaard 10)
voor wie nog binnen is de title `HINT` met als subtitle `/doolhof hinttekst` (of de windrichting).
**Finish**: achter de echte uitgang ligt een afgesloten ruimte met de finishlijn. Wie een kleur
kiest krijgt de title `GEFINISHT` met `Je zit in Rood`, een chatregel
voor iedereen (`Speler7 zit in Rood (3/4)`) en blijft waar hij is. Wie gefinisht is doet gewoon mee
als hij terug naar binnen loopt, om anderen te helpen of meer loot te zoeken: loot, mobs, valkisten,
schrikplekken en nep-uitgangen werken ook voor hem, en doodgaan zet hem terug in de startruimte.
Zijn team houdt hij. Logt hij uit en weer in, dan gaat hij verder waar hij was. **Valkisten**: een
trapped chest in regio `doolhof` heeft geen loot en gaat niet open. Wie hem opent krijgt (`core`:
`Regels.valkistGok`) 25% de jumpscare met een willekeurige foto, 25% de 8D-klop, óf 50% om zich heen (binnen twee blokken, op een vrije
plek met grond eronder) willekeurig `/doolhof valmobs` mobs (standaard 3 t/m 10), husks en
silverfish door elkaar, elk op een eigen plek als dat past, met een wolkje en
`entity.evoker.prepare_summon`. Staat `/doolhof valmobs` op 0, dan de jumpscare of de klop, 50/50. De mobs
hebben de opener als doel en doen gewone schade; wie doodgaat, gaat terug naar `doolhof_start`. Per
speler gaat een kist één keer af (een dubbele kist is één kist), daarna gaat hij voor die speler
open als lege kist. Staff zet niets af. De mobs verdwijnen aan het einde van het doolhof en komen na
een crash niet terug.

**Timer op: het gif.** Heeft nog niet iedereen een team, of loopt er nog iemand in het doolhof, dan is
het doolhof niet meteen voorbij.
Title voor iedereen `DE TIJD IS OM` met `Het doolhof is giftig · ga naar de finish`,
`entity.elder_guardian.curse`, bossbar `Doolhof · de tijd is om · gif` in paars. Wie in regio
`doolhof_gif` staat (het doolhof zelf, niet de finishruimte) krijgt elke seconde kort Poison I
(weg zodra je eruit loopt) en om de 2 seconden een klap van 1 hart die door armor heen gaat
(`core`: `Regels.DOOLHOF_GIF_ELKE`, `DOOLHOF_GIF_SCHADE`). Poison alleen doodt niet, de klap wel.
Met het gif gaat het regenen en wordt het in 30 seconden nacht (R1.10, `Lucht`, `core`: `Dagtijd`): de
mod zet de stilstaande klok elke tick een stukje verder, tot middernacht, en de regen komt bij de
spelers vanzelf geleidelijk. Aan het eind van het doolhof (ook `/doolhof stop` of `/doolhof einde`)
wordt het in 20 seconden weer middag, door de ochtend heen, en droog; `/bc reset` zet het meteen op
middag en droog.
**Na elke minuut gif verdubbelt de klap** (R1.8, `Regels.gifSchade`): 1, 2, 4, 8 en dan 16 hartjes,
verder niet. Op het moment dat het sterker wordt, ziet wie binnen staat de title `HET GIF WORDT
STERKER` met `4 hartjes per klap · ga naar de finish` en `entity.elder_guardian.curse`; de bossbar
wordt `Doolhof · de tijd is om · gif x4`.
Dat geldt voor iedereen, ook wie al gefinisht is en terug naar binnen liep. **Wie tijdens het gif
doodgaat** (door het gif of anders) raakt al zijn spullen kwijt behalve het eten uit de basiskit
(`basis.json`), krijgt de rest van de basiskit terug, gaat naar punt `doolhof_finish` in de
finishruimte met de title `VERGIFTIGD` en een chatregel `Speler7 bezweek aan het gif`. Heeft hij
nog geen team, dan krijgt hij het kleinste (bij gelijk willekeurig, zodat de teams even groot
blijven), met in de chat `en zit nu in Groen (3/4)`; hij telt niet mee als iemand die de uitgang
vond. Het gif loopt tot iedereen een team heeft en niemand er meer in loopt; `/doolhof einde` is
de noodknop.

**Einde** (R1.9): het doolhof is voorbij als iedereen een team heeft **en niemand meer in het
doolhof loopt** (regio `doolhof_gif`), gecontroleerd bij elke finish, elke dood in het gif en elke
seconde. Niemand wordt weggehaald: wie met een team terug naar binnen ging, loopt zelf naar de
finishruimte of gaat dood in het gif. Heeft iedereen een team maar loopt er nog iemand binnen, dan
een grijze chatregel voor iedereen: `Iedereen heeft een team. Het doolhof is voorbij zodra niemand er
meer in loopt.` Gebeurt dat vóór de timer, dan wacht het doolhof; loopt de timer af, dan begint het
gif voor wie nog binnen staat. Dan: iedereen ziet de title `DOOLHOF VOORBIJ` met als subtitle `14 van
de 20 vonden de uitgang` (wie zelf over de finish kwam). **`/doolhof einde`** (de noodknop) sluit
meteen af: wie nog geen team heeft gaat naar het kleinste team (bij gelijk: willekeurig) en ziet in
de actionbar `Je zit in Groen`, en wie nog in het doolhof loopt gaat naar `doolhof_finish`, met zijn
spullen. Daar kan de host de teams
bespreken; **`/doolhof naarei`** zet daarna iedereen die meedoet naar `v2` bij het Ei, met de
title `OP NAAR HET EI`. Tot dat command is de finishruimte ook het verzamelpunt voor wie
tussendoor inlogt of in de void valt; daarna `v2`. `/doolhof start` begint
zonder teams: het doolhof is de teamkeuze, dus oude keuzes gaan weg.

**Welkom.** Wie joint terwijl er geen ronde loopt, krijgt de title `PUDDING BOOTCAMP` met als
subtitle `Welkom, <naam>` en `block.note_block.chime`, alleen voor hem. Tijdens een ronde niet,
dan komt hij gewoon terug waar hij hoort.

**Ronde 2, het Ei.** Weigert zonder vastlegging (`ontbreekt: /ei vastleggen`) en als de aantallen
van `/ei blokken` samen meer zijn dan de deepslate-plekken. Start:

1. **Het Ei terugzetten**: elk blok in regio `ei` zoals het is vastgelegd, verspreid over een paar
   ticks (een paar duizend blokken per tick) zodat de server niet hapert; klaar voordat de
   countdown afloopt.
2. **Strooien**: uit de vastgelegde plekken met gewone `minecraft:deepslate` trekt de mod
   willekeurig, zonder dubbele, zoveel plekken als `/ei blokken` zegt, en zet daar de puntenblokken
   (`core`: `EiVerdeling`). Varianten zoals cobbled deepslate of deepslate tiles tellen niet mee.
3. Spelers om en om naar `ei_spawn_1..n` (die plek onthoudt de mod per speler), survival,
   `ei.json`. Alleen de pickaxe in de hotbar: wat er uit het doolhof in zat gaat naar de inventory,
   de spullen uit `ei.json` komen vooraan en slot 1 is geselecteerd, hoe de kit ze ook indeelt
   (vooraan zetten is ruilen, er gaat niets verloren). Night Vision zonder deeltjes zolang het Ei
   duurt (binnenin wordt het snel donker; ook na een val en voor wie inlogt, eraf aan het einde),
   border `eigebied`, countdown, timer (`/ei timer`, standaard 15 minuten).

`/ei vastleggen` bewaart alle blokken van de doos `ei` in de wereldmap (`bootcamp_ei.nbt`). Een
doos van meer dan 250.000 blokken weigert hij: dat is een verkeerde selectie.

**Breken** (`PlayerBlockBreakEvents.BEFORE`): buiten regio `ei` geannuleerd. Binnen `ei` breekt
elk blok zonder drop (`level.removeBlock`, event geannuleerd). De gestrooide blokken geven punten
of een effect, alleen op de plekken waar de mod ze strooide: glowstone of slime als versiering in
de schil doet niets. **Alles wat iemand vindt staat in de chat** (behalve iron, daarvan is er te veel), met de naam in de teamkleur
(`Speler7 hakte diamond (+10)`, `Speler7 hakte redstone: iedereen bevroren`, `Speler7 liet
Speler3 schrikken`).

- netherite +50, diamond +10, gold +5, iron +1: `entity.experience_orb.pickup` (bij iron iets hoger), en
  twee seconden lang
  `+10 · 85 punten · #4` in de actionbar. Bij netherite staat de `+50` in paars.
- redstone: 50/50. **Haste**: Haste II 15 seconden voor de breker, `block.beacon.power_select`,
  title `HASTE` met subtitle `15 seconden sneller hakken`, alleen voor hem. **Bevriezing**:
  iedereen behalve de breker 15 seconden bevroren: dezelfde bevriezing als de opstelling, plus
  Mining Fatigue zodat ze ook niet minen. Title voor iedereen: `BEVROREN` met subtitle
  `door <naam>`, `block.glass.break`. Een nieuwe bevriezing vervangt een lopende. Bij beide
  telt de actionbar de resterende seconden af (`Haste · 7`, `Bevroren · 12`) en klinkt een pling
  als het voorbij is.
- emerald: jumpscare bij een willekeurige andere deelnemer in het Ei. Groot in beeld voor
  iedereen: title `Speler7 → Speler3` (namen in teamkleur) met subtitle `JUMPSCARE`. De ander
  staat dan zelf in de foto; hij krijgt dezelfde title zodra de foto wegvaagt, plus drie
  seconden `Met dank aan Speler7` in de actionbar. De breker ziet ook `Jumpscare naar Speler3`
  in de actionbar.
- TNT: een TNT in je inventory (gemarkeerd als Ei-item, dus aan het eind weer weg), title `TNT`
  met `zet hem neer: hij gaat meteen af`. Neerzetten (rechtsklik op een blok) zet geen blok maar
  een brandende TNT op die plek, zonder zwaartekracht zodat hij ook aan de zijkant van het Ei
  blijft hangen; na de gewone 4 seconden knalt hij. Vanilla TNT ontploft nooit (gamerule
  `tnt_explodes` uit): de mod doet de knal zelf. Spelers krijgen gewone TNT-schade en een duw
  (ook de plaatser; wie doodgaat, gaat terug naar zijn startplek). Van de blokken gaat alleen
  gewone deepslate in regio `ei` weg, in een bol met straal 3, zonder drop; speciale blokken, de
  schil en de kettingen blijven staan. Brandt er nog een TNT als het Ei eindigt, dan verdwijnt
  hij zonder knal.
- glowstone: 10 seconden Efficiency V op de pickaxe uit het Ei en Haste II, title `TURBO`. Het
  oude niveau staat in de custom data van de pickaxe; daarna gaat hij terug naar zijn eigen
  Efficiency, met een pling. Nog een glowstone verlengt de 10 seconden.
- slime: alle anderen 15 seconden Nausea, title `MISSELIJK` met `door <naam>`,
  `entity.slime.squish`.
- target: iedereen die meedoet (ook de breker) staat ineens op de plek van een ander, met diens
  kijkrichting; niemand houdt zijn eigen plek (`core`: `Hussel`). Title `GEHUSSELD` met
  `door <naam>` voor iedereen, `entity.enderman.teleport`. Alleen met twee of meer spelers.

Haste, Nausea en Efficiency V lopen door na een val of dood (de heal haalt effecten weg, de mod
zet ze terug voor de tijd die nog over is).

**Actionbar**: elke seconde opnieuw gestuurd, anders verdwijnt hij na een paar tellen. Normaal
`85 punten · #4`: je totaal en je plek in het klassement (dezelfde volgorde als de sidebar).
Vlak na een puntenblok staat er twee seconden wat je erbij kreeg voor (`+10 · ...`). Met 0
punten alleen `0 punten`. Loopt er Haste, Efficiency V of een bevriezing, dan staat die vooraan:
`Bevroren · 12 · 85 punten · #4`, `Efficiency V · 8 · 85 punten · #4`.

**Neerzetten** kan in ronde 2 nergens: blokken (`UseBlockCallback` met een `BlockItem`) en
emmers (`UseItemCallback`) worden geannuleerd. De enige uitzondering is de TNT uit het Ei (zie
boven).

Sidebar: titel `Het Ei · top 10`, per regel de naam in de teamkleur met de punten rechts, bijgewerkt
bij elk puntenblok. De volgorde is die van de mod, ook bij gelijke punten: wie de score het eerst
had staat hoger. De client sorteert gelijke scores zelf op de naam van de regel, dus de regels heten
intern `01` t/m `10` en krijgen de spelersnaam als display name. Wie nog 0 punten heeft staat er
niet in. Geen zoekhints: het Ei is van ver te zien. Timer op: de meeste punten wint (gelijk: wie die
score het eerst had), title, adventure, en de **huldiging** op het plein bij de mob arena: de
winnaar naar `ei_podium`, de presentator naar `ei_presentator` (ernaast, ook als hij staff is; is
hij zelf de winnaar, dan het podium), de rest verspreid over regio `ei_plein` (op een willekeurige
vrije plek op de vloer, niet in elkaar, kijkend naar het podium; lukt dat niet, dan `v3`), en een
tick later een gouden vuurpijl boven het podium. Het **Warden-ei** verschijnt in het item frame van
`/ei prijskader` (punt `ei_prijskader`). Alleen de winnaar of de presentator (`/quiz presentator`,
de spelleider) kan het eruit halen, met een rechtsklik of een klap; dan komt het in zijn inventory,
met een chatregel `Speler7 pakt het Warden-ei`. Anderen zien
`Alleen Speler7 of de presentator kan het Warden-ei pakken`. Staat er geen frame, dan krijgt de
winnaar het meteen. Zonder winnaar blijft het frame leeg en het podium ook. Na een herstart weet de
mod de winnaar niet meer; dan kan alleen de presentator het ei pakken. **De pickaxe gaat weg**: elk
item met `custom_data={bootcamp_ei:1b}` verdwijnt uit inventory, offhand en cursor, ook bij
`/ei stop` en `/bc reset`. Wie tijdens het Ei uitlogde en later terugkomt, raakt hem bij het
inloggen kwijt. Het Ei blijft uitgehakt liggen tot de volgende `/ei start`; ook `/bc reset` zet het
terug (zonder puntenblokken).

**Ronde 3, mob arena.** Eén veld (`veld`); per beurt staan er van elk team twee spelers tegelijk
in. Geen kit: iedereen speelt met wat hij heeft. Start bij `v3`: border `mobarena`, iedereen naar
`tribune_mob_n`, dan het **schema** (`core`: `MobSchema`):

- Per team een willekeurige volgorde van de spelers; in beurt *b* spelen speler *2b* en *2b + 1*.
  Aantal beurten = het grootste team gedeeld door twee, naar boven afgerond: 4 teams van 4 geeft
  2 beurten van 8 spelers. Iedereen speelt één keer.
- Een kleiner team heeft in sommige beurten een plek zonder speler: dat is een **extra beurt**. De
  mod vult hem pas bij de start van die beurt, met een willekeurige speler van dat team die nog
  niet af is en niet al in deze beurt staat. Is er niemand, dan blijft de plek leeg.
- Het schema is **geheim**: het komt niet in de chat. Spelers merken pas dat ze aan de beurt zijn
  als de mod ze bij de start van een beurt naar hun plek teleporteert. Alleen staff kan het
  opvragen met `/mobarena schema`; dat antwoord ziet alleen wie het typt.

**Een beurt** (start met `/mobarena start` voor beurt 1 en `/mobarena volgende` voor de rest):
de ingeplande spelers die nog niet af zijn gaan naar hun eigen startplek, `start_<kleur>_1` en
`start_<kleur>_2` (zo staan de twee van een team nooit in elkaar), geheald en honger vol; de rest
blijft op de tribune. Wie op zijn beurt wacht, houdt daar zijn spullen, krijgt geen schade en telt
voor de mobs als kijker; alleen wie af is wordt echt kijker (inventory leeg). Title `BEURT 2` voor
iedereen (geen subtitle). De spelers die aan de beurt zijn krijgen **Glowing**, dat in hun
teamkleur gloeit, tot het einde van de beurt of tot ze af zijn. **Tot de countdown voorbij is
staan ze stil**: rondkijken kan, lopen en springen niet (dezelfde bevriezing als bij Clown en FFA).
Countdown 5, bij `GO` zijn ze los. Dan 5 waves uit `waves.json`, met tag `bootcamp_mob`,
`setPersistenceRequired()`, stenen knoop op het hoofd tegen zonlicht. Bij elke wave eerst een
wolk `large_smoke`-particles en `block.fire.extinguish` op elk spawnpunt, dan de mobs, om en om
over `mob_1..n`. Zijn alle mobs dood, dan na 5 seconden de volgende wave. Na 120 seconden telt
een wave altijd als klaar (overgebleven mobs weg), behalve de warden (zie onder). De beurt is klaar
na wave 5, of zodra iedereen in het veld af is. Dan meteen mobs en vexes weg en de title
`BEURT 2 KLAAR` met de stand (fade-in 0, 200 ticks blijven), plus voor wie in het veld of de kooi
staat een aftelling van 10 seconden in de actionbar (`Naar de tribune over 7`). **Pas na 10
seconden** gaan de spelers uit het veld en de kijkers uit de kooi naar `tribune_mob_n` (om en om
over de punten). Wie al op de tribune stond, wordt niet verplaatst. In die 10 seconden doet
niemand schade en is er niets meer om te killen. Daarna wacht de mod op `/mobarena volgende`.

**Het Warden-ei** (de prijs van het Ei): een warden spawn egg met `custom_data={bootcamp_warden:1b}`,
naam `Warden-ei`. Vanilla spawnt er nooit een warden mee: rechtsklik in de lucht, op een blok of op
een entity wordt altijd afgevangen. Inzetten kan alleen tijdens de mob arena, als deelnemer op de
tribune (regio `tribune_mob`): niet wie zelf aan de beurt is, niet vanuit de kooi, niet als staff.
Dan is het ei op, iedereen ziet `WARDEN-EI` met `Speler7 zet hem in: de volgende wave is de warden`
(`entity.warden.roar`) en een chatregel. **De volgende wave die start, is de warden** in plaats van
de geplande wave; die vervalt. Tijdens een wave of de pauze erna is dat de volgende wave van deze
beurt; in de laatste wave of tussen twee beurten wave 1 van de volgende beurt. Is er geen volgende
wave meer, dan weigert hij en houdt de speler het ei. Wie het ei heeft en in het veld sneuvelt,
houdt het (de rest van zijn spullen is hij kwijt); een ongebruikt ei gaat aan het einde van de mob
arena weg met de rest.

**De warden-wave**: title `WAVE 3` met `De warden van Speler7`, `entity.warden.emerge`. De warden
komt met de graaf-animatie uit de grond op punt `warden` (spawnreden `TRIGGERED`), met de levens
en klap uit `/mobarena warden` (standaard 200 HP, klap 8 = 4 hartjes); zijn sonic boom doet de
ingestelde schade (standaard 5 = 2,5 hartje) in plaats van 10. Hij kiest zijn doel met woede: de mod
wist woede op wie niet aan de beurt is en zet hem op de dichtstbijzijnde speler in het veld, en
houdt hem boven de grond zolang de wave loopt. Het publiek krijgt geen Darkness. De wave heeft
**geen tijdslimiet**: hij loopt tot de warden dood is, of tot `/mobarena wave volgende`. Een kill
op de warden is `/mobarena punten warden` waard (standaard 50) en komt in de chat.

**Punten**: `ServerLivingEntityEvents.AFTER_DEATH` op een mob met `bootcamp_mob`. De killer is
de speler die de laatste klap gaf, ook via een pijl of andere projectile; vanilla
(`getLastHurtByMob`/`lastHurtByPlayer`) telt een speler die de mob in de laatste 5 seconden
raakte. Punten uit `/mobarena punten` naar het team van die speler, plus één kill voor de
tiebreak. Zonder speler (magma, val) geen punten. Een kill op een **evoker, ravager of de warden**
komt als chatregel voor iedereen: `Speler7 killde de ravager (+10)`, met de naam in de teamkleur.
Andere kills alleen in de actionbar van de killer.

**Af**: wie in de mob arena sneuvelt, ziet eerst de doodtekst als title en daarna in de actionbar
de tekst van `/mobarena aftekst` (standaard `Af · je speelt geen beurt meer`), vijf seconden lang.

**Mobs laten kijkers met rust**: elke tick krijgt een `bootcamp_mob` die een kijker, een staff-lid
of een speler die niet aan de beurt is als doel heeft `setTarget(null)`; daarna kiest hij zelf de
dichtstbijzijnde speler die aan de beurt is. Dat kijkers geen schade krijgen is
niet genoeg: zonder deze check kiezen mobs ze nog steeds als doel en blijven ze om de kooi hangen.
Wie op zijn beurt wacht, kan vanaf de tribune ook geen mob raken: een klap of pijl van een speler die
niet in het veld staat, doet een `bootcamp_mob` niks, en een kill telt alleen voor wie aan de
beurt is.

**Einde**, na de laatste beurt en haar 10 seconden: het team met de meeste punten wint (gelijk:
meeste kills, dan samen), titles en vuurpijlen boven de tribune. Weer 10 seconden om te vieren,
dan **levert iedereen alles in**: inventory, armor en offhand leeg (`clearContent`), ook voor wie
op de tribune stond. Iedereen weer speler, geheald, zonder spullen naar de quiz.

**Ronde 4, quiz.** Weigert zonder presentator (`/quiz presentator`). Start: iedereen naar
`quiz_<kleur>` van zijn team, border `quiz`, geen schade, geen timer. Sidebar `Quiz` met de vier
teams op 0. Iedereen heeft een lege inventory (ingeleverd na de mob arena). De presentator gaat
naar `quiz_podium` en telt in de quiz niet mee voor zijn team; hij krijgt als enige iets: vijf
items in de hotbar, in de volgorde van een vraag en met ruimte tussen de groepen, herkenbaar aan
`custom_data={bootcamp_quiz:"..."}`:

| Slot (toets) | Item | Rechtsklik doet |
|---|---|---|
| 1 | Nether star, naam `Draai het rad` | hetzelfde als `/quiz draai` |
| 3 | Groene wol, naam `Goed` | hetzelfde als `/quiz goed` |
| 4 | Rode wol, naam `Fout` | hetzelfde als `/quiz fout` |
| 6 | Emerald, naam `Punten geven of afpakken` | opent het puntenmenu |
| 9 | Barrier, naam `Quiz beëindigen` | opent de bevestiging: `Ja, de quiz is klaar` (hetzelfde als `/quiz einde`) of `Nee, verder met de quiz`, met de stand ertussen |

Na de winnaar heeft hij alleen nog de ender pearl `Iedereen naar de Arena` in slot 1 (zie *Einde*).
Bij een gelijke stand na `Ja` krijgt hij in de chat dat het nog niet klaar is: een beslisvraag en
een punt met de emerald, of de commander kiest met `/quiz winnaar`.

**Het puntenmenu** is een kistmenu met een rij per team: vooraan de wol met de stand
(`Rood · 5`), daarachter knoppen `−2`, `−1`, `+1` en `+2` (gekleurde glazen panelen). Een klik
past de stand meteen aan, voor elk team, ook als het niet aan de beurt is (voor wie bij Pudding
slijmt); onder 0 mag. Iedereen ziet een chatregel `Pudding: +2 voor Rood (7)` met een pling
(erbij) of een bas (eraf), en de sidebar wordt bijgewerkt. Alleen de presentator kan klikken;
items verplaatsen kan niet.

Rechtsklik wordt afgevangen in `UseItemCallback` én `UseBlockCallback`, zodat de wol nooit als
blok wordt neergezet. De items zijn niet te droppen of te verplaatsen naar een kist; raakt de
presentator er toch een kwijt, dan legt de mod hem terug.

**Het quiz-rad** is een **echt rond rad in beeld**: een plaatje uit het resource pack (zie
*Resource pack*), groot in het midden van het scherm, ongeveer tweederde van de schermhoogte,
met een vast pijltje bovenin. 16 vakken in alleen de teamkleuren, elk team 4 keer, in een vaste
volgorde waarin twee buren (ook rondom) nooit dezelfde kleur hebben:
`rood blauw groen geel blauw rood geel groen rood groen blauw geel groen geel rood blauw`.

Het pack heeft 64 standen van het rad (elk 2 × 2 tegels, zie *Resource pack*), elk 5,625° verder
gedraaid: vier per vak, en elk vierde plaatje heeft een vak precies onder het pijltje. Een draai is
`core`-`QuizDraai` (R4.3): het geluid `bootcamp:rad` (spinwheel, 11,1 s, `preload`) start voor
iedereen op dezelfde tick als het rad, en het rad volgt een vaste tijdlijn die op dat geluid is
gemeten. Van 0,05 tot 0,75 s komt het op snelheid; tot 4,48 s draait het ongeveer anderhalve stand
per tick (sneller kan niet netjes: vanaf twee standen per tick, een half vak, lijkt een rad op 20
beelden per seconde achteruit te draaien); dan gaat er bij elk van de laatste 20 tikjes van het
geluid (4,48 s tot 9,18 s) precies een vakgrens onder het pijltje door; daarna remt het
gelijkmatig af naar het midden van het laatste vak en staat stil op 9,68 s. In totaal 204 standen,
ruim drie rondes. Het doel is een willekeurig vak (elk team 25%); de start volgt daaruit. De stand
volgt de echte tijd sinds de draai, niet het aantal ticks: hapert de server, dan loopt het rad toch
gelijk met het geluid, dat bij de spelers gewoon doorspeelt. Elke tick waarop de stand verandert
een title met de tegels van die stand, fade 0, blijven tot het team in beeld komt. Bij het
plingeltje (10,17 s, tick 203) gaan alle lampen van dat team aan (`quizlamp_rood_1..n`,
`LIT=true`) en knippert het gekozen vak: aan 3 ticks, uit 2, aan 3, uit 2, en dan aan. Opgelicht is
een eigen plaatje uit het pack (`QuizRad.glyphOplicht`, U+E200 + 4 × vak): het rad op de eindstand
van dat vak, met dat vak 30% lichter en een witte rand. Als het geluid uitklinkt (11,1 s, tick
222) de title `ROOD IS AAN DE BEURT` in de teamkleur en bossbar `Quiz · aan de beurt: Rood`; pas
dan is de draai klaar. Terwijl het rad draait of knippert, doen draai, goed en fout niets.

**Decorlampen** (R4.6, `Decorlampen`, `core`: `Lichtshow`): bij `/quiz start` zoekt de mod alle
redstone lampen in regio `quiz` behalve de banklampen (hooguit 4 miljoen blokken doorzoeken) en
meldt het aantal aan de ops. Elke lamp krijgt zijn hoek rond het midden van de regio, met de klok
mee vanaf het noorden. Vier lichtbalken van elk 40% lopen rond: tussen de draaien een rondje per
16 seconden; tijdens een draai draaien ze precies mee met het rad (`QuizDraai.rondjes`); op het
plingeltje gaan ze allemaal tegelijk aan en uit met het knipperende vak. Is daarna een team aan de
beurt, dan branden alleen de lampen in regio `quizdecor_<kleur>` van dat team, stil, en is de rest
uit (lampen in zo'n regio doen ook mee als ze net buiten `quiz` staan); na een fout (niemand aan de
beurt) lopen ze weer rustig rond vanaf waar ze waren. Na de winnaar branden die van het winnende
team. Bij de start noemt de chatregel voor de ops ook hoeveel lampen elk team heeft. Alleen lampen die echt veranderen krijgen een nieuw blok (zonder
de buren bij te werken), en elke seconde zet de mod alles nog eens goed. Na de quiz en bij `/bc
reset` gaan ze uit. De hal donkerder maken kan de mod niet (binnen, frog lights): wil je dat de
lampen meer opvallen, vervang dan wat frog lights door redstone lampen; die doen dan mee. De lampen doen
niet mee aan het draaien.

**Goed**: alleen als er een team aan de beurt is: +1 voor dat team in de sidebar, title `GOED!`
in groen met `+1 Rood`, `block.note_block.bell`, en uit de twee dispensers bij de bank van dat
team (`quizvuurwerk_rood_1` en `_2`) een vuurpijl in de teamkleur, de kant op waar de dispenser
naartoe wijst (er hoeft niets in te zitten). Het team blijft aan de beurt, de lamp blijft
aan. De mod telt de **reeks** goede antwoorden van het team dat aan de beurt is; vanaf twee op
rij staat die in de subtitle: `+1 Rood · 3 op rij`. Een fout of een nieuwe draai zet de reeks op
nul. **Fout**: title `FOUT!` in rood, `entity.villager.no`, geen punt; de lamp gaat uit, niemand
meer aan de beurt, bossbar `Quiz · draai het rad`. Zonder team aan de beurt krijgt de
presentator één regel: `Draai eerst het rad`. Bij het begin van elke draai zijn alle vier de
lampen uit.

**Hulpregel voor de presentator**: alleen de presentator ziet in de actionbar, elke seconde
opnieuw, `Aan de beurt: Rood · 3 op rij`, of `Draai het rad` als niemand aan de beurt is.

**Einde**: `/quiz einde` kiest het team met de meeste punten; bij gelijke stand weigert hij met de
teams die gelijk staan, ziet iedereen de title `GELIJKSPEL` met subtitle `Rood en Geel · Pudding
kiest`, en wijst de commander er een aan met `/quiz winnaar <kleur>` (Pudding kan er eerst een
beslissende vraag van maken). Title
`ROOD WINT DE QUIZ`, `ui.toast.challenge_complete`, vuurpijlen boven de bank, de lamp van de
winnaar aan. Niemand gaat vanzelf weg: de presentator houdt alleen nog een ender pearl
`Iedereen naar de Arena` in slot 1 (met in de actionbar `Klaar met vieren? De ender pearl stuurt
iedereen naar de Arena`). Gebruikt hij die, of doet de commander `/quiz naararena`, dan: alle
lampen uit, de quiz-items weg (weer een lege inventory, zoals iedereen), en iedereen naar de
tribune van de Arena (`tribune_n`, om en om) voor het Rad. Raakt hij de pearl kwijt of logt hij
opnieuw in, dan krijgt hij hem terug. `/quiz stop` en `/bc reset` zetten de lampen ook uit.

**Het Rad (start van ronde 5).** Iedereen staat op de tribune (wie er nog niet staat, zet
`/clown rad` er eerst neer). Het rad staat **alleen in beeld, als een rij spelerskoppen met
namen** (het ronde rad met plaatjes is alleen voor de quiz): n = het aantal deelnemers, elk vak
één speler in een willekeurige volgorde, het doelslot is het vak van de uitverkorene.
State-object met `pos`, `rest` en `volgendeStapTick`. Start: `rest` =
`(doelslot - pos + n) mod n + n * (2 of 3)`, `pos` willekeurig. Per stap: pos + 1, `rest` - 1,
title met de **kop en naam** van wie in dat vak zit, en als subtitle de rij van vijf: de twee
buren aan elke kant en in het midden, in goud tussen `▶ ◀`, wie er nu staat, elk met kopje en
naam. Hat-geluid; de wachttijd loopt op van 2 naar 30 ticks naarmate `rest` kleiner wordt. Bij
0: dragon growl, totem-particles, title `DE KROON` met de kop en de naam, drie seconden later
`start(5)`. Het quiz-rad gebruikt dezelfde rekenlogica over 64 plaatjes, met een willekeurig
doel. `/clown start` slaat het rad over en geeft de kroon meteen aan de uitverkorene.

**Spelerskoppen in tekst**: een tekstcomponent van het type `object` met een spelerskop (sinds
Minecraft 1.21.9, dus ook in 26.2), op naam of UUID van de speler. De client tekent de kop met de
echte skin; er hoeft niets in het resource pack. Gebruikt in het Rad, `DE KROON`, `NIEUWE KROON`,
de winnaar van het Ei, de winnaar van King of the Hill en de kroning. Controleren in de eerste test;
werkt het in 26.2 anders dan gedacht, dan valt de mod terug op alleen de naam.

**Ronde 5, Clown vs All** (in beeld overal **King of the Hill**: `Ronde.CLOWN` heeft die naam, zodat
niemand merkt dat het rigged is). Iedereen uit zijn teamkleur en van de tribune de vloer op. Clown naar
`troon` (het podium in het midden), eerst de kroon, dan `boss.json`, Glowing, team `kroon`. De
rest `jager.json`, team `jagers`, **willekeurig verdeeld over `jager_1..n`**, één per plek (zijn
er meer jagers dan plekken, dan delen ze om en om), met de kijkrichting van de plek. Iedereen
heeft dezelfde uitrusting; alleen de helm van Clown is de kroon. Dan **Opstelling zonder
countdown**: iedereen bevroren, ook Clown, tot de commander `/clown go` doet; bossbar
`King of the Hill · wacht op de start`, en in de actionbar van iedereen op de vloer, elke seconde,
de tekst van `/clown wachttekst` (standaard `Wacht op het startsein`). Border `colosseum` (of
`vloer`), geen timer, locator bar aan met alleen de kroonhouder zichtbaar. Elke seconde: is er
nog maar één levende deelnemer, dan einde.

**Strength** (R5.11, `Regels.kroonSterkte`): de mod zet het elke seconde, en meteen bij de start, een
kroonwissel of een jager die afvalt. Zijn er meer dan `SLOTSTRIJD` (3) levende deelnemers, dan heeft
de kroonhouder Strength II (oneindig) en de jagers niks; bij 3 of minder heeft iedereen Strength I.
Omlaag van II naar I gaat via weghalen en opnieuw geven, want vanilla houdt anders het sterkste
effect. Bij het einde gaat Strength er bij iedereen af.

**Actionbar van de kroonhouder**: zolang niemand stil hoeft te staan, elke seconde
`Jij hebt de kroon · 11 jagers`, alleen voor hem.

**Af**: wie in ronde 5 afvalt (jager of oude kroonhouder), geeft een chatregel voor iedereen:
`Speler3 is af door ClownPierce · 11 over`, of zonder killer `Speler3 is af · 11 over`, met de
namen in hun teamkleur (aqua, goud). De dode ziet daarnaast zijn doodtekst als title.

**Kill van de kroonhouder** (`kroonKill`): doodt de kroonhouder een jager en is de ronde daarna
niet voorbij, dan krijgt iedereen behalve de dode een title: kop en naam van de kroonhouder in
hoofdletters (goud), subtitle `pakt Speler3 · 11 over`, 5/40/15 ticks, en `entity.ravager.roar`.

**Winnaar**: title met kop en `SPELER7 WINT KING OF THE HILL`, ook als het de uitverkorene is
(geen aparte title voor Clown, die zou het rigged rad verraden); zonder winnaar
`KING OF THE HILL IS VOORBIJ`. Vuurpijl erboven,
`ui.toast.challenge_complete`.

**Kroonwissel** (`Kroon.wissel(oude, nieuwe)`): oude wordt kijker op de tribune; nieuwe naar
`troon`, heal, honger vol, alles op volle durability, de kroon als helm (oude helm naar de
inventory), `kroonpakket.json`, Glowing, Strength II (vanaf de 1v1v1 I), zweefkroon (geen
Resistance); dan
`Opstelling(10)`: deze countdown loopt **vanzelf**, zonder `/clown go`.

**Opstelling**: alle levende jagers heal en opnieuw willekeurig naar `jager_1..n` (met kijkrichting),
de kroonhouder op `troon`, iedereen bevroren. Na de start van ronde 5 wacht de opstelling op
`/clown go`; na een kroonwissel start hij meteen. Countdown van 10 seconden in de actionbar met
de laatste vijf als title plus pling, dan los met een groene GO en de raid horn. Bevriezen is
`MOVEMENT_SPEED` en `JUMP_STRENGTH` op basiswaarde 0 plus een `UseItemCallback` die zolang de vlag
staat alles blokkeert waarmee je beweegt of schiet: bogen, crossbows, tridents, pearls, wind
charges en chorus fruit. Eten (gapples) mag. Tijdens een opstelling doet niemand elkaar schade.

**Ronde 6, FFA en kroning.** In de Arena van ronde 5. Iedereen behalve Clown, ook wie af was,
willekeurig naar `jager_1..n` (één per plek, met kijkrichting), full hp, `arena.json`, geen team;
Clown als kijker naar `tribune_n`. Dan de opstelling zonder countdown: iedereen bevroren (zoals
in ronde 5, ook schieten en pearls geblokkeerd) tot de commander `/ffa go` doet; bossbar
`FFA · wacht op de start`, actionbar `/ffa wachttekst` (standaard `Wacht op het startsein`). Na
`/ffa go` 10 seconden countdown, dan los. **Geen timer**: elke seconde, is er nog maar één
levende deelnemer, dan wint die de FFA. De border staat om `colosseum` (of het
vierkant om `vloer`) en krimpt alleen met `/ffa krimp <grootte> [<seconden>]`, rond het midden
van de vloer.

In beeld tijdens de FFA:

- **Sidebar `Kills`**: de spelers met de meeste kills, bijgewerkt bij elke kill (gelijk: wie het
  eerst op dat aantal kwam, hoger). Wie nog 0 kills heeft staat er niet in.
- **Af**: chatregel voor iedereen `Speler3 is af door Speler7 · 11 over`, of zonder killer
  `Speler3 is af · 11 over`. De dode ziet zijn doodtekst.
- **Kill in beeld** (`Arena.killInBeeld`, dezelfde als bij King of the Hill): title met kop en naam
  van de killer in goud, subtitle `pakt Speler3 · 11 over`, 5/40/15 ticks, `entity.ravager.roar`
  op half volume. Alleen voor wie niet (meer) vecht (tribune, Clown, staff) en voor de killer;
  de andere vechters krijgen alleen de chatregel. Niet als er bij die kill `LAATSTE DRIE` of
  `LAATSTE TWEE` komt, en niet bij de laatste kill.
- **Actionbar**: elke seconde en na elke kill, voor iedereen die nog vecht, `3 kills · 11 over`
  (`1 kill` enkelvoud), in goud. Niet tijdens het wachten op `/ffa go` en de countdown.
- **`/ffa krimp`**: title `DE BORDER KRIMPT` in rood met subtitle `naar 20 in 60 seconden`,
  `event.raid.horn`.
- **Laatste drie en laatste twee**: zodra er drie over zijn title `LAATSTE DRIE` met de drie
  namen, bij twee `LAATSTE TWEE` met `Speler7 tegen Speler2`, in paars, met
  `entity.wither.spawn` zacht.

**Einde van de FFA**: title met kop en `SPELER7 WINT DE FFA`, vuurpijl, toast-geluid; de winnaar
blijft op de vloer. De mod bewaart de winnaar en de nummer twee (wie als laatste afviel of
uitlogde) in `bootcamp.json` onder `uitslag`, voor de finale. De sidebar met de kills blijft staan
tot de finale begint.

**Ronde 7, de finale.** In de Arena van ronde 5 en 6. De finalisten komen uit `uitslag`
(R6.6): de winnaar van King of the Hill (bewaard aan het einde van ronde 5) tegen de winnaar van
de FFA; is dat dezelfde, dan de winnaar tegen de nummer twee van de FFA. `/finale spelers <a> <b>`
zet ze met de hand (als winnaar van King of the Hill en van de FFA, zonder nummer twee). `/finale
start` weigert als er geen twee finalisten zijn, als een van beiden niet online is of in creative
of spectator staat, of als `arena.json` niet klopt. Bij de start: iedereen anders als kijker naar
`tribune_n`; de twee finalisten rol `FFA`, adventure, effecten weg, full hp, `arena.json`, de
winnaar van King of the Hill naar `finale_1` en de ander naar `finale_2` (met kijkrichting). De
sidebar gaat weg. Title voor iedereen `DE FINALE` met als subtitle `[kop] ClownPierce tegen [kop]
Speler7`, `event.raid.horn`. Dan bevroren tot `/finale go`: bossbar `De Finale · wacht op de
start` (rood), actionbar `/finale wachttekst`. Na `/finale go` 10 seconden countdown, dan los;
bossbar `De Finale · ClownPierce tegen Speler7`. Border om `colosseum` (of `vloer`), krimpt alleen
met `/finale krimp`. Een dode finalist wordt kijker met een doodtekst, chatregel `Speler7 is af
door ClownPierce · 1 over`, en de ander krijgt meteen de kroning. Logt een finalist uit (R7.1),
dan **pauzeert** de finale: een lopende countdown stopt, de ander wordt bevroren (`/finale go`
weigert), bossbar `De Finale · Speler7 is weg · Pudding beslist` (rood), actionbar voor de ander
`Speler7 is weg · even wachten`, en een gouden chatregel alleen voor de ops (en de console) met
beide keuzes. Er wordt geen winnaar gecontroleerd zolang de pauze loopt. `/finale combatlog`:
wie uitlogde telt als dood en de ander krijgt meteen de kroning, zonder melding over de combat
log; is de ander ook weg, dan weigert het. `/finale crash`: de finale stopt, gele chatregel voor
iedereen `De finale is gestopt en begint opnieuw zodra Speler7 terug is.`; de commander start hem
opnieuw. Logt hij in tijdens de pauze, dan wordt hij kijker en krijgen de ops een melding. `/bc kijker <speler> uit` werkt niet tijdens de finale. `/bc reset` wist de uitslag.

**Kroning** (na de finale): iedereen naar de tribune, de winnaar naar `troon` (het podium) met de kroon en de
zweefkroon, twintig seconden vuurpijlen, title `KING OF THE SMP BOOTCAMP` met de kop en de naam
als subtitle. Daarna blijft de bossbar `Pudding Bootcamp · King: Speler7` (goud) staan en blijft
de zweefkroon boven de winnaar, tot `/bc reset`.

## Kijkers: wie af of dood is

Geen spectator mode, geen tp-items, geen vliegen.

- Adventure mode, team `out` (grijs in de tab-list), inventory leeg. In ronde 3 houdt alleen wie op
  zijn beurt wacht zijn spullen op de tribune; wie af is, is ze kwijt.
- Geen schade, ook niet van de border. Een kijker ziet de border niet: hij krijgt een eigen
  border-pakket zo groot als de wereld, anders geeft de client een rood scherm.
- Blijft op zijn plek: glas of tralies, en een tick-check die een kijker die toch in `vloer`,
  `veld` komt terugzet op zijn tribunepunt of in zijn kooi (op regio `tribune_mob` telt
  geen veld). Bij `vloer` telt de
  cilinder: binnen de cirkel én binnen de 5 blokken hoogte. Een kijker die van de tribune de
  arena in springt of loopt, staat dus meteen weer op de tribune, met title `Terug naar de
  tribune`.
- Mobs laten kijkers met rust (zie ronde 3).
- Niet op de locator bar, geen Glowing.
- Bij de dood een title met een willekeurige doodtekst, alleen voor de dode zelf. De lijst staat
  in `bootcamp.json` en gaat in-game met `/bc doodtekst`. Standaard zes: `Grote L gepakt!`,
  `Kleine L gepakt`, `Had je nou maar beter je best gedaan`, `Gelukkig is dit de CSMP niet..`,
  `Dag 1...`, `Op de lijst..`. Staat in een oude `bootcamp.json` nog precies de oude standaard
  (drie teksten), dan maakt de mod er bij het laden de nieuwe zes van.

Waar kijkers heen gaan: ronde 3 bij een dood naar `kooi` tot het einde van die
beurt, en anders naar `tribune_mob_n`; ronde 5 en 6 naar `tribune_n`. Clown zit tijdens de FFA ook op de
tribune.

**Staff** gebruikt spectator of creative voor de camera; de mod dwingt daar niks af.

## Voice

Niks te doen in de mod. Simple Voice Chat draait ernaast op puur proximity. Details in
[07-voice.md](07-voice.md).

## Bossbar en visuals

Eén bossbar, kort, altijd hetzelfde formaat. Persoonlijke info via de actionbar.

| Ronde | Tekst | Kleur | Vulling |
|---|---|---|---|
| Basiskamp | `Pudding Bootcamp` | wit | vol |
| 1, voor `/doolhof go` | `Doolhof · wacht op de start` | groen | vol |
| 1 | `Doolhof · 04:41` | groen, de laatste minuut rood | tijd; de poort staat er niet in |
| 1, na de timer | `Doolhof · de tijd is om · gif`, na elke minuut `gif x2`, `x4` ... | paars | vol |
| 2 | `Het Ei · 07:12` | groen, de laatste minuut rood | tijd |
| 3 | `Mob Arena · beurt 2/2 · wave 2` | rood | mobs over in het veld |
| 4 | `Quiz · aan de beurt: Groen`, of `Quiz · draai het rad` | teamkleur, wit als niemand aan de beurt is | vol |
| 5 | `King of the Hill · Kroon: Clown · 12 over` | geel | spelers over |
| 6 | `FFA · 7 over` | paars | spelers over |
| 7 | `De Finale · ClownPierce tegen Speler7` | rood | vol |
| Na de kroning | `Pudding Bootcamp · King: Speler7` | goud | vol |

**Sidebar**: ronde 1 de teams met aantallen en namen (`Rood 3/4` met de spelers eronder), ronde 2 de top 10 op punten, ronde 3
de teamstand in punten (`Rood 47`), ronde 4 de quizpunten, ronde 5 de regeerperiodes, ronde 6 de
kills (die blijven staan tot de finale begint; in de finale geen sidebar).

**Actionbar in ronde 3**, voor wie aan de beurt is: na een kill twee seconden `+3 · Rood 47` (wat
je kreeg en de stand van je team).

**Zweefkroon**: een `Display.ItemDisplay` met een gouden helm (de zichtbare kroon; de echte kroon
is een diamond helm) boven het hoofd van de kroonhouder,
langzaam draaiend. Opgeruimd als de kroonhouder kijker wordt.

**Labels**: een `Display.TextDisplay` boven elk verzamelpunt, één keer geplaatst met
`/bc label zet <tekst>`. Ze overleven `/bc reset`.

**Per moment**

| Moment | Wat je ziet en hoort |
|---|---|
| Joinen in het basiskamp | Title `PUDDING BOOTCAMP`, subtitle `Welkom, <naam>`, `block.note_block.chime`. Alleen voor wie joint, en niet tijdens een ronde. |
| Countdown | Titles 5 t/m 1 in goud met een stijgende `note_block.pling`, dan `GO` met `event.raid.horn`. |
| Poort doolhof open | Cloud-particles in de poort; met `/doolhof poortmelding aan` ook `event.raid.horn` voor iedereen en de title `DE UITGANG IS OPEN` in groen. |
| Valkist | 25% de jumpscare (willekeurige foto) voor wie hem opent, 25% de 8D-klop, 50% mobs met `poof`-particles en `entity.evoker.prepare_summon`. |
| Nep-uitgang | Explosie-particles, creeper-sis en knal, grapje als title, terug in de startruimte. |
| Hint doolhof | Title `HINT` in geel, subtitle de hinttekst, voor wie nog binnen is. |
| Doolhof voorbij | Title `DOOLHOF VOORBIJ`, subtitle `14 van de 20 vonden de uitgang`; wie in een team is gezet ziet in de actionbar `Je zit in Groen`. |
| Jumpscare | Een van de vijf foto's schermvullend (vast per schrikplek, of willekeurig), `bootcamp:schrik`. |
| 8D-klop | Alleen het geluid `bootcamp:klop`, stereo, alleen voor wie de valkist opende. |
| Team gekozen | `entity.player.levelup`, je naam in de teamkleur, chatregel voor iedereen: `<naam> zit in Rood (3/4)`. |
| Ei: punten | `entity.experience_orb.pickup`, actionbar met wat je erbij kreeg, je score en je plek. |
| Ei: netherite | `+50` in paars in de actionbar, chatregel voor iedereen: `Speler7 hakte netherite (+50)`. |
| Ei: redstone | Haste (15 seconden): `block.beacon.power_select`, title `HASTE` voor de hakker. Bevriezing: title `BEVROREN` met `door <naam>`, `block.glass.break`. Aftellen in de actionbar, pling als het voorbij is. |
| Ei: emerald | Iedereen ziet de title `Speler7 → Speler3` met `JUMPSCARE`; de ander krijgt eerst de jumpscare, dan die title en `Met dank aan Speler7`; de hakker ziet ook `Jumpscare naar Speler3`. |
| Ei: TNT | Title `TNT` voor de hakker; neergezet `entity.tnt.primed`, na 4 seconden de knal met `explosion_emitter`. |
| Ei: glowstone | Title `TURBO`, `block.beacon.activate`, `Efficiency V · 8` in de actionbar, pling als het voorbij is. |
| Ei: slime | Title `MISSELIJK` met `door <naam>` voor de anderen, `entity.slime.squish`. |
| Ei: target | Title `GEHUSSELD` met `door <naam>` voor iedereen, `entity.enderman.teleport`. |
| Laatste minuut (doolhof, Ei) | Bossbar wordt rood. De laatste 10 seconden staan groot in beeld in rood, met `block.note_block.hat` per tel. |
| Winnaar Ei | Title met de kop van de winnaar en `SPELER4 WINT HET EI` in goud, subtitle `185 punten`, `ui.toast.challenge_complete`; winnaar op het podium, gouden vuurpijl erboven, het Warden-ei in het frame. |
| Start mob arena | `ui.toast.challenge_complete`; het schema blijft geheim. Bij elke beurt de title `BEURT 3` voor iedereen, zonder subtitle. |
| Spelers in de mob arena | Glowing in hun teamkleur tijdens hun beurt. |
| Nieuwe wave | Rookwolk en `block.fire.extinguish` op de spawnpunten, dan title `WAVE 3` in rood, `event.raid.horn`. |
| Warden-ei ingezet | Title `WARDEN-EI` met `Speler7 zet hem in: de volgende wave is de warden`, `entity.warden.roar`, chatregel. |
| Warden-wave | Title `WAVE 3` in donker-aqua met `De warden van Speler7`, `entity.warden.emerge`; de warden komt uit de grond. |
| Mob gekild (ronde 3) | `entity.experience_orb.pickup` voor de killer, actionbar met de punten. Evoker en ravager ook als chatregel voor iedereen. |
| Af in de mob arena | Doodtekst als title, daarna 5 seconden `/mobarena aftekst` in de actionbar. |
| Speler sneuvelt | Alleen de dode ziet een willekeurige doodtekst als title. Geen geluid, geen chatregel. |
| Beurt klaar | Meteen de title `BEURT 3 KLAAR` met de stand van de teams, 10 seconden in beeld, met `ui.toast.challenge_complete`. Aftelling in de actionbar voor wie in een arena of kooi staat; daarna pas naar de tribune. |
| Quiz-rad | Een echt rond rad groot in beeld (plaatjes uit het pack), alleen kleuren, pijltje bovenin. Het geluid `bootcamp:rad` loopt mee: bij elk tikje aan het eind een vak, stil op 9,7 s, en bij het plingeltje (10,2 s) knippert het gekozen vak en gaan de lampen aan; als het geluid uit is (11,1 s) `ROOD IS AAN DE BEURT`. `/quiz winnaar` of `/quiz stop` tijdens een draai stopt ook het geluid. |
| Quiz goed | Title `GOED!` in groen met `+1 Rood`, vanaf twee op rij `+1 Rood · 3 op rij`, `block.note_block.bell`, een vuurpijl in de teamkleur uit elk van de twee dispensers bij de bank. De lamp bij de bank blijft aan. |
| Puntenmenu | Chatregel `Pudding: +2 voor Rood (7)`, `note_block.pling` (erbij) of `note_block.bass` (eraf). |
| Quiz fout | Title `FOUT!` in rood, `entity.villager.no`. De lamp bij de bank gaat uit, de reeks op nul. |
| Quiz, presentator | Alleen voor Pudding in de actionbar: `Aan de beurt: Rood · 3 op rij` of `Draai het rad`. |
| Quiz gelijkspel | Title `GELIJKSPEL`, subtitle `Rood en Geel · Pudding kiest`. |
| Het Rad | In beeld: kop en naam van wie onder het pijltje staat als title, de rij van vijf koppen met namen als subtitle, `note_block.hat` per stap. Aan het eind `entity.ender_dragon.growl`, totem-particles, title `DE KROON` met kop en naam. |
| Kroonwissel | `entity.lightning_bolt.thunder` (geen echte bliksem), flash-particle, title `NIEUWE KROON` met kop en naam. |
| Winnaar ronde | `ui.toast.challenge_complete`, vuurpijl, title met naam of team. Bij het Ei en King of the Hill met de kop van de winnaar. |
| Wachten op `/clown go` | Bossbar `King of the Hill · wacht op de start`, actionbar `/clown wachttekst` (standaard `Wacht op het startsein`). |
| Kroonhouder | Alleen voor hem in de actionbar: `Jij hebt de kroon · 11 jagers`. |
| Af in Clown vs All | Chatregel voor iedereen: `Speler3 is af door ClownPierce · 11 over`. De dode ziet zijn doodtekst. |
| Kill van de kroonhouder | Iedereen behalve de dode: title met kop en naam van de kroonhouder, subtitle `pakt Speler3 · 11 over`, brul van een ravager. Niet bij de laatste kill. |
| Winnaar Clown vs All | Title met de kop van de winnaar en `SPELER7 WINT KING OF THE HILL`, ook als het de uitverkorene is. Vuurpijl erboven. |
| Wachten op `/ffa go` | Bossbar `FFA · wacht op de start`, actionbar `/ffa wachttekst`. |
| Af in de FFA | Chatregel voor iedereen: `Speler3 is af door Speler7 · 11 over`. |
| Kill in de FFA | Tribune, staff en de killer: title met kop en naam van de killer, subtitle `pakt Speler3 · 11 over`, zachte brul van een ravager. Niet bij LAATSTE DRIE/TWEE en niet bij de laatste kill. |
| Vechter in de FFA | Actionbar `3 kills · 11 over`. |
| Winnaar FFA | Title met de kop van de winnaar en `SPELER7 WINT DE FFA`, vuurpijl, toast-geluid. |
| Start finale | Title `DE FINALE` voor iedereen, subtitle met koppen `ClownPierce tegen Speler7`, `event.raid.horn`. |
| Wachten op `/finale go` | Bossbar `De Finale · wacht op de start`, actionbar `/finale wachttekst`. |
| Finalist uitgelogd | Bossbar `De Finale · Speler7 is weg · Pudding beslist`, actionbar voor de ander `Speler7 is weg · even wachten`, chatregel met de keuzes alleen voor de ops. |
| `/clown krimp`, `/ffa krimp` | Title `DE BORDER KRIMPT` in rood, subtitle `naar 20 in 60 seconden`, `event.raid.horn`. Kijkers zien de title ook, maar merken niets van de border. |
| Laatste drie, laatste twee | Title `LAATSTE DRIE` of `LAATSTE TWEE` in paars met de namen, `entity.wither.spawn` zacht. |
| Kroning (na de finale) | Twintig seconden vuurpijlen, title `KING OF THE SMP BOOTCAMP` met de kop en de naam van de winnaar als subtitle. Daarna blijven de bossbar `Pudding Bootcamp · King: Speler7` en de zweefkroon tot `/bc reset`. |

## Zo is dit gevibecode

1. **Volgorde.** Zie [08-taakplan.md](08-taakplan.md). Na elke stap iets testbaars.
2. **Context.** Deze doc plus [02-rondes.md](02-rondes.md) en
   [03-kroon-regels.md](03-kroon-regels.md) zijn de spec.
3. **Compileren.** `./gradlew build` groen voordat je verder gaat. Een naam die niet bestaat is
   een 1.21-gok: `./gradlew :fabric:genSources` en de echte naam opzoeken.
4. **Geen magie.** Alles tick-gestuurd, alles via `/bc reset` terug te draaien, alle
   coördinaten uit `bootcamp.json`.
5. **Testrun** met de checklist uit [05-draaiboek.md](05-draaiboek.md), en de jar van de vorige
   werkende versie bewaren.

## Oude versies

Het oude rondeplan (horde, Ei met ticket, King of the SMP, finale) staat in de git-geschiedenis
tot commit `b93727a`. De Paper + Skript-schets staat in `d351fd1`, de datapack-versie in
`e51e143`.
