# De rondes

Alle rondes uitgewerkt. Per ronde: doel, setup, regels, hoe het eindigt, wie wint en wat er mis
kan gaan.

Getallen zijn uitgangspunt voor 20 spelers: 4 teams van 5. Schaal ze mee met het aantal spelers.

| # | Ronde | Solo of team | Winnaar |
|---|---|---|---|
| 1 | De Doolhof | solo | geen; bij de uitgang kies je je team |
| 2 | Het Ei | solo | de speler met de meeste punten |
| 3 | De Mob Arena | team | het team dat het langst overleeft (knock-out) |
| 4 | De Quiz | team | het team dat de host aanwijst |
| 5 | Clown vs All | solo | de laatste die overblijft |
| 6 | De FFA | solo | de laatste die overblijft: King of the SMP |

Er is geen totaalstand over de avond: elke ronde met een winnaar staat op zichzelf.

**Gear.** Je start het doolhof met de basiskit. Wat je in de doolhofkisten vindt houd je in het Ei,
de mob arena en de quiz. Bij Clown vs All gaat alles weg en krijgt iedereen dezelfde kit, en in de
FFA weer.

**PvP** staat de hele avond uit, behalve voor de kroonhouder in Clown vs All en voor iedereen in
de FFA. De mod regelt dat, niet de teams.

**Doodgaan.** De mod laat niemand echt doodgaan: geen death-screen, geen respawn. Wat er dan
gebeurt hangt af van de ronde en staat per ronde hieronder. Wie uit een ronde ligt, gaat naar de
tribune: in adventure, zonder schade, met de andere doden. Niemand komt ooit in spectator mode.

---

## Ronde 0: Basiskamp

Iedereen spawnt in het basiskamp en leest de regels op de borden. De host legt uit wat er komen
gaat, maar niet alles: Clown vs All en het Rad pas bij ronde 5. Nog geen teams; iedereen is wit.

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
- De uitgang is een poort die pas **na 4 minuten** opengaat. Tot die tijd kun je de echte gang
  vinden, maar niet eruit. De bossbar telt af tot de poort open is.
- Wie door de uitgang komt, krijgt een menu met 4 kleuren: **rood, blauw, groen, geel**. Klik een
  kleur en je zit in dat team: je naam krijgt die kleur. **Een team is vol bij 5**; een vol team
  staat grijs in het menu en kun je niet kiezen. Clown kiest ook gewoon een kleur, als teamlid.
- Menu dicht zonder te kiezen? Dan komt het terug. Wie gekozen heeft gaat naar verzamelpunt 2 aan
  de bosrand en wacht daar op de rest.

**Regels**
- Alles wat je vindt mag je houden, tot Clown vs All.
- Doodgaan kan eigenlijk niet. Gebeurt het toch, dan sta je geheald terug in de startruimte.

**Einde**
- Timer 10 minuten. Na de timer gaat iedereen die nog binnen zit naar verzamelpunt 2 en komt in
  het team met de minste spelers. Wie buiten stond maar niet koos ook.

**Winnaar:** geen. Wie het eerst buiten is heeft de vrije keuze uit de teams.

**Voice:** proximity. Je hoort wie in de gang naast je loopt.

**Wat train je:** oriëntatie, rustig blijven, dead ends herkennen.

**Wat kan misgaan**
- Niemand vindt de uitgang: na 7 minuten een hint als title ("de echte gang begint bij de ...").
  Uiteindelijk lost de timer het op.
- Stuck: de ref zet iemand met `tp` een gang verder.
- Teams worden scheef doordat vrienden samen kiezen: dat mag. Maximaal 5 is de enige grens.

---

## Ronde 2: Het Ei

**Doel:** hak zo veel mogelijk punten uit het Grote Ei. Solo: de speler met de meeste punten wint.

**Setup**
- Zoekgebied van 150 x 150: bos met heuvels, grotten en een meertje. De worldborder sluit het af.
- Survival: je moet minen. Je houdt je gear en krijgt er een **diamond pickaxe met Efficiency II**
  bij. Diamond, omdat je een netherite block met iron niet kapot krijgt.
- **Het Grote Ei:** ±15 hoog, ±11 breed, half verstopt. Een schil van steen (stone, deepslate,
  andesite, tuff, wat obsidian-vlekken) en binnenin de puntenblokken, rommelig door elkaar met
  opvulling ertussen. Met 20 spelers ongeveer:

| Block | Wat het doet | Aantal |
|---|---|---|
| Netherite block | **50 punten.** Kost met een diamond pickaxe een paar tellen: je staat even stil. | 3 |
| Diamond block | **10 punten** | 30 |
| Gold block | **5 punten** | 40 |
| Redstone block | **Gok.** Of jij krijgt 10 seconden Haste, of iedereen behalve jij staat 15 seconden stil. 50/50. | 8 |
| Emerald block | **Jumpscare** bij een willekeurige andere speler. | 5 |

- Nep-eitjes: 3 tot 5 kleine eitjes met soms een bordje met een hint over de richting.

