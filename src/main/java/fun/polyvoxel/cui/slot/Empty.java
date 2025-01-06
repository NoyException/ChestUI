package fun.polyvoxel.cui.slot;

import fun.polyvoxel.cui.event.CUIDropAllEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class Empty extends Slot {
	private static final Empty INSTANCE = new Empty();

	private Empty() {
	}

	public static Empty getInstance() {
		return INSTANCE;
	}

	@Override
	public ItemStack display(@Nullable ItemStack legacy) {
		return legacy;
	}

	@Override
	public void prepareDrop(CUIDropAllEvent<?> event) {
	}

	@Override
	public Slot deepClone() {
		return INSTANCE;
	}
}
