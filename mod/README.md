# bootcamp-mod

Server-side Fabric-mod voor de Pudding Bootcamp SMP op Minecraft 26.2. Spelers hebben alleen
Simple Voice Chat nodig, verder een gewone client. De mod doet de zes rondes, de kroon, het rad, de
tribune voor wie dood is, de bossbar en de visuals. Voice is puur proximity en gaat buiten de mod
om.

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
  `KitsParseTest`, draait met de echte 26.2-registries en haalt elk item uit de standaardkits door
  de vanilla item-parser.

Alleen de kernlogica bouwen en testen, zonder Loom en zonder Minecraft te downloaden:

```
./gradlew -PcoreOnly :core:test
```

`./check.sh` draait de vaste verificatie: build, jar-inhoud, geen threads of sleeps, `core` zonder
Minecraft-imports, en alle command-literals uit docs/04 aanwezig.

Een naam uit 26.2 opzoeken: `./gradlew :fabric:genSources` en dan zoeken in de sources-jar onder
`.gradle/loom-cache/`.

## Installeren

De server heeft ook **Java 25** nodig, mag niet op peaceful staan (de horde), en zet
`spawn-protection=0` in `server.properties` (anders kunnen spelers bij de wereldspawn geen kisten
openen). In `mods/` van de Fabric-server (26.2):

- `bootcamp-<versie>.jar`
- Fabric API voor 26.2
- Simple Voice Chat (Fabric) voor 26.2, config in [../docs/07-voice.md](../docs/07-voice.md)

Jar vervangen betekent server herstarten. Bewaar de jar van de vorige werkende versie.

### Wat er bij de eerste start gebeurt

- In de console: `Pudding Bootcamp geladen`, `Gamerules gezet`.
- `config/bootcamp/` wordt gevuld met wat ontbreekt: `waves.json` en in `kits/` de bestanden
  `horde`, `ei`, `boss`, `kroonpakket`, `arena`, `finale` en `basis.README.txt`. Wat er al staat
  wordt nooit overschreven.
- `<wereld>/bootcamp.json` bestaat nog niet; de mod begint met een lege config.
- Gamerules: natural regeneration en PvP aan (PvP is in 26.2 een gamerule; tot ronde 4 houdt team
  `spelers` het tegen); mob spawning, mob griefing, daglichtcyclus, weer,
  advancement-meldingen en locator bar uit. De locator bar gaat aan in ronde 4 t/m 6.
- De teams `spelers`, `hunters`, `king` en `out` worden aangemaakt.

## Bestanden

| Bestand | Wat |
|---|---|
| `<wereld>/bootcamp.json` | Regio's, punten, doodteksten, de pilaar van elke kop, de uitverkorene. Wordt na elke wijziging opgeslagen. Met de hand aanpassen mag, maar alleen als de server uit staat. Een onleesbaar bestand wordt opzij gezet als `bootcamp.json.kapot`. |
| `config/bootcamp/kits/<naam>.json` | Een kit. Wordt bij elk gebruik opnieuw gelezen: aanpassen zonder herstart. |
| `config/bootcamp/kits/basis.json` | **Moet je zelf neerzetten**, de inhoud komt van Pudding. Zie `basis.README.txt` ernaast. Zonder dit bestand weigeren ronde 3 en ronde 4 te starten. |
| `config/bootcamp/waves.json` | De waves van de horde. Wordt bij `/bc start 2` gelezen. |

### Kits

Per slot een item in dezelfde syntax als `/give`; een getal achter het item is het aantal.

```json
{
  "clear": true,
  "armor": { "head": "minecraft:iron_helmet", "chest": "...", "legs": "...", "feet": "..." },
  "offhand": "minecraft:shield",
  "hotbar": ["minecraft:iron_sword", "minecraft:bow[minecraft:enchantments={\"minecraft:power\":1}]", "minecraft:arrow 16"],
  "inventory": []
}
```

- `"clear": true` maakt de inventory eerst leeg. `false` voegt toe: een item komt op zijn slot als
  dat leeg is en anders ergens in de inventory; armor dat er al zat gaat naar de inventory.
- De kroon blijft altijd op: zit hij in de head-slot, dan schrijft geen kit eroverheen en haalt
  `clear` hem niet weg. `boss.json` en `finale.json` hebben daarom geen helm.
