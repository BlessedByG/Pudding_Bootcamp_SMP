package nl.pudding.bootcamp.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import nl.pudding.bootcamp.config.ConfigStore;
import nl.pudding.bootcamp.core.BlokPos;
import nl.pudding.bootcamp.core.Kleur;
import nl.pudding.bootcamp.core.Punt;
import nl.pudding.bootcamp.core.Ronde;
import nl.pudding.bootcamp.game.ronde4.Quiz;

import java.util.List;
import java.util.function.BiFunction;

/** {@code /quiz}: ronde 4. */
final class QuizCommand {
	private QuizCommand() {
	}

	static LiteralArgumentBuilder<CommandSourceStack> maak() {
		LiteralArgumentBuilder<CommandSourceStack> cmd = BcCommand.op("quiz");
		RondeCommands.startStop(cmd, Ronde.QUIZ);
		cmd.then(Commands.literal("presentator")
				.executes(ctx -> {
					String p = ConfigStore.get().presentator();
					return BcCommand.info(ctx, p == null ? "Er is nog geen presentator." : "Presentator: " + p + ".");
				})
				.then(Commands.argument("speler", StringArgumentType.word()).suggests(BcCommand.ONLINE).executes(ctx -> {
					String naam = StringArgumentType.getString(ctx, "speler");
					ConfigStore.get().zetPresentator(naam);
					return BcCommand.bewaard(ctx, naam + " presenteert de quiz.");
				})));
		cmd.then(Commands.literal("bank").then(Commands.argument("kleur", StringArgumentType.word()).suggests(BcCommand.KLEUREN)
				.executes(QuizCommand::bank)));
		cmd.then(Commands.literal("podium").executes(ctx -> {
			ServerPlayer speler = ctx.getSource().getPlayerOrException();
			Punt p = SetupCommands.zetPunt(speler, "quiz_podium");
			return BcCommand.bewaard(ctx, "Podium (quiz_podium) gezet op " + SetupCommands.beschrijfPunt(p));
		}));
		// Elke keer een lamp erbij; met een nummer die ene opnieuw; wis haalt ze allemaal weg.
		cmd.then(Commands.literal("lamp").then(Commands.argument("kleur", StringArgumentType.word()).suggests(BcCommand.KLEUREN)
				.executes(ctx -> lamp(ctx, 0))
				.then(Commands.argument("nummer", IntegerArgumentType.integer(1, 50))
						.executes(ctx -> lamp(ctx, IntegerArgumentType.getInteger(ctx, "nummer"))))
				.then(Commands.literal("wis").executes(QuizCommand::lampenWis))));
		cmd.then(Commands.literal("vuurwerk").then(Commands.argument("kleur", StringArgumentType.word()).suggests(BcCommand.KLEUREN)
				.then(Commands.argument("nummer", IntegerArgumentType.integer(1, Quiz.VUURWERK_PER_BANK))
						.executes(QuizCommand::vuurwerk))));
		cmd.then(Commands.literal("draai").executes(ctx -> actie(ctx, (q, s) -> q.draai(s), "Het rad draait.")));
		cmd.then(Commands.literal("beurt").then(Commands.argument("kleur", StringArgumentType.word()).suggests(BcCommand.KLEUREN)
				.executes(QuizCommand::beurt)));
		cmd.then(Commands.literal("goed").executes(ctx -> actie(ctx, (q, s) -> q.goed(s), "Goed.")));
		cmd.then(Commands.literal("fout").executes(ctx -> actie(ctx, (q, s) -> q.fout(s), "Fout.")));
		cmd.then(Commands.literal("punt").then(Commands.argument("kleur", StringArgumentType.word()).suggests(BcCommand.KLEUREN)
				.executes(ctx -> punt(ctx, 1))
				.then(Commands.argument("aantal", IntegerArgumentType.integer(-100, 100))
						.executes(ctx -> punt(ctx, IntegerArgumentType.getInteger(ctx, "aantal"))))));
		cmd.then(Commands.literal("einde").executes(ctx -> actie(ctx, (q, s) -> q.einde(s), "De quiz is voorbij.")));
		cmd.then(Commands.literal("naararena").executes(ctx -> actie(ctx, (q, s) -> q.naarArena(s), "Iedereen gaat naar de tribune van de Arena.")));
		cmd.then(Commands.literal("winnaar").then(Commands.argument("kleur", StringArgumentType.word()).suggests(BcCommand.KLEUREN)
				.executes(QuizCommand::winnaar)));
		return cmd;
	}

	private static int actie(CommandContext<CommandSourceStack> ctx, BiFunction<Quiz, net.minecraft.server.MinecraftServer, String> wat, String gelukt) {
		Quiz quiz = RondeCommands.lopend(Quiz.class);
		if (quiz == null) {
			return BcCommand.fout(ctx, "De quiz loopt niet (/quiz start).");
		}
		String fout = wat.apply(quiz, ctx.getSource().getServer());
		if (fout != null) {
			return BcCommand.fout(ctx, fout + ".");
		}
		return BcCommand.info(ctx, gelukt);
	}

	private static int bank(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer speler = ctx.getSource().getPlayerOrException();
		Kleur k = BcCommand.kleur(ctx, "kleur");
		if (k == null) {
			return BcCommand.fout(ctx, "Kies rood, blauw, groen of geel.");
		}
		Punt p = SetupCommands.zetPunt(speler, "quiz_" + k.id());
		return BcCommand.bewaard(ctx, "Bank van " + k.naam() + " (quiz_" + k.id() + ") gezet op " + SetupCommands.beschrijfPunt(p));
	}

