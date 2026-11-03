/*
 * Ghostblocker - Hypixel SkyBlock quality of life
 * Reads the route format of Secret Routes Mod, Copyright 2025 yourboykyle & contributors,
 * licensed under the GNU General Public License v3.0 or later.
 */
package com.ghostblocker.dungeon.routes;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

/**
 * One step of a secret route: walk along {@code path}, use the marked etherwarp/stonk/superboom/pearl spots,
 * then collect the secret. All positions are room-relative {x, y, z}.
 */
public record RouteStep(List<int[]> path, List<int[]> etherwarps, List<int[]> mines, List<int[]> interacts,
		List<int[]> superbooms, List<int[]> enderpearls, List<int[]> bonzoStaffs, String secretType, int[] secretPos) {

	public static RouteStep parse(JsonObject json) {
		String type = null;
		int[] pos = null;
		JsonElement secret = json.get("secret");
		if (secret != null && secret.isJsonObject()) {
			JsonObject s = secret.getAsJsonObject();
			if (s.has("type")) type = s.get("type").getAsString();
			if (s.has("location")) pos = point(s.getAsJsonArray("location"));
		}
		return new RouteStep(points(json, "locations"), points(json, "etherwarps"), points(json, "mines"),
				points(json, "interacts"), points(json, "tnts"), points(json, "enderpearls"), points(json, "bonzo_staffs"),
				type, pos);
	}

	/** Whether finishing this step means picking up a secret (vs. just leaving the room). */
	public boolean hasSecret() {
		return secretPos != null && ("interact".equals(secretType) || "item".equals(secretType) || "bat".equals(secretType));
	}

	private static List<int[]> points(JsonObject json, String key) {
		List<int[]> out = new ArrayList<>();
		JsonElement e = json.get(key);
		if (e == null || !e.isJsonArray()) return out;
		for (JsonElement p : e.getAsJsonArray()) {
			if (p.isJsonArray() && p.getAsJsonArray().size() >= 3) out.add(point(p.getAsJsonArray()));
		}
		return out;
	}

	private static int[] point(JsonArray a) {
		return new int[] {a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt()};
	}
}
