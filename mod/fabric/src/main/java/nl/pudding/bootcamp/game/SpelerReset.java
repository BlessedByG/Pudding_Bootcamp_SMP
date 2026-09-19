package nl.pudding.bootcamp.game;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import nl.pudding.bootcamp.Mc;
import nl.pudding.bootcamp.config.ConfigStore;
import nl.pudding.bootcamp.core.Punt;

/** De reset-stappen die over de spelers zelf gaan. Staff (creative of spectator) blijft met rust. */
public final class SpelerReset {
	private SpelerReset() {
	}

	public static void init() {
		Reset.REGISTER.registreer("effecten en heal", server -> {
			for (ServerPlayer s : Mc.deelnemers(server)) {
				s.removeAllEffects();
				Mc.heal(s);
			}
		});
		Reset.REGISTER.registreer("inventory", server -> {
			for (ServerPlayer s : Mc.deelnemers(server)) {
				s.getInventory().clearContent();
			}
		});
		Reset.REGISTER.registreer("gamemode", server -> {
			for (ServerPlayer s : Mc.deelnemers(server)) {
				s.setGameMode(GameType.ADVENTURE);
			}
		});
		Reset.REGISTER.registreer("teleport naar basiskamp", server -> {
			// Geen basiskamp gezet: dan blijft iedereen staan. Reset weigert niks.
			Punt basiskamp = ConfigStore.get().punten().get("basiskamp");
			if (basiskamp != null) {
				for (ServerPlayer s : Mc.deelnemers(server)) {
					Mc.teleport(s, basiskamp);
				}
			}
		});
	}
}
