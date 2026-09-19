package nl.pudding.bootcamp.game.ronde6;

import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.GameType;
import nl.pudding.bootcamp.Mc;
import nl.pudding.bootcamp.core.BossbarTekst;
import nl.pudding.bootcamp.core.FinaleStand;
import nl.pudding.bootcamp.core.Punt;
import nl.pudding.bootcamp.core.Regels;
import nl.pudding.bootcamp.core.Regio;
import nl.pudding.bootcamp.core.Rol;
import nl.pudding.bootcamp.core.Ronde;
import nl.pudding.bootcamp.crown.Kroon;
import nl.pudding.bootcamp.crown.Opstelling;
import nl.pudding.bootcamp.game.Aftelling;
import nl.pudding.bootcamp.game.Border;
import nl.pudding.bootcamp.game.RondeLogica;
import nl.pudding.bootcamp.game.Spel;
import nl.pudding.bootcamp.game.SpelerStatus;
import nl.pudding.bootcamp.game.Spelregels;
import nl.pudding.bootcamp.kits.Kits;
import nl.pudding.bootcamp.tribune.Tribune;
import nl.pudding.bootcamp.visuals.Bossbar;
import nl.pudding.bootcamp.visuals.Vuurwerk;
import nl.pudding.bootcamp.visuals.Zweefkroon;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Ronde 6: de finale, 1v1 tussen de twee kroondragers, best of 3. Per potje full heal, kit reset en
 * vijf seconden countdown. Duurt een potje langer dan drie minuten, dan krimpt de border in dertig
 * seconden naar 6 x 6. Wie er twee wint is King of the SMP en wordt gekroond.
 */
public final class Finale extends RondeLogica {
	private static final int VUURWERK_STRAAL = 14;
	private static final int VUURPIJLEN_PER_SECONDE = 2;

	private enum Fase {
		POTJE, WACHT_OP_FINALIST, KRONING
	}

	private FinaleStand stand;
	private Fase fase = Fase.POTJE;
	private boolean gekrompen;
	private int wacht;
	private int kroning;
	private UUID winnaar;
	/** Tijdens het disconnect-event staat de speler nog in de spelerslijst; hier houden we bij wie echt weg is. */
	private final Set<UUID> uitgelogd = new HashSet<>();

	@Override
	public Ronde ronde() {
		return Ronde.FINALE;
	}

	@Override
	public String magStarten(MinecraftServer server) {
		UUID een = Spel.finalist1();
		UUID twee = Spel.finalist2();
		if (een == null || twee == null) {
			return "er zijn nog geen twee finalisten: finalist 1 komt uit ronde 4, finalist 2 uit de FFA";
		}
		if (een.equals(twee)) {
			return "finalist 1 en finalist 2 zijn dezelfde speler";
		}
		List<String> weg = new ArrayList<>();
		for (UUID id : List.of(een, twee)) {
			if (server.getPlayerList().getPlayer(id) == null) {
				weg.add(Spel.naamVan(id));
			}
		}
		if (!weg.isEmpty()) {
			return "niet online: " + String.join(", ", weg);
		}
		// De border is 20 x 20 om het midden van regio finale: de startpunten horen daarbinnen.
		Regio midden = Spel.regio("finale");
		List<String> buiten = new ArrayList<>();
		for (String naam : List.of("finale_1", "finale_2")) {
			Punt p = Spel.punt(naam);
			double half = Regels.FINALE_BORDER / 2.0 - 1;
			if (Math.abs(p.x() - midden.centerX()) > half || Math.abs(p.z() - midden.centerZ()) > half) {
				buiten.add(naam);
			}
		}
		if (!buiten.isEmpty()) {
			return "deze punten liggen buiten de border van 20 x 20 om het midden van regio finale: " + String.join(", ", buiten);
		}
		String fout = Kits.controleer(server, "finale");
		return fout != null ? fout : Spel.binnenRegio("vloer", "tribune_1", "tribune_2", "tribune_3", "tribune_4");
	}

