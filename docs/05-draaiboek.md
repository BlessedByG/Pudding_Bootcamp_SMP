# Draaiboek

Tijdschema, rollen en checklists voor de avond zelf. Tijden zijn een voorbeeld met start om 20:00.

## Tijdschema

| Tijd | Wat | Wie |
|---|---|---|
| 19:15 | Staff online. Wereldbackup maken. `/bc reset` draaien. Poort, teleports en `/bc schrik` op een testaccount even testen. | Admins |
| 19:40 | Whitelist open. Spelers spawnen in het basiskamp en krijgen het resource pack. Voice-test: iedereen zegt wat, loopt weg en komt terug. | Iedereen |
| 19:55 | Host legt de regels uit, zonder Clown vs All te verklappen. | Host |
| 20:00 | **Intro op stream.** `/doolhof start`: iedereen naar de startruimte van het doolhof. Op het sein `/doolhof go`: countdown. | Host, Admin 1 |
| 20:02 | **Ronde 1: De Doolhof** (15 min, uitgang open na 4 min, hint na 10 min, teamkeuze bij de uitgang) | |
| 20:18 | Iedereen in de finishruimte van het doolhof: teams in beeld, praatje. Dan `/doolhof naarei`: iedereen naar V2 bij het Ei. | Host, Admin 1 |
| 20:20 | **Ronde 2: Het Ei** (15 min) | |
| 20:36 | Winnaar van het Ei op het podium bij de mob arena, de rest op het plein. De winnaar pakt het Warden-ei uit het frame (of Pudding reikt het uit). | Host, Pudding |
| 20:38 | **Pauze** (5 min). | Host |
| 20:43 | **Ronde 3: De Mob Arena**: 5 beurten van 5 waves (wie wanneer speelt blijft een verrassing), elke beurt op het sein van Pudding (± 25 min) | Host, Admin 1 |
| 21:08 | Winnend team. Naar de quiz. | Host |
| 21:10 | **Ronde 4: De Quiz** (± 15 min, Pudding presenteert en bepaalt) | Pudding, Admin 1 |
| 21:25 | Winnend team. Naar de Arena. Host legt King of the Hill uit (zo heet de ronde in beeld; zeg nooit "Clown vs All"). "Het lot beslist wie de kroon krijgt." Het Rad landt op Clown. | Host, Admin 1 |
| 21:28 | **Ronde 5: King of the Hill** (intern Clown vs All; iedereen stil tot `/clown go`, dan 10 sec countdown, geen timer, reken op 10 tot 20 min) | Admin 1 |
| 21:45 | Winnaar. Iedereen behalve Clown de vloer op. | Admin 1 |
| 21:47 | **Ronde 6: De FFA** (iedereen stil tot `/ffa go`, dan 10 sec countdown, geen timer, reken op 5 tot 10 min) | Admin 1 |
| 21:58 | Winnaar FFA. `/finale spelers`: wie speelt de finale? Host kondigt aan: de winnaar van King of the Hill tegen de winnaar van de FFA. | Host, Admin 1 |
| 22:00 | **Ronde 7: De Finale** (1 tegen 1, stil tot `/finale go`, dan 10 sec countdown, geen timer, reken op 2 tot 5 min) | Admin 1 |
| 22:05 | **Kroning** op het podium in het midden van de Arena, tribunes vol. | Host |
| 22:15 | Einde stream. | |

Totaal ruim 1,5 uur speeltijd, plan 2 uur en een kwartier met buffer. De tijden van het doolhof
en het Ei volgen hun timers (`/doolhof timer`, `/ei timer`); zet je die om, schuift het schema
mee. De mob arena, Clown vs All, de FFA en de finale hebben geen timer; die kunnen uitlopen. Tussen de
beurten van de mob arena bepaalt de host het tempo. Loopt het echt uit, dan kan de ref een wave
forceren (`/mobarena wave volgende`) of de border laten krimpen (`/clown krimp`, `/ffa krimp`, `/finale krimp`).

## Commando's per ronde

Wat de commander en de ref per ronde typen. Het volledige overzicht staat in
[04-technische-schets.md](04-technische-schets.md).

