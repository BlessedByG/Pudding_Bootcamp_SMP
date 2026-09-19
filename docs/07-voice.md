# Voice: Simple Voice Chat

Alle voice loopt via de mod **Simple Voice Chat** (van henkelmax). Iedereen installeert de
client-mod, de server draait de server-mod (Fabric/Forge) of de Paper-plugin. Geen Discord-call
ernaast, anders is proximity zinloos.

## Regels per ronde

| Ronde | Levend | Dood / kijkers |
|---|---|---|
| Basiskamp | Proximity, iedereen bij elkaar rond het kampvuur. | – |
| 1 De Doolhof | Proximity. Je hoort wie in de gang naast je loopt, niet wie aan de andere kant zit. | – (niemand gaat dood) |
| 2 De Horde | Proximity. | Dood = spectator tot het einde van de ronde. Je hoort alles, de levenden horen jou niet. Je zit met de andere doden in de groep **Doden**, zodat je met elkaar kunt praten waar je ook rondzweeft. |
| 3 Het Ei | Iedereen in de groep **Ei**: je hoort elkaar overal in het zoekgebied, geen proximity. | – |
| 4 King of the SMP | Proximity, hunters én koning. | Spectator: je hoort alles, niemand hoort jou. Wie eruit ligt zit in de groep **Doden**. Hunters die 20 seconden op hun respawn wachten zijn ook even spectator: horen alles, zeggen niks, hoeven niet in de groep. |
| 5 Arena FFA | Proximity. | Zoals ronde 4. Finalist 1 wacht ook in **Doden**. |
| 6 De Finale | Proximity, de twee finalisten. | Iedereen anders in **Doden**. |
| Kroning | Proximity in de troonzaal, iedereen uit zijn groep. | – |

Kort: levend is altijd proximity, behalve in ronde 3. Dood is altijd "alles horen, niet gehoord
worden, praten met de andere doden". Ronde 5 en 6 lopen gewoon door op de stand van ronde 4.

## Hoe de mod dat doet

Drie mechanismen uit de mod, meer is er niet nodig:

1. **Proximity** is de standaard. Bereik is `max_voice_distance`, standaard 48 blokken. Er is een
   fluister-toets met kleiner bereik; handig in het doolhof en in de King zone.
2. **Groepen.** Wie in een groep zit hoort de andere leden overal. Groepstype **Normal**: je hoort
   daarnaast ook spelers zonder groep via proximity, en zij horen jou via proximity. Dat laatste
   is voor de doden geen probleem, zie punt 3.
3. **Spectators.** Met `spectator_interaction=false` (de standaard) komt de stem van een spectator
   nooit bij niet-spectators aan. Horen doet een spectator wel gewoon, alles binnen bereik. Dus:
   dood = spectator = alles horen, niet gehoord worden. De groep Doden is er alleen zodat de doden
   elkaar ook op afstand horen.

Dat een spectator in een Normal-groep niet hoorbaar is voor levenden in de buurt, is hoe de mod
zich hoort te gedragen (de spectator-check zit op het proximity-pad). **Test het in de testrun.**
Lekt het toch, zet Doden dan op type **Isolated**: doden horen dan alleen elkaar en niet meer de
levenden. Minder leuk, wel dicht.

## De groepen

- **Ei**: iedereen, alleen tijdens ronde 3.
- **Doden**: alle uitgeschakelde spelers vanaf ronde 2, plus finalist 1 tijdens de FFA.

Een groep verdwijnt als de laatste speler eruit gaat. Daarom heeft de ref (Admin 2) een tweede
taak als **voice-admin**: die maakt de groep aan via het groepsmenu (toets G), zonder wachtwoord,
type Normal, en blijft erin zitten. Eén account kan maar in één groep tegelijk, dus:

- Start ronde 2: voice-admin maakt **Doden** en blijft erin.
- Start ronde 3: voice-admin maakt **Ei** (verlaat daarmee Doden; niemand is dan dood, dus prima).
- Start ronde 4: voice-admin maakt **Doden** opnieuw en blijft erin tot de kroning.

Spelers wisselen zelf van groep, maar hoeven niks te typen: Skript stuurt op de goede
momenten een klikbare knop in de chat (zie [04-technische-schets.md](04-technische-schets.md)).
Klik op **[DODEN]** en je zit erin. Achter de knop zit gewoon `/voicechat join Doden`,
`/voicechat join Ei` of `/voicechat leave`.

Wanneer de knop komt:

| Moment | Knop | Voor wie |
|---|---|---|
| Dood in ronde 2 | [DODEN] | De dode |
| Start ronde 3 | [EI-VOICE] | Iedereen |
| Start ronde 4 (na het rad) | [VERLATEN] | Iedereen |
| Eruit in ronde 4, 5 of 6 (ex-koning, sudden death, FFA) | [DODEN] | De uitgeschakelde |
| Einde ronde 4 | [DODEN] | Finalist 1 |
| Start finale | [VERLATEN] | Beide finalisten |
| Kroning | [VERLATEN] | Iedereen |

## Eigen groepen maken is valsspelen

In ronde 4 zou een groepje hunters met een eigen groep een walkietalkie hebben over de hele map.
Dat mag niet. Spelregel: alleen de groepen Ei en Doden, en alleen als de knop komt. Het is
zichtbaar: de mod laat linksboven in beeld zien in welke groep je zit, dus op je eigen stream
staat het meteen. Op Paper kun je het maken van groepen met permissions tot staff beperken;
check dan wel dat spelers nog gewoon via `/voicechat join` de officiële groepen in kunnen.

## Server-config

`voicechat-server.properties` (Fabric: `config/voicechat/`, Paper: `plugins/voicechat/`):

```
port=24454
max_voice_distance=48
enable_groups=true
spectator_interaction=false
spectator_player_possession=false
force_voice_chat=true
```

- `force_voice_chat=true` kickt iedereen die de mod niet heeft. Voor een event wil je dat: dan
  weet je bij het joinen meteen wie er nog moet installeren.
- **UDP-poort 24454** moet open staan naast de normale TCP-poort van de server. Bij een hoster
  moet je die vaak apart aanvragen. Dit is de nummer-één oorzaak van "ik hoor niks".
- De client- en servermod moeten dezelfde hoofdversie hebben.

## Praktisch

- **Voice-test om 19:40** in het basiskamp: iedereen zegt wat, iedereen loopt een stuk weg en terug.
  Wie een kruis door het voice-icoontje heeft is niet verbonden (bijna altijd de UDP-poort of een
  verkeerde modversie).
- **Streams:** de mod is gewoon game-audio, dus proximity komt vanzelf op de stream. Push-to-talk
  of stemactivatie is aan de streamer zelf.
- **Host:** zit niet in de spelersvoice, praat op zijn eigen stream. Aankondigingen in-game gaan
  via `title` en chat. Wil de host toch tegen iedereen in-game praten, dan is er de addon
  *Voice Chat Broadcast* (ops kunnen met een toets naar iedereen omroepen); check of die er is
  voor jullie versie.
- **Uitgeschakelde spelers** mogen in de Doden-groep alles zeggen, ook waar de koning zit. De
  levenden horen het toch niet. Wat niet mag: het via Discord of een andere stream alsnog
  doorgeven.
