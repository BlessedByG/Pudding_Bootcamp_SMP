package nl.pudding.bootcamp.game;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import nl.pudding.bootcamp.Mc;
import nl.pudding.bootcamp.core.Punt;
import nl.pudding.bootcamp.core.Regels;
import nl.pudding.bootcamp.core.Regio;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/** Wat ronde 5 en 6 in de Arena delen: de startplekken, de border, krimpen, de af-melding en de kill in beeld. */
public final class Arena {
	private Arena() {
	}

	/**
	 * De border van ronde 5 en 6: altijd om het midden van de vloer, zodat {@code /clown krimp} en
	 * {@code /ffa krimp} naar het midden krimpen. Is er een {@code colosseum}, dan zo groot dat de hele
	 * Arena erin past; anders zo groot als de vloer.
	 */
	public static void zetBorder(MinecraftServer server) {
		Regio vloer = Spel.regio("vloer");
		Regio c = Spel.regio("colosseum");
		double x = vloer.centerX();
		double z = vloer.centerZ();
		double grootte = vloer.grootte();
		if (c != null) {
			Regio.Doos d = c.omhullende();
			double half = Math.max(Math.max(Math.abs(d.min().x() - x), Math.abs(d.max().x() + 1 - x)),
					Math.max(Math.abs(d.min().z() - z), Math.abs(d.max().z() + 1 - z)));
			grootte = Math.max(grootte, 2 * half);
		}
		Border.zet(server, x, z, grootte);
	}

	/**
	 * Willekeurig over {@code jager_1..n}, één per plek (zijn er meer spelers dan plekken, dan
	 * delen ze om en om), met de kijkrichting van de plek.
	 */
	public static void verdeel(List<ServerPlayer> spelers) {
		List<String> plekken = Spel.reeks("jager_");
		if (plekken.isEmpty() || spelers.isEmpty()) {
			return;
		}
		int[] plek = Regels.verdeelWillekeurig(spelers.size(), plekken.size(), Spel.RANDOM);
		for (int i = 0; i < spelers.size(); i++) {
			Spel.naarPunt(spelers.get(i), plekken.get(plek[i] - 1));
		}
	}

	/**
	 * Voor {@code magStarten}: de startplekken binnen de border, de tribuneplekken buiten de vloer
	 * (anders zet de mod kijkers elke halve seconde terug).
	 */
	public static String controleerPlekken() {
		List<String> punten = new ArrayList<>(Spel.reeks("jager_"));
		punten.add("troon");
		String regio = Spel.regio("colosseum") != null ? "colosseum" : "vloer";
		String fout = Spel.buitenRegio(regio, punten);
		if (fout != null) {
			return fout;
		}
		Regio vloer = Spel.regio("vloer");
		List<String> opDeVloer = new ArrayList<>();
		for (String t : Spel.reeks("tribune_")) {
			Punt p = Spel.punt(t);
			if (vloer.bevatSpeler(p.x(), p.y(), p.z())) {
				opDeVloer.add(t);
			}
		}
		return opDeVloer.isEmpty() ? null
				: "deze tribuneplekken liggen op de vloer, waar kijkers juist af moeten blijven: " + String.join(", ", opDeVloer);
	}

	/** {@code /clown krimp} en {@code /ffa krimp}: de border krimpt, iedereen ziet het. */
	public static void krimp(MinecraftServer server, int grootte, int seconden) {
		Border.krimp(server, grootte, seconden);
		Mc.titleAllen(server, Mc.tekst("DE BORDER KRIMPT", ChatFormatting.RED, ChatFormatting.BOLD),
				Mc.tekst("naar " + grootte + " in " + seconden + " seconden", ChatFormatting.WHITE), 5, 50, 15);
		Mc.geluidAllen(server, SoundEvents.RAID_HORN, 1f, 1f);
	}

	/** {@code Speler3 is af door ClownPierce · 11 over}, met de namen in hun kleur. */
	public static void afMelding(MinecraftServer server, String dode, ChatFormatting dodeKleur,
			String killer, ChatFormatting killerKleur, int over) {
		MutableComponent regel = Component.empty().append(Mc.tekst(dode, dodeKleur)).append(Mc.tekst(" is af", ChatFormatting.GRAY));
		if (killer != null) {
			regel.append(Mc.tekst(" door ", ChatFormatting.GRAY)).append(Mc.tekst(killer, killerKleur));
		}
		regel.append(Mc.tekst(" · " + over + " over", ChatFormatting.GRAY));
		Mc.chatAllen(server, regel);
	}

	/**
	 * Een kill groot in beeld: kop en naam van de killer in goud, eronder {@code pakt Speler3 · 11 over},
	 * met de brul van een ravager. Alleen voor wie in {@code voor} zit.
	 */
	public static void killInBeeld(Collection<ServerPlayer> voor, ServerPlayer killer, ServerPlayer dode,
			ChatFormatting dodeKleur, int over, float volume) {
		MutableComponent titel = Component.empty().append(Mc.kop(killer)).append(Component.literal(" "))
				.append(Mc.tekst(Mc.naam(killer).toUpperCase(Locale.ROOT), ChatFormatting.GOLD, ChatFormatting.BOLD));
		MutableComponent onder = Component.empty().append(Mc.tekst("pakt ", ChatFormatting.WHITE))
				.append(Mc.tekst(Mc.naam(dode), dodeKleur))
				.append(Mc.tekst(" · " + over + " over", ChatFormatting.GRAY));
		for (ServerPlayer s : voor) {
			Mc.title(s, titel, onder, 5, 40, 15);
			Mc.geluid(s, SoundEvents.RAVAGER_ROAR, volume, 1f);
		}
	}
}
