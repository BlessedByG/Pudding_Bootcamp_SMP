package nl.pudding.bootcamp.schrik;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.font.providers.GlyphProviderDefinition;
import net.minecraft.server.Bootstrap;
import nl.pudding.bootcamp.core.QuizRad;
import nl.pudding.bootcamp.core.Regels;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Het resource pack dat BouwPack maakte, naast de regels van 26.2: elke font-provider gaat door de
 * echte codec (die weigert bijvoorbeeld een ascent hoger dan de height), elke tegel past op een
 * font-vel van 256 x 256 en is een heel aantal eenheden breed, en elk teken dat de mod stuurt staat
 * in de font. Zonder gebouwd pack slaat hij over (eerst {@code java pack/BouwPack.java}).
 */
class PackFontTest {
	private static final Path PACK = Path.of(System.getProperty("bootcamp.pack", "../../pack"));
	private static final Path FONTS = PACK.resolve("assets/bootcamp/font");
	private static final Path TEXTURES = PACK.resolve("assets/bootcamp/textures");
	/** Minecraft zet glyphs op vellen van deze maat; groter wordt een leeg vierkantje. */
	private static final int VEL = 256;

	@BeforeAll
	static void start() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void schrik() throws IOException {
		for (int foto = 1; foto <= Regels.SCHRIK_FOTOS; foto++) {
			controleer("schrik_" + foto + ".json", Schrik.TEKST);
		}
	}

	@Test
	void rad() throws IOException {
		for (int s = 0; s < QuizRad.STANDEN; s++) {
			controleer("rad.json", QuizRad.glyph(s));
		}
		for (int vak = 0; vak < QuizRad.VAKKEN.size(); vak++) {
			controleer("rad.json", QuizRad.glyphOplicht(vak));
		}
	}

	/** De tegenproef: de codec weigert een glyph die hoger boven de basislijn staat dan hij hoog is. */
	@Test
	void codecWeigertTeHogeAscent() {
		JsonObject p = JsonParser.parseString(
				"{\"type\": \"bitmap\", \"file\": \"bootcamp:font/x.png\", \"height\": 34, \"ascent\": 35, \"chars\": [\"\\uE000\"]}").getAsJsonObject();
		assertTrue(GlyphProviderDefinition.MAP_CODEC.codec().parse(JsonOps.INSTANCE, p).isError());
	}

	private static void controleer(String bestand, String tekst) throws IOException {
		Path json = FONTS.resolve(bestand);
		assumeTrue(Files.exists(json), "geen gebouwd pack in " + PACK.toAbsolutePath());
		JsonObject root = JsonParser.parseString(Files.readString(json, StandardCharsets.UTF_8)).getAsJsonObject();
		Set<Integer> bekend = new HashSet<>();
		for (JsonElement el : root.getAsJsonArray("providers")) {
			JsonObject p = el.getAsJsonObject();
			GlyphProviderDefinition.MAP_CODEC.codec().parse(JsonOps.INSTANCE, p)
					.getOrThrow(fout -> new AssertionError(bestand + ": " + fout + " in " + p));
			if (p.get("type").getAsString().equals("space")) {
				for (String k : p.getAsJsonObject("advances").keySet()) {
					bekend.add(k.codePointAt(0));
				}
				continue;
			}
			bekend.add(p.getAsJsonArray("chars").get(0).getAsString().codePointAt(0));
			String file = p.get("file").getAsString();
			Path png = TEXTURES.resolve(file.substring(file.indexOf(':') + 1));
			BufferedImage img = ImageIO.read(png.toFile());
			assertTrue(img.getWidth() <= VEL && img.getHeight() <= VEL, png + " is " + img.getWidth() + " x " + img.getHeight());
			int hoogte = p.get("height").getAsInt();
			// Een hele eenheid breed, anders sluiten de tegels niet naadloos aan.
			assertEquals(0, img.getWidth() * hoogte % img.getHeight(), png + " is geen heel aantal eenheden breed");
			// De meest rechtse kolom heeft een pixel, zodat Minecraft de volle breedte rekent.
			boolean rechts = false;
			for (int y = 0; y < img.getHeight() && !rechts; y++) {
				rechts = (img.getRGB(img.getWidth() - 1, y) >>> 24) != 0;
			}
			assertTrue(rechts, png + " heeft een lege rechterkolom");
		}
		tekst.codePoints().forEach(c -> {
			if (!bekend.contains(c)) {
				fail(bestand + " mist teken U+" + Integer.toHexString(c).toUpperCase());
			}
		});
	}
}
