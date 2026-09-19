# Taakplan: de bootcamp-mod bouwen

Dit is het plan dat Claude Code zonder toezicht uitvoert. Jij leest het, zegt "voer het taakplan
uit", en gaat slapen. 's Ochtends ligt er een gepushte branch met de mod, een bouwlog met wat wel
en niet is geverifieerd, en een testlijst voor op je eigen server.

Het plan is nagekeken door drie onafhankelijke reviewagents (haalbaarheid, dekking van de spec,
uitvoerbaarheid zonder toezicht) en elke bevinding is door een tweede agent tegengesproken. De
26 bevindingen die overeind bleven zijn hierin verwerkt; de meetresultaten uit die review staan
bij "Wat er over deze omgeving vaststaat".

## Doel

Een server-side Fabric-mod `bootcamp` voor Minecraft 26.2 in de map `mod/` van deze repo, die
alles doet wat in [04-technische-schets.md](04-technische-schets.md) staat: regio's en punten met
een wand en commands, kits uit JSON, de zes rondes, de kroon met reset en bevriezing, het rad, de
tribune voor wie dood is, bossbar en visuals. Spelregels komen uit [02-rondes.md](02-rondes.md)
en [03-kroon-regels.md](03-kroon-regels.md); de docs zijn de spec, niet andersom.

## Wat er over deze omgeving vaststaat (gemeten)

- Java 21 en Gradle 8.14 staan geïnstalleerd. **Minecraft 26.2 vraagt Java 25** (de officiële
  Fabric-template voor 26.2 zet `release 25` en `"java": ">=25"`). JDK 25 is hier te installeren
  met `apt-get install openjdk-25-jdk-headless` (gesimuleerd, slaagt). Gradle draait op 21 en
  compileert met een toolchain 25.
- Bereikbaar: Maven Central (`repo1.maven.org`, met een 429-limiet bij veel gelijktijdige
  downloads, dus één worker en een retry), `plugins.gradle.org`, `services.gradle.org`,
  `raw.githubusercontent.com`, en `git clone` van `github.com`.
- **Niet bereikbaar** (403 via de proxy): `maven.fabricmc.net`, `meta.fabricmc.net`,
  `piston-meta.mojang.com`, `piston-data.mojang.com`, `libraries.minecraft.net`, `api.foojay.io`,
  `codeload.github.com`, `api.github.com`.
- De Fabric-template voor 26.2 (raw.githubusercontent.com/FabricMC/fabric-example-mod/master)
  geeft de echte versies: `minecraft_version=26.2`, `loader_version=0.19.5`,
  `loom_version=1.17-SNAPSHOT`, `fabric_api_version=0.160.0+26.2`, Gradle-wrapper 9.5.1, plugin-id
  `net.fabricmc.fabric-loom`, `org.gradle.configuration-cache=false`. **Geen mappings-regel**:
  26.2 is niet meer geobfusceerd, Loom werkt met de echte Mojang-namen. Loader en Fabric API gaan
  met `implementation`.
- De Fabric API-bron voor 26.2 (`git clone --depth 1 --branch 26.2 https://github.com/FabricMC/fabric.git`)
  en de Loom-bron (`--branch dev/1.17 .../fabric-loom.git`) zijn hier te clonen. Fabric API is in
  Mojang-namen geschreven, dus daar staan de echte 26.2-namen van honderden Minecraft-klassen in.
  Dat is de naamcontrole voor alle code die hier niet gecompileerd kan worden.
- Al bekende 26.2-namen (uit die bron): `Identifier` in plaats van `ResourceLocation`
  (`Identifier.fromNamespaceAndPath`, `Identifier.parse`), `EntityTypes.ZOMBIE` in plaats van
  `EntityType.ZOMBIE`, `EntitySpawnReason` in plaats van `MobSpawnType`, gamerules via
  `level.getGameRules().set(GameRules.ADVANCE_TIME, false, server)` met nieuwe namen
  (`ADVANCE_TIME`, `SPAWN_MOBS`, ...), permissies via `src.permissions().hasPermission(...)`.

## Voorwaarde vóór je gaat slapen: netwerk

