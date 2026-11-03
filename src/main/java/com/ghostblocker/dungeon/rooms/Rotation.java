/*
 * Ported from Odin by odtheking (via Secret Routes Mod). BSD 3-Clause License, Copyright (c) 2025, odtheking.
 * See LICENSE-ODIN.txt in the repository root for the full license text.
 */
package com.ghostblocker.dungeon.rooms;

/**
 * Which way a room is rotated, found by locating the blue terracotta marker in one of its corners.
 * {@code x}/{@code z} are the offset from a cell's centre to that corner.
 */
public enum Rotation {
	NORTH(15, 15),
	SOUTH(-15, -15),
	WEST(15, -15),
	EAST(-15, 15),
	NONE(0, 0);

	public final int x;
	public final int z;

	Rotation(int x, int z) {
		this.x = x;
		this.z = z;
	}
}
