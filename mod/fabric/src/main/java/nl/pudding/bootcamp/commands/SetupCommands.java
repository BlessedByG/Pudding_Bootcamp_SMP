package nl.pudding.bootcamp.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import nl.pudding.bootcamp.Mc;
import nl.pudding.bootcamp.config.ConfigStore;
import nl.pudding.bootcamp.core.BlokPos;
import nl.pudding.bootcamp.core.BootcampConfig;
import nl.pudding.bootcamp.core.Punt;
import nl.pudding.bootcamp.core.Regio;
import nl.pudding.bootcamp.setup.Wand;
import nl.pudding.bootcamp.visuals.Labels;

import java.util.Locale;
import java.util.regex.Pattern;

/** {@code /bc wand}, {@code /bc region ...} en {@code /bc point ...}: eén keer zetten na het bouwen. */
final class SetupCommands {
	private static final Pattern NAAM = Pattern.compile("[a-z0-9_]{1,32}");
	private static final double KIJKAFSTAND = 32.0;

	private static final SuggestionProvider<CommandSourceStack> REGIOS =
			(ctx, b) -> SharedSuggestionProvider.suggest(ConfigStore.get().regios().keySet(), b);
	private static final SuggestionProvider<CommandSourceStack> PUNTEN =
			(ctx, b) -> SharedSuggestionProvider.suggest(ConfigStore.get().punten().keySet(), b);

	private SetupCommands() {
	}

	static void voegToe(LiteralArgumentBuilder<CommandSourceStack> bc) {
		bc.then(Commands.literal("wand").executes(SetupCommands::wand));

		bc.then(Commands.literal("region")
				.then(Commands.literal("save").then(Commands.argument("naam", StringArgumentType.word()).suggests(REGIOS)
						.executes(SetupCommands::regionSave)))
				.then(Commands.literal("add").then(Commands.argument("naam", StringArgumentType.word()).suggests(REGIOS)
						.executes(SetupCommands::regionAdd)))
				.then(Commands.literal("show").then(Commands.argument("naam", StringArgumentType.word()).suggests(REGIOS)
						.executes(SetupCommands::regionShow)))
				.then(Commands.literal("list").executes(SetupCommands::regionList))
				.then(Commands.literal("del").then(Commands.argument("naam", StringArgumentType.word()).suggests(REGIOS)
						.executes(SetupCommands::regionDel))));

		bc.then(Commands.literal("label")
				.then(Commands.literal("weg").executes(SetupCommands::labelWeg))
				.then(Commands.literal("zet").then(Commands.argument("tekst", StringArgumentType.greedyString())
						.executes(SetupCommands::labelZet))));

		bc.then(Commands.literal("point")
				.then(Commands.literal("set").then(Commands.argument("naam", StringArgumentType.word()).suggests(PUNTEN)
						.executes(SetupCommands::pointSet)))
				.then(Commands.literal("block").then(Commands.argument("naam", StringArgumentType.word()).suggests(PUNTEN)
						.executes(SetupCommands::pointBlock)))
				.then(Commands.literal("tp").then(Commands.argument("naam", StringArgumentType.word()).suggests(PUNTEN)
						.executes(SetupCommands::pointTp)))
				.then(Commands.literal("list").executes(SetupCommands::pointList))
				.then(Commands.literal("del").then(Commands.argument("naam", StringArgumentType.word()).suggests(PUNTEN)
						.executes(SetupCommands::pointDel))));
	}

	private static String naam(CommandContext<CommandSourceStack> ctx) {
		return StringArgumentType.getString(ctx, "naam").toLowerCase(Locale.ROOT);
	}

	private static boolean geldig(String naam) {
		return NAAM.matcher(naam).matches();
	}

	private static int bewaard(CommandContext<CommandSourceStack> ctx, String tekst) {
		return BcCommand.bewaard(ctx, tekst);
	}

	// wand

	private static int wand(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer speler = ctx.getSource().getPlayerOrException();
		Wand.geef(speler);
		return BcCommand.info(ctx, "Wand: linksklik op een blok is hoek 1, rechtsklik hoek 2. Daarna /bc region save <naam>.");
	}

	// region

