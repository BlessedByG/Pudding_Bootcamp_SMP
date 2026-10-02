package nl.pudding.bootcamp.game.ronde4;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.state.BlockState;
import nl.pudding.bootcamp.Mc;
import nl.pudding.bootcamp.config.ConfigStore;
import nl.pudding.bootcamp.core.BlokPos;
import nl.pudding.bootcamp.core.BossbarTekst;
import nl.pudding.bootcamp.core.Kleur;
import nl.pudding.bootcamp.core.Lichtshow;
import nl.pudding.bootcamp.core.Punt;
import nl.pudding.bootcamp.core.QuizDraai;
import nl.pudding.bootcamp.core.QuizRad;
import nl.pudding.bootcamp.core.QuizStand;
import nl.pudding.bootcamp.core.Regels;
import nl.pudding.bootcamp.core.Rol;
import nl.pudding.bootcamp.core.Ronde;
import nl.pudding.bootcamp.game.Border;
import nl.pudding.bootcamp.game.Reset;
import nl.pudding.bootcamp.game.RondeLogica;
import nl.pudding.bootcamp.game.Spel;
import nl.pudding.bootcamp.game.Spelregels;
import nl.pudding.bootcamp.kits.Items26;
import nl.pudding.bootcamp.teams.Teams;
import nl.pudding.bootcamp.tribune.Tribune;
import nl.pudding.bootcamp.visuals.Bossbar;
import nl.pudding.bootcamp.visuals.Sidebar;
import nl.pudding.bootcamp.visuals.Vuurwerk;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Ronde 4: de quiz. Iedereen zonder spullen bij de bank van zijn team, de presentator op het podium
 * met vijf items: groene wol (goed), rode wol (fout), een nether star (het rad draaien), een
 * emerald (het puntenmenu, {@link PuntenMenu}) en een barrier (de quiz beëindigen, met een tweede
 * bevestiging in {@link EindeMenu}). Het rad is een echt rond rad in beeld, uit het
 * resource pack; het team waar het op landt is aan de beurt en de lamp bij hun bank brandt. Bij een
 * goed antwoord schieten de twee dispensers bij de bank van dat team een vuurpijl in de teamkleur.
 */
public final class Quiz extends RondeLogica {
	public static final Identifier RAD_FONT = Identifier.fromNamespaceAndPath("bootcamp", "rad");
	/** Het geluid van een draai (spinwheel): het rad volgt zijn tikjes, zie {@link QuizDraai}. */
	private static final Identifier RAD_GELUID_ID = Identifier.fromNamespaceAndPath("bootcamp", "rad");
	private static final Holder<SoundEvent> RAD_GELUID = Holder.direct(SoundEvent.createVariableRangeEvent(RAD_GELUID_ID));
	/** Binnen zoveel ticks na een klik met een quiz-item telt een volgende klik niet. */
	private static final int KLIK_PAUZE = 5;
	/** Zoveel dispensers met vuurwerk staan er bij elke bank. */
	public static final int VUURWERK_PER_BANK = 2;
	private static int laatsteKlik = -1000;

	private enum Fase {
		SPELEN, VIEREN
	}

	private final QuizStand stand = new QuizStand();
	/** De lopende draai, of {@code null}. */
	private QuizDraai draai;
	/** Wanneer de draai (en het geluid) begon, in {@link System#nanoTime()}: echte tijd, ook als de server hapert. */
	private long draaiStart;
	/** Het plaatje dat nu in beeld staat: een stand (0 t/m 63), 100 + vak voor een opgelicht vak, -1 voor geen. */
	private int getoond = -1;
	/** De lampen van het gekozen team zijn al aan (op het plingeltje). */
	private boolean lampAan;
	/** De decorlampen van de hal (R4.6), gezocht bij de start. */
	private Decorlampen decor;
	/** Hoe ver de lichtshow rond is, in rondjes; en waar hij was toen het rad begon te draaien. */
	private double decorRond;
	private double decorBijDraai;
	/** Het team dat de quiz won, voor de lampen tijdens het vieren. */
	private Kleur winnaarKleur;
	private Fase fase = Fase.SPELEN;

