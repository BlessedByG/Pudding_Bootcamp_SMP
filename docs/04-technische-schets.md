# Technische schets: Paper + Skript op Minecraft 26.2

Hoe je dit bouwt: een Paper-server op 26.2 met Skript voor alle spellogica en de setup-tools,
WorldEdit voor het bouwen en Simple Voice Chat als plugin. Spelers hebben alleen de voice-mod
nodig, verder een gewone client.

Het principe: Skript doet events, variabelen en timers. Alles wat de speler ziet (bossbar, titles,
particles, worldborder, fill) gaat via gewone Minecraft-commands die Skript als console uitvoert.
Zo blijft de syntax van de effecten hetzelfde als in een command block en hoef je alleen de
Skript-lijm te leren.

> **Versie.** Doel is Minecraft 26.2. Ik kan niet controleren of Paper, Skript, SkBee, WorldEdit
> en Simple Voice Chat op het moment van bouwen al een 26.2-build hebben; plugins lopen vaak
> dagen tot weken achter op een nieuwe versie. Check dat vóór je begint. Zo niet: bouw op de
> nieuwste versie die alles ondersteunt en update later. Alle commands hieronder zijn syntax van
> 1.21.5 en nieuwer en horen op 26.2 te werken, maar zijn **niet getest**. De Skript-regels zijn
> schetsen; controleer ze tegen de Skript-docs van jullie versie.

## Stack

| Wat | Waarvoor |
|---|---|
| Paper 26.2 | De server. Draait met gewone clients. |
| Skript + SkBee | Alle spellogica en de setup-commands. SkBee voor NBT en wat extra's. |
| WorldEdit of FAWE | Bouwen: doolhof plaatsen, Ei kopiëren, terrein. Niet voor de spellogica. |
| Simple Voice Chat (plugin) | Voice, zie [07-voice.md](07-voice.md). |
| Optioneel: WorldGuard | Builds beschermen buiten de rondes. |
| Optioneel: LuckPerms | Voice-groepen maken beperken tot staff. |
| Optioneel: een mini-datapack | Alleen voor dialogs (zie Visuals). |

## Server

- `view-distance=8`, `simulation-distance=6`, whitelist aan, `pvp=true` (PvP regelen we via teams).
- UDP-poort 24454 open voor de voice-plugin.
- Gamerules bij de start:

```
gamerule keepInventory true
gamerule doImmediateRespawn true
gamerule naturalRegeneration true
gamerule doMobSpawning false
gamerule doDaylightCycle false
gamerule announceAdvancements false
gamerule spectatorsGenerateChunks false
gamerule locatorBar false
```

`doImmediateRespawn` staat aan zodat Skript de respawn-vertraging zelf regelt. `locatorBar` (de
balk die spelers als stipjes laat zien) staat standaard uit, anders loop je in het Ei-bos gewoon
achter de rest aan; in ronde 4 gaat hij aan voor alleen de koning, zie Visuals.

**Teams** zijn de PvP-schakelaar (friendly fire uit betekent dat teamgenoten elkaar niet raken):

```
team add spelers
team modify spelers friendlyFire false
team add hunters
team modify hunters friendlyFire false
team modify hunters color aqua
team add king
team modify king color gold
team add out
team modify out color gray
```

Ronde 0 t/m 3: iedereen in `spelers`. Ronde 4: `hunters` + `king`. Ronde 5 en 6: iedereen uit
zijn team behalve de koningen (`king` heeft friendly fire aan, dus die kunnen elkaar in de finale
raken). De teamkleur is ook de kleur van de Glowing-outline, de naam in de tab-list en de stip op
de locator bar.

**Tags** op spelers, gezet door Skript:

| Tag | Betekenis |
|---|---|
| `speler` | Doet mee. |
| `uitverkoren` | De verborgen rol: op deze speler landt Het Rad. |
| `hunter`, `king`, `ffa`, `finalist` | Rol in ronde 4 t/m 6. |
| `dood` | Ronde 2: gesneuveld, spectator tot het einde van de ronde. |
| `ticket` | Ronde 3: diamond block ingeleverd. |
| `out` | Uitgeschakeld. |

**Scoreboard:** `scoreboard objectives add reign dummy` met `setdisplay sidebar reign` voor de
regeerperiodes. **Bossbar:** `bossbar add bootcamp:main "Pudding Bootcamp"` en
`bossbar set bootcamp:main players @a`.

