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
> Voice Chat zijn er voor. **Deze doc beschrijft het nieuwe rondeplan van 25 september 2026**
> (doolhof, Ei, mob arena, quiz, Clown vs All, FFA). De code in `mod/` implementeert nog het oude
> plan (doolhof, horde, Ei met ticket, King of the SMP, FFA, finale) en is nog nooit in-game
> gedraaid. De ombouw staat in [08-taakplan.md](08-taakplan.md). Een naam uit 26.2 opzoeken:
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
`announceAdvancements` uit, `locatorBar` uit (aan in ronde 5). In 26.2 heten de gamerules in code
anders (`GameRules.ADVANCE_TIME`, `SPAWN_MOBS`, ...) en zet je ze via
`level.getGameRules().set(...)`.

Gamemode zet `/bc start` per ronde: survival alleen in ronde 2 (het Ei), adventure in alle andere
rondes; kijkers altijd adventure.

**Staff is wie in creative of spectator staat.** Geen teleport, geen kit, telt niet mee, zit in
geen team. Er is geen apart command voor.

Op de server: difficulty niet op peaceful (de mob arena) en `spawn-protection=0`. Voor het
resource pack in `server.properties`:

```
resource-pack=<url van bootcamp-pack.zip>
resource-pack-sha1=<sha1 van dat bestand>
require-resource-pack=true
resource-pack-prompt=Nodig voor de bootcamp
```

## PvP

Eén regel in `ALLOW_DAMAGE`, niet via teams, want spelers zitten in verschillende teamkleuren en
mogen elkaar toch niet raken. Bron is de speler, ook via een pijl of andere projectile.

| Ronde | Speler raakt speler |
|---|---|
| 0 t/m 4 | nooit |
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

**Het teammenu** is een vanilla kistmenu (`ChestMenu`, 1 rij, `MenuType.GENERIC_9x1`) met een
`SimpleContainer`: op vier plekken een gekleurd wolblok met de teamnaam en het aantal
(`Rood · 3/5`). Een vol team is grijze wol. Items verplaatsen kan niet (het menu annuleert elke
klik en handelt hem zelf af); klik op een kleur zet je in het team, sluit het menu en teleporteert
je naar `v2`. Sluit je het menu zonder keuze terwijl je in regio `doolhof_uit` staat, dan opent
het na 2 seconden opnieuw. Allemaal server-side, geen client-mod nodig.

## Resource pack en de jumpscare

Het pack staat in `pack/` in deze repo. `pack/bouw.sh` zipt het naar `bootcamp-pack.zip` en
print de SHA-1 voor `server.properties`. Online zetten als bijlage van een GitHub-release (de repo
is public), en die URL in `resource-pack=`.

```
pack/
  pack.mcmeta
  assets/bootcamp/font/schrik.json            bitmap-provider: één glyph U+E000 → clown.png
  assets/bootcamp/textures/font/clown.png     de foto van Clown (vierkant, 256 x 256)
  assets/bootcamp/sounds.json                 bootcamp:clown_lach → sounds/clown_lach.ogg
  assets/bootcamp/sounds/clown_lach.ogg       het lachje (mono, ogg vorbis)
```

De foto en het lachje levert Pudding aan. Tot die er zijn zit er een placeholder in.

**De jumpscare** (`Schrik.op(speler)`): een title met de glyph `` in font
`bootcamp:schrik`, fade-in 0, blijven 30 ticks, fade-out 10, plus `bootcamp:clown_lach` op volle
sterkte, alleen voor die speler. De `height` en `ascent` in `schrik.json` bepalen hoe groot de
foto op het scherm staat; afstemmen in de eerste test tot hij het beeld vult. Heeft een speler het
pack niet (weigerde of downloadfout), dan ziet die een leeg vierkantje en hoort niks; met
`require-resource-pack=true` kan dat niet. `/bc schrik <speler>` doet een jumpscare met de hand,
voor het testen en voor de lol.

