# Map en flow: de open wereld

Alles speelt zich af in één open wereld van grofweg 500 x 500 blokken: heuvels, bos, water. De
events zijn in dat landschap gebouwd: een hagendoolhof in een dal, een zwevend Ei aan kettingen,
een mob arena met een plein ervoor, een quizhal, en in het midden **de Arena**: een
colosseum met tribunes waar het eindigt. Geen gangen, geen wachtkamers, geen lobby.

Twee dingen houden het toch strak:

- **Verzamelpunten.** Waar je tussen de rondes staat: een plek in de open lucht (kampvuur,
  banners, een bordje) bij de volgende zone. Daar praat de host en daar begint de countdown.
- **Worldborder per ronde.** De mod zet de worldborder elke ronde om de zone die aan de beurt is.
  Buiten de zone kom je niet, dus je hoeft geen muren om een zone te bouwen.

## Plattegrond

```
                                  N
                 ┌────────────────────────────┐
                 │ 1 DE DOOLHOF               │
                 │ start in het midden,       │
                 │ 4 gangen, 1 echte uitgang  │
                 │ BASISKAMP ernaast          │
                 └─────────────┬──────────────┘
                               │ uitgang → teamkeuze
                               │
 ┌──────────────────┐   ┌──────┴──────────────────┐   ┌────────────────────┐
 │ 4 DE QUIZ        │   │ DE ARENA                │   │ 3 DE MOB ARENA     │
 │ quizhal, 4 banken│   │ colosseum, vloer Ø 70   │   │ arena 1 │ arena 2  │
 │ podium Pudding   │   │ tribunes rondom         │   │ gespiegeld, kooi   │
 │                  │   │ 5 CLOWN VS ALL          │   │ in het midden      │
 └──────────────────┘   │ 6 FFA · 7 FINALE        │   │ V3 = bij de poort  │
                        │ 20 startplekken jagers  │   └────────────────────┘
                        └────────────┬────────────┘
                                     │
                 ┌───────────────────┴────────────┐
                 │ 2 HET EI                       │
                 │ zwevend Ei ±30x30x60, kettingen│
                 │ V2 = bij het Ei                │
                 └────────────────────────────────┘

  V = verzamelpunt. Paden tussen de zones zijn decor; tussen de rondes word je geteleporteerd.
```

## Hoe je van ronde naar ronde gaat

1. Iedereen staat bij het verzamelpunt van de zone. Host doet een praatje.
2. Op het sein van Pudding start de commander de ronde: iedereen naar het startpunt, countdown
   (`title` 5..4..3..2..1), de mod zet de worldborder om de zone.
3. Ronde klaar: iedereen wordt naar het volgende verzamelpunt geteleporteerd. Wie eerder klaar
   is wacht op de tribune (af in de Arena), of loopt nog rond (over de finish van het doolhof: je
   mag terug naar binnen).

| Van | Naar |
|---|---|
| Basiskamp | Startruimte in het midden van het doolhof |
| Doolhof, als iedereen een team heeft | De finishruimte van het doolhof; met `/doolhof naarei` naar V2 bij het Ei |
| Het Ei | Het plein bij de mob arena (V3): de winnaar op het podium, de rest ervoor; met `/ei naarmobarena` naar de tribune van de mob arena |
| Mob arena | Iedereen blijft op de tribune van de mob arena; met `/mobarena naarquiz` naar de quiz: iedereen bij de bank van zijn team, Pudding op het podium |
| Quiz | De tribune van de Arena, voor het Rad; na het Rad de vloer op |
| Clown vs All | Blijft in de Arena: wie af is zit al op de tribune |
| FFA | Blijft in de Arena: de winnaar op de vloer, de rest op de tribune |
| Finale | Kroning op het podium in het midden van de Arena, iedereen op de tribune; na twintig seconden vuurwerk iedereen samen naar het basiskamp |

Lopen is er niet bij: teleporteren, en elke ronde start op het sein van Pudding.

## Worldborder per ronde

| Ronde | Om | Krimp |
|---|---|---|
| 1 De Doolhof | het doolhof | nee |
| 2 Het Ei | het Ei met de kettingen | nee |
| 3 De Mob Arena | het veld met de tribune en het plein | nee |
| 4 De Quiz | de quizhal | nee |
| 5 Clown vs All | de hele Arena inclusief tribunes | alleen als de commander `/clown krimp` doet |
| 6 De FFA | de hele Arena | alleen als de commander `/ffa krimp` doet |
| 7 De Finale | de hele Arena | alleen als de commander `/finale krimp` doet |

Krimpt de border, dan komt de tribune erbuiten. Kijkers krijgen daar geen schade van en zien de
border niet; dat regelt de mod. Levende spelers buiten de border krijgen wel schade, dus altijd
eerst teleporteren, dan de border zetten.

## De zones

