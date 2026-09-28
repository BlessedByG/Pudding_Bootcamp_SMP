# Taakplan 2: de mod ombouwen naar het nieuwe rondeplan

Het eerste taakplan (T0 t/m T17) is op 19 september 2026 uitgevoerd en bouwde de mod voor het
oude rondeplan. Dat plan staat in de git-geschiedenis tot commit `b93727a`; wat er toen gebouwd
is staat in [mod/BOUWLOG.md](../mod/BOUWLOG.md). Dit plan bouwt die mod om naar het rondeplan van
25 september: doolhof, Ei, mob arena, quiz, Clown vs All, FFA.

De spec is [02-rondes.md](02-rondes.md), [03-kroon-regels.md](03-kroon-regels.md) en
[04-technische-schets.md](04-technische-schets.md). De docs zijn de spec, niet andersom.

## Uitgangspunt

- Lokaal op Windows, JDK 25, netwerk open: **Modus A**. Elke taak compileert tegen de echte
  26.2-jar met `./gradlew build`. Geen globale Gradle, altijd de wrapper in `mod/`.
- Werken op `main`, één commit per taak. Pushen alleen als jij dat vraagt.
- **De bestaande mod is nog nooit in-game gedraaid.** Het raamwerk (config, wand, commands,
  kits, tribune, poorten, border, bossbar, opstelling, kroonwissel) blijft staan en wordt
  hergebruikt. Daarom eerst N0.

## Wat blijft, wat verandert, wat verdwijnt

| Nu in de code | Wordt |
|---|---|
| `ronde1/Doolhof` | Ombouwen: startruimte, nep-uitgangen, schrikplekken, poort na 4 min, kisten vullen, teammenu. Voorsprongkistje eruit. |
| `ronde2/Horde` + `HordeVerloop` | `ronde3/MobArena` + `MobSchema` + `MobVerloop`: twee arena's tegelijk, per beurt van elk team één speler per arena, 5 waves, punten per kill, kooi in het midden. Wave-spawnen en de stenen knoop blijven; het bewaren van de inventory gaat eruit (na de ronde levert iedereen alles in). |
| `ronde3/Ei` | `ronde2/Ei`: zwevend Ei vastleggen en terugzetten, puntenblokken strooien, punten per block, redstone-gok, emerald-jumpscare. Ticket, drukplaat en beacon-hint eruit. |
| `ronde4/King` + `crown/` + `rad/` | `ronde5/ClownVsAll`: geen timer, PvP alleen met de kroonhouder, einde bij één over. Kroonwissel, opstelling, uitlog-wacht en het Rad blijven. |
| `ronde5/Ffa` | `ronde6/Ffa`: iedereen behalve Clown, daarna meteen de kroning. Rust en finalist 2 eruit. |
| `ronde6/Finale` | Verdwijnt, behalve de kroning die naar `ronde6/Ffa` gaat. `finale.json`, `/bc finalist` eruit. |
| `Teams` (spelers, hunters, king, out) | spelers, rood, blauw, groen, geel, jagers, kroon, out; teamkeuze bewaard in `bootcamp.json`. |
| – | Nieuw: `teams/Teammenu`, `schrik/Schrik`, `ronde4/Quiz`, de PvP-regel, `pack/`. |

## Wat ik zelf beslis en waar ik van je afblijf

**Zelf:** implementatiekeuzes binnen de spec, namen van packages en bestanden, de JSON-formaten,
kleine concretiseringen van docs/04 waar de code iets nodig heeft. Elke afwijking komt in het
bouwlog én in docs/04.

**Van jou:** de foto van Clown en het lachje, de quizvragen, alle in-game tests, de bouw van de
wereld, en spelregels. Kom ik iets tegen dat niet kan of tegenstrijdig is, dan kies ik de
kleinste werkende interpretatie en schrijf dat op.

**Nooit:** client-side code, iets aan de voice-mod veranderen, het raamwerk herschrijven, of een
spelregel veranderen.

## Hoe ik het uitvoer

- Na elke taak `mod/check.sh` en `./gradlew build` groen, bouwlog bijgewerkt
  (statusregel `Status: plan=2; klaar=N0,N1,...; bezig=Nn`), één commit.
