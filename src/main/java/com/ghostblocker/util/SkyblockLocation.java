package com.ghostblocker.util;

import com.ghostblocker.Ghostblocker;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

/** Reads Hypixel's sidebar to find out where the player is. */
public final class SkyblockLocation {
	private static long cachedTick = -1;
	private static boolean cachedInDungeon;

	public static boolean inDungeon() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return false;
		long tick = mc.level.getGameTime();
		if (tick != cachedTick) {
			cachedTick = tick;
			boolean now = sidebarLines().stream().anyMatch(line -> line.contains("The Catacombs"));
			if (now != cachedInDungeon) Ghostblocker.LOGGER.info(now ? "Entered the Catacombs" : "Left the Catacombs");
			cachedInDungeon = now;
		}
		return cachedInDungeon;
	}

	/**
	 * Visible sidebar lines. Hypixel puts each line's text in its team's prefix and suffix; the score holder's
	 * own name is just colour codes and a filler emoji, so it is left out instead of splitting words apart.
	 */
	public static List<String> sidebarLines() {
		List<String> lines = new ArrayList<>();
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return lines;
		Scoreboard scoreboard = mc.level.getScoreboard();
		Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
		if (objective == null) return lines;
		for (PlayerScoreEntry entry : scoreboard.listPlayerScores(objective)) {
			PlayerTeam team = scoreboard.getPlayersTeam(entry.owner());
			String text = team != null
					? team.getPlayerPrefix().getString() + team.getPlayerSuffix().getString()
					: entry.owner();
			lines.add(clean(text));
		}
		return lines;
	}

	/** Strips formatting codes and the emoji/control characters Hypixel scatters through sidebar lines. */
	public static String clean(String raw) {
		String stripped = ChatFormatting.stripFormatting(raw);
		if (stripped == null) return "";
		StringBuilder sb = new StringBuilder(stripped.length());
		stripped.codePoints()
				.filter(cp -> cp >= 0x20 && cp < 0x7F || cp == '⏣' || Character.isLetter(cp))
				.forEach(sb::appendCodePoint);
		return sb.toString();
	}

	private SkyblockLocation() {
	}
}
