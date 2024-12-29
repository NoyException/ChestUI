package fun.polyvoxel.cui.ui;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class VanillaInventoryWrapper implements Viewable {
	private final CameraManager manager;
	private final Inventory inventory;

	VanillaInventoryWrapper(CameraManager manager, Inventory inventory) {
		this.manager = manager;
		this.inventory = inventory;
	}

	@Override
	public boolean open(Player viewer, boolean asChild) {
		if (!asChild) {
			boolean success = manager.closeAll(viewer, false);
			if (!success) {
				return false;
			}
		}
		var stack = manager.getViewingStack(viewer);
		if (!stack.empty()) {
			var parent = stack.peek();
			if (parent != null) {
				parent.notifySwitchOut(viewer);
			}
		}
		stack.push(this);

		viewer.openInventory(inventory);
		return true;
	}

	@Override
	public boolean close(Player viewer, boolean force) {
		var stack = manager.getViewingStack(viewer);
		if (stack == null || stack.empty()) {
			return false;
		}
		var popped = stack.pop();
		if (popped != this) {
			throw new RuntimeException("Inventory not match");
		}
		if (!stack.empty()) {
			stack.peek().notifySwitchBack(viewer);
		}

		if (viewer.getOpenInventory().getTopInventory() == inventory) {
			viewer.closeInventory();
		}
		return true;
	}

	@Override
	public void notifySwitchOut(Player viewer) {
	}

	@Override
	public void notifySwitchBack(Player viewer) {
	}

	@Override
	public void checkOpen(Player viewer) {
		if (viewer.getOpenInventory().getTopInventory() != inventory) {
			viewer.openInventory(inventory);
		}
	}
}