Zonder de servers van Fabric en Mojang kan Gradle Loom de Minecraft-jar en Fabric API niet
ophalen en kan ik de mod niet compileren. Zet in de instellingen van deze Claude Code-omgeving
het netwerkbeleid op volledige toegang, of voeg deze hosts toe aan de allowlist:

```
maven.fabricmc.net
piston-meta.mojang.com
piston-data.mojang.com
libraries.minecraft.net
repo1.maven.org
plugins.gradle.org
services.gradle.org
```

Optioneel: `resources.download.minecraft.net` (alleen voor `runServer`/`runClient`) en
`meta.fabricmc.net`. JDK 25 komt via apt, daar is geen host voor nodig.

Het plan werkt in twee modi en kiest zelf bij stap 0:

- **Modus A (netwerk open):** ik compileer na elke taak met de echte Minecraft-jar, fix
  compile-fouten zelf en lever een werkende jar op in `mod/fabric/build/libs/`.
- **Modus B (netwerk dicht):** ik schrijf alle code met de naamcontrole tegen de Fabric API-bron,
  verifieer de kernlogica met unit tests en het Fabric-deel met een syntaxcheck, en zet in het
  bouwlog precies wat niet gecompileerd is. Jij draait 's ochtends `./gradlew build` op je eigen
  machine (met JDK 25) en plakt de fouten terug; die fix ik in een tweede run.

## Wat ik zelf beslis en waar ik van je afblijf

**Zelf:** alle implementatiekeuzes binnen de spec, naamgeving van packages en bestanden, de
JSON-formaten voor kits en waves, en kleine concretiseringen van de docs waar de code iets nodig
heeft (bijvoorbeeld een extra regio). Elke afwijking komt in het bouwlog én in docs/04.

**Van jou:** de inhoud van de basiskit (jij levert `basis.json`), alle in-game tests (ik heb geen
Minecraft-server), de bouw van de wereld, en spelregels. Ik verander geen regel uit docs/02 of
docs/03; kom ik iets tegen dat niet kan of tegenstrijdig is, dan kies ik de kleinste werkende
interpretatie en schrijf dat op.

**Nooit:** een pull request openen, pushen naar een andere branch dan
`blessedbyg/dreamy-shannon-psxiuh`, client-side code, iets aan de voice-mod veranderen, of het
raamwerk herschrijven nadat de rondes erop gebouwd zijn.

## Hoe ik het uitvoer

- **Eén commit per taak**, gepusht na elke taak. Gaat het mis, dan staat het werk tot dan toe
  veilig op de branch. Een mislukte push is een stop met een melding in de chat, niet doorgaan
  met ongepusht werk.
- **Kernlogica los van Minecraft.** Alles wat rekent (regio's, het rad, de timer,
  ronde-overgangen, kit- en config-modellen, de uitlog-regels) komt in een apart Gradle-project
  `mod/core` zonder Minecraft-imports, met JUnit-tests. Dat draait hier altijd, ook in Modus B.
  Het Fabric-project `mod/fabric` gebruikt `core` en bevat alleen de lijm naar Minecraft, met één
  dunne adapterlaag (`Mc.java`: teleport, geluid, particles, title, attributes, gamerules) zodat
  compile-fouten 's ochtends in één of twee bestanden zitten.
- **Naamcontrole.** Een Minecraft- of Fabric-naam mag alleen zonder `// TODO 26.2` in de code
  staan als hij met `grep -r` in de gekloonde Fabric API-bron voor 26.2 voorkomt. Geluiden,
  particles, effecten, attributes en entity-types worden opgezocht op id-string via de registries
  (`"minecraft:..."`), niet via statische constanten.
- **Vaste verificatie na elke taak** (`mod/check.sh`, gemaakt in T1), in beide modi:
  1. `./gradlew -PcoreOnly :core:test` groen (één worker, retry bij 429).
  2. Syntaxcheck van het Fabric-deel: `javac` over alle bronnen zonder classpath; alleen "cannot
     find symbol" en "package ... does not exist" worden genegeerd, elke andere fout is echt.
  3. Een JUnit-test in `core` die `fabric.mod.json`, alle `kits/*.json` en `waves.json` uit de
     resources inleest en valideert.
  4. Grep-checks: elk command uit de tabel in docs/04 komt letterlijk voor in het
     commands-package; `Thread.sleep` en `new Thread` komen nergens voor; elke Minecraft-naam
     zonder `// TODO 26.2` staat in de Fabric API-bron.
  5. Modus A extra: `./gradlew build` groen en `unzip -l` van de jar laat de `core`-klassen zien.
