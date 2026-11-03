/*
 * Ported from Odin by odtheking (via Secret Routes Mod's DungeonScanner). BSD 3-Clause License,
 * Copyright (c) 2025, odtheking. See LICENSE-ODIN.txt in the repository root for the full license text.
 */
package com.ghostblocker.dungeon.rooms;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ghostblocker.Ghostblocker;
import com.ghostblocker.dungeon.routes.RouteTracker;
import com.ghostblocker.util.SkyblockLocation;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Works out which Catacombs room the player is in. Every room cell has a unique column of blocks at its centre;
 * hashing that column gives a "core" that rooms.json maps to a room name. The blue terracotta marker in one
 * corner then gives the room's rotation.
 */
public final class RoomScanner {
	private static final Map<Integer, RoomData> CORE_TO_ROOM = new HashMap<>();
	private static final long RETRY_NANOS = 250_000_000L;
	private static final Direction[] HORIZONTALS = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

	private static DungeonRoom current;
	private static final Set<DungeonRoom> visited = new HashSet<>();
	private static long nextScanNanos;
	private static int lastCellX = Integer.MIN_VALUE;
	private static int lastCellZ = Integer.MIN_VALUE;

	public static void load() {
		try (InputStream in = Ghostblocker.class.getResourceAsStream("/assets/ghostblocker/dungeons/rooms.json");
				Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
			List<RoomData> rooms = new Gson().fromJson(reader, new TypeToken<List<RoomData>>() {}.getType());
			for (RoomData room : rooms) {
				if (room.cores() != null) room.cores().forEach(core -> CORE_TO_ROOM.put(core, room));
			}
			Ghostblocker.LOGGER.info("Loaded {} dungeon rooms", rooms.size());
		} catch (Exception e) {
			Ghostblocker.LOGGER.error("Could not load dungeon room data", e);
		}
	}

	/** Test hook: the core hash of the cell centred at (x, z) in the current world. */
	public static int coreForTesting(int x, int z) {
		Minecraft mc = Minecraft.getInstance();
		return core(mc, x, z, topLayer(mc, x, z));
	}

	/** Test hook: teach the scanner a room fingerprint. */
	public static void registerForTesting(RoomData room) {
		room.cores().forEach(core -> CORE_TO_ROOM.put(core, room));
	}

	public static DungeonRoom current() {
		return current;
	}

	public static void reset() {
		current = null;
		visited.clear();
		lastCellX = lastCellZ = Integer.MIN_VALUE;
	}

	public static void tick(Minecraft mc) {
		if (mc.player == null || mc.level == null || !SkyblockLocation.inDungeon()) {
			if (current != null || !visited.isEmpty()) {
				reset();
				RouteTracker.onRoomChanged(null);
			}
			return;
		}
		// (int) truncation, not floor, to match how the room data was recorded.
		int[] cell = RoomTransform.cellCentre((int) mc.player.getX(), (int) mc.player.getZ());
		if (current != null && current.covers(cell[0], cell[1])) return;

		if (cell[0] != lastCellX || cell[1] != lastCellZ) {
			lastCellX = cell[0];
			lastCellZ = cell[1];
			nextScanNanos = 0;
			for (DungeonRoom room : visited) {
				if (room.covers(cell[0], cell[1])) {
					enter(room);
					return;
				}
			}
		}

		long now = System.nanoTime();
		if (now < nextScanNanos || !mc.level.getChunkSource().hasChunk(cell[0] >> 4, cell[1] >> 4)) return;
		nextScanNanos = now + RETRY_NANOS;
		DungeonRoom scanned = scan(mc, cell[0], cell[1]);
		if (scanned != null && scanned.rotation != Rotation.NONE) {
			visited.add(scanned);
			enter(scanned);
		} else if (current != null) {
			current = null; // walked into a corridor or an unknown room
			RouteTracker.onRoomChanged(null);
		}
	}

