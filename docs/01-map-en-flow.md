# Map en flow: de open wereld

Alles speelt zich af in één open wereld van grofweg 500 x 500 blokken: heuvels, bos, water. De
events zijn in dat landschap gebouwd: een hagendoolhof in een dal, een bos met het Ei erin
verstopt, twee mob-arena's naast elkaar, een quizpodium, en in het midden **de Arena**: een
colosseum met tribunes waar het eindigt. Geen gangen, geen wachtkamers, geen lobby.

Twee dingen houden het toch strak:

- **Verzamelpunten.** Waar je tussen de rondes staat: een plek in de open lucht (kampvuur,
  banners, een bordje) bij de volgende zone. Daar praat de host en daar begint de countdown.
- **Worldborder per ronde.** De mod zet de worldborder elke ronde om de zone die aan de beurt is.
  Buiten de zone kom je niet, dus je hoeft geen muren om het bos te bouwen.

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
 │ podium, 4 vakken │   │ colosseum, vloer Ø 70   │   │ arena A │ arena B  │
 │ in teamkleuren   │   │ tribunes rondom         │   │ tribune ertussen,  │
 │                  │   │ 5 CLOWN VS ALL          │   │ kooi naast elk veld│
 └──────────────────┘   │ 6 FFA · kroning         │   │ V3 = bij de poort  │
                        │ DE KRING = 20 pilaren   │   └────────────────────┘
                        └────────────┬────────────┘
                                     │
                 ┌───────────────────┴────────────┐
                 │ 2 HET EI                       │
                 │ bos + grotten 150 x 150        │
                 │ V2 = bosrand                   │
                 └────────────────────────────────┘

  V = verzamelpunt. Paden tussen de zones zijn decor; tussen de rondes word je geteleporteerd.
