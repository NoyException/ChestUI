package cn.noy.cui.ui;

import cn.noy.cui.CUIPlugin;
import org.bukkit.NamespacedKey;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CUITypeHandler<T extends CUIHandler<T>> {
	private final CUIPlugin plugin;
	private final Class<T> handlerClass;
	private final NamespacedKey key;
	private final boolean singleton;
	private final HashMap<Integer, ChestUI<T>> instances = new HashMap<>();
	private int nextId = 0;

	public CUITypeHandler(CUIPlugin plugin, Class<T> handlerClass, NamespacedKey key, boolean singleton) {
		this.plugin = plugin;
		this.handlerClass = handlerClass;
		this.key = key;
		this.singleton = singleton;
		if (singleton) {
			createInstance().edit().setKeepAlive(true).finish();
		}
	}

	public void tick() {
		// 用for iter来在过程中移除
		for (var iterator = instances.entrySet().iterator(); iterator.hasNext();) {
			var entry = iterator.next();
			var cui = entry.getValue();
			switch (cui.getState()) {
				case READY -> cui.getTrigger().tick();
				case DESTROYED -> iterator.remove();
			}
		}
	}

	public NamespacedKey getKey() {
		return key;
	}

	public boolean isSingleton() {
		return singleton;
	}

	public ChestUI<T> getInstance() {
		if (!singleton) {
			throw new IllegalStateException("CUI `" + key + "` is not a singleton");
		}
		return instances.get(0);
	}

	public ChestUI<T> getInstance(int id) {
		return instances.get(id);
	}

	public List<ChestUI<T>> getInstances() {
		return new ArrayList<>(instances.values());
	}

	public ChestUI<T> createInstance() {
		if (singleton && !instances.isEmpty()) {
			throw new IllegalStateException("Singleton CUI `" + key + "` already exists");
		}
		T handler;
		try {
			Constructor<T> constructor = handlerClass.getConstructor();
			handler = constructor.newInstance();
		} catch (NoSuchMethodException | InstantiationException | IllegalAccessException
				| InvocationTargetException e) {
			throw new RuntimeException(
					"CUI Handler `" + handlerClass.getCanonicalName() + "` must have a public no-args constructor");
		}
		var id = nextId++;
		var cui = new ChestUI<>(plugin, handler, key.toString(), id);
		instances.put(id, cui);
		return cui;
	}
}
