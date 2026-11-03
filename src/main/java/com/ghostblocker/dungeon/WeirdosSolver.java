package com.ghostblocker.dungeon;

import com.ghostblocker.config.GhostblockerConfig;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

/**
 * Three Weirdos puzzle: three NPCs each make a statement and exactly one of the statements below
 * comes from the NPC whose chest has the reward.
 */
public final class WeirdosSolver {
	private static final Pattern NPC_LINE = Pattern.compile("^\\[NPC] (\\w+): (.+)$");

	/** Statements made by the NPC with the reward. Some are prefixes because they end with another NPC's name. */
	static final List<String> REWARD_STATEMENTS = List.of(
			"The reward is not in my chest!",
			"At least one of them is lying, and the reward is not in",
			"My chest doesn't have the reward. We are all telling the truth.",
			"My chest has the reward and I'm telling the truth!",
			"The reward isn't in any of our chests.",
			"Both of them are telling the truth. Also,");

	/** Returns the NPC name if this chat line identifies the reward chest, else null. */
	static String solve(String chatLine) {
		Matcher m = NPC_LINE.matcher(chatLine);
		if (!m.matches()) return null;
		String statement = m.group(2);
		for (String reward : REWARD_STATEMENTS) {
			if (statement.startsWith(reward)) return m.group(1);
		}
		return null;
	}

	public static void onGameMessage(String raw) {
		if (!GhostblockerConfig.get().weirdosSolver) return;
		String line = ChatFormatting.stripFormatting(raw);
		String npc = line == null ? null : solve(line.trim());
		if (npc == null) return;

		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return;
		mc.player.sendSystemMessage(Component.literal("[Ghostblocker] Open " + npc + "'s chest!").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
		mc.gui.hud.setTitle(Component.literal(npc).withStyle(ChatFormatting.GREEN));
		mc.gui.hud.setSubtitle(Component.literal("has the reward"));
		mc.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
	}

	private WeirdosSolver() {
	}
}
