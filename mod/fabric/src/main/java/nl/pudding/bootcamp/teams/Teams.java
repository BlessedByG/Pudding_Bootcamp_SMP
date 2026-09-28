package nl.pudding.bootcamp.teams;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.TeamColor;
import nl.pudding.bootcamp.Mc;
import nl.pudding.bootcamp.config.ConfigStore;
import nl.pudding.bootcamp.core.Kleur;
import nl.pudding.bootcamp.core.TeamKeuze;
import nl.pudding.bootcamp.game.Reset;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * De scoreboard-teams uit docs/04: {@code spelers} (wit), de vier teamkleuren, {@code jagers}
 * (aqua), {@code kroon} (goud) en {@code out} (grijs). Ze zijn er voor de kleur van naam,
 * Glowing-outline en locator-stip; wie wie mag raken beslist de PvP-regel, niet het team. Friendly
 * fire staat overal uit.
 *
 * <p>De teamkeuze staat per naam in {@code bootcamp.json}, zodat een herstart tussen ronde 1 en 5
 * de teams niet kwijtraakt.
 */
public final class Teams {
	public static final String SPELERS = "spelers";
	public static final String JAGERS = "jagers";
	public static final String KROON = "kroon";
	public static final String OUT = "out";

	/** Het maximum per team, vastgezet op het moment dat de eerste kiest; {@code 0} = nog niet. */
	private static int maximum;

	private Teams() {
	}

	public static void init() {
		Reset.REGISTER.registreer("teams en teamkeuzes", server -> {
			ConfigStore.get().teams().clear();
			ConfigStore.bewaar();
			maximum = 0;
			zorgDatZeBestaan(server);
			for (ServerPlayer s : Mc.deelnemers(server)) {
				zet(server, s, SPELERS);
			}
		});
	}

	/** Een nieuw doolhof is een nieuwe teamkeuze: alle keuzes weg, het maximum opnieuw. */
	public static void wisKeuzes() {
		ConfigStore.get().teams().clear();
		ConfigStore.bewaar();
		maximum = 0;
	}

	public static void zorgDatZeBestaan(MinecraftServer server) {
		maak(server, SPELERS, TeamColor.WHITE);
		maak(server, Kleur.ROOD.id(), TeamColor.RED);
		maak(server, Kleur.BLAUW.id(), TeamColor.BLUE);
		maak(server, Kleur.GROEN.id(), TeamColor.GREEN);
		maak(server, Kleur.GEEL.id(), TeamColor.YELLOW);
		maak(server, JAGERS, TeamColor.AQUA);
		maak(server, KROON, TeamColor.GOLD);
		maak(server, OUT, TeamColor.GRAY);
	}

	private static void maak(MinecraftServer server, String naam, TeamColor kleur) {
		ServerScoreboard bord = server.getScoreboard();
		PlayerTeam team = bord.getPlayerTeam(naam);
		if (team == null) {
			team = bord.addPlayerTeam(naam);
		}
		team.setColor(Optional.of(kleur));
		team.setAllowFriendlyFire(false);
	}

	public static void zet(MinecraftServer server, ServerPlayer speler, String teamNaam) {
		ServerScoreboard bord = server.getScoreboard();
		PlayerTeam team = bord.getPlayerTeam(teamNaam);
		if (team == null) {
			zorgDatZeBestaan(server);
			team = bord.getPlayerTeam(teamNaam);
		}
		bord.addPlayerToTeam(speler.getScoreboardName(), team);
	}

	/** Ronde 6: zonder team. */
	public static void uitTeam(MinecraftServer server, ServerPlayer speler) {
		ServerScoreboard bord = server.getScoreboard();
		if (bord.getPlayersTeam(speler.getScoreboardName()) != null) {
			bord.removePlayerFromTeam(speler.getScoreboardName());
		}
	}

	// De teamkeuze

	/** De gekozen kleur van deze speler, of {@code null}. */
	public static Kleur keuze(ServerPlayer speler) {
		return ConfigStore.get().teamVan(Mc.naam(speler));
	}

	/** Hoeveel spelers deze kleur gekozen hebben, ook wie offline is. */
	public static int aantal(Kleur k) {
		return ConfigStore.get().aantalInTeam(k);
	}

	public static Map<Kleur, Integer> aantallen() {
		Map<Kleur, Integer> uit = new EnumMap<>(Kleur.class);
		for (Kleur k : Kleur.values()) {
			uit.put(k, aantal(k));
		}
		return uit;
	}

	/**
	 * Het maximum per team. Vastgezet bij de eerste keuze, met iedereen die dan meedoet; daarvoor
	 * het maximum voor wie er nu is.
	 */
	public static int maximum(MinecraftServer server) {
		return maximum > 0 ? maximum : TeamKeuze.maximum(Mc.deelnemers(server).size());
	}

	public static boolean vol(MinecraftServer server, Kleur k) {
		return TeamKeuze.vol(aantal(k), maximum(server));
	}

	/**
	 * Zet de keuze en de teamkleur. Het maximum controleert wie dit aanroept (het menu wel,
	 * {@code /bc team} niet).
	 */
	public static void kies(MinecraftServer server, ServerPlayer speler, Kleur k) {
		if (maximum == 0 && k != null) {
			maximum = TeamKeuze.maximum(Mc.deelnemers(server).size());
		}
		ConfigStore.get().zetTeam(Mc.naam(speler), k);
		ConfigStore.bewaar();
		zet(server, speler, k == null ? SPELERS : k.id());
	}

	/** {@code /bc team} voor iemand die offline is: alleen de keuze. */
	public static void kiesOpNaam(String naam, Kleur k) {
		ConfigStore.get().zetTeam(naam, k);
		ConfigStore.bewaar();
	}

	/** De online deelnemers met deze kleur. */
	public static List<ServerPlayer> leden(MinecraftServer server, Kleur k) {
		List<ServerPlayer> uit = new ArrayList<>();
		for (ServerPlayer s : Mc.deelnemers(server)) {
			if (keuze(s) == k) {
				uit.add(s);
			}
		}
		return uit;
	}

	/** Het scoreboard-team dat past bij een speler zonder bijzondere rol: zijn kleur, of wit. */
	public static String teamVoorSpeler(ServerPlayer speler, boolean metKleuren) {
		Kleur k = metKleuren ? keuze(speler) : null;
		return k == null ? SPELERS : k.id();
	}
}