- **Kernlogica in `core`** met JUnit-tests: alles wat rekent of beslist. `mod/core/REGELS.md`
  krijgt een nieuwe lijst regels met per regel een test; regels van het oude plan die vervallen
  gaan eruit.
- **Rondes sequentieel**, niet parallel: ze delen de nieuwe teams, de PvP-regel en de
  jumpscare, en er zijn er maar zes.
- **Reviewronde aan het eind**, begrensd zoals in plan 1: maximaal tien bevindingen per agent,
  alleen hoog en midden fixen, één fixronde.

## Taken

### Fase 0: basis controleren

**N0. Rooktest van de bestaande mod (jij).**
De checklist "eerste test van de mod" uit het oude draaiboek (git `b93727a`, docs/05) voor het
raamwerk: jar laadt, wand, regio's, punten, `/bc kit basis`, een ronde starten en stoppen, dood
gaan en op de tribune komen, `/bc reset`. Fouten in één bericht terug. Wat hier stuk is, is straks
in elke ronde stuk; daarom eerst.
*Mag ook parallel aan N1 t/m N3, want die raken het raamwerk nauwelijks.*

### Fase 1: fundament

**N1. Kern: de nieuwe regels met tests.**
`Ronde` opnieuw: DOOLHOF(1), EI(2), MOBARENA(3), QUIZ(4), CLOWN(5), FFA(6) met duur, survival en
vereiste regio's en punten uit docs/04. `Rol`: SPELER, JAGER, KROON, FFA, KIJKER, STAFF. Nieuw in
`core`: `TeamKeuze` (maximum `max(5, ceil(n/4))`, vol-check, kleinste team bij gelijk
willekeurig), `EiPunten` (punten per block, gelijkspel op wie het eerst), `EiVerdeling` (k
plekken zonder dubbele trekken uit de deepslate-plekken, weigeren als k te groot is),
`RedstoneGok` (50/50 uit een meegegeven bron), `MobSchema` (beurten = grootste team, per team
een gelote volgorde, arena 2 verschoven met ⌊n/2⌋, extra beurten voor kleinere teams, wie af is
speelt niet meer), `MobVerloop` (twee arena's, 5 waves, volgende wave 5 s na beide klaar, na
120 s altijd, arena klaar na wave 5 of als al haar spelers af zijn), `MobPunten` (punten per
type, teamstand, tiebreak op kills), `ClownRegels` (wie mag wie raken, einde bij één over,
kroonopvolger), `PvpRegel` (de tabel uit docs/04), `LootTabel` (gewogen trekken, aantal per
kist), het quiz-rad als `Rad` met 16 slots (elk team 4 keer, buren nooit gelijk, ook rondom) en
een willekeurig doel, `QuizStand` (goed +1, fout wist de beurt, winnaar of gelijkspel), uitlog- en
join-regels
per ronde. `REGELS.md` herschreven. Oude regels (voorsprong, ticket, finale, rust) eruit.
Verificatie: `:core:test` groen.

**N2. Raamwerk: rondes, teams, PvP.**
`Rondes`, `Spel` en `Tribune` naar de nieuwe nummering. `Teams` met de nieuwe teams, teamkeuze in
`bootcamp.json`, `/bc team`. De PvP-regel in `ALLOW_DAMAGE`. Verzamelpunten per ronde uit docs/04
(`Tribune.verzamelpuntNa`). De oude rondeklassen verhuizen naar hun nieuwe nummer als lege
rondes die alleen teleporteren en border zetten, zodat alles compileert. `Finale`,
`/bc finalist`, `finale.json`, `horde.json` eruit; `jager.json` erbij. `/bc
reset` ruimt ook de teamkeuzes op. `/bc status` toont het team.
**Commando's per ronde** zoals in docs/04: `/doolhof`, `/ei`, `/mobarena`, `/quiz`, `/clown`,
`/ffa`, elk met `start` en `stop` (en `resterend` voor rondes met een timer). `/bc start`, `stop`,
`timer`, `poort`, `rad`, `kroon`, `uitverkoren` en `slot` verhuizen naar hun ronde; `/bc` houdt
alleen de algemene commando's. **Regio's met meerdere delen**: `Regio` in `core` wordt een
lijst dozen (met tests voor bevat, overlap en de doos om alles heen), `/bc region add`, en
`bootcamp.json` leest de oude vorm met één doos nog in. Instellingen per ronde in
`bootcamp.json`: de timers van doolhof en Ei, de poort en hint van het doolhof, de aantallen van
het Ei en de punten van de mob arena.
Verificatie: `check.sh` (met de nieuwe command-literals), build.

