# Technische schets: een server-side Fabric-mod op Minecraft 26.2

Eén eigen mod, `bootcamp`, op een Fabric-server. Server-side only: spelers hebben alleen Simple
Voice Chat nodig, verder een gewone client. De mod doet alles: regio's en punten zetten met een
wand en commands, de rondes, de kroon, het rad, de kijkersmodus met tp-items, voice-filtering via
de API van de voice-mod, bossbar en visuals.

De mod wordt gevibecode: Claude Code schrijft de Java, jij compileert, test en plakt fouten
terug. Deze doc is de spec die je hem geeft.

> **Versie.** Doel is Minecraft 26.2. Check vóór je begint of Fabric Loader, Fabric API en de
> Fabric-versie van Simple Voice Chat er al zijn voor 26.2 (Fabric is er meestal binnen dagen).
> De interne namen van 26.2 zijn deels nieuw voor het model, dus reken op compile-fix-rondjes:
> laat Loom de bronnen genereren (`./gradlew genSources`) zodat je de echte namen kunt opzoeken
> als een gok niet compileert. Niets hieronder is getest.

## Stack

| Wat | Waarvoor |
|---|---|
| Fabric-server 26.2 + Fabric API | De server en de event/command-API. |
| `bootcamp`-mod (deze repo, map `mod/`) | Alles wat hieronder staat. |
| Simple Voice Chat (Fabric) + `voicechat-api` | Voice. De API is een gewone Java-dependency, versie-onafhankelijk. |
| WorldEdit (Fabric) | Bouwen. Niet voor de spellogica. |

Geen Skript, geen datapack, geen plugins. Dialogs (schermpjes met knoppen) bouwt de mod in code.

## Project opzetten

1. Genereer een leeg project met de Fabric-template (fabricmc.net/develop/template): versie
   26.2, **Mojang mappings** (die namen zijn stabiel tussen versies en het model kent ze het
   best), de Java-versie die de template vraagt.
2. `fabric.mod.json`: `"environment": "server"`, entrypoints `main` (mod-init) en `voicechat`
   (de voice-plugin, zie Voice).
3. `build.gradle`: naast `fabric-api` de dependency `de.maxhenkel.voicechat:voicechat-api` uit
   de Maven-repo `https://maven.maxhenkel.de/repository/public`. De voice-mod zelf zet je als
   jar in `run/mods/` voor de dev-server en in `mods/` op de echte server.
4. Dev-loop: `./gradlew runServer` start een testserver; `./gradlew build` maakt de jar in
   `build/libs/`. Fixen tijdens het event betekent jar vervangen en herstarten, dus test vooraf.
5. Zet de mod in deze repo onder `mod/`, dan heeft Claude Code de docs en de code bij elkaar.

## Modules

| Package | Doet | Belangrijkste API |
|---|---|---|
| `config` | Regio's en punten opslaan en laden, JSON in `<wereld>/bootcamp.json`. | Gson, `ServerLifecycleEvents` |
| `commands` | Het hele `/bc`-commandboompje. | Brigadier, `CommandRegistrationCallback` |
| `setup` | De wand, `region show` met particles. | `AttackBlockCallback`, `UseBlockCallback` |
| `game` | Spelstatus, timer, de zes rondes als klassen met `start/tick/onDeath/end`. | `ServerTickEvents.END_SERVER_TICK` |
| `crown` | Koning, laatste hit, kroonwissel, opstelling, bevriezing. | `ServerLivingEntityEvents.ALLOW_DEATH`, `ALLOW_DAMAGE` |
| `rad` | Het Rad: lampjes, ritme, landing op de uitverkorene. | tick-gestuurd, geen threads |
| `horde` | Waves spawnen en tellen. | `EntityType.spawn`, entity-tags |
| `spectate` | Kijkersmodus, de twee tp-items, tp-dialog. | mixins (zie Kijkers), `ServerPlayer.openDialog` |
| `voice` | Voice-plugin: wie hoort wie. | `VoicechatPlugin`, `SoundPacketEvent` |
| `visuals` | Bossbar, titles, geluid, particles, vuurwerk, zweefkroon, labels, locator bar. | `ServerBossEvent`, packets, `Display`-entities |