Waar een jumpscare vandaan komt:
- Doolhof: in een regio `schrik_1` t/m `schrik_n`, één keer per regio per speler.
- Het Ei: een emerald block, bij een willekeurige andere levende deelnemer.

## Modules

| Package | Doet | Belangrijkste API |
|---|---|---|
| `config` | Regio's, punten, teamkeuzes, grapjes opslaan en laden, JSON in `<wereld>/bootcamp.json`. | Gson, `ServerLifecycleEvents` |
| `kits` | Kits uit JSON-bestanden lezen en op spelers zetten; de loot-tabel van het doolhof. | `ItemParser` (dezelfde syntax als `/give`) |
| `commands` | Het hele `/bc`-commandboompje. | Brigadier, `CommandRegistrationCallback` |
| `setup` | De wand, `region show` met particles. | `AttackBlockCallback`, `UseBlockCallback` |
| `game` | Spelstatus, timer, de zes rondes als klassen met `start/tick/onDeath/end`, de PvP-regel. | `ServerTickEvents.END_SERVER_TICK`, `ALLOW_DAMAGE` |
| `teams` | Vier teamkleuren, het teammenu, de maximumregel. | `ChestMenu`, `SimpleContainer` |
| `crown` | Kroonhouder, laatste hit, kroonwissel, opstelling, bevriezing. | `ServerLivingEntityEvents.ALLOW_DEATH`, `ALLOW_DAMAGE` |
| `rad` | Het Rad (20 lampen, rigged) en de quiz-randomizer (4 lampen, echt toeval): dezelfde code. | tick-gestuurd, geen threads |
| `mobs` | Waves spawnen en tellen, per arena. | `EntityType.spawn`, entity-tags |
| `schrik` | De jumpscare en de nep-uitgang. | title-packets, `playNotifySound` |
| `tribune` | Wie af is naar de tribune of de kooi, daar houden, geen schade, locator bar uit. | `ALLOW_DAMAGE`, tick-check op regio's |
| `visuals` | Bossbar, sidebar, titles, geluid, particles, vuurwerk, zweefkroon, labels. | `ServerBossEvent`, packets, `Display`-entities |

Vuistregel: alles draait op de server-tick. Geen `Thread.sleep`, geen eigen threads; een
wachttijd is een tick-teller in een state-object.

## Commands

Allemaal onder `/bc`, op-level 2.

| Command | Doet |
|---|---|
| `/bc wand` | Geeft de regio-wand (een stick met een custom data component). |
| `/bc region save\|show\|list\|del <naam>` | Regio uit de wand-selectie opslaan; `show` tekent tien seconden particles op de randen. |
| `/bc point set\|block\|tp\|list\|del <naam>` | Punt op je positie (met kijkrichting) of op het blok waar je naar kijkt (tot 32 blokken). |
| `/bc label zet <tekst>` / `/bc label weg` | Een text display boven je hoofd plaatsen of het dichtstbijzijnde weghalen. |
| `/bc start <1-6>` | Teleport naar het startpunt, border, kits, countdown, timer. Breekt een lopende ronde eerst af. Weigert met één regel en verandert niets als er een regio, punt of kit mist. |
| `/bc kit <naam> [<speler>]` | Zet een kit op iedereen die meedoet, of op één speler. |
| `/bc stop` / `/bc timer <sec>` | `stop` breekt de ronde af; `timer` stelt de resterende tijd bij (ronde 1, 2, 6). |
| `/bc status` | Rollen, teams en vlaggen van alle spelers, huidige ronde en timer. |
| `/bc poort <naam> open\|dicht` | Handmatig een poort bedienen. |
| `/bc team <speler> <rood\|blauw\|groen\|geel\|weg>` | Noodknop: iemand in een team zetten of de keuze weghalen. Mag boven het maximum. |
| `/bc schrik <speler>` | Een jumpscare, met de hand. |
| `/bc wave volgende` | Mob arena: de huidige wave telt als klaar in beide arena's (overgebleven mobs weg). |
| `/bc quiz draai` | Quiz: de randomizer draaien. |
| `/bc quiz punt <kleur> [<aantal>]` | Quiz: punten erbij (standaard 1, negatief mag) in de sidebar. Optioneel: de host mag ook zelf tellen. |
| `/bc quiz winnaar <kleur>` | Quiz: winnend team aanwijzen, titles en vuurwerk, einde ronde. |
| `/bc uitverkoren [<speler>]` / `/bc slot <speler> <0-19>` | De verborgen rol en de pilaar van elke kop in De Kring. Het antwoord ziet alleen wie het typt: het rad blijft geheim. |
| `/bc rad` | Het Rad, en daarna start ronde 5. Weigert zonder uitverkorene met een pilaar, zonder de twintig lampen, en als ronde 5 daarna niet zou kunnen starten. |
| `/bc kroon <speler>` | Ronde 5: kroonwissel forceren (de noodknop van de ref). |
| `/bc krimp <grootte> [<seconden>]` | Ronde 5: de border laten krimpen als het stilvalt. Standaard in 60 seconden. |
| `/bc kijker <speler> aan\|uit` | Noodknop: iemand met de hand op de tribune zetten of eraf halen. |
| `/bc reset` | Alles terug naar de basiskamp-staat via het reset-register; ook alle teamkeuzes weg. |