**N3. Resource pack en de jumpscare.**
`pack/` zoals in docs/04 met een placeholder-afbeelding en een placeholder-geluid (een stil ogg
van een seconde, want ik maak geen audio), `pack/BouwPack.java` (jpg of png van elk formaat uit
`pack/aanleveren/` naar png, langste kant hooguit 1024 met behoud van verhouding, lachje erbij,
de 64 plaatjes van het ronde quiz-rad getekend met Java2D plus font `bootcamp:rad`, zip + SHA-1;
draait met `java pack/BouwPack.java`, zonder extra software). `schrik/Schrik`: de title
met de glyph in font `bootcamp:schrik` plus het geluid, alleen voor die speler. `/bc schrik
<speler>`. In `mod/README.md` hoe je het pack online zet en `server.properties` invult.
Verificatie: `check.sh`, build; zip bevat de juiste paden. In-game: jij.

**N4. Het teammenu.**
`teams/Teammenu`: `ChestMenu` met `MenuType.GENERIC_9x1`, vier wolblokken met naam en aantal,
grijs als vol, elke klik geannuleerd en zelf afgehandeld, keuze → team, chatregel, levelup,
teleport naar `v2`. Opnieuw openen na sluiten zolang je in `doolhof_uit` staat.
Verificatie: `check.sh`, build; de vol-regel zit in `core`.

### Fase 2: de rondes

**N5. Ronde 1: De Doolhof.**
Startruimte, basiskit, kisten vullen uit `doolhof_loot.json` (standaardbestand meeleveren), poort
na 4 minuten met bossbar ervoor en erna, nep-uitgangen (particles, geluid, grapje uit de config,
terug naar start), schrikplekken (één keer per regio per speler), teammenu bij de uitgang, hint,
timer op: kleinste team voor wie geen team heeft, iedereen naar `v2`. Dood = terug naar start.
Sidebar met de teams. `/doolhof timer` (standaard 15), `/doolhof poort <minuten>` (standaard 4)
en `/doolhof hint` (standaard 10) met de grenzen uit docs/04, live toegepast als het doolhof
loopt; de grenscheck zit in `core` met tests. `/doolhof hinttekst` (eigen tekst, anders de
windrichting), title `DOOLHOF VOORBIJ` met de uitkomst, en de welkomsttitle bij het joinen
buiten een ronde.

**N6. Ronde 2: Het Ei.**
`/ei vastleggen` (doos `ei` naar `bootcamp_ei.nbt`), bij de start het Ei terugzetten verspreid
over ticks en de puntenblokken strooien op gewone deepslate (`/ei blokken`), ook terugzetten in
het reset-register. Spelers om en om over `ei_spawn_n`. `ei.json` met de diamond pickaxe, die
de mod `custom_data={bootcamp_ei:1b}` geeft en aan het einde (ook bij stop, reset en inloggen
buiten ronde 2) weer weghaalt,
survival, alleen binnen `ei` breken en alles zonder drop, nergens neerzetten, punten per block,
redstone-gok (Haste of bevriezing met Mining Fatigue, een nieuwe vervangt de oude),
emerald-jumpscare bij een ander (met `Jumpscare naar …` en `Met dank aan …`), netherite als
chatregel voor iedereen, aftellen van Haste en bevriezing in de actionbar, actionbar met punten
en plek (elke seconde), top-10-sidebar, winnaar bij de timer, naar `v3`.
Dood = terug naar je eigen startplek. `/ei timer` (standaard 15). Beacon, ticket en drukplaat
eruit.