Vuistregel voor het model: alles draait op de server-tick. Geen `Thread.sleep`, geen eigen
threads; een wachttijd is een tick-teller in een state-object.

## Commands

Allemaal onder `/bc`, op-level 2, behalve `/bc tp` (ook voor kijkers).

| Command | Doet |
|---|---|
| `/bc wand` | Geeft de regio-wand (een stick met een custom data component). |
| `/bc region save\|show\|list\|del <naam>` | Regio uit de wand-selectie opslaan; `show` tekent tien seconden particles op de randen. |
| `/bc point set\|block\|tp\|list\|del <naam>` | Punt op je positie (met kijkrichting) of op het blok waar je naar kijkt. |
| `/bc start <ronde>` | Teleport naar het verzamelpunt, border, kits, countdown, poort open, timer. |
| `/bc stop` / `/bc timer <sec>` | Timer stil, of resterende tijd bijstellen. |
| `/bc poort <naam> open\|dicht` | Handmatig een poort bedienen. |
| `/bc kroon <speler>` | Kroonwissel forceren (de ref z'n noodknop). |
| `/bc uitverkoren <speler>` / `/bc slot <speler> <0-19>` | De verborgen rol en de pilaar van elke kop in De Kring. |
| `/bc rad` | Het Rad. |
| `/bc kijker <speler> aan\|uit` | Kijkersmodus aan of uit, ook voor staff. |
| `/bc tools` | De twee tp-items in je hotbar. |
| `/bc tp <speler>` | Naar een speler; alleen voor kijkers en staff. Dit zit achter de knoppen. |
| `/bc reset` | Alles terug naar de basiskamp-staat: vlaggen, teams, gamemode, attributes, inventory, tp, border, bossbar, kijkersmodus uit. |

## Regio's en punten

Eén keer zetten na het bouwen. Alles wordt opgeslagen in `<wereld>/bootcamp.json` en de mod
leest het bij het opstarten. Geen coördinaten in code.

**Regio's** met de wand: linksklik op een blok is hoek 1, rechtsklik hoek 2 (exacte blokposities
uit de events), dan `/bc region save <naam>`. De mod bewaart `min` en `max` en berekent zelf
center en grootte voor de worldborder.

| Regio's | Waarvoor |
|---|---|
| `doolhof`, `arena`, `eibos`, `king`, `binnenplaats`, `troonzaal` | Worldborder per ronde. |
| `poort_doolhof`, `poort_arena`, `poort_bos` | De muur die open en dicht gaat (`fill` met lucht of iron bars). |
| `eiplaat` | Het vak bij de uitgang van het bos waar je ticket wordt ingenomen. |

**Punten** met `/bc point set <naam>` (positie plus kijkrichting) of `/bc point block <naam>`
(het blok waar je naar kijkt).

| Punten | Waarvoor |
|---|---|
| `basiskamp`, `v2`, `v3`, `kring` | Verzamelpunten. |
| `doolhof_start`, `doolhof_uit` | Ingang en waar je uitkomt. |
| `mob_1` t/m `mob_4`, `arena_spawn` | Horde-spawns en waar spelers de arena binnenkomen. |
| `ei_start`, `ei_beacon` (blok) | Bosrand-ingang en het ontbrekende blok in de beaconpiramide onder het Ei. |
| `burcht`, `hunter_1` t/m `hunter_4` | Startpunten ronde 4; bij elke kroonwissel gaat iedereen hierheen terug. |
| `sd_1` t/m `sd_4` | Startpunten voor een reset tijdens sudden death, binnen de 60 x 60. |
| `ffa_midden`, `finale_1`, `finale_2`, `kroning` | Binnenplaats en troonzaal. |
| `lamp_0` t/m `lamp_19` (blokken) | De lichtblokken van De Kring, met de klok mee. |