| Zone | Waar | Bouwnotities | Gamemode |
|---|---|---|---|
| Basiskamp | Noord, naast het doolhof | Spawnpoint, kampvuur, tenten, regels op borden. | adventure |
| 1 De Doolhof | Noord, in een dal | Hagendoolhof ±64 x 64, hagen 4 hoog, geen plafond. **Een grote startruimte in het midden** met 4 gangen die het doolhof in lopen. Eén gang leidt naar de echte uitgang, met een poort die de mod na 4 minuten opent. De andere drie eindigen in een nep-uitgang: een vak dat de mod herkent. 10 tot 15 kisten in doodlopende gangen, leeg neerzetten: de mod vult ze. 2 of 3 schrikplekken. Achter de uitgang een vak waar je je team kiest. | adventure |
| 2 Het Ei (V2) | Zuid | Een **zwevend Ei** van ±30 x 30 x 60, van ver te zien. De schil van andere blokken, de binnenkant helemaal gewone deepslate (de mod strooit daar de puntenblokken in). Gebruik in de schil en de kettingen geen gewone deepslate. **Kettingen** van de grond naar het Ei, met aan het buiteneinde de startplekken. V2 met kampvuur in de buurt. | survival, alleen het Ei te breken |
| 3 De Mob Arena (V3) | Oost | **Eén veld** waar per beurt van elk team twee spelers tegelijk in staan, met per team twee startplekken, mob-spawnpunten op zelf gekozen plekken, een plek waar de warden uit de grond komt, en **een kooi** van tralies voor wie sneuvelt. Een **tribune** (balkon) met zicht op het veld. Ervoor een **plein met een podium** en een item frame: daar wordt de winnaar van het Ei gehuldigd en krijgt hij het Warden-ei. | adventure |
| 4 De Quiz | West | Een **quizhal** ("?CSMP?" op de muur): 4 **banken** in rood, blauw, groen en geel, bij elke bank een redstone lamp (brandt als dat team aan de beurt is), een **trap met een klein podium** waar Pudding presenteert, en in het midden een rond vloerontwerp met een vraagteken (decor; het rad staat alleen in beeld). Klein genoeg dat iedereen binnen voice-bereik (48 blokken) staat. | adventure |
| De Arena | Midden | Colosseum met een open zandvloer. In het midden een **klein podium** (een pilaartje) waar de kroonhouder spawnt. Rondom op de vloer **20 redstone blocks in een cirkel**: de startplekken van de jagers (één meer dan nodig, voor de symmetrie en zodat de verdeling willekeurig blijft). **Twee tribuneringen**: alleen de onderste wordt gebruikt, de bovenste is decoratie. Ronde 5, 6 en de kroning. | adventure |

**Het Rad** staat alleen in beeld: een rad met de namen van alle spelers dat stopt op Clown. Er
staat niets voor in de wereld. Werking in [04-technische-schets.md](04-technische-schets.md).

**Waarom adventure vs survival:** in adventure mode kun je niet bouwen of breken. Dat wil je
overal behalve bij het Ei, waar je moet minen. Ook daar kun je alleen het Ei zelf breken en nergens
iets neerzetten; dat regelt de mod.

## Wie klaar, af of dood is

Niemand gaat ooit in spectator mode en er zijn geen tp-items. Wie klaar is of af, gaat naar een
plek waar de rest ook komt en hangt daar tot de ronde voorbij is:

- Over de finish van het doolhof, team gekozen: je blijft in het doolhof en mag terug naar
  binnen om te helpen of loot te zoeken. Als het doolhof voorbij is verzamelt iedereen in de
  finishruimte, en met `/doolhof naarei` gaat iedereen samen naar V2.
- Dood in de mob arena: de rest van die beurt in de kooi in het veld, daarna op de
  tribune. Wie niet aan de beurt is, staat ook op de tribune.
- Af in Clown vs All, de FFA of de finale: naar de tribune van de Arena.
- Doodgaan in het doolhof of het Ei kan eigenlijk niet; gebeurt het toch, dan sta je geheald
  terug bij de start van die ronde.
- Na de timer van het doolhof is het doolhof giftig (de finishruimte niet). Wie daar doodgaat,
  raakt zijn spullen kwijt behalve zijn eten, krijgt de basiskit terug en komt in de
  finishruimte. Had hij nog geen team, dan krijgt hij het kleinste (bij gelijk willekeurig).

Op zo'n plek ben je een **kijker**: gewoon in adventure mode, je kunt lopen en praten, maar je
krijgt geen schade, je komt het veld niet op (glas of tralies, en de mod zet je terug), mobs laten
je met rust en je staat niet op de locator bar. Voice is gewoon proximity (zie [07-voice.md](07-voice.md)). Alleen de staff
gebruikt spectator of creative, voor de camera.

## Stream-overwegingen

- Het doolhof is van bovenaf een mooi shot, zeker de startruimte waar mensen na een nep-uitgang
  terugploffen.
- De mob arena: een camera boven het veld. Acht spelers in vier kleuren, en in de kooi
  zie je wie het al niet gered heeft.
- De quiz: vast shot op de hal met de vier gekleurde banken en Pudding op het podium; het rad en
  GOED/FOUT staan in beeld.
- De Arena is het centrale beeld voor het einde: vanaf de tribune zie je de hele vloer.
- Bossbar bovenin het scherm voor timer en status, zodat elke stream dezelfde info in beeld
  heeft.
- De wereld blijft na de bootcamp bestaan. Handig als hub of spawn voor de SMP zelf.
