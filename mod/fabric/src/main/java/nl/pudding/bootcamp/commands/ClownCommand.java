package nl.pudding.bootcamp.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import nl.pudding.bootcamp.Mc;
import nl.pudding.bootcamp.config.ConfigStore;
import nl.pudding.bootcamp.core.Punt;
import nl.pudding.bootcamp.core.Regels;
import nl.pudding.bootcamp.core.Regio;
import nl.pudding.bootcamp.core.Ronde;
import nl.pudding.bootcamp.game.Planner;
import nl.pudding.bootcamp.game.Spel;
import nl.pudding.bootcamp.game.ronde5.ClownVsAll;
import nl.pudding.bootcamp.rad.KroonRad;
import nl.pudding.bootcamp.setup.Wand;

/** {@code /clown}: ronde 5, Clown vs All, met het Rad. */
final class ClownCommand {
	/** De vloer is een cilinder van vijf hoog vanaf je voeten. */
	private static final int VLOER_HOOGTE = 5;

	private ClownCommand() {
	}

	static LiteralArgumentBuilder<CommandSourceStack> maak() {
		LiteralArgumentBuilder<CommandSourceStack> cmd = BcCommand.op("clown");
		cmd.then(Commands.literal("rad").executes(ClownCommand::rad));
		cmd.then(Commands.literal("start").executes(ctx -> RondeCommands.start(ctx, Ronde.CLOWN)));
		cmd.then(Commands.literal("stop").executes(ClownCommand::stop));
		cmd.then(Commands.literal("go").executes(ClownCommand::go));
		cmd.then(Commands.literal("uitverkoren")
				.executes(ctx -> {
					String naam = ConfigStore.get().uitverkoren();
					return BcCommand.info(ctx, naam == null ? "Er is nog geen uitverkorene." : "Uitverkoren: " + naam + ".");
				})
				.then(Commands.argument("speler", StringArgumentType.word()).suggests(BcCommand.ONLINE).executes(ctx -> {
					String naam = StringArgumentType.getString(ctx, "speler");
					ConfigStore.get().zetUitverkoren(naam);
					// Niet naar de andere ops en niet in de log: het rad blijft geheim.
					return BcCommand.bewaardStil(ctx, "Uitverkoren: " + naam + ".");
				})));
		cmd.then(Commands.literal("troon").executes(ctx -> {
			ServerPlayer speler = ctx.getSource().getPlayerOrException();
			Punt p = SetupCommands.zetPunt(speler, "troon");
			return BcCommand.bewaard(ctx, "Podium (troon) gezet op " + SetupCommands.beschrijfPunt(p));
		}));
		cmd.then(Commands.literal("jagerplek")
				.executes(ctx -> genummerd(ctx, "jager_", "jagerplek", 0))
				.then(Commands.argument("nummer", IntegerArgumentType.integer(1, 200))
						.executes(ctx -> genummerd(ctx, "jager_", "jagerplek", IntegerArgumentType.getInteger(ctx, "nummer")))));
		cmd.then(Commands.literal("tribune")
				.executes(ctx -> genummerd(ctx, "tribune_", "tribune", 0))
				.then(Commands.argument("nummer", IntegerArgumentType.integer(1, 200))
						.executes(ctx -> genummerd(ctx, "tribune_", "tribune", IntegerArgumentType.getInteger(ctx, "nummer")))));
		cmd.then(Commands.literal("vloer").then(Commands.argument("diameter", IntegerArgumentType.integer(3, 500))
				.executes(ClownCommand::vloer)));
		cmd.then(RondeCommands.tekst("wachttekst", "De wachttekst", () -> Spel.instellingen().clownWachttekst(),
				t -> Spel.instellingen().zetClownWachttekst(t)));
		cmd.then(Commands.literal("kroon").then(Commands.argument("speler", EntityArgument.player()).executes(ClownCommand::kroon)));
		cmd.then(Commands.literal("krimp").then(Commands.argument("grootte", IntegerArgumentType.integer(1, 1000))
				.executes(ctx -> krimp(ctx, Regels.KRIMP_SECONDEN))
				.then(Commands.argument("seconden", IntegerArgumentType.integer(1, 3600))
						.executes(ctx -> krimp(ctx, IntegerArgumentType.getInteger(ctx, "seconden"))))));
		return cmd;
	}

