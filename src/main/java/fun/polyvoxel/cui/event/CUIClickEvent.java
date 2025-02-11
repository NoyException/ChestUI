package fun.polyvoxel.cui.event;

import fun.polyvoxel.cui.slot.Slot;
import fun.polyvoxel.cui.ui.ChestUI;
import fun.polyvoxel.cui.ui.Camera;
import fun.polyvoxel.cui.ui.CUIInstance;
import fun.polyvoxel.cui.util.Position;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CUIClickEvent<T extends ChestUI<T>> extends Event implements Cancellable {
	private static final HandlerList HANDLERS = new HandlerList();
	private final @NotNull Camera<T> camera;
	private final @NotNull Player player;
	private final @NotNull ClickType clickType;
	private final @NotNull InventoryAction action;
	// 相对于Camera的点击位置
	private final @NotNull Position position;
	private @Nullable ItemStack cursor;
	private @Nullable Slot slot;
	private boolean cancel;

	public CUIClickEvent(@NotNull Camera<T> camera, @NotNull Player player, @NotNull ClickType clickType,
			@NotNull InventoryAction action, @NotNull Position position, @Nullable ItemStack cursor) {
		this.camera = camera;
		this.player = player;
		this.clickType = clickType;
		this.action = action;
		this.position = position;
		this.cursor = cursor;
	}

	public CUIInstance<T> getCUIInstance() {
		return camera.getCUIInstance();
	}

	public @NotNull Camera<T> getCamera() {
		return camera;
	}

	public @NotNull Player getPlayer() {
		return player;
	}

	public @NotNull ClickType getClickType() {
		return clickType;
	}

	public @NotNull InventoryAction getAction() {
		return action;
	}

	public @NotNull Position getPosition() {
		return position;
	}

	public @Nullable ItemStack getCursor() {
		return cursor;
	}

	public void setCursor(@Nullable ItemStack cursor) {
		this.cursor = cursor;
	}

	public @Nullable Slot getSlot() {
		return slot;
	}

	public void setSlot(@Nullable Slot slot) {
		this.slot = slot;
	}

	@Override
	public boolean isCancelled() {
		return cancel;
	}

	@Override
	public void setCancelled(boolean cancel) {
		this.cancel = cancel;
	}

	@SuppressWarnings("unused")
	public static HandlerList getHandlerList() {
		return HANDLERS;
	}

	@Override
	public @NotNull HandlerList getHandlers() {
		return HANDLERS;
	}
}
