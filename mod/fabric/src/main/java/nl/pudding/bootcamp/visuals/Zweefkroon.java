package nl.pudding.bootcamp.visuals;

import com.mojang.math.Transformation;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.pudding.bootcamp.Mc;
import nl.pudding.bootcamp.game.Reset;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * De zweefkroon: een item display met een gouden helm boven het hoofd van elke koning, die
 * langzaam ronddraait. Twee koningen in de finale hebben elk hun eigen.
 */
public final class Zweefkroon {
	private static final String TAG = "bootcamp_zweefkroon";
	private static final double HOOGTE = 2.45;
	private static final float SCHAAL = 0.5f;
	private static final int ELKE_TICKS = 2;
	private static final float GRADEN_PER_STAP = 6f;

	private static final Map<UUID, Display.ItemDisplay> KRONEN = new HashMap<>();
	private static float hoek;

	private Zweefkroon() {
	}

	public static void init() {
		Reset.REGISTER.registreer("zweefkroon", Zweefkroon::ruimAllesOp);
		// Een zweefkroon die van schijf geladen wordt is een overblijfsel van een vorige serverrun
		// (of van een chunk die uit- en weer ingeladen is): nooit binnenlaten. Opruimen bij het
		// opstarten vangt alleen geladen chunks, dit vangt de rest.
		ServerEntityEvents.ALLOW_LOAD.register((entity, level, reden, vanSchijf) -> !(vanSchijf && entity.entityTags().contains(TAG)));
	}

	public static void aan(ServerPlayer koning) {
		if (KRONEN.containsKey(koning.getUUID())) {
			return;
		}
		ServerLevel wereld = Mc.wereld(koning.level().getServer());
		Display.ItemDisplay kroon = new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY, wereld);
		kroon.setItemStack(new ItemStack(Items.GOLDEN_HELMET));
		kroon.setTransformation(new Transformation(null, null, new Vector3f(SCHAAL, SCHAAL, SCHAAL), null));
		// Vloeiend tussen twee stappen in, in plaats van schokkerig verspringen.
		kroon.setPosRotInterpolationDuration(ELKE_TICKS);
		kroon.setPos(koning.getX(), koning.getY() + HOOGTE, koning.getZ());
		kroon.addTag(TAG);
		KRONEN.put(koning.getUUID(), kroon);
		wereld.addFreshEntity(kroon);
	}

	public static void uit(UUID koning) {
		Display.ItemDisplay kroon = KRONEN.remove(koning);
		if (kroon != null) {
			kroon.discard();
		}
	}

	/** Elke servertick. */
	public static void tick(MinecraftServer server) {
		if (KRONEN.isEmpty() || server.getTickCount() % ELKE_TICKS != 0) {
			return;
		}
		hoek = (hoek + GRADEN_PER_STAP) % 360f;
		List<ServerPlayer> opnieuw = new ArrayList<>();
		for (Iterator<Map.Entry<UUID, Display.ItemDisplay>> it = KRONEN.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<UUID, Display.ItemDisplay> e = it.next();
			ServerPlayer koning = server.getPlayerList().getPlayer(e.getKey());
			Display.ItemDisplay kroon = e.getValue();
			if (kroon.isRemoved()) {
				it.remove();
				if (koning != null) {
					opnieuw.add(koning);
				}
				continue;
			}
			if (koning == null) {
				// Uitgelogd: de kroon blijft hangen waar hij was tot de koning terug is of de kroon doorgaat.
				continue;
			}
			kroon.absSnapTo(koning.getX(), koning.getY() + HOOGTE, koning.getZ(), hoek, 0f);
		}
		// De entity is weggevallen (chunk uitgeladen) maar de koning is er nog: nieuwe kroon.
		opnieuw.forEach(Zweefkroon::aan);
	}

	/** Ook de kronen die een vorige serverrun heeft achtergelaten. */
	public static void ruimAllesOp(MinecraftServer server) {
		KRONEN.values().forEach(Entity::discard);
		KRONEN.clear();
		List<Entity> oud = new ArrayList<>();
		for (Entity e : Mc.wereld(server).getAllEntities()) {
			if (e.entityTags().contains(TAG)) {
				oud.add(e);
			}
		}
		oud.forEach(Entity::discard);
	}
}