## Regio's en punten

Eén keer zetten na het bouwen. Alles wordt opgeslagen in `<wereld>/bootcamp.json`. Geen
coördinaten in code.

**Regio's** met de wand: linksklik op een blok is hoek 1, rechtsklik hoek 2, dan
`/bc region save <naam>`. **Voor spelers is een regio een kolom**: alleen x en z tellen. Een
poort gebruikt wel de hele doos, en de kistenscan van het doolhof ook. Een veld waar kijkers af
moeten blijven (`vloer`) telt als de **cirkel die in de selectie past**; `veld_a` en `veld_b`
tellen als rechthoek. Startpunten horen binnen de border-regio van hun ronde te liggen; anders
weigert `/bc start`.

| Regio's | Waarvoor |
|---|---|
| `doolhof`, `eibos`, `mobarena`, `quiz`, `vloer` | Worldborder per ronde. `mobarena` omvat beide arena's met tribune en kooien, `vloer` is de vloer van de Arena. |
| `colosseum` | *Optioneel.* De hele Arena inclusief tribunes: border van ronde 5 en 6. Anders `vloer`. |
| `doolhof_uit` | Het vak achter de echte uitgang: wie erin staat krijgt het teammenu. |
| `nep_1` t/m `nep_3` | De vakken aan het eind van de nep-gangen. |
| `schrik_1` t/m `schrik_n` | Schrikplekken in het doolhof. Zoveel als je wilt, genummerd vanaf 1. |
| `poort_doolhof` | De poort voor de echte uitgang, opent na 4 minuten. |
| `veld_a`, `veld_b` | De twee mob-arenavelden. Een kijker die erin komt wordt teruggezet; een levende speler die eruit komt ook. |

**Punten** met `/bc point set <naam>` of `/bc point block <naam>`.

| Punten | Waarvoor |
|---|---|
| `basiskamp` | Spawn en reset. |
| `doolhof_start` | De startruimte in het midden van het doolhof; ook waar een nep-uitgang je neerzet. |
| `v2` | Bosrand van het Ei: na de teamkeuze. |
| `ei_start`, `ei_beacon` (blok) | Waar het Ei begint (en waar je na een dood terugkomt), en het ontbrekende blok in de beaconpiramide. |
| `v3` | Verzamelpunt bij de mob arena. |
| `start_a`, `start_b` | Waar een team het veld van arena A of B op komt. |
| `mob_a_1` t/m `mob_a_4`, `mob_b_1` t/m `mob_b_4` | Mob-spawns per arena. Zelfde volgorde in beide arena's, zodat de waves gelijk zijn. |
| `kooi_a`, `kooi_b` | De kooi naast elk veld. |
| `tribune_mob_1`, `tribune_mob_2` | De tribune tussen de arena's. |
| `quiz_rood`, `quiz_blauw`, `quiz_groen`, `quiz_geel` | De vier vakken van het podium. |
| `quizlamp_0` t/m `quizlamp_3` (blokken) | De lampen voor de vakken, in de volgorde rood, blauw, groen, geel. Gedoofde redstone lamp, geen redstone ernaast. |
| `troon`, `jager_1` t/m `jager_4` | Het verhoogde midden van de Arena en de startpunten aan de rand. |
| `tribune_1` t/m `tribune_4` | De tribune van de Arena. |
| `kroning` | De plek van de kroning. |
| `lamp_0` t/m `lamp_19` (blokken) | De lichtblokken van De Kring, met de klok mee. Gedoofde redstone lamp, geen redstone ernaast. |