- **Parallel waar het veilig is, zonder worktrees.** De zes rondes bouw ik met zes subagents in
  dezelfde working tree, met de harde regel: schrijf alleen in je eigen package
  (`game/rondeN/`; ronde 4 ook `crown/` en `rad/`), raak geen ander bestand aan, lever je
  registratieregels en je bouwlogtekst als tekst terug. Ik zet die registratieregels erin, draai
  `check.sh`, en commit en push T8 t/m T13 na elkaar. Raamwerk, config, commands en kijkers bouw
  ik sequentieel, want daar raakt alles elkaar.
- **Reviewronde aan het eind**, begrensd: drie agents, maximaal tien bevindingen elk, alleen hoog
  en midden worden gefixt, één fixronde, geen refactors van het raamwerk. Wat overblijft komt als
  lijst in het bouwlog.
- **Bouwlog** `mod/BOUWLOG.md` met bovenin een vaste statusregel
  `Status: modus=A|B; klaar=T0,T1,...; bezig=Tn` die in elke taakcommit wordt bijgewerkt, en per
  taak wat er gedaan is, wat geverifieerd is en wat open staat (in-game test, aanname, afwijking
  van de docs).
- **Hervatten na een uitval:** `git status`; alles wat niet gecommit is weggooien
  (`git checkout -- . && git clean -fd mod/`); T0 alleen herhalen als het netwerk veranderd is;
  doorgaan bij de eerste taak die niet in `klaar` staat.

## Besluiten die de review nodig maakte

Deze staan nu ook in de docs (02, 03, 04, 05), zodat spec en plan hetzelfde zeggen:

- **Uitloggen.** De mod handelt het af, niet de ref: een hunter die uitlogt telt als dood; logt de
  koning uit, dan telt de mod 30 seconden af (zichtbaar in de bossbar) en volgt daarna dezelfde
  kroonwissel als bij een val-dood; wie terugkomt in ronde 4 t/m 6 wordt kijker op de tribune, in
  ronde 1 t/m 3 gaat hij naar het verzamelpunt of de tribune van dat moment.
- **Gamemode per ronde** zet `/bc start`: survival alleen in ronde 3, adventure in alle andere
  rondes, kijkers altijd adventure. De cobblestone is uit de Ei-kisten (bouwen is toch verboden).
- **Gamerules** zet de mod bij serverstart en bij `/bc reset`, inclusief `mobGriefing` uit
  (creepers in de ruïne-arena). `locatorBar` aan in `start(4)`, uit bij reset.
- **Doden in ronde 2 houden hun spullen**: de inventory wordt bewaard en bij `v3` teruggegeven.
  In de Arena is de inventory van een kijker leeg.
- **De kroon vervangt de helm**; de vorige helm gaat naar de inventory. Kits schrijven nooit over
  de head-slot van een koning of finalist; `boss.json` en `finale.json` hebben geen helm.
- **Sidebar** toont in ronde 2 de sneuvelvolgorde en in ronde 4 de regeerperiodes.
- **`/bc stop` breekt de ronde af** (timer stil, mobs en border weg, bevriezing eraf, bossbar
  terug), `/bc timer` stelt bij. `/bc reset` ruimt via een register op waar elk onderdeel zijn
  eigen stap in zet (effecten, zweefkroon, sidebar, attributes, pearl-blokkade, locator-attribute,
  horde-mobs, kijkers, teams, gamemode, inventory, border, bossbar).
- **De mod is de bron van waarheid**, scoreboard-tags zijn read-only spiegels in kleine letters
  (`king`, `hunter`, `kijker`, `uitverkoren`, `ticket`) die elke seconde worden bijgezet.
  `/bc status` toont rollen en vlaggen. Het draaiboek verwijst niet meer naar `/tag`.
- **Ontbrekende config** is een nette weigering: elke ronde declareert welke regio's en punten
  ze nodig heeft; `/bc start` weigert met "ontbreekt: troon, hunter_3, ..." en verandert niets.
  `/bc rad` weigert zonder precies één uitverkorene. FFA-startpunten zijn `hunter_1..4` om en om.
