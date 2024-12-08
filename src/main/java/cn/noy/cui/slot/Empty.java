package cn.noy.cui.slot;

import cn.noy.cui.event.CUIClickEvent;
import org.bukkit.inventory.ItemStack;

public class Empty extends Slot {
	private static final Empty INSTANCE = new Empty();

	private Empty() {
	}

	public static Empty getInstance() {
		return INSTANCE;
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public ItemStack display(ItemStack legacy) {
		return legacy;
	}

	@Override
	public void click(CUIClickEvent<?> event) {
	}

	@Override
	public ItemStack place(ItemStack itemStack) {
		return itemStack;
	}

	@Override
	public ItemStack collect(ItemStack itemStack) {
		return itemStack;
	}

	@Override
	public Slot deepClone() {
		return INSTANCE;
	}
}
