package nl.pudding.bootcamp.game;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.TeamColor;
import nl.pudding.bootcamp.Mc;

import java.util.Optional;

/**
 * De vier teams uit docs/04. Ze zijn er voor de kleur van naam, Glowing-outline en locator-stip,
 * en voor "geen PvP" tot ronde 4.
 */
public final class Teams {
	public static final String SPELERS = "spelers";
	public static final String HUNTERS = "hunters";
	public static final String KING = "king";
	public static final String OUT = "out";

	private Teams() {
	}

	public static void init() {
		Reset.REGISTER.registreer("teams", server -> {
			zorgDatZeBestaan(server);
			for (ServerPlayer s : Mc.deelnemers(server)) {
				zet(server, s, SPELERS);
			}
		});
	}

	public static void zorgDatZeBestaan(MinecraftServer server) {
		maak(server, SPELERS, TeamColor.WHITE, false);
		maak(server, HUNTERS, TeamColor.AQUA, true);
		maak(server, KING, TeamColor.GOLD, true);
		maak(server, OUT, TeamColor.GRAY, false);
	}

	private static void maak(MinecraftServer server, String naam, TeamColor kleur, boolean friendlyFire) {
		ServerScoreboard bord = server.getScoreboard();
		PlayerTeam team = bord.getPlayerTeam(naam);
		if (team == null) {
			team = bord.addPlayerTeam(naam);
		}
		team.setColor(Optional.of(kleur));
		team.setAllowFriendlyFire(friendlyFire);
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

	/** Ronde 5: iedereen uit zijn team, dus alles is PvP. */
	public static void uitTeam(MinecraftServer server, ServerPlayer speler) {
		ServerScoreboard bord = server.getScoreboard();
		if (bord.getPlayersTeam(speler.getScoreboardName()) != null) {
			bord.removePlayerFromTeam(speler.getScoreboardName());
		}
	}
}
