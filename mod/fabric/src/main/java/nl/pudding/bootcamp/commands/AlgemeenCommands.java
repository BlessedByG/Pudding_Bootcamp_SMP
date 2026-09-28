package nl.pudding.bootcamp.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import nl.pudding.bootcamp.Mc;
import nl.pudding.bootcamp.config.ConfigStore;
import nl.pudding.bootcamp.config.Standaardbestanden;
import nl.pudding.bootcamp.core.BootcampConfig;
import nl.pudding.bootcamp.core.Kleur;
import nl.pudding.bootcamp.core.Rol;
import nl.pudding.bootcamp.game.Reset;
import nl.pudding.bootcamp.game.Spel;
import nl.pudding.bootcamp.kits.Kits;
import nl.pudding.bootcamp.schrik.Schrik;
import nl.pudding.bootcamp.teams.Teams;
import nl.pudding.bootcamp.tribune.Tribune;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/** {@code /bc status|kit|team|schrik|kijker|reset}: wat voor de hele avond geldt, plus de noodknoppen. */
final class AlgemeenCommands {
	private AlgemeenCommands() {
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

	private static final SuggestionProvider<CommandSourceStack> TEAMS = (ctx, b) -> {
		List<String> opties = new ArrayList<>();
		for (Kleur k : Kleur.values()) {
			opties.add(k.id());
		}
		opties.add("weg");
		return SharedSuggestionProvider.suggest(opties, b);
	};

	static void voegToe(LiteralArgumentBuilder<CommandSourceStack> bc) {
		bc.then(Commands.literal("status").executes(AlgemeenCommands::status));
		bc.then(Commands.literal("kit").then(Commands.argument("naam", StringArgumentType.word()).suggests(KITS)
				.executes(ctx -> kit(ctx, Mc.deelnemers(ctx.getSource().getServer()).stream()
						.filter(s -> Spel.rol(s) != Rol.KIJKER).toList()))
				.then(Commands.argument("speler", EntityArgument.player())
						.executes(ctx -> kit(ctx, List.of(EntityArgument.getPlayer(ctx, "speler")))))));
		bc.then(Commands.literal("team").then(Commands.argument("speler", StringArgumentType.word()).suggests(BcCommand.ONLINE)
				.then(Commands.argument("kleur", StringArgumentType.word()).suggests(TEAMS).executes(AlgemeenCommands::team))));
		bc.then(Commands.literal("schrik").then(Commands.argument("speler", EntityArgument.player())
				.executes(AlgemeenCommands::schrik)));
		bc.then(Commands.literal("kijker").then(Commands.argument("speler", EntityArgument.player())
				.then(Commands.literal("aan").executes(ctx -> kijker(ctx, true)))
				.then(Commands.literal("uit").executes(ctx -> kijker(ctx, false)))));
		bc.then(Commands.literal("reset").executes(AlgemeenCommands::reset));
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

	/** Noodknop: iemand in een team zetten of de keuze weghalen. Mag boven het maximum. */
	private static int team(CommandContext<CommandSourceStack> ctx) {
		MinecraftServer server = ctx.getSource().getServer();
		String naam = StringArgumentType.getString(ctx, "speler");
		String kleurNaam = StringArgumentType.getString(ctx, "kleur").toLowerCase(Locale.ROOT);
		Kleur kleur = Kleur.vanId(kleurNaam);
		if (kleur == null && !kleurNaam.equals("weg")) {
			return BcCommand.fout(ctx, "Kies rood, blauw, groen, geel of weg.");
		}
		ServerPlayer speler = server.getPlayerList().getPlayerByName(naam);
		if (speler != null) {
			Teams.kies(server, speler, kleur);
			// Is hij nu gewoon speler, dan meteen de goede kleur (een jager of kijker houdt zijn team).
			if (Spel.rol(speler) == Rol.SPELER) {
				Spel.zetRol(server, speler, Rol.SPELER);
			}
		} else {
			Teams.kiesOpNaam(naam, kleur);
		}
		String wat = kleur == null ? "heeft geen team meer" : "zit in " + kleur.naam() + " (" + Teams.aantal(kleur) + ")";
		return BcCommand.ok(ctx, naam + " " + wat + ".");
	}

	private static int schrik(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer speler = EntityArgument.getPlayer(ctx, "speler");
		Schrik.op(speler);
		return BcCommand.info(ctx, "Jumpscare naar " + Mc.naam(speler) + ".");
	}

	private static int kijker(CommandContext<CommandSourceStack> ctx, boolean aan) throws CommandSyntaxException {
		ServerPlayer speler = EntityArgument.getPlayer(ctx, "speler");
		String fout = aan ? Tribune.handmatigAan(ctx.getSource().getServer(), speler)
				: Tribune.handmatigUit(ctx.getSource().getServer(), speler);
		if (fout != null) {
			return BcCommand.fout(ctx, "Kijker: " + fout + ".");
		}
		return BcCommand.ok(ctx, Mc.naam(speler) + (aan ? " is nu kijker op de tribune." : " is geen kijker meer en doet weer mee."));
	}

	private static int status(CommandContext<CommandSourceStack> ctx) {
		BootcampConfig c = ConfigStore.get();
		StringBuilder sb = new StringBuilder(Spel.statusTekst(ctx.getSource().getServer()));
		sb.append("\n  config: ").append(c.regios().size()).append(" regio's, ").append(c.punten().size()).append(" punten");
		sb.append("\n  teams:");
		for (Kleur k : Kleur.values()) {
			sb.append(" ").append(k.naam()).append(" ").append(Teams.aantal(k));
		}
		sb.append(" (maximum ").append(Teams.maximum(ctx.getSource().getServer())).append(")");
		sb.append("\n  uitverkoren: ").append(c.uitverkoren() == null ? "niemand" : c.uitverkoren());
		sb.append("\n  presentator: ").append(c.presentator() == null ? "niemand" : c.presentator());
		return BcCommand.info(ctx, sb.toString());
	}

	private static int reset(CommandContext<CommandSourceStack> ctx) {
		List<String> mislukt = Reset.draai(ctx.getSource().getServer());
		if (!mislukt.isEmpty()) {
			return BcCommand.fout(ctx, "Reset gedaan, maar deze stappen faalden (zie console): " + String.join(", ", mislukt));
		}
		return BcCommand.ok(ctx, "Reset: alles terug naar de basiskamp-staat. Teamkeuzes zijn weg, het Ei staat er weer, de instellingen blijven.");
	}
}
