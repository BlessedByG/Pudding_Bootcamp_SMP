import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
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
 * <li>leest de foto van Clown uit {@code pack/aanleveren/} (jpg, jpeg of png, elk formaat) en
 * verkleint hem tot de langste kant hooguit 1024 pixels is, met behoud van de verhouding; zonder
 * foto tekent het een placeholder;</li>
 * <li>zet het lachje ({@code clown_lach.ogg}) erbij als het er is;</li>
 * <li>tekent het quiz-rad: 64 plaatjes van 512 x 512, elk 5,625 graden verder met de klok mee
 * gedraaid, 16 vakken in de teamkleuren, pijltje vast bovenin;</li>
 * <li>schrijft de fonts en {@code pack.mcmeta}, zipt alles naar {@code pack/bootcamp-pack.zip} en
 * print de SHA-1 voor {@code server.properties}.</li>
 * </ol>
 *
 * De volgorde van de vakken en de draairichting zijn dezelfde als in de mod
 * ({@code nl.pudding.bootcamp.core.QuizRad}): op plaatje 0 staat vak 0 onder het pijltje.
 */
public class BouwPack {
	/** Het resource-packformaat van Minecraft 26.2. */
	static final int PACK_FORMAT = 88;
	static final int MAX_FOTO = 1024;
	static final int RAD = 512;
	static final int STANDEN = 64;
	static final char SCHRIK_GLYPH = '';
	static final int RAD_GLYPH = 0xE100;

	/**
	 * Hoe groot de plaatjes in beeld staan. Een title tekent tekst vier keer zo groot; bij de
	 * automatische GUI-schaal op een 1080p-scherm is het scherm 270 hoog. Het rad komt dan op
	 * ongeveer tweederde, de jumpscare vult het scherm. Afstemmen in de eerste test.
	 */
	static final int RAD_HOOGTE = 44;
	static final int SCHRIK_HOOGTE = 68;

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
		Files.createDirectories(fontTex);
		Files.createDirectories(assets.resolve("font"));
		Files.createDirectories(aanleveren);

		// 1. De foto van Clown
		Path foto = zoekFoto(aanleveren);
		BufferedImage clown;
		if (foto != null) {
			BufferedImage bron = ImageIO.read(foto.toFile());
			if (bron == null) {
				throw new IllegalStateException(foto + " is geen jpg of png die Java kan lezen. Sla hem opnieuw op als png.");
			}
			clown = verklein(bron, MAX_FOTO);
			System.out.println("Foto: " + foto.getFileName() + " (" + bron.getWidth() + " x " + bron.getHeight() + ") -> "
					+ clown.getWidth() + " x " + clown.getHeight());
		} else {
			clown = placeholder();
			System.out.println("Foto: geen clown.jpg of clown.png in " + aanleveren + ", dus een placeholder.");
		}
		ImageIO.write(clown, "png", fontTex.resolve("clown.png").toFile());

		// 2. Het lachje
		Path lach = aanleveren.resolve("clown_lach.ogg");
		Path sounds = assets.resolve("sounds");
		Path soundsJson = assets.resolve("sounds.json");
		if (Files.exists(lach)) {
			Files.createDirectories(sounds);
			Files.copy(lach, sounds.resolve("clown_lach.ogg"), StandardCopyOption.REPLACE_EXISTING);
			schrijf(soundsJson, """
					{
					  "clown_lach": {
					    "sounds": ["bootcamp:clown_lach"]
					  }
					}
					""");
			System.out.println("Lachje: clown_lach.ogg erbij.");
		} else {
			Files.deleteIfExists(sounds.resolve("clown_lach.ogg"));
			Files.deleteIfExists(soundsJson);
			System.out.println("Lachje: geen clown_lach.ogg in " + aanleveren + "; de jumpscare is dan stil.");
		}

		// 3. Het quiz-rad
		for (int s = 0; s < STANDEN; s++) {
			ImageIO.write(tekenRad(s), "png", fontTex.resolve(String.format("rad_%02d.png", s)).toFile());
		}
		System.out.println("Quiz-rad: " + STANDEN + " plaatjes getekend.");

		// 4. Fonts en pack.mcmeta
		schrijf(assets.resolve("font").resolve("schrik.json"), """
				{
				  "providers": [
				    {"type": "bitmap", "file": "bootcamp:font/clown.png", "height": %d, "ascent": %d, "chars": ["%s"]}
				  ]
				}
				""".formatted(SCHRIK_HOOGTE, SCHRIK_HOOGTE / 2 - 3, escape(SCHRIK_GLYPH)));
		StringBuilder rad = new StringBuilder("{\n  \"providers\": [\n");
		for (int s = 0; s < STANDEN; s++) {
			rad.append(String.format("    {\"type\": \"bitmap\", \"file\": \"bootcamp:font/rad_%02d.png\", \"height\": %d, \"ascent\": %d, \"chars\": [\"%s\"]}%s\n",
					s, RAD_HOOGTE, RAD_HOOGTE / 2 - 3, escape((char) (RAD_GLYPH + s)), s < STANDEN - 1 ? "," : ""));
		}
		rad.append("  ]\n}\n");
		schrijf(assets.resolve("font").resolve("rad.json"), rad.toString());
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

	static Path zoekPackMap() {
		if (Files.exists(Path.of("pack", "BouwPack.java"))) {
			return Path.of("pack");
		}
		if (Files.exists(Path.of("BouwPack.java"))) {
			return Path.of(".");
		}
		throw new IllegalStateException("Draai dit vanuit de repo (java pack/BouwPack.java) of vanuit de map pack/.");
	}

	static Path zoekFoto(Path map) {
		for (String naam : List.of("clown.png", "clown.jpg", "clown.jpeg", "clown.PNG", "clown.JPG", "clown.JPEG")) {
			Path p = map.resolve(naam);
			if (Files.exists(p)) {
				return p;
			}
		}
		return null;
	}

	/** Verkleint tot de langste kant hooguit {@code max} is; kleiner blijft zoals het is. In stappen, voor een scherp resultaat. */
	static BufferedImage verklein(BufferedImage bron, int max) {
		BufferedImage beeld = naarArgb(bron);
		int langste = Math.max(beeld.getWidth(), beeld.getHeight());
		if (langste <= max) {
			return beeld;
		}
		double factor = (double) max / langste;
		int doelB = Math.max(1, (int) Math.round(beeld.getWidth() * factor));
		int doelH = Math.max(1, (int) Math.round(beeld.getHeight() * factor));
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

	/** Tot de foto er is: een clownsgezicht met "foto volgt". */
	static BufferedImage placeholder() {
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
		String tekst = "foto volgt";
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

	/**
	 * Het rad in stand {@code s}: {@code s} x 5,625 graden met de klok mee gedraaid. Vak {@code i}
	 * heeft zijn midden op {@code i} x 22,5 graden met de klok mee vanaf boven, plus de draaiing.
	 */
	static BufferedImage tekenRad(int stand) {
		BufferedImage beeld = new BufferedImage(RAD, RAD, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = mooi(beeld);
		double cx = RAD / 2.0;
		double cy = RAD / 2.0 + 8;
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
		// De dop in het midden
		g.setColor(naad);
		g.fill(new Ellipse2D.Double(cx - 38, cy - 38, 76, 76));
		g.setColor(new Color(0xF2F2F2));
		g.fill(new Ellipse2D.Double(cx - 26, cy - 26, 52, 52));
		g.setColor(new Color(0xFFD24A));
		g.fill(new Ellipse2D.Double(cx - 14, cy - 14, 28, 28));

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

	static String escape(char c) {
		return String.format("\\u%04X", (int) c);
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