	@Override
	public void start(MinecraftServer server) {
		Spelregels.locatorBar(server, true);
		stand = new FinaleStand(Spel.finalist1(), Spel.finalist2());

		// Alle anderen zijn publiek.
		for (ServerPlayer s : Mc.deelnemers(server)) {
			if (stand.isFinalist(s.getUUID())) {
				continue;
			}
			SpelerStatus st = Spel.status(s);
			if (st.rol != Rol.KIJKER || st.tribunepunt == null) {
				st.dood = true;
				Tribune.maakKijker(server, s, Tribune.Spullen.LEGEN, false);
			}
		}
		startPotje(server);
	}

	private ServerPlayer speler(MinecraftServer server, UUID id) {
		return uitgelogd.contains(id) ? null : server.getPlayerList().getPlayer(id);
	}

	/** Per potje: full heal, kit reset, vijf seconden countdown. */
	private void startPotje(MinecraftServer server) {
		ServerPlayer een = speler(server, stand.een());
		ServerPlayer twee = speler(server, stand.twee());
		if (een == null || twee == null) {
			fase = Fase.WACHT_OP_FINALIST;
			wacht = Regels.KONING_UITLOG_WACHT;
			Spel.stopTimer();
			Aftelling.stop();
			Opstelling.losIedereen(server);
			return;
		}
		fase = Fase.POTJE;
		gekrompen = false;
		Spel.stopTimer();

		zetKlaar(server, een, "finale_1");
		zetKlaar(server, twee, "finale_2");
		// De kroon blijft de helm: finale.json heeft geen helm en de kit blijft van de head-slot af.
		Kits.geefAan(server, "finale", List.of(een, twee));
		Border.zet(server, Spel.regio("finale"), Regels.FINALE_BORDER);

		Mc.titleAllen(server, Mc.tekst("POTJE " + stand.potje(), ChatFormatting.GOLD, ChatFormatting.BOLD),
				Mc.tekst(Mc.naam(een) + "  " + stand.winstEen() + " - " + stand.winstTwee() + "  " + Mc.naam(twee), ChatFormatting.YELLOW));
		Opstelling.start(server, List.of(een, twee), Regels.FINALE_COUNTDOWN, "Het potje begint over",
				() -> Spel.startTimer(Regels.FINALE_KRIMP_NA));
	}

	private void zetKlaar(MinecraftServer server, ServerPlayer speler, String punt) {
		SpelerStatus st = Spel.status(speler);
		st.dood = false;
		st.tribunepunt = null;
		speler.setGameMode(GameType.ADVENTURE);
		speler.removeAllEffects();
		// Koning in team king (friendly fire aan): de twee koningen raken elkaar gewoon.
		Kroon.geef(server, speler);
		Mc.heal(speler);
		Spel.naarPunt(speler, punt);
	}

	@Override
	public void onDeath(MinecraftServer server, ServerPlayer speler, DamageSource bron) {
		if (fase != Fase.POTJE || !stand.isFinalist(speler.getUUID())) {
			return;
		}
		potjeVerloren(server, speler.getUUID());
	}

	private void potjeVerloren(MinecraftServer server, UUID verliezer) {
		stand.potjeVerlorenDoor(verliezer);
		Spel.stopTimer();
		if (stand.winnaar() != null) {
			kroning(server, stand.winnaar());
			return;
		}
		String naam = Spel.naamVan(stand.tegenstander(verliezer));
		server.getPlayerList().broadcastSystemMessage(Mc.tekst(naam + " wint het potje: " + stand.winstEen() + " - " + stand.winstTwee(),
				ChatFormatting.GOLD), false);
		startPotje(server);
	}

	/** Na drie minuten krimpt de border in dertig seconden naar 6 x 6. Het potje loopt door. */
	@Override
	public void timerOp(MinecraftServer server) {
		if (fase == Fase.POTJE && !gekrompen) {
			gekrompen = true;
			Border.krimp(server, Regels.FINALE_KRIMP_NAAR, Regels.FINALE_KRIMP_DUUR);
			Mc.titleAllen(server, Mc.tekst("DE BORDER KRIMPT", ChatFormatting.RED, ChatFormatting.BOLD), null);
		}
	}

	// Uitloggen en terugkomen

	@Override
	public void onQuit(MinecraftServer server, ServerPlayer speler) {
		uitgelogd.add(speler.getUUID());
		if (stand != null && stand.isFinalist(speler.getUUID()) && fase == Fase.POTJE) {
			// Het lopende potje gaat naar de tegenstander.
			potjeVerloren(server, speler.getUUID());
		}
	}

