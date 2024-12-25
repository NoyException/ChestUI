package fun.polyvoxel.cui.prebuilt.cui;

import fun.polyvoxel.cui.layer.Layer;
import fun.polyvoxel.cui.slot.SlotHandler;

import fun.polyvoxel.cui.ui.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

@CUI(name = "monitor", singleton = true, icon = Material.BARRIER)
public class CUIMonitor implements ChestUI<CUIMonitor> {

	@Override
	public void onInitialize(CUIType<CUIMonitor> type) {
		type.edit().triggerByDisplayCommand(
				player -> new CUIType.TriggerResult<>(CUIType.TriggerResultType.CREATE_NEW_CAMERA, camera -> {
				}));
	}

	@Override
	public ChestUI.@NotNull Handler<CUIMonitor> createHandler() {
		return new Handler();
	}

	private static class Handler implements ChestUI.Handler<CUIMonitor> {
		private CUIInstance<CUIMonitor> cui;
		private Layer displayCUIs;
		private int size;

		@Override
		public void onInitialize(CUIInstance<CUIMonitor> cui) {
			this.displayCUIs = new Layer(5, 9);
			this.cui = cui.edit().setKeepAlive(true).setLayer(1, displayCUIs).setLayer(0, new Layer(1, 9).edit()
					.relative(true)
					.editAll((slotHandler, row, column) -> slotHandler.button(
							builder -> builder.material(Material.BLACK_STAINED_GLASS_PANE).displayName(" ").build()))
					.editSlot(0, 0, slotHandler -> slotHandler.button(builder -> builder
							.material(Material.RED_STAINED_GLASS_PANE).displayName("Previous").clickHandler(event -> {
								var camera = event.getCamera();
								var position = camera.getPosition();
								if (position.row() <= 0)
									return;
								camera.edit().move(-5, 0);
							}).build()))
					.editSlot(0, 8, slotHandler -> slotHandler.button(builder -> builder
							.material(Material.GREEN_STAINED_GLASS_PANE).displayName("Next").clickHandler(event -> {
								var camera = event.getCamera();
								var position = camera.getPosition();
								if ((position.row() / 5 + 1) * 45 >= size)
									return;
								camera.edit().move(5, 0);
							}).build()))
					.finish()).finish();
		}

		@Override
		public void onCreateCamera(Camera<CUIMonitor> camera) {
			camera.edit().setTitle(Component.text("CUI Monitor").color(NamedTextColor.GOLD)).setRowSize(6);
		}

		@Override
		public void onTick() {
			if (cui.getTicksLived() % 10 != 0) {
				return;
			}

			var cuis = cui.getPlugin().getCUIManager().getCUIs();
			size = cuis.size();
			var maxRow = (size - 1) / 9 + 1;
			displayCUIs = new Layer(maxRow, 9).edit().marginTop(1).finish();

			for (int row = 0; row < maxRow; row++) {
				for (int column = 0; column < 9; column++) {
					int index = row * 9 + column;
					if (index >= size) {
						displayCUIs.edit().editSlot(row, column, SlotHandler::empty);
						continue;
					}

					var target = cuis.get(index);
					if (target.getType().getChestUI().getClass() == CUIMonitor.class) {
						displayCUIs.edit().editSlot(row, column,
								slotHandler -> slotHandler.button(builder -> builder.material(Material.BARRIER)
										.displayName(target.getDefaultTitle())
										.lore("&7A CUI Monitor like" + " this", "&cCannot be monitored").build()));
					} else {
						displayCUIs.edit().editSlot(row, column,
								slotHandler -> slotHandler.button(builder -> builder
										.material(target.getType().getIcon()).displayName(target.getDefaultTitle())
										.lore("name: &b" + target.getName(),
												String.format("&b%d&r" + " camera(s)", target.getCameraCount()))
										.clickHandler(event -> {
											if (target.getState() == CUIInstance.State.READY) {
												target.createCamera().open(event.getPlayer(), true);
											}
										}).build()));
					}
				}
			}

			cui.edit().setLayer(1, displayCUIs).finish();
		}
	}
}
