import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Bouwt het server resource pack van de Pudding Bootcamp. Draaien vanuit de repo:
 *
 * <pre>java pack/BouwPack.java</pre>
 *
 * Alleen de JDK is nodig. Het programma:
 * <ol>
 * <li>leest de vijf jumpscare-foto's {@code schrik_1} t/m {@code schrik_5} uit
 * {@code pack/aanleveren/} (jpg, jpeg of png, elk formaat) en schaalt ze naar 476 pixels hoog (of zo
 * breed als past, tot 1428 pixels), met behoud van de verhouding; een foto die er niet is, wordt een
 * placeholder;</li>
 * <li>zet de geluiden {@code schrik.ogg} (de jumpscare), {@code klop.ogg} (de 8D-klop uit de
 * valkisten) en {@code rad.ogg} (een draai van het quiz-rad) erbij als ze er zijn;</li>
 * <li>tekent het quiz-rad: 64 standen van 484 x 484, elk 5,625 graden verder met de klok mee
 * gedraaid, 16 vakken in de teamkleuren, pijltje vast bovenin;</li>
 * <li>knipt de foto en elke stand van het rad in tegels (zie hieronder) en schrijft de fonts en
 * {@code pack.mcmeta};</li>
 * <li>zipt alles naar {@code pack/bootcamp-pack.zip} en print de SHA-1 voor
 * {@code server.properties}.</li>
 * </ol>
 *
 * <p><b>Tegels.</b> Minecraft zet font-glyphs op vellen van 256 x 256 pixels; een glyph die groter
 * is, wordt stilletjes een leeg vierkantje. Daarom wordt elk plaatje geknipt in twee rijen tegels
 * van hooguit 242 pixels, elk met zijn eigen glyph. Twee rijen, omdat een glyph niet hoger boven de
 * basislijn mag staan dan hij hoog is ({@code ascent <= height}). De mod zet de tegels weer aan
 * elkaar met de tekst uit {@code nl.pudding.bootcamp.core.FontTegels}: na elke tegel een spatie van
 * -1 (U+F801), na de bovenste rij een spatie terug over de hele breedte (U+F802). Een tegel moet
 * precies een heel aantal font-eenheden breed zijn, anders ontstaat er een naad; daarom zijn de
 * pixelmaten veelvouden van de pixels per eenheid (7 voor de foto, 11 voor het rad).
 *
 * <p>De volgorde van de vakken en de draairichting zijn dezelfde als in de mod
 * ({@code nl.pudding.bootcamp.core.QuizRad}): in stand 0 staat vak 0 onder het pijltje.
 */
public class BouwPack {
	/** Het resource-packformaat van Minecraft 26.2. */
	static final int PACK_FORMAT = 88;
	static final int STANDEN = 64;

	/** Spatie van -1 na elke tegel, en de spatie terug na de bovenste rij (FontTegels in de mod). */
	static final int TERUG_EEN = 0xF801;
	static final int TERUG_RIJ = 0xF802;

	/**
	 * De jumpscare: twee rijen van 34 font-eenheden (238 pixels, 7 per eenheid), dus 68 eenheden
	 * hoog; een title tekent vier keer zo groot, dat vult op 1080p met de automatische GUI-schaal het
	 * scherm. Hooguit zes tegels breed. Tegel (r, c) is U+E000 + 16r + c (Schrik in de mod).
	 */
	static final int SCHRIK_EERSTE = 0xE000;
	static final int SCHRIK_RIJ_STAP = 16;
	static final int SCHRIK_KOLOMMEN = 6;
	static final int SCHRIK_EENHEDEN = 34;
	static final int SCHRIK_PX_PER_EENHEID = 7;
	/** Zoveel jumpscare-foto's, elk in een eigen font {@code schrik_1} t/m {@code schrik_5} (Regels.SCHRIK_FOTOS in de mod). */
	static final int FOTOS = 5;

