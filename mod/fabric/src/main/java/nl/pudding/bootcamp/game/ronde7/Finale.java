package nl.pudding.bootcamp.game.ronde7;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.GameType;
import nl.pudding.bootcamp.Mc;
import nl.pudding.bootcamp.config.ConfigStore;
import nl.pudding.bootcamp.core.BootcampConfig;
import nl.pudding.bootcamp.core.BossbarTekst;
import nl.pudding.bootcamp.core.Punt;
import nl.pudding.bootcamp.core.Regels;
import nl.pudding.bootcamp.core.Rol;
import nl.pudding.bootcamp.core.Ronde;
import nl.pudding.bootcamp.crown.Kroon;
import nl.pudding.bootcamp.crown.Opstelling;
import nl.pudding.bootcamp.game.Aftelling;
import nl.pudding.bootcamp.game.Arena;
import nl.pudding.bootcamp.game.Border;
import nl.pudding.bootcamp.game.Reset;
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
 * Ronde 7: de finale in de Arena, één tegen één met de FFA-kit. De winnaar van King of the Hill
 * tegen de winnaar van de FFA; won één speler allebei, dan tegen de nummer twee van de FFA. De rest
 * kijkt vanaf de tribune. Wie wint is King of the SMP Bootcamp: meteen de kroning op het podium,
 * twintig seconden vuurwerk, en dan gaat iedereen samen naar het basiskamp.
 * Logt een finalist uit, dan pauzeert de finale tot de staff kiest: combat log (de ander wint) of
 * crash (de finale stopt en de commander start hem opnieuw).
 */
public final class Finale extends RondeLogica {
	private static final int VUURWERK_STRAAL = 14;
	private static final int VUURPIJLEN_PER_SECONDE = 2;

	private enum Fase {
		VECHTEN, PAUZE, KRONING
	}

	private Fase fase = Fase.VECHTEN;
	/** King of the Hill eerst ({@code finale_1}), dan de FFA ({@code finale_2}). */
	private final List<UUID> finalisten = new ArrayList<>();
	private int kroning;
	private UUID king;
	/** In de pauze: de finalist die uitlogde. */
	private UUID weg;
	private String wegNaam;
	private final Set<UUID> netDood = new HashSet<>();
	/** Na het vuurwerk van de kroning is iedereen naar het basiskamp; tot de volgende {@code /finale start}. */
	private static boolean naarBasiskamp;

	public static void init() {
		Reset.REGISTER.registreer("uitslag voor de finale", server -> {
			ConfigStore.get().wisUitslag();
			ConfigStore.bewaar();
		});
	}

	@Override
	public Ronde ronde() {
		return Ronde.FINALE;
	}

	/** R6.6: de namen van de finalisten uit de uitslag, King of the Hill eerst. Leeg als er iemand ontbreekt. */
	public static List<String> namen() {
		BootcampConfig c = ConfigStore.get();
		return Regels.finalisten(c.winnaarKing(), c.winnaarFfa(), c.tweedeFfa());
	}

	@Override
	public String magStarten(MinecraftServer server) {
		List<String> namen = namen();
		if (namen.isEmpty()) {
			return "er zijn nog geen twee finalisten; speel eerst King of the Hill en de FFA, of zet ze met /finale spelers";
		}
		for (String naam : namen) {
			ServerPlayer s = server.getPlayerList().getPlayerByName(naam);
			if (s == null) {
				return naam + " is niet online";
			}
			if (Mc.isStaff(s)) {
				return naam + " staat in creative of spectator";
			}
		}
		return Kits.controleer(server, "arena");
	}

