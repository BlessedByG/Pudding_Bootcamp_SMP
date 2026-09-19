package nl.pudding.bootcamp.game;

import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import nl.pudding.bootcamp.Mc;
import nl.pudding.bootcamp.core.Countdown;

/**
 * De countdown voor iedereen: een teller in de actionbar, de laatste vijf seconden als title met
 * een stijgende pling, dan een groene GO met de raid horn. Er loopt er hooguit één tegelijk.
 */
public final class Aftelling {
	private static final int TITLES_VANAF = 5;

	private static Countdown countdown;
	private static Runnable bijNul;
	private static String label;
	private static int startgetal;

	private Aftelling() {
	}

	/**
	 * @param label  wat er in de actionbar voor de teller staat, bijvoorbeeld "De jacht begint over"
	 * @param bijNul wat er gebeurt bij GO
	 */
	public static void start(int seconden, String label, Runnable bijNul) {
		Aftelling.countdown = new Countdown(seconden);
		// Het startgetal zelf hoort er ook bij: "5" met de laagste pling, niet pas "4".
		Aftelling.startgetal = seconden;
		Aftelling.label = label;
		Aftelling.bijNul = bijNul;
	}

	public static boolean loopt() {
		return countdown != null;
	}

	public static void stop() {
		countdown = null;
		bijNul = null;
		startgetal = 0;
	}

	/** Elke servertick. */
	public static void tick(MinecraftServer server) {
		if (countdown == null) {
			return;
		}
		int s = countdown.tick();
		if (startgetal > 0) {
			s = startgetal;
			startgetal = 0;
		}
		if (s == Countdown.NIKS) {
			return;
		}
		if (s == 0) {
			Runnable werk = bijNul;
			stop();
			Mc.titleAllen(server, Mc.tekst("GO", ChatFormatting.GREEN, ChatFormatting.BOLD), null);
			Mc.geluidAllen(server, SoundEvents.RAID_HORN, 1f, 1f);
			if (werk != null) {
				werk.run();
			}
			return;
		}
		for (ServerPlayer speler : Mc.spelers(server)) {
			if (s <= TITLES_VANAF) {
				Mc.title(speler, Mc.tekst(String.valueOf(s), ChatFormatting.YELLOW, ChatFormatting.BOLD), null, 0, 25, 5);
				// Van laag naar hoog: 5 is de laagste toon, 1 de hoogste.
				Mc.geluid(speler, SoundEvents.NOTE_BLOCK_PLING, 1f, 0.6f + (TITLES_VANAF - s) * 0.2f);
			} else {
				Mc.actionbar(speler, Mc.tekst(label + " " + s, ChatFormatting.YELLOW));
			}
		}
	}
}