Worldborder: `ServerLevel.getWorldBorder()`, center en grootte uit de regio, krimpen met
`lerpSizeBetween`. Altijd eerst teleporteren, dan de border zetten.

## Kits (JSON)

Elke kit is een JSON-bestand in `config/bootcamp/kits/`. De bestanden worden bij elk gebruik
opnieuw gelezen; een ronde weigert te starten als een kit die ze nodig heeft ontbreekt of een fout
bevat.

| Bestand | Wanneer | `clear` |
|---|---|---|
| `basis.json` | Start ronde 1, iedereen: iron armor, iron sword, 32 steak. | true |
| `ei.json` | Start ronde 2: diamond pickaxe met Efficiency II erbij. | false |
| `mobarena.json` | Start ronde 3: boog, 32 pijlen, schild, 16 steak erbij. | false |
| `boss.json` | Clown bij de start van ronde 5. Geen helm: de kroon. | true |
| `jager.json` | Iedereen anders bij de start van ronde 5. Zelfde voor iedereen. | true |
| `kroonpakket.json` | Bij elke kroonwissel erbij: 2 gapples, 2 pearls. | false |
| `arena.json` | Start ronde 6, iedereen. | true |

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
willekeurig gekozen items uit de tabel. Gewicht bepaalt hoe vaak een item valt.

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

**Waves** staan in `config/bootcamp/waves.json`: per wave een lijst mobs met type, aantal (voor
een team van 5), gear en spawnpunt 1 t/m 4. Het aantal schaalt met het aantal levende spelers in
het team met de meeste spelers, zodat beide arena's precies dezelfde wave krijgen. Na de laatste
wave uit het bestand komt die wave opnieuw met `extraPerWave` (standaard 1) extra per mobtype,
elke keer weer.

## Spellogica

**Status.** Eén `GameState`: huidige ronde, timer, vlaggen, en per speler een rol (`SPELER`,
`JAGER`, `KROON`, `FFA`, `KIJKER`, `STAFF`), een team, en vlaggen (`dood`, `klaar`, `uitverkoren`,
slot, punten, schrikplekken gehad). De mod is de bron van waarheid; scoreboard-tags zijn read-only
spiegels die elke seconde worden bijgezet. `/bc status` toont alles.

**Ontbrekende config.** Elke ronde declareert welke regio's, punten en kits ze nodig heeft.
`/bc start` weigert met één regel ("ontbreekt: kooi_b, mob_a_3, ...") en verandert niets. Ronde 3
en 4 weigeren ook als niet iedereen een team heeft: `/bc team` lost dat op.

**Uitloggen en terugkomen.** In ronde 1 t/m 4 kom je terug waar je hoort: in het doolhof bij de
start, in het Ei bij `ei_start` met je punten, in de mob arena in de kooi (een uitlogger telt als
dood), in de quiz in je vak. In ronde 5: een jager die uitlogt is af; de kroonhouder krijgt 30
seconden. Wie terugkomt in ronde 5 of 6 wordt kijker op de tribune.

**Fouten.** Een fout in de tick van een ronde stopt de server niet: de mod logt hem, breekt de
ronde af en meldt het in de chat.

