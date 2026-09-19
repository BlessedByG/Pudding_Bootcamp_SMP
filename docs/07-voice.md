# Voice: Simple Voice Chat

Alle voice loopt via de mod **Simple Voice Chat** (van henkelmax). Iedereen installeert de
client-mod, de server draait de Fabric-versie. Geen Discord-call ernaast, anders is proximity
zinloos.

Spelers hoeven **niks** te doen, te typen of aan te klikken. De regels zijn simpel en de
bootcamp-mod regelt ze via de API van de voice-mod.

## De regels

1. **Alles is proximity.** Basiskamp, doolhof, horde, Ei-bos, King zone, arena's: je hoort wie
   bij je in de buurt is. Bereik 48 blokken, fluister-toets voor kleiner bereik.
2. **Dood = kijker = niet hoorbaar voor de levenden.** Je hoort zelf nog alles wat er om je heen
   gebeurt, maar geen levende speler hoort jou. Ook hunters die in ronde 4 op hun respawn wachten
   zijn die 20 seconden stil.
3. **Doden horen elkaar wel, overal.** Kijkers kunnen met elkaar praten waar ze ook rondzweven.
   Word je weer levend (na ronde 2), dan is dat vanzelf weer voorbij.

Per ronde:

| Ronde | Levend | Dood / kijkers |
|---|---|---|
| Basiskamp, Doolhof, Horde, Het Ei | Proximity. | Ronde 2: kijker tot het einde van de ronde. Hoort alles, wordt niet gehoord, praat met de andere doden. |
| King of the SMP | Proximity, hunters én koning. | Kijker: hoort alles, wordt niet gehoord, praat met de andere doden. Wie op zijn respawn wacht is even stil. |
| FFA en Finale | Proximity. | Hetzelfde. Finalist 1 kijkt tijdens de FFA ook als kijker. |
| Kroning | Proximity in de troonzaal. | – |

## Hoe de mod dat doet

De bootcamp-mod is ook een voice-plugin. Drie dingen, allemaal in code, de speler merkt er niks
van:

1. **Filter.** Bij elk geluidspakket dat de voice-mod wil versturen (`SoundPacketEvent`) kijkt de
   mod: is de zender een kijker en de ontvanger levend? Dan gaat het pakket niet. Dat is regel 2,
   en het hangt aan de eigen dood-vlag van de mod, niet aan spectator mode.
2. **Verborgen groep.** Bij het opstarten maakt de mod één persistente, verborgen groep. Wie
   kijker wordt gaat erin, wie weer levend wordt eruit. Leden van een groep horen elkaar overal,
   dus regel 3. Levenden zitten nooit in een groep, dus voor hen is alles proximity. Niemand hoeft
   de groep te maken of erin te blijven, de mod doet het.
3. **Slot op eigen groepen.** Het maken van of joinen bij een groep wordt geannuleerd voor
   iedereen zonder staff-rol. Een groepje hunters met een walkietalkie over de hele map kan dus
   niet.

Test in de testrun: een kijker naast een levende speler; de levende hoort niks, de kijker hoort
alles, en twee kijkers aan weerskanten van de map horen elkaar.

## Server-config

`config/voicechat/voicechat-server.properties`:

```
port=24454
max_voice_distance=48
enable_groups=true
force_voice_chat=true
```

- `enable_groups` moet aan blijven, anders kan de mod de kijkersgroep niet maken. Spelers kunnen
  er zelf toch niks mee, zie hierboven.
- `force_voice_chat=true` kickt iedereen die de mod niet heeft. Voor een event wil je dat: dan
  weet je bij het joinen meteen wie er nog moet installeren.
- **UDP-poort 24454** moet open staan naast de normale TCP-poort van de server. Bij een hoster
  moet je die vaak apart aanvragen. Dit is de nummer-één oorzaak van "ik hoor niks".
- De client- en servermod moeten dezelfde hoofdversie hebben, en de `voicechat-api` waarmee de
  bootcamp-mod gebouwd is moet bij die versie passen.

## Praktisch

- **Voice-test om 19:40** in het basiskamp: iedereen zegt wat, iedereen loopt een stuk weg en
  terug. Wie een kruis door het voice-icoontje heeft is niet verbonden (bijna altijd de UDP-poort
  of een verkeerde modversie).
- **Streams:** de mod is gewoon game-audio, dus proximity komt vanzelf op de stream. Push-to-talk
  of stemactivatie is aan de streamer zelf.
- **Host:** zit niet in de spelersvoice, praat op zijn eigen stream. Aankondigingen in-game gaan
  via `title` en chat. Wil de host toch tegen iedereen in-game praten, dan kan de mod een
  omroepknop voor staff krijgen (zelfde API: een pakket van staff naar iedereen doorlaten), of
  je gebruikt de addon *Voice Chat Broadcast*.
- **Uitgeschakelde spelers** mogen als kijker alles zeggen, ook waar de koning zit. De levenden
  horen het toch niet. Wat niet mag: het via Discord of een andere stream alsnog doorgeven.
