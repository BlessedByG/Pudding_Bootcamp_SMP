package nl.pudding.bootcamp.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import nl.pudding.bootcamp.Mc;
import nl.pudding.bootcamp.config.ConfigStore;
import nl.pudding.bootcamp.core.Kleur;

import java.util.Arrays;

/**
 * Alle commando's, op op-level 2: {@code /bc} voor wat de hele avond geldt (setup, spelers,
 * noodknoppen) en per ronde een eigen commando ({@code /doolhof}, {@code /ei}, {@code /mobarena},
 * {@code /quiz}, {@code /clown}, {@code /ffa}).
 */
public final class BcCommand {
	/** Namen mogen ook van spelers zijn die nog niet online zijn; online spelers worden voorgesteld. */
	static final SuggestionProvider<CommandSourceStack> ONLINE =
			(ctx, b) -> SharedSuggestionProvider.suggest(ctx.getSource().getOnlinePlayerNames(), b);
	static final SuggestionProvider<CommandSourceStack> KLEUREN =
			(ctx, b) -> SharedSuggestionProvider.suggest(Arrays.stream(Kleur.values()).map(Kleur::id), b);

	private BcCommand() {
	}

	public static void registreer(CommandDispatcher<CommandSourceStack> dispatcher) {
		LiteralArgumentBuilder<CommandSourceStack> bc = op("bc");
		SetupCommands.voegToe(bc);
		AlgemeenCommands.voegToe(bc);
		dispatcher.register(bc);

		dispatcher.register(DoolhofCommand.maak());
		dispatcher.register(EiCommand.maak());
		dispatcher.register(MobArenaCommand.maak());
		dispatcher.register(QuizCommand.maak());
		dispatcher.register(ClownCommand.maak());
		dispatcher.register(FfaCommand.maak());
	}

	/** Een commando dat alleen ops (level 2) zien en kunnen gebruiken. */
	static LiteralArgumentBuilder<CommandSourceStack> op(String naam) {
		return Commands.literal(naam).requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
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

	/** Opslaan in {@code bootcamp.json} en melden; mislukt het opslaan, dan staat dat erbij. */
	static int bewaard(CommandContext<CommandSourceStack> ctx, String tekst) {
		if (!ConfigStore.bewaar()) {
			return fout(ctx, tekst + " Maar bootcamp.json kon niet worden opgeslagen, kijk in de console.");
		}
		return ok(ctx, tekst);
	}

	/** Als {@link #bewaard}, maar alleen voor wie het typt (de uitverkorene blijft geheim). */
	static int bewaardStil(CommandContext<CommandSourceStack> ctx, String tekst) {
		if (!ConfigStore.bewaar()) {
			return fout(ctx, tekst + " Maar bootcamp.json kon niet worden opgeslagen, kijk in de console.");
		}
		return info(ctx, tekst);
	}

	static Kleur kleur(CommandContext<CommandSourceStack> ctx, String arg) {
		return Kleur.vanId(com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, arg));
	}
}
