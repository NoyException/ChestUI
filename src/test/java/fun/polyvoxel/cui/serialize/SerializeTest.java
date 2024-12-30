package fun.polyvoxel.cui.serialize;

import fun.polyvoxel.cui.CUIPlugin;
import fun.polyvoxel.cui.util.Position;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.ArrayList;

public class SerializeTest {
	private static CUIPlugin plugin;

	@BeforeAll
	public static void setup() {
		var server = MockBukkit.mock();
		plugin = MockBukkit.load(CUIPlugin.class);
	}

	@AfterAll
	public static void teardown() {
		MockBukkit.unmock();
	}

	@Test
	public void testSerialize() {
		var cuiData = new CUIData();
		cuiData.key = "test:ui1";
		cuiData.title = "测试";
		cuiData.singleton = true;
		var layerData = new LayerData();
		cuiData.layers.put(1, layerData);
		for (int i = 1; i < 9; i++) {
			var slotData = new SlotData();
			slotData.type = "button";
			slotData.material = Material.BLACK_STAINED_GLASS_PANE;
			slotData.displayName = "";
			layerData.slots.put(new Position(0, i), slotData);
		}
		var exitButton = new SlotData();
		exitButton.type = "button";
		exitButton.material = Material.RED_STAINED_GLASS_PANE;
		exitButton.displayName = "退出";
		var onClick = new SlotData.OnClick();
		onClick.action = "command";
		onClick.value = "/cui close @s top";
		exitButton.onClicks.put(ClickType.LEFT, new ArrayList<>() {
			{
				add(onClick);
			}
		});
		layerData.slots.put(new Position(0, 0), exitButton);

		Assertions.assertDoesNotThrow(() -> {
			String json = cuiData.toJson();
			Assertions.assertNotNull(json);
		});
	}

	@Test
	public void testDeserialize() {
		var json = """
				{
				  "key": "test:ui1",
				  "singleton": true,
				  "title": "测试",
				  "maxRow": 6,
				  "maxColumn": 9,
				  "layers": {
				    "1": {
				      "maxRow": 1,
				      "maxColumn": 9,
				      "marginTop": 0,
				      "marginLeft": 0,
				      "relative": false,
				      "slots": {
				        "(0, 0)": {
				          "type": "button",
				          "material": "RED_STAINED_GLASS_PANE",
				          "displayName": "退出",
				          "onClicks": {
				            "LEFT": [
				              {
				                "action": "command-op",
				                "value": "cui close @s top"
				              }
				            ]
				          }
				        },
				        "(0, 1)": {
				          "type": "button",
				          "material": "BLACK_STAINED_GLASS_PANE",
				          "displayName": ""
				        },
				        "(0, 2)": {
				          "type": "button",
				          "material": "BLACK_STAINED_GLASS_PANE",
				          "displayName": ""
				        },
				        "(0, 3)": {
				          "type": "button",
				          "material": "BLACK_STAINED_GLASS_PANE",
				          "displayName": ""
				        },
				        "(0, 4)": {
				          "type": "button",
				          "material": "BLACK_STAINED_GLASS_PANE",
				          "displayName": ""
				        },
				        "(0, 5)": {
				          "type": "button",
				          "material": "BLACK_STAINED_GLASS_PANE",
				          "displayName": ""
				        },
				        "(0, 6)": {
				          "type": "button",
				          "material": "BLACK_STAINED_GLASS_PANE",
				          "displayName": ""
				        },
				        "(0, 7)": {
				          "type": "button",
				          "material": "BLACK_STAINED_GLASS_PANE",
				          "displayName": ""
				        },
				        "(0, 8)": {
				          "type": "button",
				          "material": "BLACK_STAINED_GLASS_PANE",
				          "displayName": ""
				        }
				      }
				    }
				  }
				}""";
		Assertions.assertDoesNotThrow(() -> {
			var cuiData = CUIData.fromJson(json);
			Assertions.assertNotNull(cuiData);
			plugin.getCUIManager().registerCUI(cuiData, null);
		});
	}
}
