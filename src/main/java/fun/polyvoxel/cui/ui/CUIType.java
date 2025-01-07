package fun.polyvoxel.cui.ui;

import fun.polyvoxel.cui.CUIPlugin;
import fun.polyvoxel.cui.serialize.SerializableChestUI;
import fun.polyvoxel.cui.ui.provider.BlockCUIProvider;
import fun.polyvoxel.cui.ui.provider.CUIProvider;
import fun.polyvoxel.cui.ui.provider.ItemStackCUIProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.*;

public final class CUIType<T extends ChestUI<T>> {
	private final CUIPlugin cuiPlugin;
	private final @Nullable Plugin plugin;
	private final T chestUI;
	private final NamespacedKey key;
	private final boolean singleton;
	private final Material icon;
	private Component defaultTitle = Component.text("Chest UI", NamedTextColor.GOLD);
	private final HashMap<Integer, CUIInstance<T>> instances = new HashMap<>();
	private final List<CUIProvider<T>> providers = new LinkedList<>();
	private boolean registered = false;
	private int nextId = 0;

	public CUIType(CUIPlugin cuiPlugin, @Nullable Plugin plugin, T chestUI, NamespacedKey key, boolean singleton,
			Material icon) {
		this.cuiPlugin = cuiPlugin;
		this.plugin = plugin;
		this.chestUI = chestUI;
		this.key = key;
		this.singleton = singleton;
		this.icon = icon;
		chestUI.onInitialize(this);
	}

	public CUIPlugin getCUIPlugin() {
		return cuiPlugin;
	}

	public @Nullable Plugin getPlugin() {
		return plugin;
	}

	public void tickStart() {
		chestUI.onTickStart();
		// 用for iter来在过程中移除
		for (var iterator = instances.entrySet().iterator(); iterator.hasNext();) {
			var entry = iterator.next();
			var cui = entry.getValue();
			switch (cui.getState()) {
				case READY -> cui.tickStart();
				case DESTROYED -> iterator.remove();
			}
		}
	}

	public void tick() {
		chestUI.onTick();
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

	public void tickEnd() {
		chestUI.onTickEnd();
		// 用for iter来在过程中移除
		for (var iterator = instances.entrySet().iterator(); iterator.hasNext();) {
			var entry = iterator.next();
			var cui = entry.getValue();
			switch (cui.getState()) {
				case READY -> cui.tickEnd();
				case DESTROYED -> iterator.remove();
			}
		}
	}

	public @NotNull NamespacedKey getKey() {
		return key;
	}

	public @NotNull T getChestUI() {
		return chestUI;
	}

	public boolean isSingleton() {
		return singleton;
	}

	public Material getIcon() {
		return icon;
	}

	public Component getDefaultTitle() {
		return defaultTitle;
	}

	public boolean isSerializable() {
		return chestUI.getClass() == SerializableChestUI.class;
	}

	public <S> boolean display(DisplayContext<S> context) {
		var camera = chestUI.getDisplayedCamera(context);
		if (camera == null) {
			return false;
		}
		// TODO: 简化为直接传context？
		camera.open(context.getViewer(), context.isAsChild(), context.getSource());
		return true;
	}

	/**
	 * 获取单例CUI实例。如果不是单例模式，将不保证每次调用返回的是同一个实例。<br>
	 * Get the singleton CUI instance. If it is not in singleton mode, it is not
	 * guaranteed that the same instance will be returned each time.
	 * 
	 * @return CUI实例。<br>
	 *         CUI instance.
	 */
	public @Nullable CUIInstance<T> getInstance() {
		if (instances.isEmpty()) {
			return null;
		}
		return instances.values().iterator().next();
	}

	public @Nullable CUIInstance<T> getInstance(int id) {
		return instances.get(id);
	}

	public @NotNull List<CUIInstance<T>> getInstances() {
		return new ArrayList<>(instances.values());
	}

	public int getInstanceCount() {
		return instances.size();
	}

	public @NotNull CUIInstance<T> createInstance(CUIInstanceHandler<T> handler) {
		if (singleton && !instances.isEmpty()) {
			throw new IllegalStateException("Singleton CUI `" + key + "` already exists");
		}
		var id = nextId++;
		var cui = new CUIInstance<>(cuiPlugin, this, handler, id);
		instances.put(id, cui);
		chestUI.onCreateInstance(cui);
		return cui;
	}

	public void onRegister() {
		for (var provider : providers) {
			provider.enable(this);
		}
		registered = true;
	}

	public void onUnregister() {
		registered = false;
		for (var provider : providers) {
			provider.disable();
		}
		getInstances().forEach(CUIInstance::destroy);
	}

	public Editor edit() {
		return new Editor();
	}

	public class Editor {
		private Editor() {
		}

		public CUIType<T> done() {
			return CUIType.this;
		}

		public Editor defaultTitle(String title) {
			defaultTitle = LegacyComponentSerializer.legacyAmpersand().deserialize(title);
			return this;
		}

		public Editor defaultTitle(Component title) {
			defaultTitle = title;
			return this;
		}

		public Editor provide(CUIProvider<T> provider) {
			if (registered) {
				provider.enable(CUIType.this);
			}
			providers.add(provider);
			return this;
		}

		public Editor provideByBlock(Predicate<PlayerInteractEvent> predicate) {
			return provide(new BlockCUIProvider<>(predicate));
		}

		public Editor provideByItemStack(Predicate<PlayerInteractEvent> predicate) {
			return provide(new ItemStackCUIProvider<>(predicate));
		}
	}
}
