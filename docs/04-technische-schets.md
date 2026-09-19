# Technische schets: een server-side Fabric-mod op Minecraft 26.2

Eén eigen mod, `bootcamp`, op een Fabric-server. Server-side only: spelers hebben alleen Simple
Voice Chat nodig, verder een gewone client. De mod doet alles: regio's en punten zetten met een
wand en commands, de rondes, de kroon, het rad, de tribune voor wie dood is, bossbar en visuals.
Voice is puur proximity en gaat buiten de mod om.

De mod is gevibecode met Claude Code en staat in `mod/`. Deze doc was de spec en is na het bouwen
bijgewerkt waar de implementatie afwijkt of concreter is. Hoe je hem bouwt, installeert en instelt
staat in [mod/README.md](../mod/README.md); wat er gebouwd en geverifieerd is, en wat er nog in-game
getest moet worden, in [mod/BOUWLOG.md](../mod/BOUWLOG.md).

> **Versie en status.** Minecraft 26.2; Fabric Loader, Fabric API en de Fabric-versie van Simple
> Voice Chat zijn er voor. De mod compileert tegen de echte 26.2-jar en de kernlogica heeft tests,
> maar **er is nog niets in-game gedraaid**. De eerste test staat in
> [05-draaiboek.md](05-draaiboek.md). Een naam uit 26.2 opzoeken: `./gradlew :fabric:genSources`.

## Stack

| Wat | Waarvoor |
|---|---|
| Fabric-server 26.2 + Fabric API | De server en de event/command-API. |
| `bootcamp`-mod (deze repo, map `mod/`) | Alles wat hieronder staat. |
| Simple Voice Chat (Fabric) | Voice. Puur proximity, de mod doet er niks mee. |
| WorldEdit (Fabric) | Bouwen. Niet voor de spellogica. |

Geen Skript, geen datapack, geen plugins.

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
   `core`-project zodat de kernlogica in de jar komt. De voice-mod staat los van onze mod: als
   jar in `run/mods/` voor de dev-server en in `mods/` op de echte server.
4. Dev-loop: `./gradlew build` maakt de jar in `fabric/build/libs/`; die kopieer je naar `mods/`
   van je eigen testserver en je herstart. `./gradlew runServer` kan ook, voor snel lokaal
   testen. Fixen tijdens het event betekent jar vervangen en herstarten, dus test vooraf.
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

Gamerules die de mod bij het opstarten en bij `/bc reset` zet: `keepInventory` en
`doImmediateRespawn` doen er niet toe (spelers gaan nooit echt dood), `naturalRegeneration` aan,
`pvp` aan (in 26.2 een gamerule; tot ronde 4 houdt team `spelers` PvP tegen),
`doMobSpawning` uit (de horde spawnen we zelf), `mobGriefing` uit (creepers in de ruïne-arena),
`doDaylightCycle` uit, `announceAdvancements` uit, `locatorBar` uit (aan in ronde 4 t/m 6, zie
Visuals). In 26.2 heten de gamerules in code anders (`GameRules.ADVANCE_TIME`, `SPAWN_MOBS`, ...)
en zet je ze via `level.getGameRules().set(...)`.

Gamemode zet `/bc start` per ronde: survival alleen in ronde 3 (minen), adventure in alle andere
rondes; kijkers altijd adventure. Wie inlogt terwijl er geen ronde loopt gaat ook naar adventure.

**Staff is wie in creative of spectator staat.** De mod zet deelnemers zelf in adventure of
survival, dus wie in creative of spectator staat is host, camera of admin: geen teleport, geen
kit, telt niet mee, en gaat gewoon dood als hij doodgaat. Er is geen apart command voor.

Op de server: difficulty niet op peaceful (de horde) en `spawn-protection=0`.

Teams zijn er voor de kleur van naam, Glowing-outline en locator-stip, en voor PvP tot ronde 4:

| Team | Kleur | Friendly fire | Wie |
|---|---|---|---|
| `spelers` | wit | uit | Iedereen in ronde 0 t/m 3: geen PvP. |
| `hunters` | aqua | **aan** | Ronde 4. Hunters kunnen elkaar raken; het team is er voor de kleur. |
| `king` | goud | aan | De koning(en). In de finale raken de twee koningen elkaar gewoon. |
| `out` | grijs | – | Kijkers. |