	private static int regionSave(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer speler = ctx.getSource().getPlayerOrException();
		String naam = naam(ctx);
		if (!geldig(naam)) {
			return BcCommand.fout(ctx, "Een naam is kleine letters, cijfers en _ (max 32).");
		}
		Regio regio = Wand.selectie(speler);
		if (regio == null) {
			return BcCommand.fout(ctx, "Selecteer eerst twee hoeken met de wand (/bc wand).");
		}
		boolean bestond = ConfigStore.get().regios().put(naam, regio) != null;
		return bewaard(ctx, "Regio " + naam + (bestond ? " overschreven: " : " opgeslagen: ") + beschrijf(regio));
	}

	private static int regionAdd(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer speler = ctx.getSource().getPlayerOrException();
		String naam = naam(ctx);
		if (!geldig(naam)) {
			return BcCommand.fout(ctx, "Een naam is kleine letters, cijfers en _ (max 32).");
		}
		Regio deel = Wand.selectie(speler);
		if (deel == null) {
			return BcCommand.fout(ctx, "Selecteer eerst twee hoeken met de wand (/bc wand).");
		}
		Regio oud = ConfigStore.get().regios().get(naam);
		if (oud == null) {
			ConfigStore.get().regios().put(naam, deel);
			return bewaard(ctx, "Regio " + naam + " bestond nog niet en is gemaakt met dit deel: " + beschrijf(deel));
		}
		if (oud.isCilinder()) {
			return BcCommand.fout(ctx, "Regio " + naam + " is een cirkel; die krijgt geen extra delen.");
		}
		Regio nieuw = oud.metDeel(deel.min(), deel.max());
		ConfigStore.get().regios().put(naam, nieuw);
		return bewaard(ctx, "Deel " + nieuw.aantalDelen() + " toegevoegd aan regio " + naam + ": " + beschrijf(deel));
	}

	private static int regionShow(CommandContext<CommandSourceStack> ctx) {
		String naam = naam(ctx);
		Regio regio = ConfigStore.get().regios().get(naam);
		if (regio == null) {
			return BcCommand.fout(ctx, "Regio " + naam + " bestaat niet.");
		}
		Wand.toon(regio);
		return BcCommand.info(ctx, "Regio " + naam + " tien seconden zichtbaar: " + beschrijf(regio));
	}

	private static int regionList(CommandContext<CommandSourceStack> ctx) {
		BootcampConfig c = ConfigStore.get();
		if (c.regios().isEmpty()) {
			return BcCommand.info(ctx, "Nog geen regio's.");
		}
		StringBuilder sb = new StringBuilder("Regio's (" + c.regios().size() + "):");
		c.regios().forEach((n, r) -> sb.append("\n  ").append(n).append(": ").append(beschrijf(r)));
		return BcCommand.info(ctx, sb.toString());
	}

	private static int regionDel(CommandContext<CommandSourceStack> ctx) {
		String naam = naam(ctx);
		if (ConfigStore.get().regios().remove(naam) == null) {
			return BcCommand.fout(ctx, "Regio " + naam + " bestaat niet.");
		}
		return bewaard(ctx, "Regio " + naam + " verwijderd.");
	}

	private static String beschrijf(Regio r) {
		if (r.isCilinder()) {
			Regio.Cilinder c = r.cilinder();
			return "cirkel om " + rond(c.x()) + " " + c.y() + " " + rond(c.z()) + ", doorsnede " + c.diameter() + ", " + c.hoogte() + " hoog";
		}
		String delen = r.aantalDelen() > 1 ? r.aantalDelen() + " delen, samen " : "";
		return delen + r.min().x() + " " + r.min().y() + " " + r.min().z() + " t/m " + r.max().x() + " " + r.max().y() + " " + r.max().z()
				+ " (" + r.breedteX() + " x " + r.hoogte() + " x " + r.breedteZ() + ", border " + r.grootte() + ")";
	}

	// label

	private static int labelZet(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer speler = ctx.getSource().getPlayerOrException();
		String tekst = StringArgumentType.getString(ctx, "tekst").strip();
		if (tekst.isEmpty()) {
			return BcCommand.fout(ctx, "Geef de tekst van het label.");
		}
		Labels.plaats(speler, tekst);
		return BcCommand.ok(ctx, "Label '" + tekst + "' geplaatst boven je hoofd.");
	}

