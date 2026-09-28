# Keuzes

Op 25 september 2026 is het rondeplan omgegooid. De besluiten daarvan staan hieronder, en daarna
welke besluiten van 19 september nog gelden. De oude lijst staat in de git-geschiedenis (tot
commit `b93727a`).

## Het nieuwe rondeplan (25 september)

| # | Keuze | Besluit | Gevolg |
|---|---|---|---|
| 1 | Volgorde | Doolhof (solo), Ei (solo), Mob Arena (team), Quiz (team), Clown vs All (solo), FFA (solo) | De horde, King of the SMP en de 1v1-finale vervallen. |
| 2 | Teams | 4 kleuren, max 5 per team, gekozen bij de uitgang van het doolhof | Wie het eerst buiten is kiest het eerst. Na de timer gaat wie nog geen team heeft naar het kleinste. |
| 3 | Clown in de teamrondes | Gewoon teamlid | Kiest ook een kleur. Gaat pas solo bij Clown vs All. |
| 4 | Basiskit | Iron armor, iron sword, 32 steak | Start van het doolhof, inventory eerst leeg. |
| 5 | Gear (herzien 28 september) | Doolhof-gear blijft tot het einde van de mob arena | Dan levert iedereen alles in: de quiz doe je zonder spullen, alleen Pudding krijgt zijn quiz-items. Bij Clown vs All iedereen dezelfde kit, Clown ook (alleen zijn helm is de kroon). In de FFA weer een gelijke kit. |
| 6 | Winnaars | Ei (1 speler), Mob Arena (1 team), Quiz (1 team), Clown vs All (1 speler), FFA (1 speler) | Het doolhof heeft geen winnaar. Geen totaalstand over de avond. |
| 7 | Doolhof | Start in het midden, 4 gangen, 1 echt, 3 nep; uitgang open na 4 minuten | Nep-uitgang: knal, grapje, terug naar start. Kisten door de mod gevuld uit een loot-tabel. |
| 8 | Jumpscare | Foto van Clown met lachje | Server resource pack nodig. In het doolhof op vaste plekken, in het Ei via het emerald block. |
| 9 | Ei-punten | Netherite 50, diamond 10, gold 5 | Tellen bij het breken, block valt niet. Diamond pickaxe nodig voor netherite. |
| 10 | Redstone block | 50/50: zelf 10 s Haste, of iedereen behalve jij 15 s stil | Stil betekent: niet lopen en niet minen. |
| 11 | Emerald block | Jumpscare bij een willekeurige ander | – |
| 12 | Mob arena (herzien 28 september) | Twee gespiegelde arena's tegelijk, per beurt van elk team één speler per arena, 5 waves per beurt, punten per mob-kill | Iedereen speelt twee beurten (één per arena), schema geloot door de mod, commander start elke beurt. Geen kit, geen eten. Meeste punten wint, gelijk: meeste kills. Kleinere teams krijgen gelote extra beurten. |
| 13 | Dood in de mob arena (herzien 28 september) | Kooi in het midden van je arena, daarna de tribune | Wie doodgaat speelt geen beurt meer; zijn plek blijft dan leeg. Punten blijven staan. Mobs negeren kijkers. |
| 14 | Quiz (herzien 28 september) | Pudding presenteert vanaf het podium in de quizhal, leest voor en keurt met groene en rode wol | Het rad is een echt rond rad in beeld (plaatjes uit het resource pack, alleen kleuren, pijltje bovenin): 16 vakken, elk team 4 keer, echt toeval. Bij elke bank een lamp die brandt als dat team aan de beurt is. Goed = +1 punt en hetzelfde team door; fout = opnieuw draaien. Meeste punten wint (`/quiz einde`). Pudding speelt in de quiz niet mee, zijn team heeft één speler minder. Vragen niet in beeld, alleen voorgelezen. |
| 15 | Clown vs All | Rigged Rad kiest Clown, friendly fire uit, geen timer | Alleen de kroonhouder kan doden en geraakt worden. Wie de kroonhouder killt krijgt de kroon, reset met de overlevenden. Laatste over wint. |
| 16 | Rad geheim | Alleen de admins weten het | Nooit vertellen. |
| 17 | FFA (herzien 28 september) | Iedereen behalve Clown, in de Arena op de 20 startplekken | Ook wie af was in Clown vs All. Dezelfde kit als Clown vs All met 32 gapples. Stil tot `/ffa go`, dan 10 seconden. Geen timer: tot er één over is. Border krimpt alleen met `/ffa krimp`. De winnaar is King of the SMP Bootcamp, kroning meteen daarna op het podium. |
| 18 | Kijkers | Tribune, nooit spectator mode | In de mob arena de kooi tot het einde van de beurt waarin je sneuvelt. |
| 19 | Duur doolhof (28 september) | 15 minuten, poort na 4, hint na 10 | Alle drie instelbaar met `/doolhof timer`, `/doolhof poort` en `/doolhof hint`, bewaard in `bootcamp.json`. |
| 20 | Commands (28 september) | Per ronde een eigen commando: `/doolhof`, `/ei`, `/mobarena`, `/quiz`, `/clown`, `/ffa` | `/bc` houdt alleen setup, spelers en noodknoppen. `/bc start`, `stop`, `timer`, `poort`, `rad`, `kroon` en de rest verhuizen naar hun ronde. |
| 21 | Timer Ei (28 september) | Ei 15 minuten, instelbaar met `/ei timer` | De FFA heeft geen timer meer (zie 17). |
| 22 | Het Ei (28 september) | Een zwevend Ei van ±30 x 30 x 60 aan kettingen, binnenkant deepslate | Geen zoeken meer: beacon, vuurpijl en nep-eitjes vervallen. Start aan het eind van de kettingen (`ei_spawn_n`). De mod zet het Ei bij elke start terug (`/ei vastleggen`) en strooit de puntenblokken willekeurig in de deepslate (`/ei blokken`). Alleen het Ei is te breken, niets valt als item, neerzetten kan nergens. |
| 23 | Regio's met meerdere delen (28 september) | `/bc region add` voegt een selectie toe aan een regio | Voor de T-vormige mob-arenavelden. Geldt voor elke regio. |
| 24 | Kit Clown vs All (28 september) | Iedereen dezelfde kit, ook Clown: full diamond, maximaal enchant, zwaard, bijl, boog met pijlen, schild, 16 gapples | De kroon is een diamond helm met dezelfde enchants plus Curse of Binding; de kroonhouder is dus niet zwakker. Kroonpakket (2 gapples, 2 pearls) blijft bij elke wissel; geen Resistance voor de nieuwe kroonhouder. Voor het Rad staat iedereen op de tribune. |
| 25 | Arena en start van Clown vs All (28 september) | Open vloer, podium in het midden, 20 redstone blocks als jagerplekken, alleen de onderste tribunering | Het Rad staat in beeld als een rij spelerskoppen met namen (geen De Kring met pilaren, geen `/clown slot`). Jagers willekeurig over de 20 plekken, met kijkrichting. Iedereen stil, ook Clown, tot `/clown go`: 10 seconden en dan tegelijk los, geen voorsprong. Na een kroonwissel loopt de countdown vanzelf. Stilstaan blokkeert ook schieten. De vloer is een cirkel van 5 hoog, gezet met `/clown vloer <diameter>`; kijkers die erin komen gaan terug naar de tribune. Tribuneplekken met `/clown tribune`. |
| 26 | Spelerskoppen in beeld (28 september) | Koppen met de echte skin naast de naam, via tekst, zonder resource pack | In Het Rad van Clown vs All (een rij koppen die langs een pijltje schuift), bij DE KROON, NIEUWE KROON, de winnaar van het Ei en van Clown vs All, en bij de kroning. |
| 27 | Visuals per ronde (28 september) | Alle momenten per ronde doorgelopen met schetsen | Staat per moment in docs/04 onder *Bossbar en visuals*. Teksten die je wilt aanpassen (hint, af-tekst, wachttekst) hebben een command. |