	/**
	 * Het quiz-rad: 484 x 484 pixels, 2 x 2 tegels van 22 eenheden (242 pixels, 11 per eenheid), dus
	 * 44 eenheden hoog: ongeveer tweederde van het scherm. Stand s, tegel (r, c) is
	 * U+E100 + 4s + 2r + c (QuizRad in de mod).
	 */
	static final int RAD_EERSTE = 0xE100;
	static final int RAD_EENHEDEN = 22;
	static final int RAD_PX_PER_EENHEID = 11;
	static final int RAD = 2 * RAD_EENHEDEN * RAD_PX_PER_EENHEID;
	/**
	 * De naaf van het rad (op de tekening van 512 x 512): een donkere ring met daarin de schijf met het
	 * logo uit {@code aanleveren/logo.png}, rechtop, ook als het rad draait (zie {@link #naaf}).
	 */
	static final double NAAF = 82;
	static final double NAAF_SCHIJF = 75;
	/**
	 * Tot zover van het midden reikt het puddingkje, in halve breedtes van het logo: het blaadje
	 * linksboven steekt net buiten de cirkel die in het vierkant past (gemeten: 1,056).
	 */
	static final double LOGO_REIKWIJDTE = 1.06;
	/** Zoveel van de straal van de schijf mag het puddingkje vullen; de rest is ruimte tot de rand. */
	static final double LOGO_MARGE = 0.94;
	/** De randen van het logo lopen over deze breedte (deel van het logo) zacht over in de achtergrond. */
	static final double LOGO_ZACHTE_RAND = 0.06;

	static final String ROOD = "E24B4A";
	static final String BLAUW = "378ADD";
	static final String GROEN = "639922";
	static final String GEEL = "EF9F27";
	/** Dezelfde volgorde als QuizRad.VAKKEN: buren (ook rondom) nooit gelijk. */
	static final String[] VAKKEN = {
			ROOD, BLAUW, GROEN, GEEL, BLAUW, ROOD, GEEL, GROEN,
			ROOD, GROEN, BLAUW, GEEL, GROEN, GEEL, ROOD, BLAUW};

