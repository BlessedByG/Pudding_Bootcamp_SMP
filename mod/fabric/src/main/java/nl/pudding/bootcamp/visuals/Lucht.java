package nl.pudding.bootcamp.visuals;

import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.clock.ServerClockManager;
import net.minecraft.world.clock.WorldClock;
import nl.pudding.bootcamp.Mc;
import nl.pudding.bootcamp.core.Dagtijd;
import nl.pudding.bootcamp.game.Reset;

/**
 * R1.10: het weer en de tijd van de dag, vloeiend. Als het gif van het doolhof begint, gaat het
 * regenen en wordt het in 30 seconden nacht; aan het eind van het doolhof wordt het in 20 seconden
 * weer dag en droog.
 *
 * <p>De klok staat stil (gamerule {@code advance_time} uit), dus de mod zet hem zelf elke tick een
 * stukje verder; elke verandering gaat meteen naar alle spelers. De regen komt en gaat bij de
 * spelers vanzelf geleidelijk; het weer telt niet af ({@code advance_weather} uit), dus het blijft
 * regenen tot de mod het stopt.
 */
public final class Lucht {
	private static final int NACHT_IN = 30 * 20;
	private static final int DAG_IN = 20 * 20;
	/** Hoe lang het weer zou duren; het telt niet af, dus dit is alleen voor de vorm. */
	private static final int WEER = 60 * 60 * 20;

	/** Waar de klok heen loopt, of -1 als hij stil staat. */
	private static long doel = -1;
	private static int stap;

	private Lucht() {
	}

	public static void init() {
		Reset.REGISTER.registreer("dag en droog weer", server -> {
			doel = -1;
			server.setWeatherParameters(WEER, 0, false, false);
			klok(server, (m, klok) -> m.setTotalTicks(klok, m.getTotalTicks(klok) + Dagtijd.totDagtijd(m.getTotalTicks(klok), Dagtijd.MIDDAG)));
		});
	}

	/** Regen, en in 30 seconden naar middernacht. */
	public static void naarNacht(MinecraftServer server) {
		server.setWeatherParameters(0, WEER, true, false);
		begin(server, Dagtijd.MIDDERNACHT, NACHT_IN);
	}

	/** Droog, en in 20 seconden naar de middag, door de ochtend heen. */
	public static void naarDag(MinecraftServer server) {
		server.setWeatherParameters(WEER, 0, false, false);
		begin(server, Dagtijd.MIDDAG, DAG_IN);
	}

	private static void begin(MinecraftServer server, int dagtijd, int ticks) {
		klok(server, (m, klok) -> {
			long nu = m.getTotalTicks(klok);
			long afstand = Dagtijd.totDagtijd(nu, dagtijd);
			doel = afstand == 0 ? -1 : nu + afstand;
			stap = Dagtijd.stap(afstand, ticks);
		});
	}

	/** Elke servertick: de klok een stap verder, tot hij er is. */
	public static void tick(MinecraftServer server) {
		if (doel < 0) {
			return;
		}
		klok(server, (m, klok) -> {
			long nu = m.getTotalTicks(klok);
			if (nu + stap >= doel) {
				m.setTotalTicks(klok, doel);
				doel = -1;
			} else {
				m.addTicks(klok, stap);
			}
		});
	}

	private interface MetKlok {
		void doe(ServerClockManager manager, Holder<WorldClock> klok);
	}

	/** De klok van de overworld; een wereld zonder klok laat de mod met rust. */
	private static void klok(MinecraftServer server, MetKlok wat) {
		ServerLevel wereld = Mc.wereld(server);
		wereld.dimensionType().defaultClock().ifPresent(klok -> wat.doe(wereld.clockManager(), klok));
	}
}
