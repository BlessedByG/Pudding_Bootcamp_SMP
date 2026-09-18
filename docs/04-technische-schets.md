# Technische schets

Hoe je dit bouwt zonder plugins: vanilla Java + een datapack. Alles hieronder is een schets om
van te bouwen, geen kant-en-klaar pakket.

> **Let op:** commands zijn geschreven voor Java 1.21.5 of nieuwer en zijn **niet getest**. Test
> elke command op je eigen server voordat het event draait. Waar de syntax per versie verschilt
> staat het erbij.

## Server

- Paper of vanilla, 1.21.x. Paper is fijner voor performance met 20 spelers en 60 mobs, en je
  hebt er geen plugins bij nodig.
- `view-distance=8`, `simulation-distance=6`. Whitelist aan.
- `server.properties`: `pvp=true` (PvP regelen we via teams, niet via deze setting).
- Gamerules bij de start:

```
gamerule keepInventory true
gamerule doImmediateRespawn true
gamerule naturalRegeneration true
gamerule doMobSpawning false
gamerule doDaylightCycle false
gamerule announceAdvancements false
gamerule spectatorsGenerateChunks false
```

`doImmediateRespawn` staat aan zodat je de respawn-vertraging zelf kunt regelen (spectator voor
X seconden) zonder dat iemand in het death-scherm blijft hangen. `doMobSpawning` uit, de horde
spawnen we zelf.

## Bouwblokken

**Tags** (per speler):

| Tag | Betekenis |
|---|---|
| `speler` | Doet mee (alle 20). |
| `hunter` | Ronde 4: hunter. |
| `king` | Ronde 4 t/m 6: heeft een kroon. |
| `out` | Uitgeschakeld, spectator. |
| `finalist` | Finalist 1 of 2. |
| `ticket` | Ronde 3: heeft een diamond block ingeleverd. |
| `ffa` | Ronde 5: doet mee aan de FFA. |
| `staff` | Admins, worden door alle selectors overgeslagen. |

**Teams** (dit is de PvP-schakelaar; friendly fire uit betekent dat teamgenoten elkaar niet
kunnen raken):

```
team add spelers
team modify spelers friendlyFire false
team modify spelers color white

team add hunters
team modify hunters friendlyFire false
team modify hunters color aqua

team add king
team modify king color gold

team add out
team modify out color gray
```

Ronde 0 t/m 3: iedereen in `spelers`. Ronde 4: `hunters` + `king`. Ronde 5 en 6: iedereen
`team leave`, behalve de koningen (die zitten in `king` maar `king` heeft friendly fire aan, dus
ze kunnen elkaar in de finale gewoon raken). De teamkleur bepaalt ook de kleur van de
Glowing-outline en de naam in de tab-list.

**Scoreboards:**

```
scoreboard objectives add deaths deathCount
scoreboard objectives add respawn dummy
scoreboard objectives add timer dummy
scoreboard objectives add reign dummy
scoreboard objectives add kills playerKillCount
scoreboard objectives add hordedeaths dummy
scoreboard objectives setdisplay sidebar reign
```

**Bossbar:**

```
bossbar add bootcamp:main "Pudding Bootcamp"
bossbar set bootcamp:main players @a
bossbar set bootcamp:main color blue
```

## Datapack-structuur