**N7. Ronde 3: De Mob Arena.**
Het schema uit `MobSchema` blijft geheim (niet in de chat); `/mobarena schema` laat het alleen
aan wie het typt zien. `/mobarena startplek <arena>
<kleur>` zet `start_<arena>_<kleur>`. Per beurt de ingeplande spelers naar de startplek in hun
teamkleur, geheald, geen kit; de rest op de tribune met hun spullen,
zonder schade. 5 waves in beide arena's tegelijk met `arena_1`/`arena_2`-tags, om en om over
`mob_<arena>_n`. Kills toeschrijven (laatste klap, ook projectiles) en punten naar het team uit
`/mobarena punten`. Mobs laten kijkers en wachtenden met rust (`setTarget(null)` elke tick).
Glowing in teamkleur tijdens de beurt, rookwolk op de spawnpunten bij elke wave, evoker- en
ravagerkills in de chat, `/mobarena aftekst` in de actionbar na een dood.
Dood = kooi van je arena tot het einde van de beurt, dan tribune, af voor de rest van de ronde,
inventory leeg. Extra beurten bij de start van de beurt geloot. Na elke beurt meteen de tekst,
10 seconden aftellen, dan alleen de arena's en kooien naar de tribune (de tribune blijft staan),
dan wachten op `/mobarena volgende`. `/mobarena wave volgende`, sidebar met de teamstand,
actionbar na een kill, winnaar (tiebreak op kills), 10 seconden vieren, dan levert iedereen alles
in (inventory, armor, offhand leeg) en gaat geheald naar de quiz. `waves.json` omgezet naar 5
waves voor 4 spelers per arena, zonder schaling.

**N8. Ronde 4: De Quiz.**
`/quiz presentator`, `/quiz bank <kleur>`, `/quiz lamp <kleur>`, `/quiz podium`. De lamp bij
de bank van het team aan de beurt brandt (aan bij de landing, uit bij fout en bij een nieuwe
draai, de winnaar brandt tijdens het vieren, alles uit bij einde, stop en reset). Iedereen zonder spullen naar zijn
bank, de presentator naar het podium met als enige de drie items (groene wol, rode wol, nether
star) die rechtsklik afvangen en nooit worden neergezet of kwijtraken. Het ronde quiz-rad in beeld
(title met de glyph van de stand, 64 standen, afremmend, landt op een vakmidden), `/quiz goed`, `/quiz fout`,
`/quiz draai`, `/quiz punt`, `/quiz einde` en `/quiz winnaar`, de reeks (`3 op rij`) vanaf twee
op rij, de hulpregel voor de presentator in de actionbar, `GELIJKSPEL` bij een gelijke stand,
sidebar en bossbar, titles in de
teamkleur, 10 seconden vieren, dan de items van de presentator weg en iedereen naar de Arena.

**N9. Ronde 5: Clown vs All.**
Het Rad in beeld als een rij spelerskoppen met namen (tekstcomponent `object` met een
spelerskop; n = aantal deelnemers, rigged op de uitverkorene), met iedereen op de tribune; koppen
ook bij `DE KROON`, `NIEUWE KROON` en de winnaar; `/clown slot`, De Kring en de lampen eruit. Daarna de
vloer op: `/clown troon`, `/clown jagerplek`, `/clown tribune`, jagers willekeurig over
`jager_1..n` met kijkrichting. `/clown vloer <diameter>` maakt `vloer` als cilinder (midden,
doorsnede, 5 hoog vanaf de voeten; `Regio` in `core` krijgt een cilindervorm met tests) en
kijkers erin gaan terug naar de tribune. Iedereen uit zijn teamkleur, `boss.json` en `jager.json` (standaard dezelfde
full-diamond kit met maximale enchants en 16 gapples, `boss.json` zonder helm), de kroon als
diamond helm met Curse of Binding. Iedereen bevroren (ook bogen, crossbows, tridents, pearls)
tot `/clown go`, dan 10 seconden; na een kroonwissel loopt de countdown vanzelf. PvP alleen met
de kroonhouder, geen timer, kroonwissel als reset, uitlog-wacht, einde bij één over, winnaar,
`/clown krimp` (met `DE BORDER KRIMPT`), `/clown start` zonder rad, locator bar, regeerperiodes
in de sidebar.
`/clown wachttekst` in de actionbar tijdens het wachten, `Jij hebt de kroon · n jagers` voor de
kroonhouder, een chatregel bij elke speler die af is, `DE EINDBAAS WINT` als de uitverkorene
wint.

