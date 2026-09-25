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
| `ronde2/Horde` + `HordeVerloop` | `ronde3/MobArena` + `MobVerloop`: twee arena's, twee teams tegelijk, knock-out, kooien. Wave-spawnen en de stenen knoop blijven. |
| `ronde3/Ei` | `ronde2/Ei`: punten per block, redstone-gok, emerald-jumpscare. Ticket en drukplaat eruit. |
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
willekeurig), `EiPunten` (punten per block, gelijkspel op wie het eerst), `RedstoneGok` (50/50
uit een meegegeven bron), `Loting` (vier teams naar twee paren), `MobVerloop` (twee arena's,
volgende wave 10 s na beide klaar, na 120 s altijd, eindeloos met `extraPerWave`, winnaar bij
leeg team met de tiebreaks uit docs/04), `ClownRegels` (wie mag wie raken, einde bij één over,
kroonopvolger), `PvpRegel` (de tabel uit docs/04), `LootTabel` (gewogen trekken, aantal per
kist), de quiz-randomizer als `Rad` met 4 slots en een willekeurig doel, uitlog- en join-regels
per ronde. `REGELS.md` herschreven. Oude regels (voorsprong, ticket, finale, rust) eruit.
Verificatie: `:core:test` groen.

**N2. Raamwerk: rondes, teams, PvP.**
`Rondes`, `Spel` en `Tribune` naar de nieuwe nummering. `Teams` met de nieuwe teams, teamkeuze in
`bootcamp.json`, `/bc team`. De PvP-regel in `ALLOW_DAMAGE`. Verzamelpunten per ronde uit docs/04
(`Tribune.verzamelpuntNa`). De oude rondeklassen verhuizen naar hun nieuwe nummer als lege
rondes die alleen teleporteren en border zetten, zodat alles compileert. `Finale`,
`/bc finalist`, `finale.json`, `horde.json` eruit; `mobarena.json`, `jager.json` erbij. `/bc
reset` ruimt ook de teamkeuzes op. `/bc status` toont het team.
Verificatie: `check.sh`, build.

**N3. Resource pack en de jumpscare.**
`pack/` zoals in docs/04 met een placeholder-afbeelding en een placeholder-geluid (een stil ogg
van een seconde, want ik maak geen audio), `pack/bouw.sh` (zip + SHA-1). `schrik/Schrik`: de title
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
Sidebar met de teams.

**N6. Ronde 2: Het Ei.**
`ei.json` met de diamond pickaxe, survival, block-breken met punten zonder drop, redstone-gok
(Haste of bevriezing met Mining Fatigue, een nieuwe vervangt de oude), emerald-jumpscare bij een
ander, actionbar en top-10-sidebar, beacon-hint en vuurpijl, winnaar bij de timer, naar `v3`.
Dood = terug naar `ei_start`.

**N7. Ronde 3: De Mob Arena.**
Loting, wedstrijden na elkaar (de volgende start vanzelf 15 seconden na de vorige, met een
title ertussen), waves in beide arena's tegelijk met `arena_a`/`arena_b`-tags,
kooien, dood blijft dood tot het einde van de ronde, bewaarde inventory, tiebreaks, finale met
alleen de levenden, `/bc wave volgende`, winnaar, einde met iedereen levend en spullen terug.
`waves.json` omgezet naar aantallen voor een team van 5 met `extraPerWave`.

**N8. Ronde 4: De Quiz.**
Iedereen naar zijn vak, border `quiz`, geen schade. `/bc quiz draai` met de randomizer over
`quizlamp_0..3`, `/bc quiz punt`, `/bc quiz winnaar`, sidebar, titles in de teamkleur.

**N9. Ronde 5: Clown vs All.**
Het Rad start ronde 5. Iedereen uit zijn teamkleur, `boss.json` en `jager.json`, opstelling 30
seconden, PvP alleen met de kroonhouder, geen timer, kroonwissel als reset, uitlog-wacht, einde
bij één over, winnaar, `/bc krimp`, locator bar, regeerperiodes in de sidebar.

**N10. Ronde 6: De FFA en de kroning.**
Iedereen behalve de uitverkorene, `arena.json`, geen team, countdown, border-krimp, tiebreak op
kills na 10 minuten, dan de kroning.

### Fase 3: afronding

**N11. Visuals.**
Bossbar-teksten en sidebar per ronde uit docs/04, alle geluiden uit de tabel "per moment",
vuurwerk voor elke winnaar.

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
2. De foto van Clown (vierkant) en het lachje (ogg) in `pack/` zetten, `pack/bouw.sh`, zip online
   als GitHub-release, URL en SHA-1 in `server.properties`.
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
| Een wedstrijd in de mob arena houdt nooit op | `extraPerWave` omhoog in `waves.json`, of `/bc wave volgende`. |
| Een regel uit de docs is niet te bouwen zoals beschreven | Kleinste werkende interpretatie, in het bouwlog en docs/04. Nooit een spelregel veranderen. |