- **`doolhof_uit` is een regio** (het vak achter de uitgang), geen punt meer.
- **Finale-border** krimpt in 30 seconden naar 6 x 6.
- **Standaardbestanden**: de voorbeeld-kits en `waves.json` zitten als resources in de jar en
  worden bij de eerste serverstart naar `config/bootcamp/` geschreven als ze ontbreken.
  `basis.json` wordt niet meegeleverd; `/bc kit basis` meldt netjes dat het bestand ontbreekt.

## Taken

Elke taak eindigt met `mod/check.sh` groen, een bijgewerkt bouwlog, een commit en een push.

### Fase 1: fundament (sequentieel)

**T0. Omgevingscheck en gereedschap.**
Hosts uit de lijst meten en de modus kiezen. JDK 25 installeren met apt (terugval: de Oracle-
tarball naar `/opt`) en het pad in het bouwlog zetten. De template-versies ophalen van
raw.githubusercontent.com (`gradle.properties` en `gradle-wrapper.properties` van
FabricMC/fabric-example-mod). De Fabric API-bron (branch 26.2) en de Loom-bron (dev/1.17) clonen
naar de scratchpad voor de naamcontrole. `mod/BOUWLOG.md` aanmaken met de statusregel.
Klaar als: modus, JDK-pad, versies en clone-paden in het bouwlog staan.

**T1. Projectskelet.**
`mod/settings.gradle` met `pluginManagement` (Fabric-maven, Maven Central, Gradle-portal),
`include 'core'` en `fabric` alleen als `coreOnly` niet op waar staat
(`providers.gradleProperty('coreOnly').map { it != 'false' }.getOrElse(false)`); geen Loom buiten
het fabric-project. Wrapper 9.5.1 (`gradle wrapper --gradle-version 9.5.1 -PcoreOnly`).
`mod/gradle.properties` met de template-versies, `org.gradle.configuration-cache=false`,
`org.gradle.parallel=true`, `org.gradle.jvmargs=-Xmx2G`, en in Modus B `coreOnly=true`.
`core/build.gradle`: Java `release 21`, JUnit 5, `group` en `version`. `fabric/build.gradle`:
plugin `net.fabricmc.fabric-loom`, toolchain 25 en `release 25`, `minecraft "com.mojang:minecraft:26.2"`,
`implementation` voor loader en Fabric API, `implementation project(':core')` én
`include project(':core')` zodat `core` in de jar komt. `fabric.mod.json` met
`"environment": "server"`, entrypoint `main`, depends `fabricloader >=0.19.5`, `minecraft ~26.2`,
`java >=25`, `fabric-api *`; geen mixins-config. De entrypoint-klasse, `.gitignore`,
`mod/README.md` (bouwen met JDK 25, installeren, dat de testserver Java 25 nodig heeft) en
`mod/check.sh`.
Klaar als: de structuur staat, `check.sh` draait.
Verificatie: Modus A `./gradlew build` groen en de jar bevat `core`; Modus B
`./gradlew -PcoreOnly :core:test` groen op JDK 21.

**T2. Kernlogica met tests (`mod/core`).**
Eerst `mod/core/REGELS.md`: een genummerde lijst van elke regel uit docs/02 en docs/03 die rekent
of beslist, met per regel de testnaam. Dan de code: Regio (min, max, center, grootte,
bevat-punt), Punt (positie plus kijkrichting), configmodel voor `bootcamp.json` (regio's, punten,
doodteksten, slots, uitverkoren) met JSON in en uit, het rad (stappen tot het doel, start en
rondes uit een meegegeven bron, ritme per resterende stap van 2 naar 30 ticks), timer en
countdown als tick-state, rollen en rondes als enums, de overgangstabel per ronde (inclusief
"iedereen behalve Clown en finalist 1", tiebreak op kills, einde bij geen levende hunter), de
uitlog-regels, kit- en wave-modellen als tekst met validatie, het kiezen van een doodtekst, de
reset-registerlogica.
Klaar als: elke regel in `REGELS.md` een groene test heeft.
Verificatie: `:core:test` groen, in beide modi.

### Fase 2: raamwerk (sequentieel)

