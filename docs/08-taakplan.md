# Taakplan: de bootcamp-mod bouwen

Dit is het plan dat Claude Code zonder toezicht uitvoert. Jij leest het, zegt "voer het taakplan
uit", en gaat slapen. 's Ochtends ligt er een gepushte branch met de mod, een bouwlog met wat wel
en niet is geverifieerd, en een testlijst voor op je eigen server.

## Doel

Een server-side Fabric-mod `bootcamp` voor Minecraft 26.2 in de map `mod/` van deze repo, die
alles doet wat in [04-technische-schets.md](04-technische-schets.md) staat: regio's en punten met
een wand en commands, kits uit JSON, de zes rondes, de kroon met reset en bevriezing, het rad, de
tribune voor wie dood is, bossbar en visuals. Spelregels komen uit [02-rondes.md](02-rondes.md)
en [03-kroon-regels.md](03-kroon-regels.md); de docs zijn de spec, niet andersom.

## Voorwaarde vóór je gaat slapen: netwerk

Deze omgeving komt nu wél bij Maven Central en de Gradle-servers, maar **niet** bij de servers van
Fabric en Mojang (403 via de proxy). Zonder die servers kan Gradle Loom de Minecraft-jar, de
mappings en Fabric API niet ophalen, en kan ik de mod niet compileren.

Zet in de instellingen van deze Claude Code-omgeving het netwerkbeleid op volledige toegang, of
voeg deze hosts toe aan de allowlist:

```
maven.fabricmc.net
meta.fabricmc.net
piston-meta.mojang.com
piston-data.mojang.com
launchermeta.mojang.com
libraries.minecraft.net
resources.download.minecraft.net
repo1.maven.org
repo.maven.apache.org
plugins.gradle.org
services.gradle.org
api.foojay.io
```

De laatste is voor het geval 26.2 een nieuwere Java dan 21 vraagt; Gradle haalt dan zelf een JDK
op. Ik check dat bij stap 0.

Het plan werkt in twee modi en kiest zelf bij stap 0:

- **Modus A (netwerk open):** ik compileer na elke taak, fix compile-fouten zelf en lever een
  werkende jar op in `mod/fabric/build/libs/`.
- **Modus B (netwerk dicht):** ik schrijf alle code, verifieer alleen de kernlogica (die geen
  Minecraft nodig heeft) met unit tests, en zet in het bouwlog precies wat niet gecompileerd is.
  Jij draait 's ochtends `./gradlew build` op je eigen machine en plakt de fouten terug; die fix ik
  in een tweede run.

## Wat ik zelf beslis en waar ik van je afblijf

**Zelf:** alle implementatiekeuzes binnen de spec, naamgeving van packages en bestanden,
de JSON-formaten voor kits en waves, kleine toevoegingen aan de docs waar de implementatie iets
concreter maakt (bijvoorbeeld een extra regio die de code nodig heeft). Elke afwijking van de
docs komt in het bouwlog te staan.

**Van jou:** de inhoud van de basiskit (jij levert `basis.json`), alle in-game tests (ik heb
geen Minecraft-server), de bouw van de wereld, en spelregels. Ik verander geen regel uit
docs/02 of docs/03; kom ik iets tegen dat niet kan of tegenstrijdig is, dan kies ik de kleinste
werkende interpretatie en schrijf dat op.

**Nooit:** een pull request openen, pushen naar een andere branch dan
`blessedbyg/dreamy-shannon-psxiuh`, client-side code, of iets aan de voice-mod veranderen.

## Hoe ik het uitvoer

- **Eén commit per taak**, gepusht na elke taak. Gaat het mis, dan staat het werk tot dan toe
  veilig op de branch.
- **Kernlogica los van Minecraft.** Alles wat rekent (regio's, het rad, de timer, ronde-
  overgangen, kit- en config-modellen) komt in een apart Gradle-project `mod/core` zonder
  Minecraft-imports, met JUnit-tests. Dat draait hier altijd, ook in Modus B. Het Fabric-project
  `mod/fabric` gebruikt `core` en bevat alleen de lijm naar Minecraft.
