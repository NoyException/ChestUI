package cn.noy.cui.prebuilt.cui;

import cn.noy.cui.layer.Layer;
import cn.noy.cui.slot.SlotHandler;
import cn.noy.cui.ui.*;

import org.bukkit.Material;

@DefaultCamera(rowSize = 6)
@CUITitle("CUI Monitor")
public class CUIMonitor implements CUIHandler<CUIMonitor> {
	private static ChestUI<CUIMonitor> INSTANCE;
	public static ChestUI<CUIMonitor> getInstance() {
		if (INSTANCE == null) {
			INSTANCE = CUIManager.getInstance().createCUI(CUIMonitor.class);
		}
		return INSTANCE;
	}
	private ChestUI<CUIMonitor> cui;
	private Layer displayCUIs;
	private int size;

	@Override
	public void onInitialize(ChestUI<CUIMonitor> cui) {
		this.displayCUIs = new Layer(5, 9);
		this.cui = cui.edit().setLayer(1, displayCUIs).finish();
		this.cui.getDefaultCamera().edit().setMask(1, new Layer(1, 9).edit()
				.editAll(slotHandler -> slotHandler.button(
						builder -> builder.material(Material.BLACK_STAINED_GLASS_PANE).displayName(" ").build()))
				.editSlot(0, 0, slotHandler -> slotHandler.button(builder -> builder
						.material(Material.RED_STAINED_GLASS_PANE).displayName("Previous").clickHandler(event -> {
							var camera = Camera.Manager.getCamera(event.getPlayer());
							var position = camera.getPosition();
							if (position.row() <= 0)
								return;
							camera.edit().move(-5, 0);
						}).build()))
				.editSlot(0, 8, slotHandler -> slotHandler.button(builder -> builder
						.material(Material.GREEN_STAINED_GLASS_PANE).displayName("Next").clickHandler(event -> {
							var camera = Camera.Manager.getCamera(event.getPlayer());
							var position = camera.getPosition();
							if ((position.row() / 5 + 1) * 45 >= size)
								return;
							camera.edit().move(5, 0);
						}).build()))
				.finish());
	}

	@Override
	public void onTick() {
		var cuis = CUIManager.getInstance().getCUIs();
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
				if (target.getHandlerClass() == CUIMonitor.class) {
					displayCUIs.edit()
							.editSlot(row, column,
									slotHandler -> slotHandler.button(builder -> builder.material(Material.BARRIER)
											.displayName(target.getDefaultTitle())
											.lore("&7A CUI Monitor like" + " this", "&cCannot be monitored").build()));
				} else {
					displayCUIs.edit().editSlot(row, column, slotHandler -> slotHandler.button(builder -> builder
							.material(Material.CHEST).displayName(target.getDefaultTitle())
							.lore(String.format("&b%d&r" + " camera(s)", target.getCameras().size()))
							.clickHandler(event -> target.getDefaultCamera().open(event.getPlayer(), true)).build()));
				}
			}
		}

		cui.edit().setLayer(1, displayCUIs).finish();
	}
}