**T3. Config, gamerules en commands.**
Laden en opslaan van `<wereld>/bootcamp.json` bij server start en stop; standaardbestanden uit de
resources naar `config/bootcamp/` schrijven als ze ontbreken; gamerules zetten bij
`SERVER_STARTED`; het complete `/bc`-commandboompje uit docs/04 op op-level 2 (plus `/bc status`),
met nette meldingen voor wat nog niet bestaat; `/bc reset` als command plus het `Reset`-register
waar elk later onderdeel zijn opruimstap in registreert (T3 registreert zelf: gamerules, teams,
gamemode adventure, teleport naar `basiskamp`, bossbar).
Verificatie: `check.sh`.

**T4. Wand, regio's en punten.**
`/bc wand` (stick met custom data component), linksklik en rechtsklik op blokken voor de hoeken,
`/bc region save|show|list|del`, `/bc point set|block|tp|list|del`, `region show` met tien
seconden particles op de randen.
Verificatie: `check.sh`; regiologica in `core` heeft tests.

**T5. Kits.**
Loader voor `config/bootcamp/kits/*.json` in het formaat uit docs/04, items geparst met de
vanilla item-parser, `/bc kit <naam> [<speler>]`, head-slot van een koning of finalist wordt
overgeslagen, en de voorbeeldbestanden `horde`, `ei`, `boss` (zonder helm), `kroonpakket`,
`arena`, `finale` (zonder helm) als resources, ingevuld uit docs/02. `basis.json` niet
meegeleverd, wel `basis.README` en een nette melding als het ontbreekt. Een fout in een
kitbestand geeft één regel in de console met bestandsnaam en slot.
Verificatie: `check.sh` (de resourcetest valideert alle kits).

**T6. Spelraamwerk en gedeelde primitieven.**
`GameState` (ronde, timer, vlaggen, rol per speler, inventory-snapshots), tags als spiegel, teams
zoals in docs/04 (`hunters` met friendly fire aan), de tick-loop, de bossbar, worldborder uit
een regio met krimp-met-duur, poorten open en dicht, teleports naar verzamelpunten, gamemode per
ronde, countdown met titles en geluid, `/bc start|stop|timer|poort` met de vereiste-config-check,
join- en quit-events die naar de actieve ronde gaan. De gedeelde kroon-primitieven komen hier
ook al: kroon geven en afnemen (helm naar inventory), team `king`, Glowing, zweefkroon aan en
uit, finalist markeren, laatste hit bijhouden, Opstelling met bevriezing (attributes op 0 en
terug, pearls geblokkeerd, countdown), de rust-state met bossbar-timer. Elke ronde is een klasse
met `start`, `tick`, `onDeath`, `onQuit`, `onJoin`, `end`, `vereisteRegios`, `vereistePunten`;
in deze taak nog lege rondes die alleen teleporteren, border en gamemode zetten en de timer laten
lopen. Registreert in het reset-register: effecten, zweefkroon, attributes, pearl-blokkade,
border, teams, timer.
Verificatie: `check.sh`; timer-, overgangs- en uitloglogica zit in `core` met tests.

**T7. Kijkers en tribune.**
Dood afvangen (`ALLOW_DEATH`), per ronde afhandelen zoals de tabel in docs/04, teleport naar
`tribune_n` of `tribune_horde_n`, inventory bewaren in ronde 2 en teruggeven bij `v3`, geen
schade voor kijkers (ook niet van de border), de tick-check die een kijker uit regio `vloer` of
`arena` terugzet, locator-attribute uit voor kijkers, de doodtekst als title alleen voor de dode,
`/bc kijker <speler> aan|uit`, quit = dood en join = rol herstellen en op de goede plek zetten.
Registreert in het reset-register: kijkers weg, locator-attribute terug.
Verificatie: `check.sh`.

### Fase 3: de rondes (zes subagents, eigen package, sequentieel samengevoegd)

Elke ronde raakt buiten haar package alleen registratieregels, die ik zelf plaats. Na elke ronde
`check.sh`, commit, push.

