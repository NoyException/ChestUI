package fun.polyvoxel.cui.ui.tool;

import fun.polyvoxel.cui.CUIPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Consumer;

public class Tools implements Listener {
	private final CUIPlugin plugin;

	public Tools(CUIPlugin plugin) {
		this.plugin = plugin;
	}

	public VanillaInventoryWrapper wrapInventory(Inventory inventory) {
		return new VanillaInventoryWrapper(plugin, inventory);
	}

	public VanillaInventoryViewWrapper wrapInventoryView(InventoryView view) {
		return new VanillaInventoryViewWrapper(plugin, view);
	}

	@ApiStatus.Experimental
	public AnvilTextInput createAnvilTextInput(Player player, Consumer<String> onConfirm) {
		return new AnvilTextInput(plugin, player, onConfirm);
	}
}