## Wat van 19 september nog geldt

| Keuze | Besluit |
|---|---|
| Techniek | Eigen server-side Fabric-mod op 26.2, gevibecode met Claude Code. Spelers hebben alleen de voice-mod nodig (plus nu het resource pack, dat de server zelf aanbiedt). |
| Spelers | 20, Clown inbegrepen. Alles schaalt mee. |
| Prijs | Geen. Just for fun. |
| Overgangen | Teleporteren. De commander start elke ronde op het sein van Pudding. |
| Doodtekst | Alleen voor de dode, lijst in de config: "Grote L gepakt!", "Had je nou maar beter je best gedaan", "Gelukkig is dit niet de CSMP". |
| Kijkers | Geen spectator mode, geen tp-items. Adventure, geen schade, blijven van het veld. |
| Bouwen | Nergens, behalve minen in het Ei. |
| Voice | Puur proximity, 48 blokken, geen groepen, de tribune is hoorbaar voor iedereen. |
| Kroon | Gaat naar de killer; zonder killer de laatste hit, anders willekeurig. Elke wissel is een reset. |

## Praktisch geregeld

- Fabric Loader, Fabric API en de voice-mod zijn er voor 26.2.
- Testen op Pudding's eigen server: jar bouwen, kopiëren, herstarten.
- De basiskit is binnen (25 september) en zit in de jar.
- Geen SMP-aankondiging bij de kroning.

## Nog open (praktisch)

- De foto van Clown en het lachje voor het resource pack.
- De quizvragen (Pudding).
- Hoeveel puntenblokken er in het Ei komen (voorlopig 6 / 90 / 120 / 10 / 10, afstemmen in de
  testrun met `/ei blokken`).
- De precieze maat van het Ei (±30 x 30 x 60).
- De aantallen in de 5 waves van de mob arena en de punten per mob (afstemmen in de testrun).
- De grapjes voor de nep-uitgangen (er staan er drie als voorbeeld in de config).
- Datum en starttijd, wie host, commander, ref en camera is.
- De spelerslijst met 20 namen.
- Datum van de testrun en wie er bouwt.