In ronde 5 gaat iedereen uit zijn team behalve de koningen, dus daar is alles PvP.

## Modules

| Package | Doet | Belangrijkste API |
|---|---|---|
| `config` | Regio's en punten opslaan en laden, JSON in `<wereld>/bootcamp.json`. | Gson, `ServerLifecycleEvents` |
| `kits` | Kits uit JSON-bestanden lezen en op spelers zetten. | `ItemParser` (dezelfde syntax als `/give`) |
| `commands` | Het hele `/bc`-commandboompje. | Brigadier, `CommandRegistrationCallback` |
| `setup` | De wand, `region show` met particles. | `AttackBlockCallback`, `UseBlockCallback` |
| `game` | Spelstatus, timer, de zes rondes als klassen met `start/tick/onDeath/end`. | `ServerTickEvents.END_SERVER_TICK` |
| `crown` | Koning, laatste hit, kroonwissel, opstelling, bevriezing. | `ServerLivingEntityEvents.ALLOW_DEATH`, `ALLOW_DAMAGE` |
| `rad` | Het Rad: lampjes, ritme, landing op de uitverkorene. | tick-gestuurd, geen threads |
| `horde` | Waves spawnen en tellen. | `EntityType.spawn`, entity-tags |
| `tribune` | Wie dood is naar de tribune, daar houden, geen schade, locator bar uit. | `ALLOW_DAMAGE`, tick-check op regio `vloer` en `arena` |
| `visuals` | Bossbar, titles, geluid, particles, vuurwerk, zweefkroon, labels, locator bar. | `ServerBossEvent`, packets, `Display`-entities |

Vuistregel voor het model: alles draait op de server-tick. Geen `Thread.sleep`, geen eigen
threads; een wachttijd is een tick-teller in een state-object.

## Commands

Allemaal onder `/bc`, op-level 2.

| Command | Doet |
|---|---|
| `/bc wand` | Geeft de regio-wand (een stick met een custom data component). |
| `/bc region save\|show\|list\|del <naam>` | Regio uit de wand-selectie opslaan; `show` tekent tien seconden particles op de randen. |
| `/bc point set\|block\|tp\|list\|del <naam>` | Punt op je positie (met kijkrichting) of op het blok waar je naar kijkt (tot 32 blokken). |
| `/bc label zet <tekst>` / `/bc label weg` | Een text display boven je hoofd plaatsen; het dichtstbijzijnde label binnen zes blokken weghalen. |
| `/bc start <ronde>` | Teleport naar het startpunt, border, kits, countdown, poort open, timer. Breekt een lopende ronde eerst af. Weigert met één regel en verandert dan niets als er een regio, punt of kit mist of niet klopt. |
| `/bc kit <naam> [<speler>]` | Zet de kit uit `kits/<naam>.json` op iedereen die meedoet, of op één speler. De start van een ronde doet dit zelf. |
| `/bc stop` / `/bc timer <sec>` | `stop` breekt de ronde af: timer stil, mobs en border weg, bevriezing eraf, bossbar terug. `timer` stelt de resterende tijd bij. |
| `/bc status` | Rollen en vlaggen van alle spelers, huidige ronde en timer. |
| `/bc poort <naam> open\|dicht` | Handmatig een poort bedienen. |
| `/bc kroon <speler>` | Kroonwissel forceren (de ref z'n noodknop). Alleen in ronde 4; mag ook naar iemand op de tribune. Haalt de kroon weg bij iedereen die hem ten onrechte draagt. |
| `/bc finalist <1\|2> <speler>` | Noodknop: wijst een finalist aan en zet hem met zijn kroon op de tribune. De finalisten staan alleen in het geheugen, dus nodig na een crash of als een finalist wegblijft. |
| `/bc uitverkoren [<speler>]` / `/bc slot <speler> <0-19>` | De verborgen rol en de pilaar van elke kop in De Kring. Op naam, dus ook voor wie nog niet online is. Het antwoord ziet alleen wie het typt: het rad blijft geheim. |
| `/bc rad` | Het Rad. Weigert zonder uitverkorene met een pilaar, zonder de twintig lampen, en als ronde 4 daarna niet zou kunnen starten. |
| `/bc kijker <speler> aan\|uit` | Noodknop: iemand met de hand op de tribune zetten of eraf halen. |
| `/bc reset` | Alles terug naar de basiskamp-staat via een register waar elk onderdeel zijn eigen opruimstap in zet: vlaggen, teams, gamemode, gamerules, effecten, attributes, pearl-blokkade, locator-attribute, zweefkroon, sidebar, horde-mobs, inventory, tp, border, bossbar, kijkers weg. Weigert niks, ruimt alles op. |

## Regio's en punten

Eén keer zetten na het bouwen. Alles wordt opgeslagen in `<wereld>/bootcamp.json` en de mod
leest het bij het opstarten. Geen coördinaten in code.

**Regio's** met de wand: linksklik op een blok is hoek 1, rechtsklik hoek 2 (exacte blokposities
uit de events), dan `/bc region save <naam>`. De mod bewaart `min` en `max` en berekent zelf
center en grootte voor de worldborder. Eerst bouwen, dan selecteren: voor de Arena die jullie
zelf bouwen selecteer je na het bouwen `vloer` en `finale` en zet je de tribunepunten. Een regio
opnieuw opslaan overschrijft de oude.