- Een fout komt als één regel in de console en in de chat, met bestandsnaam en slot:
  `arena.json, hotbar[2]: 'minecraft:arow' is geen geldig item`.

| Kit | Wanneer | clear |
|---|---|---|
| `basis` | Zonder ticket uit het Ei-bos; hunters met een lege inventory in ronde 4. | zelf kiezen |
| `horde` | Start ronde 2. | nee: je houdt wat je in het doolhof vond |
| `ei` | Start ronde 3: de pickaxe. | nee |
| `boss` | Clown bij de start van ronde 4. | ja |
| `kroonpakket` | Bij elke kroonwissel. | nee |
| `arena` | Start ronde 5. | ja |
| `finale` | Elk potje van ronde 6. | ja |

### Waves

```json
{ "waves": [
  { "naam": "Zombies", "mobs": [ { "type": "minecraft:zombie", "aantal": 20 } ] },
  { "naam": "Boss wave", "schaal": false, "mobs": [
    { "type": "minecraft:vindicator", "aantal": 10, "gear": { "mainhand": "minecraft:iron_axe" } } ] }
] }
```

Aantallen gelden voor twintig spelers en schalen naar boven afgerond mee met het aantal spelers
bij de start; `"schaal": false` houdt een wave vast. Gear-slots: `head`, `chest`, `legs`, `feet`,
`mainhand`, `offhand`. **Voor een testrun met vijf man is de boss wave te zwaar**: zet de aantallen
omlaag of `"schaal": true`.

## Eén keer zetten na het bouwen

Alles hieronder komt in `bootcamp.json`. Geen coördinaten in code.

**Regio's**: `/bc wand`, linksklik op een blok is hoek 1, rechtsklik hoek 2, dan
`/bc region save <naam>`. Voor spelers telt een regio als kolom (alleen x en z), dus twee hoeken op
de grond is genoeg. Een poort is de hele doos: selecteer de onderhoek en de bovenhoek van de muur.

| Regio | Waarvoor |
|---|---|
| `doolhof` | Border van ronde 1. **Ruim selecteren**: het plein voor de poort (`doolhof_start`) en het vak achter de uitgang moeten erbinnen liggen. |
| `doolhof_uit` | Het vak achter de uitgang: wie erin staat is eruit. |
| `arena` | Border van ronde 2 en de vloer waar kijkers af moeten blijven. `arena_spawn` en `mob_1..4` liggen erbinnen, `tribune_horde_1..2` buiten de cirkel die in de selectie past. |
| `eibos` | Border van ronde 3. `ei_start`, `ei_beacon` en `eiplaat` liggen erbinnen. |
| `eiplaat` | Het vak waar je ticket wordt ingenomen. |
| `vloer` | De vloer van de Arena: waar kijkers af moeten blijven, en de border van ronde 4 en 5 als `colosseum` er niet is. `troon` en `hunter_1..4` liggen erbinnen, `tribune_1..4` juist erbuiten. Selecteer het vierkant om de ronde vloer heen: voor kijkers telt de cirkel die erin past, dus de hoeken en de ring achter de rand zijn tribune. |
| `colosseum` | *Optioneel.* De hele Arena inclusief tribunes; dan is dit de border van ronde 4 en 5. |
| `finale` | Het midden van de Arena. De border van ronde 6 is 20 x 20 om het midden van deze regio. |
| `poort_doolhof`, `poort_arena`, `poort_bos` | *Optioneel.* De muur die open en dicht gaat. |

**Punten**: `/bc point set <naam>` (je positie plus kijkrichting) of `/bc point block <naam>` (het
blok waar je naar kijkt, tot 32 blokken).

| Punt | Waarvoor |
|---|---|
| `basiskamp` | Waar `/bc reset` iedereen neerzet. Optioneel: zonder blijft iedereen staan. |
| `doolhof_start`, `v2` | Ingang van het doolhof; verzamelpunt bij de horde-arena. |
| `arena_spawn`, `mob_1..4`, `tribune_horde_1..2`, `v3` | Horde: binnenkomst, spawns, tribune, verzamelpunt aan de bosrand. |
| `ei_start`, `ei_beacon` (blok), `kring` | Bosrand; het ontbrekende blok in de beaconpiramide; De Kring op de vloer van de Arena. |
| `troon`, `hunter_1..4`, `tribune_1..4` | Het midden; de startpunten aan de rand (ook voor de FFA); de tribune. |
| `lamp_0..19` (blokken) | De twintig lampen van De Kring, met de klok mee. Zet er bij voorkeur een gedoofde redstone lamp neer, zonder redstone ernaast. |
| `finale_1`, `finale_2`, `kroning` | Startpunten van de finale (binnen 9 blokken van het midden van `finale`) en de plek van de kroning. |

