package nl.pudding.bootcamp.game.ronde5;

import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameType;
import nl.pudding.bootcamp.Mc;
import nl.pudding.bootcamp.core.BossbarTekst;
import nl.pudding.bootcamp.core.Regels;
import nl.pudding.bootcamp.core.Regio;
import nl.pudding.bootcamp.core.Rol;
import nl.pudding.bootcamp.core.Ronde;
import nl.pudding.bootcamp.crown.Kroon;
import nl.pudding.bootcamp.crown.Opstelling;
import nl.pudding.bootcamp.game.Border;
import nl.pudding.bootcamp.game.RondeLogica;
import nl.pudding.bootcamp.game.Spel;
import nl.pudding.bootcamp.game.SpelerStatus;
import nl.pudding.bootcamp.game.Spelregels;
import nl.pudding.bootcamp.kits.Kits;
import nl.pudding.bootcamp.tribune.Tribune;
import nl.pudding.bootcamp.visuals.Bossbar;
import nl.pudding.bootcamp.visuals.Vuurwerk;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Ronde 5: de Arena FFA. Iedereen behalve Clown en finalist 1 staat met dezelfde arenakit op de
 * vloer, ook wie in ronde 4 doodging. De laatste die overblijft krijgt de tweede kroon. Daarna
 * twee minuten rust en dan start de finale vanzelf.
 */
public final class Ffa extends RondeLogica {
	private static final int AANTAL_STARTPUNTEN = 4;
	private static final String REGIO_BORDER = "colosseum";

	private enum Fase {
		VECHTEN, RUST
	}

	private Fase fase = Fase.VECHTEN;
	private boolean gekrompen;
	private int rust;
	private final Set<UUID> netDood = new HashSet<>();

	@Override
	public Ronde ronde() {
		return Ronde.FFA;
	}

	@Override
	public String magStarten(MinecraftServer server) {
		if (deelnemers(server).isEmpty()) {
			return "er is niemand om mee te doen: iedereen online is Clown, finalist 1 of staff";
		}
		return Spel.buitenRegio("vloer", "hunter_1", "hunter_2", "hunter_3", "hunter_4");
	}

	/** Iedereen behalve Clown en finalist 1, ook de doden van ronde 4. */
	private static List<ServerPlayer> deelnemers(MinecraftServer server) {
		List<ServerPlayer> online = Mc.deelnemers(server);
		ServerPlayer clown = Spel.clown(server);
		List<UUID> mee = Regels.ffaDeelnemers(online.stream().map(ServerPlayer::getUUID).toList(),
				clown == null ? null : clown.getUUID(), Spel.finalist1());
		return online.stream().filter(s -> mee.contains(s.getUUID())).toList();
	}

	@Override
	public void start(MinecraftServer server) {
		Spelregels.locatorBar(server, true);
		Spel.zetFinalist2(null);
		List<ServerPlayer> vechters = deelnemers(server);

		// Clown en finalist 1 kijken vanaf de tribune.
		for (ServerPlayer s : Mc.deelnemers(server)) {
			if (vechters.contains(s)) {
				continue;
			}
			if (s.getUUID().equals(Spel.finalist1())) {
				zetFinalistOpTribune(server, s);
			} else if (Spel.status(s).rol != Rol.KIJKER || Spel.status(s).tribunepunt == null) {
				Spel.status(s).dood = true;
				Tribune.maakKijker(server, s, Tribune.Spullen.LEGEN, false);
			}
		}

		for (int i = 0; i < vechters.size(); i++) {
			ServerPlayer s = vechters.get(i);
			SpelerStatus st = Spel.status(s);
			st.dood = false;
			st.tribunepunt = null;
			// Teams weg: iedereen kan iedereen raken.
			Spel.zetRol(server, s, Rol.FFA);
			s.setGameMode(GameType.ADVENTURE);
			s.removeEffect(MobEffects.GLOWING);
			Kroon.toonOpLocator(s, false);
			Mc.heal(s);
			Spel.naarPunt(s, "hunter_" + Regels.startpunt(i, AANTAL_STARTPUNTEN));
		}
		// Dezelfde kit voor iedereen; eigen spullen gaan weg (clear: true).
		Kits.geefAan(server, "arena", vechters);

		Border.zet(server, borderRegio());
		Opstelling.start(server, vechters, Regels.FFA_COUNTDOWN, "De FFA begint over",
				() -> Spel.startTimer(ronde().duurSeconden()));
	}

	private static Regio borderRegio() {
		return Spel.regio(REGIO_BORDER) != null ? Spel.regio(REGIO_BORDER) : Spel.regio("vloer");
	}

	private static void zetFinalistOpTribune(MinecraftServer server, ServerPlayer speler) {
		Kroon.maakFinalist(server, speler);
		speler.setGameMode(GameType.ADVENTURE);
		Mc.heal(speler);
		if (Spel.status(speler).tribunepunt == null) {
			Tribune.naarTribune(speler);
		}
	}

	// Vechten

