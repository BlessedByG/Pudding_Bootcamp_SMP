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
import nl.pudding.bootcamp.core.Punt;
import nl.pudding.bootcamp.core.QuizDraai;
import nl.pudding.bootcamp.core.QuizRad;
import nl.pudding.bootcamp.core.QuizStand;
import nl.pudding.bootcamp.core.Regels;
import nl.pudding.bootcamp.core.Rol;
import nl.pudding.bootcamp.core.Ronde;
import nl.pudding.bootcamp.game.Border;
import nl.pudding.bootcamp.game.Planner;
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

/**
 * Ronde 4: de quiz. Iedereen zonder spullen bij de bank van zijn team, de presentator op het podium
 * met vier items: groene wol (goed), rode wol (fout), een nether star (het rad draaien) en een
 * emerald (het puntenmenu, {@link PuntenMenu}). Het rad is een echt rond rad in beeld, uit het
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
	/** De stand die nu in beeld staat; -1 voor geen. */
	private int getoond = -1;
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
			case "punten" -> {
				PuntenMenu.open(speler, quiz);
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

	private static List<ItemStack> quizItems() {
		return List.of(
				item(Items.WOOL.lime(), "Goed", ChatFormatting.GREEN, "goed"),
				item(Items.WOOL.red(), "Fout", ChatFormatting.RED, "fout"),
				item(Items.NETHER_STAR, "Draai het rad", ChatFormatting.GOLD, "draai"),
				item(Items.EMERALD, "Punten geven of afpakken", ChatFormatting.AQUA, "punten"));
	}

	private static void geefItems(ServerPlayer presentator) {
		Items26.haalWeg(presentator, Items26.QUIZ_TAG);
		Inventory inv = presentator.getInventory();
		List<ItemStack> items = quizItems();
		for (int i = 0; i < items.size(); i++) {
			inv.setItem(i, items.get(i));
		}
		presentator.inventoryMenu.broadcastChanges();
	}

	/** Raakt de presentator er toch een kwijt, dan legt de mod hem terug. */
	private static void controleerItems(MinecraftServer server) {
		ServerPlayer p = Spel.presentator(server);
		if (p != null) {
			for (String actie : List.of("goed", "fout", "draai", "punten")) {
				if (!p.getInventory().contains(s -> actie.equals(Items26.tagWaarde(s, Items26.QUIZ_TAG)))) {
					geefItems(p);
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
			int totUitslag = (int) Math.ceil((QuizDraai.UITSLAG - seconden) * QuizDraai.TICKS_PER_SECONDE);
			Mc.titleAllen(server, glyph(s), null, 0, Math.max(20, totUitslag + 5), 0);
		}
	}

	@Override
	public void tick(MinecraftServer server) {
		if (draai == null) {
			return;
		}
		double seconden = (System.nanoTime() - draaiStart) / 1e9;
		if (!draai.uitslag(seconden)) {
			toonDraai(server, seconden);
			return;
		}
		// Het plingeltje in het geluid is het geluid van de uitslag.
		Kleur k = draai.kleur();
		draai = null;
		stand.geland(k);
		Mc.titleAllen(server, Mc.tekst(k.naam().toUpperCase(Locale.ROOT) + " IS AAN DE BEURT", Mc.kleur(k), ChatFormatting.BOLD), null, 0, 50, 15);
		lamp(server, k, true);
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
		// Tien seconden vieren, dan de items weg en iedereen naar de tribune van de Arena voor het Rad.
		Planner.naSeconden(Regels.VIEREN, () -> {
			if (Spel.actief() != this) {
				return;
			}
			Spel.einde(server);
			Bossbar.basiskamp();
			for (ServerPlayer s : Mc.deelnemers(server)) {
				Items26.haalWeg(s, Items26.QUIZ_TAG);
				Spel.zetRol(server, s, Rol.SPELER);
				Tribune.naarTribune(s, Ronde.CLOWN);
			}
		});
		return null;
	}

	// Lampen

	/** Alle vier de lampen uit. */
	public static void lampenUit(MinecraftServer server) {
		for (Kleur k : Kleur.values()) {
			lamp(server, k, false);
		}
	}

	/** De lamp bij de bank van dit team: een redstone lamp, aan of uit zonder de buren bij te werken. */
	private static void lamp(MinecraftServer server, Kleur k, boolean aan) {
		Punt p = Spel.punt("quizlamp_" + k.id());
		if (p == null) {
			return;
		}
		ServerLevel wereld = Mc.wereld(server);
		BlokPos b = p.blokPos();
		BlockPos pos = new BlockPos(b.x(), b.y(), b.z());
		BlockState nu = wereld.getBlockState(pos);
		if (nu.hasProperty(RedstoneLampBlock.LIT) && nu.getValue(RedstoneLampBlock.LIT) != aan) {
			wereld.setBlock(pos, nu.setValue(RedstoneLampBlock.LIT, aan), Block.UPDATE_CLIENTS);
		}
	}

	// Elke seconde

	@Override
	public void seconde(MinecraftServer server) {
		if (fase != Fase.SPELEN) {
			return;
		}
		controleerItems(server);
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
		if (Spel.isPresentator(speler) && fase == Fase.SPELEN) {
			geefItems(speler);
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
		for (Kleur k : Kleur.values()) {
			lamp(server, k, false);
		}
		for (ServerPlayer s : Mc.deelnemers(server)) {
			Items26.haalWeg(s, Items26.QUIZ_TAG);
		}
		Sidebar.weg(server);
	}
}