**Regels**
- Punten tellen **op het moment dat je het block breekt**. Het block valt niet: er is niks om mee
  te nemen, niks om te stelen.
- Je eigen score staat in je actionbar, de top van het klassement in de sidebar.
- **Stilstaan** bij een redstone-bevriezing: niet lopen, niet springen, niet minen. Rondkijken
  kan. Iedereen ziet in beeld wie het deed. Breekt iemand anders tijdens een bevriezing weer een
  redstone block en valt die ook op bevriezen, dan begint de bevriezing opnieuw, nu met die
  speler als enige die los is.
- Geen PvP.
- Doodgaan (val, lava, verdrinken): je staat geheald terug aan de bosrand, je punten en spullen
  houd je.

**Einde**
- Timer 10 minuten. Wie de meeste punten heeft, wint: title voor iedereen, vuurpijl boven de
  winnaar. Gelijk? Dan wint wie die score het eerst had.
- Iedereen naar verzamelpunt 3 bij de mob arena. De punten tellen verder nergens voor.

**Voice:** proximity. Wie het Ei vindt hoort alleen wie in de buurt is.

**Wat train je:** exploren, snel minen, kiezen waar je je tijd in steekt.

**Wat kan misgaan**
- Niemand vindt het Ei: op 5 minuten gaat een beacon onder het Ei aan, op 3 minuten een vuurpijl.
- Het Ei is binnen twee minuten leeg: meer gold en diamond erin bij de testrun.
- Iemand zit in een grot vast: ref `tp`.

---

## Ronde 3: De Mob Arena

**Doel:** houd het met je team langer vol dan het andere team. Knock-out: twee halve finales,
dan de finale.

**Setup**
- **Twee identieke arena's naast elkaar**, A en B, met een tribune ertussen die op allebei
  uitkijkt. Elke arena heeft 4 mob-spawnpunten, wat dekking, en naast het veld een **kooi**: een
  glazen hok waar je in staat als je dood bent, met zicht op het veld.
- Iedereen houdt zijn gear en krijgt er de **mob-arenakit** bij: boog, 32 pijlen, schild, 16
  steak.
- **Loting** bij de start: de mod trekt welke twee teams tegen elkaar spelen. Bijvoorbeeld:

```
halve finale 1:  Rood (A)   vs  Blauw (B)
halve finale 2:  Groen (A)  vs  Geel (B)
finale:          winnaar 1  vs  winnaar 2
```

**Een wedstrijd**
- Twee teams, elk in een eigen arena. De rest staat op de tribune.
- De waves spawnen **in beide arena's tegelijk**, dezelfde mobs. Is jouw team eerder klaar, dan
  heb je rust tot de andere arena de wave ook dood heeft. **10 seconden** daarna komt de volgende
  wave, weer in allebei.
- De waves worden steeds zwaarder en houden niet op. Na de laatste wave uit de lijst komt die
  wave steeds opnieuw, met elke keer meer mobs.
- Waves voor een team van 5 (per arena):

| Wave | Wat | Aantal |
|---|---|---|
| 1 | Zombies | 5 |
| 2 | Skeletons + spiders | 4 + 3 |
| 3 | Zombies met iron gear + creepers | 5 + 2 |
| 4 | Zombies met iron gear + witches + cave spiders | 5 + 2 + 3 |
| 5 | Ravager + vindicators + evoker | 1 + 3 + 1 |
| 6 en verder | Wave 5, met elke wave één extra van elk | |

- Bossbar: `Rood vs Blauw · Wave 4`. Sidebar: per team hoeveel er nog staan.

**Regels**
- Doodgaan = je gaat **in de kooi** van je arena en kijkt de rest van de wedstrijd daar. Doodtekst
  groot in beeld, alleen voor jou. Je spullen worden bewaard en je krijgt ze na de mob arena
  terug.
- Een wedstrijd is afgelopen als een team **niemand meer op het veld** heeft. Het andere team wint.
  Gaan beide teams in dezelfde wave onderuit, dan wint het team waarvan de laatste speler het
  langst bleef staan.
- Na een wedstrijd gaan beide teams naar de tribune.
- **Dood blijft dood tot het einde van de mob arena.** Wie in de halve finale sneuvelde, begint de
  finale in de kooi, niet op het veld. De finale speel je met wie er over is. Wie wel overleefde
  begint de finale geheald.
- Geen PvP, ook niet binnen je team.

**Einde**
- Het team dat de finale wint, wint de mob arena: title voor iedereen, vuurpijlen boven het team.
- Iedereen weer levend, spullen terug, naar de quiz.

**Voice:** proximity. De tribune staat tussen de twee arena's, dus je hoort beide teams en zij
jou.

**Wat train je:** mobs, boog en schild, samenwerken, niet in de creeper rennen.

**Wat kan misgaan**
- Een mob blijft hangen achter dekking en de wave wordt nooit klaar: na 2 minuten telt de wave
  als klaar en komt de volgende. De ref kan een wave forceren met `/bc wave volgende`.
- Een wedstrijd duurt eindeloos omdat het doolhof-gear te sterk is: de waves worden elke keer
  zwaarder, dus uiteindelijk valt er een team. Stel de aantallen bij in de testrun.
