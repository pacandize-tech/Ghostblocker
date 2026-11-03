/*
 * Ported from Odin by odtheking (via Secret Routes Mod). BSD 3-Clause License, Copyright (c) 2025, odtheking.
 * See LICENSE-ODIN.txt in the repository root for the full license text.
 */
package com.ghostblocker.dungeon.rooms;

import java.util.Set;
import net.minecraft.core.BlockPos;

/** A detected room: what it is, which grid cells it covers, and its rotation anchor. */
public final class DungeonRoom {
	/** One 32x32 grid cell of a room; x/z are the cell centre. */
	public record Cell(int x, int z, int core) {
	}

	public final RoomData data;
	public final Set<Cell> cells;
	public Rotation rotation = Rotation.NONE;
	/** The blue terracotta corner block; route and secret coordinates are relative to it. */
	public BlockPos anchor = BlockPos.ZERO;

	public DungeonRoom(RoomData data, Set<Cell> cells) {
		this.data = data;
		this.cells = cells;
	}

	public String name() {
		return data.name();
	}

	public boolean covers(int cellX, int cellZ) {
		for (Cell cell : cells) {
			if (cell.x() == cellX && cell.z() == cellZ) return true;
		}
		return false;
	}
}
