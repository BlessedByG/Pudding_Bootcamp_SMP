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
> Ei, mob arena, quiz, Clown vs All, FFA). De code in `mod/` is op 28 en 29 september 2026 naar dit
> plan omgebouwd (taakplan 2, [08-taakplan.md](08-taakplan.md)): hij bouwt, alle tests zijn groen,
> maar hij is nog niet in-game gedraaid. Wat er afwijkt van deze doc, staat hieronder bij het
> onderwerp en in [mod/BOUWLOG.md](../mod/BOUWLOG.md). Een naam uit 26.2 opzoeken:
> `./gradlew :fabric:genSources`.

## Stack

| Wat | Waarvoor |
|---|---|
| Fabric-server 26.2 + Fabric API | De server en de event/command-API. |
| `bootcamp`-mod (deze repo, map `mod/`) | Alles wat hieronder staat. |
| Server resource pack (deze repo, map `pack/`) | De foto van Clown en het lachje voor de jumpscare. |
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
gebruikt. In ronde 6 zit iedereen zonder team.

**Maximum per team**: 5, of meer als er meer dan 20 spelers zijn: `max(5, ceil(spelers / 4))`,
met spelers = iedereen die meedoet op het moment dat de eerste kiest.

**Het teammenu** is een vanilla kistmenu (`ChestMenu`, 4 rijen, `MenuType.GENERIC_9x4`) met een
`SimpleContainer`, een rij per kleur. Vooraan een gekleurd wolblok met de teamnaam en het aantal
(`Rood · 3/5`); een vol team is grijze wol. Daarachter de hoofden van wie er al in zit, met hun skin
(online met hun eigen profiel, offline op naam) en hun naam in de teamkleur; zijn het er meer dan
acht, dan zeven hoofden en `+3 meer`. Elke seconde bijgewerkt. Items verplaatsen kan niet (het menu
annuleert elke klik en handelt hem zelf af). **Alleen een klik op de wol** zet je in het team en
sluit het menu; een klik op een hoofd doet niets. Je blijft waar je bent (zie ronde 1). Sluit je het
menu zonder keuze terwijl je op de finishlijn (`doolhof_uit`) staat, dan opent het na 2 seconden
opnieuw. Allemaal server-side, geen client-mod nodig.

**De sidebar in het doolhof** (`Teams`) laat per kleur `Rood 3/5` zien (vet, in de teamkleur) en
daaronder wie erin zit, met het hoofd ervoor als hij online is. De sidebar heeft maar 15 regels
(`core`: `TeamOverzicht`): past het niet met één naam per regel, dan komen er twee, drie of meer naast
elkaar, zo weinig als kan. Elke seconde bekeken (ook `/bc team` en wie in- of uitlogt), alleen
opnieuw gestuurd als er iets veranderde.

## Resource pack en de jumpscare

Het pack staat in `pack/` in deze repo. **Aanleveren** gaat in `pack/aanleveren/`:

| Bestand | Eisen |
|---|---|
| `clown.jpg`, `clown.jpeg` of `clown.png` | De foto van Clown. **Elk formaat en elke verhouding**: vierkant, liggend of staand, zo groot als je wilt. |
| `clown_lach.ogg` | Het lachje, ogg vorbis (mono is het mooist). Heb je een mp3 of wav, zet die dan eerst om, bijvoorbeeld met Audacity. |

**Bouwen:** `java pack/BouwPack.java`. Alleen de JDK 25 is nodig, die je voor de mod toch al hebt,
en het werkt ook op Windows zonder Git Bash. Het programma:

1. leest de foto (jpg of png) en schaalt hem naar 476 pixels hoog, met behoud van de verhouding
   (een heel brede foto wordt kleiner, tot 1428 pixels breed);
2. zet het lachje erbij;
3. tekent **het quiz-rad** (Java2D, niets aan te leveren): 64 standen van 484 × 484, elk 5,625°
   verder gedraaid, met de 16 vakken in de vaste volgorde uit *Ronde 4* in alleen de
   teamkleuren (rood `#E24B4A`, blauw `#378ADD`, groen `#639922`, geel `#EF9F27`), een donkere
   rand en naad tussen de vakken, een dop in het midden, en het pijltje vast bovenin;
4. knipt de foto en elke stand van het rad in **tegels** en schrijft de fonts (zie hieronder);
5. zipt het pack naar `bootcamp-pack.zip` en print de SHA-1 voor `server.properties`.

**Waarom tegels.** Minecraft 26.2 zet font-glyphs op vellen van 256 × 256 pixels; een glyph die
groter is, wordt zonder foutmelding een leeg vierkantje. Daarom is elk plaatje geknipt in twee
rijen tegels van hooguit 242 pixels, elk een eigen glyph. Twee rijen, omdat een glyph niet hoger
boven de basislijn mag staan dan hij hoog is (`ascent` ≤ `height`): de bovenste rij hangt boven de
basislijn, de onderste eronder, dus het plaatje staat in het midden van het scherm. De mod zet de
tegels weer aan elkaar met één tekst (`core`: `FontTegels`): na elke tegel een spatie van -1
(U+F801, want een bitmap-glyph schuift één eenheid meer op dan hij breed is), na de bovenste rij
een spatie terug over de hele breedte (U+F802). Elke tegel is een heel aantal font-eenheden breed
(7 pixels per eenheid voor de foto, 11 voor het rad), anders ontstaat er een naad.
- Jumpscare: tegel (rij r, kolom c) is U+E000 + 16r + c, tot zes kolommen; 34 eenheden per rij,
  dus 68 hoog. Kolommen die een smallere foto niet nodig heeft zijn spaties van +1.
- Quiz-rad: stand s is 2 × 2 tegels, U+E100 + 4s + 2r + c; 22 eenheden per rij, dus 44 hoog.
- `PackFontTest` in het fabric-project haalt elke provider door de echte font-codec van 26.2 en
  controleert de maten van de tegels.

Online zetten als bijlage van een GitHub-release (de repo is public), en die URL in
`resource-pack=`. Na een nieuwe foto: opnieuw bouwen, opnieuw uploaden, nieuwe SHA-1 invullen,
server herstarten.

```
pack/
  aanleveren/clown.jpg                        wat je aanlevert (jpg of png, elk formaat)
  aanleveren/clown_lach.ogg
  BouwPack.java                               bouwt het pack en de zip
  pack.mcmeta
  assets/bootcamp/font/schrik.json            per tegel een bitmap-provider, plus de spaties
  assets/bootcamp/textures/font/clown_R_C.png de tegels van de foto (rij R, kolom C), gemaakt door BouwPack
  assets/bootcamp/font/rad.json               per tegel een bitmap-provider (64 standen × 4), plus de spaties
  assets/bootcamp/textures/font/rad_SS_R_C.png de tegels van stand SS van het quiz-rad
  assets/bootcamp/sounds.json                 bootcamp:clown_lach → sounds/clown_lach.ogg
  assets/bootcamp/sounds/clown_lach.ogg       gekopieerd uit aanleveren/
```