| Ronde | Starten | Tijdens de ronde |
|---|---|---|
| 1 De Doolhof | `/doolhof start`, dan `/doolhof go` op het sein van Pudding | `/doolhof poort open`, `/doolhof resterend <sec>`, noodknop `/doolhof einde`; na afloop `/doolhof naarei` |
| 2 Het Ei | `/ei start` | `/ei resterend <sec>` |
| 3 De Mob Arena | `/mobarena start` | `/mobarena volgende` (elke volgende beurt), `/mobarena schema`, `/mobarena wave volgende` |
| 4 De Quiz | `/quiz start` | Pudding doet het met de vier items (ook het puntenmenu); noodknoppen `/quiz draai`, `/quiz goed`, `/quiz fout`, `/quiz punt <kleur> [-1]`. Einde: `/quiz einde` (bij gelijke stand `/quiz winnaar <kleur>`) |
| 5 Clown vs All | `/clown rad`, dan `/clown go` op het sein van Pudding | `/clown kroon <speler>`, `/clown krimp <grootte>` |
| 6 De FFA | `/ffa start`, dan `/ffa go` op het sein van Pudding | `/ffa krimp <grootte>` als het stilvalt |
| 7 De Finale | `/finale start`, dan `/finale go` op het sein van Pudding | `/finale krimp <grootte>` als het stilvalt; `/finale spelers` laat zien wie er speelt; logt een finalist uit, dan pauzeert de finale: Pudding kiest `/finale combatlog` (de ander wint) of `/finale crash` (daarna opnieuw `/finale start`) |

Elke ronde stopt met `/<ronde> stop`. Voor de hele avond: `/bc status`, `/bc team`,
`/bc kijker`, `/bc reset`.

Vooraf instellen, in minuten (blijft bewaard):

| Instelling | Standaard |
|---|---|
| `/doolhof timer` | 15 |
| `/doolhof poort` | 4 |
| `/doolhof hint` | 10 |
| `/doolhof hinttekst` | geen: dan noemt de mod de windrichting. Zet je eigen hint, bijvoorbeeld "De echte gang begint bij de lantaarn". |
| `/doolhof poortmelding` | aan: hoorn en title als de uitgang opengaat. `uit` voor een stille poort. |
| `/doolhof valmobs` | willekeurig 3 t/m 10 mobs per valkist |
| `/doolhof schrik` | elke schrikplek een willekeurige foto; `/doolhof schrik <nr> <1..5>` voor een vaste |
| `/doolhof wachttekst` | "Wacht op het startsein" |
| `/ei timer` | 15 |
| `/ei blokken` | netherite 6, diamond 90, gold 120, redstone 10, emerald 10, tnt 10, glowstone 10, slime 10, target 5 (voorlopig; samen 271, dat moet op de deepslate passen) |
| `/mobarena punten` | zombie 1; skeleton, spider, cave spider 2; creeper 3; witch 4; vindicator 5; evoker 8; ravager 10; warden 50 |
| `/mobarena aftekst` | "Af · je speelt geen beurt meer" |
| `/mobarena veldhoogte` | 3 |
| `/mobarena warden` | leven 200, klap 8, boom 5 (HP; 2 HP is één hartje) |
| `/clown wachttekst` | "Wacht op het startsein" |
| `/ffa wachttekst` | "Wacht op het startsein" |
| `/finale wachttekst` | "Wacht op het startsein" |

Na elke bouwwijziging aan het Ei: `/ei vastleggen`.

## Rollen

| Rol | Aantal | Wat |
|---|---|---|
| **Host / caster** | 1 | Praat op de stream, legt regels uit, kondigt rondes aan. Speelt Het Rad recht: "iedereen kan de kroon krijgen". |
| **Pudding: presentator van de quiz** | 1 | Speelt verder gewoon mee in zijn team, maar presenteert de quiz vanaf het podium: leest de vragen voor, keurt met groene en rode wol, draait het rad met het derde item. Zijn team heeft in de quiz één speler minder. |
| **Admin 1: commander** | 1 | Start elke ronde op het sein van Pudding, elke beurt van de mob arena (`/mobarena volgende`), sluit de quiz af (`/quiz einde`), draait het Rad (`/clown rad`) en geeft het startsein in Clown vs All en de FFA (`/clown go`, `/ffa go`). |
| **Admin 2: ref** | 1 | Kijkt naar problemen: stuck spelers, disconnects, bugs met de kroon, een team dat scheef zit. Heeft het randgevallen-lijstje uit [03-kroon-regels.md](03-kroon-regels.md) bij de hand. |
| **Camera** | 0 tot 2 | Kijker-accounts voor het hoofdbeeld: boven het doolhof, boven de mob arena, in de quizhal, boven de Arena. |
| **Bouwers** | 2 tot 4 | Vooraf. Zie de bouwlijst hieronder. |