**T8. Ronde 1: De Doolhof.**
Start bij `doolhof_start`, border `doolhof`, adventure, uitgang via regio `doolhof_uit` naar `v2`,
de eerste vijf krijgen het voorsprongkistje (1 gapple, 1 pearl) en een title, hint-title op 7
minuten, na de timer iedereen naar `v2`. Vereist: regio's `doolhof`, `doolhof_uit`; punten
`doolhof_start`, `v2`.

**T9. Ronde 2: De Horde.**
Start bij `arena_spawn`, border `arena`, hordekit, waves uit `config/bootcamp/waves.json` (per
wave mobs met aantal, gear en spawnpunt; de vijf waves uit docs/02 als standaard), spawnen via
`EntityTypes` met tag `horde` en `setPersistenceRequired`, mobteller in de bossbar, title en horn
per wave, volgende wave bij teller 0 of na 120 seconden, sidebar met sneuvelvolgorde, ronde stopt
bij wave 5 dood, iedereen dood of de timer, pearls voor overlevers, doden weer speler bij `v3`
met hun bewaarde inventory, alle `horde`-mobs weg bij `end` en via het reset-register.
Vereist: regio `arena`; punten `arena_spawn`, `mob_1..4`, `tribune_horde_1..2`, `v3`.

**T10. Ronde 3: Het Ei.**
Start bij `ei_start`, border `eibos`, survival, pickaxe-kit erbij, ticketcheck in regio `eiplaat`
(diamond block innemen, ticket, adventure, teleport naar `kring`), beacon-hint op 5:00
(`ei_beacon` plaatsen), vuurpijl boven het Ei op 3:00, na de timer wie geen ticket heeft leeg plus
basiskit, adventure, naar `kring`. Vereist: regio's `eibos`, `eiplaat`; punten `ei_start`,
`ei_beacon`, `kring`.

**T11. Ronde 4: King of the SMP.**
Het rad (lampen `lamp_0..19`, slots, precies één uitverkorene, ritme uit `core`, visuals, drie
seconden later de start), Clown naar `troon` met bosskit en kroon, hunters naar `hunter_1..4`,
Opstelling voor 30 en bij elke wissel 10 seconden, kroonwissel als reset (ex-koning naar de
tribune, nieuwe koning naar `troon` met heal, reparatie, kroonpakket, Resistance, Glowing, helm,
team), koning-uitlog met 30-secondenteller, timer en einde (0 of geen levende hunter),
regeerperiodes in de sidebar, locator bar aan met alleen koningen zichtbaar,
`/bc kroon|rad|uitverkoren|slot`. Vereist: regio `vloer`; punten `troon`, `hunter_1..4`,
`tribune_1..4`, `lamp_0..19`.

**T12. Ronde 5: Arena FFA en de rust.**
Deelnemers: iedereen behalve Clown en finalist 1, ook de doden van ronde 4; startpunten
`hunter_1..4` om en om, arenakit, teams weg, countdown, border `vloer` krimpt na 5 minuten in 2
minuten naar 10, dood naar de tribune, laatste over krijgt de tweede kroon, tiebreak op kills bij
10 minuten, daarna twee minuten rust met timer in de bossbar en beide finalisten op de tribune.
Vereist: regio `vloer`; punten `hunter_1..4`, `tribune_1..4`.

**T13. Ronde 6: De Finale en de kroning.**
Startpunten `finale_1` en `finale_2`, finalekit (kroon blijft op), border `finale` (20, na 3
minuten in 30 seconden naar 6), potjes tellen met heal en kit-reset, na twee gewonnen potjes de
kroning: iedereen op de tribune, winnaar op `kroning`, twintig seconden vuurpijlen, titles.
Vereist: regio `finale`; punten `finale_1`, `finale_2`, `kroning`, `tribune_1..4`.

### Fase 4: afronding (sequentieel)

**T14. Visuals.**
Bossbar-teksten en kleuren per ronde uit de tabel in docs/04, alle geluiden en particles uit de
tabel "per moment" via registry-lookup op id, labels via `/bc label <tekst>` (text display op je
positie), zweefkroon vloeiend. Registreert in het reset-register: bossbar terug naar
`Pudding Bootcamp`, sidebar leeg.
Verificatie: `check.sh`.

