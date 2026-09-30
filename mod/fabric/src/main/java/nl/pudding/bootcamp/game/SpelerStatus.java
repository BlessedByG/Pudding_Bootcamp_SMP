package nl.pudding.bootcamp.game;

import net.minecraft.network.chat.Component;
import nl.pudding.bootcamp.core.Rol;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Wat de mod van één speler weet. De mod is de bron van waarheid; scoreboard-tags zijn spiegels. */
public final class SpelerStatus {
	public final UUID id;
	public String naam;
	public Rol rol = Rol.SPELER;
	/** Uit de lopende ronde: gesneuveld of uitgelogd. In de mob arena: af voor de rest van de ronde. */
	public boolean dood;
	/** Klaar met de lopende ronde: uit het doolhof (team gekozen). */
	public boolean klaar;
	/** Staat bevroren: niet lopen, niet springen, niet schieten, niet pearlen. */
	public boolean bevroren;
	/** Kills in de FFA. */
	public int kills;
	/** Het tribunepunt (of de kooi) waar deze kijker staat; daar zet de mod hem terug. */
	public String tribunepunt;
	/** Ronde 1: de schrikplekken die hij al had. */
	public final Set<String> schrikGehad = new HashSet<>();
	/** Ronde 1: de servertick waarop hij het teammenu sloot zonder te kiezen. */
	public int menuDicht = -1000;
	/** Ronde 2: zijn eigen startplek. */
	public String eiSpawn;
	/** Ronde 3: 1 als hij deze beurt in het veld staat, anders 0. */
	public int arena;
	/** Ronde 3: 1 als hij als kijker in de kooi zit, anders 0. */
	public int kooi;
	/** Een tijdelijke regel vooraan in de actionbar, tot servertick {@link #meldingTot}. */
	public Component melding;
	public int meldingTot;

	SpelerStatus(UUID id, String naam) {
		this.id = id;
		this.naam = naam;
	}

	/** Terug naar een schone speler, voor {@code /bc reset}. */
	void wis() {
		rol = Rol.SPELER;
		nieuweRonde();
	}

	/** Bij de start van elke ronde. */
	void nieuweRonde() {
		dood = false;
		klaar = false;
		bevroren = false;
		kills = 0;
		tribunepunt = null;
		schrikGehad.clear();
		menuDicht = -1000;
		eiSpawn = null;
		arena = 0;
		kooi = 0;
		melding = null;
		meldingTot = 0;
	}
}