	@Override
	public void onJoin(MinecraftServer server, ServerPlayer speler) {
		uitgelogd.remove(speler.getUUID());
		super.onJoin(server, speler);
	}

	@Override
	protected void finalistTerug(MinecraftServer server, ServerPlayer speler) {
		if (fase == Fase.WACHT_OP_FINALIST) {
			startPotje(server);
		} else if (fase == Fase.KRONING && speler.getUUID().equals(winnaar)) {
			// De winnaar relogt tijdens zijn eigen kroning: hij houdt zijn kroon.
			Kroon.geef(server, speler);
			Spel.naarPunt(speler, "kroning");
		} else if (fase == Fase.KRONING) {
			Tribune.maakKijker(server, speler, Tribune.Spullen.LEGEN, false);
		}
	}

	// Klok

	@Override
	public void seconde(MinecraftServer server) {
		switch (fase) {
			case POTJE -> Bossbar.zet(BossbarTekst.finale(stand.winstEen(), stand.winstTwee()), BossEvent.BossBarColor.YELLOW, 1f);
			case WACHT_OP_FINALIST -> {
				UUID weg = speler(server, stand.een()) == null ? stand.een() : stand.twee();
				Bossbar.zet("Wacht op " + Spel.naamVan(weg) + " · " + wacht, BossEvent.BossBarColor.RED, (float) wacht / Regels.KONING_UITLOG_WACHT);
				if (wacht-- <= 0) {
					// Niet terug: ook dit potje gaat naar de tegenstander.
					potjeVerloren(server, weg);
				}
			}
			case KRONING -> {
				Bossbar.zet("King of the SMP: " + Spel.naamVan(winnaar), BossEvent.BossBarColor.YELLOW, 1f);
				vuurwerk(server);
				if (kroning-- <= 0) {
					Spel.stop(server);
				}
			}
		}
	}

	// Kroning

	private void kroning(MinecraftServer server, UUID winnaarId) {
		fase = Fase.KRONING;
		winnaar = winnaarId;
		kroning = Regels.KRONING_VUURWERK;
		Aftelling.stop();
		Opstelling.losIedereen(server);
		Border.weg(server);

		// De verliezer gaat naar de tribune; de tribunes zijn vol.
		UUID verliezerId = stand.tegenstander(winnaarId);
		ServerPlayer verliezer = speler(server, verliezerId);
		if (verliezer == null) {
			// Uitgelogd: zijn zweefkroon hangt er nog, en hij mag straks niet als koning terugkomen.
			Zweefkroon.uit(verliezerId);
			SpelerStatus weg = Spel.status(verliezerId);
			if (weg != null) {
				weg.dood = true;
				weg.rol = Rol.KIJKER;
			}
		}
		if (verliezer != null) {
			Spel.status(verliezer).dood = true;
			Tribune.maakKijker(server, verliezer, Tribune.Spullen.LEGEN, false);
		}
		ServerPlayer koning = speler(server, winnaarId);
		if (koning != null) {
			Kroon.geef(server, koning);
			koning.removeAllEffects();
			Mc.heal(koning);
			Spel.naarPunt(koning, "kroning");
		}
		Mc.titleAllen(server, Mc.tekst("KING OF THE SMP", ChatFormatting.GOLD, ChatFormatting.BOLD),
				Mc.tekst(Spel.naamVan(winnaarId), ChatFormatting.YELLOW, ChatFormatting.BOLD));
		Mc.geluidAllen(server, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
	}

	/** Twintig seconden vuurpijlen boven de Arena. */
	private void vuurwerk(MinecraftServer server) {
		Punt p = Spel.punt("kroning");
		ServerLevel wereld = Mc.wereld(server);
		for (int i = 0; i < VUURPIJLEN_PER_SECONDE; i++) {
			double x = p.x() + (Spel.RANDOM.nextDouble() * 2 - 1) * VUURWERK_STRAAL;
			double z = p.z() + (Spel.RANDOM.nextDouble() * 2 - 1) * VUURWERK_STRAAL;
			Vuurwerk.goud(wereld, x, p.y() + 4 + Spel.RANDOM.nextInt(8), z);
		}
	}
}