**T15. Reviewronde (begrensd).**
Drie agents, maximaal tien bevindingen elk: één legt de code naast docs/02 en 03, één naast
docs/04 (structuur, commands, bestanden, reset-register compleet), één zoekt bugs (null-paden,
ronde-overgangen, uitloggen, ontbrekende config). Elke bevinding wordt door een tweede agent
tegengesproken; alleen hoog en midden worden gefixt, in één ronde, zonder het raamwerk te
herschrijven. Wat overblijft komt als lijst in het bouwlog. Daarna `check.sh` en in Modus A
`./gradlew build`.

**T16. Documentatie.**
`mod/README.md` compleet (JDK 25, bouwen, installeren, configbestanden, alle commands, kits,
waves, wat er bij de eerste start gebeurt), docs/04 bijgewerkt waar de implementatie afwijkt of
concreter is (26.2-namen, geen mappings, Java 25, reset-register, `/bc status`), docs/05 met de
ochtendlijst hieronder, `mod/BOUWLOG.md` afgerond met de lijst `// TODO 26.2`-plekken.

**T17. Eindrapport.**
Laatste `check.sh` (Modus A: build), alles gepusht, en in de chat een samenvatting: wat er ligt,
wat geverifieerd is, wat jij moet doen, en welke aannames ik heb gemaakt.

## Wat jij 's ochtends doet

1. `git pull` op de branch, lees `mod/BOUWLOG.md` (statusregel bovenin) en `mod/README.md`.
2. Modus B: op een machine met JDK 25 `cd mod && ./gradlew build`. Compile-fouten in één bericht
   terugplakken; ik fix ze in een tweede run. Modus A: de jar staat er al.
3. Jar uit `mod/fabric/build/libs/` naar `mods/` van je testserver (Java 25), samen met Fabric API
   en de voice-mod. Server starten; in de console staat dat de standaardbestanden in
   `config/bootcamp/` zijn geschreven.
4. `basis.json` in `config/bootcamp/kits/` zetten.
5. In-game met een tweede account, in deze volgorde. Voor de eerste test mogen alle punten op
   dezelfde plek staan.
   1. `/bc wand`, een regio opslaan, `/bc region show`, `/bc point set`, `/bc point tp`,
      `/bc status`.
   2. `/bc kit horde`, `/bc kit basis`.
   3. `/bc start 1`, dan `/bc stop`; `/bc start 2`, ga dood (tribune, doodtekst), `/bc stop`;
      `/bc start 3`, lever een diamond block in, `/bc stop`.
   4. `/bc uitverkoren <naam>`, `/bc slot <naam> <0-19>` voor beide accounts, `/bc rad`.
   5. In ronde 4: kill de koning (reset, kroon over), laat de koning van een hoogte vallen
      (laatste hit), log uit als koning (30 seconden, kroon over), `/bc stop`.
   6. `/bc start 5`, `/bc start 6`, `/bc reset`.
6. Alles wat niet klopt in één bericht aan mij, met de console-regels erbij.

## Risico's en wat ik dan doe

| Risico | Wat ik doe |
|---|---|
| Netwerk blijft dicht | Modus B: alle code met naamcontrole, kern getest, Fabric-deel alleen op syntax gecheckt en zo gemarkeerd. |
| apt voor JDK 25 faalt | Oracle-tarball naar `/opt`. Faalt dat ook: Modus B voor het Fabric-deel, `core` op JDK 21. |
| Een Minecraft-naam staat niet in de Fabric API-bron | `// TODO 26.2` erbij, opgezocht via de registry op id als dat kan, en in het bouwlog gelijst. |
| Maven Central geeft 429 | Één Gradle-worker, tot drie keer opnieuw met een pauze. |
| Een regel uit de docs is niet te implementeren zoals beschreven | Kleinste werkende interpretatie, genoteerd in het bouwlog en in docs/04. Nooit een spelregel veranderen. |
| Twee ronde-agents raken toch hetzelfde bestand | De package-regel is hard; ik plaats zelf alle registratieregels en los een conflict met de hand op vóór de commit. |
| De sessie valt uit | Elke taak is een gepushte commit; hervatten volgens de procedure hierboven, ongecommit werk wordt weggegooid. |
| De review vindt iets fundamenteels in het raamwerk | Niet fixen in de nacht; als bevinding met voorstel in het bouwlog, jij beslist 's ochtends. |