De foto en het lachje levert Pudding aan, wanneer het uitkomt. Tot de foto er is tekent BouwPack
een placeholder (een clownsgezicht met "foto volgt"); zonder lachje is de jumpscare stil, want
een stil ogg-bestand kan BouwPack niet maken. Wat BouwPack maakt (`assets/bootcamp/textures/`, de
zip) staat niet in git; de fonts en `pack.mcmeta` wel. Een foto die niet vierkant is, blijft in de jumpscare in zijn eigen verhouding:
de breedte volgt de hoogte.

**De jumpscare** (`Schrik.op(speler)`): een title met de tegels van de foto in font
`bootcamp:schrik`, fade-in 0, blijven 30 ticks, fade-out 10, plus `bootcamp:clown_lach` op volle
sterkte, alleen voor die speler. Hoe groot hij op het scherm staat, bepalen `SCHRIK_EENHEDEN` en
`SCHRIK_PX_PER_EENHEID` bovenin `BouwPack.java` (nu 68 eenheden hoog; een title tekent vier keer
zo groot); afstemmen in de eerste test. Heeft een speler het pack niet (weigerde of
downloadfout), dan ziet die lege vierkantjes en hoort niks; met `require-resource-pack=true` kan
dat niet. `/bc schrik <speler>` doet een jumpscare met de hand,
voor het testen en voor de lol.

Waar een jumpscare vandaan komt:
- Doolhof: in een regio `schrik_1` t/m `schrik_n`, één keer per regio per speler.
- Doolhof: een valkist (trapped chest) openen, één keer per kist per speler; de helft van de keren
  (de andere helft komen er mobs).
- Het Ei: een emerald block, bij een willekeurige andere levende deelnemer; iedereen ziet groot
  wie naar wie.

## Modules

| Package | Doet | Belangrijkste API |
|---|---|---|
| `config` | Regio's, punten, teamkeuzes, grapjes opslaan en laden, JSON in `<wereld>/bootcamp.json`. | Gson, `ServerLifecycleEvents` |
| `kits` | Kits uit JSON-bestanden lezen en op spelers zetten; de loot-tabel van het doolhof. | `ItemParser` (dezelfde syntax als `/give`) |
| `commands` | `/bc` en de commando's per ronde (`/doolhof`, `/ei`, `/mobarena`, `/quiz`, `/clown`, `/ffa`). | Brigadier, `CommandRegistrationCallback` |
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
| `/doolhof start\|stop\|resterend` | Ronde 1. |
| `/doolhof einde` | Noodknop: het doolhof nu afsluiten zoals na het gif (kleinste team voor wie er nog geen heeft, iedereen naar `v2`). Bijvoorbeeld als iemand in de finishruimte blijft staan zonder te kiezen. |
| `/doolhof timer [<minuten>]` | Hoe lang het doolhof duurt, standaard 15. |
| `/doolhof poort [<minuten>]` | Na hoeveel minuten de poort van de uitgang opengaat, standaard 4. |
| `/doolhof hint [<minuten>]` | Na hoeveel minuten de hint komt, standaard 10. |
| `/doolhof hinttekst [<tekst>]` | Wat er in de hint staat (subtitle onder `HINT`), bijvoorbeeld `De echte gang begint bij de lantaarn`. Bewaard in `bootcamp.json`. Zonder tekst: de huidige laten zien. Is er nooit een tekst gezet, dan rekent de mod een windrichting uit: `De uitgang ligt aan de noordkant`. `/doolhof hinttekst -` wist hem weer. |
| `/doolhof poort open\|dicht` | De poort met de hand bedienen. |
| `/doolhof startpoort open\|dicht` | De startpoort (`poort_start`) met de hand bedienen, om te testen. Open gaat stil, zoals bij de start van de timer. Dicht zet terug wat er stond. |
| `/doolhof poortmelding [aan\|uit]` | Of iedereen de raid-hoorn hoort en de title `DE UITGANG IS OPEN` ziet als de uitgang opengaat, standaard `aan`. Met `uit` gaat de poort stil open (de wolkjes in de poort komen er wel). Geldt ook voor `/doolhof poort open`. Zonder argument: de huidige stand. Bewaard in `bootcamp.json`. |
| `/doolhof valmobs [<min> [<max>]]` | Hoeveel mobs (husks en silverfish door elkaar) er uit een valkist komen: elke keer willekeurig van `<min>` t/m `<max>`, standaard 3 t/m 10. Met één getal altijd zoveel; `0` is alleen de jumpscare. Bewaard in `bootcamp.json`. |
| `/ei start\|stop\|resterend` | Ronde 2. |
| `/ei timer [<minuten>]` | Hoe lang het Ei duurt, standaard 15. |
| `/ei blokken [<soort> <aantal>]` | Hoeveel blokken van een soort (`netherite`, `diamond`, `gold`, `redstone`, `emerald`, `tnt`, `glowstone`, `slime`, `target`) de mod in het Ei strooit. Zonder argumenten: het overzicht, met het aantal deepslate-plekken in het Ei. |
| `/ei vastleggen` | Legt het Ei vast zoals het nu gebouwd is: alle blokken in regio `ei`. Eén keer na het bouwen, en opnieuw na elke bouwwijziging. Niet tijdens ronde 2. Antwoord: `Ei vastgelegd: 54.000 blokken, waarvan 27.812 deepslate.` |
| `/mobarena start` | Ronde 3: loot het schema (geheim, niet in de chat) en start beurt 1. |
| `/mobarena volgende` | Start de volgende beurt. Weigert zolang de huidige beurt nog loopt, ook tijdens de 10 seconden na de beurt. |
| `/mobarena schema` | Het schema, alleen voor wie het typt (de spelers zien het niet): per beurt wie in welke arena staat, wie af is. |
| `/mobarena startplek <1\|2> <rood\|blauw\|groen\|geel>` | Zet de startplek van dat team in die arena op de plek waar je staat, met je kijkrichting. Ga midden op het gekleurde vlak staan. Hetzelfde als `/bc point set start_<arena>_<kleur>`. |
| `/mobarena stop` | Breekt ronde 3 af. |
| `/mobarena wave volgende` | De huidige wave telt als klaar in beide arena's (overgebleven mobs weg). |
| `/mobarena punten [<mob> <punten>]` | Hoeveel punten een mobtype waard is. Zonder argumenten: de tabel. |
| `/mobarena aftekst [<tekst>]` | De tekst die in de actionbar staat bij wie in de mob arena sneuvelt, standaard `Af · je speelt geen beurt meer`. Zonder tekst: de huidige laten zien. Bewaard in `bootcamp.json`. |
| `/mobarena veldhoogte [<blokken>]` | Tot hoeveel blokken boven de selectie een veld telt voor kijkers en wachtenden, standaard 3. Op regio `tribune_mob` telt het veld nooit, dus meestal hoef je hier niets aan te doen. Bij `0` telt alleen wie binnen de selectie zelf staat. Geldt meteen; bewaard in `bootcamp.json`. |
| `/quiz start\|stop` | Ronde 4. Weigert zonder presentator. |
| `/quiz presentator [<speler>]` | Wie presenteert (Pudding). Op naam, mag ook voor iemand die nog niet online is; bewaard in `bootcamp.json`. Die gaat bij de start naar het podium in plaats van naar zijn bank en krijgt de drie quiz-items. |
| `/quiz bank <rood\|blauw\|groen\|geel>` / `/quiz podium` | Zet de bank van dat team, of het podium, op de plek waar je staat, met je kijkrichting. Hetzelfde als `/bc point set quiz_<kleur>` en `quiz_podium`. |
| `/quiz lamp <rood\|blauw\|groen\|geel>` | Zet de lamp bij de bank van dat team: het blok waar je naar kijkt (tot 32 blokken). Hetzelfde als `/bc point block quizlamp_<kleur>`. |
| `/quiz draai` | Het rad draaien. Hetzelfde als het rad-item van de presentator. |
| `/quiz goed` / `/quiz fout` | Het antwoord van het team dat aan de beurt is goedkeuren (+1 punt) of afkeuren. Hetzelfde als de groene en rode wol. |
| `/quiz punt <kleur> [<aantal>]` | Punten erbij (standaard 1, negatief mag): om een verkeerde klik recht te zetten. |
| `/quiz einde` | Het team met de meeste punten wint: titles en vuurwerk. Bij gelijke stand weigert hij en noemt de teams die gelijk staan. |
| `/quiz winnaar <kleur>` | Een winnaar aanwijzen, voor een gelijke stand. |
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
| `/ffa start\|stop` | Ronde 6, met de kroning aan het eind. `start` zet iedereen bevroren klaar. Geen timer. |
| `/ffa go` | Start de countdown van 10 seconden; daarna is iedereen los. |
| `/ffa krimp <grootte> [<seconden>]` | De border laten krimpen als het stilvalt. Standaard in 60 seconden. Zonder dit commando blijft de hele vloer vrij. Iedereen ziet `DE BORDER KRIMPT`. |
| `/ffa wachttekst [<tekst>]` | De tekst in de actionbar terwijl iedereen wacht op `/ffa go`, standaard `Wacht op het startsein`. Zonder tekst: de huidige laten zien. Bewaard in `bootcamp.json`. |

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
| `/bc schrik <speler>` | Een jumpscare, met de hand. |
| `/bc kijker <speler> aan\|uit` | Noodknop: iemand met de hand op de tribune zetten of eraf halen. |
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
| `/doolhof valmobs` | 3 t/m 10 | 0 tot 20, min niet boven max |
| `/ei timer` | 15 | 5 tot 60 |
| `/ei blokken` | netherite 6, diamond 90, gold 120, redstone 10, emerald 10, tnt 10, glowstone 10, slime 10, target 5 (voorlopig; samen 271) | 0 of meer; samen niet meer dan de deepslate-plekken in het Ei |
| `/mobarena punten` | zombie 1; skeleton, spider, cave spider 2; creeper 3; witch 4; vindicator 5; evoker 8; ravager 10; elk ander type 1 | 0 tot 100 |
| `/mobarena aftekst` | `Af · je speelt geen beurt meer` | tot 60 tekens |
| `/mobarena veldhoogte` | 3 blokken | 0 tot 10 |
| `/clown wachttekst` | `Wacht op het startsein` | tot 60 tekens |
| `/ffa wachttekst` | `Wacht op het startsein` | tot 60 tekens |

