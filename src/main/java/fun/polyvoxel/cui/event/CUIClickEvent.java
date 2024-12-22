package fun.polyvoxel.cui.event;

import fun.polyvoxel.cui.ui.CUIHandler;
import fun.polyvoxel.cui.ui.Camera;
import fun.polyvoxel.cui.ui.ChestUI;
import fun.polyvoxel.cui.util.Position;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class CUIClickEvent<T extends CUIHandler<T>> extends Event implements Cancellable {
	private static final HandlerList HANDLERS = new HandlerList();
	private final Camera<T> camera;
	private final Player player;
	private final ClickType clickType;
	private final InventoryAction action;
	// 相对于ChestUI的点击位置
	private final Position position;
	private ItemStack cursor;
	private boolean cancel;

	public CUIClickEvent(Camera<T> camera, Player player, ClickType clickType, InventoryAction action,
			Position position, ItemStack cursor) {
		this.camera = camera;
		this.player = player;
		this.clickType = clickType;
		this.action = action;
		this.position = position;
		this.cursor = cursor;
	}

	public ChestUI<T> getChestUI() {
		return camera.getChestUI();
	}

	public Camera<T> getCamera() {
		return camera;
	}

	public Player getPlayer() {
		return player;
	}

	public ClickType getClickType() {
		return clickType;
	}

	public InventoryAction getAction() {
		return action;
	}

	public Position getPosition() {
		return position;
	}

	public ItemStack getCursor() {
		return cursor;
	}

	public void setCursor(ItemStack cursor) {
		this.cursor = cursor;
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
