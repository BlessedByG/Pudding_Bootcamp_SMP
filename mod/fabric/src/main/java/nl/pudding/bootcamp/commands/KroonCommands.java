package nl.pudding.bootcamp.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import nl.pudding.bootcamp.config.ConfigStore;
import nl.pudding.bootcamp.core.Ronde;

/** {@code /bc kroon|uitverkoren|slot|rad|kijker}: het rad, de kroon en de noodknoppen van de ref. */
final class KroonCommands {
	/** Namen mogen ook van spelers zijn die nog niet online zijn; online spelers worden voorgesteld. */
	private static final SuggestionProvider<CommandSourceStack> ONLINE =
			(ctx, b) -> SharedSuggestionProvider.suggest(ctx.getSource().getOnlinePlayerNames(), b);

	private KroonCommands() {
	}

	static void voegToe(LiteralArgumentBuilder<CommandSourceStack> bc) {
		bc.then(Commands.literal("kroon").then(Commands.argument("speler", EntityArgument.player())
				.executes(ctx -> SpelCommands.nogNiet(ctx, "kroon", "T11"))));
		bc.then(Commands.literal("rad").executes(ctx -> SpelCommands.nogNiet(ctx, "rad", "T11")));
		bc.then(Commands.literal("kijker").then(Commands.argument("speler", EntityArgument.player())
				.then(Commands.literal("aan").executes(ctx -> SpelCommands.nogNiet(ctx, "kijker", "T7")))
				.then(Commands.literal("uit").executes(ctx -> SpelCommands.nogNiet(ctx, "kijker", "T7")))));

		bc.then(Commands.literal("uitverkoren")
				.executes(KroonCommands::toonUitverkoren)
				.then(Commands.argument("speler", StringArgumentType.word()).suggests(ONLINE)
						.executes(KroonCommands::zetUitverkoren)));
		bc.then(Commands.literal("slot").then(Commands.argument("speler", StringArgumentType.word()).suggests(ONLINE)
				.then(Commands.argument("pilaar", IntegerArgumentType.integer(0, Ronde.AANTAL_LAMPEN - 1))
						.executes(KroonCommands::zetSlot))));
	}

	private static int toonUitverkoren(CommandContext<CommandSourceStack> ctx) {
		String naam = ConfigStore.get().uitverkoren();
		return BcCommand.info(ctx, naam == null ? "Er is nog geen uitverkorene." : "Uitverkoren: " + naam);
	}

	private static int zetUitverkoren(CommandContext<CommandSourceStack> ctx) {
		String naam = StringArgumentType.getString(ctx, "speler");
		ConfigStore.get().zetUitverkoren(naam);
		ConfigStore.bewaar();
		// Niet naar de andere ops en niet in de log: het rad blijft geheim.
		String slot = ConfigStore.get().slotVan(naam) < 0 ? " Let op: die heeft nog geen pilaar (/bc slot)." : "";
		return BcCommand.info(ctx, "Uitverkoren: " + naam + "." + slot);
	}

	private static int zetSlot(CommandContext<CommandSourceStack> ctx) {
		String naam = StringArgumentType.getString(ctx, "speler");
		int pilaar = IntegerArgumentType.getInteger(ctx, "pilaar");
		ConfigStore.get().zetSlot(naam, pilaar);
		ConfigStore.bewaar();
		return BcCommand.info(ctx, "Kop van " + naam + " staat op pilaar " + pilaar + " (lamp_" + pilaar + ").");
	}
}
