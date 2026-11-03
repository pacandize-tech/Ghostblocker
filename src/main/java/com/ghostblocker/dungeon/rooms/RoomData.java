/*
 * Ported from Odin by odtheking (via Secret Routes Mod). BSD 3-Clause License, Copyright (c) 2025, odtheking.
 * See LICENSE-ODIN.txt in the repository root for the full license text.
 */
package com.ghostblocker.dungeon.rooms;

import java.util.List;

/** One entry of rooms.json: a room's name and the "core" hashes of each of its 32x32 cells. */
public record RoomData(String name, Type type, List<Integer> cores, int crypts, int secrets, int trappedChests, String shape) {
	public enum Type { BLOOD, CHAMPION, ENTRANCE, FAIRY, NORMAL, PUZZLE, RARE, TRAP }
}
