package nl.pudding.bootcamp.config;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import nl.pudding.bootcamp.Bootcamp;
import nl.pudding.bootcamp.core.BootcampConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Laadt en bewaart {@code <wereld>/bootcamp.json}. Elke wijziging via een command wordt meteen opgeslagen. */
public final class ConfigStore {
	public static final String BESTAND = "bootcamp.json";

	private static BootcampConfig config = new BootcampConfig();
	private static Path pad;

	private ConfigStore() {
	}

	public static BootcampConfig get() {
		return config;
	}

	public static void laad(MinecraftServer server) {
		pad = server.getWorldPath(LevelResource.ROOT).resolve(BESTAND).normalize();
		if (!Files.exists(pad)) {
			config = new BootcampConfig();
			Bootcamp.LOG.info("Geen {} gevonden; begin met een lege config ({})", BESTAND, pad);
			return;
		}
		try {
			config = BootcampConfig.uitJson(Files.readString(pad, StandardCharsets.UTF_8));
			Bootcamp.LOG.info("{} geladen: {} regio's, {} punten", BESTAND, config.regios().size(), config.punten().size());
		} catch (IOException | IllegalArgumentException e) {
			// Niet overschrijven wat er staat: eerst opzij zetten, dan leeg beginnen.
			config = new BootcampConfig();
			Path kapot = pad.resolveSibling(BESTAND + ".kapot");
			try {
				Files.copy(pad, kapot, StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException kopieFout) {
				Bootcamp.LOG.error("Kon {} niet opzij zetten", pad, kopieFout);
			}
			Bootcamp.LOG.error("{} is niet te lezen ({}). Kopie staat in {}; de mod begint met een lege config.",
					BESTAND, e.getMessage(), kapot);
		}
	}

	/** @return {@code true} als het opslaan gelukt is */
	public static boolean bewaar() {
		if (pad == null) {
			return false;
		}
		try {
			Path tijdelijk = pad.resolveSibling(BESTAND + ".tmp");
			Files.writeString(tijdelijk, config.naarJson(), StandardCharsets.UTF_8);
			Files.move(tijdelijk, pad, StandardCopyOption.REPLACE_EXISTING);
			return true;
		} catch (IOException e) {
			Bootcamp.LOG.error("Kon {} niet opslaan", pad, e);
			return false;
		}
	}
}
