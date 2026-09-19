# Voice: Simple Voice Chat

Alle voice loopt via de mod **Simple Voice Chat** (van henkelmax). Iedereen installeert de
client-mod, de server draait de Fabric-versie. Geen Discord-call ernaast, anders is proximity
zinloos.

Spelers hoeven **niks** te doen, te typen of aan te klikken, en de bootcamp-mod doet ook niks met
voice. Het is puur proximity, de hele avond.

## De regels

1. **Alles is proximity, voor iedereen.** Basiskamp, doolhof, horde, Ei-bos, de Arena: je hoort
   wie bij je in de buurt is. Bereik 48 blokken, fluister-toets voor kleiner bereik.
2. **Dood ben je publiek.** Op de tribune hoor je de vloer, en de vloer hoort jou. Joelen,
   juichen, roepen waar iemand zit: mag allemaal. In een open arena met een glowende koning verraad
   je toch niks.
3. **Geen groepen.** Die staan in de voice-config uit, dus niemand kan een walkietalkie maken over
   de hele map.

Per ronde:

| Ronde | Levend | Dood / kijkers |
|---|---|---|
| Basiskamp, Doolhof, Horde, Het Ei | Proximity. | Ronde 2: op de tribune van de ruïne-arena, hoorbaar voor de vloer en voor elkaar. |
| King of the SMP | Proximity, hunters én koning. | Op de tribune van de Arena: publiek. |
| FFA en Finale | Proximity. | Tribune, ook finalist 1 en Clown. |
| Kroning | Proximity in de Arena, tribunes vol. | – |

## Wat de mod doet

Niks. Simple Voice Chat draait naast de bootcamp-mod en die heeft de voice-API niet nodig. Wil je
later toch iets extra's, zoals een omroepknop voor de host, dan is de API er en is dat een losse
uitbreiding.

## Server-config

`config/voicechat/voicechat-server.properties`:

```
port=24454
max_voice_distance=48
enable_groups=false
force_voice_chat=true
```

- `enable_groups=false`: geen groepen, dus alles is altijd proximity en niemand kan er een maken.
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
- **Tribune:** de doden mogen alles roepen. De enige regel die blijft is niet streamsnipen (zie
  het draaiboek).
