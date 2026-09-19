package nl.pudding.bootcamp.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import nl.pudding.bootcamp.config.ConfigStore;
import nl.pudding.bootcamp.core.BootcampConfig;
import nl.pudding.bootcamp.game.Reset;

import java.util.List;

/** {@code /bc start|stop|timer|status|poort|kit|reset}: wat de commander op de avond zelf gebruikt. */
final class SpelCommands {
	private SpelCommands() {
	}

	static void voegToe(LiteralArgumentBuilder<CommandSourceStack> bc) {
		bc.then(Commands.literal("start").then(Commands.argument("ronde", IntegerArgumentType.integer(1, 6))
				.executes(ctx -> nogNiet(ctx, "start", "T6"))));
		bc.then(Commands.literal("stop").executes(ctx -> nogNiet(ctx, "stop", "T6")));
		bc.then(Commands.literal("timer").then(Commands.argument("sec", IntegerArgumentType.integer(0, 3600))
				.executes(ctx -> nogNiet(ctx, "timer", "T6"))));
		bc.then(Commands.literal("poort").then(Commands.argument("naam", StringArgumentType.word())
				.then(Commands.literal("open").executes(ctx -> nogNiet(ctx, "poort", "T6")))
				.then(Commands.literal("dicht").executes(ctx -> nogNiet(ctx, "poort", "T6")))));
		bc.then(Commands.literal("kit").then(Commands.argument("naam", StringArgumentType.word())
				.executes(ctx -> nogNiet(ctx, "kit", "T5"))
				.then(Commands.argument("speler", EntityArgument.player())
						.executes(ctx -> nogNiet(ctx, "kit", "T5")))));
		bc.then(Commands.literal("status").executes(SpelCommands::status));
		bc.then(Commands.literal("reset").executes(SpelCommands::reset));
	}

	static int nogNiet(CommandContext<CommandSourceStack> ctx, String command, String taak) {
		return BcCommand.fout(ctx, "/bc " + command + " is nog niet gebouwd (komt in " + taak + ").");
	}

	private static int status(CommandContext<CommandSourceStack> ctx) {
		BootcampConfig c = ConfigStore.get();
		StringBuilder sb = new StringBuilder("Bootcamp-status");
		sb.append("\n  config: ").append(c.regios().size()).append(" regio's, ").append(c.punten().size()).append(" punten");
		sb.append("\n  uitverkoren: ").append(c.uitverkoren() == null ? "niemand" : c.uitverkoren());
		sb.append("\n  slots: ").append(c.slots().isEmpty() ? "geen" : c.slots().toString());
		return BcCommand.info(ctx, sb.toString());
	}

	private static int reset(CommandContext<CommandSourceStack> ctx) {
		List<String> mislukt = Reset.draai(ctx.getSource().getServer());
		if (!mislukt.isEmpty()) {
			return BcCommand.fout(ctx, "Reset gedaan, maar deze stappen faalden (zie console): " + String.join(", ", mislukt));
		}
		return BcCommand.ok(ctx, "Reset: alles terug naar de basiskamp-staat.");
	}
}