Worldborder per ronde: `ServerLevel.getWorldBorder()`, center en grootte uit de regio, krimpen met
`lerpSizeBetween`. Altijd eerst teleporteren, dan de border zetten.

## Spellogica

**Status.** Eén `GameState`: huidige ronde, timer in seconden, vlaggen (`sudden`, `bevroren`),
per speler een rol (`SPELER`, `HUNTER`, `KING`, `FFA`, `FINALIST`, `KIJKER`, `STAFF`) en
vlaggen (`dood`, `ticket`, `uitverkoren`, slotnummer). Rollen staan ook als scoreboard-tag op de
speler, zodat je ze met `@a[tag=...]` in de console kunt zien.

**Tick.** `END_SERVER_TICK`; elke 20 ticks één seconde: timer omlaag, bossbar bijwerken, de
actieve ronde z'n `tick()`. Ronde-specifieke momenten (Ei-hint op 5:00, sudden death op 3:00,
einde op 0:00) zitten in die ronde.

**Dood.** `ServerLivingEntityEvents.ALLOW_DEATH`: de mod laat spelers nooit echt doodgaan. Bij
een dodelijke klap wordt de dood geannuleerd, de speler geheald en afgehandeld volgens de ronde:

| Ronde | Wat er gebeurt |
|---|---|
| 2 | Kijker tot het einde van de ronde. Bericht in de chat. |
| 4, koning | Kroonwissel naar de killer; anders de laatste hit; anders een willekeurige hunter. Ex-koning wordt kijker. |
| 4, hunter | 20 seconden kijker op de plek van de dood (zonder tp-items), dan terug naar een randpunt met 5 seconden Resistance. Vervalt bij een reset. |
| 4, sudden death | Kijker, uitgeschakeld. |
| 5 | Kijker, uitgeschakeld; laatste over wordt finalist 2. |
| 6 | Potje voor de tegenstander. |

Geen death-screen, geen respawn, geen keepInventory-gedoe. De laatste hit komt uit
`ALLOW_DAMAGE`: is het slachtoffer de koning en de bron een speler (ook via een pijl), onthoud hem.

**Kroonwissel** (`Crown.transfer(oude, nieuwe)`): oude wordt kijker; nieuwe naar `burcht`,
heal, honger vol, alle items in inventory en armor op volle durability (`setDamageValue(0)`),
gouden helm met Curse of Binding, 2 gapples en 2 pearls, 15 seconden Resistance II, Glowing
(teamkleur goud), zweefkroon; dan `Opstelling(10)`.

**Opstelling(seconden)**: alle hunters heal en naar `hunter_1..4` (of `sd_1..4` tijdens sudden
death, want de randpunten liggen dan buiten de border), bevriezen, countdown in actionbar met de
laatste vijf seconden als title plus pling, dan los met een groene GO en de raid horn. Bevriezen
is `MOVEMENT_SPEED` en `JUMP_STRENGTH` op basiswaarde 0 (terug naar 0.1 en 0.42) plus een
`UseItemCallback` die pearls blokkeert zolang de vlag staat. Bij de start van ronde 4 dezelfde
functie met 30 seconden.

**Sudden death**: vlag aan, border in 180 seconden naar 60, visuals, geen respawns meer.

**Ei-drukplaat**: elke halve seconde: spelers in regio `eiplaat` zonder ticket met een diamond
block in hun inventory, block eruit, ticket, tp naar `kring`, levelup-geluid. Na de timer: wie
geen ticket heeft krijgt inventory leeg plus basiskit en gaat alsnog naar De Kring.