Alleen de admins weten dat het Rad rigged is. Eén persoon kan host en ref combineren als je krap
zit, maar de commander moet alleen commander zijn.

## Bouwlijst (vooraf)

1. De wereld kiezen of genereren: ± 500 x 500 met heuvels, bos en water. Bepaal waar elke zone
   komt (plattegrond in [01-map-en-flow.md](01-map-en-flow.md)) (1 avond).
2. Basiskamp en de verzamelpunten (uurtje).
3. Het doolhof: startruimte in het midden, 4 gangen, 1 echte uitgang met een poort, 3
   nep-uitgangen, lege kisten, valkisten (trapped chests), schrikplekken, achter de echte uitgang
   een afgesloten ruimte met een finishlijn (`doolhof_uit`), vanwaar je terug het doolhof in kunt.
   Daarna regio `doolhof_gif` (het doolhof zonder de finishruimte, in delen als dat moet), punt
   `doolhof_finish` in de finishruimte, en barrier blocks in de openingen van de startruimte,
   geselecteerd als `poort_start` (elke opening een deel) (1 tot 2 avonden).
4. Het zwevende Ei met de kettingen: schil van andere blokken, binnenkant gewone deepslate, geen
   gewone deepslate in schil of kettingen. Daarna regio `ei`, `eigebied`, de startplekken
   `ei_spawn_n` en `/ei vastleggen` (gebouwd).
5. Het plein bij de mob arena met een podium en een item frame: punt `ei_podium` op het podium,
   punt `ei_presentator` ernaast (voor Pudding), regio `ei_plein` (de vloer ervoor), `/ei prijskader` kijkend naar het frame, en `v3` op het
   plein.
6. De mob arena: één veld met een kooi en een tribune. Daarna regio `veld` (in delen als dat
   moet), de tribunevloer (`tribune_mob`), per team twee startplekken
   (`/mobarena startplek <kleur> <1|2>`, niet te dicht bij elkaar), mob-spawns `mob_1..n`, de
   `kooi`, punt `warden` (waar de warden uit de grond komt) en tribunepunten `tribune_mob_n`.
7. De quizhal met 4 gekleurde banken, een redstone lamp bij elke bank (geen redstone ernaast),
   twee dispensers bij elke bank (de kant op gericht waar het vuurwerk heen moet) en een trap met
   podium. Daarna `/quiz bank <kleur>` bij elke bank, `/quiz lamp <kleur>` kijkend naar elke lamp,
   `/quiz vuurwerk <kleur> <1|2>` kijkend naar elke dispenser (8 keer) en `/quiz podium` boven
   aan de trap (gebouwd).
8. De Arena: colosseum met open vloer, podium in het midden, 20 redstone blocks in een cirkel,
   twee tribuneringen (alleen de onderste in gebruik). Daarna `/clown troon` op het podium,
   `/clown jagerplek` op elk redstone block (kijk de goede kant op), `/clown tribune` op twee
   plekken op de onderste ring, `/clown vloer <diameter>` midden in de Arena,
   en eventueel `colosseum` (gebouwd). Voor de finale `/finale plek 1` en `/finale plek 2` op de
   vloer, bijvoorbeeld tegenover elkaar en kijkend naar elkaar.
9. Het resource pack: de vijf jumpscare-foto's (`schrik_1.png` t/m `schrik_5.png`), het
   schrikgeluid (`schrik.ogg`) en de 8D-klop (`klop.ogg`) in `pack/aanleveren/`,
   `java pack/BouwPack.java`, de zip online zetten (uurtje, zie
   [04-technische-schets.md](04-technische-schets.md)).
10. De mod ombouwen naar dit rondeplan ([08-taakplan.md](08-taakplan.md)), dan met de wand,
   `/bc point` en de commando's per ronde alle regio's en punten zetten, zoals hierboven per
   zone (uurtje).
11. Volledige testrun met 4 tot 8 testers, zodat er minstens twee teams zijn (1 avond).

## Checklist: eerste test van de mod

Met een tweede account op de testserver (Java 25, niet op peaceful, `spawn-protection=0`). Voor
de eerste test mogen alle punten dicht bij elkaar op een vlak stuk staan. Wat er per onderdeel
te controleren valt staat in [mod/BOUWLOG.md](../mod/BOUWLOG.md).

