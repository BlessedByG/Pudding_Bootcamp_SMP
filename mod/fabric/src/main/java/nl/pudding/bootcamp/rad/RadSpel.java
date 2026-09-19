package nl.pudding.bootcamp.rad;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.state.BlockState;
import nl.pudding.bootcamp.Mc;
import nl.pudding.bootcamp.config.ConfigStore;
import nl.pudding.bootcamp.core.BlokPos;
import nl.pudding.bootcamp.core.Punt;
import nl.pudding.bootcamp.core.Rad;
import nl.pudding.bootcamp.core.Ronde;
import nl.pudding.bootcamp.game.Planner;
import nl.pudding.bootcamp.game.Reset;
import nl.pudding.bootcamp.game.Spel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Het Rad: het licht loopt langs de twintig pilaren van De Kring, steeds langzamer, en stopt op de
 * kop van de uitverkorene. Het ziet eruit als toeval; start en aantal rondes zijn ook echt
 * willekeurig, de landing niet. Drie seconden later begint ronde 4.
 *
 * <p>Een lamp die aan is, is een brandende redstone lamp; uit is wat er stond. Hij wordt gezet
 * zonder buren te updaten, want een redstone lamp zonder stroom gaat anders vanzelf weer uit.
 */
public final class RadSpel {
	private static final int NAAR_RONDE_4_NA_SECONDEN = 3;

	private static Rad rad;
	private static final Map<Integer, BlockState> STOND = new HashMap<>();

	private RadSpel() {
	}

	public static void init() {
		Reset.REGISTER.registreer("het rad", RadSpel::stop);
	}

	/**
	 * {@code /bc rad}: weigert zonder precies één uitverkorene, of als ronde 4 straks niet kan
	 * starten. Dan verandert er niets.
	 *
	 * @return {@code null} als het rad draait, anders waarom niet
	 */
	public static String start(MinecraftServer server) {
		if (rad != null) {
			return "het rad draait al";
		}
		if (Planner.heeftWerk()) {
			return "het rad is net geland en ronde 4 start zo (/bc stop om dat af te breken)";
		}
		if (Spel.loopt()) {
			return "er loopt nog een ronde (/bc stop)";
		}
		String naam = ConfigStore.get().uitverkoren();
		if (naam == null) {
			return "er is geen uitverkorene (/bc uitverkoren <speler>)";
		}
		int doel = ConfigStore.get().slotVan(naam);
		if (doel < 0) {
			return "de uitverkorene heeft geen pilaar (/bc slot <speler> <0-19>)";
		}
		List<String> mist = Ronde.ontbreekt(List.of(), Ronde.vereistePuntenRad(), ConfigStore.get().regios().keySet(),
				ConfigStore.get().punten().keySet());
		if (!mist.isEmpty()) {
			return "ontbreekt: " + String.join(", ", mist);
		}
		// Het rad mag niet landen op een ronde die daarna weigert te starten.
		String ronde4 = Spel.controleer(server, Ronde.KING);
		if (ronde4 != null) {
			return "ronde 4 kan straks niet starten, " + ronde4;
		}

		lampenUit(server);
		rad = Rad.willekeurig(Ronde.AANTAL_LAMPEN, doel, Spel.RANDOM);
		zetLamp(server, rad.pos(), true);
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
				stap(server);
				// Hoe langzamer het rad, hoe lager de tik.
				float toon = 0.8f + 0.8f * Math.min(1f, rad.rest() / (float) Rad.VERTRAAG_VANAF);
				Mc.geluidAllen(server, SoundEvents.NOTE_BLOCK_HAT, 1f, toon);
			}
			case GELAND -> {
				stap(server);
				geland(server);
			}
		}
	}

	private static void stap(MinecraftServer server) {
		zetLamp(server, rad.vorigePos(), false);
		zetLamp(server, rad.pos(), true);
	}

	private static void geland(MinecraftServer server) {
		rad = null;
		Mc.geluidAllen(server, SoundEvents.ENDER_DRAGON_GROWL, 1f, 1f);
		ServerPlayer koning = Spel.clown(server);
		String naam = koning != null ? Mc.naam(koning) : ConfigStore.get().uitverkoren();
		if (koning != null) {
			Mc.particles(Mc.wereld(server), ParticleTypes.TOTEM_OF_UNDYING, koning.getX(), koning.getY() + 1, koning.getZ(), 200, 0.6, 0.5);
		}
		Mc.titleAllen(server, Mc.tekst("DE KONING", ChatFormatting.GOLD, ChatFormatting.BOLD),
				Mc.tekst(naam.toUpperCase(), ChatFormatting.YELLOW, ChatFormatting.BOLD));
		Planner.naSeconden(NAAR_RONDE_4_NA_SECONDEN, () -> {
			String fout = Spel.start(server, Ronde.KING);
			if (fout != null) {
				server.getPlayerList().broadcastSystemMessage(Mc.tekst("[bootcamp] Ronde 4 start niet, " + fout, ChatFormatting.RED), false);
			}
		});
	}

	/** Stopt een rad dat nog draait. Is het al geland, dan blijft de lamp van de koning aan. */
	public static void stopDraaien(MinecraftServer server) {
		if (rad != null) {
			stop(server);
		}
	}

	/** Stopt het rad en zet alle lampen uit. Ook voor {@code /bc stop} en {@code /bc reset}. */
	public static void stop(MinecraftServer server) {
		rad = null;
		lampenUit(server);
	}

	// Lampen

	private static void zetLamp(MinecraftServer server, int slot, boolean aan) {
		Punt p = Spel.punt("lamp_" + slot);
		if (p == null) {
			return;
		}
		ServerLevel wereld = Mc.wereld(server);
		BlokPos b = p.blokPos();
		BlockPos pos = new BlockPos(b.x(), b.y(), b.z());
		if (aan) {
			BlockState nu = wereld.getBlockState(pos);
			if (nu.is(Blocks.REDSTONE_LAMP) && nu.getValue(RedstoneLampBlock.LIT)) {
				// Blijven branden na een crash: "wat er stond" is dan de lamp die uit is.
				nu = nu.setValue(RedstoneLampBlock.LIT, false);
			}
			STOND.putIfAbsent(slot, nu);
			wereld.setBlock(pos, Blocks.REDSTONE_LAMP.defaultBlockState().setValue(RedstoneLampBlock.LIT, true), Block.UPDATE_CLIENTS);
		} else {
			BlockState stond = STOND.remove(slot);
			if (stond != null) {
				wereld.setBlock(pos, stond, Block.UPDATE_CLIENTS);
			}
		}
	}

	public static void lampenUit(MinecraftServer server) {
		for (Integer slot : List.copyOf(STOND.keySet())) {
			zetLamp(server, slot, false);
		}
	}
}
