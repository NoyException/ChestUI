package fun.polyvoxel.cui.serialize;

import fun.polyvoxel.cui.CUIPlugin;
import fun.polyvoxel.cui.ui.CUITypeHandler;

public class SerializableCUITypeHandler extends CUITypeHandler<SerializableCUIHandler> {
	private final CUIData cuiData;

	public SerializableCUITypeHandler(CUIPlugin plugin, CUIData cuiData) {
		super(plugin, SerializableCUIHandler.class, cuiData.getKey(), cuiData.singleton);
		this.cuiData = cuiData;
	}

	@Override
	protected SerializableCUIHandler createHandler() {
		if (cuiData.singleton && getInstancesCount() > 0) {
			throw new IllegalStateException("Singleton CUI `" + getKey() + "` already exists");
		}
		return new SerializableCUIHandler(cuiData);
	}
}
