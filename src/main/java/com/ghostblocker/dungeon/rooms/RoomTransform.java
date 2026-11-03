/*
 * Ghostblocker - Hypixel SkyBlock quality of life
 * Coordinate maths adapted from Secret Routes Mod (RoomRotationUtils), Copyright 2025 yourboykyle & contributors,
 * licensed under the GNU General Public License v3.0 or later.
 */
package com.ghostblocker.dungeon.rooms;

/**
 * Converts between room-relative coordinates (as stored in the route and secret files) and world coordinates.
 * Pure maths with no Minecraft types, so it can be unit tested.
 */
public final class RoomTransform {
	/** Relative (rx, ry, rz) to world {x, y, z} for a room anchored at (ax, az) with this rotation. */
	public static double[] toWorld(double rx, double ry, double rz, Rotation rotation, double ax, double az) {
		return switch (rotation) {
			case WEST -> new double[] {ax - rz, ry, az + rx};
			case NORTH -> new double[] {ax - rx, ry, az - rz};
			case EAST -> new double[] {ax + rz, ry, az - rx};
			default -> new double[] {ax + rx, ry, az + rz}; // SOUTH (and NONE)
		};
	}

	/** World (x, y, z) to room-relative {rx, ry, rz}; the inverse of {@link #toWorld}. */
	public static double[] toRelative(double x, double y, double z, Rotation rotation, double ax, double az) {
		double dx = x - ax;
		double dz = z - az;
		return switch (rotation) {
			case WEST -> new double[] {dz, y, -dx};
			case NORTH -> new double[] {-dx, y, -dz};
			case EAST -> new double[] {-dz, y, dx};
			default -> new double[] {dx, y, dz};
		};
	}

	/** Centre of the 32x32 dungeon grid cell containing this world position. */
	public static int[] cellCentre(int x, int z) {
		return new int[] {snap(x), snap(z)};
	}

	private static final int START = -185;

	private static int snap(int v) {
		int cell = (v - START + 16) >> 5;
		return (cell << 5) + START;
	}

	private RoomTransform() {
	}
}
