# Draaiboek

Tijdschema, rollen en checklists voor de avond zelf. Tijden zijn een voorbeeld met start om 20:00.

## Tijdschema

| Tijd | Wat | Wie |
|---|---|---|
| 19:15 | Staff online. Wereldbackup maken. `/bc reset` draaien. Poort, teleports en `/bc schrik` op een testaccount even testen. | Admins |
| 19:40 | Whitelist open. Spelers spawnen in het basiskamp en krijgen het resource pack. Voice-test: iedereen zegt wat, loopt weg en komt terug. | Iedereen |
| 19:55 | Host legt de regels uit, zonder Clown vs All te verklappen. | Host |
| 20:00 | **Intro op stream.** Iedereen naar de startruimte van het doolhof, countdown. | Host, Admin 1 |
| 20:02 | **Ronde 1: De Doolhof** (10 min, uitgang open na 4 min, teamkeuze bij de uitgang) | |
| 20:13 | V2 aan de bosrand: teams in beeld, praatje. | Host |
| 20:15 | **Ronde 2: Het Ei** (10 min) | |
| 20:26 | Winnaar van het Ei. Naar V3 bij de mob arena. | Host |
| 20:28 | **Pauze** (5 min). | Host |
| 20:33 | **Ronde 3: De Mob Arena**: loting, halve finale 1, halve finale 2, finale (± 25 min) | |
| 20:58 | Winnend team. Naar de quiz. | Host |
| 21:00 | **Ronde 4: De Quiz** (± 15 min, host bepaalt) | Host |
| 21:15 | Winnend team. Naar de Arena. Host legt Clown vs All uit. "Het lot beslist wie de kroon krijgt." Het Rad landt op Clown. | Host, Admin 1 |
| 21:18 | **Ronde 5: Clown vs All** (30 sec voorsprong, geen timer, reken op 10 tot 20 min) | |
| 21:35 | Winnaar. Iedereen behalve Clown de vloer op. | Admin 1 |
| 21:37 | **Ronde 6: De FFA** (max 10 min) | |
| 21:48 | **Kroning** in het midden van de Arena, tribunes vol. | Host |
| 22:00 | Einde stream. | |

Totaal ruim 1,5 uur speeltijd, plan 2 uur met buffer. De mob arena en Clown vs All hebben geen
harde timer; die kunnen uitlopen. Loopt het echt uit, dan kan de ref een wave forceren
(`/bc wave volgende`) of de border laten krimpen (`/bc krimp`).

## Rollen

| Rol | Aantal | Wat |
|---|---|---|
| **Host / caster** | 1 | Praat op de stream, legt regels uit, kondigt rondes aan. **Leest de quizvragen voor** en houdt de quizstand bij; staat daarvoor in-game bij het podium. Speelt Het Rad recht: "iedereen kan de kroon krijgen". |
| **Admin 1: commander** | 1 | Start elke ronde op het sein van Pudding, draait de quiz-randomizer (`/bc quiz draai`) op het teken van de host, en het Rad. |
| **Admin 2: ref** | 1 | Kijkt naar problemen: stuck spelers, disconnects, bugs met de kroon, een team dat scheef zit. Heeft het randgevallen-lijstje uit [03-kroon-regels.md](03-kroon-regels.md) bij de hand. |
| **Camera** | 0 tot 2 | Kijker-accounts voor het hoofdbeeld: boven het doolhof, boven de mob-arenatribune, voor het podium, boven de Arena. |
| **Bouwers** | 2 tot 4 | Vooraf. Zie de bouwlijst hieronder. |

Alleen de admins weten dat het Rad rigged is. Eén persoon kan host en ref combineren als je krap
zit, maar de commander moet alleen commander zijn.

## Bouwlijst (vooraf)

1. De wereld kiezen of genereren: ± 500 x 500 met heuvels, bos en water. Bepaal waar elke zone
   komt (plattegrond in [01-map-en-flow.md](01-map-en-flow.md)) (1 avond).
