# De kroon: regels van Clown vs All

Ronde 5. De kroon is een gouden helm met Curse of Binding (kan niet af) plus het Glowing-effect.
Wie de kroon heeft is de **kroonhouder** en is voor iedereen zichtbaar door alles heen. De rest
zijn **jagers**. Alles speelt in de Arena: een colosseum met tribunes waar wie af is op komt.

## Start: Het Rad

1. Iedereen staat op de vloer van de Arena. De teams van ronde 1 t/m 4 zijn weg; vanaf nu speelt
   iedereen solo, ook Clown. De host legt de regels uit en sluit af met: "Wie de kroon krijgt?
   Iedereen kan het zijn. Het lot beslist."
2. Op het sein van Pudding start de commander het rad. Het lampje loopt langs de 20 pilaren rond
   de vloer, twee of drie rondes, wordt langzamer, en stopt op ClownPierce. Geluid, particles,
   `title` voor iedereen: **DE KROON: CLOWNPIERCE**.
3. Het rad is rigged. Clown heeft vooraf de verborgen rol `uitverkoren` gekregen en het rad landt
   altijd op de speler met die rol. De startpositie en het aantal rondes zijn wel echt
   willekeurig, zodat het er elke keer anders uitziet. **Alleen de admins weten dit**, en dat
   blijft zo. Valt Clown uit, dan verhuis je de rol en landt het rad op iemand anders.
4. Drie seconden later gaat Clown naar het verhoogde midden van de Arena en krijgt de kroon, de
   bosskit en Glowing.
5. Alle gear van ronde 1 t/m 4 gaat weg. De jagers krijgen allemaal dezelfde **jagerskit** en
   worden verdeeld over 4 startpunten aan de rand van de vloer.
6. **30 seconden voorsprong** voor Clown: de jagers staan bevroren op hun startpunt, met een
   countdown in beeld. Rondkijken en inventory sorteren kan, lopen, springen of pearlen niet.
   Dan zijn ze los.

## Wie kan wie raken

**Friendly fire staat uit.** Alleen gevechten met de kroonhouder tellen:

| Aanvaller | Slachtoffer | Schade? |
|---|---|---|
| Kroonhouder | Jager | Ja |
| Jager | Kroonhouder | Ja |
| Jager | Jager | **Nee**, ook niet met pijlen |

Jagers kunnen elkaar dus niet in de weg zitten met zwaard of boog. Samenwerken is de enige optie.

## Eén leven

- Ga je dood, door de kroonhouder of door een val, dan ben je **af** en ga je de tribune op. Geen
  respawns.
- Wie doodgaat ziet groot in beeld een van de doodteksten: **Grote L gepakt!**, **Had je nou maar
  beter je best gedaan**, **Gelukkig is dit niet de CSMP**. Willekeurig, alleen voor de dode
  zelf. Geen chatregel.

## De kroon wisselt: elke wissel is een reset

**Wie de kroonhouder killt, krijgt de kroon.** Dan begint de jacht opnieuw:

- De oude kroonhouder is af en gaat de tribune op.
- **Alle levende jagers** worden full hp geheald en teruggezet op hun startpunt aan de rand, waar
  ze bevroren staan. Wie af is, blijft af.
- **De nieuwe kroonhouder** gaat naar het midden en:
  - wordt full hp geheald, honger vol,
  - krijgt al zijn armor en wapens gerepareerd,
  - krijgt het kroonpakketje: 2 gapples + 2 ender pearls,
  - krijgt 15 seconden Resistance II en Glowing,
  - krijgt de kroon als helm; de oude helm gaat naar de inventory.
- Iedereen krijgt een `title`: **NIEUWE KROON: <naam>**. Bossbar update.
- **10 seconden** countdown, dan zijn de jagers los.

**De kroonhouder gaat dood zonder killer** (val, disconnect):

- Precies dezelfde reset. De kroon gaat naar de jager die de kroonhouder als laatste raakte, en
  anders naar een willekeurige levende jager. De ronde zit nooit zonder kroonhouder.
- Bij een disconnect telt de mod 30 seconden af in de bossbar. Komt de speler niet terug, dan
  gaat de kroon door op dezelfde manier. Komt die later terug, dan als kijker op de tribune.

## Einde

- **Geen timer.** De ronde loopt tot er nog maar één speler leeft.
- Dat is meestal de kroonhouder die de laatste jager killt. Het kan ook een jager zijn: killt de
  laatste jager de kroonhouder, dan is die jager alleen over en wint.
- Winnaar: `title` voor iedereen, vuurpijl erboven. Is het Clown, dan heeft de eindbaas gewonnen.
- Daarna de FFA: **iedereen behalve Clown**, ook wie af was en ook de winnaar.

## Randgevallen

| Situatie | Wat gebeurt er |
|---|---|
| Twee jagers raken de kroonhouder tegelijk | Wie de laatste klap geeft krijgt de kroon. Het spel bepaalt dat. |
| Jager schiet een pijl op een andere jager | Geen schade. |
| Er zijn nog twee over: kroonhouder en één jager | Doorspelen. Wie wint is de winnaar. |
| Het valt stil: de kroonhouder verstopt zich of niemand durft | Glowing en de locator bar wijzen de kroonhouder aan. Blijft het hangen, dan laat de commander de border krimpen met `/bc krimp <grootte>`. |
| De kroonhouder logt uit | 30 seconden wachten, dan gaat de kroon door (laatste hit, anders willekeurig). |
| Een jager logt uit | Af. Komt die terug, dan als kijker op de tribune. |
| Clown is er niet of valt uit vóór ronde 5 | Geef de rol `uitverkoren` aan iemand anders. Het rad landt dan op die speler. |
| Het rad stopt op de verkeerde kop | De slot van die speler klopt niet met de plek van de kop. Host: "technische storing", ref fixt de slot, rad nog een keer. In het uiterste geval de kroon met de hand geven. |
| Iemand met de kroon wordt kijker door een bug | Admin geeft de kroon met `/bc kroon <speler>`. |
| Alle jagers vallen van een hoogte tegelijk dood | Dan is de kroonhouder als enige over en wint. |

## Display voor de stream

- **Bossbar:** `Clown vs All · Kroon: <naam> · 12 over`.
- **Sidebar:** de regeerperiodes: `Clown 4:12 · Speler X 0:38`. Leuk als eretitel achteraf.
- **Tab-list:** kroonhouder in goud, jagers in aqua, wie af is in grijs.
- **Locator bar:** aan, alleen de kroonhouder is zichtbaar.
