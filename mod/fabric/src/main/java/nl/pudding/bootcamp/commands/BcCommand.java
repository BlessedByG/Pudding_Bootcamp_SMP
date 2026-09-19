package nl.pudding.bootcamp.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import nl.pudding.bootcamp.Mc;

/** Het hele {@code /bc}-commandboompje, op op-level 2. De takken staan in de klassen ernaast. */
public final class BcCommand {
	private BcCommand() {
	}

	public static void registreer(CommandDispatcher<CommandSourceStack> dispatcher) {
		LiteralArgumentBuilder<CommandSourceStack> bc = Commands.literal("bc")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
		SetupCommands.voegToe(bc);
		SpelCommands.voegToe(bc);
		KroonCommands.voegToe(bc);
		dispatcher.register(bc);
	}

	// Gedeelde hulpjes voor de takken

	static int ok(CommandContext<CommandSourceStack> ctx, String tekst) {
		ctx.getSource().sendSuccess(() -> Mc.tekst(tekst), true);
		return 1;
	}

	/** Alleen voor wie het typt, niet naar de andere ops en niet in de log. */
	static int info(CommandContext<CommandSourceStack> ctx, String tekst) {
		ctx.getSource().sendSuccess(() -> Mc.tekst(tekst), false);
		return 1;
	}

	static int info(CommandContext<CommandSourceStack> ctx, String tekst, ChatFormatting kleur) {
		ctx.getSource().sendSuccess(() -> Mc.tekst(tekst, kleur), false);
		return 1;
	}

	static int fout(CommandContext<CommandSourceStack> ctx, String tekst) {
		ctx.getSource().sendFailure(Mc.tekst(tekst));
		return 0;
	}
}
