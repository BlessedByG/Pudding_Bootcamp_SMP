# Open keuzes

Dingen die je nog moet beslissen. Per keuze: wat de opties zijn, wat er nu in het concept staat
en wat ik zou doen.

## 1. Liggen ex-koningen uit het hele event?

**Nu in het concept:** ja. Wie de kroon had en doodgaat, ligt eruit, ook voor de FFA. Dat volgt
het idee van 19v1, 18v1, 17v1: elke kroonwissel haalt iemand uit het spel.

**Het probleem:** dit straft precies het spannendste moment van de avond. Wie Clown killt, heeft
de kroon en 18 man achter zich aan. Grote kans dat die binnen een minuut dood is en dan 40
minuten als kijker rondzweeft. Slimme spelers gaan de kroon daardoor juist vermijden tot de laatste
minuten. Met 19 streamers die niet samen kunnen overleggen zal het in de praktijk toch chaos
worden, maar het is een rare prikkel.

**Variant:** ex-koningen zijn de rest van ronde 4 kijker (dus het blijft 18v1, 17v1),
maar doen wel weer mee aan de FFA. Killen van de koning is dan puur winst: highlight plus een
kans op de kroon, en als het misgaat zit je hooguit een paar minuten uit. Alle streamers blijven
tot de FFA actief. Nadeel: de FFA wordt groter (tot 19 man) en er is minder eliminatie-spanning
in ronde 4.

**Aanbeveling:** de variant. Voor een streamer-event is "iedereen blijft lang meedoen" meer
waard dan de strakke eliminatieladder. Wil je toch de harde versie, laat het dan zoals het staat.

## 2. Gaat de kroon naar de killer of naar "de volgende"?

**Nu in het concept:** naar de killer.

**Alternatief:** naar een random hunter, of naar de hunter die het langst leeft. Dat maakt het
minder voorspelbaar, maar ook minder verdiend.

**Aanbeveling:** killer. "Kill the king, become the king" snapt iedereen en het is het beste
streammoment.

## 3. Kunnen hunters elkaar raken in ronde 4?

**Nu in het concept:** nee, hunters zitten in één team.

**Alternatief:** friendly fire aan. Dan wordt het 19 man tegen elkaar met een koning ertussen,
en kunnen spelers elkaar de kill afpakken. Meer chaos, minder focus op de koning.

**Aanbeveling:** uit. Het is King of the SMP, niet Battle Royale. De FFA erna is al de
iedereen-tegen-iedereen ronde.

## 4. Eigen gear of standaardkit in de FFA en de finale?

**Nu in het concept:** standaardkit. Je eigen loot uit het Ei gebruik je in ronde 4, daarna
krijgt iedereen hetzelfde.

**Alternatief:** eigen gear meenemen. Dan loont het Ei nog meer, maar na een kwartier 19v1 zijn
de verschillen enorm (iemand heeft niks meer, iemand heeft 6 gapples).

**Aanbeveling:** standaardkit. Eerlijke finale, en een 1v1 met gelijke kits is spannender dan
een die op de kist van ronde 3 beslist wordt.

## 5. Speelt Clown de vroege rondes mee?

**Nu in het concept:** ja, hij doet ronde 1 t/m 3 gewoon mee en krijgt in ronde 4 de kroon en
de bosskit.

**Alternatief:** Clown verschijnt pas in ronde 4 als "de eindbaas". Meer entree, minder content.

**Aanbeveling:** meespelen. Hij sloopt de horde, mensen zien hem in het doolhof dwalen, en het
bouwt op naar ronde 4. De bosskit maakt hem in ronde 4 alsnog de baas.

## 6. Sudden death en border-shrink in ronde 4?

**Nu in het concept:** ja, laatste 3 minuten geen respawns, border naar 60 x 60.

**Alternatief:** geen sudden death, de koning mag zich verstoppen tot de timer afloopt.

**Aanbeveling:** houden. Zonder sudden death eindigt de ronde met een koning die ergens in een
gat zit en 19 man die zoeken. Met sudden death eindigt hij met een gevecht.

## 7. PvP-twist na de horde?

**Nu in het concept:** optioneel, standaard uit. Na wave 5 zou 60 seconden PvP aan kunnen.

**Aanbeveling:** uit laten, tenzij de groep elkaar goed kent. Ronde 2 is de samenwerkronde;
elkaar in de rug steken hoort in ronde 5.

## 8. Wat wint de winnaar?

Opties, kunnen ook gecombineerd:

- De kroon (de helm) dragen op de echte SMP, eerste week.
- Als eerste een plek kiezen voor je base op de SMP.
- Een "bounty": wie de King of the SMP in de eerste week op de SMP killt, krijgt de kroon.
- Iets fysieks: een puddinkje.

**Aanbeveling:** kroon dragen plus de bounty. Dat trekt de bootcamp-verhaallijn de SMP in.

## 9. Datapack, plugin of eigen mod?

