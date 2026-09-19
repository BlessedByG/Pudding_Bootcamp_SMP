# Draaiboek

Tijdschema, rollen en checklists voor de avond zelf. Tijden zijn een voorbeeld met start om 20:00.

## Tijdschema

| Tijd | Wat | Wie |
|---|---|---|
| 19:15 | Staff online. Wereldbackup maken. `/bc reset` draaien. Elke poort en teleport even testen. | Admins |
| 19:40 | Whitelist open. Spelers spawnen in het basiskamp. Voice-test: iedereen zegt wat, loopt weg en komt terug. Wie de mod niet heeft wordt gekickt en installeert alsnog. | Iedereen |
| 19:55 | Host legt de regels uit (ronde 1 t/m 3 kort, ronde 4 nog niet). | Host |
| 20:00 | **Intro op stream.** Countdown, poort 1 open. | Host, Admin 1 |
| 20:02 | **Ronde 1: De Doolhof** (10 min) | |
| 20:13 | Verzamelpunt 2 bij de arena: voorsprongkistjes, praatje. | Host |
| 20:16 | **Ronde 2: De Horde** (10 min) | |
| 20:27 | Verzamelpunt 3 aan de bosrand: pearls voor de overlevers. | Host |
| 20:30 | **Pauze** (5 min). Toiletmoment, host houdt de stream warm. | Host |
| 20:35 | **Ronde 3: Het Ei** (10 min) | |
| 20:46 | De Kring: host legt nu de kroonregels uit. "Het lot beslist wie de koning wordt." Het Rad draait en landt op Clown. Clown naar de burcht. | Host, Admin 1 |
| 20:50 | **Ronde 4: King of the SMP** (30 sec voorsprong + 15 min) | |
| 21:06 | Uitslag: finalist 1. Rest wordt naar de FFA geteleporteerd. | Admin 1 |
| 21:08 | **Ronde 5: Arena FFA** (max 10 min) | |
| 21:19 | Uitslag: finalist 2. Beide finalisten naar de 1v1-arena. | Admin 1 |
| 21:22 | **Ronde 6: De Finale** (best of 3, ± 10 min) | |
| 21:33 | **Kroning** in de troonzaal, iedereen erbij. SMP-aankondiging als die er is. | Host |
| 21:45 | Einde stream. | |

Totaal ruim 1,5 uur speeltijd, plan 2 uur met buffer. Als een ronde uitloopt: de timers zijn
hard, dus het loopt vooral uit door praatjes. Dat is de host z'n verantwoordelijkheid.

## Rollen

| Rol | Aantal | Wat |
|---|---|---|
| **Host / caster** | 1 | Praat op de stream, legt regels uit, kondigt rondes aan, houdt de verzamelpunten gezellig. Zit in kijkersmodus. Speelt Het Rad recht: "iedereen kan de koning worden". |
| **Admin 1: commander** | 1 | Draait de functies: poorten, starts, timers, teleports. Doet verder niks anders. |
| **Admin 2: ref** | 1 | Kijkt naar problemen: stuck spelers, disconnects, bugs met de kroon. Overrulet handmatig waar nodig. Houdt het randgevallen-lijstje uit [03-kroon-regels.md](03-kroon-regels.md) bij de hand. |
| **Camera** | 0 tot 2 | Kijker-accounts voor een mooi hoofdbeeld op de stream (top-down doolhof, overzicht King zone). Optioneel. |
| **Bouwers** | 2 tot 4 | Vooraf. Zie de bouwlijst hieronder. |

Eén persoon kan host en ref combineren als je krap zit, maar de commander moet alleen commander
zijn.

## Bouwlijst (vooraf)

Grofweg op volgorde van werk:

1. De wereld kiezen of genereren: ± 500 x 500 met heuvels, bos en water. Bepaal waar elke zone
   komt (plattegrond in [01-map-en-flow.md](01-map-en-flow.md)) en zet alle coördinaten in een
   bestand (1 avond).
2. Basiskamp en de verzamelpunten: kampvuur, banners, bordjes (uurtje). De Kring met 20 pilaren,
   koppen en lichtblokken (uurtje extra).
3. Hagendoolhof genereren en in het dal plaatsen, kisten vullen (1 avond).
4. Ruïne-arena met 4 spawnpunten, dekking en een poort (1 avond).
5. Ei-bos: plek kiezen, Grote Ei bouwen en vullen, nep-eitjes, beacon eronder, drukplaat bij de
   uitgang (1 tot 2 avonden).
6. Burcht op de heuvel met binnenplaats (FFA) en troonzaal (finale), dorpje, toren, 4
   hunterspawns aan de rand van de King zone (2 avonden).
7. De mod bouwen, module voor module, en per onderdeel testen op de dev-server (4 tot 5 avonden,
   zie [04-technische-schets.md](04-technische-schets.md)). Daarna met de wand en `/bc point`
   alle regio's en punten in de wereld zetten (uurtje).
8. Volledige testrun met 4 of 5 testers (1 avond).

Reken op twee weken met een paar mensen die af en toe een avond hebben.

## Checklist: testrun

Doe minstens één keer de hele avond met 4 of 5 testers, van basiskamp tot kroning. Let vooral op:

- [ ] Fabric Loader, Fabric API, de voice-mod en de bootcamp-mod draaien op 26.2 zonder errors in
      de console.
- [ ] Alle regio's en punten staan erin (`/bc region list`, `/bc point list`) en `/bc region show`
      laat de goede randen zien.
