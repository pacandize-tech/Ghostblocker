package com.ghostblocker.feature;

import com.ghostblocker.config.GhostblockerConfig;
import com.ghostblocker.mixin.AbstractContainerScreenAccessor;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.core.component.DataComponents;

/**
 * Never pulls your whole stash into your inventory. Instead it:
 *  - adds a [Choose items] button to Hypixel's stash messages that opens the stash menu,
 *  - adds a search box to the stash menu that highlights matching items so you click only what you want.
 * It never clicks anything for you.
 */
public final class StashPicker {
	private static final Pattern STASH_MESSAGE = Pattern.compile("You have ([\\d,]+) (items?|materials?) stashed away");
	private static final Pattern ALMOST_FULL = Pattern.compile("STASH IS ALMOST AT MAX CAPACITY", Pattern.CASE_INSENSITIVE);

	private static EditBox searchBox;
	private static Screen searchScreen;

	public static Component onGameMessage(Component message) {
		GhostblockerConfig c = GhostblockerConfig.get();
		String text = ChatFormatting.stripFormatting(message.getString());
		if (text == null) return message;

		Matcher m = STASH_MESSAGE.matcher(text);
		if (m.find() && c.stashChooseButton) {
			String kind = m.group(2).startsWith("material") ? "material" : "item";
			return message.copy().append(chooseButton(kind));
		}
		if (ALMOST_FULL.matcher(text).find() && c.stashFullAlert) {
			Minecraft mc = Minecraft.getInstance();
			mc.gui.hud.setTitle(Component.literal("Stash almost full!").withStyle(ChatFormatting.RED));
			mc.gui.hud.setSubtitle(Component.literal("Use /viewstash to choose what to take"));
			if (mc.player != null) mc.player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 1.0f, 0.6f);
			return message.copy().append(chooseButton("item")).append(chooseButton("material"));
		}
		return message;
	}

	private static MutableComponent chooseButton(String kind) {
		String command = "/viewstash " + kind;
		return Component.literal(" [Choose " + kind + "s]").withStyle(Style.EMPTY
				.withColor(ChatFormatting.AQUA)
				.withBold(true)
				.withClickEvent(new ClickEvent.RunCommand(command))
				.withHoverEvent(new HoverEvent.ShowText(Component.literal("Open the stash menu and pick only what you want\n(" + command + ")"))));
	}

	public static boolean isStashScreen(Screen screen) {
		return screen instanceof AbstractContainerScreen<?>
				&& screen.getTitle().getString().toLowerCase(Locale.ROOT).contains("stash");
	}

	public static void onScreenInit(Screen screen) {
		searchBox = null;
		searchScreen = null;
		if (!GhostblockerConfig.get().stashSearch || !isStashScreen(screen)) return;

		AbstractContainerScreenAccessor gui = (AbstractContainerScreenAccessor) screen;
		Minecraft mc = Minecraft.getInstance();
		EditBox box = new EditBox(mc.font, gui.ghostblocker$getLeftPos(), gui.ghostblocker$getTopPos() - 22,
				gui.ghostblocker$getImageWidth(), 18, Component.literal("Search stash"));
		box.setHint(Component.literal("Search stash, then click the items you want").withStyle(ChatFormatting.DARK_GRAY));
		net.fabricmc.fabric.api.client.screen.v1.Screens.getWidgets(screen).add(box);
		searchBox = box;
		searchScreen = screen;
	}

	/** True while the user is typing in the stash search box, so container hotkeys must be ignored. */
	public static boolean isTyping(Screen screen) {
		return searchBox != null && searchScreen == screen && searchBox.isFocused();
	}

	public static EditBox searchBox() {
		return searchBox;
	}

	/** Overlay colour for a slot in the stash menu, or 0 for none. */
	public static int highlight(Screen screen, Slot slot) {
		if (searchBox == null || searchScreen != screen || slot.container instanceof net.minecraft.world.entity.player.Inventory) return 0;
		String query = searchBox.getValue().trim().toLowerCase(Locale.ROOT);
		if (query.isEmpty() || slot.getItem().isEmpty()) return 0;
		return matches(slot.getItem(), query) ? GhostblockerConfig.get().highlightPrimary : 0xB0101010;
	}

	private static boolean matches(ItemStack stack, String query) {
		if (stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(query)) return true;
		ItemLore lore = stack.get(DataComponents.LORE);
		if (lore == null) return false;
		for (Component line : lore.lines()) {
			if (line.getString().toLowerCase(Locale.ROOT).contains(query)) return true;
		}
		return false;
	}

	private StashPicker() {
	}
}
