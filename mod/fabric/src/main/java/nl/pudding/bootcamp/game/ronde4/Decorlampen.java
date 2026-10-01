package nl.pudding.bootcamp.game.ronde4;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.state.BlockState;
import nl.pudding.bootcamp.Mc;
import nl.pudding.bootcamp.config.ConfigStore;
import nl.pudding.bootcamp.core.BlokPos;
import nl.pudding.bootcamp.core.Kleur;
import nl.pudding.bootcamp.core.Lichtshow;
import nl.pudding.bootcamp.core.Regio;
import nl.pudding.bootcamp.core.Ronde;
import nl.pudding.bootcamp.game.Spel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * R4.6: de decorlampen van de quizhal: alle redstone lampen in regio {@code quiz}, behalve die van
 * de banken. De mod zoekt ze bij de start van de quiz zelf op en zet ze aan en uit volgens de
 * {@link Lichtshow}, met de klok mee rond het midden van de regio. Alleen een lamp die echt
 * verandert, krijgt een nieuw blok; de buren worden niet bijgewerkt.
 *
 * <p>Per team kan er een regio {@code quizdecor_<kleur>} zijn (met de wand, ook in delen): de lampen
 * daarin horen bij dat team. Is een team aan de beurt, dan branden alleen die en staat de rest uit.
 * Lampen in zo'n regio doen ook mee als ze net buiten regio {@code quiz} staan.
 */
final class Decorlampen {
	/** Groter dan dit zoekt de mod niet: dan is regio {@code quiz} vast verkeerd gezet. */
	private static final long MAX_BLOKKEN = 4_000_000;
	/** Elke zoveel ticks alles opnieuw zetten, voor een lamp die door iets anders uit of aan ging. */
	private static final int VERS_ELKE = 20;

	private final List<BlockPos> lampen;
	private final double[] hoek;
	/** Bij welk team de lamp hoort ({@code quizdecor_<kleur>}), of {@code null}. */
	private final Kleur[] team;
	private final boolean[] aan;
	private final boolean[] bekend;
	private int ticks;

	private Decorlampen(List<BlockPos> lampen, double midX, double midZ) {
		this.lampen = lampen;
		this.hoek = new double[lampen.size()];
		this.team = new Kleur[lampen.size()];
		this.aan = new boolean[lampen.size()];
		this.bekend = new boolean[lampen.size()];
		for (int i = 0; i < lampen.size(); i++) {
			BlockPos p = lampen.get(i);
			hoek[i] = Lichtshow.hoek(p.getX() + 0.5 - midX, p.getZ() + 0.5 - midZ);
			for (Kleur k : Kleur.values()) {
				Regio r = Spel.regio(teamRegio(k));
				if (r != null && r.bevatDoos(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5)) {
					team[i] = k;
					break;
				}
			}
		}
	}

	/** De regio met de decorlampen achter de bank van dit team. */
	static String teamRegio(Kleur k) {
		return "quizdecor_" + k.id();
	}

	/**
	 * Zoekt alle redstone lampen in regio {@code quiz} en in de regio's {@code quizdecor_<kleur>},
	 * behalve die van de banken.
	 */
	static Decorlampen zoek(MinecraftServer server) {
		Regio quiz = Spel.regio("quiz");
		Set<BlockPos> banken = new HashSet<>();
		for (Kleur k : Kleur.values()) {
			for (String naam : Ronde.allemaal("quizlamp_" + k.id() + "_", ConfigStore.get().punten().keySet())) {
				BlokPos b = Spel.punt(naam).blokPos();
				banken.add(new BlockPos(b.x(), b.y(), b.z()));
			}
		}
		ServerLevel wereld = Mc.wereld(server);
		Set<BlockPos> gevonden = new LinkedHashSet<>();
		zoekIn(wereld, quiz, banken, gevonden);
		for (Kleur k : Kleur.values()) {
			zoekIn(wereld, Spel.regio(teamRegio(k)), banken, gevonden);
		}
		return new Decorlampen(new ArrayList<>(gevonden), quiz == null ? 0 : quiz.centerX(), quiz == null ? 0 : quiz.centerZ());
	}

	private static void zoekIn(ServerLevel wereld, Regio regio, Set<BlockPos> banken, Set<BlockPos> gevonden) {
		if (regio == null || regio.aantalBlokken() > MAX_BLOKKEN) {
			return;
		}
		Regio.Doos doos = regio.omhullende();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int x = doos.min().x(); x <= doos.max().x(); x++) {
			for (int z = doos.min().z(); z <= doos.max().z(); z++) {
				for (int y = doos.min().y(); y <= doos.max().y(); y++) {
					pos.set(x, y, z);
					if (wereld.getBlockState(pos).is(Blocks.REDSTONE_LAMP) && regio.bevatDoos(x + 0.5, y + 0.5, z + 0.5)
							&& !banken.contains(pos)) {
						gevonden.add(pos.immutable());
					}
				}
			}
		}
	}

	/** Hoeveel decorlampen er bij dit team horen. */
	int aantal(Kleur k) {
		int n = 0;
		for (Kleur t : team) {
			n += t == k ? 1 : 0;
		}
		return n;
	}

	/** Een team aan de beurt: zijn lampen branden stil, de rest is uit. */
	void team(ServerLevel wereld, Kleur k) {
		vers();
		for (int i = 0; i < lampen.size(); i++) {
			zet(wereld, i, team[i] == k);
		}
	}

	int aantal() {
		return lampen.size();
	}

	/** De show zo ver rond (in rondjes): elke lamp aan of uit naar zijn plek. */
	void toon(ServerLevel wereld, double rond) {
		vers();
		for (int i = 0; i < lampen.size(); i++) {
			zet(wereld, i, Lichtshow.aan(hoek[i], rond));
		}
	}

	/** Alle lampen tegelijk aan of uit: knipperen op het plingeltje, en uit na de quiz. */
	void allemaal(ServerLevel wereld, boolean aanOfUit) {
		vers();
		for (int i = 0; i < lampen.size(); i++) {
			zet(wereld, i, aanOfUit);
		}
	}

	private void vers() {
		if (++ticks % VERS_ELKE == 0) {
			java.util.Arrays.fill(bekend, false);
		}
	}

	private void zet(ServerLevel wereld, int i, boolean nu) {
		if (bekend[i] && aan[i] == nu) {
			return;
		}
		bekend[i] = true;
		aan[i] = nu;
		BlockPos pos = lampen.get(i);
		BlockState state = wereld.getBlockState(pos);
		if (state.hasProperty(RedstoneLampBlock.LIT) && state.getValue(RedstoneLampBlock.LIT) != nu) {
			wereld.setBlock(pos, state.setValue(RedstoneLampBlock.LIT, nu), Block.UPDATE_CLIENTS);
		}
	}
}