Buiten de grenzen weigert het commando met één regel (`de hint (16 min) valt na het einde
(15 min)`). Loopt de ronde al, dan geldt een nieuwe waarde meteen. Een poort of hint die al
geweest is, blijft geweest; ligt het nieuwe moment al achter je, dan gebeurt het nu. Een timer
korter dan wat er al gespeeld is, weigert hij.

## Regio's en punten

Eén keer zetten na het bouwen. Alles wordt opgeslagen in `<wereld>/bootcamp.json`. Geen
coördinaten in code.

**Regio's** met de wand: linksklik op een blok is hoek 1, rechtsklik hoek 2, dan
`/bc region save <naam>`. **Voor spelers is een regio een kolom**: alleen x en z tellen. Een
poort gebruikt wel de hele doos, net als de kistenscan van het doolhof en het Ei (`ei`). `veld_1`
en `veld_2` tellen als rechthoek. Startpunten horen binnen de border-regio van hun ronde te
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
| `doolhof`, `eigebied`, `mobarena`, `quiz`, `vloer` | Worldborder per ronde. `quiz` is de quizhal. `eigebied` omvat het Ei, de kettingen en hun startplekken, `mobarena` beide arena's met de tribune. `vloer` is de vloer van de Arena: een cilinder van 5 hoog, gezet met `/clown vloer <diameter>`; kijkers die erin komen gaan terug naar de tribune. |
| `ei` | Het Ei zelf, als doos: onderhoek en bovenhoek. Alleen hierbinnen kun je in ronde 2 breken; `/ei vastleggen` legt deze doos vast en de mod strooit de puntenblokken op de gewone deepslate erin. Kettingen (iron en copper) blijven ook binnen de doos heel: daar klim je over. |
| `colosseum` | *Optioneel.* De hele Arena inclusief tribunes: border van ronde 5 en 6. Anders `vloer`. |
| `doolhof_uit` | De finishlijn in de afgesloten ruimte achter de echte uitgang: wie erop staat en nog geen team heeft krijgt het teammenu. Een smalle lijn van één blok mag (de mod kijkt elke tick). Moet binnen regio `doolhof` liggen, anders weigert `/doolhof start`. |
| `nep_1` t/m `nep_3` | De vakken aan het eind van de nep-gangen. |
| `schrik_1` t/m `schrik_n` | Schrikplekken in het doolhof. Zoveel als je wilt, genummerd vanaf 1. |
| `poort_doolhof` | De poort voor de echte uitgang, opent na `/doolhof poort` minuten. |
| `poort_start` | De openingen van de startruimte, elk een deel (`save` + `add`). Zet er zelf barrier blocks in (of glas): tijdens de countdown kan niemand de startruimte uit. Bij de start van de timer haalt de mod ze stil weg (geen hoorn); bij de volgende `/doolhof start` zet hij ze terug, vóór iedereen de startruimte in gaat. Weet de mod na een herstart niet meer wat er stond, dan barrier. |
| `doolhof_gif` | Het doolhof zelf, zonder de finishruimte (in delen als dat moet: `save` + `add`). Na de timer is het hier giftig. Punt `doolhof_finish` mag er niet in liggen, anders weigert `/doolhof start`. |
| `veld_1`, `veld_2` | De twee mob-arenavelden, elk uit meerdere delen (de T-vorm: `save` voor de balk, `add` voor de poot). Een kijker die erin komt wordt teruggezet, behalve wie in de kooi van dat veld zit (de tralies houden die binnen); een speler die aan de beurt is en eruit komt ook. Voor kijkers telt een veld tot drie blokken boven de selectie (`/mobarena veldhoogte`), zodat het balkon erboven geen veld is; voor wie aan de beurt is telt alleen de kolom. |
| `tribune_mob` | De tribune van de mob arena: selecteer de vloer waar de kijkers op staan, in delen als dat moet (`save` + `add`). Daarop telt niemand als in het veld, ook waar de selectie over een veld hangt of ermee overlapt: tot drie blokken boven de selectie (springen telt mee), niet eronder. Wie aan de beurt is en de tribune op loopt, gaat terug naar zijn startplek. `/mobarena start` weigert als een `tribune_mob_n` er niet op ligt of een startplek er wel op ligt. |

