package nl.pudding.bootcamp.game.ronde6;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.GameType;
import nl.pudding.bootcamp.Mc;
import nl.pudding.bootcamp.core.BossbarTekst;
import nl.pudding.bootcamp.core.Klassement;
import nl.pudding.bootcamp.core.Punt;
import nl.pudding.bootcamp.core.Regels;
import nl.pudding.bootcamp.core.Rol;
import nl.pudding.bootcamp.core.Ronde;
import nl.pudding.bootcamp.crown.Kroon;
import nl.pudding.bootcamp.crown.Opstelling;
import nl.pudding.bootcamp.game.Arena;
import nl.pudding.bootcamp.game.Border;
import nl.pudding.bootcamp.game.RondeLogica;
import nl.pudding.bootcamp.game.Spel;
import nl.pudding.bootcamp.game.SpelerStatus;
import nl.pudding.bootcamp.game.Spelregels;
import nl.pudding.bootcamp.kits.Kits;
import nl.pudding.bootcamp.tribune.Tribune;
import nl.pudding.bootcamp.visuals.Bossbar;
import nl.pudding.bootcamp.visuals.Sidebar;
import nl.pudding.bootcamp.visuals.Vuurwerk;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Ronde 6: de FFA in de Arena. Iedereen behalve de uitverkorene, ook wie in Clown vs All af was,
 * met dezelfde kit. Geen timer: de laatste die overblijft is King of the SMP Bootcamp. Daarna meteen
 * de kroning op het podium in het midden.
 */
public final class Ffa extends RondeLogica {
	private static final int VUURWERK_STRAAL = 14;
	private static final int VUURPIJLEN_PER_SECONDE = 2;
	private static final int PAARS = 0xAA00AA;

	private enum Fase {
		VECHTEN, KRONING
	}

	private Fase fase = Fase.VECHTEN;
	private final Klassement kills = new Klassement();
	private int bijStart;
	private int laatstGemeld = Integer.MAX_VALUE;
	private int kroning;
	private UUID king;
	private final Set<UUID> netDood = new HashSet<>();

	@Override
	public Ronde ronde() {
		return Ronde.FFA;
	}

	@Override
	public String magStarten(MinecraftServer server) {
		if (deelnemers(server).size() < 2) {
			return "er zijn minstens twee spelers nodig (iedereen behalve de uitverkorene en staff)";
		}
		String fout = Kits.controleer(server, "arena");
		return fout != null ? fout : Arena.controleerPlekken();
	}

	/** Iedereen behalve de uitverkorene, ook wie in ronde 5 af was. */
	private static List<ServerPlayer> deelnemers(MinecraftServer server) {
		List<ServerPlayer> online = Mc.deelnemers(server);
		ServerPlayer clown = Spel.clown(server);
		List<UUID> mee = Regels.ffaDeelnemers(online.stream().map(ServerPlayer::getUUID).toList(),
				clown == null ? null : clown.getUUID());
		return online.stream().filter(s -> mee.contains(s.getUUID())).toList();
	}

	@Override
	public void start(MinecraftServer server) {
		Spelregels.locatorBar(server, false);
		Kroon.wisHits();
		List<ServerPlayer> vechters = deelnemers(server);

		// Clown kijkt vanaf de tribune.
		for (ServerPlayer s : Mc.deelnemers(server)) {
			if (!vechters.contains(s)) {
				Spel.status(s).dood = true;
				Tribune.maakKijker(server, s, Tribune.Spullen.LEGEN, false);
			}
		}
		for (ServerPlayer s : vechters) {
			SpelerStatus st = Spel.status(s);
			st.dood = false;
			st.tribunepunt = null;
			// Een oude kroon gaat eraf: de kit blijft er anders vanaf.
			Kroon.neemAf(s);
			// Geen team: iedereen kan iedereen raken.
			Spel.zetRol(server, s, Rol.FFA);
			s.setGameMode(GameType.ADVENTURE);
			s.removeAllEffects();
			Kroon.toonOpLocator(s, false);
			Mc.heal(s);
		}
		Arena.verdeel(vechters);
		// Dezelfde kit voor iedereen; eigen spullen gaan weg (clear: true).
		Kits.geefAan(server, "arena", vechters);
		bijStart = vechters.size();
		Border.zet(server, Arena.borderRegio());
		toonSidebar(server);
		// Iedereen bevroren tot de commander /ffa go doet.
		Opstelling.wacht(server, vechters);
	}

