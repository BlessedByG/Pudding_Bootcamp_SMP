package nl.pudding.bootcamp.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
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
		cmd.then(Commands.literal("einde").executes(DoolhofCommand::einde));
		cmd.then(Commands.literal("poortmelding")
				.executes(DoolhofCommand::toonPoortMelding)
				.then(Commands.literal("aan").executes(ctx -> poortMelding(ctx, true)))
				.then(Commands.literal("uit").executes(ctx -> poortMelding(ctx, false))));
		cmd.then(Commands.literal("valmobs")
				.executes(DoolhofCommand::toonValMobs)
				// Eén getal: altijd zoveel. Twee: willekeurig van het ene t/m het andere.
				.then(Commands.argument("min", IntegerArgumentType.integer(0, Instellingen.MAX_VAL_MOBS))
						.executes(ctx -> valMobs(ctx, IntegerArgumentType.getInteger(ctx, "min"), IntegerArgumentType.getInteger(ctx, "min")))
						.then(Commands.argument("max", IntegerArgumentType.integer(0, Instellingen.MAX_VAL_MOBS))
								.executes(ctx -> valMobs(ctx, IntegerArgumentType.getInteger(ctx, "min"), IntegerArgumentType.getInteger(ctx, "max"))))));
		return cmd;
	}

	/** Noodknop: het doolhof nu afsluiten zoals bij de timer, ook tijdens het gif. */
	private static int einde(CommandContext<CommandSourceStack> ctx) {
		Doolhof doolhof = RondeCommands.lopend(Doolhof.class);
		if (doolhof == null) {
			return BcCommand.fout(ctx, "Het doolhof loopt niet.");
		}
		doolhof.eindeNu(ctx.getSource().getServer());
		return BcCommand.ok(ctx, "Doolhof voorbij: wie nog geen team had zit in het kleinste, iedereen is bij v2.");
	}

	private static int toonPoortMelding(CommandContext<CommandSourceStack> ctx) {
		return BcCommand.info(ctx, "Hoorn en title als de uitgang opengaat: " + (Spel.instellingen().poortMelding() ? "aan" : "uit") + ".");
	}

	private static int poortMelding(CommandContext<CommandSourceStack> ctx, boolean aan) {
		Spel.instellingen().zetPoortMelding(aan);
		return BcCommand.bewaard(ctx, aan
				? "Als de uitgang opengaat, hoort iedereen de hoorn en ziet DE UITGANG IS OPEN."
				: "De uitgang gaat voortaan stil open: geen hoorn, geen title.");
	}

	private static int toonValMobs(CommandContext<CommandSourceStack> ctx) {
		Instellingen i = Spel.instellingen();
		return BcCommand.info(ctx, "Uit een valkist komen " + aantal(i.valMobsMin(), i.valMobsMax()) + " mobs (husks en silverfish), standaard "
				+ aantal(Instellingen.VAL_MOBS_MIN, Instellingen.VAL_MOBS_MAX) + ".");
	}

	private static int valMobs(CommandContext<CommandSourceStack> ctx, int min, int max) {
		String fout = Instellingen.checkValMobs(min, max);
		if (fout != null) {
			return BcCommand.fout(ctx, "Kan niet: " + fout + ".");
		}
		Spel.instellingen().zetValMobs(min, max);
		return BcCommand.bewaard(ctx, max == 0
				? "Een valkist geeft nu alleen de jumpscare, zonder mobs."
				: "Uit een valkist komen nu " + aantal(min, max) + " mobs.");
	}

	/** {@code 5} of {@code willekeurig 3 t/m 10}. */
	private static String aantal(int min, int max) {
		return min == max ? String.valueOf(min) : "willekeurig " + min + " t/m " + max;
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
		String fout = open ? Poorten.open(server, Doolhof.POORT, Spel.instellingen().poortMelding()) : Poorten.dicht(server, Doolhof.POORT);
		if (fout != null) {
			return BcCommand.fout(ctx, "Poort: " + fout + ".");
		}
		return BcCommand.ok(ctx, "Poort " + (open ? "open." : "dicht."));
	}
}
