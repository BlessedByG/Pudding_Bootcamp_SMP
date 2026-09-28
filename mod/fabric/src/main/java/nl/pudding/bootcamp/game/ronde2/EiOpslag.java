package nl.pudding.bootcamp.game.ronde2;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import nl.pudding.bootcamp.Bootcamp;
import nl.pudding.bootcamp.Mc;
import nl.pudding.bootcamp.core.Regio;
import nl.pudding.bootcamp.game.Reset;
import nl.pudding.bootcamp.game.Spel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Het Ei zoals het gebouwd is: alle blokken in de doos van regio {@code ei}, vastgelegd in
 * {@code <wereld>/bootcamp_ei.nbt}. Bij elke start van ronde 2 en bij {@code /bc reset} zet de mod
 * het terug, verspreid over een paar ticks zodat de server niet hapert. Waar gewone deepslate
 * staat, kunnen puntenblokken komen.
 */
public final class EiOpslag {
	public static final String BESTAND = "bootcamp_ei.nbt";
	/** Een doos van meer blokken is een verkeerde selectie. */
	public static final long MAX_BLOKKEN = 250_000;
	/** Zoveel blokken per tick bij het terugzetten: 250.000 blokken in 50 ticks, ruim binnen de countdown. */
	private static final int PER_TICK = 5_000;

	/** Wat er vastgelegd is. Blok {@code i} staat op x = i % sx, z = (i / sx) % sz, y = i / (sx * sz). */
	public record Vastlegging(BlockPos min, int sx, int sy, int sz, List<BlockState> palet, int[] blokken, int[] deepslate) {
		public BlockPos pos(int i) {
			int x = i % sx;
			int z = (i / sx) % sz;
			int y = i / (sx * sz);
			return min.offset(x, y, z);
		}

		public int aantal() {
			return blokken.length;
		}
	}

	private static Vastlegging vastlegging;
	private static boolean gelezen;

	// Het terugzetten dat bezig is
	private static Vastlegging bezig;
	private static int index;
	private static Map<Integer, BlockState> strooi = Map.of();
	private static Runnable naKlaar;

	private EiOpslag() {
	}

	public static void init() {
		Reset.REGISTER.registreer("het Ei terugzetten", server -> {
			if (get(server) != null) {
				herstel(server, Map.of(), null);
			}
		});
	}

	private static Path pad(MinecraftServer server) {
		return server.getWorldPath(LevelResource.ROOT).resolve(BESTAND).normalize();
	}

	/** De vastlegging, of {@code null} als het Ei nog niet is vastgelegd. */
	public static Vastlegging get(MinecraftServer server) {
		if (!gelezen) {
			gelezen = true;
			vastlegging = lees(server);
		}
		return vastlegging;
	}

	/** Hoeveel deepslate-plekken er zijn, of -1 zonder vastlegging. */
	public static int deepslate(MinecraftServer server) {
		Vastlegging v = get(server);
		return v == null ? -1 : v.deepslate().length;
	}

