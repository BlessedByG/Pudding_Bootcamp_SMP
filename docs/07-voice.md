# Voice: Simple Voice Chat

Alle voice loopt via de mod **Simple Voice Chat** (van henkelmax). Iedereen installeert de
client-mod (die zit in het modpack voor de streamers, zie [modpack/LEESMIJ.md](../modpack/LEESMIJ.md)), de
server draait de Fabric-versie. Geen Discord-call ernaast, anders is proximity
zinloos.

Spelers hoeven **niks** te doen, te typen of aan te klikken, en de bootcamp-mod doet ook niks met
voice. Het is puur proximity, de hele avond, ook in de teamrondes: er zijn geen teamkanalen.

## De regels

1. **Alles is proximity, voor iedereen.** Je hoort wie bij je in de buurt is. Bereik 48 blokken,
   fluister-toets voor kleiner bereik.
2. **Af ben je publiek.** Op de tribune en in de kooi hoor je het veld, en het veld hoort jou.
3. **Geen groepen.** Die staan in de voice-config uit, dus niemand kan een walkietalkie maken over
   de hele map, ook niet met zijn team.

Per ronde:

| Ronde | Levend | Af / kijkers |
|---|---|---|
| Basiskamp, Doolhof, Het Ei | Proximity. | – |
| Mob Arena | Proximity. Je staat met één speler van elk ander team in de arena. | De kooi in het midden van je arena, of de tribune: hoorbaar voor wie in de buurt is. |
| Quiz | Proximity. De hal is klein: iedereen hoort Pudding op het podium en elkaar. Overleggen met je team kan, de rest hoort mee. | – |
| Clown vs All | Proximity, jagers en kroonhouder. | Tribune van de Arena: publiek. |
| FFA | Proximity. | Tribune, ook Clown. |
| Kroning | Proximity in de Arena, tribunes vol. | – |

## Wat de mod doet

Niks. Simple Voice Chat draait naast de bootcamp-mod en die heeft de voice-API niet nodig.

## Server-config

`config/voicechat/voicechat-server.properties`:

```
port=24454
max_voice_distance=48
enable_groups=false
force_voice_chat=true
```

- `enable_groups=false`: geen groepen, dus alles is altijd proximity.
- `force_voice_chat=true` kickt iedereen die de mod niet heeft. Dan weet je bij het joinen meteen
  wie er nog moet installeren.
- **UDP-poort 24454** moet open staan naast de normale TCP-poort. Bij een hoster moet je die vaak
  apart aanvragen. Dit is de nummer-één oorzaak van "ik hoor niks".
- De client- en servermod moeten dezelfde hoofdversie hebben.

## Praktisch

- **Voice-test om 19:40** in het basiskamp: iedereen zegt wat, loopt een stuk weg en terug.
- **Streams:** de mod is gewoon game-audio, dus proximity komt vanzelf op de stream.
- **Host:** praat op de eigen stream en zit meestal niet in de spelersvoice.
- **Quiz:** Pudding presenteert als gewone speler vanaf het podium en leest de vragen voor via
  proximity. Test in de testrun of Pudding bij alle vier de banken goed te horen is. Anders de
  addon *Voice Chat Broadcast* (ops kunnen met een toets naar iedereen omroepen) als die er is
  voor 26.2.
- **Tribune:** de doden mogen alles roepen. Niet streamsnipen blijft de regel.