- **Parallel waar het veilig is.** De zes rondes zijn eigen packages die alleen het raamwerk
  gebruiken; die bouw ik met parallelle agents in eigen worktrees en voeg ze daarna samen.
  Raamwerk, config en commands bouw ik sequentieel, want daar raakt alles elkaar.
- **Reviewronde aan het eind.** Onafhankelijke agents leggen de code naast docs/02, 03 en 04
  en zoeken afwijkingen en bugs; wat ze vinden wordt geverifieerd en gefixt, daarna opnieuw
  gecompileerd.
- **Bouwlog** in `mod/BOUWLOG.md`: per taak wat er gedaan is, wat geverifieerd is (compileert,
  tests groen) en wat open staat (in-game test, aanname, afwijking van de docs).

## Taken

Elke taak heeft een klaar-als en een verificatie. Klaar-als geldt voor beide modi; verificatie
verschilt per modus waar dat staat.

### Fase 1: fundament (sequentieel)

**T0. Omgevingscheck.**
Java-versie, Gradle, bereik naar de hosts hierboven, welke Java 26.2 vraagt (uit de Fabric
meta-API als die bereikbaar is). Kies Modus A of B en schrijf het in `mod/BOUWLOG.md`.
Klaar als: het bouwlog bestaat en de modus staat vast.

**T1. Projectskelet.**
`mod/` met `settings.gradle` (projecten `core` en `fabric`; `fabric` wordt overgeslagen met
`-PcoreOnly` zodat de tests ook zonder Loom draaien), Gradle-wrapper, `core/build.gradle`
(Java, JUnit), `fabric/build.gradle` (Fabric Loom, Mojang mappings, Fabric API, dependency op
`core`), `gradle.properties` met de 26.2-versies (uit de meta-API, anders duidelijk gemarkeerde
placeholders), `fabric.mod.json` (`"environment": "server"`, entrypoint `main`), een lege
mixins-config, de entrypoint-klasse, `.gitignore`, `mod/README.md` met bouw- en installatiestappen.
Klaar als: de structuur staat en is gedocumenteerd.
Verificatie: Modus A `./gradlew build` groen; Modus B `./gradlew -PcoreOnly :core:test` groen.

