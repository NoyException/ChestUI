package fun.polyvoxel.cui.ui.tool;

import fun.polyvoxel.cui.CUIPlugin;
import fun.polyvoxel.cui.ui.Viewable;
import fun.polyvoxel.cui.ui.source.DisplaySource;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.jetbrains.annotations.Nullable;

public class VanillaInventoryWrapper extends Viewable {
	private final Inventory inventory;

	VanillaInventoryWrapper(CUIPlugin plugin, Inventory inventory) {
		super(plugin);
		this.inventory = inventory;
	}

	public Inventory getInventory() {
		return inventory;
	}

	@Override
	protected void doOpen(Player viewer, boolean asChild, @Nullable DisplaySource<?> source) {
	}

	@Override
	protected void doClose(Player viewer) {
		if (viewer.getOpenInventory().getTopInventory() == inventory) {
			viewer.closeInventory(InventoryCloseEvent.Reason.PLUGIN);
		}
	}

	@Override
	public InventoryView keepOpening(Player viewer) {
		var view = viewer.getOpenInventory();
		if (view.getTopInventory() != inventory) {
			return viewer.openInventory(inventory);
		}
		return null;
	}
}
