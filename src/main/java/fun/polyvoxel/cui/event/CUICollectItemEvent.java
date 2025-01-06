package fun.polyvoxel.cui.event;

import fun.polyvoxel.cui.ui.CUIInstance;
import fun.polyvoxel.cui.ui.Camera;
import fun.polyvoxel.cui.ui.ChestUI;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class CUICollectItemEvent<T extends ChestUI<T>> extends Event implements Cancellable {
	private static final HandlerList HANDLERS = new HandlerList();
	private final Camera<T> camera;
	private final Player player;
	private ItemStack cursor;
	private boolean includeBackpack;
	private boolean cancel;

	public CUICollectItemEvent(Camera<T> camera, Player player, ItemStack cursor, boolean includeBackpack) {
		this.camera = camera;
		this.player = player;
		this.cursor = cursor;
		this.includeBackpack = includeBackpack;
	}

	public CUIInstance<T> getCUIInstance() {
		return camera.getCUIInstance();
	}

	public Camera<T> getCamera() {
		return camera;
	}

	public Player getPlayer() {
		return player;
	}

	public ItemStack getCursor() {
		return cursor;
	}

	public void setCursor(ItemStack cursor) {
		this.cursor = cursor;
	}

	public boolean isIncludeBackpack() {
		return includeBackpack;
	}

	public void setIncludeBackpack(boolean includeBackpack) {
		this.includeBackpack = includeBackpack;
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