	private static void enter(DungeonRoom room) {
		if (room == current) return;
		current = room;
		Ghostblocker.LOGGER.info("Entered dungeon room {} ({})", room.name(), room.rotation);
		RouteTracker.onRoomChanged(room);
	}

	private static DungeonRoom scan(Minecraft mc, int x, int z) {
		int height = topLayer(mc, x, z);
		RoomData data = CORE_TO_ROOM.get(core(mc, x, z, height));
		if (data == null) return null;
		Set<DungeonRoom.Cell> cells = new HashSet<>();
		collectCells(mc, x, z, data.cores(), height, new HashSet<>(), cells);
		DungeonRoom room = new DungeonRoom(data, cells);
		findRotation(mc, room, height);
		return room;
	}

	private static void collectCells(Minecraft mc, int x, int z, List<Integer> cores, int height, Set<Long> seen, Set<DungeonRoom.Cell> out) {
		if (!seen.add(((long) x << 32) | (z & 0xFFFFFFFFL))) return;
		int core = core(mc, x, z, height);
		if (!cores.contains(core)) return;
		out.add(new DungeonRoom.Cell(x, z, core));
		for (Direction d : HORIZONTALS) {
			collectCells(mc, x + d.getStepX() * 32, z + d.getStepZ() * 32, cores, height, seen, out);
		}
	}

	private static void findRotation(Minecraft mc, DungeonRoom room, int height) {
		if ("Fairy".equals(room.name()) && !room.cells.isEmpty()) {
			DungeonRoom.Cell first = room.cells.iterator().next();
			room.anchor = new BlockPos(first.x() - 15, height, first.z() - 15);
			room.rotation = Rotation.SOUTH;
			return;
		}
		Block blue = Blocks.DYED_TERRACOTTA.blue();
		for (Rotation rot : new Rotation[] {Rotation.NORTH, Rotation.SOUTH, Rotation.WEST, Rotation.EAST}) {
			for (DungeonRoom.Cell cell : room.cells) {
				BlockPos corner = new BlockPos(cell.x() + rot.x, height, cell.z() + rot.z);
				if (mc.level.getBlockState(corner).getBlock() != blue) continue;
				boolean valid = true;
				if (room.cells.size() > 1) {
					for (Direction d : HORIZONTALS) {
						Block n = mc.level.getBlockState(corner.relative(d)).getBlock();
						if (n != Blocks.AIR && n != blue) {
							valid = false;
							break;
						}
					}
				}
				if (valid) {
					room.anchor = corner;
					room.rotation = rot;
					return;
				}
			}
		}
		room.rotation = Rotation.NONE;
	}

	/** Hash of the block column at a cell centre. Must match how rooms.json was generated, byte for byte. */
	private static int core(Minecraft mc, int x, int z, int knownHeight) {
		int height = Math.max(11, Math.min(140, knownHeight));
		StringBuilder sb = new StringBuilder(150);
		sb.append("0".repeat(140 - height));
		int bedrock = 0;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int y = height; y >= 12; y--) {
			pos.set(x, y, z);
			Block block = mc.level.getBlockState(pos).getBlock();
			if (block == Blocks.AIR && bedrock >= 2 && y < 69) {
				sb.append("0".repeat(y - 11));
				break;
			}
			if (block == Blocks.BEDROCK) {
				bedrock++;
			} else {
				bedrock = 0;
				if (block == Blocks.OAK_PLANKS || block == Blocks.TRAPPED_CHEST || block == Blocks.CHEST) continue;
			}
			sb.append(block);
		}
		return sb.toString().hashCode();
	}

	private static int topLayer(Minecraft mc, int x, int z) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int y = 160; y >= 12; y--) {
			pos.set(x, y, z);
			if (!mc.level.isEmptyBlock(pos)) {
				return mc.level.getBlockState(pos).getBlock() == Blocks.GOLD_BLOCK ? y - 1 : y;
			}
		}
		return 0;
	}

	private RoomScanner() {
	}
}
