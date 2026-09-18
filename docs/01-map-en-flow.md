# Map en flow: De Gang

Het idee: één wereld, alle zones op een rij, aan elkaar geknoopt door **De Gang**. De Gang is
een brede, rechte gang met genummerde poorten. Je komt een zone binnen door een poort, je komt
er aan de andere kant weer uit in een **wachtkamer** in De Gang, en daar wacht je voor de
volgende poort. Zo word je zonder nadenken van level naar level geleid en heeft de host tussen
elke ronde een natuurlijk moment om te praten.

Hoeft niet mooi. Stone bricks, glas, een paar lanterns, klaar.

## Layout

```
  ┌────────────┐      ┌────────────┐      ┌────────────┐      ┌──────────────┐
  │ 1 DOOLHOF  │      │ 2 HORDE    │      │ 3 HET EI   │      │ 4 KING ZONE  │
  │ 64 x 64    │      │ arena Ø 50 │      │ 150 x 150  │      │ 200 x 200    │
  └──┬──────┬──┘      └──┬──────┬──┘      └──┬──────┬──┘      └──┬───────────┘
   in│      │uit       in│      │uit       in│      │uit       in│
 ════╧══════╧════════════╧══════╧════════════╧══════╧════════════╧═══════════
 LOBBY ·P1·    WK2     ·P2·    WK3     ·P3·    WK4     ·P4·            DE GANG

  P = poort, WK = wachtkamer. Je gaat door P1 het doolhof in, komt er via "uit" weer uit in
  wachtkamer 2, en wacht daar voor P2. Enzovoort.

  Los van De Gang, alleen via teleport:
  ┌──────────────┐   ┌──────────────┐   ┌────────────────────┐
  │ 5 FFA ARENA  │   │ 6 1v1 ARENA  │   │ SPECTATOR-DECK     │
  │ Ø 40         │   │ 20 x 20      │   │ (of gewoon         │
  └──────────────┘   └──────────────┘   │  spectator mode)   │
                                        └────────────────────┘
```

Zet de zones een paar honderd blokken uit elkaar of scheid ze met dikke muren, zodat je nooit
per ongeluk van de ene zone in de andere kijkt of loopt.

## Hoe een poort werkt

1. Iedereen staat in de wachtkamer. Glazen wand richting de volgende zone, dus je ziet al waar
   je heen gaat. Host doet z'n praatje.
2. Admin start een countdown (`title` 5..4..3..2..1) en haalt de poort weg met één `fill`
   command (poort is gewoon een muur van iron bars of stone bricks).
3. Iedereen rent naar binnen. Na 10 seconden zet de admin de poort terug, zodat niemand terug kan.
4. De zone-uitgang komt uit in de volgende wachtkamer. Wie klaar is, wacht daar (achter glas,
   kan niet meer terug de zone in).
5. Loopt de timer van de ronde af, dan wordt iedereen die nog in de zone zit naar de wachtkamer
   geteleporteerd. Niemand blijft hangen.

Wachtkamers zijn ook de plek voor de **voorsprongkistjes** (bonus uit de vorige ronde, zie
[02-rondes.md](02-rondes.md)).

## Wachtkamer 4: Het Rad

Wachtkamer 4 is groter dan de andere (zeg 15 x 10) en heeft op de achterwand **Het Rad**: een
cirkel van alle 20 spelerskoppen, met onder elke kop een lichtblok. Aan het begin van ronde 4
gaat het licht rond, steeds langzamer, tot het stopt op de koning. Iedereen staat ervoor, de
stream kijkt mee. Het rad is rigged en stopt altijd op Clown; hoe dat werkt staat in
[03-kroon-regels.md](03-kroon-regels.md) en [04-technische-schets.md](04-technische-schets.md).

## Vanaf ronde 4: teleports

Na ronde 4 valt de groep uit elkaar (finalist 1, de rest naar de FFA, uitgeschakelde spelers naar
spectator). Poorten werken dan niet meer, dus:

- Einde ronde 4: kroondrager blijft staan (of gaat naar het spectator-deck om te kijken), de rest
  wordt naar de FFA-arena geteleporteerd.
- Einde ronde 5: de twee finalisten worden naar de 1v1-arena geteleporteerd.
- Uitgeschakelde spelers gaan in spectator mode. Vraag ze om binnen hun zone te blijven kijken
  (voor de stream), maar het hoeft niet hard afgedwongen.

## De zones

| Zone | Formaat | Gamemode | Bouwnotities |
|---|---|---|---|
| Lobby | 30 x 30 | adventure | Spawn, regels op borden, een paar targets om te warmen. |
| 1 De Doolhof | 64 x 64, muren 4 hoog, dicht plafond | adventure | Genereer met een maze-generator (WorldEdit-script of online generator naar schematic). Plafond van barriers of bedrock zodat niemand eroverheen kan. 10 tot 15 kisten in doodlopende gangen. |
| 2 De Horde | ronde arena Ø 50, muur 6 hoog | adventure | 4 mob-spawnpunten aan de rand, wat dekking (pilaren, muurtjes) in het midden. Spelersrespawn aan de rand. |
| 3 Het Ei | 150 x 150 zoekgebied | survival | Natuurlijk terrein is prima: bos, heuvels, een paar grotten, een meertje. Muur of worldborder eromheen. Eén Groot Ei, 3 tot 5 nep-eitjes. |
| 4 King zone | 200 x 200 open map | survival | Natuurlijk terrein plus een paar structures: dorpje, burcht in het midden, toren, bos, water. Hunters spawnen op 4 punten aan de rand, de koning in de burcht. Worldborder-shrink in de laatste minuten. |
| 5 FFA-arena | Ø 40, plat met wat dekking | adventure | Simpel. Worldborder-shrink na 5 minuten. |
| 6 1v1-arena | 20 x 20 | adventure | Simpel. Twee startpunten tegenover elkaar. |
| Spectator-deck | glazen platform boven elke zone | spectator | Optioneel, spectator mode is genoeg. |

**Waarom adventure vs survival:** in adventure mode kun je niet bouwen of breken. Dat wil je in
het doolhof (niet door de muur), de horde (niet inbouwen) en de arena's (puur vechten). In de
Ei-zone moet je juist minen, en in de King zone mag bouwen omdat dat SMP-achtig is (pillaren,
trap bouwen, inbouwen op je laatste hartje).

## Stream-overwegingen

- Het doolhof is van bovenaf een mooi shot (spectator boven het plafond, plafond van barriers is
  dan onzichtbaar).
- Elke wachtkamer is een vaste "camerapositie" voor de host: iedereen staat bij elkaar.
- Bossbar bovenin het scherm voor timer en status (wave, koning, tijd), zodat elke stream
  dezelfde info in beeld heeft.
