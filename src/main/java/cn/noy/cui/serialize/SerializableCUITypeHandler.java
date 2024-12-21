package cn.noy.cui.serialize;

import cn.noy.cui.CUIPlugin;
import cn.noy.cui.ui.CUITypeHandler;
import cn.noy.cui.ui.ChestUI;
import org.bukkit.NamespacedKey;

import java.lang.reflect.InvocationTargetException;

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