**T2. Kernlogica met tests (`mod/core`).**
Regio (min, max, center, grootte, bevat-punt), Punt (positie plus kijkrichting), configmodel
voor `bootcamp.json` (regio's, punten, doodteksten, slots, uitverkoren) met JSON in en uit,
het rad (stappen tot het doel, willekeurige start en rondes uit een meegegeven bron, ritme per
resterende stap), timer en countdown als tick-state, rollen en rondes als enums, de
overgangsregels (wie gaat waar aan het eind van elke ronde, inclusief "iedereen behalve Clown en
finalist 1"), kit- en wave-modellen als tekst met validatie, het kiezen van een doodtekst.
Klaar als: elke regel uit docs/02 en docs/03 die rekent of beslist, hier staat met een test.
Verificatie: `:core:test` groen, in beide modi.

### Fase 2: raamwerk (sequentieel)

**T3. Config en commands.**
Laden en opslaan van `<wereld>/bootcamp.json` bij server start en stop; het complete
`/bc`-commandboompje uit docs/04 geregistreerd op op-level 2, met nette meldingen voor wat nog
niet bestaat; `/bc reset` als eerste echte implementatie (vlaggen, teams, gamemode, attributes,
inventory, teleport naar `basiskamp`, border, bossbar, kijkers weg).
Verificatie: Modus A compileert.

**T4. Wand, regio's en punten.**
`/bc wand` (stick met custom data component), linksklik en rechtsklik op blokken voor de hoeken,
`/bc region save|show|list|del`, `/bc point set|block|tp|list|del`, `region show` met tien
seconden particles op de randen. Regio `doolhof_uit` toevoegen aan de lijst in docs/04 (de code
heeft een vak nodig om te zien dat iemand uit het doolhof is).
Verificatie: Modus A compileert; regiologica in `core` heeft tests.

**T5. Kits.**
Loader voor `config/bootcamp/kits/*.json` in het formaat uit docs/04, items geparst met de
vanilla item-parser (dezelfde syntax als `/give`), `/bc kit <naam> [<speler>]`, en de
voorbeeldbestanden `horde`, `ei`, `boss`, `kroonpakket`, `arena`, `finale` ingevuld uit docs/02.
`basis.json` blijft leeg met een README ernaast: die komt van Pudding. Een fout in een
kitbestand geeft één regel in de console met bestandsnaam en slot.
Verificatie: Modus A compileert; kitmodel-validatie in `core` heeft tests.

**T6. Spelraamwerk.**
`GameState` (ronde, timer, vlaggen, rol per speler), tags en teams zoals in docs/04
(`hunters` met friendly fire aan), de tick-loop, de bossbar, worldborder uit een regio, poorten
open en dicht, teleports naar verzamelpunten, countdown met titles en geluid,
`/bc start|stop|timer|poort`. Elke ronde is een klasse met `start`, `tick`, `onDeath`, `end`;
in deze taak zijn dat nog lege rondes die alleen teleporteren, border zetten en de timer laten
lopen.
Verificatie: Modus A compileert; timer- en overgangslogica zit in `core` met tests.

**T7. Kijkers en tribune.**
Dood afvangen (`ALLOW_DEATH`), per ronde afhandelen zoals de tabel in docs/04, teleport naar
`tribune_n` of `tribune_horde_n`, geen schade voor kijkers (ook niet van de border), de
tick-check die een kijker uit regio `vloer` of `arena` terugzet, locator bar uit voor kijkers,
de doodtekst als title alleen voor de dode, `/bc kijker <speler> aan|uit`, en aan het eind van
ronde 2 iedereen weer speler bij `v3`.
Verificatie: Modus A compileert.

### Fase 3: de rondes (parallel in worktrees, daarna samenvoegen)

Elke ronde is een eigen package en raakt buiten die package alleen één registratieregel.
Na het samenvoegen compileer ik opnieuw (Modus A) en draai ik alle tests.

**T8. Ronde 1: De Doolhof.**
Start bij `doolhof_start`, border `doolhof`, uitgang via regio `doolhof_uit` naar `v2`, de
eerste vijf krijgen het voorsprongkistje (1 gapple, 1 pearl) en een title, hint-title op 7
minuten, na de timer iedereen naar `v2`.

**T9. Ronde 2: De Horde.**
Waves uit `config/bootcamp/waves.json` (per wave een lijst mobs met aantal, gear en spawnpunt;
de vijf waves uit docs/02 als voorbeeld), spawnen met tag `horde` en `setPersistenceRequired`,
mobteller in de bossbar, volgende wave bij teller 0 of na 120 seconden, ronde stopt bij wave 5
dood, iedereen dood of de timer, pearls voor overlevers, doden weer speler bij `v3`.

**T10. Ronde 3: Het Ei.**
Start bij `ei_start`, border `eibos`, pickaxe-kit erbij, ticketcheck in regio `eiplaat` (diamond
block innemen, ticket, teleport naar `kring`), beacon-hint op 5:00 (`ei_beacon` plaatsen),
vuurpijl boven het Ei op 3:00, na de timer wie geen ticket heeft leeg plus basiskit en naar
`kring`.

**T11. Ronde 4: King of the SMP.**
Het rad (lampen `lamp_0..19`, slots, `uitverkoren`, ritme uit `core`, visuals, drie seconden
later de start), Clown naar `troon` met bosskit en kroon, hunters naar `hunter_1..4`, opstelling
met bevriezing (attributes op 0 en terug, pearls geblokkeerd, countdown) voor 30 en 10
seconden, laatste hit bijhouden, kroonwissel als reset (ex-koning naar de tribune, nieuwe koning
naar `troon` met heal, reparatie, kroonpakket, Resistance, Glowing, helm, team), timer en einde
(0 of geen levende hunter), regeerperiodes in de sidebar, zweefkroon, locator bar alleen voor
koningen, `/bc kroon|rad|uitverkoren|slot`.

**T12. Ronde 5: Arena FFA en de rust.**
Deelnemers: iedereen behalve Clown en finalist 1, ook de doden van ronde 4; spreiden over de
vloer, arenakit, teams weg, countdown, border krimpt na 5 minuten naar 10, dood naar de tribune,
laatste over krijgt de tweede kroon, tiebreak op kills bij 10 minuten, daarna twee minuten rust
met timer in de bossbar en beide finalisten op de tribune.

**T13. Ronde 6: De Finale en de kroning.**
Startpunten `finale_1` en `finale_2`, finalekit, border `finale` (20, na 3 minuten naar 6),
potjes tellen met heal en kit-reset, na twee gewonnen potjes de kroning: iedereen naar de
tribune, winnaar op `kroning`, twintig seconden vuurpijlen, titles.

### Fase 4: afronding (sequentieel)

**T14. Visuals.**
Bossbar-teksten en kleuren per ronde uit de tabel in docs/04, alle geluiden en particles uit de
tabel "per moment", labels boven verzamelpunten en De Kring via `/bc label <tekst>` (zet een
text display op je positie), `zweefkroon` vloeiend.

**T15. Reviewronde.**
Drie onafhankelijke agents: één legt de code naast docs/02 en 03 (klopt elke regel?), één naast
docs/04 (klopt de structuur, staan alle commands en bestanden erin?), één zoekt bugs
(null-paden, ronde-overgangen, wat er gebeurt als een speler uitlogt). Elke bevinding wordt door
een tweede agent tegengesproken; wat overblijft fix ik. Daarna opnieuw compileren en testen.

**T16. Documentatie.**
`mod/README.md` compleet (bouwen, installeren, configbestanden, alle commands, kits, waves),
docs/04 bijgewerkt waar de implementatie afwijkt of concreter is, docs/05 aangevuld met de
in-game testlijst hieronder, `mod/BOUWLOG.md` afgerond.

**T17. Eindrapport.**
Laatste build, alles gepusht, en in de chat een samenvatting: wat er ligt, wat geverifieerd is,
wat jij moet doen, en welke aannames ik heb gemaakt.

## Wat jij 's ochtends doet

1. `git pull` op de branch, lees `mod/BOUWLOG.md`.
2. Modus B: `cd mod && ./gradlew build` op je eigen machine; compile-fouten terugplakken, ik fix.
3. Jar uit `mod/fabric/build/libs/` naar `mods/` van je testserver, samen met Fabric API en de
   voice-mod. Start de server.
4. `basis.json` in `config/bootcamp/kits/` zetten.
5. In-game, in deze volgorde, elk met een tweede account:
   `/bc wand` en een regio opslaan, `/bc region show`; `/bc point set` en `/bc point tp`;
   `/bc kit horde`; `/bc start 1` tot en met `/bc start 6` één voor één met `/bc stop` ertussen;
   een dood in ronde 2 (tribune, doodtekst); het rad met `/bc uitverkoren` en `/bc slot`; een
   kroonwissel; een val-dood van de koning; `/bc reset`.
6. Alles wat niet klopt in één bericht aan mij, met de console-regels erbij.

## Risico's en wat ik dan doe

| Risico | Wat ik doe |
|---|---|
| Netwerk blijft dicht | Modus B. Alle code, kern getest, rest ongecompileerd en zo gemarkeerd. |
| 26.2 vraagt een nieuwere Java dan 21 | Gradle-toolchain laat een JDK ophalen (heeft `api.foojay.io` nodig). Lukt dat niet: Modus B voor het Fabric-deel. |
| Een Minecraft-naam uit mijn geheugen bestaat niet in 26.2 | `./gradlew genSources` en de echte naam opzoeken. In Modus B kan dat niet; dan staat de gok in het bouwlog met een `// TODO 26.2` erbij. |
| Een regel uit de docs is niet te implementeren zoals beschreven | Kleinste werkende interpretatie, genoteerd in het bouwlog en in docs/04. Nooit een spelregel veranderen. |
| Parallelle rondes botsen bij het samenvoegen | Rondes raken buiten hun package alleen de registratie; conflicten los ik met de hand op en ik compileer daarna opnieuw. |
| De sessie valt uit | Elke taak is een gepushte commit; het plan is hervatbaar vanaf de laatste taak in het bouwlog. |
