package nl.pudding.bootcamp.game;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import nl.pudding.bootcamp.Mc;
import nl.pudding.bootcamp.core.Ronde;
import nl.pudding.bootcamp.core.Tijd;
import nl.pudding.bootcamp.visuals.Bossbar;

/**
 * Een ronde zonder spel: teleporteren, border, gamemode, countdown en een timer. Staat er tot de
 * echte ronde gebouwd is, zodat het raamwerk van begin tot eind te testen is.
 */
final class LegeRonde extends RondeLogica {
	private final Ronde ronde;
	private final String startpunt;
	private final String regio;
	private final String volgendPunt;

	LegeRonde(Ronde ronde, String startpunt, String regio, String volgendPunt) {
		this.ronde = ronde;
		this.startpunt = startpunt;
		this.regio = regio;
		this.volgendPunt = volgendPunt;
	}

	@Override
	public Ronde ronde() {
		return ronde;
	}

	@Override
	public void start(MinecraftServer server) {
		Spel.maakSpelers(server, ronde.survival());
		for (ServerPlayer s : Mc.deelnemers(server)) {
			Spel.naarPunt(s, startpunt);
		}
		Border.zet(server, Spel.regio(regio));
		Aftelling.start(5, "Start over", () -> Spel.startTimer(Math.max(60, ronde.duurSeconden())));
	}

	@Override
	public void seconde(MinecraftServer server) {
		Bossbar.zet(ronde.naam() + " · " + Tijd.mmss(Spel.timer()), BossEvent.BossBarColor.WHITE, Spel.timerDeel());
	}

	@Override
	public void timerOp(MinecraftServer server) {
		Spel.stop(server);
		Spel.maakSpelers(server, false);
		if (volgendPunt != null) {
			for (ServerPlayer s : Mc.deelnemers(server)) {
				Spel.naarPunt(s, volgendPunt);
			}
		}
	}
}
