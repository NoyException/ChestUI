package cn.noy.cui.prebuilt.cui;

import cn.noy.cui.layer.Layer;
import cn.noy.cui.ui.CUIHandler;
import cn.noy.cui.ui.CUITitle;
import cn.noy.cui.ui.ChestUI;
import cn.noy.cui.ui.DefaultCamera;

import org.bukkit.Material;

@DefaultCamera(rowSize = 6)
@CUITitle("Test")
public class TestCUI implements CUIHandler<TestCUI> {

	@Override
	public void onInitialize(ChestUI<TestCUI> cui) {
		cui.edit().setKeepAlive(true).setLayer(0, new Layer(1, 9).edit()
				.editAll(slotHandler -> slotHandler.button(
						builder -> builder.material(Material.BLACK_STAINED_GLASS_PANE).displayName(" ").build()))
				.editSlot(0, 0, slotHandler -> slotHandler.button(builder -> builder
						.material(Material.RED_STAINED_GLASS_PANE).displayName("Previous").clickHandler(event -> {
						}).build()))
				.editSlot(0, 8, slotHandler -> slotHandler.button(builder -> builder
						.material(Material.GREEN_STAINED_GLASS_PANE).displayName("Next").clickHandler(event -> {
						}).build()))
				.finish()).finish();
	}
}
