package cn.noy.cui.prebuilt.cui;

import cn.noy.cui.layer.Layer;
import cn.noy.cui.ui.*;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;

@DefaultCamera(rowSize = 6)
@CUITitle("Inventory Monitor")
public class InventoryMonitor implements CUIHandler {
	private ChestUI<InventoryMonitor> cui;
	private Layer displayPlayers;
	private Layer monitorInventory;
	private int page = 0;

	@Override
	public void onInitialize(ChestUI<?> cui) {
		this.displayPlayers = new Layer(5, 9).edit().marginTop(1).finish();
		this.monitorInventory = new Layer(5, 9).edit().marginTop(1)
				.editRow(3, slotHandler -> slotHandler.button(
						builder -> builder.material(Material.WHITE_STAINED_GLASS_PANE).displayName(" ").build()))
				.finish();
		this.cui = cui.edit().setLayer(0, new Layer(1, 9).edit()
				.editAll(slotHandler -> slotHandler.button(
						builder -> builder.material(Material.BLACK_STAINED_GLASS_PANE).displayName(" ").build()))
				.editSlot(0, 0, slotHandler -> slotHandler.button(builder -> builder
						.material(Material.RED_STAINED_GLASS_PANE).displayName("Previous").clickHandler(event -> {
							if (page > 0) {
								page--;
							}
						}).build()))
				.editSlot(0, 8, slotHandler -> slotHandler.button(builder -> builder
						.material(Material.GREEN_STAINED_GLASS_PANE).displayName("Next").clickHandler(event -> {
							var players = Bukkit.getOnlinePlayers();
							if (players.size() > (page + 1) * 45) {
								page++;
							}
						}).build()))
				.finish()).setLayer(1, displayPlayers).setLayer(3, monitorInventory).setActive(monitorInventory, false)
				.finish();
	}

	private void updatePlayers() {
		if (!cui.isActive(displayPlayers)) {
			return;
		}

		var players = Bukkit.getOnlinePlayers().stream().sorted((a, b) -> {
			var aName = a.getName();
			var bName = b.getName();
			return aName.compareTo(bName);
		}).toList();
		int size = players.size();
		int maxPage = (size - 1) / 45 + 1;
		if (page >= maxPage) {
			page = maxPage - 1;
		}
		for (int row = 0; row < 5; row++) {
			for (int col = 0; col < 9; col++) {
				var index = page * 45 + row * 9 + col;
				if (index >= size) {
					return;
				}
				var player = players.get(index);
				displayPlayers.edit().editSlot(row, col,
						slotHandler -> slotHandler.button(builder -> builder.skull(player)
								.displayName(player.displayName()).clickHandler(event -> monitor(player)).build()));
			}
		}
	}

	private void monitor(Player player) {
		PlayerInventory inventory = player.getInventory();
		for (int i = 0; i < 9; i++) {
			// hotbar, 0~8
			int finalI = i;
			monitorInventory.edit().editSlot(4, i,
					slotHandler -> slotHandler.storage(builder -> builder.source(inventory, finalI).build()));
		}
		for (int i = 0; i < 27; i++) {
			// main inventory, 9~35
			int finalI = i;
			monitorInventory.edit().editSlot(i / 9, i % 9,
					slotHandler -> slotHandler.storage(builder -> builder.source(inventory, finalI + 9).build()));
		}

		cui.edit().setActive(0, false).setActive(displayPlayers, false).setActive(monitorInventory, true).finish();
	}

	@Override
	public void onTick() {
		updatePlayers();
	}
}
