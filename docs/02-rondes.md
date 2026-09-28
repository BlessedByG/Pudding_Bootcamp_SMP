# De rondes

Alle rondes uitgewerkt. Per ronde: doel, setup, regels, hoe het eindigt, wie wint en wat er mis
kan gaan.

Getallen zijn uitgangspunt voor 20 spelers: 4 teams van 5. Schaal ze mee met het aantal spelers.

| # | Ronde | Solo of team | Winnaar |
|---|---|---|---|
| 1 | De Doolhof | solo | geen; bij de uitgang kies je je team |
| 2 | Het Ei | solo | de speler met de meeste punten |
| 3 | De Mob Arena | team | het team met de meeste punten uit mob-kills |
| 4 | De Quiz | team | het team met de meeste goede antwoorden |
| 5 | Clown vs All | solo | de laatste die overblijft |
| 6 | De FFA | solo | de laatste die overblijft: King of the SMP Bootcamp |

Er is geen totaalstand over de avond: elke ronde met een winnaar staat op zichzelf.

**Gear.** Je start het doolhof met de basiskit. Wat je in de doolhofkisten vindt houd je in het Ei
en de mob arena. De pickaxe die je in het Ei krijgt, ben je na het Ei weer kwijt. **Na de mob arena levert iedereen alles in**: de quiz doe je zonder spullen (alleen
Pudding krijgt zijn quiz-items). Bij Clown vs All krijgt iedereen dezelfde kit, en in de FFA weer
(dezelfde kit met meer gapples).

**PvP** staat de hele avond uit, behalve voor de kroonhouder in Clown vs All en voor iedereen in
de FFA. De mod regelt dat, niet de teams.

**Doodgaan.** De mod laat niemand echt doodgaan: geen death-screen, geen respawn. Wat er dan
gebeurt hangt af van de ronde en staat per ronde hieronder. Wie uit een ronde ligt, gaat naar de
tribune: in adventure, zonder schade, met de andere doden. Niemand komt ooit in spectator mode.

---

## Ronde 0: Basiskamp

Iedereen spawnt in het basiskamp en ziet bij het joinen **PUDDING BOOTCAMP** met "Welkom, <naam>"
in beeld. Daarna de regels op de borden. De host legt uit wat er komen gaat, maar niet alles:
Clown vs All en het Rad pas bij ronde 5. Nog geen teams; iedereen is wit.

**Voice:** proximity vanaf het moment dat je joint. Voice-test voor de start (zie
[07-voice.md](07-voice.md)).

---

## Ronde 1: De Doolhof

**Doel:** vind als eerste de uitgang, want wie het eerst buiten is kiest het eerst een team.

**Setup**
- Hagendoolhof van ongeveer 64 x 64, hagen 4 hoog, geen plafond. Adventure mode: niet breken,
  niet bouwen.
- In het midden één grote startruimte. Daaruit lopen **4 gangen** het doolhof in. Maar één daarvan
  leidt naar de echte uitgang. De andere 3 eindigen in een **nep-uitgang**.
- Iedereen krijgt bij de start de **basiskit**: volledig iron armor, iron sword, 32 steak. De
  inventory gaat eerst leeg.
- **Kisten** in doodlopende gangen, 10 tot 15 stuks, met betere gear dan de basiskit. De mod vult
  ze bij de start uit een loot-tabel, dus elke run ligt er iets anders in en niemand hoeft ze met
  de hand te vullen. Denk aan losse diamond armor pieces, een enchanted sword, een boog met
  pijlen, een gapple. Geen ender pearls: zonder plafond gooi je die zo over de haag.
- 2 of 3 **schrikplekken** in het doolhof: loop je erdoor, dan krijg je de jumpscare. Per plek één
  keer per speler.

**Nep-uitgang**
- Loop je een nep-uitgang in, dan: een creeper-explosie (particles en geluid, geen schade), groot
  een grapje in beeld ("BOEM. Verkeerde deur.", "Haha, nep!", "Dit is niet de uitgang, sukkel")
  en je staat weer in de startruimte in het midden. De grapjes staan in de config.

**Jumpscare**
- Een foto van Clown schermvullend in beeld met het lachje van Clown erbij, een paar tellen. Daarvoor
  gebruikt de server een resource pack; zie [04-technische-schets.md](04-technische-schets.md).

**De uitgang**
- De uitgang is een poort die pas **na 4 minuten** (instelbaar) opengaat. Tot die tijd kun je de echte gang
  vinden, maar niet eruit. De bossbar telt af tot de poort open is.
