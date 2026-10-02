# Bouwt het client-modpack voor de streamers als Modrinth-pack (.mrpack).
#
#   powershell -ExecutionPolicy Bypass -File modpack/BouwModpack.ps1 -Server "<adres>:<poort>"
#
# Het pack zelf bevat geen mods: alleen een lijst met de vaste versies hieronder, met de
# download-url en de hashes van Modrinth. De Modrinth App, Prism Launcher en ATLauncher halen de
# bestanden bij het importeren zelf op. Daarnaast zet het pack klaar (overrides):
#   - options.txt          GUI-schaal 3, zodat bossbar en titles goed in beeld passen
#   - config/iris.properties  shaders aan, met Complementary Reimagined
#   - servers.dat          de bootcamp-server in de Multiplayer-lijst, resource pack automatisch aan
# En als modpack/icon.png er ligt, wordt dat het icoon van de instance.
#
# Het serveradres staat bewust niet in dit script: de repo is publiek.

param(
    [Parameter(Mandatory = $true)][string]$Server,
    [string]$ServerNaam = "Pudding's Bootcamp",
    [string]$Versie = "1.1.0"
)

$ErrorActionPreference = 'Stop'

$Minecraft = '26.2'
$FabricLoader = '0.19.5'

# De vaste versies: project op Modrinth, versienummer daar, en de map in de instance.
$Bestanden = @(
    @{ Project = 'fabric-api';               Versie = '0.161.0+26.2';        Map = 'mods' },
    @{ Project = 'sodium';                   Versie = 'mc26.2-0.9.2-fabric'; Map = 'mods' },
    @{ Project = 'iris';                     Versie = '1.11.4+26.2-fabric';  Map = 'mods' },
    @{ Project = 'simple-voice-chat';        Versie = 'fabric-2.6.24+26.2';  Map = 'mods' },
    @{ Project = 'essential';                Versie = '1.5.0.1';             Map = 'mods' },
    @{ Project = 'complementary-reimagined'; Versie = 'r5.9.3';              Map = 'shaderpacks' },
    @{ Project = 'complementary-unbound';    Versie = 'r5.9.3';              Map = 'shaderpacks' }
)
# Deze shader staat bij de eerste start aan.
$StandaardShader = 'complementary-reimagined'

$Map = Split-Path -Parent $MyInvocation.MyCommand.Path
$Bouw = Join-Path $Map 'bouw'
$Kop = @{ 'User-Agent' = 'BlessedByG/Pudding_Bootcamp_SMP modpack' }
$Utf8 = New-Object System.Text.UTF8Encoding($false)

# 1. Per bestand de versie op Modrinth opzoeken: url, hashes en grootte.
$Files = @()
$ShaderBestand = $null
foreach ($b in $Bestanden) {
    $versies = Invoke-RestMethod -Uri "https://api.modrinth.com/v2/project/$($b.Project)/version" -Headers $Kop
    $v = $versies | Where-Object { $_.version_number -eq $b.Versie -and ($_.game_versions -contains $Minecraft) } | Select-Object -First 1
    if ($null -eq $v) {
        throw "Versie $($b.Versie) van $($b.Project) voor $Minecraft niet gevonden op Modrinth."
    }
    $f = $v.files | Where-Object { $_.primary } | Select-Object -First 1
    if ($null -eq $f) {
        $f = $v.files | Select-Object -First 1
    }
    $Files += [ordered]@{
        path      = "$($b.Map)/$($f.filename)"
        hashes    = [ordered]@{ sha1 = $f.hashes.sha1; sha512 = $f.hashes.sha512 }
        env       = [ordered]@{ client = 'required'; server = 'unsupported' }
        downloads = @($f.url)
        fileSize  = $f.size
    }
    if ($b.Project -eq $StandaardShader) {
        $ShaderBestand = $f.filename
    }
    Write-Host ("{0,-26} {1,-22} {2}" -f $b.Project, $b.Versie, $f.filename)
}

