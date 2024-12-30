package fun.polyvoxel.cui.ui.tool;

import fun.polyvoxel.cui.CUIPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.view.AnvilView;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;
import java.util.function.Consumer;

@ApiStatus.Experimental
public class AnvilTextInput extends VanillaInventoryViewWrapper implements Listener {
	private final Player player;
	private final Consumer<String> onConfirm;
	private String initialText = "";
	private Component title = Component.text("Input Text Below");

	public AnvilTextInput(CUIPlugin plugin, Player player, Consumer<String> onConfirm) {
		super(plugin, createAnvilView(player));
		this.player = player;
		this.onConfirm = onConfirm;
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	private static AnvilView createAnvilView(Player player) {
		var view = (AnvilView) player.openAnvil(null, true);
		player.closeInventory(InventoryCloseEvent.Reason.PLUGIN);
		return view;
	}

	@Override
	public AnvilView getInventoryView() {
		return (AnvilView) super.getInventoryView();
	}

	public String getInitialText() {
		return initialText;
	}

	public void setInitialText(String initialText) {
		this.initialText = initialText;
	}

	public Component getTitle() {
		return title;
	}

	public void setTitle(Component title) {
		this.title = title;
	}

	@EventHandler
	public void onClick(InventoryClickEvent event) {
		var view = event.getView();
		if (view != this.getInventoryView()) {
			return;
		}
		if (event.getClickedInventory() != view.getTopInventory()) {
			return;
		}
		event.setCancelled(true);
		var slot = event.getSlot();
		if (slot == 2) {
			onConfirm.accept(this.getInventoryView().getRenameText());
			close(player);
		}
	}

	private ItemStack createPaper() {
		var itemStack = ItemStack.of(Material.PAPER);
		itemStack.editMeta(itemMeta -> {
			itemMeta.displayName(Component.text(initialText));
			itemMeta.lore(List.of(Component.text("Type your text in the rename box"),
					Component.text("and then take the result to confirm")));
		});
		return itemStack;
	}

	@Override
	protected void doClose(Player viewer) {
		InventoryClickEvent.getHandlerList().unregister(this);
		AnvilInventory anvilInventory = (AnvilInventory) getInventoryView().getTopInventory();
		anvilInventory.setFirstItem(null);
		super.doClose(viewer);
	}

	@Override
	public InventoryView keepOpening(Player viewer) {
		var view = super.keepOpening(viewer);
		if (view == null) {
			return null;
		}
		view.setTitle(LegacyComponentSerializer.legacySection().serialize(title));
		((AnvilInventory) view.getTopInventory()).setFirstItem(createPaper());
		this.getInventoryView().setMaximumRepairCost(0);
		this.getInventoryView().setRepairCost(0);
		return view;
	}
}