- [ ] Elke poort opent en sluit op de juiste plek.
- [ ] Achterblijvers worden na elke timer naar het goede verzamelpunt geteleporteerd.
- [ ] Worldborder staat per ronde om de goede zone en niemand staat erbuiten na de teleport.
- [ ] Doolhof: niemand kan over of door het plafond. Kisten gevuld.
- [ ] Horde: waves volgen elkaar op, dood = kijker tot het einde, doden worden bij verzamelpunt 3
      weer levend, bossbar telt mobs. Ronde stopt ook als iedereen dood is.
- [ ] Voice: proximity werkt; een kijker is niet hoorbaar voor een levende speler ernaast, hoort
      die wel, en hoort andere kijkers overal. Niemand kan een eigen groep maken. Dit is de
      belangrijkste voice-test, zie [07-voice.md](07-voice.md).
- [ ] Kijkers: dood = onzichtbaar vliegen met de twee tp-items; pijlen en klappen gaan door je
      heen, je kunt niks oppakken of aanraken, je staat niet op de locator bar, en de items
      openen de spelerslijst en teleporteren.
- [ ] Ei: drukplaat neemt het diamond block in en teleporteert. Zonder block gebeurt er niks.
      Beacon-hint gaat aan op 5 min.
- [ ] Rad: landt op de speler met tag `uitverkoren`. Draai hem vijf keer, dan zie je meteen of
      de slot-scores kloppen met de volgorde van de koppen.
- [ ] King: kroon gaat naar de killer (test met 2 man). Kroon gaat naar random hunter bij val-dood.
      Respawn na 20 sec. Sudden death stopt respawns en krimpt de border. Timer 0 wijst
      finalist 1 aan en teleporteert de rest.
- [ ] Kroonwissel: alle hunters staan geheald en bevroren op hun startpunt (niet lopen, niet
      springen, niet pearlen), de nieuwe koning staat in de burcht met volle armor, na 10
      seconden is iedereen los en loopt weer normaal. Ook testen tijdens sudden death (sd-punten)
      en terwijl iemand op zijn respawn wacht.
- [ ] King: koning logt uit en weer in. Wat gebeurt er? Zorg dat de ref weet wat te doen.
- [ ] FFA: laatste levende wordt finalist 2, teleport naar de 1v1-arena werkt.
- [ ] Finale: kit reset en full heal per potje.
- [ ] `/bc reset` brengt alles terug naar de basiskamp-staat.
- [ ] Serverperformance tijdens wave 5 met alle mobs.

## Checklist: dag zelf

- [ ] Wereldbackup gemaakt.
- [ ] `/bc reset` gedraaid, iedereen start schoon.
- [ ] Whitelist compleet, staff heeft op.
- [ ] Bossbar zichtbaar voor iedereen.
- [ ] Coördinaten van alle tp-punten in een tekstbestand naast de commander.
- [ ] Ref heeft [03-kroon-regels.md](03-kroon-regels.md) open.
- [ ] Reserve-diamond-blocks in de admin-kist.
- [ ] Tag `uitverkoren` staat op Clown en op niemand anders (`tag @a list`).
- [ ] Voice: `force_voice_chat=true`, UDP-poort open, voice-mod en bootcamp-mod geladen (staat in
      de serverlog bij het opstarten).
- [ ] Host en camera-accounts staan in kijkersmodus (`/bc kijker <naam> aan`).
- [ ] De jar van de vorige werkende versie van de mod staat klaar naast de huidige.

## Spelregels voor de streamers

Kort en op de borden in het basiskamp:

1. Geen x-ray, geen cheats, geen mods die voordeel geven. Sodium en dat soort dingen mag.
   Simple Voice Chat is verplicht.
2. Niet streamsnipen: niet kijken op andermans stream om de koning te vinden. Vertrouwen, geen
   controle. Wil je het hard afdwingen, dan een streamvertraging van een minuut voor iedereen.
3. Hunters zijn een team tot de FFA. In de FFA mag je teamen, maar er wint er één.
4. Bug of stuck? Roep de ref, niet de chat.
5. Als de admin zegt stop, dan stop.
6. Geen Discord-call tijdens het event, alleen de voice-mod. Eigen voice-groepen maken kan niet,
   de mod blokkeert dat.

## Als het misgaat

| Probleem | Oplossing |
|---|---|
| Server crasht | Backup terugzetten, `/bc reset`, ronde opnieuw starten vanaf het laatste verzamelpunt. |
| Timer loopt niet | `/bc stop`, dan `/bc start <ronde>` en met `/bc timer <seconden>` de resterende tijd terugzetten. |
| Rad stopt op de verkeerde kop | Slot-score van die speler klopt niet met de plek van zijn kop. Host: "technische storing", ref fixt de score, rad nog een keer. |
| Kroon zit bij niemand | `/bc kroon <speler>`. |
| Kroon zit bij twee spelers | `tag <verkeerde> remove king` en de helm eraf; ref beslist wie hem hoort te hebben (laatste kill). |
| Speler zit vast in een blok | `tp` door de ref. |
| Ei niet gevonden en de hint werkt niet | Ref zet handmatig een vuurpijl of zegt de richting in de chat. |
| Iemand hoort niks in voice | Kruis door het voice-icoontje: UDP-poort dicht of verkeerde modversie. Geen kruis maar toch stil: kijk of hij per ongeluk als kijker staat (`/bc kijker <naam> uit`). |
| De mod gooit errors in de console | `/bc stop`, `/bc reset`, ronde opnieuw. Blijft het misgaan: server herstarten met de vorige jar. Daarom alles vooraf testen en die jar bij de hand houden. |
