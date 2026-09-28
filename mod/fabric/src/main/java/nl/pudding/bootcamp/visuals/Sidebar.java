package nl.pudding.bootcamp.visuals;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
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

import java.util.ArrayList;
import java.util.List;

/**
 * De sidebar, rechts in beeld. Twee vormen:
 * <ul>
 * <li>{@link #toonTekst}: regels zonder cijfers (de teams in het doolhof, de regeerperiodes);</li>
 * <li>{@link #toonScores}: een naam met een getal rechts (de top 10 van het Ei, de teamstand, de
 * kills).</li>
 * </ul>
 * Elke regel is een vaste, onzichtbare score-houder ({@code 01}, {@code 02}, ...) met de tekst als
 * weergavenaam. De client sorteert gelijke scores op de naam van de houder, dus zo blijft de
 * volgorde van de mod staan, ook bij gelijke punten.
 */
public final class Sidebar {
	private static final String OBJECTIVE = "bootcamp";
	/** De sidebar van de client laat er maar vijftien zien. */
	private static final int MAX_REGELS = 15;

	public record Regel(Component naam, int score) {
	}

	private static int getoond;

	private Sidebar() {
	}

	public static void init() {
		Reset.REGISTER.registreer("sidebar", Sidebar::weg);
	}

	/** Regels zonder cijfers, de eerste bovenaan; bij meer dan vijftien de laatste vijftien. */
	public static void toonTekst(MinecraftServer server, String titel, List<? extends Component> regels) {
		List<? extends Component> zichtbaar = regels.size() > MAX_REGELS ? regels.subList(regels.size() - MAX_REGELS, regels.size()) : regels;
		List<Regel> r = new ArrayList<>();
		for (int i = 0; i < zichtbaar.size(); i++) {
			// Hoogste score staat bovenaan.
			r.add(new Regel(zichtbaar.get(i), zichtbaar.size() - i));
		}
		toon(server, titel, r, false);
	}

	public static void toonTekstRegels(MinecraftServer server, String titel, List<String> regels) {
		toonTekst(server, titel, regels.stream().map(Mc::tekst).toList());
	}

	/** Een naam met een getal rechts, in deze volgorde (de beste eerst). */
	public static void toonScores(MinecraftServer server, String titel, List<Regel> regels) {
		toon(server, titel, regels.size() > MAX_REGELS ? regels.subList(0, MAX_REGELS) : regels, true);
	}

	private static void toon(MinecraftServer server, String titel, List<Regel> regels, boolean cijfers) {
		ServerScoreboard bord = server.getScoreboard();
		Objective obj = bord.getObjective(OBJECTIVE);
		Component kop = Mc.tekst(titel, ChatFormatting.GOLD, ChatFormatting.BOLD);
		if (obj == null) {
			obj = bord.addObjective(OBJECTIVE, ObjectiveCriteria.DUMMY, kop, ObjectiveCriteria.RenderType.INTEGER, false,
					cijfers ? null : BlankFormat.INSTANCE);
		} else {
			obj.setDisplayName(kop);
			obj.setNumberFormat(cijfers ? null : BlankFormat.INSTANCE);
		}
		for (int i = 0; i < regels.size(); i++) {
			ScoreAccess score = bord.getOrCreatePlayerScore(houder(i), obj);
			score.set(regels.get(i).score());
			score.display(regels.get(i).naam());
		}
		for (int i = regels.size(); i < getoond; i++) {
			bord.resetSinglePlayerScore(houder(i), obj);
		}
		getoond = regels.size();
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
		return ScoreHolder.forNameOnly(String.format("%02d", regel + 1));
	}
}
