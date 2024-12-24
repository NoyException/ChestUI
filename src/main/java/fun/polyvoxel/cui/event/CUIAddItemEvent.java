package fun.polyvoxel.cui.event;

import fun.polyvoxel.cui.ui.*;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class CUIAddItemEvent<T extends ChestUI<T>> extends Event implements Cancellable {
	private static final HandlerList HANDLERS = new HandlerList();
	private final Camera<T> camera;
	private final Player player;
	private ItemStack itemStack;
	private boolean cancel;

	public CUIAddItemEvent(Camera<T> camera, Player player, ItemStack itemStack) {
		this.camera = camera;
		this.player = player;
		this.itemStack = itemStack;
	}

	public CUIInstance<T> getChestUI() {
		return camera.getCUIInstance();
	}

	public Camera<T> getCamera() {
		return camera;
	}

	public Player getPlayer() {
		return player;
	}

	public ItemStack getItemStack() {
		return itemStack;
	}

	public void setItemStack(ItemStack itemStack) {
		this.itemStack = itemStack;
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
