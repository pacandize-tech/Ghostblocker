/*
 * Ghostblocker - Hypixel SkyBlock quality of life
 * Route data from Secret Routes Mod (https://github.com/yourboykyle/SecretRoutes),
 * Copyright 2025 yourboykyle, wyannnnn (R-aMcC), zzyyrraa, Itplays, 3ppopka. GPL-3.0-or-later.
 * Secret locations from Dungeon Rooms Mod (https://github.com/Quantizr/DungeonRoomsMod),
 * Copyright 2021 Quantizr(_risk). GPL-3.0-or-later.
 */
package com.ghostblocker.dungeon.routes;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ghostblocker.Ghostblocker;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Loads the bundled route sets and Dungeon Rooms Mod's secret locations. */
public final class RouteDatabase {
	/** The bundled Secret Routes route sets. */
	public enum RouteSet {
		FOW("fowroutes.json", "FOW routes (default)"),
		STANDARD("routes.json", "Standard routes"),
		PEARL("pearlroutes.json", "Pearl routes"),
		POPKA("3ppopkaroutes.json", "3ppopka routes");

		public final String file;
		public final String label;

		RouteSet(String file, String label) {
			this.file = file;
			this.label = label;
		}
	}

	/** A route for a room. Rooms can have several ("Room", "Room:2", ...); the closest start is used. */
	public record Variant(String key, int number, List<RouteStep> steps) {
		public int[] start() {
			return steps.isEmpty() || steps.getFirst().path().isEmpty() ? null : steps.getFirst().path().getFirst();
		}
	}

	/** A secret from Dungeon Rooms Mod, e.g. "2 - Chest" in category "chest", room-relative. */
	public record Secret(String name, String category, int x, int y, int z) {
	}

	private static final Map<RouteSet, Map<String, List<Variant>>> ROUTES = new HashMap<>();
	private static Map<String, List<Secret>> secrets = Map.of();

	public static List<Variant> variants(RouteSet set, String room) {
		return ROUTES.computeIfAbsent(set, RouteDatabase::loadRoutes).getOrDefault(room.toLowerCase(Locale.ROOT), List.of());
	}

	public static List<Secret> secrets(String room) {
		return secrets.getOrDefault(room.toLowerCase(Locale.ROOT), List.of());
	}

	public static void loadSecrets() {
		JsonObject json = read("/assets/ghostblocker/dungeons/secretlocations.json");
		Map<String, List<Secret>> out = new HashMap<>();
		if (json != null) {
			for (Map.Entry<String, JsonElement> e : json.entrySet()) {
				if (!e.getValue().isJsonArray()) continue; // skips the copyright/license fields
				List<Secret> list = new ArrayList<>();
				for (JsonElement s : e.getValue().getAsJsonArray()) {
					JsonObject o = s.getAsJsonObject();
					list.add(new Secret(o.get("secretName").getAsString(), o.get("category").getAsString(),
							o.get("x").getAsInt(), o.get("y").getAsInt(), o.get("z").getAsInt()));
				}
				out.put(e.getKey().toLowerCase(Locale.ROOT), list);
			}
		}
		secrets = out;
		Ghostblocker.LOGGER.info("Loaded secret locations for {} rooms", out.size());
	}

	private static Map<String, List<Variant>> loadRoutes(RouteSet set) {
		JsonObject json = read("/assets/ghostblocker/dungeons/routes/" + set.file);
		Map<String, List<Variant>> out = parseRoutes(json);
		Ghostblocker.LOGGER.info("Loaded {} with routes for {} rooms", set.file, out.size());
		return out;
	}

	/** Groups "Room" and "Room:N" keys under the room name. Visible for testing. */
	static Map<String, List<Variant>> parseRoutes(JsonObject json) {
		Map<String, List<Variant>> out = new HashMap<>();
		if (json == null) return out;
		for (Map.Entry<String, JsonElement> e : json.entrySet()) {
			if (!e.getValue().isJsonArray()) continue; // "Version", "#origin", ...
			String key = e.getKey();
			int colon = key.lastIndexOf(':');
			String room = key;
			int number = 0;
			if (colon > 0) {
				try {
					number = Integer.parseInt(key.substring(colon + 1));
					room = key.substring(0, colon);
				} catch (NumberFormatException ignored) {
					// not a variant suffix
				}
			}
			List<RouteStep> steps = new ArrayList<>();
			for (JsonElement step : (JsonArray) e.getValue()) {
				if (step.isJsonObject()) steps.add(RouteStep.parse(step.getAsJsonObject()));
			}
			if (!steps.isEmpty()) {
				out.computeIfAbsent(room.toLowerCase(Locale.ROOT), k -> new ArrayList<>()).add(new Variant(key, number, steps));
			}
		}
		out.values().forEach(list -> list.sort(Comparator.comparingInt(Variant::number)));
		return out;
	}

	private static JsonObject read(String path) {
		try (InputStream in = Ghostblocker.class.getResourceAsStream(path);
				Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
			return JsonParser.parseReader(reader).getAsJsonObject();
		} catch (Exception e) {
			Ghostblocker.LOGGER.error("Could not load {}", path, e);
			return null;
		}
	}

	private RouteDatabase() {
	}
}
