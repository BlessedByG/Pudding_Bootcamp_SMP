# Map en flow: de open wereld

Alles speelt zich af in één open wereld van grofweg 500 x 500 blokken: heuvels, bos, water. De
events zijn in dat landschap gebouwd. Een hagendoolhof in een dal, een ruïne-arena, een bos met
het Ei erin verstopt, en in het midden op een heuvel de burcht waar het eindigt. Geen gangen, geen
wachtkamers, geen lobby.

Twee dingen houden het toch strak:

- **Verzamelpunten.** Elke zone heeft bij de ingang een plek in de open lucht (kampvuur, banners,
  een bordje) waar iedereen tussen de rondes staat. Daar praat de host, daar staan de
  voorsprongkistjes, daar begint de countdown.
- **Worldborder per ronde.** Skript zet de worldborder elke ronde om de zone die aan de beurt
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
 │ vrij terrein     │   │ 4 KING ZONE 200 x 200   │   │ 2 DE HORDE         │
 │ (bos, meer,      │   │                         │   │ ruïne-arena Ø 50   │
 │  heuvels)        │   │   ┌─────────────────┐   │   │ V2 = bij de poort  │
 │                  │   │   │ BURCHT          │   │   └────────────────────┘
 └──────────────────┘   │   │ binnenplaats:   │   │
                        │   │   5 FFA ARENA   │   │
                        │   │ troonzaal:      │   │
                        │   │   6 FINALE      │   │
                        │   └─────────────────┘   │
                        │ V4 = DE KRING (het rad) │
                        └────────────┬────────────┘
                                     │
                 ┌───────────────────┴────────────┐
                 │ 3 HET EI                       │
                 │ bos + grotten 150 x 150        │
                 │ V3 = bosrand                   │
                 └────────────────────────────────┘

  V = verzamelpunt. Paden tussen de zones zijn decor; tussen de rondes word je geteleporteerd.
```

De burcht staat in het midden omdat daar alles samenkomt: de King zone eromheen, de FFA op de
binnenplaats, de finale en de kroning in de troonzaal. De andere zones liggen op minstens 100
blokken van de burcht, buiten de border van ronde 4.

## Hoe je van ronde naar ronde gaat

1. Iedereen staat bij het verzamelpunt van de zone. Host doet z'n praatje.
2. Countdown (`title` 5..4..3..2..1). Bij het doolhof, de arena en de bosrand gaat een poort open
   (een muur die de admin met één command weghaalt). Daarna zet Skript de worldborder om de
   zone.
3. Ronde klaar (timer of doel gehaald): iedereen wordt naar het volgende verzamelpunt
   geteleporteerd. Wie eerder klaar is komt bij de uitgang van de zone uit en wordt meteen
   doorgezet, of loopt het stukje zelf. Daar staan de voorsprongkistjes.
4. Ronde 4 begint bij De Kring voor de poort van de burcht. Na het rad worden Clown en de hunters
   naar hun startpunten geteleporteerd.
5. Na ronde 4 valt de groep uit elkaar: finalist 1 op de muur van de binnenplaats, de rest naar
   de FFA, de doden in spectator.

Teleporteren in plaats van lopen is een keuze: iedereen staat tegelijk op de goede plek, de host
heeft z'n moment en niemand loopt te dwalen. Wil je meer open-world-gevoel, laat ze dan lopen
over gemarkeerde paden; zie [06-open-keuzes.md](06-open-keuzes.md).

## Worldborder per ronde

| Ronde | Center | Grootte |
|---|---|---|
| 1 De Doolhof | midden van het doolhof | 80 |
| 2 De Horde | midden van de arena | 60 |
| 3 Het Ei | midden van het bos | 150 |
| 4 King of the SMP | burcht | 200, in sudden death naar 60 |
| 5 Arena FFA | binnenplaats | 40, na 5 minuten naar 10 |
| 6 De Finale | troonzaal | 20, na 3 minuten naar 6 |

Spectators vliegen dwars door de border heen, dus doden en staff hebben er geen last van. Wie
buiten de border staat op het moment dat hij gezet wordt krijgt schade, dus altijd eerst
teleporteren, dan de border zetten.

## De zones

| Zone | Waar | Bouwnotities | Gamemode |
|---|---|---|---|
| Basiskamp (V1) | Noord, bij de poort van het doolhof | Spawnpoint, kampvuur, tenten, regels op borden, wat targets om te warmen. | adventure |
| 1 De Doolhof | Noord, in een dal | Hagendoolhof 64 x 64, hagen 4 hoog, geen plafond nodig: adventure mode en niemand heeft nog pearls. Genereer met een maze-generator (WorldEdit-script of online generator naar schematic). 10 tot 15 kisten in doodlopende gangen. | adventure |
| 2 De Horde (V2) | Oost | Ruïne-arena Ø 50, muur 6 hoog, 4 mob-spawnpunten aan de rand, wat dekking in het midden. Verzamelpunt bij de poort. | adventure |
| 3 Het Ei (V3) | Zuid | Bos met heuvels, grotten en een meertje, 150 x 150. Eén Groot Ei, 3 tot 5 nep-eitjes, beacon onder het Ei. Verzamelpunt met kampvuur aan de bosrand. | survival |
| 4 King zone (V4) | Midden, 200 x 200 rond de burcht | Burcht op een heuvel, dorpje, toren, bos, water. 4 hunterspawns met hekjes aan de rand. De Kring voor de poort van de burcht. | survival |
| 5 Arena FFA | Binnenplaats van de burcht | Ø 40, plat met wat dekking (putrand, karren, pilaren). | adventure |
| 6 De Finale | Troonzaal van de burcht (of het dak) | 20 x 20, twee startpunten tegenover elkaar. Hier is ook de kroning. | adventure |

**De Kring:** 20 pilaren in een cirkel, op elke pilaar een spelerskop en eronder een lichtblok.
Dat is Het Rad in de open lucht. Bouw en werking staan in
[04-technische-schets.md](04-technische-schets.md).

**Waarom adventure vs survival:** in adventure mode kun je niet bouwen of breken. Dat wil je in
het doolhof (niet door de haag), de horde (niet inbouwen) en de arena's (puur vechten). In het
Ei-bos moet je juist minen, en in de King zone mag bouwen omdat dat SMP-achtig is (pillaren,
trap bouwen, inbouwen op je laatste hartje).

## Spectators

Gewoon spectator mode. Doden en staff vliegen vrij rond, door de border heen. Vraag de doden om
in de buurt van de actie te blijven kijken (voor hun eigen stream), meer regels zijn er niet.

## Stream-overwegingen

- Het doolhof is van bovenaf een mooi shot (spectator boven de hagen).
- De burcht is het centrale beeld: vanaf de muur zie je de hele King zone, de binnenplaats en de
  troonzaal.
- Elk verzamelpunt is een vaste camerapositie voor de host: iedereen staat bij elkaar rond het
  kampvuur.
- Bossbar bovenin het scherm voor timer en status (wave, koning, tijd), zodat elke stream
  dezelfde info in beeld heeft.
- De wereld blijft na de bootcamp gewoon bestaan. Handig als hub of spawn voor de SMP zelf.
