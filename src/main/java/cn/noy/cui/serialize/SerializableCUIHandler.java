package cn.noy.cui.serialize;

import cn.noy.cui.ui.CUIHandler;
import cn.noy.cui.ui.ChestUI;

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