**Voor spelers is een regio een kolom**: alleen x en z tellen, dus twee hoeken op de grond is
genoeg, ook bij een vloer met hoogteverschil. Een poort gebruikt wel de hele doos. De vloer waar
kijkers af moeten blijven (`vloer`, `arena`) telt als de **cirkel die in de selectie past**:
selecteer het vierkant om de ronde vloer heen, dan zijn de hoeken en de ring achter de rand
tribune. Startpunten horen binnen de border-regio van hun ronde te liggen en tribunepunten buiten
de vloer; anders weigert `/bc start`.

| Regio's | Waarvoor |
|---|---|
| `doolhof`, `arena`, `eibos`, `vloer`, `finale` | Worldborder per ronde. `arena` is de horde-arena, `vloer` de vloer van de Arena (ronde 4 en 5), `finale` het midden (ronde 6). |
| `doolhof_uit` | Het vak achter de uitgang van het doolhof: wie erin staat is eruit en gaat naar `v2`. |
| `poort_doolhof`, `poort_arena`, `poort_bos` | *Optioneel.* De muur die open en dicht gaat. Open is lucht; dicht zet terug wat er stond, en alleen als de mod dat niet meer weet (herstart met open poort) iron bars. |
| `colosseum` | *Optioneel.* De hele Arena inclusief tribunes. Bestaat hij, dan is dit de border van ronde 4 en 5 (zoals [01-map-en-flow.md](01-map-en-flow.md) zegt); anders `vloer`. |
| `eiplaat` | Het vak bij de uitgang van het bos waar je ticket wordt ingenomen. |

**Punten** met `/bc point set <naam>` (positie plus kijkrichting) of `/bc point block <naam>`
(het blok waar je naar kijkt).

| Punten | Waarvoor |
|---|---|
| `basiskamp`, `v2`, `v3`, `kring` | Verzamelpunten. |
| `doolhof_start` | Ingang van het doolhof. |
| `mob_1` t/m `mob_4`, `arena_spawn` | Horde-spawns en waar spelers de arena binnenkomen. |
| `ei_start`, `ei_beacon` (blok) | Bosrand-ingang en het ontbrekende blok in de beaconpiramide onder het Ei. |
| `troon`, `hunter_1` t/m `hunter_4` | Het midden van de Arena en de startpunten aan de rand van de vloer; bij elke kroonwissel gaat iedereen hierheen terug. |
| `tribune_1` t/m `tribune_4`, `tribune_horde_1` en `tribune_horde_2` | Waar doden neerkomen: op de tribune van de Arena (verdeeld over de ringen) en op die van de ruïne-arena. |
| `finale_1`, `finale_2`, `kroning` | Startpunten van de finale en de plek van de kroning, in het midden van de Arena. |
| `lamp_0` t/m `lamp_19` (blokken) | De lichtblokken van De Kring: de 20 pilaren rond de arenavloer, met de klok mee. Aan is een brandende redstone lamp, uit is wat er stond; zet er dus een gedoofde redstone lamp neer, zonder redstone ernaast. |

Worldborder per ronde: `ServerLevel.getWorldBorder()`, center en grootte uit de regio, krimpen met
`lerpSizeBetween`. Altijd eerst teleporteren, dan de border zetten.

## Kits (JSON)