2. Basiskamp en de verzamelpunten (uurtje).
3. Het doolhof: startruimte in het midden, 4 gangen, 1 echte uitgang met een poort, 3
   nep-uitgangen, lege kisten, schrikplekken, het teamkeuzevak achter de uitgang (1 tot 2
   avonden).
4. Ei-bos: Groot Ei met de puntenblokken, nep-eitjes, beacon eronder (1 tot 2 avonden).
5. De mob arena: twee identieke arena's met elk 4 spawnpunten, een tribune ertussen, een kooi
   naast elk veld (1 tot 2 avonden).
6. Het quizpodium met 4 gekleurde vakken en lampen (uurtje).
7. De Arena: colosseum met vloer, verhoogd midden, tribunes, en de 20 pilaren van De Kring met
   koppen en lichtblokken (2 tot 3 avonden).
8. Het resource pack: foto van Clown en het lachje aanleveren, pack bouwen en online zetten
   (uurtje, zie [04-technische-schets.md](04-technische-schets.md)).
9. De mod ombouwen naar dit rondeplan ([08-taakplan.md](08-taakplan.md)), dan met de wand en
   `/bc point` alle regio's en punten zetten (uurtje).
10. Volledige testrun met 4 tot 8 testers, zodat er minstens twee teams zijn (1 avond).

## Checklist: eerste test van de mod

Met een tweede account op de testserver (Java 25, niet op peaceful, `spawn-protection=0`). Voor
de eerste test mogen alle punten dicht bij elkaar op een vlak stuk staan. Wat er per onderdeel
te controleren valt staat in [mod/BOUWLOG.md](../mod/BOUWLOG.md).

- [ ] Jar in `mods/`, samen met Fabric API en de voice-mod. In de console `Pudding Bootcamp
      geladen` en dat de standaardbestanden in `config/bootcamp/` zijn geschreven.
- [ ] Resource pack wordt aangeboden bij het joinen. `/bc schrik <naam>`: foto vult het scherm,
      lachje klinkt.
- [ ] `/bc wand`, een regio opslaan, `/bc region show`, `/bc point set`, `/bc point tp`,
      `/bc status`, `/bc kit basis`.
- [ ] `/bc start 1`: kisten gevuld, nep-uitgang zet je terug met knal en grapje, schrikplek werkt
      één keer, poort open na 4 minuten (`/bc timer 365` om te versnellen), teammenu bij de
      uitgang, vol team is grijs.
- [ ] `/bc start 2`: punten per block, redstone (beide uitkomsten, een paar keer breken),
      emerald geeft de ander een jumpscare, winnaar bij de timer.
- [ ] `/bc start 3` met twee accounts in twee teams: loting, waves tegelijk, 10 seconden na
      beide klaar de volgende, ga dood (kooi), einde wedstrijd, spullen terug.
- [ ] `/bc start 4`: iedereen in zijn vak, `/bc quiz draai` een paar keer, `/bc quiz punt`,
      `/bc quiz winnaar`.
- [ ] `/bc uitverkoren <naam>`, `/bc slot` voor beide accounts, `/bc rad`: ronde 5 begint.
      Jager raakt jager: geen schade. Kill de kroonhouder: kroon over, reset. Laatste over wint.
- [ ] `/bc start 6`, FFA, kroning.
- [ ] `/bc reset`: alles terug, ook de teams.
- [ ] Alles wat niet klopt in één bericht terug, met de console-regels erbij.

## Checklist: testrun

Minstens één keer de hele avond met 4 tot 8 testers. Let vooral op:

- [ ] Geen errors in de console van Fabric, de voice-mod of de bootcamp-mod.
- [ ] Alle regio's en punten staan erin (`/bc region list`, `/bc point list`).
- [ ] Doolhof: niemand kan over de haag. Nep-uitgangen triggeren op de goede plek. Poort gaat
      open op 4 minuten. Teamkeuze werkt, en na de timer krijgt iedereen zonder team er een.
- [ ] Het Ei: netherite is te minen met de diamond pickaxe. Punten kloppen. Bevriezing voelt
      niet oneerlijk lang. Er zijn genoeg blokken voor 10 minuten.
