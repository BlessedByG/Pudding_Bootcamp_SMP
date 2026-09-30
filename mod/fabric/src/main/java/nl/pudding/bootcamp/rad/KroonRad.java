package nl.pudding.bootcamp.rad;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import nl.pudding.bootcamp.Mc;
import nl.pudding.bootcamp.core.Rad;
import nl.pudding.bootcamp.core.Regels;
import nl.pudding.bootcamp.core.Ronde;
import nl.pudding.bootcamp.game.Planner;
import nl.pudding.bootcamp.game.Reset;
import nl.pudding.bootcamp.game.Spel;
import nl.pudding.bootcamp.game.ronde4.Quiz;
import nl.pudding.bootcamp.kits.Items26;
import nl.pudding.bootcamp.tribune.Tribune;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Het Rad van ronde 5, in beeld: een rij spelerskoppen met namen die langs een pijltje schuift,
 * steeds langzamer, en stopt op de uitverkorene. De volgorde, de startpositie en het aantal rondes
 * zijn echt willekeurig, de landing niet. Drie seconden na DE KROON begint Clown vs All (in beeld: King of the Hill).
 */
public final class KroonRad {
	private static Rad rad;
	private static final List<UUID> VAKKEN = new ArrayList<>();

	private KroonRad() {
	}

	public static void init() {
		Reset.REGISTER.registreer("het rad", KroonRad::stop);
	}

	/**
	 * {@code /clown rad}: weigert zonder uitverkorene die online is en meedoet, of als ronde 5 straks
	 * niet kan starten. Dan verandert er niets.
	 *
	 * @return {@code null} als het rad draait, anders waarom niet
	 */
	public static String start(MinecraftServer server) {
		if (rad != null) {
			return "het rad draait al";
		}
		if (Spel.loopt()) {
			return "nu loopt: " + Spel.actief().ronde().naam();
		}
		ServerPlayer clown = Spel.clown(server);
		if (clown == null) {
			return "de uitverkorene is niet online of niet gezet (/clown uitverkoren <speler>)";
		}
		if (Mc.isStaff(clown)) {
			return "de uitverkorene staat in creative of spectator en doet dus niet mee";
		}
		// Het rad mag niet landen op een ronde die daarna weigert te starten.
		String ronde5 = Spel.controleer(server, Ronde.CLOWN);
		if (ronde5 != null) {
			return "King of the Hill kan straks niet starten, " + ronde5;
		}
		Planner.wisAlles();
		Quiz.lampenUit(server);
		List<ServerPlayer> deelnemers = new ArrayList<>(Mc.deelnemers(server));
		// Iedereen op de tribune; wie er nog niet stond, zet de mod er eerst neer.
		for (ServerPlayer s : deelnemers) {
			Items26.haalWeg(s, Items26.QUIZ_TAG);
			if (Spel.status(s).tribunepunt == null) {
				Tribune.naarTribune(s, Ronde.CLOWN);
			}
		}
		Collections.shuffle(deelnemers, Spel.RANDOM);
		VAKKEN.clear();
		deelnemers.forEach(s -> VAKKEN.add(s.getUUID()));
		rad = Rad.willekeurig(VAKKEN.size(), VAKKEN.indexOf(clown.getUUID()), Spel.RANDOM);
		toon(server);
		return null;
	}

	public static boolean draait() {
		return rad != null;
	}

	/** Elke servertick. */
	public static void tick(MinecraftServer server) {
		if (rad == null) {
			return;
		}
		switch (rad.tick()) {
			case NIKS -> {
			}
			case STAP -> {
				toon(server);
				// Hoe langzamer het rad, hoe lager de tik.
				float toon = 0.8f + 0.8f * Math.min(1f, rad.rest() / (float) Rad.VERTRAAG_VANAF);
				Mc.geluidAllen(server, SoundEvents.NOTE_BLOCK_HAT, 1f, toon);
			}
			case GELAND -> geland(server);
		}
	}

	/** Kop en naam van wie onder het pijltje staat, met de rij van vijf eronder. */
	private static void toon(MinecraftServer server) {
		int n = VAKKEN.size();
		int pos = rad.pos();
		MutableComponent titel = kopEnNaam(server, VAKKEN.get(pos), ChatFormatting.GOLD);
		MutableComponent rij = Component.empty();
		for (int d = -2; d <= 2; d++) {
			UUID id = VAKKEN.get(Math.floorMod(pos + d, n));
			if (d != -2) {
				rij.append(Mc.tekst("  ", ChatFormatting.GRAY));
			}
			if (d == 0) {
				rij.append(Mc.tekst("▶ ", ChatFormatting.GOLD, ChatFormatting.BOLD))
						.append(kopEnNaam(server, id, ChatFormatting.GOLD))
						.append(Mc.tekst(" ◀", ChatFormatting.GOLD, ChatFormatting.BOLD));
			} else {
				rij.append(kopEnNaam(server, id, ChatFormatting.GRAY));
			}
		}
		Mc.titleAllen(server, titel, rij, 0, rad.wacht() + 5, 0);
	}

	private static MutableComponent kopEnNaam(MinecraftServer server, UUID id, ChatFormatting kleur) {
		ServerPlayer s = server.getPlayerList().getPlayer(id);
		if (s != null) {
			return Mc.kopEnNaam(s, kleur);
		}
		return Mc.tekst(Spel.naamVan(id), kleur);
	}

	private static void geland(MinecraftServer server) {
		UUID gekozen = VAKKEN.get(rad.pos());
		rad = null;
		Mc.geluidAllen(server, SoundEvents.ENDER_DRAGON_GROWL, 1f, 1f);
		ServerPlayer kroon = server.getPlayerList().getPlayer(gekozen);
		MutableComponent wie = kroon != null ? Mc.kopEnNaam(kroon, ChatFormatting.YELLOW, ChatFormatting.BOLD)
				: Mc.tekst(Spel.naamVan(gekozen), ChatFormatting.YELLOW, ChatFormatting.BOLD);
		if (kroon != null) {
			Mc.particles(Mc.wereld(server), ParticleTypes.TOTEM_OF_UNDYING, kroon.getX(), kroon.getY() + 1, kroon.getZ(), 200, 0.6, 0.5);
		}
		Mc.titleAllen(server, Mc.tekst("DE KROON", ChatFormatting.GOLD, ChatFormatting.BOLD), wie, 0, 70, 20);
		Planner.naSeconden(Regels.NA_HET_RAD, () -> {
			String fout = Spel.start(server, Ronde.CLOWN);
			if (fout != null) {
				Mc.chatAllen(server, Mc.tekst("[bootcamp] King of the Hill start niet, " + fout, ChatFormatting.RED));
			}
		});
	}

	/** Stopt het rad. Ook voor {@code /clown stop} en {@code /bc reset}. */
	public static void stop(MinecraftServer server) {
		rad = null;
		VAKKEN.clear();
	}
}