- [ ] Jar in `mods/`, samen met Fabric API en de voice-mod. In de console `Pudding Bootcamp
      geladen` en dat de standaardbestanden in `config/bootcamp/` zijn geschreven.
- [ ] Resource pack wordt aangeboden bij het joinen. `/bc schrik <naam> 1` t/m `5`: elke foto vult
      het scherm, het schrikgeluid klinkt. `/bc klop <naam>`: de klop gaat van oor naar oor.
- [ ] `/bc wand`, een regio opslaan, `/bc region show`, `/bc point set`, `/bc point tp`,
      `/bc status`, `/bc kit basis`.
- [ ] Een T-vormige regio: `/bc region save test`, tweede selectie, `/bc region add test`.
      `/bc region show test` tekent beide delen, `/bc region list` zegt 2 delen.
- [ ] `/doolhof timer`, `/doolhof poort`, `/doolhof hint` en `/ei timer` zonder getal: 15, 4,
      10 en 15. Zet er een om en weer terug; een hint na het einde wordt
      geweigerd. Na een herstart staan ze er nog.
- [ ] `/doolhof start`: iedereen in de startruimte, bossbar `Doolhof · wacht op de start` en de
      wachttekst onderin, geen countdown. `/doolhof go`: de countdown. Je komt de startruimte niet
      uit tot de timer begint, dan zijn de openingen weg; kisten gevuld, nep-uitgang zet je terug met knal en grapje,
      schrikplek werkt één keer, poort open na 4 minuten (`/doolhof resterend 665` om te versnellen), hint
      na 10 minuten met je eigen `/doolhof hinttekst` (`/doolhof resterend 305`), teammenu op de
      finishlijn met de hoofden per team (alleen de wol kiest), vol team is grijs, de sidebar laat
      de namen per team zien, na je keuze `GEFINISHT` en je kunt terug het doolhof in (mobs
      en vallen werken dan nog), zodra iedereen een team heeft `DOOLHOF VOORBIJ`: wie nog in het
      doolhof liep staat in de finishruimte, wie er al stond blijft staan; `/doolhof naarei` zet
      iedereen naar `v2`. Opnieuw joinen buiten een ronde:
      welkomsttitle.
- [ ] Het gif: met twee accounts, één kiest geen team. Timer op (`/doolhof resterend 5`):
      `DE TIJD IS OM`, paarse bossbar, in het doolhof Poison en om de 2 seconden een hart eraf,
      in de finishruimte niets. Doodgaan in het gif: spullen weg behalve je steak, basiskit
      terug, `VERGIFTIGD` in de finishruimte en je zit in het kleinste team. Was je de laatste
      zonder team: `DOOLHOF VOORBIJ`. `/doolhof einde` sluit het doolhof ook midden in het gif af.
- [ ] Valkist in het doolhof: de bossbar noemt alleen de totale tijd. Een trapped chest openen geeft
      de jumpscare (willekeurige foto), de 8D-klop, of `/doolhof valmobs` husks en silverfish om je
      heen (open er een paar: ongeveer een kwart jumpscare, een kwart klop, de helft mobs); nog eens openen geeft een lege kist, een tweede speler krijgt hem wel. `/doolhof poortmelding uit`: de poort gaat stil
      open. Na `/doolhof stop` zijn de mobs weg.
- [ ] `/ei vastleggen`, `/ei blokken`: het overzicht noemt het aantal deepslate-plekken.
- [ ] `/ei start`: spelers verdeeld over de startplekken, punten per block, alles wat je vindt in
      de chat, redstone (beide uitkomsten met aftellen onderin, een paar keer breken), emerald geeft
      de ander een jumpscare en iedereen ziet groot wie naar wie, laatste minuut rood met aftellen,
      niets valt als item, kettingen en wereld niet te breken, niets neer te zetten behalve de TNT,
      dood = terug op je eigen startplek, winnaar bij de timer, daarna is de pickaxe weg (ook na
      `/ei stop`). Dan nog een keer `/ei start`: het Ei is weer heel en de blokken liggen ergens
      anders.
