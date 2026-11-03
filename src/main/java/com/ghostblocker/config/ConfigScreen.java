package com.ghostblocker.config;

import com.ghostblocker.dungeon.routes.RouteDatabase.RouteSet;
import com.ghostblocker.dungeon.routes.RouteTracker;
import com.ghostblocker.feature.InputLocks;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** A simple two-column page of toggles. */
public class ConfigScreen extends Screen {
	private final Screen parent;
	private int row;

	public ConfigScreen(Screen parent) {
		super(Component.literal("Ghostblocker Settings"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		row = 0;
		GhostblockerConfig c = GhostblockerConfig.get();
		toggle("Stash: [Choose items] button", () -> c.stashChooseButton, v -> c.stashChooseButton = v);
		toggle("Stash: search & highlight", () -> c.stashSearch, v -> c.stashSearch = v);
		toggle("Stash: almost-full alert", () -> c.stashFullAlert, v -> c.stashFullAlert = v);
		toggle("Slot locking", () -> c.slotLocking, v -> c.slotLocking = v);
		toggle("Dungeons: terminal solver", () -> c.terminalSolver, v -> c.terminalSolver = v);
		toggle("Dungeons: Three Weirdos solver", () -> c.weirdosSolver, v -> c.weirdosSolver = v);
		toggle("Dungeons: starred mob glow", () -> c.starredMobGlow, v -> c.starredMobGlow = v);
		toggle("Dungeons: secret routes", () -> c.secretRoutes, v -> c.secretRoutes = v);
		toggle("Dungeons: show all secrets", () -> c.showAllSecrets, v -> c.showAllSecrets = v);
		cycle(() -> "Route set: " + c.routeSet.label, () -> {
			RouteSet[] sets = RouteSet.values();
			c.routeSet = sets[(c.routeSet.ordinal() + 1) % sets.length];
			RouteTracker.reload();
		});
		cycle(() -> "Route steps shown: " + c.visibleRouteSteps, () -> c.visibleRouteSteps = c.visibleRouteSteps % 5 + 1);
		toggle("Hotbar slot lock (now)", () -> InputLocks.hotbarLocked, v -> InputLocks.hotbarLocked = v);
		toggle("Camera lock (now)", () -> InputLocks.cameraLocked, v -> InputLocks.cameraLocked = v);

		addRenderableWidget(Button.builder(Component.literal("Unlock all slots"), b -> {
			c.lockedSlots.clear();
			GhostblockerConfig.save();
		}).bounds(width / 2 - 155, height - 52, 150, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
				.bounds(width / 2 + 5, height - 52, 150, 20).build());
	}

	private void toggle(String label, Supplier<Boolean> getter, Consumer<Boolean> setter) {
		int x = width / 2 - 155 + (row % 2) * 160;
		int y = 40 + (row / 2) * 24;
		row++;
		addRenderableWidget(Button.builder(label(label, getter.get()), button -> {
			setter.accept(!getter.get());
			button.setMessage(label(label, getter.get()));
			GhostblockerConfig.save();
		}).bounds(x, y, 150, 20).build());
	}

	private void cycle(Supplier<String> label, Runnable next) {
		int x = width / 2 - 155 + (row % 2) * 160;
		int y = 40 + (row / 2) * 24;
		row++;
		addRenderableWidget(Button.builder(Component.literal(label.get()), button -> {
			next.run();
			button.setMessage(Component.literal(label.get()));
			GhostblockerConfig.save();
		}).bounds(x, y, 150, 20).build());
	}

	private static Component label(String name, boolean on) {
		return Component.literal(name + ": " + (on ? "§aON" : "§cOFF"));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		graphics.text(font, title, width / 2 - font.width(title) / 2, 16, 0xFFFFFFFF);
	}

	@Override
	public void onClose() {
		GhostblockerConfig.save();
		minecraft.gui.setScreen(parent);
	}
}