	@Override
	public void start(MinecraftServer server) {
		naarBasiskamp = false;
		Spelregels.locatorBar(server, false);
		Kroon.wisHits();
		// De kills van de FFA zijn klaar.
		Sidebar.weg(server);
		List<ServerPlayer> spelers = new ArrayList<>();
		for (String naam : namen()) {
			spelers.add(server.getPlayerList().getPlayerByName(naam));
		}
		spelers.forEach(s -> finalisten.add(s.getUUID()));

		// De rest kijkt vanaf de tribune.
		for (ServerPlayer s : Mc.deelnemers(server)) {
			if (!finalisten.contains(s.getUUID())) {
				Spel.status(s).dood = true;
				Tribune.maakKijker(server, s, Tribune.Spullen.LEGEN, false);
			}
		}
		for (int i = 0; i < spelers.size(); i++) {
			ServerPlayer s = spelers.get(i);
			SpelerStatus st = Spel.status(s);
			st.dood = false;
			st.tribunepunt = null;
			// Een oude kroon gaat eraf: de kit blijft er anders vanaf.
			Kroon.neemAf(s);
			Spel.zetRol(server, s, Rol.FFA);
			s.setGameMode(GameType.ADVENTURE);
			s.removeAllEffects();
			Kroon.toonOpLocator(s, false);
			Mc.heal(s);
			Spel.naarPunt(s, "finale_" + (i + 1));
		}
		// Dezelfde kit als de FFA; eigen spullen gaan weg (clear: true).
		Kits.geefAan(server, "arena", spelers);
		Arena.zetBorder(server);

		MutableComponent tegen = Component.empty().append(Mc.kopEnNaam(spelers.get(0), ChatFormatting.YELLOW))
				.append(Mc.tekst(" tegen ", ChatFormatting.WHITE))
				.append(Mc.kopEnNaam(spelers.get(1), ChatFormatting.YELLOW));
		Mc.titleAllen(server, Mc.tekst("DE FINALE", ChatFormatting.GOLD, ChatFormatting.BOLD), tegen, 10, 80, 20);
		Mc.geluidAllen(server, SoundEvents.RAID_HORN, 1f, 1f);
		// Allebei bevroren tot de commander /finale go doet.
		Opstelling.wacht(server, spelers);
	}

