package nl.pudding.bootcamp.schrik;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import nl.pudding.bootcamp.Mc;
import nl.pudding.bootcamp.core.FontTegels;

/**
 * De jumpscare: de foto van Clown schermvullend en zijn lachje, alleen voor die speler. De foto
 * staat in font {@code bootcamp:schrik} uit het resource pack, in tegels (twee rijen van hooguit
 * zes, zie {@link FontTegels}); het lachje is het geluid {@code bootcamp:clown_lach}. Heeft een
 * speler het pack niet, dan ziet hij lege vierkantjes en hoort hij niks.
 */
public final class Schrik {
	/** Tegel (rij r, kolom c) is U+E000 + 16r + c; BouwPack gebruikt dezelfde nummering. */
	public static final int EERSTE_TEGEL = 0xE000;
	public static final int RIJ_STAP = 16;
	public static final int KOLOMMEN = 6;
	public static final String TEKST = FontTegels.tekst(EERSTE_TEGEL, RIJ_STAP, KOLOMMEN);
	public static final Identifier FONT = Identifier.fromNamespaceAndPath("bootcamp", "schrik");
	public static final Identifier LACH = Identifier.fromNamespaceAndPath("bootcamp", "clown_lach");
	/** Zo lang staat de foto in beeld, in ticks: fade-in 0, blijven 30, fade-out 10. */
	public static final int IN_BEELD = 40;

	private static final Holder<SoundEvent> GELUID = Holder.direct(SoundEvent.createVariableRangeEvent(LACH));

	private Schrik() {
	}

	public static void op(ServerPlayer speler) {
		// Zonder schaduw: een title krijgt anders een donkere rand rechtsonder.
		Component foto = Component.literal(TEKST).withStyle(s -> s.withFont(new FontDescription.Resource(FONT)).withoutShadow());
		Mc.title(speler, foto, null, 0, 30, 10);
		Mc.geluid(speler, GELUID, 1f, 1f);
	}
}
