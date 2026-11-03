/*
 * Ghostblocker - Hypixel SkyBlock quality of life
 * Route progression modelled on Secret Routes Mod (https://github.com/yourboykyle/SecretRoutes),
 * Copyright 2025 yourboykyle & contributors. GPL-3.0-or-later.
 */
package com.ghostblocker.dungeon.routes;

import com.ghostblocker.config.GhostblockerConfig;
import com.ghostblocker.dungeon.rooms.DungeonRoom;
import com.ghostblocker.dungeon.rooms.RoomTransform;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

/**
 * Tracks where you are in the current room's route. A step only advances when you actually get its secret
 * (open the chest / flip the lever, pick up the item, kill the bat) or when you press the next-step key.
 */
public final class RouteTracker {
	private static DungeonRoom room;
	private static List<RouteDatabase.Variant> variants = List.of();
	private static int variantIndex = -1;
	private static int stepIndex;
	/** World positions of Dungeon Rooms Mod secrets already collected in this room. */
	private static final Set<BlockPos> foundSecrets = new HashSet<>();

	public static DungeonRoom room() {
		return room;
	}

	public static List<RouteStep> steps() {
		return variantIndex < 0 ? List.of() : variants.get(variantIndex).steps();
	}

	public static int stepIndex() {
		return stepIndex;
	}

	public static RouteStep currentStep() {
		List<RouteStep> steps = steps();
		return stepIndex < steps.size() ? steps.get(stepIndex) : null;
	}

	public static boolean isFound(BlockPos worldPos) {
		return foundSecrets.contains(worldPos);
	}

	public static void onRoomChanged(DungeonRoom newRoom) {
		room = newRoom;
		stepIndex = 0;
		foundSecrets.clear();
		variantIndex = -1;
		variants = newRoom == null ? List.of() : RouteDatabase.variants(GhostblockerConfig.get().routeSet, newRoom.name());
		if (newRoom == null) return;
		pickClosestVariant();

		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null && GhostblockerConfig.get().secretRoutes) {
			String route = variantIndex < 0 ? "no route available"
					: steps().size() + " steps" + (variants.size() > 1 ? ", route " + (variantIndex + 1) + "/" + variants.size() : "");
			mc.player.sendOverlayMessage(Component.literal(newRoom.name() + " • " + route).withStyle(ChatFormatting.AQUA));
		}
	}

	/** Re-reads routes after the route set is changed in settings. */
	public static void reload() {
		onRoomChanged(room);
	}

	private static void pickClosestVariant() {
		Minecraft mc = Minecraft.getInstance();
		if (variants.isEmpty() || mc.player == null) return;
		double[] rel = RoomTransform.toRelative(mc.player.getX(), mc.player.getY(), mc.player.getZ(),
				room.rotation, room.anchor.getX(), room.anchor.getZ());
		double best = Double.MAX_VALUE;
		for (int i = 0; i < variants.size(); i++) {
			int[] start = variants.get(i).start();
			if (start == null) continue;
			double dx = start[0] - rel[0], dy = start[1] - rel[1], dz = start[2] - rel[2];
			double d = dx * dx + dy * dy + dz * dz;
			if (d < best) {
				best = d;
				variantIndex = i;
			}
		}
		if (variantIndex < 0) variantIndex = 0;
	}

	public static void nextStep() {
		if (stepIndex < steps().size()) stepIndex++;
		announce();
	}

	public static void previousStep() {
		if (stepIndex > 0) stepIndex--;
		announce();
	}

	public static void cycleVariant() {
		if (variants.size() <= 1) return;
		variantIndex = (variantIndex + 1) % variants.size();
		stepIndex = 0;
		announce();
	}

	private static void announce() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || room == null) return;
		String text = stepIndex >= steps().size() ? "Route complete" : "Step " + (stepIndex + 1) + "/" + steps().size();
		mc.player.sendOverlayMessage(Component.literal(text).withStyle(ChatFormatting.AQUA));
	}

	private static void completeStep() {
		stepIndex++;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null) mc.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.6f, 1.4f);
		announce();
	}

	// ---- secret detection, called from mixins on the main thread

	/** You right-clicked a block (chest, lever, wither essence skull, ...). */
	public static void onInteract(BlockPos clicked) {
		if (room == null) return;
		markSecret(clicked.getX() + 0.5, clicked.getY() + 0.5, clicked.getZ() + 0.5, 1.5, "chest", "wither", "lever");
		RouteStep step = currentStep();
		if (step != null && "interact".equals(step.secretType()) && near(step.secretPos(), clicked.getX(), clicked.getY(), clicked.getZ(), 1.5)) {
			completeStep();
		}
	}

	/** You picked up an item entity at this position. */
	public static void onItemPickup(double x, double y, double z) {
		if (room == null) return;
		markSecret(x, y, z, 6, "item");
		RouteStep step = currentStep();
		if (step != null && "item".equals(step.secretType()) && near(step.secretPos(), x, y, z, 6)) completeStep();
	}

	/** A bat died at this position. */
	public static void onBatDeath(double x, double y, double z) {
		if (room == null) return;
		markSecret(x, y, z, 10, "bat");
		RouteStep step = currentStep();
		if (step != null && "bat".equals(step.secretType()) && near(step.secretPos(), x, y, z, 10)) completeStep();
	}

	private static void markSecret(double x, double y, double z, double radius, String... categories) {
		for (RouteDatabase.Secret s : RouteDatabase.secrets(room.name())) {
			if (!List.of(categories).contains(s.category())) continue;
			BlockPos pos = toWorld(s.x(), s.y(), s.z());
			if (pos.distToCenterSqr(x, y, z) <= radius * radius) foundSecrets.add(pos);
		}
	}

	private static boolean near(int[] relative, double x, double y, double z, double radius) {
		if (relative == null) return false;
		return toWorld(relative[0], relative[1], relative[2]).distToCenterSqr(x, y, z) <= radius * radius;
	}

	public static BlockPos toWorld(int rx, int ry, int rz) {
		double[] w = RoomTransform.toWorld(rx, ry, rz, room.rotation, room.anchor.getX(), room.anchor.getZ());
		return BlockPos.containing(w[0], w[1], w[2]);
	}

	private RouteTracker() {
	}
}
