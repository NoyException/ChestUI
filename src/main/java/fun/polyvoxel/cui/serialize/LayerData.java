package fun.polyvoxel.cui.serialize;

import fun.polyvoxel.cui.layer.Layer;
import fun.polyvoxel.cui.util.Position;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.HashMap;

public class LayerData {
	public int maxRow = 3;
	public int maxColumn = 9;
	public int marginTop;
	public int marginLeft;
	public boolean relative;
	public final HashMap<Position, SlotData> slots = new HashMap<>();

	public Layer toLayer() {
		var layer = new Layer(maxRow, maxColumn).edit().marginTop(marginTop).marginLeft(marginLeft).relative(relative)
				.done();
		slots.forEach((position, slotData) -> layer.edit().slot(position.row(), position.column(), slotData::toSlot));
		return layer;
	}

	public static class JsonAdapter implements JsonSerializer<LayerData>, JsonDeserializer<LayerData> {
		@Override
		public JsonElement serialize(LayerData src, Type typeOfSrc, JsonSerializationContext context) {
			var obj = new JsonObject();
			obj.addProperty("maxRow", src.maxRow);
			obj.addProperty("maxColumn", src.maxColumn);
			obj.addProperty("marginTop", src.marginTop);
			obj.addProperty("marginLeft", src.marginLeft);
			obj.addProperty("relative", src.relative);
			var slots = new JsonObject();
			src.slots.forEach((position, slotData) -> slots.add(position.toString(), context.serialize(slotData)));
			obj.add("slots", slots);
			return obj;
		}

		@Override
		public LayerData deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
				throws JsonParseException {
			var obj = json.getAsJsonObject();
			var layerData = new LayerData();
			layerData.maxRow = obj.get("maxRow").getAsInt();
			layerData.maxColumn = obj.get("maxColumn").getAsInt();
			layerData.marginTop = obj.get("marginTop").getAsInt();
			layerData.marginLeft = obj.get("marginLeft").getAsInt();
			layerData.relative = obj.get("relative").getAsBoolean();
			var slots = obj.getAsJsonObject("slots");
			slots.entrySet().forEach(entry -> layerData.slots.put(Position.fromString(entry.getKey()),
					context.deserialize(entry.getValue(), SlotData.class)));
			return layerData;
		}
	}
}
