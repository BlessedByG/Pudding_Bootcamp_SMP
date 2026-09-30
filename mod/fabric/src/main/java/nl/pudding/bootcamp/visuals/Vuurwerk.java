package nl.pudding.bootcamp.visuals;

import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** Vuurpijlen: een grote bol met staart, vluchtduur 1. */
public final class Vuurwerk {
	public static final int GOUD = 0xFFD700;
	private static final int WIT = 0xFFFFFF;

	private Vuurwerk() {
	}

	/** Schiet een gouden vuurpijl af vanaf dit punt; hij ontploft een stuk hoger. */
	public static void goud(ServerLevel wereld, double x, double y, double z) {
		kleur(wereld, x, y, z, GOUD);
	}

	/** Een vuurpijl in een eigen kleur (de teamkleur), met een witte fade. */
	public static void kleur(ServerLevel wereld, double x, double y, double z, int rgb) {
		wereld.addFreshEntity(new FireworkRocketEntity(wereld, x, y, z, pijl(rgb)));
	}

	/**
	 * Een vuurpijl in deze kleur uit een dispenser, de kant op waar hij naartoe wijst, zoals een
	 * dispenser dat zelf doet. Er hoeft niets in de dispenser te zitten.
	 */
	public static void uitDispenser(ServerLevel wereld, BlockPos pos, int rgb) {
		BlockState state = wereld.getBlockState(pos);
		Direction richting = state.hasProperty(DispenserBlock.FACING) ? state.getValue(DispenserBlock.FACING) : Direction.UP;
		double x = pos.getX() + 0.5 + richting.getStepX() * 0.7;
		double y = pos.getY() + 0.5 + richting.getStepY() * 0.7;
		double z = pos.getZ() + 0.5 + richting.getStepZ() * 0.7;
		FireworkRocketEntity vuurpijl = new FireworkRocketEntity(wereld, pijl(rgb), x, y, z, true);
		vuurpijl.shoot(richting.getStepX(), richting.getStepY(), richting.getStepZ(), 0.5f, 1.0f);
		wereld.addFreshEntity(vuurpijl);
		wereld.playSound(null, x, y, z, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.BLOCKS, 1f, 1f);
	}

	private static ItemStack pijl(int rgb) {
		ItemStack pijl = new ItemStack(Items.FIREWORK_ROCKET);
		FireworkExplosion bol = new FireworkExplosion(FireworkExplosion.Shape.LARGE_BALL, IntList.of(rgb), IntList.of(WIT), true, false);
		pijl.set(DataComponents.FIREWORKS, new Fireworks(1, List.of(bol)));
		return pijl;
	}
}