- Wie door de uitgang komt, krijgt een menu met 4 kleuren: **rood, blauw, groen, geel**. Klik een
  kleur en je zit in dat team: je naam krijgt die kleur. **Een team is vol bij 5**; een vol team
  staat grijs in het menu en kun je niet kiezen. Clown kiest ook gewoon een kleur, als teamlid.
- Menu dicht zonder te kiezen? Dan komt het terug. Wie gekozen heeft gaat naar verzamelpunt 2 bij
  het Ei en wacht daar op de rest.

**Regels**
- Alles wat je vindt mag je houden, tot het einde van de mob arena.
- Doodgaan kan eigenlijk niet. Gebeurt het toch, dan sta je geheald terug in de startruimte.

**Einde**
- Timer 15 minuten. Na de timer gaat iedereen die nog binnen zit naar verzamelpunt 2 en komt in
  het team met de minste spelers. Wie buiten stond maar niet koos ook. Iedereen ziet
  **DOOLHOF VOORBIJ** met hoeveel spelers de uitgang vonden; wie in een team is gezet, ziet in
  welk.
- De timer, het openen van de poort en het moment van de hint zijn instelbaar met
  `/doolhof timer`, `/doolhof poort` en `/doolhof hint`, in minuten (zie
  [04-technische-schets.md](04-technische-schets.md)).

**Winnaar:** geen. Wie het eerst buiten is heeft de vrije keuze uit de teams.

**Voice:** proximity. Je hoort wie in de gang naast je loopt.

**Wat train je:** oriëntatie, rustig blijven, dead ends herkennen.

**Wat kan misgaan**
- Niemand vindt de uitgang: na 10 minuten een hint als title. De tekst zet je zelf met
  `/doolhof hinttekst <tekst>` ("De echte gang begint bij de lantaarn"); zonder tekst noemt de
  mod de windrichting van de uitgang.
  Uiteindelijk lost de timer het op.
- Stuck: de ref zet iemand met `tp` een gang verder.
- Teams worden scheef doordat vrienden samen kiezen: dat mag. Maximaal 5 is de enige grens.

---

## Ronde 2: Het Ei

**Doel:** hak zo veel mogelijk punten uit het Ei. Solo: de speler met de meeste punten wint.

**Setup**
- **Het Ei zweeft** boven het landschap: ongeveer 30 x 30 en 60 hoog, van ver te zien. Zoeken
  hoeft niet. De schil is van andere blokken, de binnenkant is helemaal deepslate. Vanaf de
  grond lopen **kettingen** omhoog naar het Ei.
- Iedereen begint aan het buiteneinde van een ketting, eerlijk verdeeld over de startplekken, en
  klimt over de ketting naar het Ei.
- Survival: je moet minen. Je houdt je gear en krijgt er een **diamond pickaxe met Efficiency II**
  bij. Diamond, omdat je een netherite block met iron niet kapot krijgt. **De pickaxe is alleen
  voor het Ei**: na de ronde ben je hem weer kwijt.
- Bij elke start zet de mod het Ei terug zoals het gebouwd is en strooit hij de **puntenblokken**
  op willekeurige plekken in de deepslate. Elke run ligt alles ergens anders. Standaard, voorlopig:

| Block | Wat het doet | Aantal |
|---|---|---|
| Netherite block | **50 punten.** Kost met een diamond pickaxe een paar tellen: je staat even stil. | 6 |
| Diamond block | **10 punten** | 90 |
| Gold block | **5 punten** | 120 |
| Redstone block | **Gok.** Of jij krijgt 10 seconden Haste, of iedereen behalve jij staat 15 seconden stil. 50/50. | 10 |
| Emerald block | **Jumpscare** bij een willekeurige andere speler. | 10 |

- De aantallen stel je in met `/ei blokken <soort> <aantal>`. Afstemmen in de testrun: het Ei
  heeft zo'n 28.000 blokken deepslate, en met 20 spelers in 15 minuten kan het grotendeels leeg.

**Regels**
- **Alleen het Ei is te breken**: de schil en de binnenkant. De kettingen en de rest van de wereld
  niet. Blokken neerzetten kan nergens, dus geen pilaren en geen dichtgezette gangen.
- Wat je uit het Ei hakt valt niet als item. Punten tellen **op het moment dat je het block
  breekt**: er is niks om mee te nemen, niks om te stelen en je inventory loopt niet vol met
  deepslate.
