package nl.pudding.bootcamp.game;

import net.minecraft.world.item.ItemStack;
import nl.pudding.bootcamp.core.Rol;

import java.util.List;
import java.util.UUID;

/** Wat de mod van één speler weet. De mod is de bron van waarheid; scoreboard-tags zijn spiegels. */
public final class SpelerStatus {
	public final UUID id;
	public String naam;
	public Rol rol = Rol.SPELER;
	/** Uit de lopende ronde: gesneuveld of uitgelogd. */
	public boolean dood;
	/** Klaar met de lopende ronde: uit het doolhof, of het ticket ingeleverd. */
	public boolean klaar;
	public boolean ticket;
	/** Staat in een Opstelling: niet lopen, niet springen, niet pearlen. */
	public boolean bevroren;
	/** Kills in de FFA, voor de tiebreak. */
	public int kills;
	/** Het tribunepunt waar deze kijker staat; daar zet de mod hem terug als hij de vloer op komt. */
	public String tribunepunt;
	/** Ronde 2: de inventory van een dode, terug te geven bij v3. Index = slot. */
	public List<ItemStack> bewaard;

	SpelerStatus(UUID id, String naam) {
		this.id = id;
		this.naam = naam;
	}

	/** Terug naar een schone speler, voor {@code /bc reset}. */
	void wis() {
		rol = Rol.SPELER;
		nieuweRonde();
		ticket = false;
		bewaard = null;
	}

	/** Bij de start van elke ronde. */
	void nieuweRonde() {
		dood = false;
		klaar = false;
		bevroren = false;
		kills = 0;
		tribunepunt = null;
	}
}
