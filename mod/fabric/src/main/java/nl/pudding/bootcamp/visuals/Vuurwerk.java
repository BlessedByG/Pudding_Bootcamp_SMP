package nl.pudding.bootcamp.visuals;

import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;

import java.util.List;

/** Vuurpijlen: een grote gouden bol met staart, vluchtduur 1. */
public final class Vuurwerk {
	private static final int GOUD = 0xFFD700;
	private static final int WIT = 0xFFFFFF;

	private Vuurwerk() {
	}

	/** Schiet een gouden vuurpijl af vanaf dit punt; hij ontploft een stuk hoger. */
	public static void goud(ServerLevel wereld, double x, double y, double z) {
		ItemStack pijl = new ItemStack(Items.FIREWORK_ROCKET);
		FireworkExplosion bol = new FireworkExplosion(FireworkExplosion.Shape.LARGE_BALL, IntList.of(GOUD), IntList.of(WIT), true, false);
		pijl.set(DataComponents.FIREWORKS, new Fireworks(1, List.of(bol)));
		wereld.addFreshEntity(new FireworkRocketEntity(wereld, x, y, z, pijl));
	}
}