- Onderin beeld (de actionbar) staan steeds je punten en je plek: `85 punten · #14`. Breek je
  een puntenblok, dan staat er even voor wat je erbij kreeg: `+10 · 95 punten · #11`.
- Rechts in beeld staat de **top 10**: naam en punten, bijgewerkt bij elk blok. Bij gelijke
  punten staat wie die score het eerst had bovenaan.
- **Stilstaan** bij een redstone-bevriezing: niet lopen, niet springen, niet minen. Rondkijken
  kan. Iedereen ziet in beeld wie het deed, en onderin telt het af hoe lang je nog stil staat.
  Bij Haste telt het ook af.
- **Netherite** meldt de mod in de chat: "Speler7 hakte netherite (+50)".
- **Emerald**: de hakker ziet naar wie de jumpscare ging, en de ander ziet na de schrik van wie
  hij kwam.
- De laatste minuut wordt de bossbar rood, en de laatste 10 seconden tellen groot af. Breekt iemand anders tijdens een bevriezing weer een
  redstone block en valt die ook op bevriezen, dan begint de bevriezing opnieuw, nu met die
  speler als enige die los is.
- Geen PvP.
- Doodgaan (van een ketting of het Ei vallen): je staat geheald terug op je startplek, je punten
  en spullen houd je. Wie valt en het overleeft, loopt zelf terug naar een ketting.

**Einde**
- Timer 15 minuten, instelbaar met `/ei timer <minuten>`. Wie de meeste punten heeft, wint: title
  voor iedereen, vuurpijl boven de winnaar. Gelijk? Dan wint wie die score het eerst had.
- De pickaxe gaat weer weg. Iedereen naar verzamelpunt 3 bij de mob arena. De punten tellen
  verder nergens voor.

**Voice:** proximity. Op het Ei hoor je wie naast je hakt.

**Wat train je:** klimmen, snel minen, kiezen waar je je tijd in steekt.

**Wat kan misgaan**
- Het Ei is te snel leeg, of er valt te weinig te halen: de aantallen bijstellen met
  `/ei blokken` in de testrun.
- Iemand zit vast in het Ei: ref `tp`.
- Het Ei is aan de buitenkant verbouwd: eerst opnieuw `/ei vastleggen`, anders zet de mod bij de
  start de oude versie terug.

---

## Ronde 3: De Mob Arena

**Doel:** haal zo veel mogelijk punten voor je team door mobs te killen. Het team met de meeste
punten wint.

**Setup**
- **Twee arena's**, 1 en 2. Ze zien er hetzelfde uit maar zijn gespiegeld. In elke arena vier
  startplekken in de teamkleuren (rood, blauw, groen, geel) en **in het midden een kooi** voor
  wie sneuvelt. Een **tribune** (balkon) met zicht op de arena.
- **Arena 1 en 2 spelen tegelijk.** Per beurt staat in elke arena **van elk team één speler** op
  het vlak in de kleur van zijn team (de kleur die hij in het doolhof koos): acht spelers
  tegelijk. De rest kijkt vanaf de tribune.
- **Iedereen speelt twee beurten**: één in arena 1 en één in arena 2. Met teams van 5 zijn dat
  5 beurten.
- **Het schema loot de mod** bij de start en houdt het **geheim**: je merkt pas dat je aan de
  beurt bent als je naar je vlak wordt geteleporteerd. Niemand speelt twee beurten achter
  elkaar. Voorbeeld voor één team:

| Beurt | Arena 1 | Arena 2 |
|---|---|---|
| 1 | speler 1 | speler 3 |
| 2 | speler 2 | speler 4 |
| 3 | speler 3 | speler 5 |
| 4 | speler 4 | speler 1 |
| 5 | speler 5 | speler 2 |

- **Ongelijke teams** (bijvoorbeeld 5-5-4-4): er zijn zoveel beurten als het grootste team
  spelers heeft. De plekken die een kleiner team tekortkomt zijn **extra beurten**: aan het begin
  van die beurt loot de mod een speler van dat team die nog niet af is en niet al in de andere
  arena staat. Is er niemand, dan blijft de plek leeg.
- **Gear:** alleen wat je al hebt, uit het doolhof. Geen kit, geen eten, niets extra's. Je begint
  elke beurt wel met volle levens.

**Een beurt**
- De commander start elke beurt op het sein van Pudding. Countdown, dan **5 waves**, in beide
  arena's tegelijk dezelfde mobs.
