# bootcamp-mod

Server-side Fabric-mod voor de Pudding Bootcamp SMP op Minecraft 26.2. Spelers hebben alleen
Simple Voice Chat nodig en krijgen bij het joinen vanzelf het resource pack. De mod doet de zes
rondes (doolhof, Ei, mob arena, quiz, Clown vs All, FFA), de teams en het teammenu, de kroon, het
Rad, het quiz-rad, de tribune voor wie af is, de jumpscare, de bossbar en de visuals. Voice is puur
proximity en gaat buiten de mod om.

- De spec: [../docs/04-technische-schets.md](../docs/04-technische-schets.md), met de spelregels in
  [../docs/02-rondes.md](../docs/02-rondes.md) en [../docs/03-kroon-regels.md](../docs/03-kroon-regels.md).
- Wat er gebouwd en geverifieerd is, en wat jij nog in-game moet testen: [BOUWLOG.md](BOUWLOG.md).
- Elke spelregel die rekent, met zijn test: [core/REGELS.md](core/REGELS.md).

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
- `fabric`: de lijm naar Minecraft (Fabric Loom). Neemt `core` mee in de jar. Eén test,
  `KitsParseTest`, draait met de echte 26.2-registries en haalt elk item uit de standaardkits, de
  loot-tabel, de waves en de kroon door de vanilla item-parser.

Alleen de kernlogica bouwen en testen, zonder Loom: `./gradlew -PcoreOnly :core:test`.

`./check.sh` draait de vaste verificatie: build, jar-inhoud, geen threads of sleeps, `core` zonder
Minecraft-imports, en alle command-literals uit docs/04 aanwezig.

## Het resource pack

Het pack staat in [../pack/](../pack/). Foto van Clown (jpg of png, elk formaat) en het lachje
(`clown_lach.ogg`) in `pack/aanleveren/`, dan vanuit de repo:

```
java pack/BouwPack.java
```

Dat maakt `pack/bootcamp-pack.zip` (foto, quiz-rad in 64 standen, fonts) en print de SHA-1. Zonder
foto zit er een placeholder in, zonder lachje is de jumpscare stil. Zet de zip online, bijvoorbeeld
als bijlage van een GitHub-release, en vul `server.properties` in:

```
resource-pack=<url van bootcamp-pack.zip>
resource-pack-sha1=<de sha-1 die BouwPack print>
require-resource-pack=true
resource-pack-prompt=
```

De prompt is in 26.2 een JSON-tekst. Een host als Pterodactyl herschrijft `server.properties` bij
elke start en slikt dan het afsluitende aanhalingsteken en de regelovergang van
`"Nodig voor de bootcamp"` in; daarom leeg laten (dan toont Minecraft zijn eigen tekst).

Na een nieuwe foto: opnieuw bouwen, opnieuw uploaden, nieuwe SHA-1 invullen, server herstarten. Hoe
groot de foto en het rad in beeld staan, stel je in met `SCHRIK_EENHEDEN` en `RAD_EENHEDEN` (en
de pixels per eenheid) bovenin `BouwPack.java`. De plaatjes worden in tegels van hooguit 242 pixels
geknipt, omdat Minecraft grotere font-plaatjes als leeg vierkantje toont; zie docs/04.

## Installeren

De server heeft **Java 25** nodig, mag niet op peaceful staan (de mob arena), en zet
`spawn-protection=0` in `server.properties`. In `mods/` van de Fabric-server (26.2):

- `bootcamp-<versie>.jar`
- Fabric API voor 26.2
- Simple Voice Chat (Fabric) voor 26.2, config in [../docs/07-voice.md](../docs/07-voice.md)

Jar vervangen betekent server herstarten. Bewaar de jar van de vorige werkende versie.

### Wat er bij de eerste start gebeurt

- In de console: `Pudding Bootcamp geladen`, `Gamerules gezet`.
- `config/bootcamp/` wordt gevuld met wat ontbreekt: `waves.json`, `doolhof_loot.json` en in
  `kits/` de bestanden `basis`, `ei`, `jager`, `boss`, `kroonpakket` en `arena`. Wat er al staat
  wordt nooit overschreven. Staan er nog bestanden van het oude rondeplan (`kits/horde.json`), dan
  gaan `waves.json`, `ei`, `boss`, `arena`, `horde` en `finale` opzij als `.oud` en komen de nieuwe
  ervoor in de plaats.
- `<wereld>/bootcamp.json` bestaat nog niet; de mod begint met een lege config en de standaard
  instellingen.
- Gamerules: natural regeneration en PvP aan (de mod beslist zelf wie wie mag raken); mob spawning,
  mob griefing, daglichtcyclus, weer, advancement-meldingen en locator bar uit, en `random_tick_speed`
  op 0 (geplakte bladeren vergaan dan niet). De locator bar gaat
  aan in Clown vs All.