**Horde**: mobs spawnen op `mob_1..4` met een entity-tag `horde` en `setPersistenceRequired()`
zodat ze niet despawnen; gear via `setItemSlot`. Volgende wave als de teller 0 is of na 120
seconden. Ronde stopt bij wave 5 dood, iedereen dood, of de timer; de doden worden weer levend
bij `v3` en overlevers krijgen een pearl.

**Het Rad**: state-object met `pos`, `rest` en `volgendeStapTick`. Start: `rest` =
`(doelslot - pos + 20) mod 20 + 20 * (2 of 3)`, `pos` willekeurig. Per stap: lamp uit, pos + 1,
lamp aan, `rest` - 1, hat-geluid, en de wachttijd tot de volgende stap loopt op van 2 naar 30
ticks naarmate `rest` kleiner wordt. Bij 0: dragon growl, totem-particles op de uitverkorene,
title `DE KONING` met naam, drie seconden later `start(4)`.

**FFA en finale**: levende `FFA`-spelers tellen; bij één over kroon, finalist-visual, vijf
seconden later `start(6)`. Finale: potjes tellen, heal en kit-reset per potje, na twee gewonnen
potjes de kroning.

## Kijkers: doden, host en camera

Doden gaan niet in spectator mode (daar kun je geen items in vasthouden) maar in **kijkersmodus**,
die de mod zelf maakt:

- Adventure mode, mag vliegen en vliegt, onzichtbaar (effect zonder particles), team `out` (grijs
  in de tab-list), hotbar leeg op de twee tp-items na.
- Onaantastbaar: geen schade (`ALLOW_DAMAGE` annuleren), pijlen en klappen gaan door je heen
  (mixin op `Player`: `canBeHitByProjectile` en `isAttackable` geven `false` voor kijkers), geen
  botsing (mixin op `isPushable`/`canCollideWith`), niks oppakken (mixin op `ItemEntity`), niks
  aanraken of gebruiken (`UseBlockCallback`, `UseItemCallback`, `AttackEntityCallback` geven
  `FAIL`, behalve voor de tp-items).
- Niet op de locator bar (`WAYPOINT_TRANSMIT_RANGE` op 0), geen naamplaatje (onzichtbaar).
- Grens: door muren vliegen kan niet, dat is client-side. Eroverheen wel. En kijkers komen niet
  door de border; wie erbuiten zweeft als hij krimpt, kijkt van buiten mee.

**De twee items:** een kompas **Levenden** en een spelerskop **Doden**, herkenbaar aan een custom
data component. Rechtsklik opent een dialog (`ServerPlayer.openDialog`, type multi-action,
drie kolommen) met een knop per speler uit die lijst; elke knop draait `/bc tp <naam>`. De lijst
wordt bij elke klik opnieuw gebouwd. Levenden zijn alle spelers met een rol die niet kijker is,
doden zijn de kijkers met rol `SPELER` (staff staat er niet tussen).

