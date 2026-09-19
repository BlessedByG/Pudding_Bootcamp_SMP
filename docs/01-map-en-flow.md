# Map en flow: de open wereld

Alles speelt zich af in één open wereld van grofweg 500 x 500 blokken: heuvels, bos, water. De
events zijn in dat landschap gebouwd. Een hagendoolhof in een dal, een ruïne-arena voor de horde,
een bos met het Ei erin verstopt, en in het midden **de Arena**: een colosseum met tribunes waar
het eindigt. Geen gangen, geen wachtkamers, geen lobby.

Twee dingen houden het toch strak:

- **Verzamelpunten.** Elke zone heeft bij de ingang een plek in de open lucht (kampvuur, banners,
  een bordje) waar iedereen tussen de rondes staat. Daar praat de host, daar staan de
  voorsprongkistjes, daar begint de countdown.
- **Worldborder per ronde.** De mod zet de worldborder elke ronde om de zone die aan de beurt
  is. Buiten de zone kom je niet, dus je hoeft geen muren om het bos te bouwen.

## Plattegrond

```
                                  N
                 ┌────────────────────────────┐
                 │ 1 DE DOOLHOF               │
                 │ hagendoolhof 64 x 64       │
                 │ V1 = BASISKAMP bij de poort│
                 └─────────────┬──────────────┘
                               │ uitgang
                               │
 ┌──────────────────┐   ┌──────┴──────────────────┐   ┌────────────────────┐
 │ vrij terrein     │   │ DE ARENA                │   │ 2 DE HORDE         │
 │ (bos, meer,      │   │ colosseum, vloer Ø 70   │   │ ruïne-arena Ø 50   │
 │  heuvels)        │   │ tribunes rondom         │   │ V2 = bij de poort  │
 │                  │   │                         │   └────────────────────┘
 └──────────────────┘   │ 4 KING OF THE SMP       │
                        │ 5 FFA · 6 FINALE        │
                        │ kroning                 │
                        │ DE KRING = 20 pilaren   │
                        │ V4 = de vloer zelf      │
                        └────────────┬────────────┘
                                     │
                 ┌───────────────────┴────────────┐
                 │ 3 HET EI                       │
                 │ bos + grotten 150 x 150        │
                 │ V3 = bosrand                   │
                 └────────────────────────────────┘

  V = verzamelpunt. Paden tussen de zones zijn decor; tussen de rondes word je geteleporteerd.
```

De Arena staat in het midden omdat daar alles eindigt: ronde 4, de FFA, de finale en de kroning.
Rond de vloer lopen de tribunes waar de doden op komen. De andere zones liggen eromheen.

## Hoe je van ronde naar ronde gaat

1. Iedereen staat bij het verzamelpunt van de zone. Host doet z'n praatje.
2. Op het sein van Pudding start de commander de ronde: countdown (`title` 5..4..3..2..1), bij
   het doolhof, de horde-arena en de bosrand gaat een poort open (een muur die met één command
   weggaat), daarna zet de mod de worldborder om de zone.
3. Ronde klaar (timer of doel gehaald): iedereen wordt naar het volgende verzamelpunt
   geteleporteerd. Wie eerder klaar is komt bij de uitgang van de zone uit en wordt meteen
   doorgezet. Daar staan de voorsprongkistjes.
4. Ronde 4 begint in de Arena zelf: iedereen op de vloer, het rad loopt langs de 20 pilaren rond
   de vloer. Daarna Clown naar het midden en de hunters naar hun startpunten aan de rand.
5. Vanaf ronde 4 blijft iedereen in de Arena. Wie doodgaat, gaat de tribune op. De FFA en de
   finale zijn op dezelfde vloer, de kroning ook.

Lopen is er niet bij: teleporteren, en elke ronde start op het sein van Pudding. Zo staat
iedereen tegelijk op de goede plek en gaat de tijd in de rondes in plaats van in het wandelen.

## Worldborder per ronde

| Ronde | Center | Grootte |
|---|---|---|
| 1 De Doolhof | midden van het doolhof | 80 |
| 2 De Horde | midden van de ruïne-arena | 60 |
| 3 Het Ei | midden van het bos | 150 |
| 4 King of the SMP | midden van de Arena | de hele Arena inclusief tribunes, geen krimp |
| 5 Arena FFA | midden van de Arena | de hele Arena, na 5 minuten krimpt hij naar 10 |
| 6 De Finale | midden van de Arena | 20, na 3 minuten naar 6 |