	/** {@code /ffa go}: de countdown van tien seconden, daarna is iedereen los. */
	public String go(MinecraftServer server) {
		if (!Opstelling.wachtOpGo()) {
			return "er staat niemand klaar";
		}
		Opstelling.go(server, Regels.OPSTELLING, "De FFA begint over", null);
		return null;
	}

	// Vechten

	@Override
	public void onDeath(MinecraftServer server, ServerPlayer speler, DamageSource bron) {
		if (fase != Fase.VECHTEN || Spel.status(speler).rol != Rol.FFA) {
			return;
		}
		netDood.add(speler.getUUID());
		try {
			ServerPlayer killer = Tribune.aanvaller(bron);
			if (killer != null && (killer == speler || Spel.status(killer).rol != Rol.FFA)) {
				killer = null;
			}
			if (killer != null) {
				Spel.status(killer).kills++;
				kills.voegToe(killer.getUUID(), 1);
				toonSidebar(server);
			}
			Spel.status(speler).dood = true;
			Tribune.maakKijker(server, speler, Tribune.Spullen.LEGEN, true);
			int over = Spel.levend(server, Rol.FFA).size();
			Arena.afMelding(server, Mc.naam(speler), ChatFormatting.WHITE, killer == null ? null : Mc.naam(killer), ChatFormatting.WHITE, over);
			laatsten(server, over);
			controleerWinnaar(server);
		} finally {
			netDood.clear();
		}
	}

	@Override
	protected void naQuitDood(MinecraftServer server, ServerPlayer speler) {
		if (fase == Fase.VECHTEN) {
			int over = Spel.levend(server, Rol.FFA).size() - 1;
			Arena.afMelding(server, Mc.naam(speler), ChatFormatting.WHITE, null, ChatFormatting.WHITE, over);
			laatsten(server, over);
		}
	}

	/** Bij drie over LAATSTE DRIE, bij twee LAATSTE TWEE, met de namen. */
	private void laatsten(MinecraftServer server, int over) {
		String titel = Regels.aftelTitle(over);
		if (titel == null || over >= laatstGemeld) {
			return;
		}
		laatstGemeld = over;
		List<String> namen = Spel.levend(server, Rol.FFA).stream().map(Mc::naam).toList();
		String sub = over == 2 && namen.size() == 2 ? namen.get(0) + " tegen " + namen.get(1) : String.join(", ", namen);
		Mc.titleAllenBehalve(server, netDood, Component.literal(titel).withStyle(s -> s.withColor(PAARS).withBold(true)),
				Mc.tekst(sub, ChatFormatting.LIGHT_PURPLE), 5, 50, 15);
		Mc.geluidAllen(server, SoundEvents.WITHER_SPAWN, 0.3f, 1f);
	}

	private void controleerWinnaar(MinecraftServer server) {
		if (fase != Fase.VECHTEN) {
			return;
		}
		List<ServerPlayer> over = Spel.levend(server, Rol.FFA);
		if (over.size() == 1) {
			kroning(server, over.get(0));
		} else if (over.isEmpty()) {
			// Iedereen tegelijk weg (de laatste twee logden uit): de ref beslist.
			Spel.stop(server);
			Mc.chatAllen(server, Mc.tekst("[bootcamp] De FFA heeft geen winnaar: er staat niemand meer. Start hem opnieuw met /ffa start.",
					ChatFormatting.RED));
		}
	}

	/** {@code /ffa krimp}: de border rond de vloer krimpt, iedereen ziet het. */
	public void krimp(MinecraftServer server, int grootte, int seconden) {
		Arena.krimp(server, grootte, seconden);
	}

	// Klok