**Punten** met `/bc point set <naam>` of `/bc point block <naam>`.

| Punten | Waarvoor |
|---|---|
| `basiskamp` | Spawn en reset. |
| `doolhof_start` | De startruimte in het midden van het doolhof; ook waar een nep-uitgang je neerzet. |
| `doolhof_finish` | In de finishruimte: waar wie tijdens het gif doodgaat neerkomt. Binnen regio `doolhof`, niet in `doolhof_gif`. |
| `v2` | Verzamelpunt bij het Ei: als het doolhof voorbij is. |
| `ei_spawn_1` t/m `ei_spawn_n` | De startplekken aan het buiteneinde van de kettingen. Zoveel als je wilt, genummerd vanaf 1. Spelers worden er om en om over verdeeld; na een dodelijke klap kom je terug op je eigen startplek. |
| `v3` | Verzamelpunt bij de mob arena. |
| `start_1_rood`, `start_1_blauw`, `start_1_groen`, `start_1_geel`, en hetzelfde met `start_2_` | De vier gekleurde startplekken in arena 1 en in arena 2: de speler van een team start altijd op het vlak in zijn eigen kleur. Zetten met `/mobarena startplek <arena> <kleur>`. |
| `mob_1_1` t/m `mob_1_n`, `mob_2_1` t/m `mob_2_n` | Mob-spawns per arena, op zelf gekozen plekken. Zoveel als je wilt per arena; arena 1 en 2 mogen iets verschillen. De mobs van een wave gaan om en om over de spawns van hun arena. |
| `kooi_1`, `kooi_2` | De kooi in het midden van elke arena. |
| `tribune_mob_1` t/m `tribune_mob_n` | De tribune (het balkon). Zoveel als je wilt, allemaal op regio `tribune_mob`. |
| `quiz_rood`, `quiz_blauw`, `quiz_groen`, `quiz_geel` | De vier banken in de quizhal. Zetten met `/quiz bank <kleur>`. |
| `quiz_podium` | Het podium boven aan de trap, waar de presentator staat. Zetten met `/quiz podium`. |
| `quizlamp_rood`, `quizlamp_blauw`, `quizlamp_groen`, `quizlamp_geel` (blokken) | De lamp bij elke bank. Gedoofde redstone lamp, geen redstone ernaast. Zetten met `/quiz lamp <kleur>`. |
| `troon` | Het kleine podium in het midden van de Arena, waar de kroonhouder spawnt. Zetten met `/clown troon`. |
| `jager_1` t/m `jager_n` | De startplekken van de jagers: in de Arena de 20 redstone blocks in een cirkel. Zetten met `/clown jagerplek`, bovenop het blok en kijkend waar de speler heen moet kijken; de kijkrichting gaat mee met de teleport. |
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
| `jager.json` | Iedereen behalve Clown bij de start van ronde 5: volledig diamond armor (Protection IV, Unbreaking III), diamond sword (Sharpness V, Unbreaking III), diamond axe (Sharpness V, Unbreaking III), bow (Power V, Unbreaking III), 32 pijlen, schild (Unbreaking III), 16 golden apples. | true |
| `boss.json` | Clown bij de start van ronde 5: precies de jagerskit, maar zonder helm (de kroon zit al op zijn hoofd). De twee bestanden zijn standaard gelijk; ze mogen later uit elkaar lopen. | true |
| `kroonpakket.json` | Bij elke kroonwissel erbij: 2 gapples, 2 pearls. | false |

**De kroon** is een diamond helm met dezelfde enchants als de kit (Protection IV, Unbreaking III)
plus Curse of Binding en de naam `Kroon` in goud, herkenbaar aan
`custom_data={bootcamp_kroon:1b}`. De kroonhouder is dus niet zwakker dan de jagers; dat hij de
kroon heeft zie je aan Glowing en de zwevende gouden kroon boven zijn hoofd.
| `arena.json` | Start ronde 6, iedereen: dezelfde kit als `jager.json` (met gewone diamond helm), maar met 32 golden apples in plaats van 16. | true |

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
minder, voor een test), per wave een lijst mobs met
type, aantal en eventueel gear. Het aantal geldt per arena (voor 4 spelers) en schaalt niet mee:
een lege startplek maakt de wave niet kleiner, zodat beide arena's altijd precies dezelfde mobs
krijgen. De mobs gaan om en om over de spawns van hun arena (`mob_1_n` of `mob_2_n`).

## Spellogica

**Status.** Eén `GameState`: huidige ronde, timer, vlaggen, en per speler een rol (`SPELER`,
`JAGER`, `KROON`, `FFA`, `KIJKER`, `STAFF`), een team, en vlaggen (`dood`, `klaar`, `uitverkoren`,
slot, punten, schrikplekken gehad). De mod is de bron van waarheid; scoreboard-tags zijn read-only
spiegels die elke seconde worden bijgezet. `/bc status` toont alles.

**Ontbrekende config.** Elke ronde declareert welke regio's, punten en kits ze nodig heeft.
`/<ronde> start` weigert met één regel ("ontbreekt: kooi_2, start_1_geel, ...") en verandert
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
| 3 | Kijker in de kooi van zijn arena (`kooi_1` of `kooi_2`), na de beurt naar de tribune. Af voor de rest van de mob arena: een latere beurt op het schema blijft leeg. Punten blijven staan. Inventory leeg: wie af is speelt niet meer, en na ronde 3 levert iedereen toch alles in. Doodtekst. Zijn alle spelers in een arena af, dan is die arena klaar. |
| 4 | Kan niet: geen schade. |
| 5, kroonhouder | Kroonwissel naar de killer, anders de laatste hit, anders een willekeurige levende jager. Ex-kroonhouder wordt kijker op de tribune. |
| 5, jager | Kijker op de tribune, af. Is er nog maar één speler over, dan is die de winnaar. |
| 6 | Kijker op de tribune; laatste over is King of the SMP Bootcamp. |

Geen death-screen, geen respawn. De laatste hit komt uit `ALLOW_DAMAGE`.

