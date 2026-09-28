package nl.pudding.bootcamp.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import nl.pudding.bootcamp.core.Instellingen;
import nl.pudding.bootcamp.core.Ronde;
import nl.pudding.bootcamp.core.Tijd;
import nl.pudding.bootcamp.game.RondeLogica;
import nl.pudding.bootcamp.game.Spel;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/** Wat elke ronde hetzelfde heeft: {@code start}, {@code stop}, en bij een timer {@code resterend}. */
final class RondeCommands {
	private RondeCommands() {
	}

	/** Voegt {@code start} en {@code stop} toe. */
	static void startStop(LiteralArgumentBuilder<CommandSourceStack> cmd, Ronde ronde) {
		cmd.then(Commands.literal("start").executes(ctx -> start(ctx, ronde)));
		cmd.then(Commands.literal("stop").executes(ctx -> stop(ctx, ronde)));
	}

	static int start(CommandContext<CommandSourceStack> ctx, Ronde ronde) {
		String fout = Spel.start(ctx.getSource().getServer(), ronde);
		if (fout != null) {
			return BcCommand.fout(ctx, ronde.naam() + " start niet, " + fout);
		}
		return BcCommand.ok(ctx, ronde.naam() + " gestart.");
	}

	static int start(CommandContext<CommandSourceStack> ctx, RondeLogica logica) {
		String fout = Spel.start(ctx.getSource().getServer(), logica);
		if (fout != null) {
			return BcCommand.fout(ctx, logica.ronde().naam() + " start niet, " + fout);
		}
		return BcCommand.ok(ctx, logica.ronde().naam() + " gestart.");
	}

	/** Breekt deze ronde af; loopt er een andere, dan weigert hij met één regel. */
	static int stop(CommandContext<CommandSourceStack> ctx, Ronde ronde) {
		String fout = magStoppen(ronde);
		if (fout != null) {
			return BcCommand.fout(ctx, fout);
		}
		Spel.stop(ctx.getSource().getServer());
		return BcCommand.ok(ctx, ronde.naam() + " afgebroken: timer stil, border weg, bevriezing eraf.");
	}

	static String magStoppen(Ronde ronde) {
		RondeLogica actief = Spel.actief();
		if (actief == null) {
			return "Er loopt geen ronde.";
		}
		if (actief.ronde() != ronde) {
			return "nu loopt: " + actief.ronde().naam();
		}
		return null;
	}

	/** De lopende ronde van dit type, of {@code null}. */
	@SuppressWarnings("unchecked")
	static <T extends RondeLogica> T lopend(Class<T> klasse) {
		RondeLogica r = Spel.actief();
		return klasse.isInstance(r) ? (T) r : null;
	}

	/** {@code resterend <seconden>}: alleen terwijl de ronde loopt, de klok op zoveel seconden. */
	static void resterend(LiteralArgumentBuilder<CommandSourceStack> cmd, Ronde ronde) {
		cmd.then(Commands.literal("resterend").then(Commands.argument("seconden", IntegerArgumentType.integer(0, 3600))
				.executes(ctx -> {
					if (Spel.actief() == null || Spel.actief().ronde() != ronde || !Spel.timerLoopt()) {
						return BcCommand.fout(ctx, "Dat kan alleen terwijl " + ronde.naam() + " loopt.");
					}
					int sec = IntegerArgumentType.getInteger(ctx, "seconden");
					Spel.zetTimer(sec);
					return BcCommand.ok(ctx, "Klok op " + Tijd.mmss(sec) + ".");
				})));
	}

	/**
	 * Een instelling in hele minuten: zonder getal de huidige waarde, met getal nieuw (met de grenzen
	 * uit {@code core}), bewaard in {@code bootcamp.json}.
	 *
	 * @param check  de grens: {@code null} als het mag, anders de regel waarom niet
	 * @param zet    zet de nieuwe waarde
	 * @param live   wat er met een lopende ronde gebeurt (of {@code null})
	 */
	static LiteralArgumentBuilder<CommandSourceStack> minuten(String naam, String wat, Supplier<Integer> huidig,
			Function<Integer, String> check, Consumer<Integer> zet, Consumer<MinecraftServer> live) {
		return Commands.literal(naam)
				.executes(ctx -> BcCommand.info(ctx, wat + ": " + huidig.get() + " minuten."))
				.then(Commands.argument("minuten", IntegerArgumentType.integer(0, 600)).executes(ctx -> {
					int min = IntegerArgumentType.getInteger(ctx, "minuten");
					String fout = check.apply(min);
					if (fout != null) {
						return BcCommand.fout(ctx, fout + ".");
					}
					zet.accept(min);
					if (live != null) {
						live.accept(ctx.getSource().getServer());
					}
					return BcCommand.bewaard(ctx, wat + ": " + min + " minuten.");
				}));
	}

	/**
	 * Een tekst-instelling: zonder tekst de huidige laten zien, {@code -} zet hem terug op de
	 * standaard (of wist hem).
	 */
	static LiteralArgumentBuilder<CommandSourceStack> tekst(String naam, String wat, Supplier<String> huidig, Consumer<String> zet) {
		return Commands.literal(naam)
				.executes(ctx -> {
					String t = huidig.get();
					return BcCommand.info(ctx, wat + ": " + (t == null ? "(geen)" : "'" + t + "'"));
				})
				.then(Commands.argument("tekst", StringArgumentType.greedyString()).executes(ctx -> {
					String tekst = StringArgumentType.getString(ctx, "tekst");
					String fout = Instellingen.checkTekst(tekst.strip());
					if (fout != null) {
						return BcCommand.fout(ctx, fout + ".");
					}
					try {
						zet.accept(tekst);
					} catch (IllegalArgumentException e) {
						return BcCommand.fout(ctx, e.getMessage() + ".");
					}
					String t = huidig.get();
					return BcCommand.bewaard(ctx, wat + ": " + (t == null ? "(geen)" : "'" + t + "'"));
				}));
	}
}
