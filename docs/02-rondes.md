# De rondes

Alle rondes uitgewerkt. Per ronde: doel, setup, regels, hoe het eindigt, wat je eraan overhoudt
(bonus) en wat er mis kan gaan.

Getallen zijn uitgangspunt voor 20 spelers. Schaal ze mee met het aantal spelers.

---

## Ronde 0: Basiskamp

Iedereen spawnt in het basiskamp, een kampement bij de poort van het doolhof, en leest de regels
op de borden. Host legt kort uit wat er komen
gaat (niet alles verklappen: de kroonregels pas uitleggen bij ronde 4). Countdown, poort 1 open.

Iedereen zit in één team (`spelers`) met friendly fire uit, dus je kunt elkaar niet raken tot
ronde 4.

**Voice:** proximity vanaf het moment dat je joint. Voice-test in het basiskamp voor de start
(zie [07-voice.md](07-voice.md)).

---

## Ronde 1: De Doolhof

**Doel:** vind de uitgang van het doolhof.

**Setup**
- 64 x 64 hagendoolhof in een dal, hagen 4 hoog, geen plafond. Ingang bij het basiskamp, uitgang
  aan de overkant.
- Adventure mode: niet breken, niet bouwen.
- 10 tot 15 kisten in doodlopende gangen met kleine loot die je later in de avond kunt gebruiken:
  een gapple, wat pijlen, een potion. Geen pearls: zonder plafond gooi je die zo over de haag.
- Optioneel: 2 of 3 "gevaarlijke" gangen met een zombie-spawner of een valkuil. Niet dodelijk,
  wel vervelend.
- Iedereen start tegelijk bij dezelfde ingang. Wil je het wat spreiden, maak dan 4 ingangen in de
  hoeken en één uitgang in het midden.

**Regels**
- Niemand heeft pearls in deze ronde (de eerste komen uit het voorsprongkistje erna), dus over de
  haag heen kan niet. Adventure mode, dus door de haag heen ook niet.
- Alles wat je vindt mag je houden.

**Einde**
- Timer 10 minuten. Wie de uitgang vindt, wordt doorgezet naar verzamelpunt 2 bij de horde-arena
  en hangt daar met de rest tot de ronde klaar is.
- Na de timer wordt iedereen die nog in het doolhof zit ook naar verzamelpunt 2 geteleporteerd.
  Niemand ligt eruit.

**Bonus:** de eerste 5 spelers die eruit zijn krijgen een voorsprongkistje bij verzamelpunt 2:
1 gapple + 1 ender pearl.

**Voice:** proximity. Je hoort wie in de gang naast je loopt, en dat is het.

**Wat train je:** oriëntatie, rustig blijven, dead ends herkennen.

