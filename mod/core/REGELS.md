# Regels die rekenen of beslissen

Elke regel uit [docs/02](../../docs/02-rondes.md) en [docs/03](../../docs/03-kroon-regels.md)
waar de mod iets voor uitrekent of beslist, met de test die hem vastlegt. Regels die alleen
Minecraft aansturen (teleport, heal, geluid) staan hier niet; die zitten in het fabric-project en
worden in-game getest.

| # | Regel | Code | Test |
|---|---|---|---|
| R0.1 | Een regio is twee hoeken, genormaliseerd; center en grootte voor de worldborder komen eruit. De border is vierkant: de langste zijde telt. | `Regio` | `RegioTest.normaliseertHoeken`, `RegioTest.centerEnGrootte` |
| R0.2 | Voor spelers is een regio een kolom (alleen x en z); poorten gebruiken de hele doos. | `Regio.bevat`, `Regio.bevatDoos` | `RegioTest.kolomNegeertHoogte`, `RegioTest.randenHorenErbij` |
| R0.3 | Regio's, punten, doodteksten, slots en de uitverkorene gaan heen en terug door `bootcamp.json`. | `BootcampConfig` | `ConfigTest.heenEnTerug`, `ConfigTest.kapotteJsonGeeftLeesbareFout` |
| R0.4 | Een pilaar heeft één kop: een slot opnieuw uitdelen haalt het bij de vorige weg. Slots lopen van 0 t/m 19. | `BootcampConfig.zetSlot` | `ConfigTest.slotIsUniek`, `ConfigTest.slotBuitenBereik` |
| R0.5 | `/bc start` weigert met de lijst van wat ontbreekt en verandert dan niets. | `Ronde.ontbreekt` | `RondeTest.ontbreektNoemtAlles`, `RondeTest.compleetIsLeeg` |
| R0.6 | Survival alleen in ronde 3; ronde 4, 5 en 6 spelen in de Arena. | `Ronde` | `RondeTest.alleenEiIsSurvival`, `RondeTest.arenaRondes` |
| R0.7 | De doodtekst is willekeurig uit de lijst in de config; een lege lijst valt terug op de drie standaardteksten. | `Doodteksten.kies` | `DoodtekstenTest` |
| R0.8 | `/bc reset` weigert niks: een stap die faalt houdt de rest niet tegen. | `ResetRegister` | `ResetRegisterTest` |
| R1.1 | Timer tien minuten. | `Ronde.DOOLHOF` | `RondeTest.duur` |
| R1.2 | De eerste vijf uit het doolhof krijgen het voorsprongkistje. | `Regels.krijgtVoorsprong` | `RegelsTest.eersteVijfKrijgenVoorsprong` |
| R1.3 | Hint na zeven minuten. | `Regels.DOOLHOF_HINT_BIJ` | `RegelsTest.momenten` |
| R2.1 | Grofweg één mob per speler per wave; de boss wave staat vast. | `Regels.schaalMobs`, `WavesDef` | `RegelsTest.mobsSchalenMee`, `WavesDefTest.totaalSchaalt` |
| R2.2 | De volgende wave start als de vorige dood is, of na twee minuten. | `HordeVerloop` | `HordeVerloopTest.volgendeWaveBijNulMobs`, `HordeVerloopTest.volgendeWaveNaTweeMinuten` |
| R2.3 | De laatste wave wacht niet op de klok: die moet dood. | `HordeVerloop` | `HordeVerloopTest.laatsteWaveWachtOpDeMobs` |
| R2.4 | De ronde stopt bij wave 5 dood of iedereen dood (de timer zit in de ronde). | `HordeVerloop` | `HordeVerloopTest.gewonnen`, `HordeVerloopTest.iedereenDood` |
| R3.3 | Beacon met vijf minuten op de klok, vuurpijl met drie. | `Regels.EI_BEACON_BIJ`, `EI_VUURPIJL_BIJ` | `RegelsTest.momenten` |
| R4.0 | Het rad landt altijd op het doelslot; start en aantal rondes (2 of 3) zijn willekeurig. | `Rad` | `RadTest.landtAltijdOpHetDoel`, `RadTest.tweeOfDrieRondes` |
| R4.0b | Het rad loopt van 2 ticks per stap op naar 30 en wordt nooit sneller. | `Rad.wachttijd` | `RadTest.ritmeLooptOp` |
| R4.1 | Hunters worden om en om over de vier startpunten verdeeld. | `Regels.startpunt` | `RegelsTest.startpuntenOmEnOm` |
| R4.2 | 30 seconden voorsprong bij de start, 10 seconden opstelling na een kroonwissel. | `Regels.OPSTELLING_*`, `Countdown` | `RegelsTest.momenten`, `CountdownTest` |
| R4.3 | Kill de koning en je krijgt de kroon. | `Regels.kroonOpvolger` | `RegelsTest.kroonNaarDeKiller` |
| R4.4 | Zonder killer: de laatste hit; anders een willekeurige levende hunter. Doden krijgen de kroon niet. | `Regels.kroonOpvolger` | `RegelsTest.kroonNaarLaatsteHit`, `RegelsTest.kroonNaarWillekeurigeHunter`, `RegelsTest.dodeKillerTeltNiet` |
| R4.7 | Ronde 4 eindigt bij timer nul of zonder levende hunter. | `Regels.ronde4Voorbij` | `RegelsTest.ronde4Einde` |
| R4.8 | Geen hunter meer als de koning valt: geen opvolger, de ronde is voorbij. | `Regels.kroonOpvolger` | `RegelsTest.geenOpvolgerZonderHunters` |
| R4.10 | Regeerperiodes: elke kroonwissel een nieuwe periode, de langste is de eretitel. | `Regeerperiodes` | `RegeerperiodesTest` |
| R5.1 | Iedereen behalve Clown en finalist 1 doet mee aan de FFA, ook de doden van ronde 4. Clown nooit. | `Regels.ffaDeelnemers` | `RegelsTest.ffaZonderClownEnFinalist`, `RegelsTest.ffaAlsClownFinalistIs` |
| R5.3 | Na vijf minuten krimpt de border in twee minuten naar 10. | `Regels.FFA_KRIMP_*` | `RegelsTest.momenten` |
| R5.5 | Bij tien minuten beslist het aantal kills. Aanname bij gelijke stand: meeste hp, dan het lot. | `Regels.ffaTiebreak` | `RegelsTest.tiebreakOpKills`, `RegelsTest.tiebreakGelijkOpHp` |
| R5.6 | Twee minuten rust voor de finale. | `Regels.RUST` | `RegelsTest.momenten` |
| R6.1 | Border 20, na drie minuten in dertig seconden naar 6. | `Regels.FINALE_*` | `RegelsTest.momenten` |
| R6.2 | Best of 3: wie er twee wint, wint. | `FinaleStand` | `FinaleStandTest` |
| R7.1 | Een hunter die uitlogt telt als dood; net zo voor een FFA-speler en voor wie in de horde staat. | `Regels.bijQuit` | `RegelsTest.quitHunterIsDood` |
| R7.2 | Logt de koning uit, dan dertig seconden wachten; daarna dezelfde kroonwissel als bij een val-dood. | `Regels.bijQuit`, `KONING_UITLOG_WACHT` | `RegelsTest.quitKoningWacht` |
| R7.3 | Logt een finalist uit tijdens de finale, dan gaat het potje naar de tegenstander. (Aanname, zie bouwlog.) | `Regels.bijQuit` | `RegelsTest.quitFinalist` |
| R7.4 | Wie terugkomt in ronde 4 t/m 6 wordt kijker op de tribune. Uitzonderingen: de koning binnen zijn dertig seconden, en een finalist. | `Regels.bijJoin` | `RegelsTest.joinInArena` |
| R7.5 | Wie terugkomt in ronde 1 t/m 3 gaat naar het verzamelpunt of de tribune van dat moment. | `Regels.bijJoin` | `RegelsTest.joinInRondeEenTotDrie` |
| R8.1 | Een kit is per slot een item in `/give`-syntax, met optioneel een aantal. Fouten noemen bestand en slot. | `KitDef` | `KitDefTest` |
| R8.2 | De bossbar heeft per ronde een vast formaat. | `BossbarTekst`, `Tijd` | `BossbarTekstTest` |
| R8.3 | De standaardbestanden in de jar (`fabric.mod.json`, kits, `waves.json`) zijn geldig. | | `ResourcesTest` |