- De volgende wave komt **5 seconden nadat de vorige in beide arena's dood is**. Blijft er een
  mob hangen, dan komt de volgende toch na 2 minuten.
- Waves per arena (voor 4 spelers):

| Wave | Wat | Aantal |
|---|---|---|
| 1 | Zombies | 4 |
| 2 | Skeletons + spiders | 4 + 3 |
| 3 | Zombies met iron gear + creepers | 4 + 2 |
| 4 | Zombies met iron gear + witches + cave spiders | 4 + 2 + 3 |
| 5 | Ravager + vindicators + evoker | 1 + 3 + 1 |

- De aantallen staan in `waves.json` en zijn af te stemmen in de testrun.
- Een arena is klaar na wave 5, of als alle spelers erin af zijn. De beurt is klaar als beide
  arena's klaar zijn.
- **Na de beurt: 10 seconden om te vieren.** `BEURT 3 KLAAR` met de stand staat meteen in beeld,
  met een aftelling van 10 seconden. Pas daarna gaan de spelers uit de arena's en de mensen in de
  kooien naar de tribune. Wie al op de tribune stond, blijft gewoon staan waar hij staat.

**Punten**
- Kill je een mob, dan krijgt je team punten. Wie de laatste klap gaf telt, ook met een pijl.
  Standaard (instelbaar met `/mobarena punten`):

| Mob | Punten |
|---|---|
| Zombie | 1 |
| Skeleton, spider, cave spider | 2 |
| Creeper | 3 |
| Witch | 4 |
| Vindicator | 5 |
| Evoker | 8 |
| Ravager | 10 |
| Elk ander type | 1 |

- **PvE, geen PvP**: je vecht alleen tegen de mobs. De vier spelers in een arena strijden wel om
  dezelfde mobs, maar elkaar raken kan niet, ook niet met pijlen.
- Sidebar: de teamstand. Actionbar: wat je net kreeg en de stand van je team
  (`+3 · Rood 47`). Bossbar: `Mob Arena · beurt 3/5 · wave 2`.
- Wie aan de beurt is, **gloeit in zijn teamkleur**: kijkers en streams zien meteen wie van welk
  team is. Een kill op een evoker of ravager komt in de chat: "Speler7 killde de ravager (+10)".

**Doodgaan**
- Ga je dood, dan ga je **in de kooi in het midden van je arena** en kijk je de rest van die beurt
  vanaf daar. Doodtekst groot in beeld, alleen voor jou, en daarna onderin "Af · je speelt geen
  beurt meer" (aan te passen met `/mobarena aftekst`). Je punten blijven staan.
- Na de beurt (en de 10 seconden) ga je naar de tribune. **Je speelt geen tweede beurt meer**:
  stond je nog op het schema, dan blijft de plek van je team in die beurt leeg. Je spullen ben je
  kwijt; die had je na de mob arena toch ingeleverd.
- Wie uitlogt telt als dood.
- Mobs vallen alleen de levende spelers in hun eigen arena aan. De kooi en de tribune laten ze met
  rust.

**Einde**
- Na de laatste beurt (ook met de 10 seconden) wint het team met de meeste punten: title voor
  iedereen, vuurpijlen boven het team. Gelijk? Dan wint het team met de meeste kills; is dat ook
  gelijk, dan winnen ze samen.
- Ook dan eerst 10 seconden om te vieren; daarna **levert iedereen alles in**: inventory en
  armor leeg. Iedereen geheald en zonder spullen naar de quiz.

**Voice:** proximity. In de arena hoor je de drie spelers van de andere teams, in de kooi en op de
tribune hoor je wie in de buurt staat.

**Wat train je:** mobs, snel reageren, kills pakken voor een ander ze pakt.

**Wat kan misgaan**
- Een mob blijft hangen achter dekking: na 2 minuten komt de volgende wave toch. De ref kan een
  wave forceren met `/mobarena wave volgende`.
- Een beurt is te snel voorbij of te zwaar: de aantallen in `waves.json` bijstellen in de testrun.
- Server lag: 2 arena's met elk hooguit 10 mobs is weinig, maar houd view distance op 8.

---

## Ronde 4: De Quiz

**Doel:** beantwoord als team zo veel mogelijk vragen goed. Rustronde na de mob arena.

**Setup**
- **De quizhal**: vier **banken** in de teamkleuren, een **trap met een klein podium** waar Pudding
  presenteert, en in het midden een rond vloerontwerp met een vraagteken (decor).
