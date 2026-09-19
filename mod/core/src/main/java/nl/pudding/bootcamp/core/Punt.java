package nl.pudding.bootcamp.core;

/**
 * Een punt uit {@code bootcamp.json}: een positie met kijkrichting ({@code /bc point set}) of een
 * blok ({@code /bc point block}). Bij een blok zijn x, y en z hele getallen en telt de
 * kijkrichting niet.
 */
public record Punt(double x, double y, double z, float yaw, float pitch, boolean blok) {

	public static Punt positie(double x, double y, double z, float yaw, float pitch) {
		return new Punt(x, y, z, yaw, pitch, false);
	}

	public static Punt blok(BlokPos pos) {
		return new Punt(pos.x(), pos.y(), pos.z(), 0f, 0f, true);
	}

	public BlokPos blokPos() {
		return new BlokPos((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
	}
}
