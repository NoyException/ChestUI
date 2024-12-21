package cn.noy.cui.prebuilt.cui;

import cn.noy.cui.event.CUIClickEvent;
import cn.noy.cui.layer.Layer;
import cn.noy.cui.slot.SlotHandler;
import cn.noy.cui.ui.*;
import cn.noy.cui.util.Position;
import com.google.common.collect.HashMultimap;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;

@DefaultCamera(rowSize = 6)
@CUITitle("CUI Creator")
@CUI(name = "creator")
public class CUICreator implements CUIHandler<CUICreator> {
	private ChestUI<CUICreator> cui;

	@Override
	public void onInitialize(ChestUI<CUICreator> cui) {
	}

	private static class JsonCUI implements CUIHandler<JsonCUI> {
		private ChestUI<JsonCUI> cui;
		private JsonObject jsonObject;

		public void setJson() {

		}

		@Override
		public void onInitialize(ChestUI<JsonCUI> cui) {
		}
	}
}