	@Override
	public void onDeath(MinecraftServer server, ServerPlayer speler, DamageSource bron) {
		if (fase != Fase.VECHTEN || Spel.status(speler).rol != Rol.FFA) {
			return;
		}
		netDood.add(speler.getUUID());
		try {
			if (bron.getEntity() instanceof ServerPlayer killer && killer != speler && Spel.status(killer).rol == Rol.FFA) {
				Spel.status(killer).kills++;
			}
			Spel.status(speler).dood = true;
			Tribune.maakKijker(server, speler, Tribune.Spullen.LEGEN, true);
			controleerWinnaar(server);
		} finally {
			netDood.clear();
		}
	}

	@Override
	protected void naQuitDood(MinecraftServer server, ServerPlayer speler) {
		if (fase == Fase.VECHTEN) {
			controleerWinnaar(server);
		}
	}

	private void controleerWinnaar(MinecraftServer server) {
		List<ServerPlayer> over = Spel.levend(server, Rol.FFA);
		if (over.size() == 1) {
			winnaar(server, over.get(0));
		} else if (over.isEmpty()) {
			// Iedereen tegelijk weg (de laatste twee logden uit): geen finalist 2, de ref beslist.
			Spel.stop(server);
			server.getPlayerList().broadcastSystemMessage(
					Mc.tekst("[bootcamp] De FFA heeft geen winnaar: er staat niemand meer. Start hem opnieuw met /bc start 5.", ChatFormatting.RED), false);
		}
	}

	@Override
	public void timerOp(MinecraftServer server) {
		// Hard maximum: staan er nog meerdere, dan beslist het aantal kills.
		List<ServerPlayer> over = Spel.levend(server, Rol.FFA);
		List<Regels.FfaStand> standen = new ArrayList<>();
		for (ServerPlayer s : over) {
			standen.add(new Regels.FfaStand(s.getUUID(), Spel.status(s).kills, s.getHealth()));
		}
		Optional<UUID> beste = Regels.ffaTiebreak(standen, Spel.RANDOM);
		if (beste.isEmpty()) {
			controleerWinnaar(server);
			return;
		}
		ServerPlayer winnaar = server.getPlayerList().getPlayer(beste.get());
		for (ServerPlayer s : over) {
			if (s != winnaar) {
				Spel.status(s).dood = true;
				Tribune.maakKijker(server, s, Tribune.Spullen.LEGEN, false);
			}
		}
		winnaar(server, winnaar);
	}

	/** De laatste die overblijft krijgt de tweede kroon; daarna twee minuten rust. */
	private void winnaar(MinecraftServer server, ServerPlayer speler) {
		fase = Fase.RUST;
		rust = Regels.RUST;
		Spel.stopTimer();
		Opstelling.losIedereen(server);
		Border.weg(server);
		Spel.zetFinalist2(speler.getUUID());

		Vuurwerk.goud(Mc.wereld(server), speler.getX(), speler.getY() + 2, speler.getZ());
		Mc.heal(speler);
		speler.getInventory().clearContent();
		Kroon.geef(server, speler);
		Kroon.maakFinalist(server, speler);

		// De finalisten staan naast elkaar op de tribune.
		UUID eerste = Spel.finalist1();
		SpelerStatus eersteStatus = eerste == null ? null : Spel.status(eerste);
		if (eersteStatus != null && eersteStatus.tribunepunt != null) {
			Spel.status(speler).tribunepunt = eersteStatus.tribunepunt;
			Spel.naarPunt(speler, eersteStatus.tribunepunt);
		} else {
			Tribune.naarTribune(speler);
		}

		Mc.titleAllenBehalve(server, netDood, Mc.tekst("FINALIST", ChatFormatting.GOLD, ChatFormatting.BOLD),
				Mc.tekst(Mc.naam(speler), ChatFormatting.YELLOW, ChatFormatting.BOLD));
		Mc.geluidAllen(server, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
	}

	@Override
	public void seconde(MinecraftServer server) {
		if (fase == Fase.RUST) {
			Bossbar.zet(BossbarTekst.rust(rust), BossEvent.BossBarColor.WHITE, (float) rust / Regels.RUST);
			if (rust-- <= 0) {
				String fout = Spel.start(server, Ronde.FINALE);
				if (fout != null) {
					Spel.stop(server);
					server.getPlayerList().broadcastSystemMessage(
							Mc.tekst("[bootcamp] De finale start niet, " + fout + ". Los het op en doe /bc start 6.", ChatFormatting.RED), false);
				}
			}
			return;
		}
		int over = Spel.levend(server, Rol.FFA).size();
		int tijd = Spel.timerLoopt() ? Spel.timer() : ronde().duurSeconden();
		Bossbar.zet(BossbarTekst.ffa(over, tijd), BossEvent.BossBarColor.PURPLE, (float) tijd / ronde().duurSeconden());

		// Na vijf minuten krimpt de border in twee minuten naar 10 x 10, zodat het niet blijft hangen.
		if (!gekrompen && Spel.timerLoopt() && Spel.timer() <= ronde().duurSeconden() - Regels.FFA_KRIMP_NA) {
			gekrompen = true;
			Border.krimp(server, Regels.FFA_KRIMP_NAAR, Regels.FFA_KRIMP_DUUR);
			Mc.titleAllen(server, Mc.tekst("DE BORDER KRIMPT", ChatFormatting.RED, ChatFormatting.BOLD), null);
		}
	}

	@Override
	protected void finalistTerug(MinecraftServer server, ServerPlayer speler) {
		zetFinalistOpTribune(server, speler);
	}
}
