package com.ghostblocker.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
	@Accessor("leftPos")
	int ghostblocker$getLeftPos();

	@Accessor("topPos")
	int ghostblocker$getTopPos();

	@Accessor("imageWidth")
	int ghostblocker$getImageWidth();

	@Accessor("hoveredSlot")
	Slot ghostblocker$getHoveredSlot();
}
