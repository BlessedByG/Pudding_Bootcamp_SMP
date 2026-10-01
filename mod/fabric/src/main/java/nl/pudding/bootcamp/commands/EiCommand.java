package nl.pudding.bootcamp.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.MinecraftServer;
import nl.pudding.bootcamp.core.EiBlok;
import nl.pudding.bootcamp.core.Instellingen;
import nl.pudding.bootcamp.core.Ronde;
import nl.pudding.bootcamp.game.Spel;
import nl.pudding.bootcamp.game.ronde2.Ei;
import nl.pudding.bootcamp.game.ronde2.Huldiging;
import nl.pudding.bootcamp.game.ronde2.EiOpslag;

import java.util.Arrays;

/** {@code /ei}: ronde 2. */
final class EiCommand {
	private EiCommand() {
	}

	static LiteralArgumentBuilder<CommandSourceStack> maak() {
		LiteralArgumentBuilder<CommandSourceStack> cmd = BcCommand.op("ei");
		RondeCommands.startStop(cmd, Ronde.EI);
		RondeCommands.resterend(cmd, Ronde.EI);
		cmd.then(RondeCommands.minuten("timer", "Het Ei duurt", () -> Spel.instellingen().eiTimer(),
				min -> {
					String fout = Instellingen.checkTimer(min);
					if (fout == null && RondeCommands.lopend(Ei.class) != null && Spel.timerLoopt()) {
						fout = Instellingen.checkNietVoorbij(min, Spel.gespeeld());
					}
					return fout;
				},
				min -> Spel.instellingen().zetEiTimer(min),
				server -> {
					if (RondeCommands.lopend(Ei.class) != null && Spel.timerLoopt()) {
						Spel.zetTotaal(Spel.instellingen().eiTimer() * 60);
					}
				}));
		cmd.then(Commands.literal("blokken")
				.executes(EiCommand::blokkenOverzicht)
				.then(Commands.argument("soort", StringArgumentType.word())
						.suggests((ctx, b) -> SharedSuggestionProvider.suggest(Arrays.stream(EiBlok.values()).map(EiBlok::id), b))
						.then(Commands.argument("aantal", IntegerArgumentType.integer(0, 100_000)).executes(EiCommand::blokken))));
		cmd.then(Commands.literal("vastleggen").executes(EiCommand::vastleggen));
		cmd.then(Commands.literal("prijskader").executes(EiCommand::prijskader));
		cmd.then(Commands.literal("naarmobarena").executes(EiCommand::naarMobarena));
		return cmd;
	}

	private static int blokkenOverzicht(CommandContext<CommandSourceStack> ctx) {
		MinecraftServer server = ctx.getSource().getServer();
		Instellingen i = Spel.instellingen();
		StringBuilder sb = new StringBuilder("Puntenblokken in het Ei:");
		for (EiBlok b : EiBlok.values()) {
			sb.append("\n  ").append(b.id()).append(": ").append(i.eiBlokken(b));
		}
		int deepslate = EiOpslag.deepslate(server);
		sb.append("\n  samen ").append(i.eiTotaal()).append(", ")
				.append(deepslate < 0 ? "het Ei is nog niet vastgelegd (/ei vastleggen)" : "op " + deepslate + " deepslate-plekken");
		return BcCommand.info(ctx, sb.toString());
	}

	private static int blokken(CommandContext<CommandSourceStack> ctx) {
		EiBlok blok = EiBlok.vanId(StringArgumentType.getString(ctx, "soort"));
		if (blok == null) {
			return BcCommand.fout(ctx, "Kies " + Arrays.stream(EiBlok.values()).map(EiBlok::id).collect(java.util.stream.Collectors.joining(", ")) + ".");
		}
		int aantal = IntegerArgumentType.getInteger(ctx, "aantal");
		String fout = Spel.instellingen().checkEiBlokken(blok, aantal, EiOpslag.deepslate(ctx.getSource().getServer()));
		if (fout != null) {
			return BcCommand.fout(ctx, fout + ".");
		}
		Spel.instellingen().zetEiBlokken(blok, aantal);
		String wanneer = RondeCommands.lopend(Ei.class) != null ? " Geldt vanaf de volgende /ei start." : "";
		return BcCommand.bewaard(ctx, blok.id() + ": " + aantal + " blokken." + wanneer);
	}

	/** Het item frame waar de speler naar kijkt, wordt het frame voor het Warden-ei. */
	private static int prijskader(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		String fout = Huldiging.zetKader(ctx.getSource().getPlayerOrException());
		if (fout != null) {
			return BcCommand.fout(ctx, "Geen frame: " + fout + ".");
		}
		return BcCommand.ok(ctx, "Dit item frame krijgt het Warden-ei als het Ei voorbij is (punt " + Huldiging.KADER + ").");
	}

	/** Na de huldiging: iedereen van het plein naar de tribune van de mob arena. */
	private static int naarMobarena(CommandContext<CommandSourceStack> ctx) {
		String fout = Ei.naarDeMobarena(ctx.getSource().getServer());
		if (fout != null) {
			return BcCommand.fout(ctx, "Nog niet: " + fout + ".");
		}
		return BcCommand.ok(ctx, "Iedereen is naar de tribune van de mob arena.");
	}

	private static int vastleggen(CommandContext<CommandSourceStack> ctx) {
		if (RondeCommands.lopend(Ei.class) != null) {
			return BcCommand.fout(ctx, "Niet tijdens ronde 2: dan is het Ei al uitgehakt.");
		}
		String uit = EiOpslag.vastleggen(ctx.getSource().getServer());
		if (uit.startsWith("!")) {
			return BcCommand.fout(ctx, "Vastleggen lukt niet: " + uit.substring(1) + ".");
		}
		return BcCommand.ok(ctx, uit);
	}
}
