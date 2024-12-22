package fun.polyvoxel.cui.serialize;

import fun.polyvoxel.cui.event.CUIClickEvent;
import fun.polyvoxel.cui.slot.SlotHandler;
import fun.polyvoxel.cui.util.ItemStacks;
import com.google.gson.*;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.*;

public class SlotData {
	public String type;
	public Material material;
	public String displayName;
	public String[] lore;
	public int amount = 1;
	public final HashMap<ClickType, ArrayList<OnClick>> onClicks = new HashMap<>();

	public void editSlot(SlotHandler slotHandler) {
		var itemStack = getItemStack();
		switch (type) {
			case "empty" -> slotHandler.empty();
			case "button" -> slotHandler.button(builder -> builder.itemStack(itemStack).clickHandler(event -> {
				var actions = onClicks.get(event.getClickType());
				if (actions == null) {
					return;
				}
				for (OnClick onClick : actions) {
					onClick.onClick(event);
				}
			}).build());
			case "storage" -> {
				if (material == null) {
					slotHandler.storage(builder -> builder.source().build());
				} else {
					slotHandler.storage(builder -> builder.source(itemStack).build());
				}
			}
		}
	}

	private @Nullable ItemStack getItemStack() {
		ItemStack itemStack;
		if (material == null) {
			itemStack = null;
		} else {
			itemStack = new ItemStack(material, amount);
			itemStack.editMeta(itemMeta -> {
				if (displayName != null) {
					TextComponent component = LegacyComponentSerializer.legacyAmpersand().deserialize(displayName);
					itemMeta.displayName(ItemStacks.cleanComponent(component));
				}
				if (lore != null) {
					itemMeta.lore(Arrays.stream(lore).map(LegacyComponentSerializer.legacyAmpersand()::deserialize)
							.map(ItemStacks::cleanComponent).toList());
				}
			});
		}
		return itemStack;
	}

	public static class JsonAdapter implements JsonSerializer<SlotData>, JsonDeserializer<SlotData> {

		@Override
		public JsonElement serialize(SlotData src, Type typeOfSrc, JsonSerializationContext context) {
			var obj = new JsonObject();
			obj.addProperty("type", src.type);
			if (src.material != null) {
				obj.addProperty("material", src.material.name());
			}
			if (src.displayName != null) {
				obj.addProperty("displayName", src.displayName);
			}
			if (src.lore != null) {
				obj.add("lore", context.serialize(src.lore));
			}
			if (src.amount != 1) {
				obj.addProperty("amount", src.amount);
			}
			if (!src.onClicks.isEmpty()) {
				var onClicks = new JsonObject();
				src.onClicks.forEach((clickType, actions) -> {
					if (actions.isEmpty())
						return;
					var actionsObj = new JsonArray();
					actions.forEach(action -> actionsObj.add(context.serialize(action)));
					onClicks.add(clickType.name(), actionsObj);
				});
				obj.add("onClicks", onClicks);
			}
			return obj;
		}

		@Override
		public SlotData deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
				throws JsonParseException {
			var obj = json.getAsJsonObject();
			var slotData = new SlotData();
			slotData.type = obj.get("type").getAsString();
			slotData.material = obj.has("material") ? Material.valueOf(obj.get("material").getAsString()) : null;
			slotData.displayName = obj.has("displayName") ? obj.get("displayName").getAsString() : null;
			slotData.lore = obj.has("lore") ? context.deserialize(obj.get("lore"), String[].class) : null;
			slotData.amount = obj.has("amount") ? obj.get("amount").getAsInt() : 1;
			var onClicks = obj.getAsJsonObject("onClicks");
			if (onClicks != null) {
				onClicks.entrySet().forEach(entry -> {
					var clickType = ClickType.valueOf(entry.getKey());
					var actions = new ArrayList<OnClick>();
					entry.getValue().getAsJsonArray()
							.forEach(action -> actions.add(context.deserialize(action, OnClick.class)));
					slotData.onClicks.put(clickType, actions);
				});
			}
			return slotData;
		}
	}

	public static class OnClick {
		public String action;
		public String value;

		public void onClick(CUIClickEvent<?> event) {
			switch (action) {
				case "command" -> event.getPlayer().performCommand(value);
				case "command-op" -> {
					// TODO: 如果官方更新了更好的执行方法，应当更换
					String playerName = event.getPlayer().getName();
					var cmd = value.replaceAll("@s", playerName);
					cmd = cmd.replaceAll("@p", playerName);
					cmd = "execute as " + playerName + " at @s run " + cmd;
					var sender = Bukkit.getServer().getConsoleSender();
					Bukkit.getServer().dispatchCommand(sender, cmd);
				}
			}
		}

		public static class JsonAdapter implements JsonSerializer<OnClick>, JsonDeserializer<OnClick> {
			@Override
			public JsonElement serialize(OnClick src, Type typeOfSrc, JsonSerializationContext context) {
				var obj = new JsonObject();
				obj.addProperty("action", src.action);
				obj.addProperty("value", src.value);
				return obj;
			}

			@Override
			public OnClick deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
					throws JsonParseException {
				var obj = json.getAsJsonObject();
				var onClick = new OnClick();
				onClick.action = obj.get("action").getAsString();
				onClick.value = obj.get("value").getAsString();
				return onClick;
			}
		}
	}
}