- [ ] Nieuwe blokken in het Ei (zet ze eventueel even hoog met `/ei blokken`): TNT komt in je
      inventory, neerzetten ontsteekt hem meteen en hij blijft hangen, de knal doet schade en blaast
      alleen deepslate weg; glowstone geeft `TURBO` en `Efficiency V · 10` onderin, daarna heeft de
      pickaxe weer Efficiency II; slime maakt de anderen misselijk; target zet met twee accounts
      iedereen op de plek van de ander.
- [ ] `/mobarena punten`: de tabel klopt.
- [ ] Einde van het Ei: de winnaar staat op het podium, Pudding ernaast, de rest verspreid op het
      plein en kijkt naar het podium, vuurpijl boven het podium. Het Warden-ei zit in het frame; een ander
      account kan het niet pakken, de winnaar of de presentator wel.
- [ ] `/mobarena start` met twee accounts in twee teams: geen schema in de chat
      (`/mobarena schema` laat het alleen jou zien), beide op hun eigen startplek, gloeiend in hun
      teamkleur, en tot `GO` kun je rondkijken maar niet lopen. Rook bij de spawnpunten, 5
      seconden na de laatste kill de volgende wave, punten per kill in de sidebar, een ravager-
      of evokerkill in de chat, mobs negeren de tribune en de kooi. Ga dood: de kooi, je
      `/mobarena aftekst` onderin, na de beurt de tribune. Na wave 5: tekst meteen in beeld, 10
      seconden aftellen, dan pas het veld en de kooi naar de tribune; wie al op de tribune stond
      blijft staan. `/mobarena volgende` weigert tijdens die 10 seconden. Einde: winnaar, 10
      seconden, daarna heeft iedereen een lege inventory en armor.
- [ ] Warden-ei: vanaf het veld of de kooi weigert hij; vanaf de tribune: `WARDEN-EI` in beeld, en
      de volgende wave is de warden, die bij punt `warden` uit de grond komt. Hij valt alleen wie
      in het veld staat aan, het publiek krijgt geen Darkness, en hij blijft tot hij dood is
      (`/mobarena wave volgende` als noodknop). De kill staat in de chat met 50 punten.
- [ ] `/quiz presentator <naam>`, `/quiz start`: iedereen zonder spullen bij zijn bank, de
      presentator op het podium met alleen groene wol, rode wol, het rad-item en de emerald. Rad een paar keer
      draaien: een rond rad groot in beeld dat afremt en met een vak onder het pijltje stopt (is
      het groot genoeg en scherp? anders `RAD_EENHEDEN` in `BouwPack.java` bijstellen), de lamp bij die bank
      gaat aan, groene wol geeft een
      punt (vanaf twee op rij met "3 op rij"), rode wol niet en zet de lamp uit, wol wordt nooit
      neergezet, de presentator ziet onderin wie aan de beurt is. Maak het gelijk en typ
      `/quiz einde`: iedereen ziet GELIJKSPEL. `/quiz punt <kleur> -1` corrigeert. `/quiz einde`: winnaar, 10 seconden,
      de items van de presentator zijn weg.
- [ ] Quiz extra: bij groene wol schieten de twee dispensers van dat team een vuurpijl in de
      teamkleur. De emerald opent het puntenmenu: `+2` en `−1` bij een team dat niet aan de beurt
      is veranderen de sidebar meteen, iedereen ziet de chatregel, onder 0 kan.
- [ ] `/clown uitverkoren <naam>`, `/clown rad` terwijl iedereen op de tribune staat: de rij
      spelerskoppen met namen schuift langs het pijltje en landt op de uitverkorene (zijn de
      koppen echte skins?), DE KROON met kop, iedereen gaat de vloer op (kroonhouder op het podium,
      jager op een willekeurige plek, goede kijkrichting) en staat stil: niet lopen, niet
      schieten, niet pearlen, onderin "Wacht op het startsein" (`/clown wachttekst` past het
      aan). `/clown go`: 10 seconden, dan los. De kroonhouder ziet onderin "Jij hebt de kroon",
      een afgevallen jager komt in de chat, en de winnaar (ook Clown) krijgt "... WINT KING OF
      THE HILL". De bossbar zegt "King of the Hill". Beide hebben dezelfde full-diamond kit met 10 gapples, de
      kroonhouder heeft de kroon als diamond helm die niet af kan, en Strength II (jagers niks).
      Bij nog 3 over heeft iedereen Strength I, ook de kroonhouder. Killt de kroonhouder een
      jager, dan ziet de rest zijn kop en naam groot met "pakt Speler3 · n over" en een brul. Jager raakt jager:
      geen schade. Kill de kroonhouder: kroon over, kroonpakket, reset. Laatste over wint.
