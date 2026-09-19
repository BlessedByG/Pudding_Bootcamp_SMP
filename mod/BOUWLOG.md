# Bouwlog

Status: modus=A; klaar=T0,T1,T2,T3,T4,T5,T6,T7; bezig=T8

Per taak: wat er gedaan is, wat geverifieerd is en wat open staat (in-game test, aanname,
afwijking van de docs). Het plan staat in [../docs/08-taakplan.md](../docs/08-taakplan.md).

## T0. Omgevingscheck en gereedschap

**Gedaan**
- De build draait lokaal op de Windows-machine van BlessedByG, niet meer in de cloud-sandbox waar
  het taakplan voor geschreven is. De sectie "Wat er over deze omgeving vaststaat" in het taakplan
  geldt hier dus niet.
- Gemeten op 2026-09-19: JDK 25 (Temurin 25.0.4.1) staat op PATH en is `JAVA_HOME`
  (`C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot`). JDK 21 staat er ook. Geen globale
  Gradle; de wrapper (9.5.1) komt uit de Fabric-template.
- Alle hosts bereikbaar: `maven.fabricmc.net`, `meta.fabricmc.net`, `piston-meta.mojang.com`,
  `repo1.maven.org`, `plugins.gradle.org`, `services.gradle.org`, `api.foojay.io`.
  **Modus A**: na elke taak een echte `./gradlew build`.
- Template-versies (FabricMC/fabric-example-mod, master): `minecraft_version=26.2`,
  `loader_version=0.19.5`, `loom_version=1.17-SNAPSHOT`, `fabric_api_version=0.161.0+26.2`
  (het taakplan noemt nog 0.160.0; de template is sindsdien bijgewerkt), wrapper 9.5.1.
- Naamcontrole: de Fabric API-bron (branch `26.2`) en de Loom-bron (`dev/1.17`) staan gekloond in
  de scratchpad van de sessie. In Modus A is de compiler de eerste controle; de bron is er om
  namen op te zoeken.

**Afwijking van het taakplan**
- Geen apt, geen JDK-installatie: JDK 25 stond er al.
- `gradle wrapper` kan niet zonder globale Gradle; de wrapperbestanden zijn uit de template
  gekopieerd.

## T1. Projectskelet

**Gedaan**
- `mod/settings.gradle` (Fabric-maven, Maven Central, Gradle-portal; `fabric` alleen zonder
  `-PcoreOnly`), `gradle.properties` met de template-versies, wrapper 9.5.1.
- `core`: `java-library`, `release 21`, JUnit 5, Gson als `compileOnly` (zit al in Minecraft).
- `fabric`: Loom 1.17, `release 25`, `implementation` + `include` van `core`, `fabric.mod.json`
  met `"environment": "server"` en zonder mixins, entrypoint `nl.pudding.bootcamp.Bootcamp`.
- `check.sh`, `README.md`, `.gitignore`.

**Geverifieerd**
- `./gradlew build` groen (Loom 1.17.21, Gradle 9.5.1, JDK 25).
- De jar bevat `META-INF/jars/core-0.1.0.jar`; Loom geeft die geneste jar zelf een
  `fabric.mod.json`, dus de loader pakt hem op.
- `check.sh` groen.

**Afwijking van het taakplan**
- Package is `nl.pudding.bootcamp` (kern in `nl.pudding.bootcamp.core`).
- `check.sh` doet in Modus A geen losse `javac`-syntaxcheck: de echte compile in
  `./gradlew build` vervangt die. `./check.sh --core-only` is de Modus B-variant.

## T2. Kernlogica met tests (`mod/core`)

**Gedaan**
- [core/REGELS.md](core/REGELS.md): elke regel uit docs/02 en docs/03 die rekent of beslist, met
  de test erbij.
- `Regio`, `Punt`, `BlokPos`, `BootcampConfig` (JSON heen en terug), `Ronde` (duur, gamemode,
  vereiste regio's en punten), `Rol`, `Rad`, `Countdown`, `Tijd`, `Regels` (kroonopvolging,
  einde ronde 4, FFA-deelnemers, tiebreak, uitlog- en terugkomregels, mobs schalen),
  `HordeVerloop`, `FinaleStand`, `Regeerperiodes`, `ResetRegister`, `KitDef`, `WavesDef`,
  `Doodteksten`, `BossbarTekst`.

**Geverifieerd**
- `:core:test`: 74 tests, 0 fouten. Twee tests in `ResourcesTest` wachten op de kits en
  `waves.json` uit T5 en slaan zichzelf tot dan over.