- [ ] Mob arena: beide arena's krijgen dezelfde wave op hetzelfde moment. Een wedstrijd duurt
      niet langer dan 10 minuten; anders de waves zwaarder maken in `waves.json`.
- [ ] Quiz: de host is overal op het podium te horen.
- [ ] Clown vs All: jagers kunnen elkaar niet raken, ook niet met pijlen. Kroonwissel zet
      iedereen goed terug. Rad landt op de uitverkorene; draai hem vijf keer.
- [ ] FFA: iedereen behalve Clown staat op de vloer.
- [ ] Kijkers: tribune en kooi, geen schade, niet het veld op.
- [ ] Voice: proximity werkt, de tribune is hoorbaar.
- [ ] Serverperformance met twee arena's vol mobs.
- [ ] `/bc reset` brengt alles terug naar de basiskamp-staat.

## Checklist: dag zelf

- [ ] Wereldbackup gemaakt.
- [ ] `/bc reset` gedraaid, iedereen start schoon en zonder team.
- [ ] Whitelist compleet, staff heeft op.
- [ ] Resource pack online, URL en SHA-1 kloppen in `server.properties`.
- [ ] `/bc status` laat zien dat Clown uitverkoren is en niemand anders.
- [ ] Host heeft de quizvragen klaar.
- [ ] Voice: `force_voice_chat=true`, UDP-poort open.
- [ ] Host en camera-accounts staan in spectator of creative.
- [ ] De jar van de vorige werkende versie staat klaar.
- [ ] Alle kits en `doolhof_loot.json` en `waves.json` staan in `config/bootcamp/`.
- [ ] `spawn-protection=0` en de difficulty niet op peaceful.

## Spelregels voor de streamers

Kort en op de borden in het basiskamp:

1. Geen x-ray, geen cheats, geen mods die voordeel geven. Sodium en dat soort dingen mag.
   Simple Voice Chat en het resource pack zijn verplicht.
2. Niet streamsnipen: niet op andermans stream kijken waar de uitgang of het Ei is.
3. Teams: maximaal 5. Wie het eerst uit het doolhof is, kiest het eerst.
4. PvP staat uit tot het eind. Wat PvP wel mag, hoor je als het zover is.
5. Bug of stuck? Roep de ref, niet de chat.
6. Als de admin zegt stop, dan stop.
7. Geen Discord-call tijdens het event, alleen de voice-mod.

## Als het misgaat

| Probleem | Oplossing |
|---|---|
| Server crasht | Backup terugzetten, ronde opnieuw starten. De teams staan in `bootcamp.json` en overleven een herstart; klopt er iets niet, dan `/bc team`. |
| Timer loopt niet | `/bc stop`, dan `/bc start <ronde>` en `/bc timer <seconden>`. |
| Iemand heeft geen team of het verkeerde | `/bc team <speler> <kleur>`. |
| Iemand ziet de jumpscare als leeg vierkantje | Pack niet geladen. Opnieuw joinen, of accepteren in het menu. |
| Mob arena: een wave komt niet af | `/bc wave volgende`. |
| Clown vs All valt stil | `/bc krimp <grootte>`. |
| Rad stopt op de verkeerde kop | Slot van die speler klopt niet. Host: "technische storing", ref fixt `/bc slot`, rad nog een keer. |
| Kroon zit bij niemand of bij twee | `/bc kroon <speler>`. |
| Een ronde breekt zichzelf af met een rode melding | De mod ving een fout af. De fout staat in de console. `/bc start <ronde>` opnieuw. |
| Speler zit vast in een blok | `tp` door de ref. |
| Ei niet gevonden en de hint werkt niet | Ref zet een vuurpijl of zegt de richting in de chat. |
| Iemand hoort niks in voice | Kruis door het voice-icoontje: UDP-poort dicht of verkeerde versie. Geen kruis maar stil: `/bc kijker <naam> uit`. |
| De mod gooit errors | `/bc stop`, `/bc reset`, ronde opnieuw. Blijft het misgaan: herstarten met de vorige jar. |