- [ ] `/ffa start`: iedereen behalve Clown op een willekeurige plek, Clown op de tribune,
      iedereen stil. Kit: dezelfde als King of the Hill met 32 gapples, gewone diamond helm.
      Onderin "Wacht op het startsein". `/ffa go`: 10 seconden, dan raakt iedereen iedereen.
      Kills rechts, wie af is in de chat, onderin "n kills · n over" voor wie vecht. Een kill
      komt groot in beeld bij de tribune en de killer (zachte brul), niet bij de andere
      vechters. `/ffa krimp 20`: DE BORDER KRIMPT en de border krimpt.
      Bij drie en twee over LAATSTE DRIE en LAATSTE TWEE. Laatste over: SPELER WINT DE FFA met
      zijn kop en een vuurpijl, geen kroning.
- [ ] `/finale spelers` toont de winnaar van King of the Hill en de FFA (won dezelfde speler
      allebei: de nummer twee van de FFA). `/finale start`: die twee op `finale_1` en `finale_2`,
      FFA-kit, de rest op de tribune, DE FINALE met hun koppen, stil tot `/finale go`. Wie wint:
      kroning op het podium, iedereen op de tribune, vuurwerk, KING OF THE SMP BOOTCAMP met de kop
      van de winnaar; de bossbar "King: <naam>" en de zwevende kroon blijven tot `/bc reset`. Log
      een finalist uit: de finale pauzeert, de ander staat stil, de ops krijgen een melding.
      `/finale combatlog`: de ander krijgt de kroning. Nog een keer met `/finale crash`: de
      finale stopt, daarna opnieuw `/finale start` en `/finale go`. `/finale spelers a b` zet ze met de hand.
- [ ] `/bc doodtekst lijst` toont de zes teksten; `nieuw`, `weg <nr>` en `standaard` werken en
      blijven na een herstart staan.
- [ ] `/bc reset`: alles terug, ook de teams.
- [ ] Alles wat niet klopt in één bericht terug, met de console-regels erbij.

## Checklist: testrun

Minstens één keer de hele avond met 4 tot 8 testers. Let vooral op:

- [ ] Geen errors in de console van Fabric, de voice-mod of de bootcamp-mod.
- [ ] Alle regio's en punten staan erin (`/bc region list`, `/bc point list`).
- [ ] Doolhof: niemand kan over de haag. Nep-uitgangen triggeren op de goede plek. Poort gaat
      open op 4 minuten. Teamkeuze werkt, en na de timer krijgt iedereen zonder team er een.
- [ ] Het Ei: netherite is te minen met de diamond pickaxe. Punten kloppen. Bevriezing voelt
      niet oneerlijk lang. Terugzetten bij de start laat de server niet haperen. Stel de
      aantallen af met `/ei blokken`: genoeg te halen voor 15 minuten, maar niet overal punten.
- [ ] Mob arena: een beurt met 8 spelers duurt 3 à 5 minuten en er valt genoeg te killen; anders
      de aantallen in `waves.json` bijstellen. Punten per mob voelen eerlijk (`/mobarena punten`).
      De warden is haalbaar maar zwaar; anders `/mobarena warden leven|klap|boom <hp>`.
- [ ] Quiz: Pudding is vanaf het podium bij alle vier de banken te horen. Het rad is goed te
      lezen in beeld.
- [ ] Clown vs All: jagers kunnen elkaar niet raken, ook niet met pijlen. Kroonwissel zet
      iedereen goed terug. Rad landt op de uitverkorene; draai hem vijf keer. Voelt Strength II voor
      de kroonhouder te sterk of te zwak, en de 10 gapples genoeg?
- [ ] FFA: iedereen behalve Clown staat op de vloer. Kijk hoe lang een potje duurt met 32
      gapples, en bij welke grootte `/ffa krimp` het afmaakt.
- [ ] Kijkers: tribune en kooi, geen schade, niet het veld op. Spring als kijker van de tribune
      de Arena in: je staat meteen weer op de tribune.
- [ ] Voice: proximity werkt, de tribune is hoorbaar.
- [ ] Serverperformance met een vol veld (tot 20 mobs, of de warden).
- [ ] `/bc reset` brengt alles terug naar de basiskamp-staat.

## Checklist: dag zelf

