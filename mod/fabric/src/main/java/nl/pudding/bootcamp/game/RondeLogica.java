package nl.pudding.bootcamp.game;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.GameType;
import nl.pudding.bootcamp.core.Regels;
import nl.pudding.bootcamp.core.Rol;
import nl.pudding.bootcamp.core.Ronde;
import nl.pudding.bootcamp.tribune.Tribune;

import java.util.List;

/**
 * Eén ronde. {@link Spel} roept deze methodes aan; een ronde raakt buiten haar eigen package
 * alleen de gedeelde primitieven ({@link Spel}, {@link Border}, {@link Poorten}, de kroon, de
 * tribune).
 */
public abstract class RondeLogica {
	public abstract Ronde ronde();

	public List<String> vereisteRegios() {
		return ronde().vereisteRegios();
	}

	public List<String> vereistePunten() {
		return ronde().vereistePunten();
	}

	/**
	 * Een extra voorwaarde naast de regio's en punten.
	 *
	 * @return {@code null} als starten mag, anders één regel waarom niet
	 */
	public String magStarten(MinecraftServer server) {
		return null;
	}

	/** Teleport, border, kits, countdown, poort open, timer. Eerst teleporteren, dan de border. */
	public abstract void start(MinecraftServer server);

	/** Elke servertick. */
	public void tick(MinecraftServer server) {
	}

	/** Elke seconde, nadat de timer is bijgewerkt. Hier hoort ook de bossbar. */
	public void seconde(MinecraftServer server) {
	}

	/** De timer van de ronde staat op nul. */
	public void timerOp(MinecraftServer server) {
	}

	/**
	 * Een deelnemer kreeg een dodelijke klap. De dood is al geannuleerd en de speler is geheald;
	 * de ronde beslist wat er met hem gebeurt.
	 */
	public void onDeath(MinecraftServer server, ServerPlayer speler, DamageSource bron) {
	}

	/**
	 * Iemand logt uit. Standaard volgens de uitlog-regels uit {@code core}: wie als dood telt krijgt
	 * de vlag en daarna {@link #naQuitDood}. Ronde 4 en 6 vullen de koning en de finalist zelf in.
	 */
	public void onQuit(MinecraftServer server, ServerPlayer speler) {
		SpelerStatus st = Spel.status(speler);
		if (st.dood) {
			return;
		}
		if (Regels.bijQuit(ronde(), st.rol) == Regels.QuitActie.DOOD) {
			st.dood = true;
			naQuitDood(server, speler);
		}
	}

	/** Een deelnemer is uitgelogd en telt als dood; hier kijkt de ronde of ze daarmee voorbij is. */
	protected void naQuitDood(MinecraftServer server, ServerPlayer speler) {
	}

	/**
	 * Iemand logt in terwijl deze ronde loopt. Standaard volgens de terugkom-regels uit
	 * {@code core}; de koning en de finalist die terugkomen vult de ronde zelf in.
	 */
	public void onJoin(MinecraftServer server, ServerPlayer speler) {
		SpelerStatus st = Spel.status(speler);
		switch (Regels.bijJoin(ronde(), st.rol, st.klaar, koningWachtLoopt(speler))) {
			case STARTPUNT -> {
				Spel.zetRol(server, speler, Rol.SPELER);
				speler.setGameMode(ronde().survival() ? GameType.SURVIVAL : GameType.ADVENTURE);
				if (startpunt() != null) {
					Spel.naarPunt(speler, startpunt());
				}
			}
			case VOLGEND_VERZAMELPUNT -> Tribune.maakKijkerOp(server, speler, Tribune.verzamelpuntNa(ronde()), Tribune.Spullen.HOUDEN, false);
			case KIJKER_TRIBUNE -> {
				st.dood = true;
				Tribune.maakKijker(server, speler, ronde() == Ronde.HORDE ? Tribune.Spullen.BEWAREN : Tribune.Spullen.LEGEN, false);
			}
			case KONING_TERUG -> koningTerug(server, speler);
			case FINALIST_TERUG -> finalistTerug(server, speler);
			case NIKS -> {
			}
		}
	}

	/** Ronde 4: lopen de dertig seconden voor deze uitgelogde koning nog? */
	protected boolean koningWachtLoopt(ServerPlayer speler) {
		return false;
	}

	/** Ronde 4: de koning is binnen zijn dertig seconden terug. */
	protected void koningTerug(MinecraftServer server, ServerPlayer speler) {
	}

	/** Ronde 5 en 6: een finalist komt terug. */
	protected void finalistTerug(MinecraftServer server, ServerPlayer speler) {
	}

	/** Waar een terugkomer heen gaat die nog niet klaar was (ronde 1 en 3). */
	protected String startpunt() {
		return null;
	}

	/**
	 * Opruimen: de ronde is afgelopen of afgebroken met {@code /bc stop}. Mag nooit falen en gaat
	 * niet over de uitslag; die heeft de ronde dan zelf al verwerkt.
	 */
	public void end(MinecraftServer server) {
	}
}