**Ronde 1, doolhof.** Start: poort en startpoort (`poort_start`) dicht, iedereen naar
`doolhof_start`, basiskit, kisten vullen, border `doolhof`. Countdown; dan gaat de startpoort stil
open en loopt de timer (`/doolhof timer`, standaard 15 minuten). Na
`/doolhof poort` minuten (standaard 4) gaat `poort_doolhof` open, met horn en title
`DE UITGANG IS OPEN` zolang `/doolhof poortmelding` aan staat. De bossbar laat alleen de totale tijd
zien, zodat niemand weet wanneer de uitgang opengaat. Elke 5 ticks: wie in `nep_n` staat krijgt
explosie-particles (`explosion_emitter`), `entity.creeper.primed` plus `entity.generic.explode`, een
willekeurig grapje als title (lijst in `bootcamp.json`) en gaat naar `doolhof_start`; wie in
`schrik_n` staat en die nog niet had krijgt de jumpscare; wie op de finishlijn (`doolhof_uit`, elke
tick gecontroleerd) staat zonder team krijgt het teammenu. Na `/doolhof hint` minuten (standaard 10)
voor wie nog binnen is de title `HINT` met als subtitle `/doolhof hinttekst` (of de windrichting).
**Finish**: achter de echte uitgang ligt een afgesloten ruimte met de finishlijn. Wie een kleur
kiest krijgt de title `GEFINISHT` met `Je zit in Rood · je mag terug het doolhof in`, een chatregel
voor iedereen (`Speler7 zit in Rood (3/5)`) en blijft waar hij is. Wie gefinisht is doet gewoon mee
als hij terug naar binnen loopt, om anderen te helpen of meer loot te zoeken: loot, mobs, valkisten,
schrikplekken en nep-uitgangen werken ook voor hem, en doodgaan zet hem terug in de startruimte.
Zijn team houdt hij. Logt hij uit en weer in, dan gaat hij verder waar hij was. **Valkisten**: een
trapped chest in regio `doolhof` heeft geen loot en gaat niet open. Wie hem opent krijgt 50/50
(`core`: `Regels.valkistGok`) óf de jumpscare, óf om zich heen (binnen twee blokken, op een vrije
plek met grond eronder) willekeurig `/doolhof valmobs` mobs (standaard 3 t/m 10), husks en
silverfish door elkaar, elk op een eigen plek als dat past, met een wolkje en
`entity.evoker.prepare_summon`. Staat `/doolhof valmobs` op 0, dan altijd de jumpscare. De mobs
hebben de opener als doel en doen gewone schade; wie doodgaat, gaat terug naar `doolhof_start`. Per
speler gaat een kist één keer af (een dubbele kist is één kist), daarna gaat hij voor die speler
open als lege kist. Staff zet niets af. De mobs verdwijnen aan het einde van het doolhof en komen na
een crash niet terug.

**Timer op: het gif.** Heeft nog niet iedereen een team, dan is het doolhof niet meteen voorbij.
Title voor iedereen `DE TIJD IS OM` met `Het doolhof is giftig · ga naar de finish`,
`entity.elder_guardian.curse`, bossbar `Doolhof · de tijd is om · gif` in paars. Wie in regio
`doolhof_gif` staat (het doolhof zelf, niet de finishruimte) krijgt elke seconde kort Poison I
(weg zodra je eruit loopt) en om de 2 seconden een klap van 1 hart die door armor heen gaat
(`core`: `Regels.DOOLHOF_GIF_ELKE`, `DOOLHOF_GIF_SCHADE`). Poison alleen doodt niet, de klap wel.
Dat geldt voor iedereen, ook wie al gefinisht is en terug naar binnen liep. **Wie tijdens het gif
doodgaat** (door het gif of anders) raakt al zijn spullen kwijt behalve het eten uit de basiskit
(`basis.json`), krijgt de rest van de basiskit terug, gaat naar punt `doolhof_finish` in de
finishruimte met de title `VERGIFTIGD` en een chatregel `Speler7 bezweek aan het gif`. Heeft hij
nog geen team, dan gaat daar meteen het teammenu open; hij telt dan niet mee als iemand die de
uitgang vond. Het gif loopt tot iedereen een team heeft; `/doolhof einde` is de noodknop.

**Einde** (iedereen een team, of `/doolhof einde`): wie nog geen team heeft gaat naar het kleinste
team (bij gelijk: willekeurig) en ziet in de actionbar `Je zit in Groen`; iedereen ziet de title
`DOOLHOF VOORBIJ` met als subtitle `14 van de 20 vonden de uitgang` (wie zelf over de finish kwam),
en iedereen naar `v2`, ook wie gefinisht nog in het doolhof loopt. `/doolhof start` begint zonder
teams: het doolhof is de teamkeuze, dus oude keuzes gaan weg.

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
   `ei.json`, Night Vision zonder deeltjes zolang het Ei duurt (binnenin wordt het snel donker; ook
   na een val en voor wie inlogt, eraf aan het einde), border `eigebied`, countdown, timer (`/ei timer`, standaard 15 minuten).

`/ei vastleggen` bewaart alle blokken van de doos `ei` in de wereldmap (`bootcamp_ei.nbt`). Een
doos van meer dan 250.000 blokken weigert hij: dat is een verkeerde selectie.

**Breken** (`PlayerBlockBreakEvents.BEFORE`): buiten regio `ei` geannuleerd. Binnen `ei` breekt
elk blok zonder drop (`level.removeBlock`, event geannuleerd). De gestrooide blokken geven punten
of een effect, alleen op de plekken waar de mod ze strooide: glowstone of slime als versiering in
de schil doet niets. **Alles wat iemand vindt staat in de chat**, met de naam in de teamkleur
(`Speler7 hakte diamond (+10)`, `Speler7 hakte redstone: iedereen bevroren`, `Speler7 liet
Speler3 schrikken`).

- netherite +50, diamond +10, gold +5: `entity.experience_orb.pickup`, en twee seconden lang
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

Sidebar: titel `Het Ei · top 10`, per regel de naam in de teamkleur met de punten rechts,
bijgewerkt bij elk puntenblok. De volgorde is die van de mod, ook bij gelijke punten: wie de
score het eerst had staat hoger. De client sorteert gelijke scores zelf op de naam van de regel,
dus de regels heten intern `01` t/m `10` en krijgen de spelersnaam als display name. Wie nog 0
punten heeft staat er niet in. Geen zoekhints: het Ei is van ver te zien. Timer op: de meeste
punten wint (gelijk: wie die score het eerst had), title en vuurpijl, iedereen naar `v3`,
adventure. **De pickaxe gaat weg**: elk item met `custom_data={bootcamp_ei:1b}` verdwijnt uit
inventory, offhand en cursor, ook bij `/ei stop` en `/bc reset`. Wie tijdens het Ei uitlogde en
later terugkomt, raakt hem bij het inloggen kwijt. Het Ei blijft uitgehakt liggen tot de
volgende `/ei start`; ook `/bc reset` zet het terug (zonder puntenblokken).