Krimpt de border in de FFA of de finale, dan komt de tribune erbuiten. Kijkers krijgen daar geen
schade van, dat vangt de mod af. De vloer komen ze niet op: glas tussen tribune en vloer, en de mod
zet iedereen die er toch komt terug. Levende spelers buiten de border krijgen wel schade, dus
altijd eerst teleporteren, dan de border zetten.

## De zones

| Zone | Waar | Bouwnotities | Gamemode |
|---|---|---|---|
| Basiskamp (V1) | Noord, bij de poort van het doolhof | Spawnpoint, kampvuur, tenten, regels op borden, wat targets om te warmen. | adventure |
| 1 De Doolhof | Noord, in een dal | Hagendoolhof 64 x 64, hagen 4 hoog, geen plafond nodig: adventure mode en niemand heeft nog pearls. Genereer met een maze-generator (WorldEdit-script of online generator naar schematic). 10 tot 15 kisten in doodlopende gangen. | adventure |
| 2 De Horde (V2) | Oost | Ruïne-arena Ø 50, muur 6 hoog, 4 mob-spawnpunten aan de rand, wat dekking in het midden, een tribune met zicht op de vloer voor de doden. Verzamelpunt bij de poort. | adventure |
| 3 Het Ei (V3) | Zuid | Bos met heuvels, grotten en een meertje, 150 x 150. Eén Groot Ei, 3 tot 5 nep-eitjes, beacon onder het Ei. Verzamelpunt met kampvuur aan de bosrand. | survival |
| De Arena (V4) | Midden | Colosseum. Vloer Ø 60 tot 80 met dekking (pilaren, muurtjes, wat hoogteverschil), een verhoogd midden voor de koning, tribunes rondom in twee of drie ringen achter een borstwering, een poort. Rond de vloer de 20 pilaren van De Kring met koppen en lichtblokken. Ronde 4, 5, 6 en de kroning. | adventure |

**De Kring:** de 20 pilaren die de vloer van de Arena omringen, op elke pilaar een spelerskop en
eronder een lichtblok. Dat is Het Rad: het licht loopt de arena rond en stopt op de koning. Bouw
en werking staan in [04-technische-schets.md](04-technische-schets.md).

**Waarom adventure vs survival:** in adventure mode kun je niet bouwen of breken. Dat wil je in
het doolhof (niet door de haag), de horde (niet inbouwen) en de Arena (in een arena met één leven
is inbouwen dodelijk saai, en de vloer blijft heel). Alleen in het Ei-bos moet je minen, dus daar
survival.

## Wie klaar of dood is

Niemand gaat ooit in spectator mode en er zijn geen tp-items. Wie klaar is met een ronde, of dood
is, gaat naar een plek waar de rest ook komt en hangt daar samen tot de ronde voorbij is:

- Uit het doolhof: naar verzamelpunt 2 bij de horde-arena.
- Dood in de horde: naar de tribune van de ruïne-arena, met zicht op de vloer.
- Ticket uit het Ei-bos: naar de vloer van de Arena, waar ronde 4 begint.
- Dood in de Arena (ronde 4, 5 en 6): naar de tribune van de Arena.

Op zo'n plek ben je een **kijker**: gewoon in adventure mode, je kunt lopen en praten, maar je
krijgt geen schade, je komt de vloer niet op (glas, en de mod zet je terug) en je staat niet op de
locator bar. Voice is gewoon proximity: de vloer hoort de tribune en andersom (zie
[07-voice.md](07-voice.md)). De doden op de tribune zijn het publiek. Alleen de staff gebruikt
spectator of creative, voor de camera.

## Stream-overwegingen

- Het doolhof is van bovenaf een mooi shot (kijker boven de hagen).
- De Arena is het centrale beeld: vanaf de tribune zie je de hele vloer, en de tribune vol doden
  is zelf ook een shot.
- Elk verzamelpunt is een vaste camerapositie voor de host: iedereen staat bij elkaar rond het
  kampvuur.
- Bossbar bovenin het scherm voor timer en status (wave, koning, tijd), zodat elke stream
  dezelfde info in beeld heeft.
- De wereld blijft na de bootcamp gewoon bestaan. Handig als hub of spawn voor de SMP zelf.