Daarna: `/bc slot <speler> <0-19>` voor elke kop op De Kring, en `/bc uitverkoren <speler>`.
Labels boven de verzamelpunten: `/bc label zet <tekst>`.

`/bc start <ronde>` weigert met één regel en verandert dan niets, als er een regio of punt mist
("ontbreekt: troon, hunter_3"), een startpunt buiten de border van de ronde ligt, een tribunepunt
binnen de vloer ligt, of een kit die de ronde nodig heeft ontbreekt of een fout bevat.

## Commands

Allemaal onder `/bc`, op-level 2.

| Command | Doet |
|---|---|
| `/bc wand` | De regio-wand. |
| `/bc region save\|show\|list\|del <naam>` | Regio uit de selectie opslaan; `show` tekent tien seconden particles op de randen. |
| `/bc point set\|block\|tp\|list\|del <naam>` | Punten zetten, ernaartoe, lijst, weg. |
| `/bc label zet <tekst>` / `/bc label weg` | Zwevende tekst boven je hoofd plaatsen; het dichtstbijzijnde label weghalen. |
| `/bc start <1-6>` | Start een ronde: teleport, border, kits, countdown, poort open, timer. Een lopende ronde wordt eerst afgebroken. |
| `/bc stop` | Breekt de ronde af (of stopt een draaiend rad): timer stil, mobs en border weg, bevriezing eraf, bossbar terug. Rollen blijven. |
| `/bc timer <sec>` | Stelt de resterende tijd bij. |
| `/bc status` | Ronde, timer, finalisten, en per speler rol en vlaggen. |
| `/bc kit <naam> [<speler>]` | Zet een kit op iedereen die meedoet, of op één speler. |
| `/bc poort <naam> open\|dicht` | `doolhof`, `arena`, `bos`. Dicht zet terug wat er stond; weet de mod dat niet meer, dan iron bars. |
| `/bc uitverkoren [<speler>]` | De verborgen rol. Op naam, mag ook voor iemand die nog niet online is. Het antwoord ziet alleen wie het typt. |
| `/bc slot <speler> <0-19>` | De pilaar van een kop. Een pilaar heeft één kop. |
| `/bc rad` | Het Rad. Drie seconden na de landing start ronde 4. |
| `/bc kroon <speler>` | Alleen in ronde 4: forceert een kroonwissel met reset, ook naar iemand op de tribune (geef die daarna een kit, zijn inventory was al leeg). |
| `/bc finalist <1\|2> <speler>` | Noodknop: wijst een finalist aan en zet hem met zijn kroon op de tribune. De finalisten staan alleen in het geheugen, dus dit heb je nodig na een crash of als een finalist niet terugkomt. Niet tijdens ronde 4 of de finale. |
| `/bc kijker <speler> aan\|uit` | Iemand op de tribune zetten, of er weer af halen en mee laten doen. |
| `/bc reset` | Alles terug naar de basiskamp-staat. Weigert niks, ruimt alles op. |

**Staff** is wie in creative of spectator staat: de mod blijft van ze af (geen teleport, geen kit,
ze tellen niet mee). Zet host, camera's en admins dus in creative of spectator vóór `/bc start`.

De scoreboard-tags `king`, `hunter`, `kijker`, `uitverkoren` en `ticket` zijn spiegels die elke
seconde worden bijgezet, zodat je met `@a[tag=...]` kunt kijken. Zelf zetten heeft geen zin; de mod
is de bron van waarheid. Een wave forceren: `/kill @e[tag=horde]`.

## De avond

Elke ronde start de commander met `/bc start <n>`, behalve ronde 4 (`/bc rad`) en ronde 6 (start
vanzelf na de twee minuten rust). Tussen twee rondes in is er geen border en geen PvP.

Gaat er in een ronde iets mis in de mod zelf, dan breekt die ronde zichzelf af met een melding in
de chat en de fout in de console; de server blijft draaien. Start de ronde opnieuw met
`/bc start <n>`.
