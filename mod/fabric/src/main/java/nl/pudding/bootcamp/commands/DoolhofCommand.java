package nl.pudding.bootcamp.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import nl.pudding.bootcamp.core.Instellingen;
import nl.pudding.bootcamp.core.Ronde;
import nl.pudding.bootcamp.game.Poorten;
import nl.pudding.bootcamp.game.Spel;
import nl.pudding.bootcamp.game.ronde1.Doolhof;

/** {@code /doolhof}: ronde 1. */
final class DoolhofCommand {
	private DoolhofCommand() {
	}

	static LiteralArgumentBuilder<CommandSourceStack> maak() {
		LiteralArgumentBuilder<CommandSourceStack> cmd = BcCommand.op("doolhof");
		RondeCommands.startStop(cmd, Ronde.DOOLHOF);
		RondeCommands.resterend(cmd, Ronde.DOOLHOF);

		cmd.then(RondeCommands.minuten("timer", "Het doolhof duurt", () -> Spel.instellingen().doolhofTimer(),
				min -> {
					String fout = Spel.instellingen().checkDoolhofTimer(min);
					if (fout == null && RondeCommands.lopend(Doolhof.class) != null && Spel.timerLoopt()) {
						fout = Instellingen.checkNietVoorbij(min, Spel.gespeeld());
					}
					return fout;
				},
				min -> Spel.instellingen().zetDoolhofTimer(min),
				server -> {
					if (RondeCommands.lopend(Doolhof.class) != null && Spel.timerLoopt()) {
						Spel.zetTotaal(Spel.instellingen().doolhofTimer() * 60);
					}
				}));
		cmd.then(Commands.literal("poort")
				.then(Commands.literal("open").executes(ctx -> poort(ctx, true)))
				.then(Commands.literal("dicht").executes(ctx -> poort(ctx, false))));
		// "/doolhof poort 4" is de instelling; "/doolhof poort open" de poort zelf.
		cmd.then(RondeCommands.minuten("poort", "De poort gaat open na", () -> Spel.instellingen().doolhofPoort(),
				min -> Spel.instellingen().checkDoolhofPoort(min), min -> Spel.instellingen().zetDoolhofPoort(min), null));
		cmd.then(RondeCommands.minuten("hint", "De hint komt na", () -> Spel.instellingen().doolhofHint(),
				min -> Spel.instellingen().checkDoolhofHint(min), min -> Spel.instellingen().zetDoolhofHint(min), null));
		cmd.then(RondeCommands.tekst("hinttekst", "De hinttekst", () -> Spel.instellingen().hinttekst(),
				t -> Spel.instellingen().zetHinttekst(t)));
		return cmd;
	}

	private static int poort(CommandContext<CommandSourceStack> ctx, boolean open) {
		MinecraftServer server = ctx.getSource().getServer();
		Doolhof doolhof = RondeCommands.lopend(Doolhof.class);
		if (doolhof != null) {
			if (open) {
				doolhof.poortOpen(server);
			} else {
				doolhof.poortDicht(server);
			}
			return BcCommand.ok(ctx, "Poort " + (open ? "open." : "dicht."));
		}
		String fout = open ? Poorten.open(server, Doolhof.POORT) : Poorten.dicht(server, Doolhof.POORT);
		if (fout != null) {
			return BcCommand.fout(ctx, "Poort: " + fout + ".");
		}
		return BcCommand.ok(ctx, "Poort " + (open ? "open." : "dicht."));
	}
}
