package fun.polyvoxel.cui.prebuilt;

import fun.polyvoxel.cui.layer.Layer;

import fun.polyvoxel.cui.slot.Button;
import fun.polyvoxel.cui.slot.Storage;
import fun.polyvoxel.cui.ui.*;
import fun.polyvoxel.cui.util.Context;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@CUI(name = "im", singleton = true, icon = Material.PLAYER_HEAD)
public class InventoryMonitor implements ChestUI<InventoryMonitor> {
	@Override
	public void onInitialize(CUIType<InventoryMonitor> type) {
		type.edit().defaultTitle("Inventory Monitor").triggerByDisplayCommand(
				player -> new CUIType.TriggerResult<>(CUIType.TriggerResultType.CREATE_NEW_CAMERA, camera -> {
				}));
	}

	@Override
	public @NotNull CUIInstanceHandler<InventoryMonitor> createCUIInstanceHandler(Context context) {
		return new InstanceHandler();
	}

	private static class InstanceHandler implements CUIInstanceHandler<InventoryMonitor> {
		private CUIInstance<InventoryMonitor> cui;
		private Layer displayPlayers;
		private int page = 0;

		@Override
		public void onInitialize(CUIInstance<InventoryMonitor> cui) {
			this.displayPlayers = new Layer(5, 9).edit().marginTop(1).done();
			this.cui = cui.edit()
					.layer(0,
							new Layer(1, 9).edit()
									.all((row, column) -> Button.builder().material(Material.BLACK_STAINED_GLASS_PANE)
											.displayName(" ").build())
									.slot(0, 0, () -> Button.builder().material(Material.RED_STAINED_GLASS_PANE)
											.displayName("Previous").clickHandler(event -> {
												if (page > 0) {
													page--;
												}
											}).build())
									.slot(0, 8, () -> Button.builder().material(Material.GREEN_STAINED_GLASS_PANE)
											.displayName("Next").clickHandler(event -> {
												var players = Bukkit.getOnlinePlayers();
												if (players.size() > (page + 1) * 45) {
													page++;
												}
											}).build())
									.done())
					.layer(1, displayPlayers).done();
		}

		@Override
		public @NotNull fun.polyvoxel.cui.ui.CameraHandler<InventoryMonitor> createCameraHandler(Context context) {
			return new CameraHandler();
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
					displayPlayers.edit().slot(row, col,
							() -> Button.builder().skull(player).displayName(player.displayName())
									.clickHandler(event -> monitor(event.getCamera(), player)).build());
				}
			}
		}

		private void monitor(Camera<?> camera, Player player) {
			var inventory = player.getInventory();
			camera.edit()
					.layer(-2,
							new Layer(1, 9).edit()
									.all((row, column) -> Button.builder().material(Material.BLACK_STAINED_GLASS_PANE)
											.displayName(" ").build())
									.done())
					.layer(-1,
							new Layer(5, 9).edit().marginTop(1)
									.row(3, column -> Button.builder().material(Material.WHITE_STAINED_GLASS_PANE)
											.displayName(" ").build())
									.row(4, column -> Storage.builder().fromInventory(inventory, column).build())
									.all((row, column) -> {
										int index = row * 9 + column;
										if (index < 27) {
											return Storage.builder().fromInventory(inventory, index + 9).build();
										}
										return null;
									}).done());
		}

		@Override
		public void onTick() {
			updatePlayers();
		}

		private static class CameraHandler implements fun.polyvoxel.cui.ui.CameraHandler<InventoryMonitor> {
			@Override
			public void onInitialize(Camera<InventoryMonitor> camera) {
				camera.edit().rowSize(6);
			}
		}
	}
}
