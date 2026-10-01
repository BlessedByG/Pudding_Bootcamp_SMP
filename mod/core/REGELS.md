# Regels die rekenen of beslissen

Elke regel uit [docs/02](../../docs/02-rondes.md), [docs/03](../../docs/03-kroon-regels.md) en
[docs/04](../../docs/04-technische-schets.md) waar de mod iets voor uitrekent of beslist, met de
test die hem vastlegt. Regels die alleen Minecraft aansturen (teleport, heal, geluid) staan hier
niet; die zitten in het fabric-project en worden in-game getest.

Dit is de lijst van het rondeplan van 25 september 2026 (taakplan 2). De regels van het oude plan
(voorsprong, ticket, horde, finale, rust) zijn eruit.

| # | Regel | Code | Test |
|---|---|---|---|
| R0.1 | Een regio is één of meer dozen, genormaliseerd; center en grootte voor de worldborder komen uit de doos om alles heen. De border is vierkant: de langste zijde telt. | `Regio` | `RegioTest.normaliseertHoeken`, `RegioTest.centerEnGrootte` |
| R0.2 | Voor spelers is een doos een kolom (alleen x en z); poorten, kistenscan en het Ei gebruiken de hele doos. | `Regio.bevat`, `Regio.bevatDoos` | `RegioTest.kolomNegeertHoogte`, `RegioTest.randenHorenErbij` |
| R0.3 | Een regio mag uit meerdere delen bestaan (de T van een veld); een plek hoort erbij als hij in één deel ligt, delen mogen overlappen. | `Regio.metDeel` | `RegioTest.tVormUitTweeDelen`, `RegioTest.overlappendeDelenMogen` |
| R0.4 | De vloer is een cilinder: binnen de cirkel én binnen 5 blokken hoogte vanaf de voeten. De border gebruikt de doorsnede. | `Regio.cilinder` | `RegioTest.cilinderTeltOokDeHoogte` |
| R0.5 | Een veld telt tot 3 blokken boven de selectie (instelbaar met `/mobarena veldhoogte`, 0 t/m 10), zodat het balkon erboven geen veld is. | `Regio.bevatSpelerTot` | `RegioTest.veldTeltTotEenPaarBlokkenBovenDeSelectie` |
| R0.6 | Regio's (ook met delen en de cilinder), punten, doodteksten, grapjes, teamkeuzes, uitverkorene, presentator, de uitslag voor de finale en instellingen gaan heen en terug door `bootcamp.json`; de oude vorm met één doos wordt nog gelezen, en een oude enkele lamp `quizlamp_<kleur>` wordt `quizlamp_<kleur>_1`. | `BootcampConfig` | `ConfigTest.heenEnTerug`, `ConfigTest.oudeVormMetEenDoosEnSlotsWordtGelezen`, `ConfigTest.oudeLampWordtLampEen`, `ConfigTest.kapotteJsonGeeftLeesbareFout` |
| R0.7 | `/<ronde> start` weigert met de lijst van wat ontbreekt en verandert dan niets. Van een genummerde reeks moet er minstens één zijn. | `Ronde.ontbreekt`, `Ronde.reeks` | `RondeTest.ontbreektNoemtAlles`, `RondeTest.compleetIsLeeg`, `RondeTest.reeksTotHetEersteGat` |
| R0.8 | Zeven rondes met elk een eigen commando; survival alleen in het Ei; ronde 5, 6 en de finale (7) spelen in de Arena, ronde 1 t/m 4 met de teamkleuren. | `Ronde` | `RondeTest.nummersEnCommandos`, `RondeTest.alleenEiIsSurvival`, `RondeTest.arenaEnTeams` |
| R0.9 | De doodtekst is willekeurig uit de lijst in de config; een lege lijst valt terug op de zes standaardteksten. Staat er nog precies de oude standaard (drie teksten), dan wordt het bij het laden de nieuwe. | `Doodteksten.kies`, `BootcampConfig` | `DoodtekstenTest`, `ConfigTest.oudeStandaardDoodtekstenWordenDeNieuwe` |
| R0.10 | PvP: ronde 0 t/m 4 nooit, ronde 5 alleen met de kroonhouder, ronde 6 en de finale altijd; nooit tijdens een opstelling of countdown, nooit met een kijker of staff. | `PvpRegel.mag` | `PvpTest.deTabelUitDocs04`, `PvpTest.nooitTijdensEenOpstellingOfMetEenKijker` |
| R0.11 | `/bc reset` weigert niks: een stap die faalt houdt de rest niet tegen. | `ResetRegister` | `ResetRegisterTest` |
| R0.12 | De instellingen hebben standaarden en grenzen: doolhof-timer 5 t/m 60 en later dan poort en hint, poort 0 tot de timer, hint 1 tot de timer, Ei-timer 5 t/m 60, mobpunten 0 t/m 100, valkistmobs 0 t/m 20 (standaard willekeurig 3 t/m 10, min niet boven max), schrikfoto per plek 1 t/m 5 of willekeurig (standaard), poortmelding standaard aan, teksten tot 60 tekens. Een lopende timer kan niet korter dan wat er al gespeeld is. | `Instellingen` | `InstellingenTest` |
| R0.13 | Op een tribune (regio `tribune_mob`) sta je vanaf de selectie tot 3 blokken erboven, niet eronder: wie onder een overhang op het veld staat, staat er niet op. Daar telt niemand als in een veld. | `Regio.staatOp` | `RegioTest.opDeTribuneTeltVanafDeVloer` |
| R1.1 | Het maximum per team is `max(4, ceil(spelers / 4))`; een vol team kun je niet kiezen. | `TeamKeuze.maximum`, `TeamKeuze.vol` | `TeamKeuzeTest.maximumIsVierOfMeer`, `TeamKeuzeTest.volBijHetMaximum` |
| R1.2 | Wie bij het einde van het doolhof geen team heeft, gaat naar het kleinste team; bij gelijk het lot. | `TeamKeuze.kleinste` | `TeamKeuzeTest.kleinsteTeamBijGelijkHetLot` |
| R1.3 | Kisten krijgen 2 t/m 4 items uit de loot-tabel, gewogen getrokken. Geen pearls. | `LootTabel` | `LootTest`, `ResourcesTest.lootIsGeldig` |
| R1.4 | Een valkist in het doolhof: 25% de jumpscare, 25% de 8D-klop, 50% de mobs (bij `/doolhof valmobs 0` de jumpscare of de klop, 50/50). | `Regels.valkistGok` | `RegelsTest.valkistGokIsKwartKwartHelft`, `RegelsTest.valkistZonderMobsIsSchrikOfKlop` |
| R1.6 | Een jumpscare laat een van de vijf foto's zien: per schrikplek een vaste (1 t/m 5) of willekeurig; valkisten, het Ei en `/bc schrik` zonder foto altijd willekeurig. | `Regels.schrikFoto`, `Instellingen.schrikFoto` | `RegelsTest.schrikFotoVastOfWillekeurig`, `ConfigTest.heenEnTerug` |
| R1.5 | De sidebar van het doolhof: per kleur een kop en de namen eronder, in hooguit 15 regels; past het niet, dan zo weinig mogelijk namen naast elkaar. | `TeamOverzicht` | `TeamOverzichtTest` |
| R1.7 | Na de timer is regio `doolhof_gif` giftig tot iedereen een team heeft en eruit is: elke seconde Poison, om de 2 seconden een klap van 1 hart. | `Regels.DOOLHOF_GIF_ELKE`, `Regels.DOOLHOF_GIF_SCHADE` | `RegelsTest.momenten` |
| R1.8 | Na elke minuut gif is de klap twee keer zo hard: 1, 2, 4, 8, 16 hartjes, en dan niet meer. | `Regels.gifSchade`, `Regels.gifKeer` | `RegelsTest.gifVerdubbeltElkeMinuut` |
| R1.9 | Het doolhof is voorbij als iedereen een team heeft en niemand meer in regio `doolhof_gif` loopt; niemand wordt weggehaald (behalve met `/doolhof einde`). | `Doolhof.klaarVoorEinde` (fabric) | testrun |
| R1.10 | Met het gif gaat het regenen en wordt het in 30 seconden middernacht; na het doolhof in 20 seconden weer middag en droog. Altijd vooruit, nooit terug in de tijd. | `Dagtijd`, `Lucht` (fabric) | `DagtijdTest` |
| R2.1 | Blokken in het Ei: netherite 50, diamond 10, gold 5, iron 1 punt (iron niet in de chat); redstone, emerald, TNT, glowstone, slime en target geven een effect. Standaard 6, 90, 120, 2000, 10, 10, 10, 10, 10, 5. | `EiBlok`, `Instellingen` | `EiVerdelingTest.blokkenEnPunten`, `InstellingenTest.standaarden` |
| R2.2 | Strooien trekt zonder dubbele uit de deepslate-plekken; samen meer dan er plekken zijn weigert hij. | `EiVerdeling` | `EiVerdelingTest` |
| R2.3 | `/ei blokken` weigert een totaal boven de deepslate van de vastlegging. | `Instellingen.checkEiBlokken` | `InstellingenTest.eiBlokkenPassenOpDeDeepslate` |
| R2.4 | De stand van het Ei: meeste punten boven, bij gelijk wie de score het eerst had; wie 0 heeft staat er niet in. Dezelfde regel voor de kills in de FFA. | `Klassement` | `KlassementTest` |
| R2.5 | Redstone is 50/50: Haste 15 seconden voor de hakker, of 15 seconden bevriezing voor de rest. | `Regels.redstoneGok` | `RegelsTest.redstoneGokIsVijftigVijftig`, `RegelsTest.momenten` |
| R2.10 | TNT uit het Ei blaast alleen gewone deepslate weg, in een bol met straal 3. | `Regels.EI_TNT_STRAAL` | - (fabric) |
| R2.11 | Glowstone: 10 seconden Efficiency V en Haste II. | `Regels.EI_GLOWSTONE` | - (fabric) |
| R2.12 | Slime: 15 seconden Nausea voor alle anderen. | `Regels.EI_MISSELIJK` | - (fabric) |
| R2.13 | Target: iedereen op de plek van een ander, niemand houdt zijn eigen plek; alleen staat blijft staan. | `Hussel.verdeel` | `HusselTest` |
| R3.1 | Eén veld, per beurt twee spelers van elk team; per team een gelote volgorde, in beurt *b* speler *2b* en *2b + 1*. Aantal beurten = het grootste team gedeeld door twee, naar boven afgerond; iedereen speelt één keer. | `MobSchema` | `MobSchemaTest.vierPerTeamTweeBeurtenIedereenEenKeer`, `MobSchemaTest.hetVoorbeeldUitDeDocs`, `MobSchemaTest.beurtenIsHetGrootsteTeamGedeeldDoorTweeNaarBoven` |
| R3.2 | Een kleiner team heeft extra beurten; die krijgen bij de start van de beurt een willekeurige speler die nog niet af is en niet al in deze beurt staat, of blijven leeg. | `MobSchema.opstelling` | `MobSchemaTest.kleinerTeamHeeftExtraBeurten`, `MobSchemaTest.extraBeurtPaktIemandDieNogMagEnNietAlSpeelt`, `MobSchemaTest.extraBeurtZonderKandidaatBlijftLeeg` |
| R3.3 | Wie af is speelt geen beurt meer: zijn geplande plek blijft leeg. | `MobSchema.opstelling` | `MobSchemaTest.wieAfIsLaatZijnPlekLeeg` |
| R3.4 | De volgende wave 5 seconden nadat alle mobs dood zijn; na 120 seconden (of `/mobarena wave volgende`) telt een wave altijd als klaar, behalve de warden: die heeft geen limiet, alleen het commando. | `MobVerloop` | `MobVerloopTest.volgendeWaveVijfSecondenNaAllesDood`, `MobVerloopTest.naTweeMinutenAltijdKlaar`, `MobVerloopTest.forcerenMetHetCommando`, `MobVerloopTest.deWardenHeeftGeenLimiet` |
| R3.5 | De beurt is klaar na de laatste wave, of zodra iedereen in het veld af is. | `MobVerloop` | `MobVerloopTest.beurtKlaarNaDeLaatsteWave`, `MobVerloopTest.beurtKlaarAlsIedereenAfIs`, `MobVerloopTest.legVeldIsMeteenKlaar` |
| R3.6 | Na elke beurt tien seconden vieren, dan pas naar de tribune. | `Regels.BEURT_VIEREN` | `RegelsTest.momenten` |
| R3.7 | Punten per mobtype (zombie 1 ... ravager 10, warden 50, elk ander type 1), naar het team van de killer, plus een kill. | `MobPunten`, `Instellingen.mobPunten` | `MobPuntenTest.standaardTabel` |
| R3.8 | Winnaar: meeste punten, dan meeste kills, dan samen. | `MobPunten.winnaars` | `MobPuntenTest.puntenPerTypeEnTiebreakOpKills`, `MobPuntenTest.helemaalGelijkIsSamen` |
| R3.9 | De warden uit het Warden-ei: standaard 200 HP, klap 8 HP, sonic boom 5 HP (0 t/m 60 en 0 t/m 40, leven 20 t/m 1000), 50 punten. | `Instellingen.WardenWaarde`, `Instellingen.mobPunten` | `InstellingenTest.standaarden`, `InstellingenTest.grenzenVanHetDoolhof` |
| R4.1 | Het quiz-rad: 16 vakken, elk team 4 keer, buren (ook rondom) nooit gelijk. | `QuizRad.VAKKEN` | `QuizTest.hetRadHeeftElkTeamVierKeerZonderGelijkeBuren` |
| R4.2 | 64 standen, vier per vak; elk vierde plaatje heeft een vak onder het pijltje; een vakgrens bij de standen 2, 6, 10 ... | `QuizRad` | `QuizTest.standenEnVakken` |
| R4.3 | Een draai volgt het geluid `bootcamp:rad`: op snelheid, dan bij elk van de laatste 20 tikjes een vakgrens onder het pijltje, stil op 9,68 s op het midden van een willekeurig vak (elk team 25%), uitslag bij het plingeltje (tick 203): lampen aan en het vak knippert (aan 3, uit 2, aan 3, uit 2, aan), en als het geluid uit is (tick 222) het team in beeld. Nooit sneller dan 1,6 stand per tick. | `QuizDraai` | `QuizTest.draaiLandtOpEenVakmiddenElkTeamEvenVaak`, `QuizTest.draaiVolgtDeTikjesVanHetGeluid`, `QuizTest.draaiNooitTeSnelVoorHetScherm` |
| R4.4 | Goed: +1 en het team blijft aan de beurt; de reeks staat erbij vanaf twee op rij. Fout of een nieuwe draai: niemand aan de beurt, reeks nul. Zonder team aan de beurt doen goed en fout niets. | `QuizStand` | `QuizTest.goedFoutEnDeReeks` |
| R4.5 | Einde: het team met de meeste punten; bij gelijke stand kiest de commander. | `QuizStand.leiders` | `QuizTest.winnaarOfGelijkspel` |
| R4.6 | De decorlampen (alle redstone lampen in regio `quiz` behalve de banken): vier lichtbalken van 40% lopen met de klok mee rond het midden, een rondje per 16 s, en draaien mee met het rad. Is een team aan de beurt (of na de quiz de winnaar), dan branden alleen de lampen in zijn regio `quizdecor_<kleur>`. | `Lichtshow` | `LichtshowTest.hoekMetDeKlokMeeVanafHetNoorden`, `LichtshowTest.balkenSchuivenMetDeKlokMee` |
| R5.1 | Het Rad landt altijd op het doelslot (de uitverkorene); start en aantal rondes (2 of 3) zijn willekeurig. | `Rad` | `RadTest.landtAltijdOpHetDoel`, `RadTest.tweeOfDrieRondes` |
| R5.1b | Het Rad loopt van 2 ticks per stap op naar 30 en wordt nooit sneller. | `Rad.wachttijd` | `RadTest.ritmeLooptOp` |
| R5.2 | Jagers willekeurig over de startplekken, één per plek; meer jagers dan plekken: om en om. | `Regels.verdeelWillekeurig` | `RegelsTest.jagersWillekeurigEenPerPlek` |
| R5.3 | Ei-spawns om en om. | `Regels.startpunt` | `RegelsTest.startpuntenOmEnOm` |
| R5.4 | Na `/clown go` en na elke kroonwissel 10 seconden countdown. | `Regels.OPSTELLING` | `RegelsTest.momenten` |
| R5.5 | Kill de kroonhouder en je krijgt de kroon. | `Regels.kroonOpvolger` | `RegelsTest.kroonNaarDeKiller` |
| R5.6 | Zonder killer: de laatste hit; anders een willekeurige levende jager. Wie af is krijgt de kroon niet; zonder jagers geen opvolger. | `Regels.kroonOpvolger` | `RegelsTest.kroonNaarLaatsteHit`, `RegelsTest.kroonNaarWillekeurigeJager`, `RegelsTest.dodeKillerTeltNiet`, `RegelsTest.geenOpvolgerZonderJagers` |
| R5.7 | Logt de kroonhouder uit, dan dertig seconden wachten; daarna dezelfde kroonwissel. | `Regels.bijQuit`, `KROON_UITLOG_WACHT` | `RegelsTest.quitRegels` |
| R5.8 | Geen timer: de ronde is voorbij zodra er één over is. | `Regels.laatsteOver` | `RegelsTest.eindeBijEenOver` |
| R5.10 | Regeerperiodes: elke kroonwissel een nieuwe periode. | `Regeerperiodes` | `RegeerperiodesTest` |
| R5.11 | Strength: de kroonhouder II en de jagers niks, tot er nog 3 over zijn (de kroonhouder meegeteld); dan iedereen I. | `Regels.kroonSterkte`, `SLOTSTRIJD` | `RegelsTest.kroonhouderSterkerTotDeSlotstrijd` |
| R6.1 | Iedereen behalve de uitverkorene doet mee aan de FFA, ook wie in ronde 5 af was. | `Regels.ffaDeelnemers` | `RegelsTest.ffaZonderDeUitverkorene` |
| R6.5 | Bij drie over `LAATSTE DRIE`, bij twee `LAATSTE TWEE`. | `Regels.aftelTitle` | `RegelsTest.laatsteDrieEnTwee` |
| R6.6 | De finale: de winnaar van King of the Hill tegen de winnaar van de FFA; won dezelfde speler allebei, dan tegen de nummer twee van de FFA. Zonder twee namen geen finale. | `Regels.finalisten` | `RegelsTest.finaleKingOfTheHillTegenFfa` |
| R7.1 | Uitloggen: een jager in ronde 5 en een FFA-speler zijn af; wie in de mob arena aan de beurt is telt als dood; logt een finalist uit, dan pauzeert de finale tot de staff kiest (combat log of crash); in de andere rondes gebeurt er niks. | `Regels.bijQuit` | `RegelsTest.quitRegels` |
| R7.2 | Terugkomen: doolhof naar de start (met een team: verder waar je was), Ei naar je eigen startplek, mob arena de tribune, quiz je bank, ronde 5, 6 en de finale kijker (de kroonhouder binnen zijn dertig seconden blijft kroonhouder). | `Regels.bijJoin` | `RegelsTest.joinRegels` |
| R8.1 | Een kit is per slot een item in `/give`-syntax, met optioneel een aantal. Fouten noemen bestand en slot. | `KitDef` | `KitDefTest` |
| R8.2 | De bossbar heeft per ronde een vast formaat. | `BossbarTekst`, `Tijd` | `BossbarTekstTest` |
| R8.3 | De standaardbestanden in de jar (`fabric.mod.json`, kits, `waves.json`, `doolhof_loot.json`) zijn geldig; de waves volgen de tabel uit docs/02. | | `ResourcesTest` |
| R8.4 | Een groot plaatje in een font is twee rijen tegels: na elke tegel een spatie van -1, na de bovenste rij een spatie terug; het quiz-rad is per stand 2 x 2 tegels. | `FontTegels`, `QuizRad.glyph` | `FontTegelsTest`, `QuizTest.standenEnVakken`, en in het fabric-project `PackFontTest` |
