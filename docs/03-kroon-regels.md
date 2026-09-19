# De kroon: regels van ronde 4 tot en met de finale

De kroon is een gouden helm met Curse of Binding (kan niet af) plus het Glowing-effect. Wie de
kroon heeft, is de koning en is voor iedereen zichtbaar door alles heen. Ronde 4, 5 en 6 spelen in
de Arena: een colosseum met tribunes waar de doden op komen.

## Start van ronde 4: Het Rad

1. Iedereen staat op de vloer van de Arena. De host legt de kroonregels uit en sluit af met:
   "Wie de koning wordt? Iedereen kan het zijn. Het lot beslist."
2. Op het sein van Pudding start de commander het rad. Het lampje loopt langs de 20 pilaren rond
   de vloer, twee of drie rondes, wordt langzamer, en stopt op ClownPierce. Geluid, particles,
   `title` voor iedereen: **DE KONING: CLOWNPIERCE**.
3. Het rad is rigged. Clown heeft vooraf de verborgen rol `uitverkoren` gekregen (een tag die de
   admin zet) en het rad landt altijd op de speler met die rol. De startpositie en het aantal
   rondes zijn wel echt willekeurig, zodat het er elke keer anders uitziet. Alleen de staff en
   Clown weten dit, en dat blijft zo: dit wordt nooit verteld. Valt Clown uit, dan verhuis je de
   rol en landt het rad op iemand anders.
4. Drie seconden later wordt Clown naar het midden van de Arena geteleporteerd, het verhoogde
   plateau. Hij krijgt de kroon, de bosskit en Glowing. Bossbar: `Koning: ClownPierce · 15:00`.
5. De 19 hunters worden verdeeld over 4 startpunten aan de rand van de vloer, in team `hunters`.
   Friendly fire staat **aan**: hunters kunnen elkaar raken. Ze hebben hun loot uit ronde 3, of
   de basiskit.
6. 30 seconden voorsprong voor de koning: de hunters staan bevroren op hun startpunt, met een
   countdown in beeld. Ze kunnen rondkijken en hun inventory sorteren, maar niet lopen, springen
   of pearlen. Bij nul zijn ze los en start de timer van 15 minuten.

## Eén leven

- Iedereen heeft in ronde 4 **één leven**. Wie doodgaat, hunter of koning, door wie dan ook, is
  uit de ronde en gaat als kijker de tribune op. Geen respawns.
- Wie doodgaat ziet groot in beeld een van de doodteksten: **Grote L gepakt!**, **Had je nou maar
  beter je best gedaan** of **Gelukkig is dit niet de CSMP**. Willekeurig, alleen voor de dode
  zelf. Verder niks, geen chatregel.
- De ronde eindigt als de timer afloopt, of eerder zodra er geen levende hunter meer is.
- 19v1 wordt zo aan beide kanten kleiner: elke kroonwissel haalt een ex-koning weg, elke gevallen
  hunter een jager.

## De kroon wisselt: elke wissel is een reset

**Wie de koning killt, krijgt de kroon.** En op dat moment begint de jacht opnieuw:

- De ex-koning verliest de kroon en gaat als kijker de tribune op. Voor ronde 4 ligt hij eruit;
  in de FFA doet hij weer mee. In voice is hij nu publiek: hij hoort de vloer en de vloer hoort
  hem (zie [07-voice.md](07-voice.md)).
- **Alle levende hunters** worden full hp geheald en teruggeteleporteerd naar hun startpunt aan
  de rand, waar ze bevroren staan. Doden blijven dood: een reset geeft geen levens terug.
- **De nieuwe koning** wordt naar het midden geteleporteerd en:
  - wordt full hp geheald, honger vol,
  - krijgt al zijn armor en wapens gerepareerd (volle durability),
  - krijgt het kroonpakketje: 2 gapples + 2 ender pearls,
  - krijgt 15 seconden Resistance II en Glowing,
  - gaat van team `hunters` naar team `king` en krijgt de helm.
- Iedereen krijgt een `title`: **NIEUWE KONING: <naam>**. Bossbar update.
- **10 seconden** countdown, dan zijn de hunters los en gaat de jacht verder. De timer loopt
  gewoon door.

