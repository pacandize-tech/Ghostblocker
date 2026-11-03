package com.ghostblocker.dungeon;

import com.ghostblocker.config.GhostblockerConfig;
import com.ghostblocker.util.SkyblockLocation;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;

/**
 * Starred dungeon mobs (the ones that count towards the room's key/score) have a nametag
 * armor stand containing ✯ floating above them. This finds the mob under each tag and makes it glow.
 */
public final class StarredMobs {
	private static final String STAR = "✯";
	private static IntSet starred = new IntOpenHashSet();

	public static boolean isStarred(Entity entity) {
		return GhostblockerConfig.get().starredMobGlow && starred.contains(entity.getId());
	}

	public static void tick(Minecraft mc) {
		if (mc.level == null || mc.player == null || mc.level.getGameTime() % 5 != 0) return;
		if (!GhostblockerConfig.get().starredMobGlow || !SkyblockLocation.inDungeon()) {
			starred.clear();
			return;
		}
		IntSet found = new IntOpenHashSet();
		AABB area = mc.player.getBoundingBox().inflate(48);
		for (ArmorStand stand : mc.level.getEntitiesOfClass(ArmorStand.class, area,
				s -> s.hasCustomName() && s.getCustomName().getString().contains(STAR))) {
			AABB below = new AABB(stand.getX() - 0.7, stand.getY() - 3.5, stand.getZ() - 0.7,
					stand.getX() + 0.7, stand.getY(), stand.getZ() + 0.7);
			LivingEntity closest = null;
			double best = Double.MAX_VALUE;
			for (LivingEntity mob : mc.level.getEntitiesOfClass(LivingEntity.class, below,
					e -> !(e instanceof ArmorStand) && e != mc.player && e.isAlive())) {
				double d = mob.distanceToSqr(stand);
				if (d < best) {
					best = d;
					closest = mob;
				}
			}
			if (closest != null) found.add(closest.getId());
		}
		starred = found;
	}

	private StarredMobs() {
	}
}
