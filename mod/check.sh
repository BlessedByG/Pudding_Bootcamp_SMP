#!/usr/bin/env bash
# Vaste verificatie na elke taak. Draai vanuit mod/:  ./check.sh
# Modus A (standaard): volledige build met de echte Minecraft-jar.
# Modus B:             ./check.sh --core-only   (alleen de kernlogica, zonder Loom)
set -u
cd "$(dirname "$0")"

CORE_ONLY=0
[ "${1:-}" = "--core-only" ] && CORE_ONLY=1

FAIL=0
ok()   { echo "  ok    $1"; }
fout() { echo "  FOUT  $1"; FAIL=1; }

# Maven Central geeft soms 429; dan tot drie keer opnieuw met een pauze.
gradle_retry() {
	local n=0
	while :; do
		n=$((n + 1))
		./gradlew "$@" --console=plain > build-check.log 2>&1 && return 0
		if [ $n -lt 3 ] && grep -q "429" build-check.log; then
			echo "  ...   429 van de repository, poging $n van 3, even wachten"
			sleep 30
			continue
		fi
		return 1
	done
}

echo "== 1. Bouwen en testen"
if [ $CORE_ONLY -eq 1 ]; then
	if gradle_retry -PcoreOnly :core:test; then ok ":core:test"; else fout ":core:test (zie build-check.log)"; tail -40 build-check.log; fi
else
	if gradle_retry build; then ok "gradlew build (core-tests + fabric-compile)"; else fout "gradlew build (zie build-check.log)"; tail -60 build-check.log; fi
fi

echo "== 2. De jar bevat de kernlogica"
if [ $CORE_ONLY -eq 0 ]; then
	JAR=$(ls fabric/build/libs/bootcamp-*.jar 2>/dev/null | grep -v sources | head -1)
	if [ -z "$JAR" ]; then
		fout "geen jar in fabric/build/libs/"
	elif unzip -l "$JAR" | grep -Eq "META-INF/jars/core-.*\.jar|nl/pudding/bootcamp/core/"; then
		ok "$JAR"
	else
		fout "$JAR bevat core niet"
	fi
else
	echo "  --    overgeslagen (--core-only)"
fi

echo "== 3. Geen threads, geen sleeps"
if grep -rnE "Thread\.sleep|new Thread\(" core/src fabric/src --include=*.java; then
	fout "Thread.sleep of new Thread gevonden; alles hoort tick-gestuurd te zijn"
else
	ok "niets gevonden"
fi

echo "== 4. Core importeert geen Minecraft"
if grep -rnE "^import (net\.minecraft|net\.fabricmc|com\.mojang)" core/src --include=*.java; then
	fout "core hoort geen Minecraft- of Fabric-imports te hebben"
else
	ok "core is schoon"
fi

echo "== 5. Elk command uit docs/04 staat in het commands-package"
CMD_DIR=fabric/src/main/java/nl/pudding/bootcamp/commands
if [ -d "$CMD_DIR" ]; then
	for c in wand region save show list del point set block tp start kit stop timer status poort kroon uitverkoren slot rad kijker reset; do
		grep -rq "\"$c\"" "$CMD_DIR" || fout "command-literal \"$c\" ontbreekt"
	done
	[ $FAIL -eq 0 ] && ok "alle literals aanwezig"
else
	echo "  --    commands-package bestaat nog niet"
fi

echo
if [ $FAIL -eq 0 ]; then echo "CHECK GROEN"; else echo "CHECK ROOD"; fi
exit $FAIL