**Wat kan misgaan**
- Niemand vindt de uitgang: na 7 minuten een `title` met een hint ("de uitgang ligt aan de
  noordkant"). Uiteindelijk lost de timer het op.
- Spelers die stuck staan: admin kan ze met `tp` een gang verder zetten.

---

## Ronde 2: De Horde

**Doel:** overleef als groep 5 waves mobs.

**Setup**
- Ruïne-arena Ø 50 ten oosten van de Arena, met wat dekking. 4 spawnpunten voor mobs aan de rand.
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
- Doodgaan = kijker tot het einde van de ronde. Je krijgt groot een doodtekst in beeld ("Grote L
  gepakt!") en staat op de tribune van de ruïne-arena, met zicht op de vloer en met de andere
  doden. Je hoort alles en de vloer hoort jou. Aan het eind van de ronde gaat iedereen door, dood
  of levend.
- De volgorde van sneuvelen komt op het scoreboard in de sidebar. Puur voor de eer.

**Einde**
- Wave 5 dood, iedereen dood, of timer 10 minuten. Iedereen naar verzamelpunt 3 aan de bosrand,
  de doden worden daar weer levend gemaakt.
- Loot die mobs droppen mag je houden (pijlen, wat rommel).

**Bonus:** iedereen die de ronde overleeft, krijgt 1 ender pearl bij verzamelpunt 3.

**Voice:** proximity voor iedereen. Dood = kijker op de tribune: je hoort de vloer en de vloer
hoort jou, de tribune is publiek.

**Wat train je:** mobs, boog en schild, samen vechten, niet in de creeper rennen.

**Wat kan misgaan**
- Server lag door 60 mobs + 20 spelers: houd view distance op 8 en spawn mobs verspreid over de
  4 punten in plaats van alles op één plek.
- Mobs blijven ergens hangen (achter dekking, in een gat): wave-timer van 2 minuten vangt dit op.
  Admin kan met `kill @e[tag=horde]` een wave forceren.
- Iedereen ligt er op wave 3 al uit: dan was het te zwaar, en een arena vol kijkers is saai.
  Zonder respawns wil je de waves liever iets te makkelijk dan te moeilijk; schaal ze in de
  testrun.

---

## Ronde 3: Het Ei

**Doel:** vind het Grote Ei, hak je naar binnen, pak zoveel loot als je kunt dragen en neem
minstens één diamond block mee. Dat block is je ticket naar ronde 4.

**Setup**
- 150 x 150 zoekgebied: het bos ten zuiden van de Arena, met heuvels, grotten en een meertje. De
  worldborder sluit het af, geen muur nodig.
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
- Voice is gewoon proximity, ook hier. Wie het Ei vindt hoort alleen wie in de buurt is; roepen
  kan, maar dan komt iedereen.
- Pak wat je pakken kunt, maar je hebt maar één inventory.
- Je komt alleen bij De Kring (verzamelpunt 4) met een diamond block op zak: de uitgang van het
  bos is een drukplaat die het block inneemt en je erheen teleporteert.

**Einde**
- Timer 10 minuten. Wie een block heeft, gaat zelf via de drukplaat (block wordt ingenomen, je
  staat bij De Kring).
- Na de timer wordt iedereen zonder ticket alsnog doorgelaten, maar met een **lege inventory +
  basiskit**. Geen ticket = geen loot. Dat is de straf.

**Bonus:** je loot is je bonus. Wie snel is heeft de beste spullen voor ronde 4.

**Voice:** proximity, zoals overal.

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

**Doel:** heb de kroon als de timer afloopt, of als er geen hunter meer over is.

**Setup**
- De Arena: een colosseum met een vloer van Ø 60 tot 80 met dekking, een verhoogd midden voor de
  koning en tribunes rondom voor de doden. **Bouwen is verboden**; adventure mode zorgt dat het
  ook niet kan.
- Het begint met **Het Rad**: iedereen op de vloer, een lampje loopt langs de 20 pilaren met
  spelerskoppen rond de vloer, steeds langzamer, en stopt op ClownPierce. Het ziet eruit als
  toeval, het is rigged: Clown heeft vooraf de verborgen rol `uitverkoren` en het rad landt
  altijd op die speler (zie [03-kroon-regels.md](03-kroon-regels.md)).
- Clown wordt naar het midden geteleporteerd en krijgt de kroon en de **bosskit** (volledig
  diamond Protection II, diamond sword Sharpness II, boog Power II, 32 pijlen, 4 gapples, 8
  pearls, schild).
- De 19 hunters starten op 4 punten aan de rand van de vloer met de spullen uit ronde 3 (of de
  basiskit als ze niks hebben). Team `hunters`, friendly fire aan: hunters kunnen elkaar raken.
- De koning heeft Glowing: je ziet hem door alles heen. Bossbar: timer + naam van de koning.
- De koning krijgt 30 seconden voorsprong: de hunters staan die tijd bevroren op hun startpunt,
  met een countdown in beeld.

**Regels in het kort**
- **Eén leven.** Wie doodgaat, hunter of koning, is uit de ronde en gaat als kijker de tribune op.
  Geen respawns. Groot in beeld voor de dode: een willekeurige doodtekst ("Grote L gepakt!",
  "Had je nou maar beter je best gedaan", "Gelukkig is dit niet de CSMP").
- Kill de koning en je krijgt de kroon. Elke kroonwissel is een **reset**: alle levende hunters
  worden geheald en terug naar hun startpunt aan de rand geteleporteerd. De nieuwe koning wordt
  geheald, krijgt zijn armor en wapens gerepareerd, het kroonpakketje (2 gapples, 2 pearls), 15
  seconden Resistance en Glowing, en staat in het midden. Tien seconden countdown waarin niemand
  van zijn plek kan, dan los. Doden blijven dood.
- Gaat de koning dood zonder killer, dan gebeurt precies hetzelfde. De kroon gaat naar de laatste
  die hem raakte.
- Hunters mogen elkaar raken. Samenwerken mag, verraden ook.
- Bouwen is verboden. Pearls, gapples, boog en schild wel.
- Timer 15 minuten. Wie de kroon heeft als de timer afloopt, of als alle hunters dood zijn, is
  **finalist 1**.

**Einde**
- Finalist 1 en Clown gaan de tribune op. **Iedereen anders gaat door naar ronde 5**, ook wie in
  ronde 4 doodging, hunter of ex-koning. Ronde 4 is een eigen wedstrijd; de FFA begint schoon.
- Is Clown nog koning, dan is hij finalist 1. Is hij de kroon kwijt, dan is de eindbaas klaar en
  kijkt hij. Hij doet nooit mee aan de FFA.

**Voice:** proximity voor iedereen. Dood = kijker op de tribune: je hoort de vloer en de vloer
hoort jou. Roepen waar de koning zit mag, hij glowt toch.

**Wat train je:** PvP tegen overmacht, target focus, wanneer je wel en niet moet gaan.

---

## Ronde 5: Arena FFA

**Doel:** laatste die overblijft.

**Setup**
- Iedereen behalve Clown en finalist 1 wordt full hp op de vloer van de Arena gezet, verspreid
  over de rand. Ook wie in ronde 4 doodging.
- Iedereen krijgt dezelfde **arenakit**: volledig diamond Protection I, diamond sword Sharpness I,
  boog Power I, 16 pijlen, 2 gapples, 8 steak, schild. Eigen spullen worden weggehaald.
- Teams weg: iedereen kan iedereen raken.
- 10 seconden countdown, dan los.

**Regels**
- Dood = uit, tribune op. Doodtekst groot in beeld.
- Teamen mag, maar er wint er maar één. Op eigen risico.
- Na 5 minuten krimpt de worldborder in 2 minuten naar 10 x 10, zodat het niet blijft hangen.

**Einde**
- Laatste levende speler krijgt de tweede kroon: **finalist 2**.
- Hard maximum 10 minuten; staan er dan nog meerdere, dan beslist het aantal kills.
- Daarna **twee minuten rust**. Finalist 2 komt net uit een gevecht, finalist 1 is uitgerust. De
  host bouwt het moment op, de finalisten staan naast elkaar op de tribune.

**Voice:** loopt door op de stand van ronde 4. Proximity voor iedereen, dood wordt kijker op de
tribune. Finalist 1 en Clown zitten er ook en mogen meejoelen.

---

## Ronde 6: De Finale

**Doel:** 1v1 tussen de twee kroondragers, best of 3.

**Setup**
- Het midden van de Arena, border 20 x 20, twee startpunten tegenover elkaar. Tribunes vol.
- Beiden krijgen de **finalekit**: volledig diamond Protection II, diamond sword Sharpness II,
  boog Power I, 16 pijlen, 2 gapples, 8 steak, schild. Geen pearls, geen potions.
- Per potje: full heal, kit reset, 5 seconden countdown.

**Regels**
- Wie er twee wint, wint.
- Duurt een potje langer dan 3 minuten, dan krimpt de border naar 6 x 6.

**Einde**
- Winnaar is King of the SMP. Kroning in het midden van de Arena, iedereen op de tribune. Geen
  prijs, just for fun: de eer, de kroon en de tribunes vol.

**Voice:** de twee finalisten proximity op de vloer, alle anderen kijker. Bij de kroning is
iedereen weer levend: proximity in de Arena.
