# De kroon: regels van Clown vs All

In beeld heet de ronde **King of the Hill**. De spelers zien nergens "Clown vs All" (bossbar,
titles, chat), zodat niemand op het idee komt dat het rad rigged is.

Ronde 5. De kroon is een diamond helm met dezelfde enchants als de kit plus Curse of Binding (kan
niet af), met daarboven een zwevende gouden kroon en het Glowing-effect.
Wie de kroon heeft is de **kroonhouder** en is voor iedereen zichtbaar door alles heen. De rest
zijn **jagers**. Alles speelt in de Arena: een colosseum met tribunes waar wie af is op komt.

## Start: Het Rad

1. Iedereen staat op de tribune van de Arena (de onderste ring). De teams van ronde 1 t/m 4 zijn
   weg; vanaf nu speelt iedereen solo, ook Clown. De host legt de regels uit en sluit af met:
   "Wie de kroon krijgt? Iedereen kan het zijn. Het lot beslist."
2. Op het sein van Pudding start de commander het rad. Het rad staat **in beeld** als een rij
   **spelerskoppen met namen**: groot in het midden wie onder het pijltje staat, eronder de rij
   die langs het pijltje schuift. Twee of drie keer rond, steeds langzamer, en het stopt op
   ClownPierce. Geluid, particles, `title` voor iedereen: **DE KROON** met de kop en de naam van
   ClownPierce.
3. Het rad is rigged. Clown heeft vooraf de verborgen rol `uitverkoren` gekregen en het rad landt
   altijd op de speler met die rol. De volgorde van de namen, de startpositie en het aantal
   rondes zijn wel echt willekeurig, zodat het er elke keer anders uitziet. **Alleen de admins
   weten dit**, en dat blijft zo. Valt Clown uit, dan verhuis je de rol en landt het rad op iemand
   anders.
4. Drie seconden later gaat iedereen de vloer op. Clown gaat naar het **podium in het midden** en
   krijgt de kroon, de kit en Glowing.
5. Iedereen heeft na de mob arena al alles ingeleverd. **Iedereen krijgt dezelfde kit**, jagers en
   Clown: volledig diamond armor (Protection IV), diamond sword en axe (Sharpness V), bow (Power V)
   met 32 pijlen, schild en 10 golden apples, alles met Unbreaking III. Bij Clown is de helm de
   kroon. De jagers worden **willekeurig verdeeld over de 20 startplekken** in de cirkel (één
   blijft leeg), elk met de kijkrichting van zijn plek.
6. **Iedereen staat stil**, ook Clown: rondkijken en je inventory sorteren kan; lopen, springen,
   schieten en pearlen niet. Pas als de commander **`/clown go`** doet, telt het 10 seconden af
   (de laatste 5 groot in beeld) en dan gaat iedereen tegelijk los. Er is geen voorsprong.

## Wie kan wie raken

**Friendly fire staat uit.** Alleen gevechten met de kroonhouder tellen:

| Aanvaller | Slachtoffer | Schade? |
|---|---|---|
| Kroonhouder | Jager | Ja |
| Jager | Kroonhouder | Ja |
| Jager | Jager | **Nee**, ook niet met pijlen |

Jagers kunnen elkaar dus niet in de weg zitten met zwaard of boog. Samenwerken is de enige optie.

## Strength

- **De kroonhouder heeft Strength II**, de jagers niks. Het effect gaat mee met de kroon: bij een
  wissel verliest de oude kroonhouder het en krijgt de nieuwe het.
- **Vanaf de 1v1v1** (nog 3 over, de kroonhouder meegeteld) krijgt **iedereen Strength I**, ook de
  kroonhouder. De kroonhouder gaat dus van II naar I en de jagers van niks naar I. Zo blijft het
  tot het einde, ook als de kroon daarna nog wisselt.
- De mod zet het elke seconde goed, dus melk drinken verandert er niks aan. Na de ronde gaat
  de Strength eraf.

## Eén leven

- Ga je dood, door de kroonhouder of door een val, dan ben je **af** en ga je de tribune op. Geen
  respawns.
- Wie doodgaat ziet groot in beeld een van de doodteksten: **Grote L gepakt!**, **Had je nou maar
  beter je best gedaan**, **Gelukkig is dit niet de CSMP**. Willekeurig, alleen voor de dode
  zelf. De rest ziet een chatregel: "Speler3 is af door ClownPierce · 11 over".