- [ ] Wereldbackup gemaakt.
- [ ] `/bc reset` gedraaid, iedereen start schoon en zonder team.
- [ ] Whitelist compleet, staff heeft op.
- [ ] Resource pack online, URL en SHA-1 kloppen in `server.properties`.
- [ ] `/bc status` laat zien dat Clown uitverkoren is en niemand anders, en dat de timers en de
      doolhofmomenten staan zoals afgesproken (tabel bij *Commando's per ronde*).
- [ ] Pudding heeft de quizvragen klaar en `/quiz presentator` staat op Pudding.
- [ ] Voice: `force_voice_chat=true`, UDP-poort open.
- [ ] Host en camera-accounts staan in spectator of creative. Pudding níet: die speelt mee en
      staat in adventure.
- [ ] De jar van de vorige werkende versie staat klaar.
- [ ] Alle kits en `doolhof_loot.json` en `waves.json` staan in `config/bootcamp/`.
- [ ] `/ei vastleggen` is gedaan na de laatste bouwwijziging aan het Ei.
- [ ] `spawn-protection=0` en de difficulty niet op peaceful.

## Spelregels voor de streamers

Kort en op de borden in het basiskamp:

1. Geen x-ray, geen cheats, geen mods die voordeel geven. Sodium en dat soort dingen mag.
   Simple Voice Chat en het resource pack zijn verplicht.
2. Niet streamsnipen: niet op andermans stream kijken waar de uitgang is of waar de punten in
   het Ei zitten.
3. Teams: maximaal 5. Wie het eerst uit het doolhof is, kiest het eerst.
4. PvP staat uit tot het eind. Wat PvP wel mag, hoor je als het zover is.
5. Bug of stuck? Roep de ref, niet de chat.
6. Als de admin zegt stop, dan stop.
7. Geen Discord-call tijdens het event, alleen de voice-mod.

## Als het misgaat

| Probleem | Oplossing |
|---|---|
| Server crasht | Backup terugzetten, ronde opnieuw starten. De teams staan in `bootcamp.json` en overleven een herstart; klopt er iets niet, dan `/bc team`. |
| Timer loopt niet | `/<ronde> stop`, dan `/<ronde> start` en `/<ronde> resterend <seconden>`. |
| Doolhof of Ei duurt te lang of te kort | Tijdens de ronde `/<ronde> timer <minuten>`: geldt meteen, gerekend vanaf de start. |
| FFA, Clown vs All of de finale valt stil | `/ffa krimp <grootte>`, `/clown krimp <grootte>` of `/finale krimp <grootte>`. |
| Finale: de verkeerde spelers, of een winnaar ontbreekt | `/finale spelers <speler1> <speler2>` (1 = King of the Hill, 2 = FFA), dan `/finale start`. |
| Finalist logt uit tijdens de finale | De finale pauzeert vanzelf. Pudding kiest: combat log (`/finale combatlog`, de ander wint) of crash (`/finale crash`; is hij terug, dan `/finale start` en `/finale go`). Alleen ops kunnen dit typen. |
| Iemand heeft geen team of het verkeerde | `/bc team <speler> <kleur>`. |
| Iemand ziet de jumpscare als leeg vierkantje | Pack niet geladen. Opnieuw joinen, of accepteren in het menu. |
| Mob arena: een wave komt niet af | `/mobarena wave volgende`. |
| Rad stopt op de verkeerde naam | De uitverkorene staat verkeerd. Host: "technische storing", ref zet het recht met `/clown uitverkoren`, rad nog een keer. |
| Kroon zit bij niemand of bij twee | `/clown kroon <speler>`. |
| Een ronde breekt zichzelf af met een rode melding | De mod ving een fout af. De fout staat in de console. `/<ronde> start` opnieuw. |
| Speler zit vast in een blok | `tp` door de ref. |
| Het Ei staat er niet goed bij de start | `/ei stop` en `/ei start`: de mod zet het opnieuw terug. Klopt de vorm niet, dan is er na een bouwwijziging niet opnieuw `/ei vastleggen` gedaan. |
| Iemand hoort niks in voice | Kruis door het voice-icoontje: UDP-poort dicht of verkeerde versie. Geen kruis maar stil: `/bc kijker <naam> uit`. |
| De mod gooit errors | `/<ronde> stop`, `/bc reset`, ronde opnieuw. Blijft het misgaan: herstarten met de vorige jar. |