	public static void init() {
		UseItemCallback.EVENT.register((player, level, hand) -> {
			if (player instanceof ServerPlayer speler && Items26.heeftTag(player.getItemInHand(hand), Items26.QUIZ_TAG)) {
				gebruik(speler, player.getItemInHand(hand));
				speler.inventoryMenu.sendAllDataToRemote();
				return InteractionResult.FAIL;
			}
			return InteractionResult.PASS;
		});
		// Ook rechtsklik op een blok, zodat de wol nooit als blok wordt neergezet.
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (player instanceof ServerPlayer speler && Items26.heeftTag(player.getItemInHand(hand), Items26.QUIZ_TAG)) {
				gebruik(speler, player.getItemInHand(hand));
				speler.inventoryMenu.sendAllDataToRemote();
				return InteractionResult.FAIL;
			}
			return InteractionResult.PASS;
		});
		Reset.REGISTER.registreer("quizlampen uit", server -> {
			for (Kleur k : Kleur.values()) {
				lamp(server, k, false);
			}
			Decorlampen.zoek(server).allemaal(Mc.wereld(server), false);
		});
	}

	private static void gebruik(ServerPlayer speler, ItemStack stack) {
		if (!(Spel.actief() instanceof Quiz quiz) || !Spel.isPresentator(speler)) {
			return;
		}
		MinecraftServer server = speler.level().getServer();
		// Eén klik op een blok stuurt de client ook als klik in de lucht: die tweede telt niet.
		int nu = server.getTickCount();
		if (nu - laatsteKlik < KLIK_PAUZE) {
			return;
		}
		laatsteKlik = nu;
		String fout = switch (String.valueOf(Items26.tagWaarde(stack, Items26.QUIZ_TAG))) {
			case "goed" -> quiz.goed(server);
			case "fout" -> quiz.fout(server);
			case "draai" -> quiz.draai(server);
			case "arena" -> quiz.naarArena(server);
			case "beurt" -> {
				BeurtMenu.open(speler, quiz);
				yield null;
			}
			case "punten" -> {
				PuntenMenu.open(speler, quiz);
				yield null;
			}
			case "einde" -> {
				EindeMenu.open(speler, quiz);
				yield null;
			}
			default -> null;
		};
		if (fout != null) {
			Mc.actionbar(speler, Mc.tekst(fout, ChatFormatting.RED));
		}
	}

	@Override
	public Ronde ronde() {
		return Ronde.QUIZ;
	}

	@Override
	public String magStarten(MinecraftServer server) {
		if (ConfigStore.get().presentator() == null) {
			return "er is geen presentator (/quiz presentator <speler>)";
		}
		ServerPlayer p = Spel.presentator(server);
		if (p == null) {
			return "de presentator (" + ConfigStore.get().presentator() + ") is niet online";
		}
		if (Mc.isStaff(p)) {
			return "de presentator staat in creative of spectator; zet hem in adventure";
		}
		List<String> zonder = new ArrayList<>();
		for (ServerPlayer s : Mc.deelnemers(server)) {
			if (Teams.keuze(s) == null && !Spel.isPresentator(s)) {
				zonder.add(Mc.naam(s));
			}
		}
		if (!zonder.isEmpty()) {
			return "deze spelers hebben geen team (/bc team <speler> <kleur>): " + String.join(", ", zonder);
		}
		List<String> punten = new ArrayList<>(List.of("quiz_podium"));
		for (Kleur k : Kleur.values()) {
			punten.add("quiz_" + k.id());
		}
		punten.addAll(vuurwerkPunten());
		String fout = Spel.buitenRegio("quiz", punten);
		if (fout != null) {
			return fout;
		}
		for (String naam : vuurwerkPunten()) {
			if (!Mc.wereld(server).getBlockState(blok(naam)).is(Blocks.DISPENSER)) {
				return "punt " + naam + " is geen dispenser (/quiz vuurwerk <kleur> <1|2>, kijkend naar de dispenser)";
			}
		}
		return null;
	}

	@Override
	public void start(MinecraftServer server) {
		Spelregels.locatorBar(server, false);
		for (Kleur k : Kleur.values()) {
			lamp(server, k, false);
		}
		decor = Decorlampen.zoek(server);
		StringBuilder perTeam = new StringBuilder();
		for (Kleur k : Kleur.values()) {
			perTeam.append(perTeam.isEmpty() ? "" : ", ").append(k.naam()).append(' ').append(decor.aantal(k));
		}
		Mc.chatOps(server, Mc.tekst("[bootcamp] Quiz: " + decor.aantal() + " decorlampen gevonden; achter de banken (quizdecor_<kleur>): "
				+ perTeam + ".", ChatFormatting.GRAY));
		for (ServerPlayer s : Mc.deelnemers(server)) {
			// Iedereen heeft na de mob arena alles ingeleverd.
			s.getInventory().clearContent();
			Spel.zetRol(server, s, Rol.SPELER);
			s.setGameMode(GameType.ADVENTURE);
			Mc.heal(s);
			naarPlek(s);
		}
		ServerPlayer presentator = Spel.presentator(server);
		if (presentator != null) {
			geefItems(presentator);
		}
		Border.zet(server, Spel.regio("quiz"));
		toonSidebar(server);
		Bossbar.zet(BossbarTekst.quiz(null), BossEvent.BossBarColor.WHITE, 1f);
	}

	private static void naarPlek(ServerPlayer s) {
		Tribune.naarVerzamelpuntQuiz(s);
	}

	// De items van de presentator

	private static ItemStack item(Item soort, String naam, ChatFormatting kleur, String actie) {
		ItemStack stack = new ItemStack(soort);
		stack.set(DataComponents.ITEM_NAME, Mc.tekst(naam, kleur, ChatFormatting.BOLD));
		Items26.markeer(stack, Items26.QUIZ_TAG, actie);
		return stack;
	}

	/**
	 * De items van de presentator met hun plek in de hotbar (0 t/m 8), in de volgorde van een vraag:
	 * links het rad (toets 1) met ernaast het beurtmenu (2), dan goed en fout naast elkaar (3 en 4),
	 * het puntenmenu (6), en de barrier apart helemaal rechts (9), zodat je hem niet per ongeluk pakt.
	 */
	private static Map<Integer, ItemStack> quizItems() {
		return Map.of(
				0, item(Items.NETHER_STAR, "Draai het rad", ChatFormatting.GOLD, "draai"),
				1, item(Items.COMPASS, "Beurt geven", ChatFormatting.YELLOW, "beurt"),
				2, item(Items.WOOL.lime(), "Goed", ChatFormatting.GREEN, "goed"),
				3, item(Items.WOOL.red(), "Fout", ChatFormatting.RED, "fout"),
				5, item(Items.EMERALD, "Punten geven of afpakken", ChatFormatting.AQUA, "punten"),
				8, item(Items.BARRIER, "Quiz beëindigen", ChatFormatting.DARK_RED, "einde"));
	}

	/** Na de quiz: alleen nog de ender pearl waarmee de presentator iedereen naar de Arena stuurt. */
	private static void geefArenaItem(ServerPlayer presentator) {
		Items26.haalWeg(presentator, Items26.QUIZ_TAG);
		presentator.getInventory().setItem(0, item(Items.ENDER_PEARL, "Iedereen naar de Arena", ChatFormatting.LIGHT_PURPLE, "arena"));
		presentator.inventoryMenu.broadcastChanges();
	}

	private static void geefItems(ServerPlayer presentator) {
		Items26.haalWeg(presentator, Items26.QUIZ_TAG);
		Inventory inv = presentator.getInventory();
		quizItems().forEach(inv::setItem);
		presentator.inventoryMenu.broadcastChanges();
	}

	/** Raakt de presentator er toch een kwijt, dan legt de mod hem terug. */
	private void controleerItems(MinecraftServer server) {
		ServerPlayer p = Spel.presentator(server);
		if (p != null) {
			List<String> nodig = fase == Fase.SPELEN ? List.of("goed", "fout", "draai", "beurt", "punten", "einde") : List.of("arena");
			for (String actie : nodig) {
				if (!p.getInventory().contains(s -> actie.equals(Items26.tagWaarde(s, Items26.QUIZ_TAG)))) {
					if (fase == Fase.SPELEN) {
						geefItems(p);
					} else {
						geefArenaItem(p);
					}
					break;
				}
			}
		}
		// Een gedropt quiz-item verdwijnt meteen.
		List<Entity> weg = new ArrayList<>();
		for (Entity e : Mc.wereld(server).getAllEntities()) {
			if (e instanceof ItemEntity item && Items26.heeftTag(item.getItem(), Items26.QUIZ_TAG)) {
				weg.add(e);
			}
		}
		weg.forEach(Entity::discard);
	}

	// Het rad

	/** {@code /quiz draai} en de nether star. */
	public String draai(MinecraftServer server) {
		if (fase != Fase.SPELEN) {
			return "de quiz is voorbij";
		}
		if (draai != null) {
			return "het rad draait al";
		}
		stand.draai();
		for (Kleur k : Kleur.values()) {
			lamp(server, k, false);
		}
		draai = QuizDraai.willekeurig(Spel.RANDOM);
		draaiStart = System.nanoTime();
		getoond = -1;
		lampAan = false;
		decorBijDraai = decorRond;
		// Geluid en rad starten op dezelfde tick: het rad volgt de tikjes van het geluid.
		Mc.geluidAllen(server, RAD_GELUID, 1f, 1f);
		toonDraai(server, 0);
		Bossbar.zet(BossbarTekst.quiz(null), BossEvent.BossBarColor.WHITE, 1f);
		return null;
	}

	private static Component glyph(int stand) {
		return Component.literal(QuizRad.glyph(stand)).withStyle(s -> s.withFont(new FontDescription.Resource(RAD_FONT)).withoutShadow());
	}

	/** Het plaatje van nu, alleen als de stand veranderd is; hij blijft staan tot de uitslag. */
	private void toonDraai(MinecraftServer server, double seconden) {
		int s = draai.stand(seconden);
		if (s != getoond) {
			getoond = s;
			Mc.titleAllen(server, glyph(s), null, 0, totDeTekst(seconden), 0);
		}
	}

	/** Op het plingeltje: het gekozen vak afwisselend opgelicht en gewoon, tot het team in beeld komt. */
	private void toonOplicht(MinecraftServer server, double seconden) {
		int vak = draai.vak();
		int nu = draai.oplichten(seconden) ? 100 + vak : draai.stand(seconden);
		if (nu != getoond) {
			getoond = nu;
			String tekst = nu >= 100 ? QuizRad.glyphOplicht(vak) : QuizRad.glyph(nu);
			Component beeld = Component.literal(tekst).withStyle(s -> s.withFont(new FontDescription.Resource(RAD_FONT)).withoutShadow());
			Mc.titleAllen(server, beeld, null, 0, totDeTekst(seconden), 0);
		}
	}

	/** Ticks tot het team in beeld komt, plus wat marge: zo lang blijft een plaatje van het rad staan. */
	private static int totDeTekst(double seconden) {
		return Math.max(20, (int) Math.ceil((QuizDraai.TEKST - seconden) * QuizDraai.TICKS_PER_SECONDE) + 5);
	}

	@Override
	public void tick(MinecraftServer server) {
		ServerLevel wereld = Mc.wereld(server);
		if (draai == null) {
			// Een team aan de beurt (of de winnaar): zijn decorlampen branden stil, de rest is uit.
			Kleur team = fase == Fase.VIEREN ? winnaarKleur : stand.aanDeBeurt();
			if (team != null && decor.aantal(team) > 0) {
				decor.team(wereld, team);
				return;
			}
			// Niemand aan de beurt: de decorlampen lopen rustig rond.
			decorRond += Lichtshow.RUST / QuizDraai.TICKS_PER_SECONDE;
			decor.toon(wereld, decorRond);
			return;
		}
		double seconden = (System.nanoTime() - draaiStart) / 1e9;
		if (!draai.uitslag(seconden)) {
			// De decorlampen draaien mee met het rad.
			decor.toon(wereld, decorBijDraai + QuizDraai.rondjes(seconden));
			toonDraai(server, seconden);
			return;
		}
		Kleur k = draai.kleur();
		if (!draai.tekst(seconden)) {
			// Het plingeltje: het gekozen vak en de decorlampen knipperen, de lampen van dat team gaan aan.
			decor.allemaal(wereld, draai.oplichten(seconden));
			if (!lampAan) {
				lampAan = true;
				lamp(server, k, true);
			}
			toonOplicht(server, seconden);
			return;
		}
		// Het geluid is uitgeklonken: het team groot in beeld, en de decorlampen lopen weer rustig verder.
		decorRond = decorBijDraai + QuizDraai.rondjes(seconden);
		draai = null;
		stand.geland(k);
		Mc.titleAllen(server, Mc.tekst(k.naam().toUpperCase(Locale.ROOT) + " IS AAN DE BEURT", Mc.kleur(k), ChatFormatting.BOLD), null, 0, 50, 15);
		bossbar();
	}

	/** Een draai die nog loopt stopt, ook zijn geluid (bij {@code /quiz winnaar} en {@code /quiz stop}). */
	private void stopDraai(MinecraftServer server) {
		if (draai != null) {
			draai = null;
			Mc.stopGeluidAllen(server, RAD_GELUID_ID);
		}
	}

	private boolean draait() {
		return draai != null;
	}

	/** Het team dat nu aan de beurt is, voor het beurtmenu; {@code null} als niemand. */
	Kleur aanDeBeurt() {
		return stand.aanDeBeurt();
	}

	/**
	 * {@code /quiz beurt} en het kompas: dit team is meteen aan de beurt, zonder rad (R4.7). Alleen
	 * zijn lampen gaan aan, en iedereen ziet het team groot in beeld, net als na een draai.
	 */
	public String geefBeurt(MinecraftServer server, Kleur k) {
		if (fase != Fase.SPELEN) {
			return "de quiz is voorbij";
		}
		if (draait()) {
			return "het rad draait nog";
		}
		stand.geefBeurt(k);
		for (Kleur team : Kleur.values()) {
			lamp(server, team, team == k);
		}
		Mc.titleAllen(server, Mc.tekst(k.naam().toUpperCase(Locale.ROOT) + " IS AAN DE BEURT", Mc.kleur(k), ChatFormatting.BOLD), null, 0, 50, 15);
		Mc.geluidAllen(server, SoundEvents.NOTE_BLOCK_PLING, 1f, 1f);
		bossbar();
		return null;
	}

	/** {@code /quiz goed} en de groene wol. */
	public String goed(MinecraftServer server) {
		if (fase != Fase.SPELEN) {
			return "de quiz is voorbij";
		}
		if (draait()) {
			return "het rad draait nog";
		}
		Kleur k = stand.aanDeBeurt();
		if (!stand.goed()) {
			return "Draai eerst het rad";
		}
		Mc.titleAllen(server, Mc.tekst("GOED!", ChatFormatting.GREEN, ChatFormatting.BOLD), Mc.tekst(stand.goedTekst(), Mc.kleur(k)), 0, 40, 10);
		Mc.geluidAllen(server, SoundEvents.NOTE_BLOCK_BELL, 1f, 1f);
		for (int n = 1; n <= VUURWERK_PER_BANK; n++) {
			Vuurwerk.uitDispenser(Mc.wereld(server), blok(vuurwerkPunt(k, n)), k.rgb());
		}
		toonSidebar(server);
		return null;
	}

	/** {@code /quiz fout} en de rode wol. */
	public String fout(MinecraftServer server) {
		if (fase != Fase.SPELEN) {
			return "de quiz is voorbij";
		}
		if (draait()) {
			return "het rad draait nog";
		}
		Kleur k = stand.aanDeBeurt();
		if (!stand.fout()) {
			return "Draai eerst het rad";
		}
		Mc.titleAllen(server, Mc.tekst("FOUT!", ChatFormatting.RED, ChatFormatting.BOLD), null, 0, 40, 10);
		Mc.geluidAllen(server, SoundEvents.VILLAGER_NO, 1f, 1f);
		lamp(server, k, false);
		bossbar();
		return null;
	}

	/** De stand van een team, voor het puntenmenu. */
	int punten(Kleur k) {
		return stand.punten(k);
	}

	/**
	 * Uit het puntenmenu: punten erbij of eraf voor elk team, ook als het niet aan de beurt is, en
	 * onder 0 mag. Iedereen ziet het in de chat, met een geluid.
	 */
	void menuPunt(MinecraftServer server, ServerPlayer presentator, Kleur k, int aantal) {
		if (fase != Fase.SPELEN) {
			return;
		}
		stand.punt(k, aantal);
		Mc.chatAllen(server, Component.empty().append(Mc.tekst(Mc.naam(presentator) + ": ", ChatFormatting.GOLD))
				.append(Mc.tekst((aantal > 0 ? "+" : "") + aantal + " voor " + k.naam(), Mc.kleur(k), ChatFormatting.BOLD))
				.append(Mc.tekst(" (" + stand.punten(k) + ")", ChatFormatting.GRAY)));
		if (aantal > 0) {
			Mc.geluidAllen(server, SoundEvents.NOTE_BLOCK_PLING, 1f, 1.5f);
		} else {
			Mc.geluidAllen(server, SoundEvents.NOTE_BLOCK_BASS, 1f, 0.7f);
		}
		toonSidebar(server);
	}

	/** {@code quizvuurwerk_rood_1}, {@code quizvuurwerk_rood_2}, ... voor elke kleur. */
	public static String vuurwerkPunt(Kleur k, int nummer) {
		return "quizvuurwerk_" + k.id() + "_" + nummer;
	}

	private static List<String> vuurwerkPunten() {
		List<String> uit = new ArrayList<>();
		for (Kleur k : Kleur.values()) {
			for (int n = 1; n <= VUURWERK_PER_BANK; n++) {
				uit.add(vuurwerkPunt(k, n));
			}
		}
		return uit;
	}

	private static BlockPos blok(String punt) {
		BlokPos b = Spel.punt(punt).blokPos();
		return new BlockPos(b.x(), b.y(), b.z());
	}

	/** {@code /quiz punt <kleur> [<aantal>]}: om een verkeerde klik recht te zetten. */
	public void punt(MinecraftServer server, Kleur k, int aantal) {
		stand.punt(k, aantal);
		toonSidebar(server);
	}

	/**
	 * {@code /quiz einde}: het team met de meeste punten wint. Bij gelijke stand ziet iedereen
	 * GELIJKSPEL en weigert hij.
	 *
	 * @return {@code null} als er een winnaar is, anders de teams die gelijk staan
	 */
	public String einde(MinecraftServer server) {
		if (fase != Fase.SPELEN) {
			return "de quiz is al voorbij";
		}
		List<Kleur> leiders = stand.leiders();
		if (leiders.size() > 1) {
			String namen = namen(leiders);
			ServerPlayer p = Spel.presentator(server);
			String presentator = p != null ? Mc.naam(p) : ConfigStore.get().presentator() != null ? ConfigStore.get().presentator() : "de presentator";
			Mc.titleAllen(server, Mc.tekst("GELIJKSPEL", ChatFormatting.GOLD, ChatFormatting.BOLD),
					Mc.tekst(namen + " · " + presentator + " kiest", ChatFormatting.YELLOW), 10, 80, 20);
			return "gelijke stand: " + namen + ". Kies met /quiz winnaar <kleur>";
		}
		return winnaar(server, leiders.get(0));
	}

	private static String namen(List<Kleur> kleuren) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < kleuren.size(); i++) {
			if (i > 0) {
				sb.append(i == kleuren.size() - 1 ? " en " : ", ");
			}
			sb.append(kleuren.get(i).naam());
		}
		return sb.toString();
	}

	/** {@code /quiz winnaar <kleur>}, en het einde als er één team bovenaan staat. */
	public String winnaar(MinecraftServer server, Kleur k) {
		if (fase != Fase.SPELEN) {
			return "de quiz is al voorbij";
		}
		fase = Fase.VIEREN;
		stopDraai(server);
		winnaarKleur = k;
		for (Kleur l : Kleur.values()) {
			lamp(server, l, l == k);
		}
		Mc.titleAllen(server, Mc.tekst(k.naam().toUpperCase(Locale.ROOT) + " WINT DE QUIZ", Mc.kleur(k), ChatFormatting.BOLD),
				Mc.tekst(stand.punten(k) + " goed", ChatFormatting.YELLOW), 10, 120, 20);
		Mc.geluidAllen(server, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
		Punt bank = Spel.punt("quiz_" + k.id());
		if (bank != null) {
			ServerLevel wereld = Mc.wereld(server);
			for (int i = 0; i < 3; i++) {
				Vuurwerk.kleur(wereld, bank.x() + Spel.RANDOM.nextDouble() * 2 - 1, bank.y() + 2, bank.z() + Spel.RANDOM.nextDouble() * 2 - 1, k.rgb());
			}
		}
		Bossbar.zet("Quiz · " + k.naam() + " wint", BossEvent.BossBarColor.YELLOW, 1f);
		// Pudding beslist wanneer iedereen naar de Arena gaat: zijn quiz-items worden een ender pearl.
		ServerPlayer p = Spel.presentator(server);
		if (p != null) {
			geefArenaItem(p);
		}
		return null;
	}

	/**
	 * De ender pearl van de presentator en {@code /quiz naararena}: de quiz is klaar, iedereen gaat
	 * zonder quiz-items naar de tribune van de Arena, voor het Rad.
	 */
	public String naarArena(MinecraftServer server) {
		if (fase != Fase.VIEREN) {
			return "de quiz is nog niet voorbij (/quiz einde)";
		}
		Spel.einde(server);
		Bossbar.basiskamp();
		for (ServerPlayer s : Mc.deelnemers(server)) {
			Items26.haalWeg(s, Items26.QUIZ_TAG);
			Spel.zetRol(server, s, Rol.SPELER);
			Tribune.naarTribune(s, Ronde.CLOWN);
		}
		return null;
	}

	// Lampen

	/** Alle vier de lampen uit. */
	public static void lampenUit(MinecraftServer server) {
		for (Kleur k : Kleur.values()) {
			lamp(server, k, false);
		}
	}

	/**
	 * De lampen bij de bank van dit team ({@code quizlamp_rood_1}, {@code _2}, ...): redstone lampen,
	 * aan of uit zonder de buren bij te werken.
	 */
	private static void lamp(MinecraftServer server, Kleur k, boolean aan) {
		ServerLevel wereld = Mc.wereld(server);
		for (String naam : Ronde.allemaal("quizlamp_" + k.id() + "_", ConfigStore.get().punten().keySet())) {
			BlokPos b = Spel.punt(naam).blokPos();
			BlockPos pos = new BlockPos(b.x(), b.y(), b.z());
			BlockState nu = wereld.getBlockState(pos);
			if (nu.hasProperty(RedstoneLampBlock.LIT) && nu.getValue(RedstoneLampBlock.LIT) != aan) {
				wereld.setBlock(pos, nu.setValue(RedstoneLampBlock.LIT, aan), Block.UPDATE_CLIENTS);
			}
		}
	}

	// Elke seconde

	@Override
	public void seconde(MinecraftServer server) {
		controleerItems(server);
		if (fase == Fase.VIEREN) {
			ServerPlayer p = Spel.presentator(server);
			if (p != null) {
				Mc.actionbar(p, Mc.tekst("Klaar met vieren? De ender pearl stuurt iedereen naar de Arena", ChatFormatting.LIGHT_PURPLE));
			}
			return;
		}
		ServerPlayer p = Spel.presentator(server);
		if (p != null) {
			Mc.actionbar(p, Mc.tekst(draait() ? "Het rad draait" : stand.presentatorTekst(),
					stand.aanDeBeurt() == null ? ChatFormatting.WHITE : Mc.kleur(stand.aanDeBeurt())));
		}
	}

	private void bossbar() {
		Kleur k = stand.aanDeBeurt();
		BossEvent.BossBarColor kleur = k == null ? BossEvent.BossBarColor.WHITE : switch (k) {
			case ROOD -> BossEvent.BossBarColor.RED;
			case BLAUW -> BossEvent.BossBarColor.BLUE;
			case GROEN -> BossEvent.BossBarColor.GREEN;
			case GEEL -> BossEvent.BossBarColor.YELLOW;
		};
		Bossbar.zet(BossbarTekst.quiz(k == null ? null : k.naam()), kleur, 1f);
	}

	private void toonSidebar(MinecraftServer server) {
		List<Kleur> volgorde = new ArrayList<>(List.of(Kleur.values()));
		volgorde.sort((a, b) -> Integer.compare(stand.punten(b), stand.punten(a)));
		List<Sidebar.Regel> regels = new ArrayList<>();
		for (Kleur k : volgorde) {
			MutableComponent naam = Mc.tekst(k.naam(), Mc.kleur(k), ChatFormatting.BOLD);
			regels.add(new Sidebar.Regel(naam, stand.punten(k)));
		}
		Sidebar.toonScores(server, "Quiz", regels);
	}

	/** Geen schade in de quiz. */
	@Override
	public boolean magSchade(ServerPlayer slachtoffer, DamageSource bron) {
		return false;
	}

	@Override
	public void onJoin(MinecraftServer server, ServerPlayer speler) {
		// De quiz doe je zonder spullen, ook wie in de mob arena wegviel.
		speler.getInventory().clearContent();
		speler.inventoryMenu.broadcastChanges();
		if (!Spel.isPresentator(speler)) {
			Teams.zorgVoorTeam(server, speler);
		}
		Spel.zetRol(server, speler, Rol.SPELER);
		speler.setGameMode(GameType.ADVENTURE);
		naarPlek(speler);
		if (Spel.isPresentator(speler)) {
			if (fase == Fase.SPELEN) {
				geefItems(speler);
			} else {
				geefArenaItem(speler);
			}
		}
	}

	@Override
	public String statusRegel(MinecraftServer server) {
		StringBuilder sb = new StringBuilder("quiz:");
		for (Kleur k : Kleur.values()) {
			sb.append(" ").append(k.naam()).append(" ").append(stand.punten(k));
		}
		sb.append(", aan de beurt: ").append(stand.aanDeBeurt() == null ? "niemand" : stand.aanDeBeurt().naam());
		return sb.toString();
	}

	@Override
	public void end(MinecraftServer server) {
		stopDraai(server);
		if (decor != null) {
			decor.allemaal(Mc.wereld(server), false);
		}
		for (Kleur k : Kleur.values()) {
			lamp(server, k, false);
		}
		for (ServerPlayer s : Mc.deelnemers(server)) {
			Items26.haalWeg(s, Items26.QUIZ_TAG);
		}
		Sidebar.weg(server);
	}
}
