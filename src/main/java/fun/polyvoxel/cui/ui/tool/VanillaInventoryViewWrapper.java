package fun.polyvoxel.cui.ui.tool;

import fun.polyvoxel.cui.CUIPlugin;
import fun.polyvoxel.cui.ui.Viewable;
import fun.polyvoxel.cui.ui.source.DisplaySource;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryView;
import org.jetbrains.annotations.Nullable;

public class VanillaInventoryViewWrapper extends Viewable {
	private final InventoryView view;

	VanillaInventoryViewWrapper(CUIPlugin plugin, InventoryView view) {
		super(plugin);
		this.view = view;
	}

	public InventoryView getInventoryView() {
		return view;
	}

	@Override
	protected void doOpen(Player viewer, boolean asChild, @Nullable DisplaySource<?> source) {
	}

	@Override
	protected void doClose(Player viewer) {
		if (viewer.getOpenInventory() == view) {
			viewer.closeInventory(InventoryCloseEvent.Reason.PLUGIN);
		}
	}

	@Override
	public InventoryView keepOpening(Player viewer) {
		var view = viewer.getOpenInventory();
		if (view != this.view) {
			viewer.openInventory(this.view);
			return this.view;
		}
		return null;
	}
}