**Dood.** `ALLOW_DEATH`: de mod laat spelers nooit echt doodgaan. Bij een dodelijke klap wordt de
dood geannuleerd, de speler geheald en afgehandeld volgens de ronde:

| Ronde | Wat er gebeurt |
|---|---|
| 1 | Geheald terug naar `doolhof_start`. |
| 2 | Geheald terug naar `ei_start`, punten en spullen blijven. |
| 3 | Kijker in de kooi van zijn arena. Inventory wordt bewaard en aan het eind van ronde 3 teruggegeven. Doodtekst. Is het team leeg, dan is de wedstrijd voorbij. |
| 4 | Kan niet: geen schade. |
| 5, kroonhouder | Kroonwissel naar de killer, anders de laatste hit, anders een willekeurige levende jager. Ex-kroonhouder wordt kijker op de tribune. |
| 5, jager | Kijker op de tribune, af. Is er nog maar één speler over, dan is die de winnaar. |
| 6 | Kijker op de tribune; laatste over is King of the SMP. |

Geen death-screen, geen respawn. De laatste hit komt uit `ALLOW_DAMAGE`.

**Ronde 1, doolhof.** Start: iedereen naar `doolhof_start`, basiskit, poort dicht, kisten vullen,
border `doolhof`. Countdown, dan de timer van 10 minuten. Op 6:00 resterend (na 4 minuten) gaat
`poort_doolhof` open met horn en title `DE UITGANG IS OPEN`. Elke 5 ticks: wie in `nep_n` staat
krijgt explosie-particles (`explosion_emitter`), `entity.creeper.primed` plus
`entity.generic.explode`, een willekeurig grapje als title (lijst in `bootcamp.json`) en gaat naar
`doolhof_start`; wie in `schrik_n` staat en die nog niet had krijgt de jumpscare; wie in
`doolhof_uit` staat zonder team krijgt het teammenu. Hint-title op 3:00 resterend. Timer op: wie
nog geen team heeft gaat naar het kleinste team (bij gelijk: willekeurig), iedereen naar `v2`.

**Ronde 2, het Ei.** Start: `ei_start`, survival, `ei.json`, border `eibos`, timer 10 minuten.
`PlayerBlockBreakEvents.BEFORE` in regio `eibos`: netherite, diamond, gold, redstone en emerald
block breken zonder drop (`level.removeBlock`, event geannuleerd), met punten of effect:

- netherite +50, diamond +10, gold +5: `entity.experience_orb.pickup`, actionbar `+10 · 85 punten`.
- redstone: 50/50. Haste II 10 seconden voor de breker, of iedereen behalve de breker 15 seconden
  bevroren: dezelfde bevriezing als de opstelling, plus Mining Fatigue zodat ze ook niet minen.
  Title voor iedereen: `BEVROREN door <naam>`. Een nieuwe bevriezing vervangt een lopende.
- emerald: jumpscare bij een willekeurige andere deelnemer in het Ei.

Sidebar: de top 10 op punten. Beacon-hint op 5:00, vuurpijl op 3:00. Timer op: de meeste punten
wint (gelijk: wie die score het eerst had), title en vuurpijl, iedereen naar `v3`, adventure.

**Ronde 3, mob arena.** Start bij `v3`: `mobarena.json`, border `mobarena`, loting van de
wedstrijden (`core`: vier teams → twee paren, willekeurig), title met het schema. Een wedstrijd:
team 1 naar `start_a`, team 2 naar `start_b`, de andere teams naar `tribune_mob_n`, countdown 5.
Waves spawnen op `mob_a_n` en `mob_b_n` met tag `bootcamp_mob` en `arena_a` of `arena_b`,
`setPersistenceRequired()`, stenen knoop op het hoofd tegen zonlicht. Een arena is klaar met een
wave als haar teller 0 is; zijn beide klaar, dan na 10 seconden de volgende wave. Na 120 seconden
telt een wave altijd als klaar. Een team zonder levende speler op het veld verliest; vallen
beide teams in dezelfde seconde, dan wint het team waarvan de laatste het laatst viel, en bij
exact gelijk de arena met de minste mobs over. Na de wedstrijd: mobs weg, beide teams naar de
tribune. Na de halve finales de finale, met alleen de levenden op het veld en de doden van die
teams in hun kooi. Winnaar: titles en vuurpijlen. Einde: iedereen weer speler, bewaarde
inventory terug, geheald, naar de quiz.