Elke kit is een JSON-bestand in `config/bootcamp/kits/`, zodat iedereen exact hetzelfde aanheeft
als een ronde begint en je de inhoud kunt aanpassen zonder te compileren. De basiskit komt van
Pudding als JSON; de andere kits vul je op dezelfde manier in met de inhoud uit
[02-rondes.md](02-rondes.md).

| Bestand | Wanneer |
|---|---|
| `basis.json` | Wie zonder ticket uit het Ei-bos komt; hunters zonder loot in ronde 4. Inhoud volgt van Pudding. |
| `horde.json` | Start ronde 2, iedereen. |
| `ei.json` | Start ronde 3: alleen de pickaxe erbij, inventory blijft. |
| `boss.json` | Clown bij de start van ronde 4. |
| `kroonpakket.json` | Bij elke kroonwissel erbij: 2 gapples, 2 pearls. |
| `arena.json` | Start ronde 5, iedereen. |
| `finale.json` | Elk potje van ronde 6, beide finalisten. |

Formaat: per slot een item in dezelfde syntax als `/give`, zodat enchantments en andere
components gewoon werken. De mod parst dat met de vanilla item-parser.

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
    "minecraft:bow[minecraft:enchantments={\"minecraft:power\":1}]",
    "minecraft:arrow 16",
    "minecraft:cooked_beef 8"
  ],
  "inventory": []
}
```

Een kit schrijft nooit over de head-slot van een koning of finalist: de kroon blijft op.
`boss.json` en `finale.json` hebben daarom geen helm.

`clear: false` voor kits die iets toevoegen (`ei`, `kroonpakket`, en ook `horde`: wat je in het
doolhof vond mag je houden). Een item komt dan op zijn slot als dat leeg is en anders ergens in de
inventory. De mod herkent de kroon aan het item zelf (`custom_data={bootcamp_kroon:1b}`): zit hij
in de head-slot, dan blijft elke kit ervan af. De bestanden worden bij elk gebruik opnieuw
gelezen, en een ronde weigert te starten als een kit die ze nodig heeft ontbreekt of een fout
bevat. De waves van de horde staan op dezelfde manier in `config/bootcamp/waves.json`. Een getal achter het item is
het aantal. `/bc kit <naam>` zet hem op iedereen die meedoet, `/bc kit <naam> <speler>` op één
speler; de rondes roepen hetzelfde aan bij de start. Een fout in een bestand komt als één
duidelijke regel in de console met bestandsnaam en slot, niet als een crash.

## Spellogica

**Status.** Eén `GameState`: huidige ronde, timer in seconden, vlaggen (`bevroren`),
per speler een rol (`SPELER`, `HUNTER`, `KING`, `FFA`, `FINALIST`, `KIJKER`, `STAFF`) en
vlaggen (`dood`, `ticket`, `uitverkoren`, slotnummer). De mod is de bron van waarheid;
scoreboard-tags (`king`, `hunter`, `kijker`, `uitverkoren`, `ticket`) zijn read-only spiegels die
elke seconde worden bijgezet, zodat je met `@a[tag=...]` kunt kijken. `/bc status` toont alles.

**Ontbrekende config.** Elke ronde declareert welke regio's en punten ze nodig heeft. `/bc start`
weigert met één regel ("ontbreekt: troon, hunter_3, ...") als er iets mist en verandert dan
niets. `/bc rad` weigert zonder precies één uitverkorene.

**Uitloggen en terugkomen.** Join en quit gaan naar de actieve ronde. Een hunter die uitlogt telt
als dood. Logt de koning uit, dan telt de mod 30 seconden af (bossbar); komt hij niet terug, dan
volgt dezelfde kroonwissel als bij een val-dood. Wie terugkomt in ronde 4 t/m 6 wordt kijker op
de tribune; in ronde 1 t/m 3 gaat hij naar het verzamelpunt of de tribune van dat moment.

**Tussen twee rondes in** is er geen border en geen PvP (iedereen in team `spelers`). Wie dan
inlogt krijgt alsnog wat het einde van de gemiste ronde met iedereen deed: adventure, een
doorgegeven kroon eraf, na ronde 3 zonder ticket de basiskit-straf, naar het verzamelpunt, of na
ronde 4 t/m 6 als kijker de tribune op.

**Fouten.** Een fout in de tick van een ronde stopt de server niet: de mod logt hem, breekt de
ronde af en meldt het in de chat.

**Tick.** `END_SERVER_TICK`; elke 20 ticks één seconde: timer omlaag, bossbar bijwerken, de
actieve ronde z'n `tick()`. Ronde-specifieke momenten (Ei-hint op 5:00, einde op 0:00, "hunters
op" in ronde 4) zitten in die ronde.

**Dood.** `ServerLivingEntityEvents.ALLOW_DEATH`: de mod laat spelers nooit echt doodgaan. Bij
een dodelijke klap wordt de dood geannuleerd, de speler geheald en afgehandeld volgens de ronde:

| Ronde | Wat er gebeurt |
|---|---|
| 2 | Kijker tot het einde van de ronde. Doodtekst als title voor de dode, verder niks. Inventory wordt bewaard en bij `v3` teruggegeven. |
| 4, koning | Kroonwissel naar de killer; anders de laatste hit; anders een willekeurige levende hunter. Ex-koning wordt kijker op de tribune. |
| 4, hunter | Kijker op de tribune, uit de ronde, geen respawn. Is er geen levende hunter meer, dan eindigt de ronde en is de koning finalist 1. |
| 5 | Kijker op de tribune; laatste over wordt finalist 2. |
| 6 | Potje voor de tegenstander. |

Geen death-screen, geen respawn, geen keepInventory-gedoe. De laatste hit komt uit
`ALLOW_DAMAGE`: is het slachtoffer de koning en de bron een speler (ook via een pijl), onthoud hem.

**Kroonwissel** (`Crown.transfer(oude, nieuwe)`): oude wordt kijker op de tribune; nieuwe naar
`troon`,
heal, honger vol, alle items in inventory en armor op volle durability (`setDamageValue(0)`),
gouden helm met Curse of Binding, 2 gapples en 2 pearls, 15 seconden Resistance II, Glowing
(teamkleur goud), zweefkroon; dan `Opstelling(10)`.

**Opstelling(seconden)**: alle levende hunters heal en naar `hunter_1..4` aan de rand van de
vloer, bevriezen, countdown in actionbar met de laatste vijf seconden als title plus pling, dan
los met een groene GO en de raid horn. Bevriezen is `MOVEMENT_SPEED` en `JUMP_STRENGTH` op
basiswaarde 0 (terug naar 0.1 en 0.42) plus een `UseItemCallback` die pearls blokkeert zolang de
vlag staat (ook wind charges en chorus fruit; pearls die al in de lucht hangen worden opgeruimd).
Bij de start van ronde 4 dezelfde functie met 30 seconden, waarin alleen de hunters bevroren
staan; na een wisel staat ook de koning stil. Zolang een opstelling loopt doet niemand elkaar
schade. Doden blijven dood: een reset geeft geen levens terug.

**Einde van ronde 4**: bij timer 0, of zodra er geen levende hunter meer is (elke seconde
gecontroleerd). De koning is finalist 1; is hij op dat moment uitgelogd, dan gaat de kroon eerst
door. De commander start daarna zelf ronde 5 (besluit 14); alleen de finale start vanzelf. Geen sudden death en geen border-krimp: de vloer is klein genoeg en iedereen heeft
één leven. Daarna `start(5)`: iedereen behalve Clown en finalist 1 de vloer op, ook de doden van
ronde 4.

**Ei-drukplaat**: elke halve seconde: spelers in regio `eiplaat` zonder ticket met een diamond
block in hun inventory, block eruit, ticket, tp naar `kring`, levelup-geluid. Na de timer: wie
geen ticket heeft krijgt inventory leeg plus basiskit en gaat alsnog naar De Kring.

**Horde**: mobs spawnen op `mob_1..4` met een entity-tag `horde` en `setPersistenceRequired()`
zodat ze niet despawnen; gear via `setItemSlot`. Elke mob zonder helm krijgt een stenen knoop op
zijn hoofd (anders branden zombies en skeletons overdag weg in de open arena) en volgbereik 64.
Aantallen schalen mee met het aantal spelers; de boss wave staat vast. Volgende wave als de teller 0 is of na 120
seconden. Ronde stopt bij wave 5 dood, iedereen dood, of de timer; de doden worden weer levend
bij `v3` en overlevers krijgen een pearl.

**Het Rad**: state-object met `pos`, `rest` en `volgendeStapTick`. Start: `rest` =
`(doelslot - pos + 20) mod 20 + 20 * (2 of 3)`, `pos` willekeurig. Per stap: lamp uit, pos + 1,
lamp aan, `rest` - 1, hat-geluid, en de wachttijd tot de volgende stap loopt op van 2 naar 30
ticks naarmate `rest` kleiner wordt. Bij 0: dragon growl, totem-particles op de uitverkorene,
title `DE KONING` met naam, drie seconden later `start(4)`.

**FFA en finale**: levende `FFA`-spelers tellen; bij één over kroon en finalist-visual, dan
twee minuten rust (beide finalisten op de tribune, timer in de bossbar) en `start(6)`. Finale:
potjes tellen, heal en kit-reset per potje, na twee gewonnen potjes de kroning in het midden.

## Kijkers: wie dood of klaar is

Geen spectator mode, geen tp-items, geen vliegen. Wie dood is wordt naar de tribune
geteleporteerd en blijft daar bij de andere doden tot de ronde voorbij is; wie klaar is met een
ronde staat bij het volgende verzamelpunt. De mod hoeft maar weinig te doen:

- Adventure mode, team `out` (grijs in de tab-list), inventory leeg (in ronde 2 bewaard en bij
  `v3` teruggegeven).
- Geen schade (`ALLOW_DAMAGE` annuleren voor kijkers), ook niet van de border als die in de FFA of
  de finale krimpt. Een kijker ziet de border ook niet: hij krijgt een eigen border-pakket zo
  groot als de wereld, want wie buiten de border staat krijgt van de client anders een volledig
  rood scherm, en dat zou tijdens de finale op de hele tribune tegelijk zijn.
- Blijft op zijn plek: glas tussen tribune en vloer, en een tick-check die een kijker die toch in
  regio `vloer` (Arena) of `arena` (horde) komt terug op zijn tribunepunt zet.
- Niet op de locator bar (`WAYPOINT_TRANSMIT_RANGE` op 0), geen Glowing.
- Bij de dood een title met een willekeurige doodtekst, alleen voor de dode zelf, geen chatregel
  en geen geluid: `Grote L gepakt!`, `Had je nou maar beter je best gedaan`, `Gelukkig is dit niet
  de CSMP`. De lijst staat in `bootcamp.json`, zodat je er meer bij kunt zetten.
- Zichtbaar en hoorbaar: op de tribune zijn de doden het publiek. Voice is gewoon proximity, dus
  de vloer hoort de tribune en andersom.

Waar kijkers heen gaan: ronde 2 naar `tribune_horde_n`, ronde 4 t/m 6 naar `tribune_n`. Finalist
1 en Clown zitten tijdens de FFA ook op de tribune, met dezelfde regels. Aan het eind van ronde 2
worden de doden weer gewoon speler bij verzamelpunt 3.

**Staff** (host, camera's, admins) gebruikt spectator of creative voor de camera; de mod dwingt
daar niks af. `/bc kijker <naam> aan|uit` is de noodknop om iemand met de hand op de tribune te
zetten of eraf te halen.

## Voice

Niks te doen in de mod. Simple Voice Chat draait ernaast op puur proximity, groepen staan in de
voice-config uit, en de tribune is publiek: de vloer hoort de doden en andersom. Details in
[07-voice.md](07-voice.md).

## Bossbar en visuals

Eén bossbar, kort, altijd hetzelfde formaat (`ServerBossEvent`, alle spelers toegevoegd).
Persoonlijke info via de actionbar. De sidebar toont in ronde 2 de sneuvelvolgorde en in ronde 4
de regeerperiodes.

| Ronde | Tekst | Kleur | Vulling |
|---|---|---|---|
| Basiskamp | `Pudding Bootcamp` | wit | vol |
| 1 | `Doolhof · 09:41` | groen | tijd |
| 2 | `Wave 3 · 12 mobs` | rood | mobs over |
| 3 | `Het Ei · 07:12` | groen, geel na de hint | tijd |
| 4 | `Koning: Clown · 12:34` | geel | tijd |
| Rust | `Finale over 01:59` | wit | tijd |
| 5 | `FFA · 7 over · 04:59` | paars | tijd |
| 6 | `Finale · 1 - 0` | geel | vol |

**Locator bar**: gamerule uit in alle rondes, aan in ronde 4 t/m 6 met alleen de koningen
zichtbaar (hunters en kijkers zenden niet, attribute `WAYPOINT_TRANSMIT_RANGE` op 0). De stip
heeft de teamkleur, dus goud.

**Zweefkroon**: een `Display.ItemDisplay` met een gouden helm, schaal 0.5, `teleport_duration`
2, elke twee ticks boven het hoofd van de koning gezet en zes graden gedraaid. Wordt opgeruimd
als de koning kijker wordt. Twee koningen in de finale hebben elk hun eigen.

**Labels**: een `Display.TextDisplay` boven elk verzamelpunt en boven De Kring, één keer
geplaatst bij het bouwen met `/bc label zet <tekst>`. Ze overleven `/bc reset`.

**Per moment**

| Moment | Wat je ziet en hoort |
|---|---|
| Countdown | Titles 5 t/m 1 met een stijgende `note_block.pling`, dan `GO` met `event.raid.horn`. |
| Poort open | `event.raid.horn` en cloud-particles in de poortopening. |
| Nieuwe wave | Title `WAVE 3` in rood, `event.raid.horn`, bossbar rood met mob-teller. |
| Speler sneuvelt | Alleen de dode ziet groot een willekeurige doodtekst als title: `Grote L gepakt!`, `Had je nou maar beter je best gedaan`, `Gelukkig is dit niet de CSMP`. Geen geluid, geen chatregel. Lijst in de config. |
| Ei-hint op 5 min | Het ontbrekende blok in de beaconpiramide erin: lichtstraal aan, `block.beacon.activate` voor iedereen, bossbar geel. Op 3 min een vuurpijl boven het Ei. |
| Ticket ingeleverd | `entity.player.levelup` voor de speler, happy-villager-particles. |
| Het Rad | Lampjes rond met `note_block.hat` per stap. Aan het eind `entity.ender_dragon.growl`, totem-particles op de uitverkorene, title `DE KONING` met naam. |
| Kroonwissel | `entity.lightning_bolt.thunder` voor iedereen (geen echte bliksem, die zet dingen in de fik), flash-particle op de nieuwe koning, title `NIEUWE KONING` met naam, de zweefkroon springt over. |
| Hunters op | Title `DE KONING STAAT` met naam, `ui.toast.challenge_complete`; ronde 4 is voorbij. |
| Finalist | `ui.toast.challenge_complete` voor iedereen, vuurpijl boven de speler, title `FINALIST` met naam. |
| Kroning | Twintig seconden vuurpijlen boven de Arena, title `KING OF THE SMP` met naam, tribunes vol. |

Vuurpijlen: een `FireworkRocketEntity` met een `Fireworks`-component (grote gouden bol met
staart, vluchtduur 1). Titles en actionbar gaan via de title-packets, geluid via
`playNotifySound`, particles via `ServerLevel.sendParticles`.

## Zo is dit gevibecode

1. **Volgorde.** Config en commands met de wand, dan de tribune (kijkers), dan ronde 1
   t/m 3, dan de kroon en ronde 4, dan het rad, dan visuals. Na elke stap iets
   testbaars, met een tweede account op de dev-server.
2. **Context.** Geef Claude Code deze repo. Deze doc plus [02-rondes.md](02-rondes.md) en
   [03-kroon-regels.md](03-kroon-regels.md) zijn de spec; laat hem één module per keer doen.
3. **Compileren.** `./gradlew build` groen voordat je verder gaat. Een naam die niet bestaat is
   een 1.21-gok: `./gradlew genSources` en de echte naam opzoeken in de gegenereerde bronnen.
4. **Geen magie.** Alles tick-gestuurd, alles via `/bc reset` terug te draaien, alle
   coördinaten uit `bootcamp.json`. Als iets een wachttijd nodig heeft, is dat een teller.
5. **Testrun** met de checklist uit [05-draaiboek.md](05-draaiboek.md), en de jar van de vorige
   werkende versie bewaren voor het geval een fix misgaat.

## Oude versies

De Paper + Skript-schets staat in de git-geschiedenis (commit `d351fd1`), de datapack-versie in
`e51e143`. Allebei bruikbaar als je toch geen mod wilt bouwen, met de omwegen die daar
beschreven staan.
