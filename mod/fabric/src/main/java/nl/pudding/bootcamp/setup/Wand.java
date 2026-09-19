package nl.pudding.bootcamp.setup;

import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import nl.pudding.bootcamp.Mc;
import nl.pudding.bootcamp.core.BlokPos;
import nl.pudding.bootcamp.core.Regio;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * De regio-wand: een stick met een custom data component. Linksklik op een blok is hoek 1,
 * rechtsklik hoek 2. {@code /bc region show} tekent tien seconden particles op de randen.
 */
public final class Wand {
	private static final String TAG = "bootcamp_wand";
	private static final int TOON_TICKS = 10 * 20;
	private static final int TEKEN_ELKE = 10;
	/** Bovengrens per tekenbeurt, zodat het Ei-bos van 150 x 150 de client niet verstikt. */
	private static final int MAX_PARTICLES = 600;

	private record Selectie(BlokPos een, BlokPos twee) {
	}

	private static final class Getoond {
		final Regio regio;
		int ticksOver = TOON_TICKS;

		Getoond(Regio regio) {
			this.regio = regio;
		}
	}

	private static final Map<UUID, Selectie> SELECTIES = new HashMap<>();
	private static final List<Getoond> GETOOND = new ArrayList<>();

	private Wand() {
	}

	public static void init() {
		AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
			if (!(player instanceof ServerPlayer speler) || !isWand(player.getItemInHand(hand))) {
				return InteractionResult.PASS;
			}
			zetHoek(speler, pos, true);
			// Niet doorgeven: in creative zou de klik het blok slopen.
			return InteractionResult.SUCCESS;
		});
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (!(player instanceof ServerPlayer speler) || hand != InteractionHand.MAIN_HAND || !isWand(player.getItemInHand(hand))) {
				return InteractionResult.PASS;
			}
			zetHoek(speler, hit.getBlockPos(), false);
			return InteractionResult.SUCCESS;
		});
	}

	public static void geef(ServerPlayer speler) {
		ItemStack stick = new ItemStack(Items.STICK);
		CompoundTag tag = new CompoundTag();
		tag.putBoolean(TAG, true);
		stick.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
		stick.set(DataComponents.ITEM_NAME, Mc.tekst("Bootcamp-wand", ChatFormatting.GOLD));
		stick.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		if (!speler.getInventory().add(stick)) {
			speler.drop(stick, false);
		}
	}

	private static boolean isWand(ItemStack stack) {
		if (!stack.is(Items.STICK)) {
			return false;
		}
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		return data != null && data.copyTag().getBooleanOr(TAG, false);
	}

	private static void zetHoek(ServerPlayer speler, BlockPos pos, boolean eerste) {
		BlokPos hoek = new BlokPos(pos.getX(), pos.getY(), pos.getZ());
		Selectie oud = SELECTIES.get(speler.getUUID());
		Selectie nieuw = eerste
				? new Selectie(hoek, oud == null ? null : oud.twee())
				: new Selectie(oud == null ? null : oud.een(), hoek);
		if (nieuw.equals(oud)) {
			// Een klik geeft soms twee events; één melding is genoeg.
			return;
		}
		SELECTIES.put(speler.getUUID(), nieuw);

		String tekst = "Hoek " + (eerste ? "1" : "2") + ": " + hoek.x() + " " + hoek.y() + " " + hoek.z();
		Regio regio = selectie(speler);
		if (regio != null) {
			tekst += "  (" + regio.breedteX() + " x " + regio.hoogte() + " x " + regio.breedteZ() + ")";
		}
		speler.sendSystemMessage(Mc.tekst(tekst, ChatFormatting.YELLOW), true);
	}

	/** De selectie van deze speler, of {@code null} als er nog geen twee hoeken zijn. */
	public static Regio selectie(Player speler) {
		Selectie s = SELECTIES.get(speler.getUUID());
		if (s == null || s.een() == null || s.twee() == null) {
			return null;
		}
		return Regio.van(s.een(), s.twee());
	}

	public static void toon(Regio regio) {
		GETOOND.add(new Getoond(regio));
	}

	/** Elke servertick. */
	public static void tick(MinecraftServer server) {
		if (GETOOND.isEmpty()) {
			return;
		}
		ServerLevel wereld = Mc.wereld(server);
		for (Iterator<Getoond> it = GETOOND.iterator(); it.hasNext(); ) {
			Getoond g = it.next();
			if (g.ticksOver % TEKEN_ELKE == 0) {
				teken(wereld, g.regio);
			}
			if (--g.ticksOver <= 0) {
				it.remove();
			}
		}
	}

	/** Particles op de twaalf randen van de doos. */
	private static void teken(ServerLevel wereld, Regio r) {
		double x0 = r.min().x();
		double y0 = r.min().y();
		double z0 = r.min().z();
		double x1 = r.max().x() + 1;
		double y1 = r.max().y() + 1;
		double z1 = r.max().z() + 1;

		double omtrek = 4 * ((x1 - x0) + (y1 - y0) + (z1 - z0));
		double stap = Math.max(1.0, omtrek / MAX_PARTICLES);

		for (double x = x0; x <= x1; x += stap) {
			punt(wereld, x, y0, z0);
			punt(wereld, x, y0, z1);
			punt(wereld, x, y1, z0);
			punt(wereld, x, y1, z1);
		}
		for (double y = y0; y <= y1; y += stap) {
			punt(wereld, x0, y, z0);
			punt(wereld, x0, y, z1);
			punt(wereld, x1, y, z0);
			punt(wereld, x1, y, z1);
		}
		for (double z = z0; z <= z1; z += stap) {
			punt(wereld, x0, y0, z);
			punt(wereld, x0, y1, z);
			punt(wereld, x1, y0, z);
			punt(wereld, x1, y1, z);
		}
	}

	private static void punt(ServerLevel wereld, double x, double y, double z) {
		wereld.sendParticles(ParticleTypes.END_ROD, true, true, x, y, z, 1, 0, 0, 0, 0);
	}
}