	public static void main(String[] args) throws Exception {
		Path pack = zoekPackMap();
		Path aanleveren = pack.resolve("aanleveren");
		Path assets = pack.resolve("assets").resolve("bootcamp");
		Path fontTex = assets.resolve("textures").resolve("font");
		// Schoon beginnen: een oud plaatje mag niet ongemerkt in de zip blijven.
		leeg(fontTex);
		Files.createDirectories(fontTex);
		Files.createDirectories(assets.resolve("font"));
		Files.createDirectories(aanleveren);

		// 1. De jumpscare-foto's, elk in tegels in een eigen font (schrik_1 t/m schrik_5)
		Files.deleteIfExists(assets.resolve("font").resolve("schrik.json"));
		for (int nr = 1; nr <= FOTOS; nr++) {
			Path foto = zoekFoto(aanleveren, "schrik_" + nr);
			BufferedImage bron;
			if (foto != null) {
				bron = ImageIO.read(foto.toFile());
				if (bron == null) {
					throw new IllegalStateException(foto + " is geen jpg of png die Java kan lezen. Sla hem opnieuw op als png.");
				}
			} else {
				bron = placeholder("foto " + nr + " volgt");
			}
			BufferedImage beeld = schaalFoto(bron);
			System.out.println(foto != null
					? "Foto " + nr + ": " + foto.getFileName() + " (" + bron.getWidth() + " x " + bron.getHeight() + ") -> " + beeld.getWidth() + " x " + beeld.getHeight()
					: "Foto " + nr + ": geen schrik_" + nr + ".png of .jpg in " + aanleveren + ", dus een placeholder.");
			FontJson schrik = new FontJson();
			int tegelPx = SCHRIK_EENHEDEN * SCHRIK_PX_PER_EENHEID;
			int kolommen = (beeld.getWidth() + tegelPx - 1) / tegelPx;
			for (int r = 0; r < 2; r++) {
				for (int c = 0; c < SCHRIK_KOLOMMEN; c++) {
					int code = SCHRIK_EERSTE + r * SCHRIK_RIJ_STAP + c;
					if (c >= kolommen) {
						// Deze foto heeft deze kolom niet nodig: +1, samen met de -1 erachter niks.
						schrik.spatie(code, 1);
						continue;
					}
					int b = Math.min(tegelPx, beeld.getWidth() - c * tegelPx);
					String naam = "schrik_" + nr + "_" + r + "_" + c + ".png";
					schrijfTegel(beeld.getSubimage(c * tegelPx, r * tegelPx, b, tegelPx), fontTex.resolve(naam));
					schrik.tegel("bootcamp:font/" + naam, SCHRIK_EENHEDEN, r == 0 ? SCHRIK_EENHEDEN : 0, code);
				}
			}
			schrik.spatie(TERUG_EEN, -1);
			schrik.spatie(TERUG_RIJ, -beeld.getWidth() / SCHRIK_PX_PER_EENHEID);
			schrijf(assets.resolve("font").resolve("schrik_" + nr + ".json"), schrik.json());
		}

		// 2. De geluiden: de jumpscare, de 8D-klop en het quiz-rad (stereo blijft stereo: Minecraft speelt dat
		// zonder richting af, dus het 8D-effect blijft)
		Path sounds = assets.resolve("sounds");
		Path soundsJson = assets.resolve("sounds.json");
		leeg(sounds);
		List<String> geluiden = new ArrayList<>();
		for (String[] g : new String[][] {{"schrik", "Jumpscare", "false", "1"}, {"klop", "8D-klop", "true", "1"}, {"rad", "Quiz-rad", "false", "0.7"}}) {
			Path ogg = aanleveren.resolve(g[0] + ".ogg");
			if (Files.exists(ogg)) {
				Files.createDirectories(sounds);
				Files.copy(ogg, sounds.resolve(g[0] + ".ogg"), StandardCopyOption.REPLACE_EXISTING);
				// Een lang geluid streamt, dan hoeft het niet helemaal in het geheugen. Een kort geluid laadt
				// al bij het laden van het pack (preload): anders laadt Minecraft het pas bij de eerste keer
				// afspelen, en dan komt de eerste jumpscare te laat.
				String laden = g[2].equals("true") ? "\"stream\": true" : "\"stream\": false, \"preload\": true";
				// Een volume onder 1 maakt het zachter (nooit harder dan het bestand); het rad staat op 0,7.
				if (!g[3].equals("1")) {
					laden += ", \"volume\": " + g[3];
				}
				geluiden.add("  \"" + g[0] + "\": {\"sounds\": [{\"name\": \"bootcamp:" + g[0] + "\", " + laden + "}]}");
				System.out.println(g[1] + ": " + g[0] + ".ogg erbij.");
			} else if (Files.exists(aanleveren.resolve(g[0] + ".wav"))) {
				System.out.println(g[1] + ": " + g[0] + ".wav gevonden, maar Minecraft speelt alleen ogg vorbis. Zet hem om naar "
						+ g[0] + ".ogg (Audacity: Bestand > Exporteren > Exporteren als OGG); tot dan is het stil.");
			} else {
				System.out.println(g[1] + ": geen " + g[0] + ".ogg in " + aanleveren + "; tot dan is het stil.");
			}
		}
		if (geluiden.isEmpty()) {
			Files.deleteIfExists(soundsJson);
		} else {
			schrijf(soundsJson, "{\n" + String.join(",\n", geluiden) + "\n}\n");
		}

		// 3. Het quiz-rad, elke stand in 2 x 2 tegels
		Path logoBestand = zoekFoto(aanleveren, "logo");
		BufferedImage logo = logoBestand == null ? null : ImageIO.read(logoBestand.toFile());
		if (logoBestand != null && logo == null) {
			throw new IllegalStateException(logoBestand + " is geen jpg of png die Java kan lezen. Sla hem opnieuw op als png.");
		}
		BufferedImage naaf = logo == null ? null : naaf(logo);
		System.out.println(logo != null ? "Logo: " + logoBestand.getFileName() + " in het midden van het rad."
				: "Logo: geen logo.png in " + aanleveren + ", dus een gewone dop in het midden van het rad.");
		FontJson rad = new FontJson();
		int radTegel = RAD_EENHEDEN * RAD_PX_PER_EENHEID;
		for (int s = 0; s < STANDEN; s++) {
			BufferedImage stand = tekenRad(s, naaf);
			for (int r = 0; r < 2; r++) {
				for (int c = 0; c < 2; c++) {
					String naam = String.format("rad_%02d_%d_%d.png", s, r, c);
					schrijfTegel(stand.getSubimage(c * radTegel, r * radTegel, radTegel, radTegel), fontTex.resolve(naam));
					rad.tegel("bootcamp:font/" + naam, RAD_EENHEDEN, r == 0 ? RAD_EENHEDEN : 0, RAD_EERSTE + 4 * s + 2 * r + c);
				}
			}
		}
		rad.spatie(TERUG_EEN, -1);
		rad.spatie(TERUG_RIJ, -2 * RAD_EENHEDEN);
		schrijf(assets.resolve("font").resolve("rad.json"), rad.json());
		System.out.println("Quiz-rad: " + STANDEN + " standen getekend, " + (STANDEN * 4) + " tegels.");

		// 4. pack.mcmeta
		schrijf(pack.resolve("pack.mcmeta"), """
				{
				  "pack": {
				    "description": "Pudding Bootcamp",
				    "min_format": %d,
				    "max_format": %d
				  }
				}
				""".formatted(PACK_FORMAT, PACK_FORMAT));

		// 5. Zippen
		Path zip = pack.resolve("bootcamp-pack.zip");
		zip(pack, zip);
		System.out.println();
		System.out.println("Klaar: " + zip.toAbsolutePath());
		System.out.println("Grootte: " + (Files.size(zip) / 1024) + " kB");
		System.out.println("SHA-1:   " + sha1(zip));
		System.out.println();
		System.out.println("Zet de zip online (bijvoorbeeld als bijlage van een GitHub-release) en vul in server.properties in:");
		System.out.println("  resource-pack=<de url van bootcamp-pack.zip>");
		System.out.println("  resource-pack-sha1=" + sha1(zip));
		System.out.println("  require-resource-pack=true");
	}

