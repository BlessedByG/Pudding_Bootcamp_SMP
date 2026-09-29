package nl.pudding.bootcamp.game;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.gamerules.GameRules;
import nl.pudding.bootcamp.Bootcamp;
import nl.pudding.bootcamp.Mc;

/** De gamerules die de mod bij het opstarten en bij {@code /bc reset} zet (docs/04, Serverinstellingen). */
public final class Spelregels {
	private Spelregels() {
	}

	public static void init() {
		Reset.REGISTER.registreer("gamerules", Spelregels::zet);
	}

	public static void zet(MinecraftServer server) {
		GameRules regels = Mc.wereld(server).getGameRules();
		regels.set(GameRules.NATURAL_HEALTH_REGENERATION, true, server);
		// In 26.2 is PvP een gamerule. Tot ronde 4 houdt team spelers (friendly fire uit) het tegen.
		regels.set(GameRules.PVP, true, server);
		// De horde spawnen we zelf.
		regels.set(GameRules.SPAWN_MOBS, false, server);
		// Creepers in de ruïne-arena mogen niks slopen.
		regels.set(GameRules.MOB_GRIEFING, false, server);
		// TNT uit het Ei knalt de mod zelf, zodat alleen de deepslate weggaat. Vanilla TNT ontploft niet.
		regels.set(GameRules.TNT_EXPLODES, false, server);
		regels.set(GameRules.ADVANCE_TIME, false, server);
		regels.set(GameRules.ADVANCE_WEATHER, false, server);
		regels.set(GameRules.SHOW_ADVANCEMENT_MESSAGES, false, server);
		// Aan in ronde 4 t/m 6, met alleen de koningen zichtbaar.
		regels.set(GameRules.LOCATOR_BAR, false, server);
		// Geen willekeurige ticks: geplakte bladeren vergaan anders, en gras en gewassen groeien door.
		regels.set(GameRules.RANDOM_TICK_SPEED, 0, server);
		Bootcamp.LOG.info("Gamerules gezet");
	}

	public static void locatorBar(MinecraftServer server, boolean aan) {
		Mc.wereld(server).getGameRules().set(GameRules.LOCATOR_BAR, aan, server);
	}
}