- Iedereen staat bij de bank van zijn team, zonder spullen: alles is na de mob arena ingeleverd.
- **Bij elke bank staat een lamp.** Die van het team dat aan de beurt is, brandt.
- **Pudding presenteert** en zit dus niet bij zijn eigen team. Dat team heeft in de quiz één
  speler minder: pech. Pudding krijgt als enige iets: drie items in de hotbar, **groene wol**
  (goed), **rode wol** (fout) en een item om **het rad te draaien**. Na de quiz zijn die weer weg.
- Adventure, geen schade, geen timer.

**Het rad**
- Een **echt rond rad**, groot in het midden van het scherm, met een pijltje bovenin: 16 vakken
  in alleen de teamkleuren, **elk team 4 keer**, zo verdeeld dat twee buren nooit dezelfde kleur
  hebben. Het draait, remt af met een tik per vak, en stopt met een vak onder het pijltje. Dat
  team is aan de beurt. Dat is echt willekeurig: elk team heeft evenveel kans. Het rad staat
  alleen in beeld (plaatjes uit het resource pack), niet in de wereld.

**Hoe het loopt**
1. Pudding draait het rad. In beeld: **ROOD IS AAN DE BEURT**, en de lamp bij de rode bank gaat
   aan.
2. Pudding leest een vraag voor. Het team overlegt en geeft antwoord.
3. **Goed?** Pudding klikt de groene wol: **GOED!** in beeld en een punt voor dat team. Het team
   krijgt de volgende vraag. Vanaf twee op rij staat de reeks erbij: "+1 Rood · 3 op rij".
4. **Fout?** Pudding klikt de rode wol: **FOUT!** in beeld, geen punt, de lamp gaat uit. Dan
   draait Pudding het rad
   opnieuw, over alle vier de teams. Hetzelfde team kan dus weer uitkomen.
5. Dit gaat door tot Pudding stopt.

**Regels**
- Pudding leest de vragen voor, keurt de antwoorden en bepaalt hoeveel vragen het worden. De mod
  telt de punten: rechts in beeld staat de stand.
- Alleen het team dat aan de beurt is antwoordt.