- Server lag: 2 arena's met elk hooguit 20 mobs is weinig, maar houd view distance op 8.

---

## Ronde 4: De Quiz

**Doel:** beantwoord als team zo veel mogelijk vragen goed. Rustronde na de mob arena.

**Setup**
- Een podium met vier vakken, één per team in de teamkleur. Voor elk vak een lamp. Iedereen staat
  in het vak van zijn team.
- Adventure, geen schade, geen timer.

**Hoe het loopt**
1. De **randomizer**: een lichtje loopt langs de vier lampen, steeds langzamer, en stopt op een
   team. Dit is echt willekeurig, niet rigged.
2. De host stelt dat team vragen. Het team overlegt en geeft antwoord.
3. Goed? Dan krijgt het team de volgende vraag. **Fout? Dan draait de randomizer opnieuw**, over
   alle vier de teams. Hetzelfde team kan dus weer uitkomen.
4. Dit gaat door tot de host stopt.

**Regels**
- De host leest de vragen voor, keurt de antwoorden, houdt de punten bij en bepaalt hoeveel vragen
  het worden. De mod doet alleen de randomizer en laat de stand zien als de host die bijhoudt
  (zie de commands in [04-technische-schets.md](04-technische-schets.md)).
- Alleen het team dat aan de beurt is antwoordt.

**Einde**
- De host wijst het winnende team aan: title voor iedereen, vuurpijlen boven dat vak.
- Daarna naar de Arena voor Clown vs All.

**Voice:** proximity. Het podium is klein, dus iedereen hoort de host en elkaar. Overleggen met je
team kan hardop: de andere teams horen het ook. Dat is onderdeel van het spel.

**Wat kan misgaan**
- De host is niet te horen: de host staat in-game bij het podium, binnen 48 blokken van iedereen.
- De randomizer landt steeds op hetzelfde team: dat is toeval. De host kan het wegpraten.

---

## Ronde 5: Clown vs All

De volledige regels staan in [03-kroon-regels.md](03-kroon-regels.md). Hier het overzicht.

**Doel:** blijf als laatste over.

**Setup**
- De Arena: een colosseum met een vloer van Ø 60 tot 80 met dekking, een verhoogd midden en
  tribunes rondom. Adventure: niet bouwen.
- De teams zijn vanaf nu weg. Iedereen speelt solo.
- Het begint met **Het Rad**: iedereen op de vloer, een lampje loopt langs de 20 pilaren met
  spelerskoppen rond de vloer en stopt op Clown. Het ziet eruit als toeval, het is rigged: alleen
  de admins weten dat het altijd op Clown landt.
- Alle gear gaat weg. Clown krijgt de kroon en de **bosskit**, iedereen anders dezelfde
  **jagerskit**.

**Regels in het kort**
- **Friendly fire uit.** Alleen de kroonhouder kan iemand doden, en alleen de kroonhouder kan
  geraakt worden door de rest. Jagers kunnen elkaar niks doen.
- Word je gedood door de kroonhouder, dan ben je **af** en ga je de tribune op.
- **Kill je de kroonhouder, dan krijg jij de kroon.** Dan begint het opnieuw: iedereen die nog
  leeft staat geheald op zijn startpunt, de nieuwe kroonhouder in het midden, en de jacht begint
  weer.
- **Geen timer.** Het gaat door tot er één over is.

**Einde**
- De laatste die overblijft wint Clown vs All. Is dat Clown, dan heeft de eindbaas gewonnen.
- Clown doet niet mee aan de FFA, ook niet na een overwinning. Iedereen anders wel, ook wie
  af was.

**Voice:** proximity. De tribune is publiek: de vloer hoort de doden.

**Wat train je:** PvP tegen overmacht, samen op één doel, wanneer je wel en niet moet gaan.

---

## Ronde 6: De FFA

**Doel:** laatste die overblijft. Die is **King of the SMP**.

**Setup**
- Iedereen behalve Clown, full hp, verspreid over de rand van de Arena. Ook wie in Clown vs All
  af was, en de winnaar ervan.
- Iedereen krijgt dezelfde **arenakit**: volledig diamond Protection I, diamond sword Sharpness I,
  boog Power I, 16 pijlen, 2 gapples, 8 steak, schild. Eigen spullen gaan weg.
- 10 seconden countdown, dan los. Iedereen kan iedereen raken.

**Regels**
- Dood = uit, tribune op. Doodtekst groot in beeld.
- Teamen mag, maar er wint er maar één.
- Na 5 minuten krimpt de worldborder in 2 minuten naar 10 x 10.

**Einde**
- De laatste levende speler is **King of the SMP**.
- Hard maximum 10 minuten; staan er dan nog meerdere, dan beslist het aantal kills.
- Meteen daarna de **kroning** in het midden van de Arena: de kroon op, twintig seconden
  vuurpijlen, iedereen op de tribune. Geen prijs, just for fun.

**Voice:** proximity. Clown zit op de tribune en mag meejoelen.
