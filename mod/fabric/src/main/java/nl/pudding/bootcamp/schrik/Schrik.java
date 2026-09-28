package nl.pudding.bootcamp.schrik;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import nl.pudding.bootcamp.Mc;

/**
 * De jumpscare: de foto van Clown schermvullend en zijn lachje, alleen voor die speler. De foto is
 * een glyph in font {@code bootcamp:schrik} uit het resource pack, het lachje het geluid
 * {@code bootcamp:clown_lach}. Heeft een speler het pack niet, dan ziet hij een leeg vierkantje en
 * hoort hij niks.
 */
public final class Schrik {
	public static final String GLYPH = "";
	public static final Identifier FONT = Identifier.fromNamespaceAndPath("bootcamp", "schrik");
	public static final Identifier LACH = Identifier.fromNamespaceAndPath("bootcamp", "clown_lach");
	/** Zo lang staat de foto in beeld, in ticks: fade-in 0, blijven 30, fade-out 10. */
	public static final int IN_BEELD = 40;

	private static final Holder<SoundEvent> GELUID = Holder.direct(SoundEvent.createVariableRangeEvent(LACH));

	private Schrik() {
	}

	public static void op(ServerPlayer speler) {
		Component foto = Component.literal(GLYPH).withStyle(s -> s.withFont(new FontDescription.Resource(FONT)));
		Mc.title(speler, foto, null, 0, 30, 10);
		Mc.geluid(speler, GELUID, 1f, 1f);
	}
}