**Ronde 3, mob arena.** Geen kit: iedereen speelt met wat hij heeft. Start bij `v3`: border
`mobarena`, iedereen naar `tribune_mob_n`, dan het **schema** (`core`: `MobSchema`):

- Aantal beurten = het grootste team (5 bij 20 spelers). Per team een willekeurige volgorde van
  de spelers; met *n* beurten speelt in beurt *b* in arena 1 speler *b* en in arena 2 speler
  *b + ⌊n/2⌋* (rondom), dus bij 5 beurten speler *b + 2*.
  Zo speelt iedereen één keer in elke arena en niemand twee beurten achter elkaar (waar het
  aantal spelers dat toelaat).
- Een kleiner team heeft in sommige beurten geen speler: dat zijn **extra beurten**. De mod vult
  ze pas bij de start van die beurt, met een willekeurige speler van dat team die nog niet af is
  en niet al in de andere arena staat. Is er niemand, dan blijft de plek leeg.
- Het schema is **geheim**: het komt niet in de chat. Spelers merken pas dat ze aan de beurt zijn
  als de mod ze bij de start van een beurt naar hun vlak teleporteert. Alleen staff kan het
  opvragen met `/mobarena schema`; dat antwoord ziet alleen wie het typt.

**Een beurt** (start met `/mobarena start` voor beurt 1 en `/mobarena volgende` voor de rest):
de ingeplande spelers die nog niet af zijn gaan naar `start_<arena>_<kleur>`, geheald en honger
vol; de rest blijft op de tribune. Wie op zijn beurt wacht, houdt daar zijn spullen, krijgt geen
schade en telt voor de mobs als kijker; alleen wie af is wordt echt kijker (inventory leeg).
Title `BEURT 3` voor iedereen (geen subtitle). De spelers die aan de beurt zijn krijgen
**Glowing**, dat in hun teamkleur gloeit (de outline volgt de kleur van hun team), tot het einde
van de beurt of tot ze af zijn. Countdown 5. Dan 5 waves uit `waves.json`, in beide arena's
tegelijk, met tag `bootcamp_mob` en `arena_1` of `arena_2`, `setPersistenceRequired()`, stenen
knoop op het hoofd tegen zonlicht. Bij elke wave eerst een wolk `large_smoke`-particles en
`block.fire.extinguish` op elk spawnpunt, dan de mobs. Een arena is klaar met een wave als haar teller 0 is; zijn
beide klaar, dan na 5 seconden de volgende wave. Na 120 seconden telt een wave altijd als klaar
(overgebleven mobs weg). Een arena is klaar met de beurt na wave 5, of zodra al haar spelers af
zijn (haar mobs weg); de beurt is klaar als beide arena's klaar zijn. Dan meteen mobs en vexes
weg en de title `BEURT 3 KLAAR` met de stand (fade-in 0, 200 ticks blijven), plus
voor wie in een arena of kooi staat een aftelling van 10 seconden in de actionbar
(`Naar de tribune over 7`). **Pas na 10 seconden** gaan de spelers uit de arena's en de kijkers
uit de kooien naar `tribune_mob_n` (om en om over de punten). Wie al op de tribune stond, wordt
niet verplaatst. In die 10 seconden doet niemand schade en is er niets meer om te killen. Daarna
wacht de mod op `/mobarena volgende`.

**Punten**: `ServerLivingEntityEvents.AFTER_DEATH` op een mob met `bootcamp_mob`. De killer is
de speler die de laatste klap gaf, ook via een pijl of andere projectile; vanilla
(`getLastHurtByMob`/`lastHurtByPlayer`) telt een speler die de mob in de laatste 5 seconden
raakte. Punten uit `/mobarena punten` naar het team van die speler, plus één kill voor de
tiebreak. Zonder speler (magma, val) geen punten. Een kill op een **evoker of ravager** komt als
chatregel voor iedereen: `Speler7 killde de ravager (+10)`, met de naam in de teamkleur. Andere
kills alleen in de actionbar van de killer.

**Af**: wie in de mob arena sneuvelt, ziet eerst de doodtekst als title en daarna in de actionbar
de tekst van `/mobarena aftekst` (standaard `Af · je speelt geen beurt meer`), vijf seconden lang.

**Mobs laten kijkers met rust**: elke tick krijgt een `bootcamp_mob` die een kijker, een staff-lid
of een speler buiten zijn eigen arena als doel heeft `setTarget(null)`; daarna kiest hij zelf de
dichtstbijzijnde speler die aan de beurt is in zijn arena. Dat kijkers geen schade krijgen is
niet genoeg: zonder deze check kiezen mobs ze nog steeds als doel en blijven ze om de kooi hangen.
Wie op zijn beurt wacht, kan vanaf de tribune ook geen mob raken: een klap of pijl van een speler die
niet in een arena staat, doet een `bootcamp_mob` niks, en een kill telt alleen voor wie aan de
beurt is.

**Einde**, na de laatste beurt en haar 10 seconden: het team met de meeste punten wint (gelijk:
meeste kills, dan samen), titles en vuurpijlen boven de tribune. Weer 10 seconden om te vieren,
dan **levert iedereen alles in**: inventory, armor en offhand leeg (`clearContent`), ook voor wie
op de tribune stond. Iedereen weer speler, geheald, zonder spullen naar de quiz.

**Ronde 4, quiz.** Weigert zonder presentator (`/quiz presentator`). Start: iedereen naar
`quiz_<kleur>` van zijn team, border `quiz`, geen schade, geen timer. Sidebar `Quiz` met de vier
teams op 0. Iedereen heeft een lege inventory (ingeleverd na de mob arena). De presentator gaat
naar `quiz_podium` en telt in de quiz niet mee voor zijn team; hij krijgt als enige iets: drie
items in hotbar-slot 1 t/m 3, herkenbaar aan `custom_data={bootcamp_quiz:"..."}`:

| Slot | Item | Rechtsklik doet |
|---|---|---|
| 1 | Groene wol, naam `Goed` | hetzelfde als `/quiz goed` |
| 2 | Rode wol, naam `Fout` | hetzelfde als `/quiz fout` |
| 3 | Nether star, naam `Draai het rad` | hetzelfde als `/quiz draai` |

Rechtsklik wordt afgevangen in `UseItemCallback` én `UseBlockCallback`, zodat de wol nooit als
blok wordt neergezet. De items zijn niet te droppen of te verplaatsen naar een kist; raakt de
presentator er toch een kwijt, dan legt de mod hem terug.

**Het quiz-rad** is een **echt rond rad in beeld**: een plaatje uit het resource pack (zie
*Resource pack*), groot in het midden van het scherm, ongeveer tweederde van de schermhoogte,
met een vast pijltje bovenin. 16 vakken in alleen de teamkleuren, elk team 4 keer, in een vaste
volgorde waarin twee buren (ook rondom) nooit dezelfde kleur hebben:
`rood blauw groen geel blauw rood geel groen rood groen blauw geel groen geel rood blauw`.

