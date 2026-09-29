package nl.pudding.bootcamp.game.ronde1;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import nl.pudding.bootcamp.Mc;
import nl.pudding.bootcamp.core.Regels;
import nl.pudding.bootcamp.core.Regio;
import nl.pudding.bootcamp.game.Spel;
import nl.pudding.bootcamp.schrik.Schrik;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Valkisten: een trapped chest in regio {@code doolhof} heeft geen loot. Wie hem opent krijgt 50/50
 * de jumpscare, of om zich heen willekeurig {@code /doolhof valmobs} husks en silverfish (standaard
 * 3 t/m 10) die gewoon schade doen. Per speler gaat een kist één keer af; daarna gaat hij voor die
 * speler open als lege kist.
 */
public final class Valkisten {
	public static final String TAG = "bootcamp_valkist";
	private static final List<String> MOBS = List.of("minecraft:husk", "minecraft:silverfish");
	/** Zo ver van de speler zoekt de mod vrije plekken voor de mobs. */
	private static final int STRAAL = 2;

	/** Per speler de kisten die al afgingen; een dubbele kist is één kist. */
	private final Map<UUID, Set<Long>> gehad = new HashMap<>();

	public static void init() {
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (!(player instanceof ServerPlayer speler) || Mc.isStaff(speler) || !(Spel.actief() instanceof Doolhof doolhof)) {
				return InteractionResult.PASS;
			}
			BlockPos pos = hit.getBlockPos();
			BlockState state = level.getBlockState(pos);
			if (!state.is(Blocks.TRAPPED_CHEST)) {
				return InteractionResult.PASS;
			}
			return doolhof.valkist(speler, pos, state) ? InteractionResult.SUCCESS : InteractionResult.PASS;
		});
		// Na een crash midden in het doolhof komen de mobs niet terug.
		ServerEntityEvents.ALLOW_LOAD.register((entity, level, reden, vanSchijf) ->
				!(vanSchijf && entity.entityTags().contains(TAG) && !(Spel.actief() instanceof Doolhof)));
	}

	/** {@code true}: de val ging af en de kist gaat niet open. */
	boolean open(ServerPlayer speler, BlockPos pos, BlockState state) {
		Regio doolhof = Spel.regio("doolhof");
		if (doolhof == null || !doolhof.omhullende().bevatDoos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)) {
			return false;
		}
		if (!gehad.computeIfAbsent(speler.getUUID(), k -> new HashSet<>()).add(sleutel(pos, state))) {
			return false;
		}
		// Staan de mobs op 0, dan altijd de jumpscare.
		if (Spel.instellingen().valMobsMax() == 0 || Regels.valkistGok(Spel.RANDOM) == Regels.Val.SCHRIK) {
			Schrik.op(speler);
		} else {
			Mc.geluid(speler, SoundEvents.EVOKER_PREPARE_SUMMON, 1f, 1f);
			spawn(speler);
		}
		return true;
	}

	/** Beide helften van een dubbele kist geven dezelfde sleutel. */
	private static long sleutel(BlockPos pos, BlockState state) {
		if (state.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
			return pos.asLong();
		}
		BlockPos ander = pos.relative(ChestBlock.getConnectedDirection(state));
		return Math.min(pos.asLong(), ander.asLong());
	}

	private static void spawn(ServerPlayer speler) {
		ServerLevel wereld = (ServerLevel) speler.level();
		List<BlockPos> vrij = plekkenRond(speler.blockPosition());
		List<BlockPos> gebruikt = new ArrayList<>();
		int min = Spel.instellingen().valMobsMin();
		int aantal = min + Spel.RANDOM.nextInt(Spel.instellingen().valMobsMax() - min + 1);
		for (int i = 0; i < aantal; i++) {
			EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(Identifier.parse(MOBS.get(Spel.RANDOM.nextInt(MOBS.size()))))
					.map(Holder::value).orElse(null);
			Entity entity = type == null ? null : type.create(wereld, EntitySpawnReason.EVENT);
			if (!(entity instanceof Mob mob)) {
				if (entity != null) {
					entity.discard();
				}
				continue;
			}
			zetNeer(wereld, mob, speler, vrij, gebruikt);
			mob.setPersistenceRequired();
			mob.addTag(TAG);
			mob.setTarget(speler);
			wereld.addFreshEntity(mob);
			Mc.particles(wereld, ParticleTypes.POOF, mob.getX(), mob.getY() + 0.5, mob.getZ(), 12, 0.3, 0.02);
		}
	}

	/** De blokken om de speler heen, in willekeurige volgorde; zijn eigen blok niet. */
	private static List<BlockPos> plekkenRond(BlockPos midden) {
		List<BlockPos> plekken = new ArrayList<>();
		for (int dy = -1; dy <= 1; dy++) {
			for (int dx = -STRAAL; dx <= STRAAL; dx++) {
				for (int dz = -STRAAL; dz <= STRAAL; dz++) {
					if (dx != 0 || dz != 0) {
						plekken.add(midden.offset(dx, dy, dz));
					}
				}
			}
		}
		Collections.shuffle(plekken, Spel.RANDOM);
		return plekken;
	}

	/**
	 * Elke mob op een eigen vrije plek met vaste grond eronder. Zijn die op (een smalle gang), dan
	 * samen op een plek die al gebruikt is; past hij nergens, dan waar de speler staat.
	 */
	private static void zetNeer(ServerLevel wereld, Mob mob, ServerPlayer speler, List<BlockPos> vrij, List<BlockPos> gebruikt) {
		float draai = Spel.RANDOM.nextFloat() * 360f;
		for (Iterator<BlockPos> it = vrij.iterator(); it.hasNext(); ) {
			BlockPos p = it.next();
			BlockPos onder = p.below();
			if (wereld.getBlockState(onder).getCollisionShape(wereld, onder).isEmpty()) {
				it.remove();
				continue;
			}
			mob.snapTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, draai, 0f);
			if (wereld.noCollision(mob)) {
				it.remove();
				gebruikt.add(p);
				return;
			}
		}
		Collections.shuffle(gebruikt, Spel.RANDOM);
		for (BlockPos p : gebruikt) {
			mob.snapTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, draai, 0f);
			if (wereld.noCollision(mob)) {
				return;
			}
		}
		mob.snapTo(speler.getX(), speler.getY(), speler.getZ(), draai, 0f);
	}

	/** Alle mobs uit valkisten weg, ook van een vorige serverrun. */
	static void ruimOp(MinecraftServer server) {
		List<Entity> weg = new ArrayList<>();
		for (Entity e : Mc.wereld(server).getAllEntities()) {
			if (e.entityTags().contains(TAG)) {
				weg.add(e);
			}
		}
		weg.forEach(Entity::discard);
	}
}