	/** Een lamp bij de bank van een team, kijkend naar de lamp. Nummer 0: de volgende vrije. */
	private static int lamp(CommandContext<CommandSourceStack> ctx, int nummer) throws CommandSyntaxException {
		ServerPlayer speler = ctx.getSource().getPlayerOrException();
		Kleur k = BcCommand.kleur(ctx, "kleur");
		if (k == null) {
			return BcCommand.fout(ctx, "Kies rood, blauw, groen of geel.");
		}
		String prefix = "quizlamp_" + k.id() + "_";
		int n = nummer > 0 ? nummer : Ronde.volgendVrij(prefix, ConfigStore.get().punten().keySet());
		Punt p = SetupCommands.zetBlokPunt(speler, prefix + n);
		if (p == null) {
			return BcCommand.fout(ctx, "Kijk naar de lamp (binnen 32 blokken).");
		}
		int aantal = Ronde.allemaal(prefix, ConfigStore.get().punten().keySet()).size();
		return BcCommand.bewaard(ctx, "Lamp " + n + " van " + k.naam() + " (" + prefix + n + ") gezet op " + SetupCommands.beschrijfPunt(p)
				+ ". " + k.naam() + " heeft nu " + aantal + (aantal == 1 ? " lamp." : " lampen."));
	}

	/** {@code /quiz lamp <kleur> wis}: alle lampen van dat team weg, om opnieuw te beginnen. */
	private static int lampenWis(CommandContext<CommandSourceStack> ctx) {
		Kleur k = BcCommand.kleur(ctx, "kleur");
		if (k == null) {
			return BcCommand.fout(ctx, "Kies rood, blauw, groen of geel.");
		}
		List<String> weg = Ronde.allemaal("quizlamp_" + k.id() + "_", ConfigStore.get().punten().keySet());
		weg.forEach(ConfigStore.get().punten()::remove);
		return BcCommand.bewaard(ctx, weg.size() + (weg.size() == 1 ? " lamp" : " lampen") + " van " + k.naam() + " weggehaald.");
	}

	/** Een dispenser bij de bank van een team, waar bij een goed antwoord een vuurpijl uit komt. */
	private static int vuurwerk(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer speler = ctx.getSource().getPlayerOrException();
		Kleur k = BcCommand.kleur(ctx, "kleur");
		if (k == null) {
			return BcCommand.fout(ctx, "Kies rood, blauw, groen of geel.");
		}
		String naam = Quiz.vuurwerkPunt(k, IntegerArgumentType.getInteger(ctx, "nummer"));
		Punt p = SetupCommands.zetBlokPunt(speler, naam);
		if (p == null) {
			return BcCommand.fout(ctx, "Kijk naar de dispenser (binnen 32 blokken).");
		}
		BlokPos b = p.blokPos();
		if (!speler.level().getBlockState(new BlockPos(b.x(), b.y(), b.z())).is(Blocks.DISPENSER)) {
			ConfigStore.get().punten().remove(naam);
			return BcCommand.fout(ctx, "Dat blok is geen dispenser.");
		}
		return BcCommand.bewaard(ctx, "Vuurwerk " + IntegerArgumentType.getInteger(ctx, "nummer") + " van " + k.naam() + " (" + naam
				+ ") gezet op " + SetupCommands.beschrijfPunt(p) + ". De pijl vliegt de kant op waar de dispenser naartoe wijst.");
	}

	private static int punt(CommandContext<CommandSourceStack> ctx, int aantal) {
		Quiz quiz = RondeCommands.lopend(Quiz.class);
		if (quiz == null) {
			return BcCommand.fout(ctx, "De quiz loopt niet.");
		}
		Kleur k = BcCommand.kleur(ctx, "kleur");
		if (k == null) {
			return BcCommand.fout(ctx, "Kies rood, blauw, groen of geel.");
		}
		quiz.punt(ctx.getSource().getServer(), k, aantal);
		return BcCommand.ok(ctx, (aantal >= 0 ? "+" : "") + aantal + " voor " + k.naam() + ".");
	}

	/** Noodknop voor het kompas: dit team meteen aan de beurt, zonder rad. */
	private static int beurt(CommandContext<CommandSourceStack> ctx) {
		Quiz quiz = RondeCommands.lopend(Quiz.class);
		if (quiz == null) {
			return BcCommand.fout(ctx, "De quiz loopt niet.");
		}
		Kleur k = BcCommand.kleur(ctx, "kleur");
		if (k == null) {
			return BcCommand.fout(ctx, "Kies rood, blauw, groen of geel.");
		}
		String fout = quiz.geefBeurt(ctx.getSource().getServer(), k);
		if (fout != null) {
			return BcCommand.fout(ctx, fout + ".");
		}
		return BcCommand.info(ctx, k.naam() + " is aan de beurt.");
	}

	private static int winnaar(CommandContext<CommandSourceStack> ctx) {
		Quiz quiz = RondeCommands.lopend(Quiz.class);
		if (quiz == null) {
			return BcCommand.fout(ctx, "De quiz loopt niet.");
		}
		Kleur k = BcCommand.kleur(ctx, "kleur");
		if (k == null) {
			return BcCommand.fout(ctx, "Kies rood, blauw, groen of geel.");
		}
		String fout = quiz.winnaar(ctx.getSource().getServer(), k);
		if (fout != null) {
			return BcCommand.fout(ctx, fout + ".");
		}
		return BcCommand.ok(ctx, k.naam() + " wint de quiz.");
	}
}
