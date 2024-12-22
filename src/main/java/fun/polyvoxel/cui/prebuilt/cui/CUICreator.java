package fun.polyvoxel.cui.prebuilt.cui;

import fun.polyvoxel.cui.ui.*;
import com.google.gson.JsonObject;

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
