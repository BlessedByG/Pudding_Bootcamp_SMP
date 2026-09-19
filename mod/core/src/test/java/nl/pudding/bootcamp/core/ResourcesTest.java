package nl.pudding.bootcamp.core;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Valideert de standaardbestanden die in de jar van de mod gaan. */
class ResourcesTest {
	private static final Path RESOURCES = Path.of(System.getProperty("bootcamp.fabricResources", "../fabric/src/main/resources"));
	private static final Path STANDAARD = RESOURCES.resolve("standaard");

	private static String lees(Path p) throws IOException {
		return Files.readString(p, StandardCharsets.UTF_8);
	}

	@Test
	void fabricModJson() throws IOException {
		JsonObject mod = JsonParser.parseString(lees(RESOURCES.resolve("fabric.mod.json"))).getAsJsonObject();
		assertEquals("bootcamp", mod.get("id").getAsString());
		assertEquals("server", mod.get("environment").getAsString());
		assertFalse(mod.has("mixins"), "geen mixins-config");
		assertTrue(mod.getAsJsonObject("entrypoints").has("main"));
		JsonObject depends = mod.getAsJsonObject("depends");
		assertEquals("~26.2", depends.get("minecraft").getAsString());
		assertEquals(">=25", depends.get("java").getAsString());
		assertTrue(depends.has("fabric-api"));

		String entry = mod.getAsJsonObject("entrypoints").getAsJsonArray("main").get(0).getAsString();
		Path bron = RESOURCES.resolve("../java").resolve(entry.replace('.', '/') + ".java").normalize();
		assertTrue(Files.exists(bron), "entrypoint bestaat niet: " + bron);
	}

	@Test
	void alleKitsZijnGeldig() throws IOException {
		Path kits = STANDAARD.resolve("kits");
		assumeTrue(Files.isDirectory(kits), "kits komen in T5");
		try (Stream<Path> s = Files.list(kits)) {
			List<Path> bestanden = s.filter(p -> p.toString().endsWith(".json")).toList();
			for (Path p : bestanden) {
				KitDef.uitJson(p.getFileName().toString(), lees(p));
			}
			List<String> namen = bestanden.stream().map(p -> p.getFileName().toString()).toList();
			for (String verwacht : List.of("horde.json", "ei.json", "boss.json", "kroonpakket.json", "arena.json", "finale.json")) {
				assertTrue(namen.contains(verwacht), verwacht + " ontbreekt");
			}
			assertFalse(namen.contains("basis.json"), "basis.json komt van Pudding en wordt niet meegeleverd");
		}
		// De kroon blijft op: deze kits hebben geen helm.
		assertFalse(KitDef.uitJson("boss.json", lees(kits.resolve("boss.json"))).heeftHelm());
		assertFalse(KitDef.uitJson("finale.json", lees(kits.resolve("finale.json"))).heeftHelm());
		// Deze kits voegen toe en wissen dus niks.
		assertFalse(KitDef.uitJson("ei.json", lees(kits.resolve("ei.json"))).clear());
		assertFalse(KitDef.uitJson("kroonpakket.json", lees(kits.resolve("kroonpakket.json"))).clear());
	}

	@Test
	void wavesZijnGeldig() throws IOException {
		Path waves = STANDAARD.resolve(WavesDef.BESTAND);
		assumeTrue(Files.exists(waves), "waves.json komt in T5");
		WavesDef def = WavesDef.uitJson(lees(waves));
		assertEquals(5, def.waves().size());
		assertEquals(20, def.waves().get(0).totaal(20));
		assertEquals(25, def.waves().get(1).totaal(20));
		assertEquals(23, def.waves().get(2).totaal(20));
		assertEquals(29, def.waves().get(3).totaal(20));
		assertEquals(16, def.waves().get(4).totaal(20));
		assertFalse(def.waves().get(4).schaal(), "de boss wave staat vast");
	}
}