```
datapacks/bootcamp/
  pack.mcmeta
  data/bootcamp/
    function/
      setup.mcfunction                 # teams, scoreboards, bossbar, gamerules
      poort/open_1.mcfunction .. open_4 # fill de poort weg
      poort/dicht_1.mcfunction .. dicht_4
      doolhof/start.mcfunction         # timer 600, tp iedereen naar ingang
      doolhof/tick.mcfunction
      doolhof/einde.mcfunction         # tp achterblijvers naar wachtkamer 2
      horde/start.mcfunction
      horde/wave_1.mcfunction .. wave_5
      horde/tick.mcfunction            # telt mobs, start volgende wave
      horde/kit.mcfunction
      ei/start.mcfunction
      ei/tick.mcfunction               # poortcheck, beacon-hint op 5 min
      ei/pas.mcfunction                # neemt block in, tp naar wachtkamer 4
      ei/einde.mcfunction              # achterblijvers zonder ticket: clear + basiskit
      horde/volgende.mcfunction        # start de volgende wave
      horde/dood.mcfunction            # 15 sec spectator, dan respawn
      king/start.mcfunction
      king/tick.mcfunction             # timer, respawns, sudden death, bossbar
      king/transfer.mcfunction         # kroon naar de killer (run as killer)
      king/give.mcfunction             # kroon geven (run as de nieuwe koning)
      king/dood.mcfunction             # hunter dood: 20 sec spectator
      king/respawn.mcfunction          # hunter terug op een randpunt
      king/uit.mcfunction              # sudden death: dood = eruit
      king/einde.mcfunction            # finalist 1, rest naar FFA
      ffa/start.mcfunction
      ffa/tick.mcfunction              # last man standing check, border
      ffa/uit.mcfunction               # dood = spectator
      finale/start.mcfunction
      finale/potje.mcfunction
      kit/basis.mcfunction
      kit/horde.mcfunction
      kit/boss.mcfunction
      kit/arena.mcfunction
      kit/finale.mcfunction
      reset.mcfunction                 # alles terug naar lobby-staat
    advancement/
      kill_king.json                   # vuurt als je de koning killt
```

Op 1.20.x heten de mappen `functions/` en `advancements/` (meervoud).

## De kroon

**Geven** (`king/give.mcfunction`, run `as` de nieuwe koning):

```
tag @s add king
tag @s remove hunter
team join king @s
item replace entity @s armor.head with minecraft:golden_helmet[minecraft:enchantments={"minecraft:binding_curse":1},minecraft:unbreakable={}]
effect give @s minecraft:glowing infinite 0 true
effect give @s minecraft:instant_health 1 3 true
effect give @s minecraft:resistance 15 1 true
give @s minecraft:golden_apple 2
give @s minecraft:ender_pearl 2
scoreboard players reset @s deaths
scoreboard players set @s reign 0
title @a title {"text":"NIEUWE KONING","color":"gold"}
title @a subtitle {"text":"","extra":[{"selector":"@s"}]}
```

Op 1.20.5 t/m 1.21.4 is de enchantment-component `minecraft:enchantments={levels:{"minecraft:binding_curse":1}}`.
Een naam op de helm is leuk maar niet nodig; de syntax van `custom_name` verschilt per versie.

**Overdracht na een kill** (`king/transfer.mcfunction`, run `as` de killer):

```
# oude koning eruit
item replace entity @a[tag=king] armor.head with minecraft:air
effect clear @a[tag=king] minecraft:glowing
tag @a[tag=king] add out
tag @a[tag=king] remove king
team join out @a[tag=out]
gamemode spectator @a[tag=out]

# nieuwe koning
function bootcamp:king/give
advancement revoke @s only bootcamp:kill_king
```

**De trigger** (`advancement/kill_king.json`): vuurt voor de speler die een lid van team `king`
doodt en draait dan `king/transfer` als die speler.

```json
{
  "criteria": {
    "kill_king": {
      "trigger": "minecraft:player_killed_entity",
      "conditions": {
        "entity": [
          {
            "condition": "minecraft:entity_properties",
            "entity": "this",
            "predicate": { "type": "minecraft:player", "team": "king" }
          }
        ]
      }
    }
  },
  "rewards": { "function": "bootcamp:king/transfer" }
}
```

Geen `display`, dus de advancement is onzichtbaar. De `advancement revoke` aan het eind van
`transfer` zorgt dat hij opnieuw kan vuren voor dezelfde speler.

**Koning dood zonder killer** (in `king/tick`): een koning met een death op zijn teller is dood
gegaan zonder dat de advancement vuurde (val, lava, mob). Geef de kroon dan aan een random hunter.
"Laatste hunter die hem raakte" is in vanilla lastig; random is de simpele versie, en de admin
kan altijd handmatig overrulen met `execute as <speler> run function bootcamp:king/give`.

```
execute if entity @a[tag=king,scores={deaths=1..}] as @r[tag=hunter,gamemode=survival] run function bootcamp:king/transfer
```

## Timer en tick

Eén functie per ronde die zichzelf elke seconde opnieuw inplant:

```
# king/tick.mcfunction
scoreboard players remove #king timer 1
scoreboard players add @a[tag=king] reign 1
execute store result bossbar bootcamp:main value run scoreboard players get #king timer

# sudden death op 180 sec
execute if score #king timer matches 180 run title @a title {"text":"SUDDEN DEATH","color":"red"}
execute if score #king timer matches 180 run worldborder set 60 180
execute if score #king timer matches 180 run bossbar set bootcamp:main color yellow

# respawns (alleen buiten sudden death)
execute if score #king timer matches 181.. as @a[tag=hunter,scores={deaths=1..}] run function bootcamp:king/dood
execute if score #king timer matches ..180 as @a[tag=hunter,scores={deaths=1..}] run function bootcamp:king/uit
scoreboard players remove @a[scores={respawn=1..}] respawn 1
execute as @a[tag=hunter,gamemode=spectator,scores={respawn=0}] run function bootcamp:king/respawn

# einde
execute if score #king timer matches 0 run function bootcamp:king/einde
execute if score #king timer matches 1.. run schedule function bootcamp:king/tick 1s
```

```
# king/dood.mcfunction  (run as de dode hunter)
scoreboard players reset @s deaths
gamemode spectator @s
scoreboard players set @s respawn 20

# king/respawn.mcfunction
scoreboard players reset @s respawn
spreadplayers <x> <z> 0 1 false @s       # of tp naar een van de 4 randpunten
gamemode survival @s
effect give @s minecraft:resistance 5 4 true

# king/uit.mcfunction  (sudden death: dood = eruit)
scoreboard players reset @s deaths
tag @s add out
tag @s remove hunter
team join out @s
gamemode spectator @s
```

`king/start` zet `#king timer` op 900, de bossbar op max 900, de worldborder op 200 rond de
burcht, en plant de eerste `tick` in. Stoppen: `schedule clear bootcamp:king/tick`.

Bossbar-naam met de naam van de koning updaten:

```
bossbar set bootcamp:main name {"text":"Koning: ","extra":[{"selector":"@a[tag=king,limit=1]"}]}
```

## Einde ronde 4 en de FFA

```
# king/einde.mcfunction
tag @a[tag=king] add finalist
tag @a[tag=hunter,gamemode=survival] add ffa
execute as @a[tag=ffa] run function bootcamp:kit/arena
effect give @a[tag=ffa] minecraft:instant_health 1 3 true
team leave @a[tag=ffa]
spreadplayers <ffa-x> <ffa-z> 5 18 false @a[tag=ffa]
tp @a[tag=king] <spectator-deck boven de ffa>
function bootcamp:ffa/start
```

Vervalt de FFA (maar één hunter over), dan tag je die direct `finalist` en `king` en ga je door
naar `finale/start`.

```
# ffa/tick.mcfunction
execute as @a[tag=ffa,scores={deaths=1..}] run function bootcamp:ffa/uit
execute store result score #alive timer if entity @a[tag=ffa,gamemode=survival]
execute if score #alive timer matches 1 as @a[tag=ffa,gamemode=survival] run function bootcamp:king/give
execute if score #alive timer matches 1 run function bootcamp:finale/start
execute if score #alive timer matches 2.. run schedule function bootcamp:ffa/tick 1s
```

```
# ffa/uit.mcfunction  (run as de dode speler)
scoreboard players reset @s deaths
tag @s add out
tag @s remove ffa
team join out @s
gamemode spectator @s
```

Border-shrink voor de FFA: `worldborder center <x> <z>`, `worldborder set 40`, en op 5 minuten
`worldborder set 10 120`.

## Poorten

Een poort is een muur die je met `fill` weghaalt en terugzet:

```
# poort/open_2.mcfunction
fill <x1 y1 z1> <x2 y2 z2> minecraft:air
# poort/dicht_2.mcfunction
fill <x1 y1 z1> <x2 y2 z2> minecraft:iron_bars
```

Countdown ervoor met `title @a title {"text":"3"}` enzovoort via `schedule`.

## De Ei-poort (ticketcheck)

In `ei/tick`: iedereen die op de drukplaat voor poort 4 staat en een diamond block bij zich
heeft, gaat door:

```
execute as @a[tag=speler,tag=!ticket,x=<px>,y=<py>,z=<pz>,dx=2,dy=2,dz=2] if items entity @s container.* minecraft:diamond_block run function bootcamp:ei/pas
```

```
# ei/pas.mcfunction
clear @s minecraft:diamond_block 1
tag @s add ticket
tp @s <wachtkamer 4>
playsound minecraft:entity.player.levelup master @s
```

Op versies vóór 1.20.5 bestaat `if items` niet; gebruik dan
`@a[nbt={Inventory:[{id:"minecraft:diamond_block"}]}]`.

Hint op 5 minuten: zet met `setblock` een blok neer dat het beacon-pyramidetje onder het Ei
compleet maakt, en op 3 minuten `summon minecraft:firework_rocket` boven het Ei.

Na de timer (`ei/einde`): `clear @a[tag=speler,tag=!ticket]`, `function bootcamp:kit/basis` voor
die groep, en `tp` naar wachtkamer 4.

## De horde

Mobs spawnen met een tag, zodat je ze kunt tellen en opruimen:

```
# horde/wave_1.mcfunction (herhaal per spawnpunt)
summon minecraft:zombie <x> <y> <z> {Tags:["horde"],PersistenceRequired:1b}
...
bossbar set bootcamp:main name {"text":"Wave 1"}
```

```
# horde/tick.mcfunction
execute store result score #mobs timer if entity @e[tag=horde]
execute store result bossbar bootcamp:main value run scoreboard players get #mobs timer
scoreboard players remove #wave timer 1
execute if score #mobs timer matches 0 run function bootcamp:horde/volgende
execute if score #wave timer matches 0 run function bootcamp:horde/volgende
execute as @a[tag=speler,scores={deaths=1..}] run function bootcamp:horde/dood
...
schedule function bootcamp:horde/tick 1s
```

`horde/volgende` zet `#wave timer` op 120 en roept de volgende wave-functie aan (bijhouden welke
via een scoreboard `#wavenr`). `horde/dood` doet hetzelfde als `king/dood` maar met 15 seconden
en telt `hordedeaths` op.

Gear op mobs (voor wave 3 en 4): `{HandItems:[{id:"minecraft:iron_sword",count:1},{}],ArmorItems:[{},{},{id:"minecraft:iron_chestplate",count:1},{}]}`
achter de summon. Op 1.20.4 en ouder is dat `Count` met hoofdletter.

## Kits

Een kit is een lijstje `item replace` en `give` commands. Voorbeeld `kit/arena`:

```
clear @s
item replace entity @s armor.head with minecraft:diamond_helmet[minecraft:enchantments={"minecraft:protection":1}]
item replace entity @s armor.chest with minecraft:diamond_chestplate[minecraft:enchantments={"minecraft:protection":1}]
item replace entity @s armor.legs with minecraft:diamond_leggings[minecraft:enchantments={"minecraft:protection":1}]
item replace entity @s armor.feet with minecraft:diamond_boots[minecraft:enchantments={"minecraft:protection":1}]
item replace entity @s weapon.offhand with minecraft:shield
give @s minecraft:diamond_sword[minecraft:enchantments={"minecraft:sharpness":1}]
give @s minecraft:bow[minecraft:enchantments={"minecraft:power":1}]
give @s minecraft:arrow 16
give @s minecraft:golden_apple 2
give @s minecraft:cooked_beef 8
```

## Reset

`reset.mcfunction`: alle tags weg, iedereen `team join spelers`, `gamemode adventure`, `clear`,
`effect clear`, tp naar de lobby, alle timers `schedule clear`, bossbar leeg, worldborder terug op
groot. Handig als er iets kapot gaat en voor de testrun.

Maak daarnaast vóór het event een **wereldbackup**. Gaat het Ei op, dan kun je in het ergste
geval de wereld terugzetten in plaats van het Ei opnieuw bouwen.

## Alternatief: Paper-plugin

Heb je een developer in de groep, dan is een kleine Paper-plugin netter: echte "laatste hit"
tracking voor de kroon, per-zone PvP zonder team-trucs, en een respawn-vertraging zonder
spectator-gedoe. Maar de datapack doet alles wat hierboven staat, en je hoeft niks te
compileren. Begin met de datapack.