**Einde**
- De commander typt `/quiz einde`: het team met de meeste punten wint. Title voor iedereen,
  vuurpijlen boven de bank. Gelijk? Dan ziet iedereen **GELIJKSPEL** ("Rood en Geel · Pudding
  kiest") en kiest Pudding de winnaar, eventueel met een beslissende vraag.
- Pudding ziet de hele quiz onderin wie er aan de beurt is en hoeveel op rij, of dat het rad
  gedraaid moet worden.
- 10 seconden om te vieren, dan naar de Arena voor Clown vs All. Pudding is daar weer gewone
  speler.

**Voice:** proximity. De hal is klein, dus iedereen hoort Pudding en elkaar. Overleggen met je
team kan hardop: de andere teams horen het ook. Dat is onderdeel van het spel.

**Wat kan misgaan**
- Pudding is niet te horen bij een bank: dichter bij de banken gaan staan; de hal moet binnen 48
  blokken passen.
- Het rad landt steeds op hetzelfde team: dat is toeval. Pudding kan het wegpraten.
- Pudding klikt per ongeluk verkeerd: de commander zet het recht met `/quiz punt <kleur> -1`.

---

## Ronde 5: Clown vs All

De volledige regels staan in [03-kroon-regels.md](03-kroon-regels.md). Hier het overzicht.

**Doel:** blijf als laatste over.

**Setup**
- De Arena: een colosseum met een open zandvloer, in het midden een klein podium en rondom 20
  startplekken (redstone blocks in een cirkel). Tribune: alleen de onderste ring. Adventure: niet
  bouwen.
- De teams zijn vanaf nu weg. Iedereen speelt solo.
- Het begint met **Het Rad**, terwijl iedereen op de tribune staat. Het rad staat **in beeld**
  als een rij spelerskoppen met namen die langs een pijltje schuift, langzamer wordt en stopt op
  Clown. Het ziet
  eruit als toeval, het is rigged: alleen de admins weten dat het altijd op Clown landt.
- Daarna gaat iedereen de vloer op: **Clown op het podium in het midden**, de jagers
  **willekeurig verdeeld over de 20 startplekken** (één blijft leeg), iedereen met de kijkrichting
  van zijn plek. **Iedereen staat stil**, ook Clown: niet lopen, niet springen, niet schieten, niet
  pearlen. Pas als de commander `/clown go` doet, telt het 10 seconden af en dan gaat iedereen
  tegelijk los.
- Iedereen is sinds de mob arena zonder spullen. Nu krijgt **iedereen dezelfde kit**, Clown ook:
  volledig diamond armor (Protection IV), diamond sword en diamond axe (Sharpness V), bow
  (Power V) met 32 pijlen, schild en 16 golden apples, alles met Unbreaking III. Het enige
  verschil: Clown draagt **de kroon** als helm, een diamond helm die niet af kan.

**Regels in het kort**
- **Friendly fire uit.** Alleen de kroonhouder kan iemand doden, en alleen de kroonhouder kan
  geraakt worden door de rest. Jagers kunnen elkaar niks doen.
- Word je gedood door de kroonhouder, dan ben je **af** en ga je de tribune op. Van de tribune
  de Arena in lopen of springen kan niet: dan zet de mod je meteen terug op de tribune.
- **Kill je de kroonhouder, dan krijg jij de kroon.** Dan begint het opnieuw: de nieuwe
  kroonhouder op het podium, iedereen die nog leeft geheald op een willekeurige startplek, stil.
  Na een automatische countdown van 10 seconden begint de jacht weer.
- **Geen timer.** Het gaat door tot er één over is.

**Einde**
- De laatste die overblijft wint Clown vs All. Is dat Clown, dan staat er **DE EINDBAAS WINT**
  in beeld.
- Wie af is, komt in de chat: "Speler3 is af door ClownPierce · 11 over". De kroonhouder ziet
  onderin "Jij hebt de kroon · 11 jagers"; tijdens het wachten op `/clown go` ziet iedereen
  "Wacht op het startsein" (aan te passen met `/clown wachttekst`).
- Clown doet niet mee aan de FFA, ook niet na een overwinning. Iedereen anders wel, ook wie
  af was.

**Voice:** proximity. De tribune is publiek: de vloer hoort de doden.

**Wat train je:** PvP tegen overmacht, samen op één doel, wanneer je wel en niet moet gaan.

---

## Ronde 6: De FFA

**Doel:** laatste die overblijft. Die is **King of the SMP Bootcamp**.

**Setup**
- Dezelfde Arena als Clown vs All. Iedereen behalve Clown, full hp, **willekeurig verdeeld over de
  20 startplekken**, met de kijkrichting van de plek. Ook wie in Clown vs All af was, en de
  winnaar ervan. Clown zit op de tribune.
- Iedereen krijgt dezelfde **FFA-kit**: dezelfde als bij Clown vs All, maar met **32 golden
  apples**. Dus volledig diamond armor (Protection IV), diamond sword en axe (Sharpness V), bow
  (Power V) met 32 pijlen, schild, alles met Unbreaking III, en 32 gapples. Een gewone diamond
  helm, geen kroon. Eigen spullen gaan weg.
- **Iedereen staat stil** (niet lopen, springen, schieten of pearlen) tot de commander `/ffa go`
  doet. Dan 10 seconden countdown en iedereen tegelijk los. Iedereen kan iedereen raken.

**Regels**
- Dood = uit, tribune op. Doodtekst groot in beeld. Van de tribune de vloer op kan niet.
- Teamen mag, maar er wint er maar één.
- **Geen timer.** Het gaat door tot er één over is.
- **De border krimpt alleen als de commander dat doet**: `/ffa krimp <grootte> [<seconden>]`,
  als het stilvalt. Iedereen ziet dan **DE BORDER KRIMPT**. Zonder dat blijft de hele vloer vrij.
- In beeld: rechts de **kills** per speler, wie af is in de chat ("Speler3 is af door Speler7 ·
  11 over"), en bij drie en twee over **LAATSTE DRIE** en **LAATSTE TWEE** met de namen.

**Einde**
- De laatste levende speler is **King of the SMP Bootcamp**.
- Meteen daarna de **kroning** op het podium in het midden van de Arena: iedereen op de tribune,
  de winnaar op het podium met de kroon, twintig seconden vuurpijlen, en voor iedereen in beeld
  **KING OF THE SMP BOOTCAMP** met de kop en de naam. Geen prijs, just for fun.
- Daarna blijft de King zichtbaar: de bossbar "Pudding Bootcamp · King: Speler7" en de zwevende
  kroon boven de winnaar, tot `/bc reset`.

**Voice:** proximity. Clown zit op de tribune en mag meejoelen.