**Aannames en concretiseringen** (komen in T16 ook in docs/04)
- **Een regio is voor spelers een kolom**: alleen x en z tellen. Zo werkt een selectie van twee
  hoeken op de grond ook voor wie erop staat, en voor een vloer met hoogteverschil. Poorten
  gebruiken wel de hele doos.
- **Spelers staan op naam in `bootcamp.json`** (slots, uitverkoren), in kleine letters, zodat de
  staff alles kan klaarzetten voordat iemand online is. Een pilaar heeft één kop: een slot
  opnieuw uitdelen haalt het bij de vorige weg.
- **FFA-tiebreak bij een gelijk aantal kills**: de docs zeggen alleen "het aantal kills". Bij een
  gelijke stand wint wie de meeste hp heeft, daarna het lot.
- **Mobs schalen mee**: het aantal in `waves.json` geldt voor twintig spelers en schaalt naar
  boven afgerond mee (minimaal 1); de boss wave staat vast (`"schaal": false`). Een testrun met
  vijf man krijgt dus vijf zombies in wave 1.
- **De laatste wave wacht niet op de klok**: die moet dood (of de rondetimer loopt af).
- **Uitloggen buiten ronde 4**: een FFA-speler of een speler in de horde die uitlogt telt als
  dood, net als een hunter. Een finalist die in de finale uitlogt verliest het lopende potje; hij
  blijft finalist als hij terugkomt (anders kan de finale niet verder). De koning die binnen zijn
  dertig seconden terugkomt blijft koning.
- **Terugkomen in ronde 1 of 3**: naar de ingang van de zone, of naar het volgende verzamelpunt
  als je al klaar was. In ronde 2: kijker op de tribune.

## T3 + T4. Config, gamerules, commands, wand, regio's en punten

Samen in één commit: het commandboompje verwijst naar de wand, dus los compileren ze niet.

**Gedaan**
- `Mc`: de adapterlaag (tekst, spelers, staff, teleport, heal). Groeit per taak mee.
- `config/ConfigStore`: `<wereld>/bootcamp.json` laden bij `SERVER_STARTED`, opslaan na elke
  wijziging en bij `SERVER_STOPPING` (via een tmp-bestand). Een onleesbaar bestand wordt als
  `bootcamp.json.kapot` opzij gezet; de mod begint dan leeg in plaats van te crashen.
- `config/Standaardbestanden`: schrijft wat ontbreekt uit `standaard/` in de jar naar
  `config/bootcamp/`. De bestanden zelf komen in T5.
- `game/Spelregels`: gamerules met de 26.2-namen (`NATURAL_HEALTH_REGENERATION`, `SPAWN_MOBS`,
  `MOB_GRIEFING`, `ADVANCE_TIME`, `ADVANCE_WEATHER`, `SHOW_ADVANCEMENT_MESSAGES`, `LOCATOR_BAR`).
- `game/Teams`: `spelers` wit, `hunters` aqua met friendly fire, `king` goud, `out` grijs.
- `game/Reset` + `SpelerReset`: het register achter `/bc reset`. Nu geregistreerd: effecten en
  heal, inventory, gamemode, teleport naar `basiskamp` (overgeslagen als dat punt niet bestaat),
  teams, gamerules, bossbar.
- `visuals/Bossbar`: één `ServerBossEvent` voor iedereen.
- `commands/`: het hele `/bc`-boompje op op-level 2 (`Commands.LEVEL_GAMEMASTERS`). Werkend:
  `wand`, `region save|show|list|del`, `point set|block|tp|list|del`, `uitverkoren`, `slot`,
  `status`, `reset`. De rest meldt netjes in welke taak het komt.
- `setup/Wand`: stick met `custom_data`, linksklik hoek 1, rechtsklik hoek 2, `region show` tien
  seconden `end_rod`-particles op de twaalf randen (maximaal 600 per tekenbeurt).

**Geverifieerd**
- `./gradlew build` groen tegen de echte 26.2-jar, `check.sh` groen (alle command-literals uit
  docs/04 aanwezig).

**Open: in-game testen**
- Wand: klikken, `region save`, `region show`, of de klik in creative het blok niet sloopt.
- `/bc point block` kijkt tot 32 blokken ver.

**Concretiseringen**
- **Staff is wie in creative of spectator staat.** De mod zet deelnemers zelf in adventure of
  survival, dus wie in creative of spectator staat is host, camera of admin en wordt met rust
  gelaten (geen teleport, geen kit, telt niet mee). Geen apart command nodig.
