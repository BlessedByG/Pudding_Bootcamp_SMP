package nl.pudding.bootcamp.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import nl.pudding.bootcamp.config.ConfigStore;
import nl.pudding.bootcamp.core.BootcampConfig;
import nl.pudding.bootcamp.core.Punt;
import nl.pudding.bootcamp.core.Regels;
import nl.pudding.bootcamp.core.Ronde;
import nl.pudding.bootcamp.game.Spel;
import nl.pudding.bootcamp.game.ronde7.Finale;

import java.util.List;

/** {@code /finale}: ronde 7, één tegen één, met de kroning aan het eind. */
final class FinaleCommand {
	private FinaleCommand() {
	}

	static LiteralArgumentBuilder<CommandSourceStack> maak() {
		LiteralArgumentBuilder<CommandSourceStack> cmd = BcCommand.op("finale");
		RondeCommands.startStop(cmd, Ronde.FINALE);
		cmd.then(Commands.literal("go").executes(FinaleCommand::go));
		cmd.then(Commands.literal("combatlog").executes(FinaleCommand::combatlog));
		cmd.then(Commands.literal("crash").executes(FinaleCommand::crash));
		cmd.then(Commands.literal("plek").then(Commands.argument("nummer", IntegerArgumentType.integer(1, 2))
				.executes(FinaleCommand::plek)));
		cmd.then(Commands.literal("spelers")
				.executes(FinaleCommand::toonSpelers)
				.then(Commands.argument("speler1", StringArgumentType.word()).suggests(BcCommand.ONLINE)
						.then(Commands.argument("speler2", StringArgumentType.word()).suggests(BcCommand.ONLINE)
								.executes(FinaleCommand::zetSpelers))));
		cmd.then(Commands.literal("krimp").then(Commands.argument("grootte", IntegerArgumentType.integer(1, 1000))
				.executes(ctx -> krimp(ctx, Regels.KRIMP_SECONDEN))
				.then(Commands.argument("seconden", IntegerArgumentType.integer(1, 3600))
						.executes(ctx -> krimp(ctx, IntegerArgumentType.getInteger(ctx, "seconden"))))));
		cmd.then(RondeCommands.tekst("wachttekst", "De wachttekst", () -> Spel.instellingen().finaleWachttekst(),
				t -> Spel.instellingen().zetFinaleWachttekst(t)));
		return cmd;
	}

	private static int go(CommandContext<CommandSourceStack> ctx) {
		Finale ronde = RondeCommands.lopend(Finale.class);
		if (ronde == null) {
			return BcCommand.fout(ctx, "De finale loopt niet (/finale start).");
		}
		String fout = ronde.go(ctx.getSource().getServer());
		if (fout != null) {
			return BcCommand.fout(ctx, "Kan niet: " + fout + ".");
		}
		return BcCommand.ok(ctx, "Tien seconden, dan zijn ze los.");
	}

	/** Een finalist logde uit en het was een combat log: de ander wint. */
	private static int combatlog(CommandContext<CommandSourceStack> ctx) {
		Finale ronde = RondeCommands.lopend(Finale.class);
		if (ronde == null) {
			return BcCommand.fout(ctx, "De finale loopt niet.");
		}
		String weg = ronde.wegNaam();
		String fout = ronde.combatlog(ctx.getSource().getServer());
		if (fout != null) {
			return BcCommand.fout(ctx, "Kan niet: " + fout + ".");
		}
		return BcCommand.ok(ctx, "Combat log van " + weg + ": de ander wint, de kroning begint.");
	}

	/** Een finalist logde uit door een crash: de finale stopt, daarna opnieuw starten. */
	private static int crash(CommandContext<CommandSourceStack> ctx) {
		Finale ronde = RondeCommands.lopend(Finale.class);
		if (ronde == null) {
			return BcCommand.fout(ctx, "De finale loopt niet.");
		}
		String weg = ronde.wegNaam();
		String fout = ronde.crash(ctx.getSource().getServer());
		if (fout != null) {
			return BcCommand.fout(ctx, "Kan niet: " + fout + ".");
		}
		return BcCommand.ok(ctx, "De finale is gestopt. Is " + weg + " terug, dan /finale start en /finale go.");
	}

	/** Plek 1 is voor de winnaar van King of the Hill, plek 2 voor de winnaar van de FFA. */
	private static int plek(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer speler = ctx.getSource().getPlayerOrException();
		int nummer = IntegerArgumentType.getInteger(ctx, "nummer");
		Punt p = SetupCommands.zetPunt(speler, "finale_" + nummer);
		String wie = nummer == 1 ? "de winnaar van King of the Hill" : "de winnaar van de FFA";
		return BcCommand.bewaard(ctx, "Startplek " + nummer + " van de finale (finale_" + nummer + ", " + wie + ") gezet op "
				+ SetupCommands.beschrijfPunt(p));
	}

	private static int toonSpelers(CommandContext<CommandSourceStack> ctx) {
		BootcampConfig c = ConfigStore.get();
		StringBuilder sb = new StringBuilder("Uitslag: King of the Hill ").append(ofNiemand(c.winnaarKing()))
				.append(", FFA ").append(ofNiemand(c.winnaarFfa()));
		if (c.tweedeFfa() != null) {
			sb.append(" (nummer twee ").append(c.tweedeFfa()).append(")");
		}
		List<String> namen = Finale.namen();
		sb.append(namen.isEmpty() ? "\nNog geen finale: zet de spelers met /finale spelers <speler1> <speler2>."
				: "\nFinale: " + namen.get(0) + " (finale_1) tegen " + namen.get(1) + " (finale_2).");
		return BcCommand.info(ctx, sb.toString());
	}

	/** Noodknop, en om te testen: de twee finalisten met de hand, in plaats van de uitslag. */
	private static int zetSpelers(CommandContext<CommandSourceStack> ctx) {
		String een = StringArgumentType.getString(ctx, "speler1");
		String twee = StringArgumentType.getString(ctx, "speler2");
		if (een.equalsIgnoreCase(twee)) {
			return BcCommand.fout(ctx, "Kies twee verschillende spelers.");
		}
		ConfigStore.get().zetWinnaarKing(een);
		ConfigStore.get().zetUitslagFfa(twee, null);
		return BcCommand.bewaard(ctx, "Finale: " + een + " (finale_1) tegen " + twee + " (finale_2).");
	}

	private static int krimp(CommandContext<CommandSourceStack> ctx, int seconden) {
		Finale ronde = RondeCommands.lopend(Finale.class);
		if (ronde == null) {
			return BcCommand.fout(ctx, "De finale loopt niet.");
		}
		int grootte = IntegerArgumentType.getInteger(ctx, "grootte");
		ronde.krimp(ctx.getSource().getServer(), grootte, seconden);
		return BcCommand.ok(ctx, "De border krimpt naar " + grootte + " in " + seconden + " seconden.");
	}

	private static String ofNiemand(String naam) {
		return naam == null ? "nog niemand" : naam;
	}
}
