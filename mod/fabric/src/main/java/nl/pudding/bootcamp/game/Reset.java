package nl.pudding.bootcamp.game;

import net.minecraft.server.MinecraftServer;
import nl.pudding.bootcamp.Bootcamp;
import nl.pudding.bootcamp.core.ResetRegister;

import java.util.List;

/**
 * {@code /bc reset}: alles terug naar de basiskamp-staat. Elk onderdeel zet bij het opstarten zijn
 * eigen opruimstap in het register; de volgorde van registreren is de volgorde van opruimen.
 */
public final class Reset {
	public static final ResetRegister<MinecraftServer> REGISTER = new ResetRegister<>();

	private Reset() {
	}

	/** Weigert niks, ruimt alles op. Geeft de namen terug van de stappen die faalden. */
	public static List<String> draai(MinecraftServer server) {
		List<ResetRegister.Fout> fouten = REGISTER.draai(server);
		for (ResetRegister.Fout f : fouten) {
			Bootcamp.LOG.error("Reset-stap '{}' faalde", f.stap(), f.oorzaak());
		}
		return fouten.stream().map(ResetRegister.Fout::stap).toList();
	}
}
