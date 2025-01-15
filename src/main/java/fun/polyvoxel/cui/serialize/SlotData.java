package fun.polyvoxel.cui.serialize;

import fun.polyvoxel.cui.CUIPlugin;
import fun.polyvoxel.cui.event.CUIClickEvent;
import fun.polyvoxel.cui.slot.*;
import fun.polyvoxel.cui.util.CmdHelper;
import fun.polyvoxel.cui.util.ItemStacks;
import com.google.gson.*;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

public class SlotData {
	public SlotType type = SlotType.BUTTON;
	public Material material = Material.CLOCK;
	public NamespacedKey model = Material.AIR.getKey();
	public String displayName = "";
	public ArrayList<String> lore;
	public int amount = 1;
	public final HashMap<ClickType, ArrayList<OnClick>> onClicks = new HashMap<>();

	public enum SlotType {
		EMPTY, BUTTON, STORAGE, TRANSFORMER,;
	}

	public Slot toSlot() {
		var itemStack = getItemStack();
		return switch (type) {
			case EMPTY -> Empty.getInstance();
			case BUTTON -> Button.builder().itemStack(itemStack).click(event -> {
				var actions = onClicks.get(event.getClickType());
				if (actions == null) {
					return;
				}
				for (OnClick onClick : actions) {
					onClick.onClick(event);
				}
			}).build();
			// TODO: 更多storage的功能
			case STORAGE -> Storage.builder().withInitial(itemStack).build();
			// TODO: transformer，default全部设成null
			default -> throw new IllegalStateException("Unexpected value: " + type);
		};
	}

	public @NotNull ItemStack getItemStack() {
		ItemStack itemStack;
		if (material == null || type == SlotType.EMPTY) {
			itemStack = ItemStack.empty();
		} else {
			itemStack = ItemStack.of(material, amount);
			itemStack.editMeta(itemMeta -> {
				if (model != null) {
					itemMeta.setItemModel(model);
				}
				if (displayName != null) {
					TextComponent component = LegacyComponentSerializer.legacyAmpersand().deserialize(displayName);
					itemMeta.displayName(ItemStacks.cleanComponent(component));
				}
				if (lore != null) {
					itemMeta.lore(lore.stream().map(LegacyComponentSerializer.legacyAmpersand()::deserialize)
							.map(ItemStacks::cleanComponent).toList());
				}
			});
		}
		return itemStack;
	}

	public void setItemStack(ItemStack itemStack) {
		this.material = itemStack.getType();
		this.amount = itemStack.getAmount();
		itemStack.editMeta(meta -> {
			if (meta.hasDisplayName()) {
				this.displayName = LegacyComponentSerializer.legacyAmpersand()
						.serialize(Objects.requireNonNull(meta.displayName()));
			}
			if (meta.hasLore()) {
				this.lore = Objects.requireNonNull(meta.lore()).stream()
						.map(LegacyComponentSerializer.legacyAmpersand()::serialize)
						.collect(Collectors.toCollection(ArrayList::new));
			}
		});
	}

	public static class JsonAdapter implements JsonSerializer<SlotData>, JsonDeserializer<SlotData> {

		@Override
		public JsonElement serialize(SlotData src, Type typeOfSrc, JsonSerializationContext context) {
			var obj = new JsonObject();
			obj.addProperty("type", src.type.name());
			if (src.material != null) {
				obj.addProperty("material", src.material.name());
			}
			if (src.displayName != null) {
				obj.addProperty("displayName", src.displayName);
			}
			if (src.model != null) {
				obj.addProperty("model", src.model.toString());
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
			slotData.type = obj.has("type")
					? SlotType.valueOf(obj.get("type").getAsString().toUpperCase(Locale.ROOT))
					: SlotType.BUTTON;
			slotData.material = obj.has("material")
					? Material.valueOf(obj.get("material").getAsString().toUpperCase(Locale.ROOT))
					: null;
			try {
				slotData.model = obj.has("model") ? NamespacedKey.fromString(obj.get("model").getAsString()) : null;
			} catch (Exception e) {
				CUIPlugin.logger().warn("Failed to parse model key: {}", obj.get("model").getAsString());
				slotData.model = null;
			}
			slotData.displayName = obj.has("displayName") ? obj.get("displayName").getAsString() : "";
			slotData.lore = obj.has("lore") ? context.deserialize(obj.get("lore"), ArrayList.class) : null;
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
		public Action action = Action.COMMAND;
		public String value = "";

		public enum Action {
			COMMAND, COMMAND_OP, COMMAND_CONSOLE, CHEST_UI,;
		}

		public void onClick(CUIClickEvent<?> event) {
			switch (action) {
				case COMMAND -> event.getPlayer().performCommand(value);
				case COMMAND_OP -> CmdHelper.performCommandAsOp(event.getPlayer(), value);
				case COMMAND_CONSOLE -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), value);
			}
		}

		public static class JsonAdapter implements JsonSerializer<OnClick>, JsonDeserializer<OnClick> {
			@Override
			public JsonElement serialize(OnClick src, Type typeOfSrc, JsonSerializationContext context) {
				var obj = new JsonObject();
				obj.addProperty("action", src.action.name());
				obj.addProperty("value", src.value);
				return obj;
			}

			@Override
			public OnClick deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
					throws JsonParseException {
				var obj = json.getAsJsonObject();
				var onClick = new OnClick();
				onClick.action = Action.valueOf(obj.get("action").getAsString().toUpperCase(Locale.ROOT));
				onClick.value = obj.get("value").getAsString();
				return onClick;
			}
		}
	}
}