	private static int rad(CommandContext<CommandSourceStack> ctx) {
		String fout = KroonRad.start(ctx.getSource().getServer());
		if (fout != null) {
			return BcCommand.fout(ctx, "Het rad draait niet, " + fout + ".");
		}
		return BcCommand.info(ctx, "Het rad draait. Drie seconden na de landing staat iedereen bevroren op de vloer; dan /clown go.");
	}

	private static int stop(CommandContext<CommandSourceStack> ctx) {
		MinecraftServer server = ctx.getSource().getServer();
		if (!Spel.loopt() && (KroonRad.draait() || Planner.heeftWerk())) {
			// Het rad draait, of Clown vs All staat klaar om te starten.
			KroonRad.stop(server);
			Planner.wisAlles();
			return BcCommand.ok(ctx, "Het rad gestopt; Clown vs All start niet vanzelf.");
		}
		return RondeCommands.stop(ctx, Ronde.CLOWN);
	}

	private static int go(CommandContext<CommandSourceStack> ctx) {
		ClownVsAll ronde = RondeCommands.lopend(ClownVsAll.class);
		if (ronde == null) {
			return BcCommand.fout(ctx, "Clown vs All loopt niet (/clown rad of /clown start).");
		}
		String fout = ronde.go(ctx.getSource().getServer());
		if (fout != null) {
			return BcCommand.fout(ctx, "Kan niet: " + fout + ".");
		}
		return BcCommand.ok(ctx, "Tien seconden, dan is iedereen los.");
	}

	/** Een genummerde plek: zonder nummer het volgende vrije, met nummer overschrijf je die plek. */
	private static int genummerd(CommandContext<CommandSourceStack> ctx, String prefix, String wat, int nummer) throws CommandSyntaxException {
		ServerPlayer speler = ctx.getSource().getPlayerOrException();
		int n = nummer > 0 ? nummer : Ronde.volgendVrij(prefix, ConfigStore.get().punten().keySet());
		Punt p = SetupCommands.zetPunt(speler, prefix + n);
		return BcCommand.bewaard(ctx, wat + " " + n + " gezet (" + prefix + n + ") op " + SetupCommands.beschrijfPunt(p));
	}

	private static int vloer(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer speler = ctx.getSource().getPlayerOrException();
		int diameter = IntegerArgumentType.getInteger(ctx, "diameter");
		Regio vloer = Regio.cilinder(Math.round(speler.getX() * 100.0) / 100.0, Math.round(speler.getZ() * 100.0) / 100.0,
				(int) Math.floor(speler.getY()), diameter, VLOER_HOOGTE);
		ConfigStore.get().regios().put("vloer", vloer);
		// Meteen tien seconden de rand in particles, zodat je ziet of hij goed ligt.
		Wand.toon(vloer);
		return BcCommand.bewaard(ctx, "Vloer gezet: een cirkel van " + diameter + " om " + Math.round(speler.getX()) + " "
				+ (int) Math.floor(speler.getY()) + " " + Math.round(speler.getZ()) + ", " + VLOER_HOOGTE + " hoog.");
	}

	private static int kroon(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer speler = EntityArgument.getPlayer(ctx, "speler");
		ClownVsAll ronde = RondeCommands.lopend(ClownVsAll.class);
		if (ronde == null) {
			return BcCommand.fout(ctx, "De kroon forceren kan alleen terwijl Clown vs All loopt.");
		}
		String fout = ronde.forceer(ctx.getSource().getServer(), speler);
		if (fout != null) {
			return BcCommand.fout(ctx, "Kroon: " + fout + ".");
		}
		return BcCommand.ok(ctx, "Kroon naar " + Mc.naam(speler) + ": reset, jagers terug naar hun startplek.");
	}

	private static int krimp(CommandContext<CommandSourceStack> ctx, int seconden) {
		ClownVsAll ronde = RondeCommands.lopend(ClownVsAll.class);
		if (ronde == null) {
			return BcCommand.fout(ctx, "Clown vs All loopt niet.");
		}
		int grootte = IntegerArgumentType.getInteger(ctx, "grootte");
		ronde.krimp(ctx.getSource().getServer(), grootte, seconden);
		return BcCommand.ok(ctx, "De border krimpt naar " + grootte + " in " + seconden + " seconden.");
	}
}
