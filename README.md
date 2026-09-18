# Pudding Bootcamp SMP

Concept voor een Minecraft-bootcamp met zo'n 20 spelers (streamers) als opwarmer voor de SMP.
Zes rondes, één lange gang met poorten die je van level naar level leidt, en aan het eind een
**King of the SMP**-finale met ClownPierce als eindbaas.

Dit is het complete concept: wat elke ronde is, hoe de kroon werkt, hoe de map in elkaar zit,
hoe je het technisch bouwt en hoe de avond zelf verloopt. Niet fancy, wel compleet.

## In het kort

| | |
|---|---|
| **Spelers** | 20 (19 streamers + ClownPierce). Werkt vanaf ~8 tot ~30, de getallen schalen mee. |
| **Duur** | ± 2 uur inclusief pauzes en praatjes van de host. |
| **Server** | Java, één wereld, alle zones naast elkaar. Vanilla + datapack is genoeg, geen plugins nodig. |
| **Eliminatie** | Niemand ligt eruit vóór ronde 4. Elke streamer heeft dus minstens een uur content. |
| **Winnaar** | De speler die de finale (1v1 tussen de twee kroondragers) wint, is King of the SMP. |

Het bootcamp-idee: elke ronde traint iets wat je op de SMP nodig hebt. Navigeren, mobs, looten
onder tijdsdruk, en uiteindelijk PvP.

## De flow

```
[LOBBY] ═P1═ [1 DE DOOLHOF] ═P2═ [2 DE HORDE] ═P3═ [3 HET EI] ═P4═ [4 KING OF THE SMP]
                                                                          │
                                        kroondrager bij einde timer ──► FINALIST 1
                                        iedereen zonder kroon (nog in leven) ──► [5 ARENA FFA]
                                                                                       │
                                                          laatste die overblijft ──► FINALIST 2
                                                                                       │
                                                                          [6 DE FINALE: 1v1, best of 3]
```

`P1` t/m `P4` zijn poorten in De Gang (zie [docs/01-map-en-flow.md](docs/01-map-en-flow.md)).
Vanaf ronde 4 gaat het met teleports, omdat de groep dan uit elkaar valt.

## De rondes in één zin

| # | Ronde | Wat doe je | Duur | Wat train je |
|---|---|---|---|---|
| 1 | **De Doolhof** | Vind de uitgang van het doolhof. Kistjes in doodlopende gangen. | 10 min | Navigeren, oriëntatie |
| 2 | **De Horde** | Overleef 5 waves mobs als groep in een arena. | 10 min | Mobs, boog, schild, samenwerken |
| 3 | **Het Ei** | Vind het Grote Ei, hak je naar binnen, pak loot + een diamond block als ticket. | 10 min | Zoeken, minen, looten onder druk |
| 4 | **King of the SMP** | Clown heeft de kroon. 19v1. Kill de koning en je krijgt de kroon. Wie hem heeft als de timer afloopt is finalist 1. | 15 min | PvP, chaos, overleven |
| 5 | **Arena FFA** | Iedereen zonder kroon, full hp, één arena. Laatste die overblijft is finalist 2. | max 10 min | PvP |
| 6 | **De Finale** | 1v1 tussen de twee kroondragers, best of 3. | ± 10 min | PvP |

## Waar staat wat

- [docs/01-map-en-flow.md](docs/01-map-en-flow.md): De Gang, de poorten, de zones en hoe spelers per level worden doorgeleid.
- [docs/02-rondes.md](docs/02-rondes.md): elke ronde uitgewerkt (doel, setup, regels, einde, bonus, wat kan misgaan).
- [docs/03-kroon-regels.md](docs/03-kroon-regels.md): de kroonmechaniek van ronde 4 tot en met de finale, inclusief alle randgevallen.
- [docs/04-technische-schets.md](docs/04-technische-schets.md): hoe je het bouwt met een datapack (tags, teams, timer, kroon-overdracht, poorten).
- [docs/05-draaiboek.md](docs/05-draaiboek.md): tijdschema van de avond, rollen van de staff, checklists.
- [docs/06-open-keuzes.md](docs/06-open-keuzes.md): beslissingen die nog gemaakt moeten worden, met een aanbeveling per stuk.

## Nog te beslissen

De belangrijkste open keuze: liggen ex-koningen (spelers die de kroon hadden en doodgingen) uit
het hele event, of doen ze mee aan de FFA? Zie [docs/06-open-keuzes.md](docs/06-open-keuzes.md)
voor die en de andere keuzes.