Alleen de koning krijgt zijn spullen gerepareerd; hunters slijten door de ronde heen. Wil je dat
iedereen bij een reset gerepareerd wordt, dan is dat één regel extra in de mod.

**De koning gaat dood zonder killer** (val, mob, disconnect):

- Precies dezelfde reset. De kroon gaat naar de hunter die de koning als laatste heeft geraakt.
- Is die er niet, dan naar een willekeurige levende hunter. De ronde mag nooit zonder koning
  zitten.
- Bij een disconnect wacht de admin 30 seconden. Komt de speler niet terug, dan gaat de kroon
  door op dezelfde manier en is de speler uit de ronde.

## Hunters

- Hunters kunnen elkaar raken. Samenwerken mag, verraden ook. De kroon gaat naar wie de laatste
  klap op de koning geeft, dus een hunter die zijn maat de kill wil afpakken kan dat proberen.
- Eén leven, geen respawn. Sterf je aan een hunter, dan ben je net zo dood als aan de koning.
- **Bouwen is verboden.** Adventure mode zorgt dat het ook niet kan. Pearls, gapples, boog en
  schild wel.

## Einde van ronde 4

- Timer op nul, of geen levende hunter meer: wie op dat moment de kroon heeft is **finalist 1**.
  De timer kan aflopen midden in een gevecht; dat is prima, dat is spanning.
- **Iedereen behalve Clown en finalist 1 gaat door naar de FFA**, ook wie in ronde 4 doodging,
  hunter of ex-koning. Ronde 4 is een eigen wedstrijd; de FFA begint schoon, met full hp en een
  standaardkit.
- Clown is de eindbaas. Is hij aan het eind nog koning, dan is hij finalist 1 en speelt hij de
  finale. Is hij de kroon kwijt, dan is de eindbaas klaar en kijkt hij vanaf de tribune. Hij doet
  nooit mee aan de FFA.

## Randgevallen

| Situatie | Wat gebeurt er |
|---|---|
| Alle hunters dood voordat de timer afloopt | Ronde stopt meteen, de koning is finalist 1. |
| De koning wordt gekilld op de laatste seconde | De killer is de nieuwe koning en dus finalist 1. Kill telt zolang de timer nog loopt. |
| Twee hunters raken de koning tegelijk | De speler die de laatste klap geeft krijgt de kroon. Het spel bepaalt dat, niet de admin. |
| Hunter killt hunter | Gewoon dood, tribune op. Mag. |
| Er is nog één hunter over tegen de koning | Gewoon doorspelen tot de timer of een dood. |
| De koning logt uit | 30 seconden wachten, dan gaat de kroon door (laatste hit, anders willekeurig) en is hij uit de ronde. |
| Een hunter logt uit | Uit de ronde. Komt hij terug, dan als kijker op de tribune. |
| Clown is er niet of valt uit vóór ronde 4 | Geef de rol `uitverkoren` aan iemand anders. Het rad landt dan op die speler. |
| Het rad stopt op de verkeerde kop | De slot-score van die speler klopt niet met de plek van zijn kop. Host: "technische storing", ref fixt de score, rad nog een keer. In het uiterste geval de kroon handmatig geven. |
| Iemand met de kroon wordt kijker door een bug | Admin geeft de kroon handmatig met `/bc kroon <speler>` (zie technische schets). |
| De nieuwe koning stond midden in een gevecht op één hartje | Maakt niet uit: hij staat geheald in het midden, de rest aan de rand. |

## De tweede kroon

De FFA-winnaar krijgt de tweede kroon. Daarna twee minuten rust: finalist 2 komt net uit een
gevecht, finalist 1 is uitgerust. De host bouwt het moment op. Dan de finale, best of 3, in het
midden van de Arena met een kleine border. Winnaar is King of the SMP. Geen prijs, alleen de eer
en de kroning met de tribunes vol.

## Display voor de stream

- **Bossbar:** timer + naam van de koning. In de FFA het aantal spelers dat nog staat.
- **Sidebar:** lijstje van de regeerperiodes: `Clown 4:12 · Speler X 0:38 · Speler Y 2:05`. Dit
  is leuk voor de stream en handig als eretitel achteraf ("langste regeerperiode").
- **Tab-list:** koning in goud, hunters in blauw, kijkers in grijs (teamkleuren).