**Staff** (host, camera's, admins): `/bc kijker <naam> aan` geeft dezelfde modus, of blijf in
creative en pak alleen de items met `/bc tools`. Echte spectator mode kan ook nog steeds, alleen
zonder items.

**Respawn-wachttijd** in ronde 4 is dezelfde modus voor 20 seconden, zonder items, op de plek van
de dood.

## Voice via de API

De mod is ook een voice-plugin (`VoicechatPlugin`, entrypoint `voicechat`). Drie regels code
doen wat we willen, zonder groepen, commands of knoppen. Details in [07-voice.md](07-voice.md).

- `SoundPacketEvent`: zender is kijker en ontvanger is levend, dan annuleren. Levenden horen
  doden nooit.
- Doden horen elkaar overal: bij het opstarten maakt de mod een persistente, verborgen groep;
  wie kijker wordt gaat erin (`connection.setGroup`), wie weer levend wordt eruit. Levenden zitten
  nooit in een groep, dus alles is proximity.
- `CreateGroupEvent` en `JoinGroupEvent`: annuleren voor spelers zonder staff-rol. Eigen groepen
  maken kan dus gewoon niet.

## Bossbar en visuals

Eén bossbar, kort, altijd hetzelfde formaat (`ServerBossEvent`, alle spelers toegevoegd).
Persoonlijke info via de actionbar, de regeerperiodes via de sidebar.

| Ronde | Tekst | Kleur | Vulling |
|---|---|---|---|
| Basiskamp | `Pudding Bootcamp` | wit | vol |
| 1 | `Doolhof · 09:41` | groen | tijd |
| 2 | `Wave 3 · 12 mobs` | rood | mobs over |
| 3 | `Het Ei · 07:12` | groen, geel na de hint | tijd |
| 4 | `Koning: Clown · 12:34` | geel | tijd |
| 4, sudden death | `SUDDEN DEATH · 02:59` | rood | tijd |
| 5 | `FFA · 7 over · 04:59` | paars | tijd |
| 6 | `Finale · 1 - 0` | geel | vol |

**Locator bar**: gamerule uit in alle rondes, aan in ronde 4 t/m 6 met alleen de koningen
zichtbaar (hunters en kijkers zenden niet, attribute `WAYPOINT_TRANSMIT_RANGE` op 0). De stip
heeft de teamkleur, dus goud.

**Zweefkroon**: een `Display.ItemDisplay` met een gouden helm, schaal 0.5, `teleport_duration`
2, elke twee ticks boven het hoofd van de koning gezet en zes graden gedraaid. Wordt opgeruimd
als de koning kijker wordt. Twee koningen in de finale hebben elk hun eigen.

**Labels**: een `Display.TextDisplay` boven elk verzamelpunt en boven De Kring, één keer
geplaatst bij het bouwen.

**Per moment**

| Moment | Wat je ziet en hoort |
|---|---|
| Countdown | Titles 5 t/m 1 met een stijgende `note_block.pling`, dan `GO` met `event.raid.horn`. |
| Poort open | `event.raid.horn` en cloud-particles in de poortopening. |
| Nieuwe wave | Title `WAVE 3` in rood, `event.raid.horn`, bossbar rood met mob-teller. |
| Speler sneuvelt (ronde 2) | Grijze regel in de chat, geen geluid. |
| Ei-hint op 5 min | Het ontbrekende blok in de beaconpiramide erin: lichtstraal aan, `block.beacon.activate` voor iedereen, bossbar geel. Op 3 min een vuurpijl boven het Ei. |
| Ticket ingeleverd | `entity.player.levelup` voor de speler, happy-villager-particles. |
| Het Rad | Lampjes rond met `note_block.hat` per stap. Aan het eind `entity.ender_dragon.growl`, totem-particles op de uitverkorene, title `DE KONING` met naam. |
| Kroonwissel | `entity.lightning_bolt.thunder` voor iedereen (geen echte bliksem, die zet dingen in de fik), flash-particle op de nieuwe koning, title `NIEUWE KONING` met naam, de zweefkroon springt over. |
| Sudden death | Title `SUDDEN DEATH` in rood, `entity.wither.spawn`, bossbar rood, border-warning op 15 zodat de rand rood kleurt. |
| Finalist | `ui.toast.challenge_complete` voor iedereen, vuurpijl boven de speler, title `FINALIST` met naam. |
| Kroning | Twintig seconden vuurpijlen boven de troonzaal, title `KING OF THE SMP` met naam. |

Vuurpijlen: een `FireworkRocketEntity` met een `Fireworks`-component (grote gouden bol met
staart, vluchtduur 1). Titles en actionbar gaan via de title-packets, geluid via
`playNotifySound`, particles via `ServerLevel.sendParticles`.

## Zo vibecode je dit

1. **Volgorde.** Config en commands met de wand, dan kijkersmodus en de tp-items, dan ronde 1
   t/m 3, dan de kroon en ronde 4, dan het rad, dan voice, dan visuals. Na elke stap iets
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