## Setup-tools: regio's en punten

Dit is wat je in de wereld zelf doet, één keer, na het bouwen. Alles wordt opgeslagen in
Skript-variabelen (die overleven een restart) en de spellogica leest ze uit. Geen coördinaten in
bestanden.

**Regio's** zet je met een wand: `/bcwand` geeft een stick. Linksklik op een blok is hoek 1,
rechtsklik is hoek 2, dan `/bcregion save <naam>`. `/bcregion show <naam>` tekent tien seconden
particles op de randen zodat je ziet wat je hebt, `/bcregion list` en `/bcregion del <naam>`
spreken voor zich.

**Punten** zet je met commands: ga staan waar je wilt (kijkrichting telt mee) en typ
`/bcpoint set <naam>`. Voor een blok in plaats van een positie: kijk ernaar en typ
`/bcpoint block <naam>`. `/bcpoint tp <naam>` om te testen, `/bcpoint list` voor het overzicht.

Wat je nodig hebt:

| Regio's | Waarvoor |
|---|---|
| `doolhof`, `arena`, `eibos`, `king`, `binnenplaats`, `troonzaal` | Worldborder per ronde (center en grootte worden uit de regio berekend). |
| `poort_doolhof`, `poort_arena`, `poort_bos` | De muur die open en dicht gaat (`fill`). |
| `hek_1` t/m `hek_4` | De hekjes bij de hunterspawns (voorsprong van 30 seconden). |
| `eiplaat` | Het vak bij de uitgang van het bos waar je ticket wordt ingenomen. |

| Punten | Waarvoor |
|---|---|
| `basiskamp`, `v2`, `v3`, `kring` | Verzamelpunten. |
| `doolhof_start`, `doolhof_uit` | Ingang en waar je uitkomt. |
| `mob_1` t/m `mob_4` | Spawnpunten van de horde. |
| `arena_spawn` | Waar spelers de arena binnenkomen. |
| `ei_start`, `ei_beacon` (blok) | Bosrand-ingang en het ontbrekende blok in de beaconpiramide onder het Ei. |
| `burcht`, `hunter_1` t/m `hunter_4` | Startpunten ronde 4. |
| `ffa_midden`, `finale_1`, `finale_2`, `kroning` | Binnenplaats en troonzaal. |
| `lamp_0` t/m `lamp_19` (blokken) | De lichtblokken van De Kring, met de klok mee. |

De Skript-kant van de tools:

```
command /bcwand:
    permission: bc.admin
    trigger:
        give 1 stick named "&6Regio-wand" to player
        send "&7Linksklik = hoek 1, rechtsklik = hoek 2, dan /bcregion save <naam>"

on leftclick on block:
    player is holding a stick named "&6Regio-wand"
    cancel event
    set {bc::sel::%uuid of player%::a} to location of clicked block
    send "&7Hoek 1: &f%location of clicked block%"

on rightclick on block:
    player is holding a stick named "&6Regio-wand"
    cancel event
    set {bc::sel::%uuid of player%::b} to location of clicked block
    send "&7Hoek 2: &f%location of clicked block%"

command /bcregion <text> [<text>]:
    permission: bc.admin
    trigger:
        if arg-1 is "save":
            set {bc::regio::%arg-2%::a} to {bc::sel::%uuid of player%::a}
            set {bc::regio::%arg-2%::b} to {bc::sel::%uuid of player%::b}
            send "&aRegio %arg-2% opgeslagen."
        else if arg-1 is "show":
            bcToonRegio(arg-2)
        else if arg-1 is "list":
            loop {bc::regio::*}:
                send "%loop-index%"
        else if arg-1 is "del":
            delete {bc::regio::%arg-2%::*}

command /bcpoint <text> [<text>]:
    permission: bc.admin
    trigger:
        if arg-1 is "set":
            set {bc::punt::%arg-2%} to location of player
            send "&aPunt %arg-2%: %location of player%"
        else if arg-1 is "block":
            set {bc::punt::%arg-2%} to location of target block
            send "&aPunt %arg-2%: %location of target block%"
        else if arg-1 is "tp":
            teleport player to {bc::punt::%arg-2%}
        else if arg-1 is "list":
            loop {bc::punt::*}:
                send "%loop-index%: %loop-value%"
        else if arg-1 is "del":
            delete {bc::punt::%arg-2%}
```

