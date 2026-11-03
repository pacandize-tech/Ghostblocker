package com.ghostblocker;

import com.mojang.blaze3d.platform.InputConstants;
import com.ghostblocker.config.ConfigScreen;
import com.ghostblocker.config.GhostblockerConfig;
import com.ghostblocker.dungeon.StarredMobs;
import com.ghostblocker.dungeon.WeirdosSolver;
import com.ghostblocker.dungeon.rooms.RoomScanner;
import com.ghostblocker.dungeon.routes.RouteDatabase;
import com.ghostblocker.dungeon.routes.RouteTracker;
import com.ghostblocker.dungeon.routes.WaypointRenderer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import com.ghostblocker.feature.InputLocks;
import com.ghostblocker.feature.StashPicker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Ghostblocker implements ClientModInitializer {
	public static final String MOD_ID = "ghostblocker";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));

	public static final KeyMapping CONFIG_KEY = key("config", InputConstants.KEY_RSHIFT);
	/** Works inside inventories: hover a slot and press it. */
	public static final KeyMapping LOCK_SLOT_KEY = key("lock_slot", InputConstants.KEY_L);
	public static final KeyMapping HOTBAR_LOCK_KEY = key("hotbar_lock", InputConstants.KEY_K);
	public static final KeyMapping CAMERA_LOCK_KEY = key("camera_lock", InputConstants.KEY_J);
	public static final KeyMapping NEXT_STEP_KEY = key("next_step", InputConstants.KEY_RBRACKET);
	public static final KeyMapping PREVIOUS_STEP_KEY = key("previous_step", InputConstants.KEY_LBRACKET);
	public static final KeyMapping CYCLE_ROUTE_KEY = key("cycle_route", InputConstants.KEY_BACKSLASH);

	private static boolean openConfigNextTick;

	private static KeyMapping key(String name, int defaultKey) {
		return KeyMappingHelper.registerKeyMapping(
				new KeyMapping("key.ghostblocker." + name, InputConstants.Type.KEYBOARD, defaultKey, CATEGORY));
	}

	@Override
	public void onInitializeClient() {
		GhostblockerConfig.load();
		RoomScanner.load();
		RouteDatabase.loadSecrets();
		WaypointRenderer.register();
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			RoomScanner.reset();
			RouteTracker.onRoomChanged(null);
		});

		ClientTickEvents.END_CLIENT_TICK.register(Ghostblocker::onTick);
		ClientReceiveMessageEvents.MODIFY_GAME.register((message, overlay) -> overlay ? message : StashPicker.onGameMessage(message));
		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			if (!overlay) WeirdosSolver.onGameMessage(message.getString());
		});
		ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> StashPicker.onScreenInit(screen));

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) ->
				dispatcher.register(ClientCommands.literal("ghostblocker").executes(ctx -> {
					openConfigNextTick = true; // the chat screen closes after the command runs
					return 1;
				})));

		LOGGER.info("Ghostblocker loaded");
	}

	private static void onTick(Minecraft client) {
		if (openConfigNextTick) {
			openConfigNextTick = false;
			client.gui.setScreen(new ConfigScreen(null));
		}
		while (CONFIG_KEY.consumeClick()) client.gui.setScreen(new ConfigScreen(null));
		while (HOTBAR_LOCK_KEY.consumeClick()) {
			InputLocks.hotbarLocked = !InputLocks.hotbarLocked;
			notify(client, "Hotbar slot lock", InputLocks.hotbarLocked);
		}
		while (CAMERA_LOCK_KEY.consumeClick()) {
			InputLocks.cameraLocked = !InputLocks.cameraLocked;
			notify(client, "Camera lock", InputLocks.cameraLocked);
		}
		while (NEXT_STEP_KEY.consumeClick()) RouteTracker.nextStep();
		while (PREVIOUS_STEP_KEY.consumeClick()) RouteTracker.previousStep();
		while (CYCLE_ROUTE_KEY.consumeClick()) RouteTracker.cycleVariant();
		StarredMobs.tick(client);
		RoomScanner.tick(client);
	}

	private static void notify(Minecraft client, String what, boolean on) {
		if (client.player != null) {
			client.player.sendOverlayMessage(Component.literal(what + ": " + (on ? "§aON" : "§cOFF")));
		}
	}
}