**N10. Ronde 6: De FFA en de kroning.**
In de Arena van ronde 5. Iedereen behalve de uitverkorene willekeurig over `jager_1..n` met
kijkrichting, de uitverkorene naar de tribune. `arena.json` = de kit van `jager.json` met 32
gapples. Geen team. Bevroren (ook schieten en pearls) tot `/ffa go`, dan 10 seconden. Geen
timer: tot er één over is. `/ffa krimp <grootte> [<seconden>]`. Dan de kroning op `troon`,
iedereen naar de tribune, twintig seconden vuurwerk, title met kop. Het punt `kroning` en de oude
finale eruit. `/ffa wachttekst`, sidebar `Kills`, chatregel bij elke speler die af is,
`DE BORDER KRIMPT` bij `/ffa krimp`, `LAATSTE DRIE` en `LAATSTE TWEE`, en na de kroning de
bossbar `Pudding Bootcamp · King: <naam>` en de zweefkroon tot `/bc reset`.

### Fase 3: afronding

**N11. Visuals.**
Bossbar-teksten en sidebar per ronde uit docs/04, alle geluiden uit de tabel "per moment",
vuurwerk voor elke winnaar. De laatste minuut van het doolhof en het Ei: bossbar rood, de
laatste 10 seconden groot aftellen met een tik.

**N12. Reviewronde.**
Drie agents: code naast docs/02 en 03, code naast docs/04 (commands, regio's, punten, kits,
reset-register compleet), bugs (ronde-overgangen, uitloggen, teams na een herstart, PvP-lekken).
Alleen hoog en midden fixen, één ronde.

**N13. Documentatie en eindrapport.**
`mod/README.md` en `mod/BOUWLOG.md` bij, docs/04 waar de code afwijkt, de checklist in docs/05
nagelopen. Samenvatting in de chat: wat er ligt, wat geverifieerd is, wat jij moet doen.

## Wat jij daarna doet

1. De jar uit `mod/fabric/build/libs/` naar je testserver (Java 25), met Fabric API en de
   voice-mod.
2. De foto van Clown (jpg of png, elk formaat) en het lachje (ogg) in `pack/aanleveren/` zetten,
   `java pack/BouwPack.java`, zip online als GitHub-release, URL en SHA-1 in
   `server.properties`.
3. De checklist "eerste test van de mod" uit [05-draaiboek.md](05-draaiboek.md), met een tweede
   account. Alles wat niet klopt in één bericht terug, met de console-regels.

## Risico's en wat ik dan doe

| Risico | Wat ik doe |
|---|---|
| N0 laat zien dat het raamwerk stuk is | Eerst dat fixen, dan pas verder met fase 2. |
| Het kistmenu laat toch items verplaatsen (shift-klik, sleep, nummertoetsen) | Elke `clicked`-variant annuleren en het menu elke tick terugzetten; de keuze gaat via de slot-index. |
| De jumpscare-foto is te klein of verkeerd geplaatst | `height` en `ascent` in `schrik.json` zijn de knoppen; afstemmen in jouw eerste test. |
| Netherite breken wordt geblokkeerd door adventure-regels | Het Ei is survival; de punten gaan via het break-event, dus het werkt met elke pickaxe die het block aankan. |
| Twee arena's met mobs geven lag | Aantallen per team van 5 zijn klein; view distance 8; testen in de testrun. |
| Mobs blijven toch om de kooi of de tribune hangen | De target-check elke tick strenger maken (ook `NearestAttackableTargetGoal` filteren); tot die tijd `/mobarena wave volgende`. |
| Een regel uit de docs is niet te bouwen zoals beschreven | Kleinste werkende interpretatie, in het bouwlog en docs/04. Nooit een spelregel veranderen. |