**Ronde 4, quiz.** Start: iedereen naar `quiz_<kleur>` van zijn team, border `quiz`, geen timer.
`/bc quiz draai` draait de randomizer over `quizlamp_0..3`: dezelfde code als het Rad, met 4
lampen, een echt willekeurig doel en 2 of 3 rondes. Einde: title `<KLEUR> IS AAN DE BEURT` in de
teamkleur, `entity.player.levelup`. De sidebar toont de punten als de host `/bc quiz punt`
gebruikt. `/bc quiz winnaar <kleur>` sluit af.

**Het Rad (start van ronde 5).** State-object met `pos`, `rest` en `volgendeStapTick`. Start:
`rest` = `(doelslot - pos + n) mod n + n * (2 of 3)`, `pos` willekeurig, n = 20. Per stap: lamp
uit, pos + 1, lamp aan, `rest` - 1, hat-geluid, en de wachttijd loopt op van 2 naar 30 ticks
naarmate `rest` kleiner wordt. Bij 0: dragon growl, totem-particles, title `DE KROON` met naam,
drie seconden later `start(5)`. Bij de quiz is n = 4 en het doel willekeurig.

**Ronde 5, Clown vs All.** Iedereen uit zijn teamkleur. Clown naar `troon`, `boss.json`, de kroon,
Glowing, team `kroon`. De rest `jager.json`, team `jagers`, verdeeld over `jager_1..4`.
Opstelling van 30 seconden. Border `colosseum` (of `vloer`), geen timer, locator bar aan met
alleen de kroonhouder zichtbaar. Elke seconde: is er nog maar één levende deelnemer, dan einde.

**Kroonwissel** (`Kroon.wissel(oude, nieuwe)`): oude wordt kijker op de tribune; nieuwe naar
`troon`, heal, honger vol, alles op volle durability, de kroon als helm (oude helm naar de
inventory), `kroonpakket.json`, 15 seconden Resistance II, Glowing, zweefkroon; dan
`Opstelling(10)`.

**Opstelling(seconden)**: alle levende jagers heal en naar `jager_1..4`, bevriezen, countdown in
de actionbar met de laatste vijf seconden als title plus pling, dan los met een groene GO en de
raid horn. Bevriezen is `MOVEMENT_SPEED` en `JUMP_STRENGTH` op basiswaarde 0 plus een
`UseItemCallback` die pearls, wind charges en chorus fruit blokkeert zolang de vlag staat. Na een
wissel staat ook de kroonhouder stil. Tijdens een opstelling doet niemand elkaar schade.

**Ronde 6, FFA en kroning.** Iedereen behalve Clown, ook wie af was, naar `jager_1..4` om en om,
`arena.json`, geen team, countdown 10. Border krimpt na 5 minuten in 2 minuten naar 10. Laatste
levende wint; na 10 minuten beslist het aantal kills. Dan de kroning: iedereen naar de tribune,
de winnaar naar `kroning` met de kroon, twintig seconden vuurpijlen, title `KING OF THE SMP`.

## Kijkers: wie af of dood is

Geen spectator mode, geen tp-items, geen vliegen.

- Adventure mode, team `out` (grijs in de tab-list), inventory leeg (in ronde 3 bewaard en aan het
  eind teruggegeven).
- Geen schade, ook niet van de border. Een kijker ziet de border niet: hij krijgt een eigen
  border-pakket zo groot als de wereld, anders geeft de client een rood scherm.
