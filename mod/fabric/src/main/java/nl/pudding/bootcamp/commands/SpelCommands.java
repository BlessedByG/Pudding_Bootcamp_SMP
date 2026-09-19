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
import nl.pudding.bootcamp.game.Reset;
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
				.executes(ctx -> nogNiet(ctx, "start", "T6"))));
		bc.then(Commands.literal("stop").executes(ctx -> nogNiet(ctx, "stop", "T6")));
		bc.then(Commands.literal("timer").then(Commands.argument("sec", IntegerArgumentType.integer(0, 3600))
				.executes(ctx -> nogNiet(ctx, "timer", "T6"))));
		bc.then(Commands.literal("poort").then(Commands.argument("naam", StringArgumentType.word())
				.then(Commands.literal("open").executes(ctx -> nogNiet(ctx, "poort", "T6")))
				.then(Commands.literal("dicht").executes(ctx -> nogNiet(ctx, "poort", "T6")))));
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
