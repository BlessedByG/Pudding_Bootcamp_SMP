# Bouwlog

Status: modus=A; klaar=T0,T1,T2; bezig=T3

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
