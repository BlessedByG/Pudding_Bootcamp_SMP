package nl.pudding.bootcamp.game;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import nl.pudding.bootcamp.core.Ronde;

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

	public void onQuit(MinecraftServer server, ServerPlayer speler) {
	}

	/** Iemand logt in terwijl deze ronde loopt. */
	public void onJoin(MinecraftServer server, ServerPlayer speler) {
	}

	/**
	 * Opruimen: de ronde is afgelopen of afgebroken met {@code /bc stop}. Mag nooit falen en gaat
	 * niet over de uitslag; die heeft de ronde dan zelf al verwerkt.
	 */
	public void end(MinecraftServer server) {
	}
}
