package fun.polyvoxel.cui.serialize;

import com.google.common.collect.HashBiMap;
import fun.polyvoxel.cui.CUIPlugin;
import fun.polyvoxel.cui.ui.CUIInstance;
import com.google.gson.*;
import fun.polyvoxel.cui.ui.Camera;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.Locale;

public class CUIData {
	public static Gson gson = new GsonBuilder().setPrettyPrinting()
			.registerTypeAdapter(CUIData.class, new JsonAdapter())
			.registerTypeAdapter(LayerData.class, new LayerData.JsonAdapter())
			.registerTypeAdapter(SlotData.class, new SlotData.JsonAdapter())
			.registerTypeAdapter(SlotData.OnClick.class, new SlotData.OnClick.JsonAdapter()).create();
	public String name;
	public boolean singleton = false;
	public @NotNull Material icon = Material.CHEST;
	public @NotNull String title = "Chest UI";
	public int maxRow = 3;
	public int maxColumn = 9;
	public final HashBiMap<Integer, LayerData> layers = HashBiMap.create();

	public void toChestUI(CUIInstance<?> cui) {
		layers.forEach((depth, layerData) -> cui.edit().layer(depth, layerData.toLayer()));
	}

	public void toCamera(Camera<?> camera) {
		camera.edit().rowSize(maxRow).columnSize(maxColumn).done();
	}

	public NamespacedKey getKey() {
		return new NamespacedKey("json", name);
	}

	public Path getDefaultPath(CUIPlugin plugin) {
		return Path.of(plugin.getDataFolder().getAbsolutePath(), "json", name + ".json");
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
			obj.addProperty("name", src.name);
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
			cuiData.name = obj.get("name").getAsString();
			cuiData.singleton = obj.has("singleton") && obj.get("singleton").getAsBoolean();
			cuiData.icon = obj.has("icon")
					? Material.valueOf(obj.get("icon").getAsString().toUpperCase(Locale.ROOT))
					: Material.CHEST;
			cuiData.title = obj.has("title") ? obj.get("title").getAsString() : "Chest UI";
			cuiData.maxRow = obj.has("maxRow") ? obj.get("maxRow").getAsInt() : 3;
			cuiData.maxColumn = obj.has("maxColumn") ? obj.get("maxColumn").getAsInt() : 9;
			var layers = obj.getAsJsonObject("layers");
			layers.entrySet().forEach(entry -> cuiData.layers.put(Integer.parseInt(entry.getKey()),
					context.deserialize(entry.getValue(), LayerData.class)));
			return cuiData;
		}
	}
}
