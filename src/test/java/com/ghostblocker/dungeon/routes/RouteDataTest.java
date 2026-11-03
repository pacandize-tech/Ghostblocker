package com.ghostblocker.dungeon.routes;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ghostblocker.dungeon.rooms.Rotation;
import com.ghostblocker.dungeon.rooms.RoomTransform;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RouteDataTest {
	private static JsonElement resource(String path) throws Exception {
		try (var in = RouteDataTest.class.getResourceAsStream(path)) {
			assertNotNull(in, "missing bundled resource " + path);
			return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
		}
	}

	private static Set<String> roomNames() throws Exception {
		Set<String> names = new HashSet<>();
		for (JsonElement room : (JsonArray) resource("/assets/ghostblocker/dungeons/rooms.json")) {
			names.add(room.getAsJsonObject().get("name").getAsString().toLowerCase(Locale.ROOT));
		}
		return names;
	}

	@Test
	void everyRouteSetParsesAndMatchesDetectableRooms() throws Exception {
		Set<String> detectable = roomNames();
		for (RouteDatabase.RouteSet set : RouteDatabase.RouteSet.values()) {
			JsonObject json = resource("/assets/ghostblocker/dungeons/routes/" + set.file).getAsJsonObject();
			Map<String, List<RouteDatabase.Variant>> routes = RouteDatabase.parseRoutes(json);
			assertTrue(routes.size() > 90, set + " should cover most rooms, got " + routes.size());

			long unknown = routes.keySet().stream().filter(r -> !r.equals("f7boss") && !detectable.contains(r)).count();
			assertTrue(unknown <= 3, set + " has " + unknown + " rooms that room detection can't recognise");

			for (List<RouteDatabase.Variant> variants : routes.values()) {
				for (RouteDatabase.Variant v : variants) {
					assertFalse(v.steps().isEmpty());
					for (RouteStep step : v.steps()) {
						for (int[] p : step.path()) assertEquals(3, p.length);
					}
				}
			}
		}
	}

	@Test
	void secretLocationsCoverRoutedRooms() throws Exception {
		JsonObject secrets = resource("/assets/ghostblocker/dungeons/secretlocations.json").getAsJsonObject();
		Set<String> withSecrets = new HashSet<>();
		secrets.entrySet().stream().filter(e -> e.getValue().isJsonArray())
				.forEach(e -> withSecrets.add(e.getKey().toLowerCase(Locale.ROOT)));
		assertTrue(withSecrets.size() > 100);

		JsonObject fow = resource("/assets/ghostblocker/dungeons/routes/fowroutes.json").getAsJsonObject();
		Set<String> routed = RouteDatabase.parseRoutes(fow).keySet();
		long covered = routed.stream().filter(withSecrets::contains).count();
		assertTrue(covered >= routed.size() * 0.8, "only " + covered + "/" + routed.size() + " routed rooms have secret data");
	}

	@Test
	void variantsAreGroupedUnderTheRoomName() {
		JsonObject json = JsonParser.parseString("""
				{"Version":"1","Tomb-1":[{"locations":[[1,2,3]],"secret":{"type":"item","location":[4,5,6]}}],
				 "Tomb-1:2":[{"locations":[[9,9,9]],"secret":{"type":"bat","location":[7,7,7]}}]}
				""").getAsJsonObject();
		List<RouteDatabase.Variant> variants = RouteDatabase.parseRoutes(json).get("tomb-1");
		assertEquals(2, variants.size());
		assertEquals(0, variants.get(0).number());
		assertEquals(2, variants.get(1).number());
		assertArrayEquals(new int[] {9, 9, 9}, variants.get(1).start());
		RouteStep step = variants.get(0).steps().getFirst();
		assertEquals("item", step.secretType());
		assertArrayEquals(new int[] {4, 5, 6}, step.secretPos());
		assertTrue(step.hasSecret());
	}

	@Test
	void roomTransformRoundTripsForEveryRotation() {
		for (Rotation rot : new Rotation[] {Rotation.NORTH, Rotation.SOUTH, Rotation.EAST, Rotation.WEST}) {
			double[] world = RoomTransform.toWorld(3, 70, 11, rot, -170, -122);
			double[] back = RoomTransform.toRelative(world[0], world[1], world[2], rot, -170, -122);
			assertArrayEquals(new double[] {3, 70, 11}, back, 1e-9, rot.name());
		}
		// SOUTH is the identity rotation: relative coords are just offsets from the anchor.
		assertArrayEquals(new double[] {-167, 70, -111}, RoomTransform.toWorld(3, 70, 11, Rotation.SOUTH, -170, -122), 1e-9);
		assertArrayEquals(new double[] {-173, 70, -133}, RoomTransform.toWorld(3, 70, 11, Rotation.NORTH, -170, -122), 1e-9);
	}

	@Test
	void cellCentresSnapToTheDungeonGrid() {
		assertArrayEquals(new int[] {-185, -185}, RoomTransform.cellCentre(-185, -185));
		assertArrayEquals(new int[] {-185, -153}, RoomTransform.cellCentre(-170, -160));
		assertArrayEquals(new int[] {-153, -153}, RoomTransform.cellCentre(-168, -168));
	}
}
