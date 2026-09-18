# Draaiboek

Tijdschema, rollen en checklists voor de avond zelf. Tijden zijn een voorbeeld met start om 20:00.

## Tijdschema

| Tijd | Wat | Wie |
|---|---|---|
| 19:15 | Staff online. Wereldbackup maken. `reset` draaien. Elke poort en teleport even testen. | Admins |
| 19:40 | Whitelist open. Spelers joinen, komen in de lobby. Soundcheck met de host. | Iedereen |
| 19:55 | Host legt de regels uit (ronde 1 t/m 3 kort, ronde 4 nog niet). | Host |
| 20:00 | **Intro op stream.** Countdown, poort 1 open. | Host, Admin 1 |
| 20:02 | **Ronde 1: De Doolhof** (10 min) | |
| 20:13 | Wachtkamer 2: voorsprongkistjes, praatje. | Host |
| 20:16 | **Ronde 2: De Horde** (10 min) | |
| 20:27 | Wachtkamer 3: pearls voor de 0-deaths. | Host |
| 20:30 | **Pauze** (5 min). Toiletmoment, host houdt de stream warm. | Host |
| 20:35 | **Ronde 3: Het Ei** (10 min) | |
| 20:46 | Wachtkamer 4: host legt nu de kroonregels uit. Clown wordt naar de burcht geteleporteerd. | Host, Admin 1 |
| 20:50 | **Ronde 4: King of the SMP** (30 sec voorsprong + 15 min) | |
| 21:06 | Uitslag: finalist 1. Rest wordt naar de FFA geteleporteerd. | Admin 1 |
| 21:08 | **Ronde 5: Arena FFA** (max 10 min) | |
| 21:19 | Uitslag: finalist 2. Beide finalisten naar de 1v1-arena. | Admin 1 |
| 21:22 | **Ronde 6: De Finale** (best of 3, ± 10 min) | |
| 21:33 | **Kroning** in de lobby, iedereen erbij. SMP-aankondiging als die er is. | Host |
| 21:45 | Einde stream. | |

Totaal ruim 1,5 uur speeltijd, plan 2 uur met buffer. Als een ronde uitloopt: de timers zijn
hard, dus het loopt vooral uit door praatjes. Dat is de host z'n verantwoordelijkheid.

## Rollen

| Rol | Aantal | Wat |
|---|---|---|
| **Host / caster** | 1 | Praat op de stream, legt regels uit, kondigt rondes aan, houdt de wachtkamers gezellig. Zit in spectator. |
| **Admin 1: commander** | 1 | Draait de functies: poorten, starts, timers, teleports. Doet verder niks anders. |
| **Admin 2: ref** | 1 | Kijkt naar problemen: stuck spelers, disconnects, bugs met de kroon. Overrulet handmatig waar nodig. Houdt het randgevallen-lijstje uit [03-kroon-regels.md](03-kroon-regels.md) bij de hand. |
| **Camera** | 0 tot 2 | Spectator-accounts voor een mooi hoofdbeeld op de stream (top-down doolhof, overzicht King zone). Optioneel. |
| **Bouwers** | 2 tot 4 | Vooraf. Zie de bouwlijst hieronder. |

Eén persoon kan host en ref combineren als je krap zit, maar de commander moet alleen commander
zijn.

## Bouwlijst (vooraf)

Grofweg op volgorde van werk:

1. De Gang met 4 wachtkamers en 4 poorten (1 avond).
2. Lobby (uurtje).
3. Doolhof genereren en plaatsen, plafond dicht, kisten vullen (1 avond).
4. Horde-arena met 4 spawnpunten en dekking (1 avond).
5. Ei-zone: terrein kiezen, muur eromheen, Grote Ei bouwen en vullen, nep-eitjes, beacon
   eronder (1 tot 2 avonden).
6. King zone: terrein kiezen, burcht, dorpje, toren, 4 hunterspawns met hekjes (2 avonden).
7. FFA-arena en 1v1-arena (uurtje).
8. Datapack schrijven en per functie testen (2 avonden).
9. Volledige testrun met 4 of 5 testers (1 avond).

Reken op twee weken met een paar mensen die af en toe een avond hebben.

## Checklist: testrun

Doe minstens één keer de hele avond met 4 of 5 testers, van lobby tot kroning. Let vooral op:

- [ ] Elke poort opent en sluit met de juiste coördinaten.
- [ ] Achterblijvers worden na elke timer naar de goede wachtkamer geteleporteerd.
- [ ] Doolhof: niemand kan over of door het plafond. Kisten gevuld.
- [ ] Horde: waves volgen elkaar op, respawn na 15 sec werkt, bossbar telt mobs.
- [ ] Ei: drukplaat neemt het diamond block in en teleporteert. Zonder block gebeurt er niks.
      Beacon-hint gaat aan op 5 min.
- [ ] King: kroon gaat naar de killer (test met 2 man). Kroon gaat naar random hunter bij val-dood.
      Respawn na 20 sec. Sudden death stopt respawns en krimpt de border. Timer 0 wijst
      finalist 1 aan en teleporteert de rest.
- [ ] King: koning logt uit en weer in. Wat gebeurt er? Zorg dat de ref weet wat te doen.
- [ ] FFA: laatste levende wordt finalist 2, teleport naar de 1v1-arena werkt.
- [ ] Finale: kit reset en full heal per potje.
- [ ] `reset` brengt alles terug naar de lobby-staat.
- [ ] Serverperformance tijdens wave 5 met alle mobs.

## Checklist: dag zelf

- [ ] Wereldbackup gemaakt.
- [ ] `reset` gedraaid, iedereen start schoon.
- [ ] Whitelist compleet, staff heeft op.
- [ ] Bossbar zichtbaar voor iedereen.
- [ ] Coördinaten van alle tp-punten in een tekstbestand naast de commander.
- [ ] Ref heeft [03-kroon-regels.md](03-kroon-regels.md) open.
- [ ] Reserve-diamond-blocks in de admin-kist.

## Spelregels voor de streamers

Kort en op de lobbyborden:

1. Geen x-ray, geen cheats, geen mods die voordeel geven. Sodium en dat soort dingen mag.
2. Niet streamsnipen: niet kijken op andermans stream om de koning te vinden. Vertrouwen, geen
   controle. Wil je het hard afdwingen, dan een streamvertraging van een minuut voor iedereen.
3. Hunters zijn een team tot de FFA. In de FFA mag je teamen, maar er wint er één.
4. Bug of stuck? Roep de ref, niet de chat.
5. Als de admin zegt stop, dan stop.

## Als het misgaat

| Probleem | Oplossing |
|---|---|
| Server crasht | Backup terugzetten, `reset`, ronde opnieuw starten vanaf de laatste wachtkamer. |
| Timer loopt niet | `schedule clear` en de start-functie van de ronde opnieuw draaien met de resterende tijd. |
| Kroon zit bij niemand | `execute as <speler> run function bootcamp:king/give`. |
| Kroon zit bij twee spelers | `tag <verkeerde> remove king` en de helm eraf; ref beslist wie hem hoort te hebben (laatste kill). |
| Speler zit vast in een blok | `tp` door de ref. |
| Ei niet gevonden en de hint werkt niet | Ref zet handmatig een vuurpijl of zegt de richting in de chat. |
