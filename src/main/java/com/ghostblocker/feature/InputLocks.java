package com.ghostblocker.feature;

/** Session-only locks toggled by keybind. Enforced in InventoryMixin and EntityMixin. */
public final class InputLocks {
	/** Scroll wheel and number keys can't change the selected hotbar slot. */
	public static boolean hotbarLocked;
	/** Mouse movement doesn't turn the camera. */
	public static boolean cameraLocked;
	/** Set while the server is changing our held slot, which must always be allowed. */
	public static boolean serverSettingSlot;

	private InputLocks() {
	}
}
