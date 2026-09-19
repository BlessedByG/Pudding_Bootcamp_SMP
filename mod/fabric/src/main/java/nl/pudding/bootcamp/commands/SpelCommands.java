package nl.pudding.bootcamp.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import nl.pudding.bootcamp.Mc;
import nl.pudding.bootcamp.config.ConfigStore;
import nl.pudding.bootcamp.config.Standaardbestanden;
import nl.pudding.bootcamp.core.BootcampConfig;
import nl.pudding.bootcamp.core.Ronde;
import nl.pudding.bootcamp.core.Tijd;
import nl.pudding.bootcamp.game.Poorten;
import nl.pudding.bootcamp.game.Reset;
import nl.pudding.bootcamp.game.Spel;
import nl.pudding.bootcamp.kits.Kits;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/** {@code /bc start|stop|timer|status|poort|kit|reset}: wat de commander op de avond zelf gebruikt. */
final class SpelCommands {
	private SpelCommands() {
	}

	static void voegToe(LiteralArgumentBuilder<CommandSourceStack> bc) {
		bc.then(Commands.literal("start").then(Commands.argument("ronde", IntegerArgumentType.integer(1, 6))
				.executes(SpelCommands::start)));
		bc.then(Commands.literal("stop").executes(SpelCommands::stop));
		bc.then(Commands.literal("timer").then(Commands.argument("sec", IntegerArgumentType.integer(1, 3600))
				.executes(SpelCommands::timer)));
		bc.then(Commands.literal("poort").then(Commands.argument("naam", StringArgumentType.word()).suggests(POORTEN)
				.then(Commands.literal("open").executes(ctx -> poort(ctx, true)))
				.then(Commands.literal("dicht").executes(ctx -> poort(ctx, false)))));
		bc.then(Commands.literal("kit").then(Commands.argument("naam", StringArgumentType.word()).suggests(KITS)
				.executes(ctx -> kit(ctx, Mc.deelnemers(ctx.getSource().getServer())))
				.then(Commands.argument("speler", EntityArgument.player())
						.executes(ctx -> kit(ctx, List.of(EntityArgument.getPlayer(ctx, "speler")))))));
		bc.then(Commands.literal("status").executes(SpelCommands::status));
		bc.then(Commands.literal("reset").executes(SpelCommands::reset));
	}

	/** Stelt de kits voor die in {@code config/bootcamp/kits/} staan. */
	private static final SuggestionProvider<CommandSourceStack> KITS = (ctx, b) -> {
		List<String> namen = new ArrayList<>();
		try (Stream<Path> s = Files.list(Standaardbestanden.kitsMap())) {
			s.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(".json"))
					.forEach(n -> namen.add(n.substring(0, n.length() - ".json".length())));
		} catch (IOException e) {
			// Geen map, geen suggesties.
		}
		return SharedSuggestionProvider.suggest(namen, b);
	};

	private static final SuggestionProvider<CommandSourceStack> POORTEN = (ctx, b) -> SharedSuggestionProvider.suggest(
			ConfigStore.get().regios().keySet().stream().filter(n -> n.startsWith(Poorten.PREFIX))
					.map(n -> n.substring(Poorten.PREFIX.length())), b);

	private static int start(CommandContext<CommandSourceStack> ctx) {
		Ronde ronde = Ronde.vanNummer(IntegerArgumentType.getInteger(ctx, "ronde"));
		String fout = Spel.start(ctx.getSource().getServer(), ronde);
		if (fout != null) {
			return BcCommand.fout(ctx, "Ronde " + ronde.nummer() + " start niet, " + fout);
		}
		return BcCommand.ok(ctx, "Ronde " + ronde.nummer() + " gestart: " + ronde.naam() + ".");
	}

	private static int stop(CommandContext<CommandSourceStack> ctx) {
		if (!Spel.loopt()) {
			return BcCommand.fout(ctx, "Er loopt geen ronde.");
		}
		Ronde was = Spel.ronde();
		Spel.stop(ctx.getSource().getServer());
		return BcCommand.ok(ctx, "Ronde " + was.nummer() + " afgebroken: timer stil, border weg, bevriezing eraf.");
	}

	private static int timer(CommandContext<CommandSourceStack> ctx) {
		if (!Spel.loopt() || !Spel.timerLoopt()) {
			return BcCommand.fout(ctx, "Er loopt geen timer.");
		}
		int sec = IntegerArgumentType.getInteger(ctx, "sec");
		Spel.zetTimer(sec);
		return BcCommand.ok(ctx, "Timer op " + Tijd.mmss(sec) + ".");
	}

	private static int poort(CommandContext<CommandSourceStack> ctx, boolean open) {
		String naam = StringArgumentType.getString(ctx, "naam").toLowerCase(Locale.ROOT);
		String fout = open ? Poorten.open(ctx.getSource().getServer(), naam) : Poorten.dicht(ctx.getSource().getServer(), naam);
		if (fout != null) {
			return BcCommand.fout(ctx, "Poort: " + fout + ".");
		}
		return BcCommand.ok(ctx, "Poort " + naam + (open ? " open." : " dicht."));
	}

	private static int kit(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> spelers) {
		String naam = StringArgumentType.getString(ctx, "naam").toLowerCase(Locale.ROOT);
		String fout = Kits.geefAan(ctx.getSource().getServer(), naam, spelers);
		if (fout != null) {
			return BcCommand.fout(ctx, "Kit: " + fout);
		}
		String wie = spelers.size() == 1 ? Mc.naam(spelers.iterator().next()) : spelers.size() + " spelers";
		return BcCommand.ok(ctx, "Kit " + naam + " gezet op " + wie + ".");
	}

	static int nogNiet(CommandContext<CommandSourceStack> ctx, String command, String taak) {
		return BcCommand.fout(ctx, "/bc " + command + " is nog niet gebouwd (komt in " + taak + ").");
	}

	private static int status(CommandContext<CommandSourceStack> ctx) {
		BootcampConfig c = ConfigStore.get();
		StringBuilder sb = new StringBuilder(Spel.statusTekst(ctx.getSource().getServer()));
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