- De teams `spelers`, `rood`, `blauw`, `groen`, `geel`, `jagers`, `kroon` en `out`.

## Bestanden

| Bestand | Wat |
|---|---|
| `<wereld>/bootcamp.json` | Regio's, punten, doodteksten, grapjes van de nep-uitgangen, teamkeuzes, uitverkorene, presentator en de instellingen per ronde. Wordt na elke wijziging opgeslagen. Met de hand aanpassen mag, maar alleen als de server uit staat. Een onleesbaar bestand wordt opzij gezet als `bootcamp.json.kapot`. |
| `<wereld>/bootcamp_ei.nbt` | Het Ei zoals je het met `/ei vastleggen` hebt vastgelegd. |
| `config/bootcamp/kits/<naam>.json` | Een kit. Wordt bij elk gebruik opnieuw gelezen: aanpassen zonder herstart. |
| `config/bootcamp/waves.json` | De waves van de mob arena, per arena voor 4 spelers. Wordt bij `/mobarena start` gelezen. |
| `config/bootcamp/doolhof_loot.json` | De loot-tabel van de doolhofkisten. Wordt bij `/doolhof start` gelezen. |

### Kits

Per slot een item in dezelfde syntax als `/give`; een getal achter het item is het aantal.

```json
{
  "clear": true,
  "armor": { "head": "minecraft:iron_helmet", "chest": "...", "legs": "...", "feet": "..." },
  "offhand": "minecraft:shield",
  "hotbar": ["minecraft:iron_sword", "minecraft:cooked_beef 32"],
  "inventory": []
}
```

- `"clear": true` maakt de inventory eerst leeg. `false` voegt toe: een item komt op zijn slot als
  dat leeg is en anders ergens in de inventory; wat je al draagt blijft aan.
- De kroon blijft altijd op: zit hij in de head-slot, dan schrijft geen kit eroverheen en haalt
  `clear` hem niet weg. `boss.json` heeft daarom geen helm.
- Een fout komt als één regel in de console en in de chat, met bestandsnaam en slot:
  `arena.json, hotbar[2]: 'minecraft:arow' is geen geldig item`.

| Kit | Wanneer | clear |
|---|---|---|
| `basis` | Start doolhof: iron armor, iron sword, 32 steak. | ja |
| `ei` | Start Ei: diamond pickaxe met Efficiency II. De mod markeert hem en haalt hem aan het eind weer weg. | nee |
| `jager` | Iedereen behalve Clown bij de start van Clown vs All: full diamond (Protection IV, Unbreaking III), sword en axe (Sharpness V), bow (Power V), 32 pijlen, schild, 16 gapples. | ja |
| `boss` | Clown bij de start van Clown vs All: de jagerskit zonder helm (de kroon zit al op). | ja |
| `kroonpakket` | Bij elke kroonwissel: 2 gapples, 2 pearls. | nee |
| `arena` | Start FFA: de jagerskit met gewone helm en 32 gapples. | ja |

### Waves en loot

```json
{ "waves": [
  { "naam": "Zombies", "mobs": [ { "type": "minecraft:zombie", "aantal": 4 } ] },
  { "naam": "Ravager, vindicators en evoker", "mobs": [
    { "type": "minecraft:ravager", "aantal": 1 },
    { "type": "minecraft:vindicator", "aantal": 3, "gear": { "mainhand": "minecraft:iron_axe" } } ] }
] }
```

Het aantal geldt per arena en schaalt niet mee. Gear-slots: `head`, `chest`, `legs`, `feet`,
`mainhand`, `offhand`. De standaard is de tabel uit docs/02 (5 waves).

`doolhof_loot.json`: `"perKist": [2, 4]` en per item een `"gewicht"`. Geen ender pearls.

## Eén keer zetten na het bouwen

Alles komt in `bootcamp.json`. Geen coördinaten in code. Het volledige overzicht staat in docs/04
onder *Regio's en punten*; hier per ronde wat je zet.

**Regio's** met de wand: `/bc wand`, linksklik op een blok is hoek 1, rechtsklik hoek 2, dan
`/bc region save <naam>`. `/bc region add <naam>` voegt de selectie toe als extra deel (de T van een
veld is twee selecties). Voor spelers telt een doos als kolom (alleen x en z). `/bc region show
<naam>` tekent tien seconden alle delen.

**Punten**: `/bc point set <naam>` (je positie plus kijkrichting) of `/bc point block <naam>` (het
blok waar je naar kijkt, tot 32 blokken). De commando's per ronde zetten de meeste punten ook.