	@Override
	public void seconde(MinecraftServer server) {
		if (fase == Fase.KRONING) {
			vuurwerk(server);
			if (kroning-- <= 0) {
				Spel.einde(server);
				Bossbar.basiskamp();
			}
			return;
		}
		int over = Spel.levend(server, Rol.FFA).size();
		if (Opstelling.wachtOpGo()) {
			Bossbar.zet(BossbarTekst.FFA_WACHT, BossEvent.BossBarColor.PURPLE, 1f);
			for (ServerPlayer s : Spel.levend(server, Rol.FFA)) {
				Mc.actionbar(s, Mc.tekst(Spel.instellingen().ffaWachttekst(), ChatFormatting.YELLOW));
			}
			return;
		}
		Bossbar.zet(BossbarTekst.ffa(over), BossEvent.BossBarColor.PURPLE, bijStart <= 0 ? 1f : (float) over / bijStart);
		// Niet alleen bij een dood: ook met maar één vechter, of na /bc kijker.
		if (over <= 1) {
			controleerWinnaar(server);
		}
	}

	private void toonSidebar(MinecraftServer server) {
		List<Sidebar.Regel> regels = new ArrayList<>();
		for (Klassement.Regel r : kills.top(15)) {
			regels.add(new Sidebar.Regel(Mc.tekst(Spel.naamVan(r.speler()), ChatFormatting.WHITE), r.score()));
		}
		Sidebar.toonScores(server, "Kills", regels);
	}

	// De kroning

	private void kroning(MinecraftServer server, ServerPlayer winnaar) {
		fase = Fase.KRONING;
		king = winnaar.getUUID();
		kroning = Regels.KRONING_VUURWERK;
		Opstelling.losIedereen(server);
		Border.weg(server);
		// Iedereen op de tribune; de winnaar op het podium met de kroon en de zweefkroon.
		for (ServerPlayer s : Mc.deelnemers(server)) {
			if (s != winnaar && Spel.status(s).rol != Rol.KIJKER) {
				Spel.status(s).dood = true;
				Tribune.maakKijker(server, s, Tribune.Spullen.LEGEN, false);
			}
		}
		winnaar.removeAllEffects();
		Mc.heal(winnaar);
		Kroon.geef(server, winnaar);
		Kroon.zetKing(king);
		Spel.naarPunt(winnaar, "troon");
		Mc.titleAllenBehalve(server, netDood, Mc.tekst("KING OF THE SMP BOOTCAMP", ChatFormatting.GOLD, ChatFormatting.BOLD),
				Mc.kopEnNaam(winnaar, ChatFormatting.YELLOW, ChatFormatting.BOLD), 10, 200, 30);
		Mc.geluidAllen(server, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
		// De bossbar met de King blijft staan tot /bc reset, net als de zweefkroon.
		Bossbar.king(Mc.naam(winnaar));
	}

	/** Twintig seconden vuurpijlen rond het podium. */
	private void vuurwerk(MinecraftServer server) {
		Punt p = Spel.punt("troon");
		if (p == null) {
			return;
		}
		ServerLevel wereld = Mc.wereld(server);
		for (int i = 0; i < VUURPIJLEN_PER_SECONDE; i++) {
			double x = p.x() + (Spel.RANDOM.nextDouble() * 2 - 1) * VUURWERK_STRAAL;
			double z = p.z() + (Spel.RANDOM.nextDouble() * 2 - 1) * VUURWERK_STRAAL;
			Vuurwerk.goud(wereld, x, p.y() + 4 + Spel.RANDOM.nextInt(8), z);
		}
	}

	@Override
	public void onJoin(MinecraftServer server, ServerPlayer speler) {
		if (fase == Fase.KRONING && speler.getUUID().equals(king)) {
			// De winnaar relogt tijdens zijn eigen kroning: hij houdt zijn kroon.
			Kroon.geef(server, speler);
			Spel.naarPunt(speler, "troon");
			return;
		}
		Spel.status(speler).dood = true;
		Tribune.maakKijker(server, speler, Tribune.Spullen.LEGEN, false);
	}

	@Override
	public String statusRegel(MinecraftServer server) {
		return fase == Fase.KRONING ? "kroning van " + Spel.naamVan(king)
				: Spel.levend(server, Rol.FFA).size() + " over" + (Opstelling.wachtOpGo() ? ", wacht op /ffa go" : "");
	}

	@Override
	public void end(MinecraftServer server) {
		// De sidebar met de kills blijft na de kroning staan tot de volgende ronde of /bc reset.
		if (fase != Fase.KRONING) {
			Sidebar.weg(server);
		}
	}
}
