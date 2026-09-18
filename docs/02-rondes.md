# De rondes

Alle rondes uitgewerkt. Per ronde: doel, setup, regels, hoe het eindigt, wat je eraan overhoudt
(bonus) en wat er mis kan gaan.

Getallen zijn uitgangspunt voor 20 spelers. Schaal ze mee met het aantal spelers.

---

## Ronde 0: Lobby

Iedereen joint, komt in de lobby, leest de regels op de borden. Host legt kort uit wat er komen
gaat (niet alles verklappen: de kroonregels pas uitleggen bij ronde 4). Countdown, poort 1 open.

Iedereen zit in één team (`spelers`) met friendly fire uit, dus je kunt elkaar niet raken tot
ronde 4.

---

## Ronde 1: De Doolhof

**Doel:** vind de uitgang van het doolhof.

**Setup**
- 64 x 64 doolhof, muren 4 hoog, dicht plafond. Ingang aan één kant, uitgang aan de overkant.
- Adventure mode: niet breken, niet bouwen.
- 10 tot 15 kisten in doodlopende gangen met kleine loot die je later in de avond kunt gebruiken:
  een gapple, wat pijlen, in 3 van de kisten een ender pearl.
- Optioneel: 2 of 3 "gevaarlijke" gangen met een zombie-spawner of een valkuil. Niet dodelijk,
  wel vervelend.
- Iedereen start tegelijk bij dezelfde ingang. Wil je het wat spreiden, maak dan 4 ingangen in de
  hoeken en één uitgang in het midden.

**Regels**
- Geen pearls gooien in het doolhof (dicht plafond, dus het kan toch niet over de muur, maar het
  kan wel door een gang heen als je slim bent). Pearls die je vindt zijn voor later.
- Alles wat je vindt mag je houden.

**Einde**
- Timer 10 minuten. Wie de uitgang vindt, loopt de wachtkamer in.
- Na de timer wordt iedereen die nog in het doolhof zit naar de wachtkamer geteleporteerd.
  Niemand ligt eruit.

**Bonus:** de eerste 5 spelers die eruit zijn krijgen een voorsprongkistje in de wachtkamer:
1 gapple + 1 ender pearl.

**Wat train je:** oriëntatie, rustig blijven, dead ends herkennen.

