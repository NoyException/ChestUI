package fun.polyvoxel.cui.serialize;

import fun.polyvoxel.cui.ui.CUIInstance;
import com.google.gson.*;
import fun.polyvoxel.cui.ui.Camera;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

import java.io.Reader;
import java.lang.reflect.Type;
import java.util.HashMap;

public class CUIData {
	public static Gson gson = new GsonBuilder().setPrettyPrinting()
			.registerTypeAdapter(CUIData.class, new JsonAdapter())
			.registerTypeAdapter(LayerData.class, new LayerData.JsonAdapter())
			.registerTypeAdapter(SlotData.class, new SlotData.JsonAdapter())
			.registerTypeAdapter(SlotData.OnClick.class, new SlotData.OnClick.JsonAdapter()).create();
	public String key;
	public boolean singleton;
	public Material icon = Material.CHEST;
	public String title;
	public int maxRow = 3;
	public int maxColumn = 9;
	public final HashMap<Integer, LayerData> layers = new HashMap<>();

	public void toChestUI(CUIInstance<?> cui) {
		layers.forEach((depth, layerData) -> cui.edit().layer(depth, layerData.toLayer()));
	}

	public void toCamera(Camera<?> camera) {
		camera.edit().rowSize(maxRow).columnSize(maxColumn).done();
	}

	public NamespacedKey getKey() {
		return NamespacedKey.fromString(key);
	}

	public String toJson() {
		return gson.toJson(this);
	}

	public static CUIData fromJson(String json) {
		return gson.fromJson(json, CUIData.class);
	}

	public static CUIData fromJson(Reader reader) {
		return gson.fromJson(reader, CUIData.class);
	}

	public static class JsonAdapter implements JsonSerializer<CUIData>, JsonDeserializer<CUIData> {
		@Override
		public JsonElement serialize(CUIData src, Type typeOfSrc, JsonSerializationContext context) {
			var obj = new JsonObject();
			obj.addProperty("key", src.key);
			obj.addProperty("singleton", src.singleton);
			obj.addProperty("icon", src.icon.name());
			obj.addProperty("title", src.title);
			obj.addProperty("maxRow", src.maxRow);
			obj.addProperty("maxColumn", src.maxColumn);
			var layers = new JsonObject();
			src.layers.forEach((depth, layerData) -> layers.add(String.valueOf(depth), context.serialize(layerData)));
			obj.add("layers", layers);
			return obj;
		}

		@Override
		public CUIData deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
				throws JsonParseException {
			var obj = json.getAsJsonObject();
			var cuiData = new CUIData();
			cuiData.key = obj.get("key").getAsString();
			cuiData.singleton = obj.get("singleton").getAsBoolean();
			var rawIcon = obj.get("icon");
			if (rawIcon == null) {
				cuiData.icon = Material.CHEST;
			} else {
				cuiData.icon = Material.getMaterial(rawIcon.getAsString());
			}
			cuiData.title = obj.get("title").getAsString();
			cuiData.maxRow = obj.get("maxRow").getAsInt();
			cuiData.maxColumn = obj.get("maxColumn").getAsInt();
			var layers = obj.getAsJsonObject("layers");
			layers.entrySet().forEach(entry -> cuiData.layers.put(Integer.parseInt(entry.getKey()),
					context.deserialize(entry.getValue(), LayerData.class)));
			return cuiData;
		}
	}
}
