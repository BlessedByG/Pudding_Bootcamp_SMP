package nl.pudding.bootcamp.visuals;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.numbers.BlankFormat;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import nl.pudding.bootcamp.Mc;
import nl.pudding.bootcamp.game.Reset;

import java.util.List;

/**
 * De sidebar: in ronde 2 de sneuvelvolgorde, in ronde 4 de regeerperiodes. Elke regel is een
 * vaste, onzichtbare score-houder met de tekst als weergavenaam, zodat twee gelijke regels
 * ("Clown 0:01" twee keer) elkaar niet overschrijven. De cijfers rechts staan uit.
 */
public final class Sidebar {
	private static final String OBJECTIVE = "bootcamp";
	/** De sidebar van de client laat er maar vijftien zien. */
	private static final int MAX_REGELS = 15;

	private static int getoond;

	private Sidebar() {
	}

	public static void init() {
		Reset.REGISTER.registreer("sidebar", Sidebar::weg);
	}

	/** Toont de laatste vijftien regels, de eerste bovenaan. */
	public static void toon(MinecraftServer server, String titel, List<String> regels) {
		ServerScoreboard bord = server.getScoreboard();
		Objective obj = bord.getObjective(OBJECTIVE);
		if (obj == null) {
			obj = bord.addObjective(OBJECTIVE, ObjectiveCriteria.DUMMY, Mc.tekst(titel, ChatFormatting.GOLD, ChatFormatting.BOLD),
					ObjectiveCriteria.RenderType.INTEGER, false, BlankFormat.INSTANCE);
		} else {
			obj.setDisplayName(Mc.tekst(titel, ChatFormatting.GOLD, ChatFormatting.BOLD));
		}

		List<String> zichtbaar = regels.size() > MAX_REGELS ? regels.subList(regels.size() - MAX_REGELS, regels.size()) : regels;
		for (int i = 0; i < zichtbaar.size(); i++) {
			ScoreAccess score = bord.getOrCreatePlayerScore(houder(i), obj);
			// Hoogste score staat bovenaan.
			score.set(zichtbaar.size() - i);
			score.display(Mc.tekst(zichtbaar.get(i)));
		}
		for (int i = zichtbaar.size(); i < getoond; i++) {
			bord.resetSinglePlayerScore(houder(i), obj);
		}
		getoond = zichtbaar.size();
		bord.setDisplayObjective(DisplaySlot.SIDEBAR, obj);
	}

	public static void weg(MinecraftServer server) {
		ServerScoreboard bord = server.getScoreboard();
		Objective obj = bord.getObjective(OBJECTIVE);
		if (obj != null) {
			bord.removeObjective(obj);
		}
		getoond = 0;
	}

	private static ScoreHolder houder(int regel) {
		return ScoreHolder.forNameOnly("bc_regel_" + regel);
	}
}
