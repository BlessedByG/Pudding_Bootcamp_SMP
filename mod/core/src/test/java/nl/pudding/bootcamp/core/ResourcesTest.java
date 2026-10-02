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
			for (String verwacht : List.of("basis.json", "ei.json", "jager.json", "boss.json", "kroonpakket.json", "arena.json")) {
				assertTrue(namen.contains(verwacht), verwacht + " ontbreekt");
			}
		}
		// De kroon blijft op: de bosskit heeft geen helm; de jagers en de FFA wel.
		assertFalse(KitDef.uitJson("boss.json", lees(kits.resolve("boss.json"))).heeftHelm());
		assertTrue(KitDef.uitJson("jager.json", lees(kits.resolve("jager.json"))).heeftHelm());
		assertTrue(KitDef.uitJson("arena.json", lees(kits.resolve("arena.json"))).heeftHelm());
		// Deze kits voegen toe en wissen dus niks.
		assertFalse(KitDef.uitJson("ei.json", lees(kits.resolve("ei.json"))).clear());
		assertFalse(KitDef.uitJson("kroonpakket.json", lees(kits.resolve("kroonpakket.json"))).clear());
	}

	@Test
	void wavesZijnGeldig() throws IOException {
		Path waves = STANDAARD.resolve(WavesDef.BESTAND);
		assumeTrue(Files.exists(waves), "waves.json komt in T5");
		WavesDef def = WavesDef.uitJson(lees(waves));
		// De tabel uit docs/02, voor 8 spelers in één veld.
		assertEquals(5, def.waves().size());
		assertEquals(56, def.waves().get(0).totaal());
		assertEquals(64, def.waves().get(1).totaal());
		assertEquals(64, def.waves().get(2).totaal());
		assertEquals(80, def.waves().get(3).totaal());
		assertEquals(48, def.waves().get(4).totaal());
	}

	@Test
	void lootIsGeldig() throws IOException {
		LootTabel t = LootTabel.uitJson(lees(STANDAARD.resolve(LootTabel.BESTAND)));
		assertEquals(2, t.min());
		assertEquals(4, t.max());
		assertTrue(t.items().stream().noneMatch(i -> i.item().spec().contains("ender_pearl")), "geen pearls in het doolhof");
	}
}