	/** {@code /finale go}: de countdown van tien seconden, daarna zijn ze los. */
	public String go(MinecraftServer server) {
		if (fase == Fase.PAUZE) {
			return wegNaam + " is weg; kies eerst /finale combatlog of /finale crash";
		}
		if (!Opstelling.wachtOpGo()) {
			return "er staat niemand klaar";
		}
		Opstelling.go(server, Regels.OPSTELLING, "De finale begint over", null);
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
			if (killer == speler) {
				killer = null;
			}
			Spel.status(speler).dood = true;
			Tribune.maakKijker(server, speler, Tribune.Spullen.LEGEN, true);
			Arena.afMelding(server, Mc.naam(speler), ChatFormatting.WHITE, killer == null ? null : Mc.naam(killer),
					ChatFormatting.WHITE, Spel.levend(server, Rol.FFA).size());
			controleerWinnaar(server);
		} finally {
			netDood.clear();
		}
	}

	/**
	 * R7.1: een finalist logt uit. Geen winnaar door een crash, maar ook geen vrije uitweg voor een
	 * combat log: de finale pauzeert, de ander staat stil, en de staff kiest.
	 */
	@Override
	public void onQuit(MinecraftServer server, ServerPlayer speler) {
		SpelerStatus st = Spel.status(speler);
		if (fase != Fase.VECHTEN || st.dood
				|| Regels.bijQuit(ronde(), st.rol, false) != Regels.QuitActie.PAUZE) {
			return;
		}
		fase = Fase.PAUZE;
		weg = speler.getUUID();
		wegNaam = Mc.naam(speler);
		// Een lopende countdown stopt; de ander staat stil tot er gekozen is.
		Aftelling.stop();
		Opstelling.wacht(server, Spel.levend(server, Rol.FFA).stream().filter(s -> s != speler).toList());
		Bossbar.zet(BossbarTekst.finalePauze(wegNaam), BossEvent.BossBarColor.RED, 1f);
		String ander = anderNaam();
		Mc.chatOps(server, Mc.tekst("[bootcamp] " + wegNaam + " is uitgelogd tijdens de finale. Combat log: /finale combatlog ("
				+ ander + " wint). Crash: /finale crash (de finale stopt; is " + wegNaam + " terug, dan /finale start en /finale go).",
				ChatFormatting.GOLD));
	}

	/** {@code /finale combatlog}: wie uitlogde heeft verloren, de ander krijgt meteen de kroning. */
	public String combatlog(MinecraftServer server) {
		if (fase != Fase.PAUZE) {
			return "er is geen finalist uitgelogd";
		}
		ServerPlayer winnaar = ander(server);
		if (winnaar == null) {
			return "de andere finalist is ook weg; kies /finale crash";
		}
		SpelerStatus st = Spel.status(weg);
		if (st != null) {
			st.dood = true;
		}
		kroning(server, winnaar);
		return null;
	}

	/** {@code /finale crash}: de finale stopt zonder winnaar; de commander start hem opnieuw. */
	public String crash(MinecraftServer server) {
		if (fase != Fase.PAUZE) {
			return "er is geen finalist uitgelogd";
		}
		String naam = wegNaam;
		Spel.stop(server);
		Mc.chatAllen(server, Mc.tekst("[bootcamp] De finale is gestopt en begint opnieuw zodra " + naam + " terug is.",
				ChatFormatting.YELLOW));
		return null;
	}

	/** In de pauze: wie uitlogde. */
	public String wegNaam() {
		return wegNaam;
	}

	/** De finalist die niet uitlogde, als hij nog online is en meedoet. */
	private ServerPlayer ander(MinecraftServer server) {
		for (UUID id : finalisten) {
			ServerPlayer s = server.getPlayerList().getPlayer(id);
			if (!id.equals(weg) && s != null && Spel.status(s).rol == Rol.FFA && !Spel.status(s).dood) {
				return s;
			}
		}
		return null;
	}

	private String anderNaam() {
		for (UUID id : finalisten) {
			if (!id.equals(weg)) {
				return Spel.naamVan(id);
			}
		}
		return "?";
	}

	private void controleerWinnaar(MinecraftServer server) {
		if (fase != Fase.VECHTEN) {
			return;
		}
		List<ServerPlayer> over = Spel.levend(server, Rol.FFA);
		if (over.size() == 1) {
			kroning(server, over.get(0));
		} else if (over.isEmpty()) {
			// Allebei tegelijk dood: de ref beslist.
			Spel.stop(server);
			Mc.chatAllen(server, Mc.tekst("[bootcamp] De finale heeft geen winnaar: er staat niemand meer. Start hem opnieuw met /finale start.",
					ChatFormatting.RED));
		}
	}

	/** {@code /finale krimp}: de border rond de vloer krimpt, iedereen ziet het. */
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
				naarBasiskamp = true;
				for (ServerPlayer s : Mc.deelnemers(server)) {
					naarHetBasiskamp(server, s);
				}
			}
			return;
		}
		if (fase == Fase.PAUZE) {
			// Geen winnaar controleren: wie uitlogde telt niet als dood.
			Bossbar.zet(BossbarTekst.finalePauze(wegNaam), BossEvent.BossBarColor.RED, 1f);
			for (ServerPlayer s : Spel.levend(server, Rol.FFA)) {
				Mc.actionbar(s, Mc.tekst(wegNaam + " is weg · even wachten", ChatFormatting.YELLOW));
			}
			return;
		}
		if (Opstelling.wachtOpGo()) {
			Bossbar.zet(BossbarTekst.FINALE_WACHT, BossEvent.BossBarColor.RED, 1f);
			for (ServerPlayer s : Spel.levend(server, Rol.FFA)) {
				Mc.actionbar(s, Mc.tekst(Spel.instellingen().finaleWachttekst(), ChatFormatting.YELLOW));
			}
			return;
		}
		Bossbar.zet(BossbarTekst.finale(naam(0), naam(1)), BossEvent.BossBarColor.RED, 1f);
		// Niet alleen bij een dood: ook na /bc kijker.
		controleerWinnaar(server);
	}

	private String naam(int i) {
		return i < finalisten.size() ? Spel.naamVan(finalisten.get(i)) : "?";
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

	/** Of de kroning voorbij is en iedereen naar het basiskamp is; dan is dat ook waar wie inlogt heen gaat. */
	public static boolean naarBasiskamp() {
		return naarBasiskamp;
	}

	/**
	 * Na het vuurwerk: weer gewoon speler, met de anderen in het basiskamp. De King houdt zijn kroon,
	 * zweefkroon en bossbar (tot {@code /bc reset}).
	 */
	public static void naarHetBasiskamp(MinecraftServer server, ServerPlayer speler) {
		SpelerStatus st = Spel.status(speler);
		st.dood = false;
		st.tribunepunt = null;
		Spel.zetRol(server, speler, Kroon.isKing(speler) ? Rol.KROON : Rol.SPELER);
		speler.setGameMode(GameType.ADVENTURE);
		Spel.naarPunt(speler, "basiskamp");
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
		if (fase == Fase.PAUZE && speler.getUUID().equals(weg)) {
			Mc.chatOps(server, Mc.tekst("[bootcamp] " + wegNaam + " is terug (op de tribune). Opnieuw: /finale crash, dan /finale start"
					+ " en /finale go. Of toch /finale combatlog.", ChatFormatting.GOLD));
		}
	}

	@Override
	public String statusRegel(MinecraftServer server) {
		if (fase == Fase.PAUZE) {
			return "gepauzeerd: " + wegNaam + " is weg, kies /finale combatlog of /finale crash";
		}
		return fase == Fase.KRONING ? "kroning van " + Spel.naamVan(king)
				: naam(0) + " tegen " + naam(1) + (Opstelling.wachtOpGo() ? ", wacht op /finale go" : "");
	}
}
