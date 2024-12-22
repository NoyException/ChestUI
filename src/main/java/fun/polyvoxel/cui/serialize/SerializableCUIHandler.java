package fun.polyvoxel.cui.serialize;

import fun.polyvoxel.cui.ui.CUIHandler;
import fun.polyvoxel.cui.ui.ChestUI;

public class SerializableCUIHandler implements CUIHandler<SerializableCUIHandler> {
	private final CUIData cuiData;

	public SerializableCUIHandler(CUIData cuiData) {
		this.cuiData = cuiData;
	}

	@Override
	public void onInitialize(ChestUI<SerializableCUIHandler> cui) {
		cuiData.toChestUI(cui);
	}
}
