package nl.pudding.bootcamp.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;
import nl.pudding.bootcamp.core.Instellingen;
import nl.pudding.bootcamp.core.Kleur;
import nl.pudding.bootcamp.core.Punt;
import nl.pudding.bootcamp.core.Ronde;
import nl.pudding.bootcamp.game.Spel;
import nl.pudding.bootcamp.game.ronde3.MobArena;

import java.util.Map;

/** {@code /mobarena}: ronde 3. */
final class MobArenaCommand {
	private MobArenaCommand() {
	}

	static LiteralArgumentBuilder<CommandSourceStack> maak() {
		LiteralArgumentBuilder<CommandSourceStack> cmd = BcCommand.op("mobarena");
		RondeCommands.startStop(cmd, Ronde.MOBARENA);
		cmd.then(Commands.literal("volgende").executes(MobArenaCommand::volgende));
		cmd.then(Commands.literal("schema").executes(MobArenaCommand::schema));
		cmd.then(Commands.literal("wave").then(Commands.literal("volgende").executes(MobArenaCommand::waveVolgende)));
		cmd.then(Commands.literal("startplek").then(Commands.argument("arena", IntegerArgumentType.integer(1, 2))
				.then(Commands.argument("kleur", StringArgumentType.word()).suggests(BcCommand.KLEUREN)
						.executes(MobArenaCommand::startplek))));
		cmd.then(Commands.literal("punten")
				.executes(MobArenaCommand::puntenTabel)
				.then(Commands.argument("mob", StringArgumentType.word())
						.suggests((ctx, b) -> SharedSuggestionProvider.suggest(Instellingen.MOB_PUNTEN.keySet(), b))
						.then(Commands.argument("punten", IntegerArgumentType.integer(0, Instellingen.MAX_MOB_PUNTEN))
								.executes(MobArenaCommand::punten))));
		cmd.then(Commands.literal("veldhoogte")
				.executes(MobArenaCommand::toonVeldHoogte)
				.then(Commands.argument("blokken", IntegerArgumentType.integer(0, Instellingen.MAX_VELD_HOOGTE))
						.executes(MobArenaCommand::veldHoogte)));
		cmd.then(RondeCommands.tekst("aftekst", "De aftekst", () -> Spel.instellingen().aftekst(),
				t -> Spel.instellingen().zetAftekst(t)));
		return cmd;
	}

	private static int volgende(CommandContext<CommandSourceStack> ctx) {
		MobArena ronde = RondeCommands.lopend(MobArena.class);
		if (ronde == null) {
			return BcCommand.fout(ctx, "De mob arena loopt niet (/mobarena start).");
		}
		String fout = ronde.volgende(ctx.getSource().getServer());
		if (fout != null) {
			return BcCommand.fout(ctx, "Nog niet: " + fout + ".");
		}
		return BcCommand.ok(ctx, "Volgende beurt gestart.");
	}

	private static int schema(CommandContext<CommandSourceStack> ctx) {
		MobArena ronde = RondeCommands.lopend(MobArena.class);
		if (ronde == null) {
			return BcCommand.fout(ctx, "De mob arena loopt niet; het schema wordt geloot bij /mobarena start.");
		}
		// Alleen voor wie het typt: de spelers zien het schema niet.
		return BcCommand.info(ctx, ronde.schemaTekst());
	}

	private static int waveVolgende(CommandContext<CommandSourceStack> ctx) {
		MobArena ronde = RondeCommands.lopend(MobArena.class);
		if (ronde == null) {
			return BcCommand.fout(ctx, "De mob arena loopt niet.");
		}
		String fout = ronde.forceerWave(ctx.getSource().getServer());
		if (fout != null) {
			return BcCommand.fout(ctx, "Kan niet: " + fout + ".");
		}
		return BcCommand.ok(ctx, "De wave telt als klaar; de overgebleven mobs gaan weg.");
	}

	private static int startplek(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer speler = ctx.getSource().getPlayerOrException();
		int arena = IntegerArgumentType.getInteger(ctx, "arena");
		Kleur kleur = BcCommand.kleur(ctx, "kleur");
		if (kleur == null) {
			return BcCommand.fout(ctx, "Kies rood, blauw, groen of geel.");
		}
		String naam = "start_" + arena + "_" + kleur.id();
		Punt p = SetupCommands.zetPunt(speler, naam);
		return BcCommand.bewaard(ctx, "Startplek van " + kleur.naam() + " in arena " + arena + " (" + naam + ") gezet op "
				+ SetupCommands.beschrijfPunt(p));
	}

	private static int toonVeldHoogte(CommandContext<CommandSourceStack> ctx) {
		return BcCommand.info(ctx, "Een veld telt voor kijkers en wachtenden tot " + Spel.instellingen().veldHoogte()
				+ " blokken boven de selectie (standaard " + Instellingen.VELD_HOOGTE + ").");
	}

	private static int veldHoogte(CommandContext<CommandSourceStack> ctx) {
		int blokken = IntegerArgumentType.getInteger(ctx, "blokken");
		Spel.instellingen().zetVeldHoogte(blokken);
		String uitleg = blokken == 0
				? " Let op: nu telt alleen wie binnen de selectie zelf staat, niet wie op de vloer ervan staat."
				: "";
		return BcCommand.bewaard(ctx, "Een veld telt nu tot " + blokken + " blokken boven de selectie; geldt meteen." + uitleg);
	}

	private static int puntenTabel(CommandContext<CommandSourceStack> ctx) {
		StringBuilder sb = new StringBuilder("Punten per mob:");
		for (Map.Entry<String, Integer> e : Spel.instellingen().mobPunten().entrySet()) {
			sb.append("\n  ").append(e.getKey()).append(": ").append(e.getValue());
		}
		sb.append("\n  elk ander type: ").append(Instellingen.MOB_PUNTEN_ANDER);
		return BcCommand.info(ctx, sb.toString());
	}

	private static int punten(CommandContext<CommandSourceStack> ctx) {
		String mob = Instellingen.mobSleutel(StringArgumentType.getString(ctx, "mob"));
		int p = IntegerArgumentType.getInteger(ctx, "punten");
		Spel.instellingen().zetMobPunten(mob, p);
		return BcCommand.bewaard(ctx, mob + " is nu " + p + " punten waard.");
	}
}
