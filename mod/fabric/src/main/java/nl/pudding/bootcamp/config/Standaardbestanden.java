package nl.pudding.bootcamp.config;

import net.fabricmc.loader.api.FabricLoader;
import nl.pudding.bootcamp.Bootcamp;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * De voorbeeld-kits en {@code waves.json} zitten als resources in de jar (map {@code standaard/})
 * en worden bij de eerste serverstart naar {@code config/bootcamp/} geschreven. Wat er al staat
 * blijft staan: je eigen aanpassingen worden nooit overschreven.
 */
public final class Standaardbestanden {
	/** Paden onder {@code standaard/} in de jar, en onder {@code config/bootcamp/} op de server. */
	public static final List<String> BESTANDEN = List.of(
			"waves.json",
			"kits/horde.json",
			"kits/ei.json",
			"kits/boss.json",
			"kits/kroonpakket.json",
			"kits/arena.json",
			"kits/finale.json",
			"kits/basis.README.txt");

	private Standaardbestanden() {
	}

	public static Path map() {
		return FabricLoader.getInstance().getConfigDir().resolve("bootcamp");
	}

	public static Path kitsMap() {
		return map().resolve("kits");
	}

	public static void schrijfOntbrekende() {
		int geschreven = 0;
		for (String bestand : BESTANDEN) {
			Path doel = map().resolve(bestand);
			if (Files.exists(doel)) {
				continue;
			}
			try (InputStream in = Standaardbestanden.class.getResourceAsStream("/standaard/" + bestand)) {
				if (in == null) {
					Bootcamp.LOG.warn("Standaardbestand {} zit niet in de jar", bestand);
					continue;
				}
				Files.createDirectories(doel.getParent());
				Files.copy(in, doel);
				geschreven++;
			} catch (IOException e) {
				Bootcamp.LOG.error("Kon {} niet schrijven", doel, e);
			}
		}
		if (geschreven > 0) {
			Bootcamp.LOG.info("{} standaardbestanden geschreven naar {}", geschreven, map());
		}
	}
}
