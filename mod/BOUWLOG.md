# Bouwlog

Status: modus=A; klaar=T0; bezig=T1

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
