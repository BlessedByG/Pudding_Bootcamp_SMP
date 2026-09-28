package nl.pudding.bootcamp.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import nl.pudding.bootcamp.core.Regels;
import nl.pudding.bootcamp.core.Ronde;
import nl.pudding.bootcamp.game.Spel;
import nl.pudding.bootcamp.game.ronde6.Ffa;

/** {@code /ffa}: ronde 6, met de kroning aan het eind. */
final class FfaCommand {
	private FfaCommand() {
	}

	static LiteralArgumentBuilder<CommandSourceStack> maak() {
		LiteralArgumentBuilder<CommandSourceStack> cmd = BcCommand.op("ffa");
		RondeCommands.startStop(cmd, Ronde.FFA);
		cmd.then(Commands.literal("go").executes(FfaCommand::go));
		cmd.then(Commands.literal("krimp").then(Commands.argument("grootte", IntegerArgumentType.integer(1, 1000))
				.executes(ctx -> krimp(ctx, Regels.KRIMP_SECONDEN))
				.then(Commands.argument("seconden", IntegerArgumentType.integer(1, 3600))
						.executes(ctx -> krimp(ctx, IntegerArgumentType.getInteger(ctx, "seconden"))))));
		cmd.then(RondeCommands.tekst("wachttekst", "De wachttekst", () -> Spel.instellingen().ffaWachttekst(),
				t -> Spel.instellingen().zetFfaWachttekst(t)));
		return cmd;
	}

	private static int go(CommandContext<CommandSourceStack> ctx) {
		Ffa ronde = RondeCommands.lopend(Ffa.class);
		if (ronde == null) {
			return BcCommand.fout(ctx, "De FFA loopt niet (/ffa start).");
		}
		String fout = ronde.go(ctx.getSource().getServer());
		if (fout != null) {
			return BcCommand.fout(ctx, "Kan niet: " + fout + ".");
		}
		return BcCommand.ok(ctx, "Tien seconden, dan is iedereen los.");
	}

	private static int krimp(CommandContext<CommandSourceStack> ctx, int seconden) {
		Ffa ronde = RondeCommands.lopend(Ffa.class);
		if (ronde == null) {
			return BcCommand.fout(ctx, "De FFA loopt niet.");
		}
		int grootte = IntegerArgumentType.getInteger(ctx, "grootte");
		ronde.krimp(ctx.getSource().getServer(), grootte, seconden);
		return BcCommand.ok(ctx, "De border krimpt naar " + grootte + " in " + seconden + " seconden.");
	}
}
