package nl.pudding.bootcamp.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import nl.pudding.bootcamp.Mc;
import nl.pudding.bootcamp.config.ConfigStore;
import nl.pudding.bootcamp.core.Regio;

import java.util.HashMap;
import java.util.Map;

/**
 * Poorten: een muur (regio {@code poort_<naam>}) die met één command weggaat. Een regio met meerdere
 * delen ({@code /bc region add}) is meerdere poorten die samen open en dicht gaan; alleen de blokken
 * in de delen zelf, nooit wat ertussen staat. Open is lucht. Dicht zet terug wat er stond toen hij
 * openging; weet de mod dat niet meer (na een herstart), dan iron bars.
 */
public final class Poorten {
	public static final String PREFIX = "poort_";
	/** Een poort is een muur, geen gebouw. */
	private static final long MAX_BLOKKEN = 8192;

	private static final Map<String, Map<BlockPos, BlockState>> ONTHOUDEN = new HashMap<>();

	private Poorten() {
	}

	public static void init() {
		Reset.REGISTER.registreer("poorten dicht", server -> {
			for (String naam : ConfigStore.get().regios().keySet()) {
				if (naam.startsWith(PREFIX)) {
					dicht(server, naam);
				}
			}
		});
	}

	/** {@code doolhof} en {@code poort_doolhof} zijn dezelfde poort. */
	public static String regioNaam(String naam) {
		return naam.startsWith(PREFIX) ? naam : PREFIX + naam;
	}

	/**
	 * @param hoorn de raid-hoorn voor iedereen; de wolkjes in de poort komen er altijd
	 * @return {@code null} als het gelukt is (of er niks te doen was), anders waarom niet
	 */
	public static String open(MinecraftServer server, String naam, boolean hoorn) {
		String regioNaam = regioNaam(naam);
		Regio regio = ConfigStore.get().regios().get(regioNaam);
		if (regio == null) {
			return "regio " + regioNaam + " bestaat niet";
		}
		if (aantal(regio) > MAX_BLOKKEN) {
			return "regio " + regioNaam + " is te groot voor een poort (" + aantal(regio) + " blokken)";
		}
		ServerLevel wereld = Mc.wereld(server);
		Map<BlockPos, BlockState> stond = new HashMap<>();
		for (BlockPos pos : blokken(regio)) {
			BlockState state = wereld.getBlockState(pos);
			if (!state.isAir()) {
				stond.put(pos.immutable(), state);
				wereld.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
			}
		}
		if (stond.isEmpty()) {
			// Stond al open: wat we van de vorige keer onthouden hebben blijft geldig.
			return null;
		}
		ONTHOUDEN.put(regioNaam, stond);
		for (Regio.Doos d : delen(regio)) {
			Mc.particles(wereld, ParticleTypes.CLOUD, (d.min().x() + d.max().x() + 1) / 2.0, (d.min().y() + d.max().y() + 1) / 2.0,
					(d.min().z() + d.max().z() + 1) / 2.0, 60, Math.max(1.0, Math.max(d.breedteX(), d.breedteZ()) / 4.0), 0.02);
		}
		if (hoorn) {
			Mc.geluidAllen(server, SoundEvents.RAID_HORN, 1f, 1f);
		}
		return null;
	}

	public static String dicht(MinecraftServer server, String naam) {
		String regioNaam = regioNaam(naam);
		Regio regio = ConfigStore.get().regios().get(regioNaam);
		if (regio == null) {
			return "regio " + regioNaam + " bestaat niet";
		}
		if (aantal(regio) > MAX_BLOKKEN) {
			return "regio " + regioNaam + " is te groot voor een poort (" + aantal(regio) + " blokken)";
		}
		ServerLevel wereld = Mc.wereld(server);
		Map<BlockPos, BlockState> stond = ONTHOUDEN.remove(regioNaam);
		if (stond != null) {
			stond.forEach((pos, state) -> wereld.setBlock(pos, state, Block.UPDATE_ALL));
			return null;
		}
		boolean leeg = true;
		for (BlockPos pos : blokken(regio)) {
			if (!wereld.getBlockState(pos).isAir()) {
				leeg = false;
				break;
			}
		}
		if (!leeg) {
			// Er staat al iets: de poort is dicht, niks overschrijven.
			return null;
		}
		for (BlockPos pos : blokken(regio)) {
			wereld.setBlock(pos, Blocks.IRON_BARS.defaultBlockState(), Block.UPDATE_ALL);
		}
		return null;
	}

	/** Opent de poort als hij bestaat; een ronde zonder poort start gewoon. */
	public static void openAlsHijBestaat(MinecraftServer server, String naam, boolean hoorn) {
		if (ConfigStore.get().regios().containsKey(regioNaam(naam))) {
			open(server, naam, hoorn);
		}
	}

	public static void dichtAlsHijBestaat(MinecraftServer server, String naam) {
		if (ConfigStore.get().regios().containsKey(regioNaam(naam))) {
			dicht(server, naam);
		}
	}

	/**
	 * De blokken van alle delen van de poort, elk blok één keer. Nooit de doos om de delen heen: bij
	 * twee poorten aan verschillende kanten zou die alles daartussen weghalen.
	 */
	private static Iterable<BlockPos> blokken(Regio r) {
		java.util.Set<BlockPos> uit = new java.util.LinkedHashSet<>();
		for (Regio.Doos d : delen(r)) {
			for (BlockPos p : BlockPos.betweenClosed(d.min().x(), d.min().y(), d.min().z(), d.max().x(), d.max().y(), d.max().z())) {
				uit.add(p.immutable());
			}
		}
		return uit;
	}

	private static java.util.List<Regio.Doos> delen(Regio r) {
		return r.isCilinder() ? java.util.List.of(r.omhullende()) : r.delen();
	}

	/** Hoeveel blokken alle delen samen hebben. */
	private static long aantal(Regio r) {
		return delen(r).stream().mapToLong(Regio.Doos::aantalBlokken).sum();
	}
}
