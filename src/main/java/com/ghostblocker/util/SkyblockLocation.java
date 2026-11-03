package com.ghostblocker.util;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

/** Reads Hypixel's sidebar to find out where the player is. */
public final class SkyblockLocation {
	private static long cachedTick = -1;
	private static boolean cachedInDungeon;

	/** Lets the in-game test pretend to be in the Catacombs. */
	public static boolean forceDungeonForTesting;

	public static boolean inDungeon() {
		if (forceDungeonForTesting) return true;
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return false;
		long tick = mc.level.getGameTime();
		if (tick != cachedTick) {
			cachedTick = tick;
			cachedInDungeon = sidebarLines().stream().anyMatch(line -> line.contains("The Catacombs"));
		}
		return cachedInDungeon;
	}

	public static List<String> sidebarLines() {
		List<String> lines = new ArrayList<>();
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return lines;
		Scoreboard scoreboard = mc.level.getScoreboard();
		Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
		if (objective == null) return lines;
		for (PlayerScoreEntry entry : scoreboard.listPlayerScores(objective)) {
			PlayerTeam team = scoreboard.getPlayersTeam(entry.owner());
			String text = PlayerTeam.formatNameForTeam(team, Component.literal(entry.owner())).getString();
			String stripped = ChatFormatting.stripFormatting(text);
			if (stripped != null) lines.add(stripped);
		}
		return lines;
	}

	private SkyblockLocation() {
	}
}