Het pack heeft 64 standen van het rad (elk 2 × 2 tegels, zie *Resource pack*), elk 5,625° verder
gedraaid: vier per vak, en elk vierde plaatje heeft een vak precies onder het pijltje. Het draaien is
`core`-`Rad` over die 64 standen: doel = de middenstand van een willekeurig vak (dus elk team
25%), `rest` = de standen tot het doel plus 64 × (2 of 3). Per stap een title met de tegels van
die stand, fade 0, lang genoeg blijven tot de volgende stap; de wachttijd loopt op van 1 naar 6
ticks naarmate `rest` kleiner wordt, dus het rad remt echt af. Elke keer dat er een vakgrens
onder het pijltje door gaat (om de vier standen): `note_block.hat` voor iedereen in de hal. Bij
de landing blijft het laatste plaatje twee seconden staan, dan `entity.player.levelup` en title
`ROOD IS AAN DE BEURT` in de teamkleur; bossbar `Quiz · aan de beurt: Rood`, en `quizlamp_rood`
gaat aan (`LIT=true`). Terwijl het rad draait, doen draai, goed en fout niets. De lampen doen
niet mee aan het draaien.

**Goed**: alleen als er een team aan de beurt is: +1 voor dat team in de sidebar, title `GOED!`
in groen met `+1 Rood`, `block.note_block.bell`. Het team blijft aan de beurt, de lamp blijft
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
winnaar aan. 10 seconden later: alle lampen uit, de presentator zijn items kwijt (weer een lege
inventory, zoals iedereen), en iedereen naar de tribune van de Arena (`tribune_n`, om en om) voor
het Rad. `/quiz stop` en `/bc reset` zetten de lampen ook uit.

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
de winnaar van het Ei, de winnaar van Clown vs All en de kroning. Controleren in de eerste test;
werkt het in 26.2 anders dan gedacht, dan valt de mod terug op alleen de naam.

**Ronde 5, Clown vs All.** Iedereen uit zijn teamkleur en van de tribune de vloer op. Clown naar
`troon` (het podium in het midden), eerst de kroon, dan `boss.json`, Glowing, team `kroon`. De
rest `jager.json`, team `jagers`, **willekeurig verdeeld over `jager_1..n`**, één per plek (zijn
er meer jagers dan plekken, dan delen ze om en om), met de kijkrichting van de plek. Iedereen
heeft dezelfde uitrusting; alleen de helm van Clown is de kroon. Dan **Opstelling zonder
countdown**: iedereen bevroren, ook Clown, tot de commander `/clown go` doet; bossbar
`Clown vs All · wacht op de start`, en in de actionbar van iedereen op de vloer, elke seconde,
de tekst van `/clown wachttekst` (standaard `Wacht op het startsein`). Border `colosseum` (of
`vloer`), geen timer, locator bar aan met alleen de kroonhouder zichtbaar. Elke seconde: is er
nog maar één levende deelnemer, dan einde.

**Actionbar van de kroonhouder**: zolang niemand stil hoeft te staan, elke seconde
`Jij hebt de kroon · 11 jagers`, alleen voor hem.

**Af**: wie in ronde 5 afvalt (jager of oude kroonhouder), geeft een chatregel voor iedereen:
`Speler3 is af door ClownPierce · 11 over`, of zonder killer `Speler3 is af · 11 over`, met de
namen in hun teamkleur (aqua, goud). De dode ziet daarnaast zijn doodtekst als title.

**Winnaar**: is de laatste die overblijft de uitverkorene, dan title `DE EINDBAAS WINT` met als
subtitle zijn kop en naam; anders title met kop en `SPELER7 WINT CLOWN VS ALL`. Vuurpijl erboven,
`ui.toast.challenge_complete`.

**Kroonwissel** (`Kroon.wissel(oude, nieuwe)`): oude wordt kijker op de tribune; nieuwe naar
`troon`, heal, honger vol, alles op volle durability, de kroon als helm (oude helm naar de
inventory), `kroonpakket.json`, Glowing, zweefkroon (geen Resistance); dan
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
levende deelnemer, dan is die King of the SMP Bootcamp. De border staat om `colosseum` (of het
vierkant om `vloer`) en krimpt alleen met `/ffa krimp <grootte> [<seconden>]`, rond het midden
van de vloer.

In beeld tijdens de FFA:

- **Sidebar `Kills`**: de spelers met de meeste kills, bijgewerkt bij elke kill (gelijk: wie het
  eerst op dat aantal kwam, hoger). Wie nog 0 kills heeft staat er niet in.
- **Af**: chatregel voor iedereen `Speler3 is af door Speler7 · 11 over`, of zonder killer
  `Speler3 is af · 11 over`. De dode ziet zijn doodtekst.
- **`/ffa krimp`**: title `DE BORDER KRIMPT` in rood met subtitle `naar 20 in 60 seconden`,
  `event.raid.horn`.
- **Laatste drie en laatste twee**: zodra er drie over zijn title `LAATSTE DRIE` met de drie
  namen, bij twee `LAATSTE TWEE` met `Speler7 tegen Speler2`, in paars, met
  `entity.wither.spawn` zacht.

**Kroning**: iedereen naar de tribune, de winnaar naar `troon` (het podium) met de kroon en de
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
  `veld_1` of `veld_2` komt terugzet op zijn tribunepunt of in zijn kooi (op regio `tribune_mob` telt
  geen veld). Bij `vloer` telt de
  cilinder: binnen de cirkel én binnen de 5 blokken hoogte. Een kijker die van de tribune de
  arena in springt of loopt, staat dus meteen weer op de tribune, met title `Terug naar de
  tribune`.
- Mobs laten kijkers met rust (zie ronde 3).
- Niet op de locator bar, geen Glowing.
- Bij de dood een title met een willekeurige doodtekst, alleen voor de dode zelf. De lijst staat
  in `bootcamp.json`.

Waar kijkers heen gaan: ronde 3 bij een dood naar `kooi_1` of `kooi_2` tot het einde van die
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
| 1 | `Doolhof · 04:41` | groen, de laatste minuut rood | tijd; de poort staat er niet in |
| 1, na de timer | `Doolhof · de tijd is om · gif` | paars | vol |
| 2 | `Het Ei · 07:12` | groen, de laatste minuut rood | tijd |
| 3 | `Mob Arena · beurt 3/5 · wave 2` | rood | mobs over in beide arena's |
| 4 | `Quiz · aan de beurt: Groen`, of `Quiz · draai het rad` | teamkleur, wit als niemand aan de beurt is | vol |
| 5 | `Clown vs All · Kroon: Clown · 12 over` | geel | spelers over |
| 6 | `FFA · 7 over` | paars | spelers over |
| Na de kroning | `Pudding Bootcamp · King: Speler7` | goud | vol |