- Blijft op zijn plek: glas, en een tick-check die een kijker die toch in `vloer`, `veld_a` of
  `veld_b` komt terugzet op zijn tribunepunt of in zijn kooi.
- Niet op de locator bar, geen Glowing.
- Bij de dood een title met een willekeurige doodtekst, alleen voor de dode zelf. De lijst staat
  in `bootcamp.json`.

Waar kijkers heen gaan: ronde 3 naar `kooi_a` of `kooi_b` tijdens de wedstrijd van hun team en
anders naar `tribune_mob_n`; ronde 5 en 6 naar `tribune_n`. Clown zit tijdens de FFA ook op de
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
| 1, poort dicht | `Doolhof · uitgang open over 02:13` | rood | tijd tot open |
| 1, poort open | `Doolhof · 04:41` | groen | tijd |
| 2 | `Het Ei · 07:12` | groen, geel na de hint | tijd |
| 3 | `Rood vs Blauw · Wave 4` | rood | mobs over |
| 4 | `Quiz · aan de beurt: Groen` | teamkleur | vol |
| 5 | `Clown vs All · Kroon: Clown · 12 over` | geel | spelers over |
| 6 | `FFA · 7 over · 04:59` | paars | tijd |

**Sidebar**: ronde 1 de teams met aantallen (`Rood 3/5`), ronde 2 de top 10 op punten, ronde 3
per team hoeveel er nog staan, ronde 4 de quizpunten, ronde 5 de regeerperiodes.

**Zweefkroon**: een `Display.ItemDisplay` met een gouden helm boven het hoofd van de kroonhouder,
langzaam draaiend. Opgeruimd als de kroonhouder kijker wordt.

**Labels**: een `Display.TextDisplay` boven elk verzamelpunt, één keer geplaatst met
`/bc label zet <tekst>`. Ze overleven `/bc reset`.

**Per moment**

| Moment | Wat je ziet en hoort |
|---|---|
| Countdown | Titles 5 t/m 1 met een stijgende `note_block.pling`, dan `GO` met `event.raid.horn`. |
| Poort doolhof open | `event.raid.horn`, cloud-particles in de poort, title `DE UITGANG IS OPEN`. |
| Nep-uitgang | Explosie-particles, creeper-sis en knal, grapje als title, terug in de startruimte. |
| Jumpscare | Foto van Clown schermvullend, `bootcamp:clown_lach`. |
| Team gekozen | `entity.player.levelup`, je naam in de teamkleur, chatregel voor iedereen: `<naam> zit in Rood (3/5)`. |
| Ei: punten | `entity.experience_orb.pickup`, actionbar met je score. |
| Ei: redstone | Haste: `block.beacon.power_select`. Bevriezing: title `BEVROREN door <naam>`, `block.glass.break`. |
| Ei-hint op 5 min | Beacon aan, `block.beacon.activate`, bossbar geel. Op 3 min een vuurpijl boven het Ei. |
| Loting mob arena | Titles met het schema, `ui.toast.challenge_complete`. |
| Nieuwe wave | Title `WAVE 3` in rood, `event.raid.horn`, in beide arena's tegelijk. |
| Speler sneuvelt | Alleen de dode ziet een willekeurige doodtekst als title. Geen geluid, geen chatregel. |
| Wedstrijd gewonnen | Title `ROOD WINT` in de teamkleur, vuurpijlen boven het veld. |
| Quiz-randomizer | Lampjes rond met `note_block.hat`, aan het eind `entity.player.levelup` en de teamtitle. |
| Het Rad | Lampjes rond met `note_block.hat`. Aan het eind `entity.ender_dragon.growl`, totem-particles, title `DE KROON` met naam. |
| Kroonwissel | `entity.lightning_bolt.thunder` (geen echte bliksem), flash-particle, title `NIEUWE KROON` met naam. |
| Winnaar ronde | `ui.toast.challenge_complete`, vuurpijl, title met naam of team. |
| Kroning | Twintig seconden vuurpijlen, title `KING OF THE SMP` met naam. |

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