# 2. De index.
$Index = [ordered]@{
    formatVersion = 1
    game          = 'minecraft'
    versionId     = $Versie
    name          = "Pudding's Bootcamp"
    summary       = 'Simple Voice Chat, Essential (emotes) en Complementary Shaders (Sodium en Iris) voor de Pudding Bootcamp.'
    files         = $Files
    dependencies  = [ordered]@{ minecraft = $Minecraft; 'fabric-loader' = $FabricLoader }
}

if (Test-Path $Bouw) {
    Remove-Item -Recurse -Force $Bouw
}
New-Item -ItemType Directory -Force (Join-Path $Bouw 'overrides\config') | Out-Null
[IO.File]::WriteAllText((Join-Path $Bouw 'modrinth.index.json'), ($Index | ConvertTo-Json -Depth 10), $Utf8)

# 3. De overrides.
[IO.File]::WriteAllText((Join-Path $Bouw 'overrides\options.txt'), "guiScale:3`n", $Utf8)
[IO.File]::WriteAllText((Join-Path $Bouw 'overrides\config\iris.properties'),
    "enableShaders=true`nshaderPack=$ShaderBestand`n", $Utf8)

# servers.dat is ongecomprimeerde NBT: { servers: [ { name, ip, acceptTextures: 1 } ] }.
function Schrijf-NbtString([IO.BinaryWriter]$w, [string]$tekst) {
    $bytes = $Utf8.GetBytes($tekst)
    $w.Write([byte]($bytes.Length -shr 8)); $w.Write([byte]($bytes.Length -band 0xFF)); $w.Write($bytes)
}
$stroom = New-Object IO.MemoryStream
$w = New-Object IO.BinaryWriter($stroom)
$w.Write([byte]10); Schrijf-NbtString $w ''                     # root compound
$w.Write([byte]9); Schrijf-NbtString $w 'servers'               # list
$w.Write([byte]10); $w.Write([byte[]](0, 0, 0, 1))              # van 1 compound
$w.Write([byte]8); Schrijf-NbtString $w 'name'; Schrijf-NbtString $w $ServerNaam
$w.Write([byte]8); Schrijf-NbtString $w 'ip'; Schrijf-NbtString $w $Server
$w.Write([byte]1); Schrijf-NbtString $w 'acceptTextures'; $w.Write([byte]1)
$w.Write([byte]0)                                               # einde server
$w.Write([byte]0)                                               # einde root
$w.Flush()
[IO.File]::WriteAllBytes((Join-Path $Bouw 'overrides\servers.dat'), $stroom.ToArray())

# Het icoon van de instance: de Modrinth App en Prism lezen icon.png uit de root van het pack.
$Icoon = Join-Path $Map 'icon.png'
if (Test-Path $Icoon) {
    Copy-Item $Icoon (Join-Path $Bouw 'icon.png')
} else {
    Write-Host 'Geen modpack/icon.png: het pack krijgt het standaardicoon van de launcher.'
}

# 4. Zippen, met / in de namen (Compress-Archive in PowerShell 5 zet er \ in).
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$Uit = Join-Path $Map ("Puddings-Bootcamp-$Versie.mrpack")
if (Test-Path $Uit) {
    Remove-Item -Force $Uit
}
$zip = [IO.Compression.ZipFile]::Open($Uit, 'Create')
try {
    Get-ChildItem -Recurse -File $Bouw | ForEach-Object {
        $naam = $_.FullName.Substring($Bouw.Length + 1).Replace('\', '/')
        [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, $_.FullName, $naam) | Out-Null
    }
} finally {
    $zip.Dispose()
}
Remove-Item -Recurse -Force $Bouw

Write-Host ''
Write-Host "Klaar: $Uit"
Write-Host ("Grootte: {0} kB" -f [math]::Round((Get-Item $Uit).Length / 1024))
