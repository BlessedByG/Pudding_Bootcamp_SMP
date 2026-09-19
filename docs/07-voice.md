# Voice: Simple Voice Chat

Alle voice loopt via de mod **Simple Voice Chat** (van henkelmax). Iedereen installeert de
client-mod, de server draait de Paper-plugin. Geen Discord-call ernaast, anders is proximity
zinloos.

Spelers hoeven **niks** te doen, te typen of aan te klikken. De regels zijn simpel en de mod plus
Skript regelen ze automatisch.

## De regels

1. **Alles is proximity.** Basiskamp, doolhof, horde, Ei-bos, King zone, arena's: je hoort wie
   bij je in de buurt is. Bereik 48 blokken, fluister-toets voor kleiner bereik.
2. **Dood = spectator = niet hoorbaar voor de levenden.** Je hoort zelf nog alles wat er om je
   heen gebeurt, maar geen levende speler hoort jou. Ook hunters die in ronde 4 op hun respawn
   wachten zijn die 20 seconden stil.
3. **Doden horen elkaar wel.** Wie dood is zit automatisch in de voice-groep Doden, zodat de
   kijkers met elkaar kunnen praten waar ze ook rondzweven. Word je weer levend (na ronde 2), dan
   gaat dat ook automatisch weer uit.

Per ronde:

| Ronde | Levend | Dood / kijkers |
|---|---|---|
| Basiskamp, Doolhof, Horde, Het Ei | Proximity. | Ronde 2: spectator tot het einde van de ronde. Hoort alles, wordt niet gehoord, praat met de andere doden. |
| King of the SMP | Proximity, hunters én koning. | Spectator: hoort alles, wordt niet gehoord, praat met de andere doden. Wie op zijn respawn wacht is even stil. |
| FFA en Finale | Proximity. | Hetzelfde. Finalist 1 zit tijdens de FFA ook bij de doden. |
| Kroning | Proximity in de troonzaal. | – |

## Hoe de mod dat doet

Drie mechanismen; de speler merkt er niks van.

1. **Proximity** is de standaard van de mod. Bereik is `max_voice_distance`, standaard 48.
2. **Spectators.** Met `spectator_interaction=false` (de standaard) komt de stem van een spectator
   nooit bij niet-spectators aan. Horen doet een spectator wel gewoon. Dood is bij ons altijd
   spectator mode, dus regel 2 komt gratis mee. Een spectator kan zonder groep ook niet met andere
   spectators praten, daarom punt 3.
3. **De groep Doden.** Een Normal-groep: leden horen elkaar overal en horen niet-leden via
   proximity. De levenden horen de groep niet, want de leden zijn spectator (punt 2). Skript zet
   spectators automatisch in de groep door het join-command *als die speler* uit te voeren, en
   haalt ze er weer uit zodra ze geen spectator meer zijn. De speler ziet alleen het
   bevestigingsregeltje van de mod in zijn chat.

Dat een spectator in een Normal-groep niet hoorbaar is voor levenden in de buurt, is hoe de mod
zich hoort te gedragen (de spectator-check zit op het proximity-pad). **Test het in de testrun.**
Lekt het toch, zet Doden dan op type **Isolated**: doden horen dan alleen elkaar en niet meer de
levenden.

## De groep Doden

- Eén groep, de hele avond. Een groep verdwijnt als de laatste speler eruit gaat, dus de
  **voice-admin** (Admin 2) maakt hem bij de start aan via het groepsmenu (toets G): naam exact
  `Doden`, type Normal, geen wachtwoord. En blijft er de hele avond in zitten.
- Skript kijkt elke twee seconden: spectator die nog niet in de groep zit, erin; geen spectator
  meer maar wel in de groep, eruit. Zie [04-technische-schets.md](04-technische-schets.md).
- Verdwijnt de groep toch (voice-admin eruit gevallen): opnieuw aanmaken met dezelfde naam en
  `/bc voicesync` draaien. Iedereen zit binnen twee seconden weer goed.

## Eigen groepen maken is valsspelen

In ronde 4 zou een groepje hunters met een eigen groep een walkietalkie hebben over de hele map.
Dat mag niet. Spelregel: geen eigen groepen. Het is zichtbaar: de mod laat linksboven in beeld
zien in welke groep je zit, dus op je eigen stream staat het meteen. Op Paper kun je het maken van
groepen met permissions tot staff beperken; check dan wel dat het join-command dat Skript voor de
doden uitvoert nog werkt.

## Server-config

`voicechat-server.properties` (Paper: `plugins/voicechat/`):

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

- **Voice-test om 19:40** in het basiskamp: iedereen zegt wat, iedereen loopt een stuk weg en
  terug. Wie een kruis door het voice-icoontje heeft is niet verbonden (bijna altijd de UDP-poort
  of een verkeerde modversie).
- **Streams:** de mod is gewoon game-audio, dus proximity komt vanzelf op de stream. Push-to-talk
  of stemactivatie is aan de streamer zelf.
- **Host:** zit niet in de spelersvoice, praat op zijn eigen stream. Aankondigingen in-game gaan
  via `title` en chat. Wil de host toch tegen iedereen in-game praten, dan is er de addon
  *Voice Chat Broadcast* (ops kunnen met een toets naar iedereen omroepen); check of die er is
  voor jullie versie.
- **Uitgeschakelde spelers** mogen bij de doden alles zeggen, ook waar de koning zit. De levenden
  horen het toch niet. Wat niet mag: het via Discord of een andere stream alsnog doorgeven.