	/** De providers van één font: een bitmap per tegel en één space-provider. */
	static final class FontJson {
		private final List<String> tegels = new ArrayList<>();
		private final List<String> spaties = new ArrayList<>();

		void tegel(String file, int hoogte, int ascent, int code) {
			tegels.add(String.format("    {\"type\": \"bitmap\", \"file\": \"%s\", \"height\": %d, \"ascent\": %d, \"chars\": [\"%s\"]}",
					file, hoogte, ascent, escape(code)));
		}

		void spatie(int code, int breedte) {
			spaties.add(String.format("\"%s\": %d", escape(code), breedte));
		}

		String json() {
			List<String> alles = new ArrayList<>(tegels);
			alles.add("    {\"type\": \"space\", \"advances\": {" + String.join(", ", spaties) + "}}");
			return "{\n  \"providers\": [\n" + String.join(",\n", alles) + "\n  ]\n}\n";
		}
	}

	/**
	 * De foto op maat voor de tegels: 476 pixels hoog (twee rijen van 238), de breedte een veelvoud
	 * van 7 pixels (een hele font-eenheid). Een heel brede foto wordt kleiner, en dan in het midden
	 * van de twee rijen gezet.
	 */
	static BufferedImage schaalFoto(BufferedImage bron) {
		int hoog = 2 * SCHRIK_EENHEDEN * SCHRIK_PX_PER_EENHEID;
		int maxBreed = SCHRIK_KOLOMMEN * SCHRIK_EENHEDEN * SCHRIK_PX_PER_EENHEID;
		double factor = Math.min((double) hoog / bron.getHeight(), (double) maxBreed / bron.getWidth());
		int h = Math.max(1, (int) Math.round(bron.getHeight() * factor));
		int b = (int) Math.round(bron.getWidth() * factor / SCHRIK_PX_PER_EENHEID) * SCHRIK_PX_PER_EENHEID;
		b = Math.max(SCHRIK_PX_PER_EENHEID, Math.min(maxBreed, b));
		BufferedImage geschaald = verklein(bron, b, h);
		BufferedImage uit = new BufferedImage(b, hoog, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = uit.createGraphics();
		g.drawImage(geschaald, 0, (hoog - h) / 2, null);
		g.dispose();
		return uit;
	}

	/**
	 * Schrijft een tegel. Minecraft rekent de breedte van een glyph tot de meest rechtse kolom met
	 * een zichtbare pixel; daarom krijgt de pixel rechtsonder minstens alfa 1 (onzichtbaar), zodat
	 * elke tegel zijn volle breedte houdt en de tegels naadloos aansluiten.
	 */
	static void schrijfTegel(BufferedImage stuk, Path doel) throws IOException {
		BufferedImage t = new BufferedImage(stuk.getWidth(), stuk.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = t.createGraphics();
		g.drawImage(stuk, 0, 0, null);
		g.dispose();
		int x = t.getWidth() - 1;
		int y = t.getHeight() - 1;
		int argb = t.getRGB(x, y);
		if ((argb >>> 24) == 0) {
			t.setRGB(x, y, 0x01000000);
		}
		ImageIO.write(t, "png", doel.toFile());
	}

	static void leeg(Path map) throws IOException {
		if (!Files.exists(map)) {
			return;
		}
		try (Stream<Path> s = Files.walk(map)) {
			for (Path p : s.sorted(Comparator.reverseOrder()).toList()) {
				Files.delete(p);
			}
		}
	}


	static Path zoekPackMap() {
		if (Files.exists(Path.of("pack", "BouwPack.java"))) {
			return Path.of("pack");
		}
		if (Files.exists(Path.of("BouwPack.java"))) {
			return Path.of(".");
		}
		throw new IllegalStateException("Draai dit vanuit de repo (java pack/BouwPack.java) of vanuit de map pack/.");
	}

	static Path zoekFoto(Path map, String basis) {
		for (String ext : List.of(".png", ".jpg", ".jpeg", ".PNG", ".JPG", ".JPEG")) {
			Path p = map.resolve(basis + ext);
			if (Files.exists(p)) {
				return p;
			}
		}
		return null;
	}

	/** Naar precies deze maat. Verkleinen gaat in stappen van de helft, voor een scherp resultaat. */
	static BufferedImage verklein(BufferedImage bron, int doelB, int doelH) {
		BufferedImage beeld = naarArgb(bron);
		while (beeld.getWidth() / 2 >= doelB && beeld.getHeight() / 2 >= doelH) {
			beeld = schaal(beeld, beeld.getWidth() / 2, beeld.getHeight() / 2);
		}
		return schaal(beeld, doelB, doelH);
	}

	static BufferedImage naarArgb(BufferedImage bron) {
		BufferedImage uit = new BufferedImage(bron.getWidth(), bron.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = uit.createGraphics();
		g.drawImage(bron, 0, 0, null);
		g.dispose();
		return uit;
	}

	static BufferedImage schaal(BufferedImage bron, int b, int h) {
		BufferedImage uit = new BufferedImage(b, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = uit.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.drawImage(bron, 0, 0, b, h, null);
		g.dispose();
		return uit;
	}

	/** Tot de foto er is: een clownsgezicht met een tekst eronder. */
	static BufferedImage placeholder(String tekst) {
		int n = 512;
		BufferedImage beeld = new BufferedImage(n, n, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = mooi(beeld);
		g.setColor(new Color(0x1B1B1F));
		g.fillRect(0, 0, n, n);
		g.setColor(new Color(0xF4E3D0));
		g.fill(new Ellipse2D.Double(96, 70, 320, 360));
		g.setColor(new Color(0xE24B4A));
		g.fill(new Ellipse2D.Double(60, 60, 110, 110));
		g.fill(new Ellipse2D.Double(342, 60, 110, 110));
		g.fill(new Ellipse2D.Double(221, 215, 70, 70));
		g.setColor(Color.BLACK);
		g.fill(new Ellipse2D.Double(176, 170, 40, 50));
		g.fill(new Ellipse2D.Double(296, 170, 40, 50));
		g.setStroke(new BasicStroke(12, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor(new Color(0xB3261E));
		g.draw(new Arc2D.Double(166, 250, 180, 110, 200, 140, Arc2D.OPEN));
		g.setColor(Color.WHITE);
		g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 40));
		FontMetrics fm = g.getFontMetrics();
		g.drawString(tekst, (n - fm.stringWidth(tekst)) / 2, 480);
		g.dispose();
		return beeld;
	}

	static Graphics2D mooi(BufferedImage beeld) {
		Graphics2D g = beeld.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		return g;
	}

	/** Drie keer een vierkante vervaging (samen bijna een gaussische), met de randpixels doorgetrokken. */
	static BufferedImage vervaag(BufferedImage bron, int straal) {
		BufferedImage beeld = naarArgb(bron);
		for (int keer = 0; keer < 3; keer++) {
			beeld = vervaagRichting(vervaagRichting(beeld, straal, true), straal, false);
		}
		return beeld;
	}

	private static BufferedImage vervaagRichting(BufferedImage bron, int straal, boolean liggend) {
		int b = bron.getWidth(), h = bron.getHeight();
		BufferedImage uit = new BufferedImage(b, h, BufferedImage.TYPE_INT_ARGB);
		int n = 2 * straal + 1;
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < b; x++) {
				long a = 0, r = 0, g = 0, bl = 0;
				for (int i = -straal; i <= straal; i++) {
					int sx = liggend ? Math.clamp(x + i, 0, b - 1) : x;
					int sy = liggend ? y : Math.clamp(y + i, 0, h - 1);
					int p = bron.getRGB(sx, sy);
					a += p >>> 24;
					r += (p >> 16) & 255;
					g += (p >> 8) & 255;
					bl += p & 255;
				}
				uit.setRGB(x, y, (int) (a / n) << 24 | (int) (r / n) << 16 | (int) (g / n) << 8 | (int) (bl / n));
			}
		}
		return uit;
	}

	/**
	 * De schijf van de naaf in echte pixels, met het logo erin. Het logo staat zo groot dat het hele
	 * puddingkje binnen de schijf valt ({@link #LOGO_REIKWIJDTE}, {@link #LOGO_MARGE}); dat laat langs
	 * de vier zijden een randje van de schijf vrij. Daar ligt dezelfde afbeelding onder, net groot
	 * genoeg om de hele schijf te vullen, en de randen van het logo lopen er zacht in over: zo vult
	 * de achtergrond van het logo de schijf zonder naad, en wordt er niks van het puddingkje
	 * afgesneden.
	 */
	static BufferedImage naaf(BufferedImage logo) {
		double k = RAD / 512.0;
		int d = (int) Math.round(2 * NAAF_SCHIJF * k);
		int voor = (int) Math.round(d * LOGO_MARGE / LOGO_REIKWIJDTE);
		int achter = d + 2;
		// Flink vervaagd: van de onderste afbeelding wil je alleen de kleuren van de achtergrond zien,
		// geen stukje puddingkje dat door de zachte rand heen piept.
		BufferedImage onder = vervaag(verklein(logo, achter, achter), Math.max(2, (int) Math.round(d * 0.05)));
		BufferedImage boven = verklein(logo, voor, voor);
		// Zachte randen: van doorzichtig op de rand naar heel op LOGO_ZACHTE_RAND naar binnen.
		double zacht = Math.max(1, voor * LOGO_ZACHTE_RAND);
		for (int y = 0; y < voor; y++) {
			for (int x = 0; x < voor; x++) {
				double rand = Math.min(Math.min(x + 0.5, y + 0.5), Math.min(voor - x - 0.5, voor - y - 0.5));
				double f = Math.min(1, rand / zacht);
				f = f * f * (3 - 2 * f);
				int p = boven.getRGB(x, y);
				int alfa = (int) Math.round((p >>> 24) * f);
				boven.setRGB(x, y, (alfa << 24) | (p & 0xFFFFFF));
			}
		}
		BufferedImage schijf = new BufferedImage(d, d, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = mooi(schijf);
		g.drawImage(onder, (d - achter) / 2, (d - achter) / 2, null);
		g.drawImage(boven, (d - voor) / 2, (d - voor) / 2, null);
		// Rond uitknippen, met een gladde rand. Het masker beslaat het hele vlak: DstIn werkt alleen
		// waar er getekend wordt, dus een losse cirkel zou de hoeken laten staan.
		BufferedImage masker = new BufferedImage(d, d, BufferedImage.TYPE_INT_ARGB);
		Graphics2D m = mooi(masker);
		m.setColor(Color.WHITE);
		m.fill(new Ellipse2D.Double(0, 0, d, d));
		m.dispose();
		g.setComposite(java.awt.AlphaComposite.DstIn);
		g.drawImage(masker, 0, 0, null);
		g.dispose();
		return schijf;
	}

	/**
	 * Het rad in stand {@code s}: {@code s} x 5,625 graden met de klok mee gedraaid. Vak {@code i}
	 * heeft zijn midden op {@code i} x 22,5 graden met de klok mee vanaf boven, plus de draaiing.
	 *
	 * @param naaf de schijf met het logo ({@link #naaf}), of {@code null} voor een gewone dop
	 */
	static BufferedImage tekenRad(int stand, BufferedImage naaf) {
		BufferedImage beeld = new BufferedImage(RAD, RAD, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = mooi(beeld);
		// Getekend op 512 x 512 en verkleind naar de maat van de tegels.
		g.scale(RAD / 512.0, RAD / 512.0);
		double cx = 512 / 2.0;
		double cy = 512 / 2.0 + 8;
		double r = 222;
		double vak = 360.0 / VAKKEN.length;
		double draai = stand * (360.0 / STANDEN);
		Color naad = new Color(0x26262B);

		// Schaduw en rand
		g.setColor(new Color(0, 0, 0, 90));
		g.fill(new Ellipse2D.Double(cx - r - 6, cy - r - 2, 2 * r + 12, 2 * r + 12));
		g.setColor(naad);
		g.fill(new Ellipse2D.Double(cx - r - 10, cy - r - 10, 2 * r + 20, 2 * r + 20));

		// De vakken
		for (int i = 0; i < VAKKEN.length; i++) {
			double midden = i * vak + draai;
			// Java2D: 0 graden is rechts, tegen de klok in positief. "Met de klok mee vanaf boven" = 90 - hoek.
			double start = 90 - midden - vak / 2;
			g.setColor(new Color(Integer.parseInt(VAKKEN[i], 16)));
			g.fill(new Arc2D.Double(cx - r, cy - r, 2 * r, 2 * r, start, vak, Arc2D.PIE));
			// Een lichte band langs de binnenrand van elk vak, voor wat diepte.
			g.setColor(new Color(255, 255, 255, 40));
			g.fill(new Arc2D.Double(cx - r * 0.93, cy - r * 0.93, 2 * r * 0.93, 2 * r * 0.93, start, vak, Arc2D.PIE));
			g.setColor(new Color(Integer.parseInt(VAKKEN[i], 16)));
			g.fill(new Arc2D.Double(cx - r * 0.86, cy - r * 0.86, 2 * r * 0.86, 2 * r * 0.86, start, vak, Arc2D.PIE));
		}
		// Naden tussen de vakken en pinnetjes op de rand
		g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		for (int i = 0; i < VAKKEN.length; i++) {
			double grens = Math.toRadians(i * vak + draai + vak / 2);
			double x = cx + Math.sin(grens) * r;
			double y = cy - Math.cos(grens) * r;
			g.setColor(naad);
			g.draw(new Line2D.Double(cx, cy, x, y));
			double px = cx + Math.sin(grens) * (r + 1);
			double py = cy - Math.cos(grens) * (r + 1);
			g.setColor(new Color(0xF2F2F2));
			g.fill(new Ellipse2D.Double(px - 6, py - 6, 12, 12));
		}
		if (naaf == null) {
			// De dop in het midden
			g.setColor(naad);
			g.fill(new Ellipse2D.Double(cx - 38, cy - 38, 76, 76));
			g.setColor(new Color(0xF2F2F2));
			g.fill(new Ellipse2D.Double(cx - 26, cy - 26, 52, 52));
			g.setColor(new Color(0xFFD24A));
			g.fill(new Ellipse2D.Double(cx - 14, cy - 14, 28, 28));
		} else {
			// De naaf: een donkere ring, en daarin de schijf met het logo, rechtop.
			g.setColor(naad);
			g.fill(new Ellipse2D.Double(cx - NAAF, cy - NAAF, 2 * NAAF, 2 * NAAF));
			// De schijf is al op de echte pixelmaat: zonder de schaal van de tekening erop zetten.
			double k = RAD / 512.0;
			AffineTransform oud = g.getTransform();
			g.setTransform(new AffineTransform());
			g.drawImage(naaf, (int) Math.round(cx * k - naaf.getWidth() / 2.0), (int) Math.round(cy * k - naaf.getHeight() / 2.0), null);
			g.setTransform(oud);
		}

		// Het pijltje, vast bovenin, met de punt in het rad
		Polygon pijl = new Polygon();
		double top = cy - r - 30;
		pijl.addPoint((int) Math.round(cx - 24), (int) Math.round(top));
		pijl.addPoint((int) Math.round(cx + 24), (int) Math.round(top));
		pijl.addPoint((int) Math.round(cx), (int) Math.round(cy - r + 34));
		g.setColor(naad);
		g.setStroke(new BasicStroke(10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.draw(pijl);
		g.setColor(Color.WHITE);
		g.fill(pijl);
		g.dispose();
		return beeld;
	}

	static String escape(int code) {
		return String.format("\\u%04X", code);
	}

	static void schrijf(Path p, String tekst) throws IOException {
		Files.createDirectories(p.getParent());
		Files.writeString(p, tekst, StandardCharsets.UTF_8);
	}

	static void zip(Path pack, Path zip) throws IOException {
		List<Path> bestanden = new ArrayList<>();
		bestanden.add(pack.resolve("pack.mcmeta"));
		try (Stream<Path> s = Files.walk(pack.resolve("assets"))) {
			s.filter(Files::isRegularFile).sorted().forEach(bestanden::add);
		}
		Path tijdelijk = zip.resolveSibling(zip.getFileName() + ".tmp");
		try (OutputStream out = Files.newOutputStream(tijdelijk); ZipOutputStream z = new ZipOutputStream(out)) {
			for (Path p : bestanden) {
				String naam = pack.relativize(p).toString().replace('\\', '/');
				ZipEntry e = new ZipEntry(naam);
				// Een vaste tijd: dezelfde invoer geeft dezelfde zip, en dus dezelfde SHA-1.
				e.setTime(315532800000L);
				z.putNextEntry(e);
				Files.copy(p, z);
				z.closeEntry();
			}
		}
		Files.move(tijdelijk, zip, StandardCopyOption.REPLACE_EXISTING);
	}

	static String sha1(Path p) throws IOException, NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("SHA-1");
		md.update(Files.readAllBytes(p));
		return HexFormat.of().formatHex(md.digest());
	}
}
