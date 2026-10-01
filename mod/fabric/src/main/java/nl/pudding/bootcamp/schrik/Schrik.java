package nl.pudding.bootcamp.schrik;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import nl.pudding.bootcamp.Mc;
import nl.pudding.bootcamp.core.FontTegels;
import nl.pudding.bootcamp.core.Regels;
import nl.pudding.bootcamp.game.Spel;

/**
 * De jumpscare: een van de vijf foto's schermvullend met het schrikgeluid, alleen voor die speler.
 * Elke foto staat in een eigen font {@code bootcamp:schrik_1} t/m {@code bootcamp:schrik_5} uit het
 * resource pack, in tegels (twee rijen van hooguit zes, zie {@link FontTegels}); het geluid is
 * {@code bootcamp:schrik}. Daarnaast de 8D-klop ({@code bootcamp:klop}) uit de valkisten, alleen
 * geluid. Heeft een speler het pack niet, dan ziet hij lege vierkantjes en hoort hij niks.
 */
public final class Schrik {
	/** Tegel (rij r, kolom c) is U+E000 + 16r + c, in elke foto-font; BouwPack gebruikt dezelfde nummering. */
	public static final int EERSTE_TEGEL = 0xE000;
	public static final int RIJ_STAP = 16;
	public static final int KOLOMMEN = 6;
	public static final String TEKST = FontTegels.tekst(EERSTE_TEGEL, RIJ_STAP, KOLOMMEN);
	/** Zo lang staat de foto in beeld, in ticks: fade-in 0, blijven 30, fade-out 10. */
	public static final int IN_BEELD = 40;

	private static final Holder<SoundEvent> GELUID = geluid("schrik");
	private static final Holder<SoundEvent> KLOP = geluid("klop");

	private Schrik() {
	}

	/** De font met foto 1 t/m {@link Regels#SCHRIK_FOTOS}. */
	public static Identifier font(int foto) {
		return Identifier.fromNamespaceAndPath("bootcamp", "schrik_" + foto);
	}

	/** Een jumpscare met een willekeurige foto. */
	public static void op(ServerPlayer speler) {
		op(speler, 0);
	}

	/** @param foto 1 t/m {@link Regels#SCHRIK_FOTOS}, of 0 voor een willekeurige */
	public static void op(ServerPlayer speler, int foto) {
		Identifier font = font(Regels.schrikFoto(foto, Spel.RANDOM));
		// Zonder schaduw: een title krijgt anders een donkere rand rechtsonder.
		Component beeld = Component.literal(TEKST).withStyle(s -> s.withFont(new FontDescription.Resource(font)).withoutShadow());
		// Eerst het geluid, dan het beeld: een geluid heeft bij de client net iets meer aanlooptijd.
		Mc.geluid(speler, GELUID, 1f, 1f);
		Mc.title(speler, beeld, null, 0, 30, 10);
	}

	/** De 8D-klop, alleen voor deze speler. Stereo, dus zonder richting: het 8D-effect zit in het geluid zelf. */
	public static void klop(ServerPlayer speler) {
		Mc.geluid(speler, KLOP, 1f, 1f);
	}

	private static Holder<SoundEvent> geluid(String naam) {
		return Holder.direct(SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("bootcamp", naam)));
	}
}