```

## Hoe je van ronde naar ronde gaat

1. Iedereen staat bij het verzamelpunt van de zone. Host doet een praatje.
2. Op het sein van Pudding start de commander de ronde: iedereen naar het startpunt, countdown
   (`title` 5..4..3..2..1), de mod zet de worldborder om de zone.
3. Ronde klaar: iedereen wordt naar het volgende verzamelpunt geteleporteerd. Wie eerder klaar
   is (uit het doolhof, af in de Arena) wacht daar of op de tribune.

| Van | Naar |
|---|---|
| Basiskamp | Startruimte in het midden van het doolhof |
| Doolhof, na de teamkeuze | V2 aan de bosrand |
| Het Ei | V3 bij de mob arena |
| Mob arena | De quiz: iedereen in het vak van zijn team |
| Quiz | De vloer van de Arena, voor het Rad |
| Clown vs All | Blijft in de Arena: wie af is zit al op de tribune |
| FFA | Kroning in het midden van de Arena |

Lopen is er niet bij: teleporteren, en elke ronde start op het sein van Pudding.

## Worldborder per ronde

| Ronde | Om | Krimp |
|---|---|---|
| 1 De Doolhof | het doolhof | nee |
| 2 Het Ei | het bos, 150 | nee |
| 3 De Mob Arena | beide arena's met tribune | nee |
| 4 De Quiz | het podium | nee |
| 5 Clown vs All | de hele Arena inclusief tribunes | alleen als de commander `/bc krimp` doet |
| 6 De FFA | de hele Arena | na 5 minuten in 2 minuten naar 10 |

Krimpt de border, dan komt de tribune erbuiten. Kijkers krijgen daar geen schade van en zien de
border niet; dat regelt de mod. Levende spelers buiten de border krijgen wel schade, dus altijd
eerst teleporteren, dan de border zetten.

## De zones

| Zone | Waar | Bouwnotities | Gamemode |
|---|---|---|---|
| Basiskamp | Noord, naast het doolhof | Spawnpoint, kampvuur, tenten, regels op borden. | adventure |
| 1 De Doolhof | Noord, in een dal | Hagendoolhof ±64 x 64, hagen 4 hoog, geen plafond. **Een grote startruimte in het midden** met 4 gangen die het doolhof in lopen. Eén gang leidt naar de echte uitgang, met een poort die de mod na 4 minuten opent. De andere drie eindigen in een nep-uitgang: een vak dat de mod herkent. 10 tot 15 kisten in doodlopende gangen, leeg neerzetten: de mod vult ze. 2 of 3 schrikplekken. Achter de uitgang een vak waar je je team kiest. | adventure |
| 2 Het Ei (V2) | Zuid | Bos met heuvels, grotten en een meertje, 150 x 150. Eén Groot Ei met de puntenblokken, 3 tot 5 nep-eitjes, beacon onder het Ei. V2 met kampvuur aan de bosrand. | survival |
| 3 De Mob Arena (V3) | Oost | **Twee identieke arena's** naast elkaar, elk ±Ø 30 met muur 6 hoog, 4 mob-spawnpunten aan de rand en wat dekking. Tussen de twee een **tribune** met zicht op allebei. Naast elk veld een **kooi**: een glazen hok met zicht op het veld, voor wie in die arena sneuvelt. | adventure |
| 4 De Quiz | West | Een **podium** met 4 vakken in rood, blauw, groen en geel, voor elk vak een redstone lamp. Een plek voor de host ervoor. Klein genoeg dat iedereen binnen voice-bereik (48 blokken) staat. | adventure |
| De Arena | Midden | Colosseum. Vloer Ø 60 tot 80 met dekking (pilaren, muurtjes, wat hoogteverschil), een verhoogd midden voor de kroonhouder, tribunes rondom in twee of drie ringen achter een borstwering. Rond de vloer de 20 pilaren van De Kring met koppen en lichtblokken. Ronde 5, 6 en de kroning. | adventure |

**De Kring:** de 20 pilaren die de vloer van de Arena omringen, op elke pilaar een spelerskop en
eronder een lichtblok. Dat is Het Rad: het licht loopt de arena rond en stopt op Clown. Bouw en
werking staan in [04-technische-schets.md](04-technische-schets.md).

**Waarom adventure vs survival:** in adventure mode kun je niet bouwen of breken. Dat wil je
overal behalve in het Ei-bos, waar je moet minen.

## Wie klaar, af of dood is

Niemand gaat ooit in spectator mode en er zijn geen tp-items. Wie klaar is of af, gaat naar een
plek waar de rest ook komt en hangt daar tot de ronde voorbij is:

- Uit het doolhof, team gekozen: naar V2 aan de bosrand.
- Dood in de mob arena: in de kooi van je arena. Speelt je team niet: op de tribune tussen de
  arena's.
- Af in Clown vs All of de FFA: naar de tribune van de Arena.
- Doodgaan in het doolhof of het Ei kan eigenlijk niet; gebeurt het toch, dan sta je geheald
  terug bij de start van die ronde.

Op zo'n plek ben je een **kijker**: gewoon in adventure mode, je kunt lopen en praten, maar je
krijgt geen schade, je komt het veld niet op (glas, en de mod zet je terug) en je staat niet op de
locator bar. Voice is gewoon proximity (zie [07-voice.md](07-voice.md)). Alleen de staff
gebruikt spectator of creative, voor de camera.

## Stream-overwegingen

- Het doolhof is van bovenaf een mooi shot, zeker de startruimte waar mensen na een nep-uitgang
  terugploffen.
- De mob arena: één camera boven de tribune ziet beide arena's, dus je ziet de race tussen twee
  teams in één beeld.
- De quiz: vast shot op het podium met de vier gekleurde vakken en de lampen.
- De Arena is het centrale beeld voor het einde: vanaf de tribune zie je de hele vloer.
- Bossbar bovenin het scherm voor timer en status, zodat elke stream dezelfde info in beeld
  heeft.
- De wereld blijft na de bootcamp bestaan. Handig als hub of spawn voor de SMP zelf.