- `/bc uitverkoren` en `/bc slot` nemen een naam, geen online speler, zodat je alles vooraf kunt
  klaarzetten. Hun antwoord gaat alleen naar wie het typt en niet naar de andere ops of de log:
  het rad blijft geheim. `/bc uitverkoren` zonder naam laat zien wie het is.
- Namen van regio's en punten: kleine letters, cijfers en `_`.

## T5. Kits

**Gedaan**
- `kits/Kits`: leest `config/bootcamp/kits/<naam>.json` bij elk gebruik opnieuw (aanpassen zonder
  herstart), parst elk item met de vanilla `ItemParser`, zet de kit op spelers. `/bc kit <naam>
  [<speler>]` met suggesties uit de map.
- `kits/Items26`: item uit tekst, en de kroon zelf: gouden helm met Curse of Binding, onbreekbaar
  (een gouden helm is anders zo stuk), herkenbaar aan `custom_data={bootcamp_kroon:1b}`.
- Standaardbestanden in de jar (`standaard/`): `horde`, `ei`, `boss`, `kroonpakket`, `arena`,
  `finale`, `basis.README.txt` en `waves.json`, ingevuld uit docs/02. `basis.json` wordt niet
  meegeleverd; `/bc kit basis` meldt dat het ontbreekt en verwijst naar de README.

**Geverifieerd**
- `KitsParseTest` (fabric-project, draait met de echte 26.2-registries via `fabric-loader-junit`,
  zonder server): alle zes kits parsen, enchantments komen erop, de kroon is te maken en wordt
  herkend, de gear uit `waves.json` parst, en een onbekend item geeft
  `test.json, hotbar[1]: ...`.
- `ResourcesTest` in `core` slaat niks meer over: 76 tests groen.

**Open: in-game testen**
- `/bc kit horde`, `/bc kit basis` (na het neerzetten van `basis.json`), of armor goed aankomt.

**Concretiseringen**
- **De kroon blijft op doordat de kit naar het item kijkt**: zit de kroon in de head-slot, dan
  schrijft geen enkele kit eroverheen en haalt `clear` hem niet weg.
- **`horde.json` heeft `clear: false`**: docs/02 zegt dat je alles uit het doolhof mag houden, dus
  de hordekit komt erbij. `boss`, `arena` en `finale` wissen wel.
- Bij `clear: false` komt een item op zijn slot als dat leeg is en anders ergens in de inventory;
  armor dat er al zat gaat naar de inventory.
- De boss wave schaalt niet mee (docs/02: "boss wave vast"). Voor een testrun met vijf man is
  2 ravagers + 4 evokers + 10 vindicators te zwaar; zet dan de aantallen in
  `config/bootcamp/waves.json` omlaag of `"schaal": true`.

## T6. Spelraamwerk en gedeelde primitieven

**Gedaan**
- `game/Spel`: de spelstatus (ronde, timer, per speler rol en vlaggen, finalisten), de tick-loop,
  `start` met de vereiste-config-check ("ontbreekt: troon, hunter_3, ..." en dan verandert er
  niets), `stop`, join en quit naar de lopende ronde, de spiegeltags elke seconde, `/bc status`.
- `game/RondeLogica`: een ronde is een klasse met `start`, `tick`, `seconde`, `timerOp`, `onDeath`,
  `onQuit`, `onJoin`, `end`, `vereisteRegios`, `vereistePunten`, `magStarten`. `game/Rondes` kiest
  de klasse; tot T8 t/m T13 is dat `LegeRonde` (teleport, border, gamemode, countdown, timer).
- `game/Planner`: "drie seconden later" als tick-teller. `game/Aftelling`: de countdown voor
  iedereen (actionbar, laatste vijf als title met stijgende pling, groene GO met raid horn).
- `game/Border`: border uit een regio, krimp met duur (`lerpSizeBetween` rekent in 26.2 in ticks
  en wil de game time erbij), border weg.
- `game/Poorten`: `poort_<naam>` open en dicht, met cloud-particles en raid horn.
- `crown/Kroon`: kroon geven (oude helm naar de inventory) en afnemen, team `king`, Glowing,
  finalist markeren, laatste hit, locator bar via `WAYPOINT_TRANSMIT_RANGE`.
- `crown/Opstelling`: bevriezen (`MOVEMENT_SPEED` en `JUMP_STRENGTH` op 0, terug naar 0.1 en 0.42)
  plus een `UseItemCallback` die pearls blokkeert zolang de vlag staat.
