package nl.pudding.bootcamp.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.MinecraftServer;
import nl.pudding.bootcamp.config.ConfigStore;
import nl.pudding.bootcamp.core.Instellingen;
import nl.pudding.bootcamp.core.Regels;
import nl.pudding.bootcamp.core.Ronde;
import nl.pudding.bootcamp.game.Poorten;
import nl.pudding.bootcamp.game.Spel;
import nl.pudding.bootcamp.game.ronde1.Doolhof;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
		cmd.then(Commands.literal("startpoort")
				.then(Commands.literal("open").executes(ctx -> startpoort(ctx, true)))
				.then(Commands.literal("dicht").executes(ctx -> startpoort(ctx, false))));
		// "/doolhof poort 4" is de instelling; "/doolhof poort open" de poort zelf.
		cmd.then(RondeCommands.minuten("poort", "De poort gaat open na", () -> Spel.instellingen().doolhofPoort(),
				min -> Spel.instellingen().checkDoolhofPoort(min), min -> Spel.instellingen().zetDoolhofPoort(min), null));
		cmd.then(RondeCommands.minuten("hint", "De hint komt na", () -> Spel.instellingen().doolhofHint(),
				min -> Spel.instellingen().checkDoolhofHint(min), min -> Spel.instellingen().zetDoolhofHint(min), null));
		cmd.then(RondeCommands.tekst("hinttekst", "De hinttekst", () -> Spel.instellingen().hinttekst(),
				t -> Spel.instellingen().zetHinttekst(t)));
		cmd.then(Commands.literal("go").executes(DoolhofCommand::go));
		cmd.then(Commands.literal("einde").executes(DoolhofCommand::einde));
		cmd.then(Commands.literal("naarei").executes(DoolhofCommand::naarEi));
		cmd.then(RondeCommands.tekst("wachttekst", "De wachttekst", () -> Spel.instellingen().doolhofWachttekst(),
				t -> Spel.instellingen().zetDoolhofWachttekst(t)));
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
		cmd.then(Commands.literal("schrik")
				.executes(DoolhofCommand::toonSchrikFotos)
				.then(Commands.argument("nummer", IntegerArgumentType.integer(1, 200))
						.executes(DoolhofCommand::toonSchrikFoto)
						.then(Commands.argument("foto", StringArgumentType.word()).suggests(FOTOS)
								.executes(DoolhofCommand::zetSchrikFoto))));
		return cmd;
	}

	/** Foto 1 t/m 5, of willekeurig. */
	private static final SuggestionProvider<CommandSourceStack> FOTOS = (ctx, b) -> {
		List<String> opties = new ArrayList<>(List.of("random"));
		for (int i = 1; i <= Regels.SCHRIK_FOTOS; i++) {
			opties.add(String.valueOf(i));
		}
		return SharedSuggestionProvider.suggest(opties, b);
	};

	/** {@code /doolhof schrik}: elke schrikplek met zijn foto. */
	private static int toonSchrikFotos(CommandContext<CommandSourceStack> ctx) {
		List<String> plekken = Ronde.reeks("schrik_", ConfigStore.get().regios().keySet());
		if (plekken.isEmpty()) {
			return BcCommand.info(ctx, "Er zijn nog geen schrikplekken (/bc region save schrik_1).");
		}
		StringBuilder sb = new StringBuilder("Schrikplekken:");
		for (String p : plekken) {
			sb.append("\n  ").append(p).append(": ").append(fotoTekst(Spel.instellingen().schrikFoto(p)));
		}
		return BcCommand.info(ctx, sb.toString());
	}

	private static int toonSchrikFoto(CommandContext<CommandSourceStack> ctx) {
		String plek = "schrik_" + IntegerArgumentType.getInteger(ctx, "nummer");
		return BcCommand.info(ctx, plek + ": " + fotoTekst(Spel.instellingen().schrikFoto(plek)) + ".");
	}

	/** {@code /doolhof schrik <nr> <1..5|random>}: een vaste foto voor die plek, of willekeurig. */
	private static int zetSchrikFoto(CommandContext<CommandSourceStack> ctx) {
		String plek = "schrik_" + IntegerArgumentType.getInteger(ctx, "nummer");
		String keuze = StringArgumentType.getString(ctx, "foto").toLowerCase(Locale.ROOT);
		int foto;
		if (keuze.equals("random") || keuze.equals("willekeurig")) {
			foto = 0;
		} else {
			try {
				foto = Integer.parseInt(keuze);
			} catch (NumberFormatException e) {
				foto = -1;
			}
		}
		if (Instellingen.checkSchrikFoto(foto) != null) {
			return BcCommand.fout(ctx, "Kies foto 1 t/m " + Regels.SCHRIK_FOTOS + " of random.");
		}
		Spel.instellingen().zetSchrikFoto(plek, foto);
		String erbij = ConfigStore.get().regios().containsKey(plek) ? "" : " (de regio " + plek + " is er nog niet)";
		return BcCommand.bewaard(ctx, plek + ": " + fotoTekst(foto) + "." + erbij);
	}

	private static String fotoTekst(int foto) {
		return foto == 0 ? "willekeurige foto" : "foto " + foto;
	}

	/** Na {@code /doolhof start}: de countdown, daarna gaat de startpoort open. */
	private static int go(CommandContext<CommandSourceStack> ctx) {
		Doolhof doolhof = RondeCommands.lopend(Doolhof.class);
		if (doolhof == null) {
			return BcCommand.fout(ctx, "Het doolhof loopt niet (/doolhof start).");
		}
		String fout = doolhof.go(ctx.getSource().getServer());
		if (fout != null) {
			return BcCommand.fout(ctx, "Kan niet: " + fout + ".");
		}
		return BcCommand.ok(ctx, Regels.COUNTDOWN + " seconden, dan begint het doolhof.");
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

	/** Na het doolhof: iedereen van de finishruimte naar v2 bij het Ei. */
	private static int naarEi(CommandContext<CommandSourceStack> ctx) {
		String fout = Doolhof.naarHetEi(ctx.getSource().getServer());
		if (fout != null) {
			return BcCommand.fout(ctx, "Nog niet: " + fout + ".");
		}
		return BcCommand.ok(ctx, "Iedereen is naar v2, bij het Ei.");
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
				? "Een valkist geeft nu alleen de jumpscare of de 8D-klop, zonder mobs."
				: "Uit een valkist komen nu " + aantal(min, max) + " mobs.");
	}

	/** {@code 5} of {@code willekeurig 3 t/m 10}. */
	private static String aantal(int min, int max) {
		return min == max ? String.valueOf(min) : "willekeurig " + min + " t/m " + max;
	}

	/** De openingen van de startruimte met de hand, om te testen. Open gaat stil, net als bij de start. */
	private static int startpoort(CommandContext<CommandSourceStack> ctx, boolean open) {
		MinecraftServer server = ctx.getSource().getServer();
		String fout = open ? Poorten.open(server, Doolhof.START_POORT, false) : Poorten.dicht(server, Doolhof.START_POORT);
		if (fout != null) {
			return BcCommand.fout(ctx, "Startpoort: " + fout + ".");
		}
		return BcCommand.ok(ctx, "Startpoort " + (open ? "open." : "dicht."));
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