**Wat kan misgaan**
- Niemand vindt de uitgang: na 7 minuten een `title` met een hint ("de uitgang ligt aan de
  noordkant"). Uiteindelijk lost de timer het op.
- Spelers die stuck staan: admin in spectator kan ze met `tp` een gang verder zetten.

---

## Ronde 2: De Horde

**Doel:** overleef als groep 5 waves mobs.

**Setup**
- Ronde arena Ø 50 met wat dekking. 4 spawnpunten voor mobs aan de rand.
- Iedereen krijgt bij binnenkomst de **hordekit**: volledig iron armor, iron sword, boog, 32
  pijlen, schild, 16 steak.
- Waves (voor 20 spelers; grofweg 1 mob per speler per wave, boss wave vast):

| Wave | Wat | Aantal |
|---|---|---|
| 1 | Zombies | 20 |
| 2 | Skeletons + spiders | 15 + 10 |
| 3 | Zombies met iron gear + creepers | 15 + 8 |
| 4 | Zombies met iron gear + witches + cave spiders | 15 + 6 + 8 |
| 5 (boss) | Ravagers + evokers + vindicators | 2 + 4 + 10 |

- Een wave start als de vorige dood is, of na 2 minuten, wat het eerst komt. Zo blijft het tempo
  erin.
- Bossbar laat zien: "Wave 3 · 12 mobs over".

**Regels**
- Doodgaan = 15 seconden spectator, daarna respawn aan de rand met een verse hordekit. Niemand
  ligt eruit.
- Deaths worden geteld op een scoreboard in de sidebar. Puur voor de eer en de bonus.
- Optionele twist voor de laatste minuut: na wave 5 gaat PvP 60 seconden aan ("vrij vuur").
  Wie dan overblijft, of de meeste kills heeft, krijgt een extra bonus. Alleen doen als je de
  groep kent; het kan de sfeer ook kapotmaken.

**Einde**
- Wave 5 dood of timer 10 minuten. Poort 3 open.
- Loot die mobs droppen mag je houden (pijlen, wat rommel).

**Bonus:** iedereen die de hele ronde 0 deaths heeft, krijgt 1 ender pearl in de wachtkamer.

**Wat train je:** mobs, boog en schild, samen vechten, niet in de creeper rennen.

**Wat kan misgaan**
- Server lag door 60 mobs + 20 spelers: houd view distance op 8 en spawn mobs verspreid over de
  4 punten in plaats van alles op één plek.
- Mobs blijven ergens hangen (achter dekking, in een gat): wave-timer van 2 minuten vangt dit op.
  Admin kan met `kill @e[tag=horde]` een wave forceren.

---

## Ronde 3: Het Ei

**Doel:** vind het Grote Ei, hak je naar binnen, pak zoveel loot als je kunt dragen en neem
minstens één diamond block mee. Dat block is je ticket naar ronde 4.

**Setup**
- 150 x 150 zoekgebied, natuurlijk terrein (bos, heuvels, grotten, water). Muur of border eromheen.
- Iedereen krijgt bij binnenkomst een iron pickaxe (Efficiency II) en houdt wat ze al hadden.
- **Het Grote Ei:** ±15 hoog, ±11 breed, half verstopt (in een heuvel, in een grot, in het meer).
  Van buiten naar binnen:
  - **Schil:** mix van stone, deepslate, andesite, tuff, mossy cobble. Ziet eruit als een rots-ei.
    Een paar vlekken obsidian, zodat je moet zoeken naar een zachte plek.
  - **Eiwit:** een rommelige laag van van alles (blokken, iron blocks, gold blocks) met daartussen
    25 tot 30 kisten. Elke kist is een "setje", zodat de eerste niet alles kan meenemen.
  - **Dooier:** in het midden 30 diamond blocks (1,5x het aantal spelers).
- **Loot in de kisten** (dit is de PvP-gear voor ronde 4): losse diamond armor pieces (deels met
  Protection), diamond swords (deels Sharpness), bogen (deels Power), gapples, ender pearls,
  potions (speed, fire resistance, healing), 32 cobblestone per kist. Geen Strength, geen lava.
- **Nep-eitjes:** 3 tot 5 kleine eitjes (3 hoog) door het gebied, met één kistje met wat kleins en
  soms een bordje met een hint over de richting van het Grote Ei.

**Regels**
- Geen PvP (iedereen zit nog in hetzelfde team).
- Pak wat je pakken kunt, maar je hebt maar één inventory.
- Je komt alleen door poort 4 met een diamond block op zak. De poort neemt het block in.

**Einde**
- Timer 10 minuten. Wie een block heeft, kan zelf door poort 4 (druk op de plaat, block wordt
  ingenomen, je staat in wachtkamer 4).
- Na de timer wordt iedereen zonder ticket alsnog doorgelaten, maar met een **lege inventory +
  basiskit**. Geen ticket = geen loot. Dat is de straf.

**Bonus:** je loot is je bonus. Wie snel is heeft de beste spullen voor ronde 4.

**Wat train je:** exploren, snel minen, kiezen wat je meeneemt, onder tijdsdruk werken.

**Wat kan misgaan**
- Niemand vindt het Ei: op 5 minuten gaat een beacon onder het Ei aan (lichtstraal), op 3 minuten
  gaat er een vuurpijl af. Uiteindelijk vindt iedereen het.
- De dooier is leeg: 1,5x het aantal spelers is ruim, maar zet 5 extra diamond blocks in een
  admin-kist als reserve.
- Iemand zit in een grot vast: admin `tp`.

---

## Ronde 4: King of the SMP

Dit is de hoofdronde. De volledige regels staan in [03-kroon-regels.md](03-kroon-regels.md).
Hier het overzicht.

**Doel:** heb de kroon als de timer afloopt.

**Setup**
- 200 x 200 open map met een burcht in het midden, een dorpje, bos, water, een toren. Survival:
  bouwen mag.
- Het begint in wachtkamer 4 met **Het Rad**: een cirkel van alle 20 spelerskoppen met een lampje
  dat rondgaat, steeds langzamer, en stopt op ClownPierce. Het ziet eruit als toeval, het is
  rigged: Clown heeft vooraf de verborgen rol `uitverkoren` en het rad landt altijd op die speler
  (zie [03-kroon-regels.md](03-kroon-regels.md)).
- Clown wordt naar de burcht geteleporteerd en krijgt de kroon en de **bosskit** (volledig diamond
  Protection II, diamond sword Sharpness II, boog Power II, 32 pijlen, 4 gapples, 8 pearls, schild,
  64 cobble).
- De 19 hunters starten op 4 punten aan de rand met de spullen uit ronde 3 (of de basiskit als ze
  niks hebben). Team `hunters`, friendly fire uit: hunters kunnen elkaar niet raken.
- De koning heeft Glowing: je ziet hem door alles heen. Bossbar: timer + naam van de koning.
- De koning krijgt 30 seconden voorsprong voordat de hunters los mogen.

**Regels in het kort**
- Kill de koning en je krijgt de kroon: full heal, 15 seconden Resistance, een kroonpakketje
  (2 gapples, 2 pearls), Glowing.
- De ex-koning ligt eruit. Van 19v1 naar 18v1 naar 17v1, enzovoort.
- Hunters die doodgaan respawnen na 20 seconden aan de rand (keepInventory aan).
- **Sudden death** in de laatste 3 minuten: geen respawns meer, de worldborder krimpt naar 60 x 60
  rond de burcht.
- Timer 15 minuten. Wie de kroon heeft als de timer afloopt is **finalist 1**.

**Einde**
- Finalist 1 gaat kijken. Alle hunters die nog leven gaan naar ronde 5. Iedereen die dood is
  (ex-koningen, hunters gestorven in sudden death) is uitgeschakeld.

**Wat train je:** PvP tegen overmacht, target focus, wanneer je wel en niet moet gaan.

---

## Ronde 5: Arena FFA

**Doel:** laatste die overblijft.

**Setup**
- Iedereen zonder kroon die nog leeft wordt full hp naar de FFA-arena geteleporteerd, verspreid
  over de rand.
- Iedereen krijgt dezelfde **arenakit**: volledig diamond Protection I, diamond sword Sharpness I,
  boog Power I, 16 pijlen, 2 gapples, 8 steak, schild. Eigen spullen worden weggehaald
  (zie [06-open-keuzes.md](06-open-keuzes.md) als je liever met eigen gear vecht).
- Teams weg: iedereen kan iedereen raken.
- 10 seconden countdown, dan los.

**Regels**
- Dood = uit (spectator).
- Teamen mag, maar er wint er maar één. Op eigen risico.
- Na 5 minuten krimpt de worldborder in 2 minuten naar 10 x 10, zodat het niet blijft hangen.

**Einde**
- Laatste levende speler krijgt de tweede kroon: **finalist 2**.
- Hard maximum 10 minuten; staan er dan nog meerdere, dan beslist het aantal kills.

---

## Ronde 6: De Finale

**Doel:** 1v1 tussen de twee kroondragers, best of 3.

**Setup**
- Kleine arena 20 x 20, twee startpunten tegenover elkaar.
- Beiden krijgen de **finalekit**: volledig diamond Protection II, diamond sword Sharpness II,
  boog Power I, 16 pijlen, 2 gapples, 8 steak, schild. Geen pearls, geen potions.
- Per potje: full heal, kit reset, 5 seconden countdown.

**Regels**
- Wie er twee wint, wint.
- Duurt een potje langer dan 3 minuten, dan krimpt de border naar 6 x 6.

**Einde**
- Winnaar is King of the SMP. Kroning in de lobby, met iedereen erbij. Prijs: zie
  [06-open-keuzes.md](06-open-keuzes.md).
