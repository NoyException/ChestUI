package fun.polyvoxel.cui.ui.source;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ItemStackDisplaySource implements DisplaySource<ItemStack> {
	private final ItemStack itemStack;
	private final Player holder;

	public ItemStackDisplaySource(ItemStack itemStack, Player holder) {
		this.itemStack = itemStack;
		this.holder = holder;
	}

	@Override
	public ItemStack getSource() {
		return itemStack;
	}

	public Player getHolder() {
		return holder;
	}
}