| Ronde | Regio's | Punten |
|---|---|---|
| Algemeen | | `basiskamp` (optioneel, voor `/bc reset`) |
| Doolhof | `doolhof`, `doolhof_uit`, `poort_doolhof` (de muur, onder- en bovenhoek), `nep_1..3`, `schrik_1..n` | `doolhof_start`, `v2` |
| Het Ei | `ei` (het Ei als doos), `eigebied` (Ei, kettingen, startplekken) | `ei_spawn_1..n`, `v3`; daarna `/ei vastleggen` |
| Mob arena | `mobarena` (beide arena's met tribune), `veld_1`, `veld_2` (elk `save` + `add`) | `/mobarena startplek <1\|2> <kleur>` (8x), `mob_1_1..n`, `mob_2_1..n`, `kooi_1`, `kooi_2`, `tribune_mob_1..n` |
| Quiz | `quiz` | `/quiz bank <kleur>` (4x), `/quiz podium`, `/quiz lamp <kleur>` (4x, kijk naar de lamp) |
| Clown vs All, FFA | `/clown vloer <diameter>` (midden op de vloer staan), optioneel `colosseum` | `/clown troon`, `/clown jagerplek` (20x), `/clown tribune` (2 of meer, onderste ring) |

Daarna: `/quiz presentator <speler>` en `/clown uitverkoren <speler>`. Labels boven de
verzamelpunten: `/bc label zet <tekst>`.

`/<ronde> start` weigert met één regel en verandert dan niets als er een regio, punt of kit mist
(`ontbreekt: kooi_2, start_1_geel`), een startpunt buiten de border ligt, een tribuneplek op de vloer
ligt, of (mob arena, quiz) iemand geen team heeft.

## Commands

Alles op op-level 2. Het volledige overzicht met de grenzen van elke instelling staat in docs/04
onder *Commands*.

| Ronde | Commands |
|---|---|
| Doolhof | `/doolhof start\|stop\|resterend <sec>`, `timer [<min>]` (15), `poort [<min>]` (4), `hint [<min>]` (10), `hinttekst [<tekst>\|-]`, `poort open\|dicht` |
| Het Ei | `/ei start\|stop\|resterend <sec>`, `timer [<min>]` (15), `blokken [<soort> <aantal>]`, `vastleggen` |
| Mob arena | `/mobarena start\|volgende\|schema\|stop`, `wave volgende`, `startplek <1\|2> <kleur>`, `punten [<mob> <punten>]`, `aftekst [<tekst>]` |
| Quiz | `/quiz start\|stop`, `presentator [<speler>]`, `bank <kleur>`, `podium`, `lamp <kleur>`, `draai`, `goed`, `fout`, `punt <kleur> [<aantal>]`, `einde`, `winnaar <kleur>` |
| Clown vs All | `/clown rad\|go\|start\|stop`, `uitverkoren [<speler>]`, `troon`, `jagerplek [<nr>]`, `vloer <diameter>`, `tribune [<nr>]`, `wachttekst [<tekst>]`, `kroon <speler>`, `krimp <grootte> [<sec>]` |
| FFA | `/ffa start\|stop\|go`, `krimp <grootte> [<sec>]`, `wachttekst [<tekst>]` |
| Algemeen | `/bc wand`, `region save\|add\|show\|list\|del`, `point set\|block\|tp\|list\|del`, `label zet\|weg`, `status`, `kit <naam> [<speler>]`, `team <speler> <kleur\|weg>`, `schrik <speler>`, `kijker <speler> aan\|uit`, `reset` |

**Staff** is wie in creative of spectator staat: de mod blijft van ze af (geen teleport, geen kit,
ze tellen niet mee). Zet host, camera's en admins dus in creative of spectator vóór een ronde.

De scoreboard-tags `kroon`, `jager`, `ffa`, `kijker` en `uitverkoren` zijn spiegels die elke
seconde worden bijgezet, zodat je met `@a[tag=...]` kunt kijken. Zelf zetten heeft geen zin.

## De avond

| Ronde | Commander |
|---|---|
| 1 Doolhof | `/doolhof start`. Eindigt vanzelf na de timer, of eerder als iedereen een team heeft. |
| 2 Het Ei | `/ei start`. Eindigt na de timer. |
| 3 Mob arena | `/mobarena start` (beurt 1), daarna per beurt `/mobarena volgende`. Na de laatste beurt vanzelf de winnaar en, tien seconden later, iedereen zonder spullen naar zijn bank. |
| 4 Quiz | `/quiz start`. Pudding draait met de nether star en keurt met de wol. `/quiz einde`, bij gelijkspel `/quiz winnaar <kleur>`. Tien seconden later iedereen naar de tribune van de Arena. |
| 5 Clown vs All | `/clown rad` (of `/clown start` zonder rad), iedereen staat bevroren klaar, dan `/clown go`. |
| 6 FFA | `/ffa start`, dan `/ffa go`. De kroning volgt vanzelf. |

Tussen twee rondes in is er geen border en geen PvP. Gaat er in een ronde iets mis in de mod zelf,
dan breekt die ronde zichzelf af met een melding in de chat en de fout in de console; de server
blijft draaien. Start de ronde opnieuw met `/<ronde> start`.