**Besloten:** een eigen server-side Fabric-mod op Minecraft 26.2, gevibecode met Claude Code.
Regio's met een wand, teleportpunten met een command, en alles wat de laatste wensen lastig
maakten in Skript (automatische voice, bevriezen, kijkers met items) zit rechtstreeks in de mod.
Spelers hebben alleen de voice-mod nodig. Zie [04-technische-schets.md](04-technische-schets.md).

**Nog open:** of Fabric Loader, Fabric API en de voice-mod op tijd een 26.2-build hebben, en of
er iemand is die de dev-loop (compileren, testen, fouten terugplakken) wil draaien. De Paper +
Skript-versie (`d351fd1`) en de datapack-versie (`e51e143`) staan in de git-geschiedenis als
terugvaloptie.

## 10. Hoeveel spelers?

**Nu in het concept:** 20. Alles schaalt: diamond blocks (1,5x spelers), mobs per wave (1 per
speler), spawnpunten (4 blijft prima tot 30 man).

Onder de 10 spelers wordt ronde 4 anders: met 8 man is 7v1 goed te doen voor Clown en gaat de
kroon misschien nooit over. Dan de timer korter (10 min) en sudden death eerder (laatste 4 min).

## 11. Hoe lang is de FFA-arenakit "eerlijk" tegen finalist 1?

Finalist 1 heeft niet gevochten in de FFA en is uitgerust. Finalist 2 komt net uit een
gevecht. Daarom is de finale best of 3 met kit-reset en full heal per potje. Wil je het nog
eerlijker, geef finalist 2 dan 2 minuten pauze voor de finale. Kost bijna niks.

## 12. Weten de streamers dat Het Rad rigged is?

**Nu in het concept:** nee. Alleen de staff en Clown weten het. De host speelt het recht en het
rad ziet er elke keer anders uit (willekeurige start, willekeurig aantal rondes).

**Alternatief:** iedereen weet dat het theater is. Dan is het rad gewoon een grappige entree
voor de eindbaas en hoef je niet te liegen.

**Aanbeveling:** geheim houden tot de kroning en het dan onthullen. De chats gaan het toch
roepen ("dat was rigged!") en dat is precies de content. Bijkomend voordeel: valt Clown op de
dag zelf uit, dan verhuis je de rol `uitverkoren` en heb je zonder gedoe een andere eindbaas.

## 13. Hoe ver draagt je stem?

**Nu in het concept:** 48 blokken, de standaard van de mod. Dat geldt de hele avond, want de
afstand is een serverinstelling die je niet per ronde wisselt.

**Alternatief:** 32 blokken. Sluipen in het doolhof en de King zone wordt spannender, maar in de
horde-arena (Ø 50) hoor je elkaar dan niet meer van rand tot rand.

**Aanbeveling:** 48 laten staan. Wie stil wil zijn heeft de fluister-toets.

## 14. Lopen of teleporteren tussen de zones?

**Nu in het concept:** teleporteren naar het verzamelpunt van de volgende zone. Iedereen staat
tegelijk op de goede plek, de host heeft z'n praatje, niemand dwaalt.

**Alternatief:** lopen over gemarkeerde paden. Meer open-world-gevoel en de wereld wordt echt
gebruikt, maar het kost per overgang 3 tot 5 minuten en er is altijd iemand die de verkeerde kant
op gaat. Tussenvorm: lopen na ronde 1 en 2 (korte paden), teleporteren daarna.

**Aanbeveling:** teleporteren. De tijd gaat liever in de rondes dan in het wandelen.

## 15. King zone: 200 x 200 rond de burcht, of de hele wereld?

**Nu in het concept:** 200 x 200 rond de burcht, de rest van de wereld zit achter de border.

**Alternatief:** de hele wereld als slagveld. Het doolhof als schuilplaats, de arena als fort,
het Ei-bos als hinderlaag. Klinkt geweldig, maar 500 x 500 is met 20 man en 15 minuten te groot
om de koning te vinden, ook met Glowing. En de koning kan in survival het doolhof slopen.

**Aanbeveling:** 200 x 200 houden. Wil je toch de hele wereld, begin de border dan op 400 en laat
hem vanaf minuut 5 al krimpen.

## 16. Echte spectators, of kijkers met items in de hotbar?

**Besloten:** kijkersmodus in de mod. Doden gaan in adventure mode met fly en onzichtbaarheid,
onaantastbaar (geen schade, pijlen en klappen gaan door je heen, geen botsing, niks oppakken), met
de twee tp-items in de hotbar. De voice-regel hangt aan de dood-vlag van de mod, niet aan
spectator mode.

**Nadeel:** kijkers vliegen niet door muren; dat is client-side en zou een client-mod vragen.
Eroverheen kan wel.

**Alternatief:** echte spectator mode. Wel door muren, geen items; teleporteren gaat dan via
het ingebouwde spectator-menu. Staff kan dat altijd nog kiezen, de mod dwingt het niet af.