	private static int labelWeg(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer speler = ctx.getSource().getPlayerOrException();
		if (!Labels.verwijderDichtstbij(speler)) {
			return BcCommand.fout(ctx, "Er staat geen label binnen zes blokken.");
		}
		return BcCommand.ok(ctx, "Label verwijderd.");
	}

	// point

	private static int pointSet(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer speler = ctx.getSource().getPlayerOrException();
		String naam = naam(ctx);
		if (!geldig(naam)) {
			return BcCommand.fout(ctx, "Een naam is kleine letters, cijfers en _ (max 32).");
		}
		return bewaard(ctx, "Punt " + naam + " gezet op " + beschrijf(zetPunt(speler, naam)));
	}

	/** Een punt op de plek van de speler, met zijn kijkrichting. Ook voor de commando's per ronde. */
	static Punt zetPunt(ServerPlayer speler, String naam) {
		Punt punt = Punt.positie(rond(speler.getX()), rond(speler.getY()), rond(speler.getZ()),
				rond(speler.getYRot()), rond(speler.getXRot()));
		ConfigStore.get().punten().put(naam, punt);
		return punt;
	}

	/** Een punt op het blok waar de speler naar kijkt (tot 32 blokken), of {@code null}. */
	static Punt zetBlokPunt(ServerPlayer speler, String naam) {
		HitResult hit = speler.pick(KIJKAFSTAND, 0f, false);
		if (hit.getType() != HitResult.Type.BLOCK) {
			return null;
		}
		BlockPos pos = ((BlockHitResult) hit).getBlockPos();
		Punt punt = Punt.blok(new BlokPos(pos.getX(), pos.getY(), pos.getZ()));
		ConfigStore.get().punten().put(naam, punt);
		return punt;
	}

	static String beschrijfPunt(Punt p) {
		return beschrijf(p);
	}

	private static int pointBlock(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer speler = ctx.getSource().getPlayerOrException();
		String naam = naam(ctx);
		if (!geldig(naam)) {
			return BcCommand.fout(ctx, "Een naam is kleine letters, cijfers en _ (max 32).");
		}
		Punt punt = zetBlokPunt(speler, naam);
		if (punt == null) {
			return BcCommand.fout(ctx, "Je kijkt niet naar een blok (binnen " + (int) KIJKAFSTAND + " blokken).");
		}
		return bewaard(ctx, "Punt " + naam + " gezet op blok " + beschrijf(punt));
	}

	private static int pointTp(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer speler = ctx.getSource().getPlayerOrException();
		String naam = naam(ctx);
		Punt punt = ConfigStore.get().punten().get(naam);
		if (punt == null) {
			return BcCommand.fout(ctx, "Punt " + naam + " bestaat niet.");
		}
		Mc.teleport(speler, punt);
		return BcCommand.info(ctx, "Naar " + naam + ".");
	}

	private static int pointList(CommandContext<CommandSourceStack> ctx) {
		BootcampConfig c = ConfigStore.get();
		if (c.punten().isEmpty()) {
			return BcCommand.info(ctx, "Nog geen punten.");
		}
		StringBuilder sb = new StringBuilder("Punten (" + c.punten().size() + "):");
		c.punten().forEach((n, p) -> sb.append("\n  ").append(n).append(": ").append(beschrijf(p)));
		return BcCommand.info(ctx, sb.toString());
	}

	private static int pointDel(CommandContext<CommandSourceStack> ctx) {
		String naam = naam(ctx);
		if (ConfigStore.get().punten().remove(naam) == null) {
			return BcCommand.fout(ctx, "Punt " + naam + " bestaat niet.");
		}
		return bewaard(ctx, "Punt " + naam + " verwijderd.");
	}

	private static String beschrijf(Punt p) {
		if (p.blok()) {
			BlokPos b = p.blokPos();
			return b.x() + " " + b.y() + " " + b.z() + " (blok)";
		}
		return p.x() + " " + p.y() + " " + p.z() + " (kijkrichting " + p.yaw() + " / " + p.pitch() + ")";
	}

	/** Twee decimalen is genoeg en houdt bootcamp.json leesbaar. */
	private static double rond(double v) {
		return Math.round(v * 100.0) / 100.0;
	}

	private static float rond(float v) {
		return Math.round(v * 10f) / 10f;
	}
}
