package cn.noy.cui.event;

import cn.noy.cui.ui.CUIHandler;
import cn.noy.cui.ui.ChestUI;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.inventory.ItemStack;

public class CUIAddItemEvent<T extends CUIHandler<T>> extends CUIEvent<T> implements Cancellable {
	private final Player player;
	private ItemStack itemStack;
	private boolean cancel;

	public CUIAddItemEvent(ChestUI<T> cui, Player player, ItemStack itemStack) {
		super(cui);
		this.player = player;
		this.itemStack = itemStack;
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
}