- `visuals/Zweefkroon`: item display met een gouden helm, schaal 0.5, interpolatie 2 ticks, elke
  twee ticks boven het hoofd gezet en zes graden gedraaid.
- `/bc start|stop|timer|poort` werken; `Mc` heeft er titles, actionbar, geluid, particles,
  attributes en effecten bij.
- Reset-register, in deze volgorde: ronde stoppen en vlaggen wissen, kronen, bevriezing,
  zweefkroon, poorten dicht, effecten en heal, inventory, gamemode, teleport naar basiskamp, teams,
  gamerules, border, bossbar.

**Geverifieerd**
- `./gradlew build` en `check.sh` groen.

**Open: in-game testen**
- `/bc start 1` t/m `6` met de lege rondes: teleport, border, countdown, timer in de bossbar,
  `/bc stop`, `/bc timer 30`.
- Bevriezen: niet lopen, niet springen, niet pearlen; FOV-effect van snelheid 0 bekijken.
- De zweefkroon: hoogte (2.45 boven de voeten) en of hij vloeiend meebeweegt.

**Concretiseringen**
- **Een poort onthoudt wat er stond** toen hij openging en zet dat terug bij dicht. Alleen als de
  mod het niet meer weet (na een herstart met open poort) wordt het iron bars, zoals docs/04 zegt.
  Een ronde zonder poort-regio start gewoon; een poort is niet verplicht.
- **Ook wind charges en chorus fruit** zijn geblokkeerd tijdens een opstelling, niet alleen pearls.
- **Tijdens een opstelling doet niemand elkaar schade** (komt in T7 in `ALLOW_DAMAGE`): anders
  schiet de koning in zijn 30 seconden voorsprong op hunters die niet weg kunnen.
- **Tussen twee rondes in is er geen border.** Bij het einde van een ronde gaat de border weg en
  bij `/bc start` komt de nieuwe.
- **`/bc start` tijdens een lopende ronde** breekt die eerst af.
- **De commander start elke ronde**, ook ronde 5 (besluit 14). Alleen de finale start vanzelf na
  de twee minuten rust.
- `Spel.buitenRegio`: een ronde weigert te starten als een startpunt buiten haar border-regio
  ligt, want wie buiten de border wordt neergezet krijgt schade.

## T7. Kijkers en tribune

**Gedaan**
- `tribune/Tribune`: `ALLOW_DEATH` vangt elke dood van een deelnemer af (annuleren, zelf healen,
  effecten weg) en geeft hem aan de lopende ronde (`onDeath`). Zonder ronde gaat de speler terug
  naar het verzamelpunt van dat moment.
- `ALLOW_DAMAGE`: geen schade voor kijkers en wachtende finalisten (ook niet van de border), geen
  schade van kijkers, geen schade voor wie dan ook zolang een opstelling loopt, en de laatste hit
  op de koning wordt onthouden (ook via een pijl: `getEntity()` is de schutter).
- `maakKijker`: rol `KIJKER`, team `out`, adventure, geen Glowing, van de locator bar af, ontdooid,
  geheald, naar `tribune_n` of `tribune_horde_n` (om en om), met de doodtekst als title voor
  alleen de dode. Inventory: houden (klaar met het doolhof), bewaren (ronde 2, terug bij `v3`) of
  legen (de Arena).
- De tick-check (elke halve seconde) zet een kijker die in regio `arena` (ronde 2) of `vloer`
  (ronde 4 t/m 6) komt terug op zijn eigen tribunepunt.
- `/bc kijker <speler> aan|uit`.
- `RondeLogica` heeft nu standaardgedrag voor uitloggen en terugkomen, op basis van de regels uit
  `core` (`Regels.bijQuit`, `Regels.bijJoin`).

**Geverifieerd**
- `./gradlew build` en `check.sh` groen.

**Open: in-game testen**
- Doodgaan in een ronde: geen death-screen, doodtekst in beeld, op de tribune, geen schade daar.
- Van de tribune de vloer op lopen: teruggezet.
- Uitloggen en terugkomen in elke ronde.

**Concretiseringen**
- `/bc kijker <speler> uit` maakt iemand weer deelnemer van de lopende ronde: hunter in ronde 4,
  FFA-speler in ronde 5, anders speler. Zijn spullen komen niet vanzelf terug (behalve de bewaarde
  inventory van ronde 2); geef ze met `/bc kit`.
- Een kijker die toch een dodelijke klap krijgt (de void in, `/kill`) gaat terug naar zijn
  tribunepunt.