	/**
	 * {@code /ei vastleggen}: legt de doos van regio {@code ei} vast zoals hij nu gebouwd is.
	 *
	 * @return de melding voor wie het typt; begint met "!" als het niet gelukt is
	 */
	public static String vastleggen(MinecraftServer server) {
		Regio ei = Spel.regio("ei");
		if (ei == null) {
			return "!regio ei bestaat niet: selecteer de onderhoek en de bovenhoek van het Ei en doe /bc region save ei";
		}
		Regio.Doos doos = ei.omhullende();
		if (doos.aantalBlokken() > MAX_BLOKKEN) {
			return "!regio ei is " + doos.aantalBlokken() + " blokken, meer dan " + MAX_BLOKKEN + ": dat is een verkeerde selectie";
		}
		ServerLevel wereld = Mc.wereld(server);
		int sx = doos.breedteX();
		int sy = doos.hoogte();
		int sz = doos.breedteZ();
		BlockPos min = new BlockPos(doos.min().x(), doos.min().y(), doos.min().z());
		int n = sx * sy * sz;
		Map<BlockState, Integer> index = new HashMap<>();
		List<BlockState> palet = new ArrayList<>();
		int[] blokken = new int[n];
		List<Integer> deepslate = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			int x = i % sx;
			int z = (i / sx) % sz;
			int y = i / (sx * sz);
			BlockState state = wereld.getBlockState(min.offset(x, y, z));
			Integer p = index.get(state);
			if (p == null) {
				p = palet.size();
				palet.add(state);
				index.put(state, p);
			}
			blokken[i] = p;
			if (state.is(Blocks.DEEPSLATE)) {
				deepslate.add(i);
			}
		}
		Vastlegging v = new Vastlegging(min, sx, sy, sz, List.copyOf(palet), blokken,
				deepslate.stream().mapToInt(Integer::intValue).toArray());
		try {
			schrijf(server, v);
		} catch (IOException e) {
			Bootcamp.LOG.error("Kon {} niet schrijven", pad(server), e);
			return "!kon " + BESTAND + " niet schrijven, kijk in de console";
		}
		vastlegging = v;
		gelezen = true;
		return String.format("Ei vastgelegd: %,d blokken, waarvan %,d deepslate.", n, v.deepslate().length).replace(',', '.');
	}

	private static void schrijf(MinecraftServer server, Vastlegging v) throws IOException {
		CompoundTag root = new CompoundTag();
		root.putIntArray("min", new int[] {v.min().getX(), v.min().getY(), v.min().getZ()});
		root.putIntArray("grootte", new int[] {v.sx(), v.sy(), v.sz()});
		ListTag palet = new ListTag();
		for (BlockState s : v.palet()) {
			palet.add(NbtUtils.writeBlockState(s));
		}
		root.put("palet", palet);
		root.putIntArray("blokken", v.blokken());
		Path doel = pad(server);
		Path tijdelijk = doel.resolveSibling(BESTAND + ".tmp");
		NbtIo.writeCompressed(root, tijdelijk);
		Files.move(tijdelijk, doel, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
	}

	private static Vastlegging lees(MinecraftServer server) {
		Path pad = pad(server);
		if (!Files.exists(pad)) {
			return null;
		}
		try {
			CompoundTag root = NbtIo.readCompressed(pad, NbtAccounter.unlimitedHeap());
			int[] min = root.getIntArray("min").orElseThrow();
			int[] grootte = root.getIntArray("grootte").orElseThrow();
			int[] blokken = root.getIntArray("blokken").orElseThrow();
			HolderGetter<Block> blokRegister = server.registryAccess().lookupOrThrow(Registries.BLOCK);
			ListTag paletTag = root.getListOrEmpty("palet");
			List<BlockState> palet = new ArrayList<>();
			for (int i = 0; i < paletTag.size(); i++) {
				palet.add(NbtUtils.readBlockState(blokRegister, paletTag.getCompoundOrEmpty(i)));
			}
			List<Integer> deepslate = new ArrayList<>();
			for (int i = 0; i < blokken.length; i++) {
				if (palet.get(blokken[i]).is(Blocks.DEEPSLATE)) {
					deepslate.add(i);
				}
			}
			Bootcamp.LOG.info("{} geladen: {} blokken, {} deepslate", BESTAND, blokken.length, deepslate.size());
			return new Vastlegging(new BlockPos(min[0], min[1], min[2]), grootte[0], grootte[1], grootte[2],
					List.copyOf(palet), blokken, deepslate.stream().mapToInt(Integer::intValue).toArray());
		} catch (IOException | RuntimeException e) {
			Bootcamp.LOG.error("{} is niet te lezen; leg het Ei opnieuw vast met /ei vastleggen", pad, e);
			return null;
		}
	}

	// Terugzetten

	/**
	 * Zet het Ei terug zoals het is vastgelegd, verspreid over ticks, met op sommige plekken een
	 * ander blok (de puntenblokken).
	 *
	 * @param extra  blok-index naar het blok dat daar komt in plaats van de deepslate
	 * @param klaar  wat er daarna gebeurt, of {@code null}
	 */
	public static void herstel(MinecraftServer server, Map<Integer, BlockState> extra, Runnable klaar) {
		Vastlegging v = get(server);
		if (v == null) {
			return;
		}
		bezig = v;
		index = 0;
		strooi = extra;
		naKlaar = klaar;
	}

	public static boolean bezig() {
		return bezig != null;
	}

	/** Elke servertick: een stuk van het Ei terugzetten. */
	public static void tick(MinecraftServer server) {
		if (bezig == null) {
			return;
		}
		ServerLevel wereld = Mc.wereld(server);
		int tot = Math.min(bezig.aantal(), index + PER_TICK);
		// Geen buren bijwerken en geen drops: het is een terugzet, geen bouwwerk.
		int vlaggen = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
		for (; index < tot; index++) {
			BlockState doel = strooi.getOrDefault(index, bezig.palet().get(bezig.blokken()[index]));
			BlockPos pos = bezig.pos(index);
			if (wereld.getBlockState(pos) != doel) {
				wereld.setBlock(pos, doel, vlaggen);
			}
		}
		if (index >= bezig.aantal()) {
			bezig = null;
			Runnable r = naKlaar;
			naKlaar = null;
			strooi = Map.of();
			if (r != null) {
				r.run();
			}
		}
	}
}
