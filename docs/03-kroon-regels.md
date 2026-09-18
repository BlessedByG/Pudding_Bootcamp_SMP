# De kroon: regels van ronde 4 tot en met de finale

De kroon is een gouden helm met Curse of Binding (kan niet af) plus het Glowing-effect. Wie de
kroon heeft, is de koning en is voor iedereen zichtbaar door muren heen.

## Start van ronde 4: Het Rad

1. Iedereen staat in wachtkamer 4 voor Het Rad: een cirkel van 20 spelerskoppen met één lampje.
   De host legt de kroonregels uit en sluit af met: "Wie de koning wordt? Iedereen kan het zijn.
   Het lot beslist."
2. De commander start het rad. Het lampje gaat twee of drie rondes rond, wordt langzamer, en
   stopt op ClownPierce. Geluid, particles, `title` voor iedereen: **DE KONING: CLOWNPIERCE**.
3. Het rad is rigged. Clown heeft vooraf de verborgen rol `uitverkoren` gekregen (een tag in de
   datapack) en het rad landt altijd op de speler met die rol. De startpositie en het aantal
   rondes zijn wel echt willekeurig, zodat het er elke keer anders uitziet. Alleen de staff en
   Clown hoeven dit te weten. Valt Clown uit, dan verhuis je de rol en landt het rad op iemand
   anders.
4. Drie seconden later wordt Clown naar de burcht in het midden van de King zone geteleporteerd.
   Hij krijgt de kroon, de bosskit en Glowing. Bossbar: `Koning: ClownPierce · 15:00`.
5. De 19 hunters worden verdeeld over 4 spawnpunten aan de rand, in team `hunters` (friendly fire
   uit). Ze hebben hun loot uit ronde 3, of de basiskit.
6. 30 seconden voorsprong voor de koning: hunters staan achter een hekje. Daarna gaat het hek open
   en start de timer van 15 minuten.

## De kroon wisselt

**Wie de koning killt, krijgt de kroon.** Direct, zonder tussenstap:

- De ex-koning verliest de kroon en gaat in spectator. Hij ligt eruit (zie
  [06-open-keuzes.md](06-open-keuzes.md) voor de variant waarin ex-koningen wel meedoen aan de FFA).
  In voice hoort hij vanaf nu alles, maar niemand hoort hem; hij krijgt de knop voor de groep
  Doden (zie [07-voice.md](07-voice.md)).
- De nieuwe koning:
  - wordt full hp geheald,
  - krijgt 15 seconden Resistance II (zodat je niet meteen doodgaat als je op één hartje de kill
    maakt),
  - krijgt het kroonpakketje: 2 gapples + 2 ender pearls,
  - gaat van team `hunters` naar team `king`, krijgt de helm en Glowing.
- Iedereen krijgt een `title`: **NIEUWE KONING: <naam>**. Bossbar update.
- Van 19v1 naar 18v1. Elke kroonwissel haalt één speler uit het spel.

**De koning gaat dood zonder killer** (val, lava, mob, disconnect):

- De kroon gaat naar de hunter die de koning als laatste heeft geraakt.
- Is die er niet, dan naar een willekeurige hunter. De ronde mag nooit zonder koning zitten.
- Bij een disconnect wacht de admin 30 seconden. Komt de speler niet terug, dan gaat de kroon
  door op dezelfde manier.

## Hunters

- Hunters kunnen elkaar niet raken (zelfde team). Het is 19 tegen 1, niet 19 tegen elkaar.
- Doodgaan als hunter = 20 seconden spectator, dan respawn op een van de 4 randpunten met al je
  spullen (keepInventory staat aan). 5 seconden Resistance na respawn tegen spawncampen. In die
  20 seconden hoor je alles maar kun je niet praten; dus geen "hij zit achter de toren" naar je
  team.
- Bouwen mag. Pillaren, inbouwen, een trap zetten: allemaal SMP-gedrag.

## Sudden death: de laatste 3 minuten

- `title` voor iedereen: **SUDDEN DEATH**.
- Geen respawns meer. Een hunter die nu doodgaat is uitgeschakeld.
- De worldborder krimpt in 3 minuten van 200 x 200 naar 60 x 60 rond de burcht. Verstoppen kan
  niet meer, de koning moet vechten.

## Einde van de timer

- Wie op dat moment de kroon heeft, is **finalist 1**. De timer kan aflopen midden in een
  gevecht; dat is prima, dat is spanning.
- Alle hunters die nog leven gaan door naar de FFA.
- Alle spelers die dood zijn (ex-koningen en hunters gestorven in sudden death) zijn uitgeschakeld
  en kijken de rest in spectator.

## Randgevallen

| Situatie | Wat gebeurt er |
|---|---|
| Clown is er niet of valt uit vóór ronde 4 | Geef de rol `uitverkoren` aan iemand anders. Het rad landt dan op die speler. |
| Het rad stopt op de verkeerde kop | De slot-score van die speler klopt niet met de plek van zijn kop. Host: "technische storing", ref fixt de score, rad nog een keer. In het uiterste geval de kroon handmatig geven. |
| Alle hunters op één na zijn koning geweest en dood | Ronde 4 stopt meteen. De laatste hunter is automatisch finalist 2, de FFA vervalt. |
| De koning wordt gekilld op de laatste seconde | De killer is de nieuwe koning en dus finalist 1. Kill telt zolang de timer nog loopt. |
| Twee hunters raken de koning tegelijk | De speler die de laatste klap geeft krijgt de kroon. Het spel bepaalt dat, niet de admin. |
| De koning logt uit | Zie hierboven: 30 seconden wachten, dan gaat de kroon door. |
| De koning bouwt zich in | Mag. Sudden death en de border lossen het op. |
| Iemand met de kroon gaat in spectator door een bug | Admin geeft de kroon handmatig met de `crown/give`-functie (zie technische schets). |
| Er is maar één hunter over aan het eind van de timer | Die is finalist 2, de FFA vervalt. |

## De tweede kroon

De FFA-winnaar krijgt de tweede kroon. Zelfde helm, andere naam. Vanaf dat moment zijn er twee
koningen en die vechten de finale. Alle randgevallen van de FFA (timer, border, kills als
tiebreak) staan in [02-rondes.md](02-rondes.md).

## Display voor de stream

- **Bossbar:** timer + naam van de koning. Wordt goud tijdens sudden death.
- **Sidebar:** lijstje van de regeerperiodes: `Clown 4:12 · Speler X 0:38 · Speler Y 2:05`. Dit
  is leuk voor de stream en handig als eretitel achteraf ("langste regeerperiode").
- **Tab-list:** koning in goud, hunters in blauw, uitgeschakelde spelers in grijs (teamkleuren).
