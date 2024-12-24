package fun.polyvoxel.cui.ui;

import fun.polyvoxel.cui.CUIPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class CUIType<T extends ChestUI<T>> {
	private final CUIPlugin plugin;
	private final T chestUI;
	private final NamespacedKey key;
	private final boolean singleton;
	private final HashMap<Integer, CUIInstance<T>> instances = new HashMap<>();
	private int nextId = 0;

	public CUIType(CUIPlugin plugin, T chestUI, NamespacedKey key, boolean singleton) {
		this.plugin = plugin;
		this.chestUI = chestUI;
		this.key = key;
		this.singleton = singleton;
		chestUI.onInitialize(this);
	}

	public void tick() {
		// 用for iter来在过程中移除
		for (var iterator = instances.entrySet().iterator(); iterator.hasNext();) {
			var entry = iterator.next();
			var cui = entry.getValue();
			switch (cui.getState()) {
				case READY -> cui.tick();
				case DESTROYED -> iterator.remove();
			}
		}
	}

	public NamespacedKey getKey() {
		return key;
	}

	public T getChestUI() {
		return chestUI;
	}

	public boolean isSingleton() {
		return singleton;
	}

	public CUIInstance<T> getInstance() {
		if (!singleton) {
			throw new IllegalStateException("CUI `" + key + "` is not a singleton");
		}
		if (instances.isEmpty()) {
			return createInstance();
		}
		return instances.values().iterator().next();
	}

	public CUIInstance<T> getInstance(int id) {
		return instances.get(id);
	}

	public List<CUIInstance<T>> getInstances() {
		return new ArrayList<>(instances.values());
	}

	public int getInstancesCount() {
		return instances.size();
	}

	public CUIInstance<T> createInstance() {
		if (singleton && !instances.isEmpty()) {
			throw new IllegalStateException("Singleton CUI `" + key + "` already exists");
		}
		var id = nextId++;
		var handler = chestUI.createHandler();
		var cui = new CUIInstance<>(plugin, this, handler, key.toString(), id);
		instances.put(id, cui);
		return cui;
	}

	public Editor edit() {
		return new Editor();
	}

	public class Editor {
		public Editor triggerByBlock(Consumer<Block> trigger) {
			return this;
		}

		public Editor triggerByCommand(BiConsumer<CommandSender, String[]> trigger) {
			return this;
		}
	}
}