`bcToonRegio` tekent met `particle minecraft:end_rod` vier lijnen langs de randen op ooghoogte
(één `particle`-command per rand, met de spreiding `dx`/`dz` gelijk aan de halve lengte) en de
vier hoekpilaren. Tien keer, elke seconde.

## Spellogica

Eén bestand, `plugins/Skript/scripts/bootcamp.sk`, in blokken: helpers, admin-commands, per
ronde een start en een einde, de tick-loop, de events (dood, schade), het rad, visuals.

**Admin-commands**

| Command | Doet |
|---|---|
| `/bc start <ronde>` | Teleport naar het verzamelpunt, border, kits, voice-knop, countdown, poort open, timer. |
| `/bc stop` | Timer stil. |
| `/bc timer <seconden>` | Resterende tijd bijstellen. |
| `/bc poort <naam> open` / `dicht` | Handmatig een poort bedienen. |
| `/bc kroon <speler>` | Kroon handmatig geven (de ref z'n noodknop). |
| `/bc reset` | Alles terug naar de basiskamp-staat: tags weg, team `spelers`, adventure, inventory leeg, tp basiskamp, border groot, bossbar leeg. |
| `/bcuitverkoren <speler>` | De verborgen rol. Vooraf op Clown. |
| `/bcslot <speler> <0-19>` | Welke pilaar van De Kring de kop van die speler heeft. |
| `/bcrad` | Het Rad. |

**Helpers**

```
function bcSpelers() :: players:
    return all players where [scoreboard tags of input contains "speler"]

function bcMetTag(tag: text) :: players:
    return all players where [scoreboard tags of input contains {_tag}]

function bcBorder(naam: text):
    set {_a} to {bc::regio::%{_naam}%::a}
    set {_b} to {bc::regio::%{_naam}%::b}
    set {_cx} to (x-coord of {_a} + x-coord of {_b}) / 2 + 0.5
    set {_cz} to (z-coord of {_a} + z-coord of {_b}) / 2 + 0.5
    set {_dx} to abs(x-coord of {_a} - x-coord of {_b}) + 1
    set {_dz} to abs(z-coord of {_a} - z-coord of {_b}) + 1
    execute console command "worldborder center %{_cx}% %{_cz}%"
    execute console command "worldborder set %max({_dx}, {_dz})%"
    execute console command "worldborder warning distance 5"

function bcPoort(naam: text, open: boolean):
    set {_a} to {bc::regio::%{_naam}%::a}
    set {_b} to {bc::regio::%{_naam}%::b}
    set {_blok} to "minecraft:iron_bars"
    if {_open} is true:
        set {_blok} to "minecraft:air"
    execute console command "fill %x-coord of {_a}% %y-coord of {_a}% %z-coord of {_a}% %x-coord of {_b}% %y-coord of {_b}% %z-coord of {_b}% %{_blok}%"

function bcInRegio(loc: location, naam: text) :: boolean:
    set {_a} to {bc::regio::%{_naam}%::a}
    set {_b} to {bc::regio::%{_naam}%::b}
    x-coord of {_loc} is between min(x-coord of {_a}, x-coord of {_b}) and max(x-coord of {_a}, x-coord of {_b}) + 1
    y-coord of {_loc} is between min(y-coord of {_a}, y-coord of {_b}) and max(y-coord of {_a}, y-coord of {_b}) + 1
    z-coord of {_loc} is between min(z-coord of {_a}, z-coord of {_b}) and max(z-coord of {_a}, z-coord of {_b}) + 1
    return true

function bcVoice(soort: text, doel: players):
    if {_soort} is "doden":
        set {_cmd} to "/voicechat join Doden"
        set {_label} to "[DODEN]"
        set {_kleur} to "red"
    else if {_soort} is "ei":
        set {_cmd} to "/voicechat join Ei"
        set {_label} to "[EI-VOICE]"
        set {_kleur} to "green"
    else:
        set {_cmd} to "/voicechat leave"
        set {_label} to "[VERLATEN]"
        set {_kleur} to "aqua"
    loop {_doel::*}:
        execute console command "tellraw %loop-value% [{""text"":""Voice: "",""color"":""gray""},{""text"":""%{_label}%"",""color"":""%{_kleur}%"",""bold"":true,""click_event"":{""action"":""run_command"",""command"":""%{_cmd}%""}}]"

function bcBossbar(tekst: text, kleur: text, max: integer):
    execute console command "bossbar set bootcamp:main name {""text"":""%{_tekst}%""}"
    execute console command "bossbar set bootcamp:main color %{_kleur}%"
    execute console command "bossbar set bootcamp:main max %{_max}%"
    execute console command "bossbar set bootcamp:main value %{_max}%"
```

Doet de voice-knop niks, probeer dan het command zonder de schuine streep. Wie zonder Skript
wil: dezelfde `tellraw` werkt ook vanuit een command block.

**Een ronde starten** (ronde 3 als voorbeeld; de andere volgen hetzelfde patroon)

```
function bcStart(ronde: integer):
    set {bc::ronde} to {_ronde}
    if {_ronde} is 3:
        teleport bcSpelers() to {bc::punt::ei_start}
        execute console command "gamemode survival @a[tag=speler]"
        bcBorder("eibos")
        bcKit("ei")
        bcVoice("ei", bcSpelers())
        bcBossbar("Het Ei", "green", 600)
        bcCountdown()
        bcPoort("poort_bos", true)
        set {bc::timer} to 600
```

`bcCountdown` doet vijf titles met een stijgende `note_block.pling`, dan "GO" met de raid horn,
en duurt vijf seconden (Skript `wait`). `bcKit` is een lijstje `clear`, `item replace` en `give`
commands per kitnaam (basis, horde, ei, boss, arena, finale), bijvoorbeeld:

```
execute console command "item replace entity %{_p}% armor.chest with minecraft:diamond_chestplate[minecraft:enchantments={""minecraft:protection"":1}]"
execute console command "give %{_p}% minecraft:diamond_sword[minecraft:enchantments={""minecraft:sharpness"":1}]"
```

**De tick-loop**

```
every second:
    {bc::timer} is set
    remove 1 from {bc::timer}
    set {_m} to floor({bc::timer} / 60)
    set {_s} to mod({bc::timer}, 60)
    if {_s} < 10:
        set {_s} to "0%{_s}%"
    bcBossbarTekst("%{_m}%:%{_s}%")
    execute console command "bossbar set bootcamp:main value %{bc::timer}%"
    if {bc::ronde} is 3:
        if {bc::timer} is 300:
            bcEiHint()
    if {bc::ronde} is 4:
        add 1 to score of "reign" for bcMetTag("king")
        if {bc::timer} is 180:
            bcSuddenDeath()
    if {bc::timer} is 0:
        delete {bc::timer}
        bcEinde({bc::ronde})
```

`bcBossbarTekst` plakt de tijd achter de rondetekst (en in ronde 4 de naam van de koning), zie
Visuals. `bcSuddenDeath` zet `{bc::sudden}` op true, laat de border krimpen met
`worldborder set 60 180` en doet de visuals. `bcEinde(4)` tagt de koning `finalist`, de levende
hunters `ffa`, en start ronde 5; `bcEinde(1)`, `(2)` en `(3)` teleporteren de achterblijvers naar
het volgende verzamelpunt.

**De Ei-drukplaat** (ticketcheck, elke halve seconde):

```
every 10 ticks:
    {bc::ronde} is 3
    loop bcSpelers():
        scoreboard tags of loop-player doesn't contain "ticket"
        bcInRegio(location of loop-player, "eiplaat") is true
        loop-player's inventory contains diamond block
        remove 1 diamond block from loop-player's inventory
        add "ticket" to scoreboard tags of loop-player
        teleport loop-player to {bc::punt::kring}
        execute console command "playsound minecraft:entity.player.levelup master %loop-player%"
```

Na de timer: wie geen `ticket` heeft krijgt `clear` plus de basiskit en gaat alsnog naar De Kring.

**Dood en schade** (het hart van ronde 2, 4 en 5)

```
on damage of player:
    scoreboard tags of victim contains "king"
    attacker is a player
    set {bc::lasthit} to attacker

on death of player:
    {bc::ronde} is set
    if {bc::ronde} is 2:
        bcDood(victim)
    else if {bc::ronde} is 4:
        if scoreboard tags of victim contains "king":
            if attacker is a player:
                set {_nieuw} to attacker
            else if {bc::lasthit} is set:
                set {_nieuw} to {bc::lasthit}
            else:
                set {_nieuw} to random element of bcMetTag("hunter")
            bcUit(victim)
            bcKroon({_nieuw})
        else if {bc::sudden} is true:
            bcUit(victim)
        else:
            bcRespawn(victim)
    else if {bc::ronde} is 5:
        bcUit(victim)
        bcCheckFFA()
    else if {bc::ronde} is 6:
        bcPotje(attacker)
```

Skript geeft bij een pijl de schutter als `attacker`, dus boogkills tellen gewoon. De
"laatste hit" voor een val- of lavadood is hier één regel, in een datapack was dat niet te doen.

```
function bcDood(p: player):
    add "dood" to scoreboard tags of {_p}
    wait 1 tick
    set gamemode of {_p} to spectator
    execute console command "tellraw @a [{""text"":""%{_p}%"",""color"":""gray""},{""text"":"" is gesneuveld"",""color"":""dark_gray""}]"
    bcVoice("doden", {_p})

function bcUit(p: player):
    add "out" to scoreboard tags of {_p}
    remove "hunter", "king" and "ffa" from scoreboard tags of {_p}
    execute console command "team join out %{_p}%"
    execute console command "kill @e[tag=kroon_%{_p}%]"
    wait 1 tick
    set gamemode of {_p} to spectator
    bcVoice("doden", {_p})

function bcRespawn(p: player):
    wait 1 tick
    set gamemode of {_p} to spectator
    loop 20 times:
        send action bar "&cRespawn in %21 - loop-number%" to {_p}
        wait 1 second
    teleport {_p} to {bc::punt::hunter_%random integer between 1 and 4%}
    set gamemode of {_p} to survival
    apply resistance of tier 5 to {_p} for 5 seconds
```

**De kroon**

```
function bcKroon(p: player):
    add "king" to scoreboard tags of {_p}
    remove "hunter" from scoreboard tags of {_p}
    execute console command "team join king %{_p}%"
    execute console command "item replace entity %{_p}% armor.head with minecraft:golden_helmet[minecraft:enchantments={""minecraft:binding_curse"":1},minecraft:unbreakable={}]"
    heal {_p}
    apply resistance of tier 2 to {_p} for 15 seconds
    apply glowing to {_p} for 1 hour
    give 2 golden apples and 2 ender pearls to {_p}
    delete {bc::lasthit}
    bcVisual("kroon", {_p})
```

De kroonwissel is dus: `bcUit(oude)` en `bcKroon(nieuwe)`, zie de death-handler. De ref z'n
noodknop `/bc kroon <speler>` roept dezelfde functie aan.

**Het Rad**

```
command /bcrad:
    permission: bc.admin
    trigger:
        set {_doel} to {bc::slot::%{bc::uitverkoren}%}
        set {_pos} to random integer between 0 and 19
        set {_rest} to mod(({_doel} - {_pos} + 20), 20) + 20 * random integer between 2 and 3
        loop 20 times:
            set block at {bc::punt::lamp_%loop-number - 1%} to black concrete
        while {_rest} > 0:
            set block at {bc::punt::lamp_%{_pos}%} to black concrete
            set {_pos} to mod({_pos} + 1, 20)
            set block at {bc::punt::lamp_%{_pos}%} to glowstone
            remove 1 from {_rest}
            execute console command "playsound minecraft:block.note_block.hat master @a"
            if {_rest} > 12:
                wait 2 ticks
            else if {_rest} > 8:
                wait 4 ticks
            else if {_rest} > 5:
                wait 7 ticks
            else if {_rest} > 3:
                wait 10 ticks
            else if {_rest} is 3:
                wait 15 ticks
            else if {_rest} is 2:
                wait 20 ticks
            else:
                wait 30 ticks
        bcRadEinde()
```

Startpositie en aantal rondes zijn willekeurig, het eindpunt is het slot van de `uitverkoren`
speler. `bcRadEinde` doet de visuals (zie hieronder), wacht drie seconden en roept `bcStart(4)`
aan: hunters getagd en in team, teleport naar `hunter_1` t/m `hunter_4` achter de hekjes, Clown
naar `burcht` met bosskit en `bcKroon`, iedereen de [VERLATEN]-knop, border `king`, locator bar
aan voor de koning, na 30 seconden de hekjes open met `bcPoort("hek_1".."hek_4", true)` en de
timer op 900.

**De horde**

```
function bcWave(n: integer):
    set {bc::wave} to {_n}
    set {bc::wavetimer} to 120
    bcVisual("wave", {_n})
    loop 4 times:
        set {_loc} to {bc::punt::mob_%loop-number%}
        loop 5 times:
            spawn a zombie at {_loc}
            add "horde" to scoreboard tags of last spawned entity
            set name of last spawned entity to "Horde"
```

Per wave een ander lijstje mobs (zie [02-rondes.md](02-rondes.md)); gear op mobs via
`set helmet of last spawned entity to iron helmet` enzovoort. Een naam geven is de simpelste
manier om te zorgen dat mobs niet despawnen. De tick-loop telt
`size of all entities where [scoreboard tags of input contains "horde"]` voor de bossbar en start
de volgende wave als dat 0 is of `{bc::wavetimer}` op 0 staat. Geen levende spelers meer (niemand
zonder tag `dood`) of wave 5 dood: `bcEinde(2)`, die de doden weer levend maakt bij `v3`.

**FFA en finale**

```
function bcCheckFFA():
    set {_levend::*} to bcMetTag("ffa")
    if size of {_levend::*} is 1:
        delete {bc::timer}
        bcKroon({_levend::1})
        bcVisual("finalist", {_levend::1})
        wait 5 seconds
        bcStart(6)
```

`bcStart(6)` zet beide koningen op adventure, geeft de finalekit, [VERLATEN]-knop, teleport naar
`finale_1` en `finale_2`, border `troonzaal`. `bcPotje(winnaar)` telt de score, healt, reset de
kits, en na twee gewonnen potjes `bcKroning(winnaar)`.

## Bossbar en visuals

Eén bossbar, kort, altijd hetzelfde formaat. Persoonlijke info (respawn-teller, "jij bent de
koning") gaat via de actionbar, de regeerperiodes via de sidebar. Een bossbar is altijd even
groot; "klein" betekent hier: één balk, korte tekst, geen tweede balk.

**De bossbar per ronde**

| Ronde | Tekst | Kleur | Vulling |
|---|---|---|---|
| Basiskamp | `Pudding Bootcamp` | wit | vol |
| 1 | `Doolhof · 09:41` | groen | tijd |
| 2 | `Wave 3 · 12 mobs` | rood | mobs over |
| 3 | `Het Ei · 07:12` | groen, geel na de hint | tijd |
| 4 | `Koning: Clown · 12:34` | geel | tijd |
| 4, sudden death | `SUDDEN DEATH · 02:59` | rood | tijd |
| 5 | `FFA · 7 over · 04:59` | paars | tijd |
| 6 | `Finale · 1 - 0` | geel | vol |

Bossbar-kleuren zijn beperkt tot blue, green, pink, purple, red, white en yellow; "goud" is dus
yellow.

**Locator bar** (de balk met spelerstipjes, sinds 1.21.6): uit in alle rondes, behalve ronde 4
t/m 6 waar alleen de koningen een stip zijn. Hunters zenden niet:

```
gamerule locatorBar true
attribute @a[tag=hunter] minecraft:waypoint_transmit_range base set 0
attribute @a[tag=king] minecraft:waypoint_transmit_range base set 60000000
```

De stip heeft de teamkleur, dus goud. Samen met Glowing weet elke hunter altijd welke kant op.

**Zweefkroon:** boven de koning draait een gouden helm. Een `item_display` met `teleport_duration`
zodat hij vloeiend meebeweegt, elke twee ticks boven het hoofd van de koning geteleporteerd en een
paar graden gedraaid:

```
function bcZweefkroon(p: player):
    execute console command "kill @e[tag=kroon_%{_p}%]"
    execute console command "execute at %{_p}% run summon minecraft:item_display ~ ~2.3 ~ {Tags:[""zweefkroon"",""kroon_%{_p}%""],item:{id:""minecraft:golden_helmet"",count:1},transformation:{translation:[0f,0f,0f],scale:[0.5f,0.5f,0.5f],left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f]},billboard:""fixed"",teleport_duration:2}"

every 2 ticks:
    add 6 to {bc::spin}
    loop bcMetTag("king"):
        execute console command "execute as @e[tag=kroon_%loop-player%,limit=1] at %loop-player% run tp @s ~ ~2.3 ~ %{bc::spin}% 0"
```

`bcUit` ruimt hem op. Twee koningen in de finale hebben elk hun eigen kroon.

**Labels in de wereld:** boven elk verzamelpunt en boven De Kring een zwevende tekst, één keer
neerzetten bij het bouwen:

```
summon minecraft:text_display ~ ~2.5 ~ {text:"VERZAMELPUNT 2",billboard:"center",background:1073741824}
```

**Visuals per moment**

| Moment | Wat je ziet en hoort |
|---|---|
| Countdown | Titles 5 t/m 1 met een stijgende `block.note_block.pling`, dan `GO` met `event.raid.horn`. |
| Poort open | `event.raid.horn` en `particle minecraft:cloud` in de poortopening. |
| Nieuwe wave | Title `WAVE 3` in rood, `event.raid.horn`, bossbar rood met mob-teller. |
| Speler sneuvelt (ronde 2) | Grijze regel in de chat, geen geluid. Het gaat snel genoeg. |
| Ei-hint op 5 min | `setblock` het ontbrekende blok in de beaconpiramide (`ei_beacon`): lichtstraal aan, `block.beacon.activate` voor iedereen, bossbar geel. Op 3 min een vuurpijl boven het Ei. |
| Ticket ingeleverd | `entity.player.levelup` voor de speler, `particle minecraft:happy_villager`. |
| Het Rad | Lampjes rond met `note_block.hat` per stap. Aan het eind `entity.ender_dragon.growl`, `particle minecraft:totem_of_undying` op de uitverkorene, title `DE KONING` met de naam als subtitle. |
| Kroonwissel | `entity.lightning_bolt.thunder` voor iedereen (geen echte bliksem, die zet dingen in de fik), `particle minecraft:flash` op de nieuwe koning, title `NIEUWE KONING` met naam, de zweefkroon springt over, bossbar-naam update. |
| Sudden death | Title `SUDDEN DEATH` in rood, `entity.wither.spawn`, bossbar rood, `worldborder warning distance 15` zodat de rand rood aankleurt. |
| Finalist | `ui.toast.challenge_complete` voor iedereen, vuurpijl boven de speler, title `FINALIST` met naam. |
| Kroning | Twintig seconden vuurpijlen boven de troonzaal (elke seconde één), title `KING OF THE SMP` met naam, `ui.toast.challenge_complete`. |

Vuurpijl (gouden bol met staart):

```
summon minecraft:firework_rocket <x> <y> <z> {LifeTime:20,FireworkItem:{id:"minecraft:firework_rocket",count:1,components:{"minecraft:fireworks":{explosions:[{shape:"large_ball",colors:[I;16766720,16777215],has_trail:true}],flight_duration:1}}}}
```

**Dialogs (optioneel):** sinds 1.21.6 kun je spelers een echt schermpje met knoppen laten zien
in plaats van een regel in de chat. Dat is netter voor de regels in het basiskamp en voor de
voice-knoppen. Dialogs zijn data-driven, dus dit is het enige stukje datapack:

```json
// data/bootcamp/dialog/doden.json
{
  "type": "minecraft:notice",
  "title": "Je ligt eruit",
  "body": [{ "type": "minecraft:plain_message", "contents": "Je kijkt nu mee als spectator. Klik om bij de Doden-voice te komen." }],
  "action": {
    "label": "Naar de Doden-voice",
    "action": { "type": "minecraft:run_command", "command": "voicechat join Doden" }
  }
}
```

Tonen vanuit Skript: `execute console command "dialog show %{_p}% bootcamp:doden"`. De chatknop
blijft de fallback als de dialog op 26.2 anders blijkt te werken.

## Testen en herladen

- `/sk reload bootcamp` laadt het script opnieuw zonder restart. Fouten staan in de console met
  regelnummer.
- Test elk onderdeel los met een tweede account: `/bc start 4` met twee spelers, kill de koning,
  kijk of de kroon overgaat. Spring van een klif als koning, kijk of de laatste hit hem krijgt.
- `/bc reset` voor elke testrun. Wereldbackup vóór het event, want `fill` en mobs laten sporen na.
- Kijk na de eerste start op 26.2 in de console of geen enkele plugin "unsupported version"
  roept. Skript en SkBee zijn daar het gevoeligst voor.

## De oude datapack-versie

De eerdere schets met een datapack in plaats van Skript staat in de git-geschiedenis (commit
`e51e143`). Die werkt nog steeds als je nul plugins wilt, maar regio's zetten met een tool is er
houtje-touwtje en de laatste hit voor de kroon kan er niet in.