## De kroon wisselt: elke wissel is een reset

**Wie de kroonhouder killt, krijgt de kroon.** Dan begint de jacht opnieuw:

- De oude kroonhouder is af en gaat de tribune op.
- **Alle levende jagers** worden full hp geheald en opnieuw willekeurig verdeeld over de
  startplekken in de cirkel, waar ze bevroren staan. Wie af is, blijft af.
- **De nieuwe kroonhouder** gaat naar het podium in het midden, ook bevroren, en:
  - wordt full hp geheald, honger vol,
  - krijgt al zijn armor en wapens gerepareerd,
  - krijgt het kroonpakketje: 2 gapples + 2 ender pearls,
  - krijgt Glowing en Strength II (vanaf de 1v1v1 Strength I zoals iedereen), geen Resistance of
    ander extra effect,
  - krijgt de kroon als helm; de oude helm gaat naar de inventory.
- Iedereen krijgt een `title`: **NIEUWE KROON** met de kop en de naam. Bossbar update.
- **10 seconden** countdown, die start vanzelf (geen `/clown go` nodig). Dan is iedereen los.

**De kroonhouder gaat dood zonder killer** (val, disconnect):

- Precies dezelfde reset. De kroon gaat naar de jager die de kroonhouder als laatste raakte, en
  anders naar een willekeurige levende jager. De ronde zit nooit zonder kroonhouder.
- Bij een disconnect telt de mod 30 seconden af in de bossbar. Komt de speler niet terug, dan
  gaat de kroon door op dezelfde manier. Komt die later terug, dan als kijker op de tribune.

## Einde

- **Geen timer.** De ronde loopt tot er nog maar één speler leeft.
- Dat is meestal de kroonhouder die de laatste jager killt. Het kan ook een jager zijn: killt de
  laatste jager de kroonhouder, dan is die jager alleen over en wint.
- Winnaar: `title` voor iedereen met de kop van de winnaar, vuurpijl erboven:
  **SPELER7 WINT KING OF THE HILL**. Voor Clown precies hetzelfde, zodat niks verraadt dat hij
  moest winnen.
- Daarna de FFA: **iedereen behalve Clown**, ook wie af was en ook de winnaar.

## Randgevallen

| Situatie | Wat gebeurt er |
|---|---|
| Twee jagers raken de kroonhouder tegelijk | Wie de laatste klap geeft krijgt de kroon. Het spel bepaalt dat. |
| Jager schiet een pijl op een andere jager | Geen schade. |
| Er zijn nog twee over: kroonhouder en één jager | Doorspelen. Wie wint is de winnaar. |
| Het valt stil: de kroonhouder verstopt zich of niemand durft | Glowing en de locator bar wijzen de kroonhouder aan. Blijft het hangen, dan laat de commander de border krimpen met `/clown krimp <grootte>`; iedereen ziet dan **DE BORDER KRIMPT**. |
| De kroonhouder logt uit | 30 seconden wachten, dan gaat de kroon door (laatste hit, anders willekeurig). |
| Een jager logt uit | Af. Komt die terug, dan als kijker op de tribune. |
| Clown is er niet of valt uit vóór ronde 5 | Geef de rol `uitverkoren` aan iemand anders. Het rad landt dan op die speler. |
| Het rad stopt op de verkeerde naam | De rol `uitverkoren` staat bij de verkeerde speler. Host: "technische storing", ref zet het recht met `/clown uitverkoren`, rad nog een keer. In het uiterste geval de kroon met de hand geven. |
| Iemand beweegt of schiet voor de countdown | Kan niet: tot het einde van de countdown staat iedereen bevroren en blokkeert de mod bogen, pearls en andere items die je laten bewegen of schieten. |
| Iemand met de kroon wordt kijker door een bug | Admin geeft de kroon met `/clown kroon <speler>`. |
| Alle jagers vallen van een hoogte tegelijk dood | Dan is de kroonhouder als enige over en wint. |

## Display voor de stream

- **Bossbar:** `King of the Hill · Kroon: <naam> · 12 over`.
- **Sidebar:** de regeerperiodes: `Clown 4:12 · Speler X 0:38`. Leuk als eretitel achteraf.
- **Tab-list:** kroonhouder in goud, jagers in aqua, wie af is in grijs.
- **Locator bar:** aan, alleen de kroonhouder is zichtbaar.
