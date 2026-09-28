# Pudding Bootcamp SMP

Concept voor een Minecraft-bootcamp met zo'n 20 spelers (streamers) als opwarmer voor de SMP.
Zes rondes in één open wereld: solo en in teams, met ClownPierce als eindbaas in Clown vs All en
een FFA waarvan de winnaar **King of the SMP Bootcamp** wordt. Wie in Clown vs All de kroon krijgt,
beslist zogenaamd **Het Rad**. Dat rad is rigged en landt altijd op Clown.

Dit is het complete concept: wat elke ronde is, hoe de teams en de kroon werken, hoe de map in
elkaar zit, hoe je het technisch bouwt en hoe de avond zelf verloopt.

## In het kort

| | |
|---|---|
| **Spelers** | 20, Clown inbegrepen: 4 teams van 5. Werkt vanaf ~8 tot ~30, de getallen schalen mee. |
| **Duur** | ± 2 uur inclusief pauze en praatjes van de host. |
| **Server** | Fabric-server op Minecraft 26.2 (Java 25) met een eigen server-side mod, een server resource pack en Simple Voice Chat. Spelers hebben alleen de voice-mod nodig. |
| **Voice** | Simple Voice Chat, verplicht. Alles proximity, ook de tribune. |
| **Teams** | Kies je bij de uitgang van het doolhof: rood, blauw, groen of geel, max 5. Wie het eerst buiten is, kiest het eerst. Ze tellen in de mob arena en de quiz. |
| **Winnaar** | Per ronde, geen totaalstand. De winnaar van de FFA is King of the SMP Bootcamp. |

## De flow

```
[BASISKAMP] ─ [1 DOOLHOF] ─► teamkeuze ─ [2 HET EI] ─ [3 MOB ARENA] ─ [4 QUIZ]
    solo                                   solo         team            team
                                                                          │
                          HET RAD (rigged: Clown) ─ [5 CLOWN VS ALL] ─────┘
                                                        solo
                                                          │
                                  iedereen behalve Clown ─► [6 FFA] ─► KRONING
```

## De rondes in één zin

| # | Ronde | Wat doe je | Winnaar | Duur |
|---|---|---|---|---|
| 1 | **De Doolhof** | Solo de uitgang vinden: 4 gangen, 3 nep. Uitgang open na 4 minuten. Kisten met betere gear, een jumpscare. Buiten kies je je team. | – | 15 min |
| 2 | **Het Ei** | Solo punten hakken uit een zwevend Ei: netherite 50, diamond 10, gold 5. Redstone is een gok, emerald een jumpscare voor een ander. | meeste punten | 15 min |
| 3 | **De Mob Arena** | Per beurt van elk team één speler in elk van twee arena's, 5 waves. Punten per mob-kill; iedereen speelt twee beurten, tenzij je doodgaat. | meeste punten, 1 team | ± 25 min |
| 4 | **De Quiz** | Pudding presenteert. Een rad in beeld wijst een team aan, dat krijgt vragen tot het fout gaat. Meeste goede antwoorden wint. | 1 team | ± 15 min |
| 5 | **Clown vs All** | Het Rad kiest Clown. Friendly fire uit: alleen de kroonhouder kan doden. Kill de kroonhouder en de kroon is van jou. | laatste over | geen timer |
| 6 | **De FFA** | Iedereen behalve Clown, gelijke kit, iedereen tegen iedereen. | King of the SMP Bootcamp | geen timer |

## Waar staat wat

- [docs/01-map-en-flow.md](docs/01-map-en-flow.md): de open wereld, de zones, de verzamelpunten, de worldborder per ronde.
- [docs/02-rondes.md](docs/02-rondes.md): elke ronde uitgewerkt (doel, setup, regels, einde, winnaar, wat kan misgaan).
- [docs/03-kroon-regels.md](docs/03-kroon-regels.md): Het Rad en de kroon in Clown vs All, met alle randgevallen.
- [docs/04-technische-schets.md](docs/04-technische-schets.md): de Fabric-mod en het resource pack: modules, commands, regio's, kits, spellogica, visuals.
- [docs/05-draaiboek.md](docs/05-draaiboek.md): tijdschema, rollen, bouwlijst, checklists.
- [docs/06-open-keuzes.md](docs/06-open-keuzes.md): alle besluiten en wat nog praktisch open staat.
- [docs/07-voice.md](docs/07-voice.md): Simple Voice Chat en de voice-regels per ronde.
- [docs/08-taakplan.md](docs/08-taakplan.md): het plan om de mod om te bouwen naar dit rondeplan.
- [mod/](mod/README.md): de mod zelf. **Let op:** die implementeert nog het oude rondeplan (horde, King of the SMP, finale) tot het taakplan is uitgevoerd.