**Sidebar**: ronde 1 de teams met aantallen en namen (`Rood 3/5` met de spelers eronder), ronde 2 de top 10 op punten, ronde 3
de teamstand in punten (`Rood 47`), ronde 4 de quizpunten, ronde 5 de regeerperiodes, ronde 6 de
kills.

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
| Valkist | 50/50: de jumpscare voor wie hem opent, of mobs met `poof`-particles en `entity.evoker.prepare_summon`. |
| Nep-uitgang | Explosie-particles, creeper-sis en knal, grapje als title, terug in de startruimte. |
| Hint doolhof | Title `HINT` in geel, subtitle de hinttekst, voor wie nog binnen is. |
| Doolhof voorbij | Title `DOOLHOF VOORBIJ`, subtitle `14 van de 20 vonden de uitgang`; wie in een team is gezet ziet in de actionbar `Je zit in Groen`. |
| Jumpscare | Foto van Clown schermvullend, `bootcamp:clown_lach`. |
| Team gekozen | `entity.player.levelup`, je naam in de teamkleur, chatregel voor iedereen: `<naam> zit in Rood (3/5)`. |
| Ei: punten | `entity.experience_orb.pickup`, actionbar met wat je erbij kreeg, je score en je plek. |
| Ei: netherite | `+50` in paars in de actionbar, chatregel voor iedereen: `Speler7 hakte netherite (+50)`. |
| Ei: redstone | Haste (15 seconden): `block.beacon.power_select`, title `HASTE` voor de hakker. Bevriezing: title `BEVROREN` met `door <naam>`, `block.glass.break`. Aftellen in de actionbar, pling als het voorbij is. |
| Ei: emerald | Iedereen ziet de title `Speler7 → Speler3` met `JUMPSCARE`; de ander krijgt eerst de jumpscare, dan die title en `Met dank aan Speler7`; de hakker ziet ook `Jumpscare naar Speler3`. |
| Ei: TNT | Title `TNT` voor de hakker; neergezet `entity.tnt.primed`, na 4 seconden de knal met `explosion_emitter`. |
| Ei: glowstone | Title `TURBO`, `block.beacon.activate`, `Efficiency V · 8` in de actionbar, pling als het voorbij is. |
| Ei: slime | Title `MISSELIJK` met `door <naam>` voor de anderen, `entity.slime.squish`. |
| Ei: target | Title `GEHUSSELD` met `door <naam>` voor iedereen, `entity.enderman.teleport`. |
| Laatste minuut (doolhof, Ei) | Bossbar wordt rood. De laatste 10 seconden staan groot in beeld in rood, met `block.note_block.hat` per tel. |
| Winnaar Ei | Title met de kop van de winnaar en `SPELER4 WINT HET EI` in goud, subtitle `185 punten`, `ui.toast.challenge_complete`, vuurpijl boven de winnaar. |
| Start mob arena | `ui.toast.challenge_complete`; het schema blijft geheim. Bij elke beurt de title `BEURT 3` voor iedereen, zonder subtitle. |
| Spelers in de mob arena | Glowing in hun teamkleur tijdens hun beurt. |
| Nieuwe wave | Rookwolk en `block.fire.extinguish` op de spawnpunten, dan title `WAVE 3` in rood, `event.raid.horn`, in beide arena's tegelijk. |
| Mob gekild (ronde 3) | `entity.experience_orb.pickup` voor de killer, actionbar met de punten. Evoker en ravager ook als chatregel voor iedereen. |
| Af in de mob arena | Doodtekst als title, daarna 5 seconden `/mobarena aftekst` in de actionbar. |
| Speler sneuvelt | Alleen de dode ziet een willekeurige doodtekst als title. Geen geluid, geen chatregel. |
| Beurt klaar | Meteen de title `BEURT 3 KLAAR` met de stand van de teams, 10 seconden in beeld, met `ui.toast.challenge_complete`. Aftelling in de actionbar voor wie in een arena of kooi staat; daarna pas naar de tribune. |
| Quiz-rad | Een echt rond rad groot in beeld (plaatjes uit het pack), alleen kleuren, pijltje bovenin; het draait en remt af, `note_block.hat` per vak dat het pijltje passeert. Twee seconden stil op het gekozen vak, dan `entity.player.levelup` en `ROOD IS AAN DE BEURT`. |
| Quiz goed | Title `GOED!` in groen met `+1 Rood`, vanaf twee op rij `+1 Rood · 3 op rij`, `block.note_block.bell`. De lamp bij de bank blijft aan. |
| Quiz fout | Title `FOUT!` in rood, `entity.villager.no`. De lamp bij de bank gaat uit, de reeks op nul. |
| Quiz, presentator | Alleen voor Pudding in de actionbar: `Aan de beurt: Rood · 3 op rij` of `Draai het rad`. |
| Quiz gelijkspel | Title `GELIJKSPEL`, subtitle `Rood en Geel · Pudding kiest`. |
| Het Rad | In beeld: kop en naam van wie onder het pijltje staat als title, de rij van vijf koppen met namen als subtitle, `note_block.hat` per stap. Aan het eind `entity.ender_dragon.growl`, totem-particles, title `DE KROON` met kop en naam. |
| Kroonwissel | `entity.lightning_bolt.thunder` (geen echte bliksem), flash-particle, title `NIEUWE KROON` met kop en naam. |
| Winnaar ronde | `ui.toast.challenge_complete`, vuurpijl, title met naam of team. Bij het Ei en Clown vs All met de kop van de winnaar. |
| Wachten op `/clown go` | Bossbar `Clown vs All · wacht op de start`, actionbar `/clown wachttekst` (standaard `Wacht op het startsein`). |
| Kroonhouder | Alleen voor hem in de actionbar: `Jij hebt de kroon · 11 jagers`. |
| Af in Clown vs All | Chatregel voor iedereen: `Speler3 is af door ClownPierce · 11 over`. De dode ziet zijn doodtekst. |
| Winnaar Clown vs All | Wint de uitverkorene: title `DE EINDBAAS WINT` met zijn kop en naam als subtitle. Anders title met de kop van de winnaar en `SPELER7 WINT CLOWN VS ALL`. Vuurpijl erboven. |
| Wachten op `/ffa go` | Bossbar `FFA · wacht op de start`, actionbar `/ffa wachttekst`. |
| Af in de FFA | Chatregel voor iedereen: `Speler3 is af door Speler7 · 11 over`. |
| `/clown krimp`, `/ffa krimp` | Title `DE BORDER KRIMPT` in rood, subtitle `naar 20 in 60 seconden`, `event.raid.horn`. Kijkers zien de title ook, maar merken niets van de border. |
| Laatste drie, laatste twee | Title `LAATSTE DRIE` of `LAATSTE TWEE` in paars met de namen, `entity.wither.spawn` zacht. |
| Kroning | Twintig seconden vuurpijlen, title `KING OF THE SMP BOOTCAMP` met de kop en de naam van de winnaar als subtitle. Daarna blijven de bossbar `Pudding Bootcamp · King: Speler7` en de zweefkroon tot `/bc reset`. |

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
