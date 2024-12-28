package fun.polyvoxel.cui.ui;

import fun.polyvoxel.cui.CUIPlugin;
import fun.polyvoxel.cui.event.CUIDisplayEvent;
import fun.polyvoxel.cui.serialize.SerializableChestUI;
import fun.polyvoxel.cui.util.Context;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
	private CameraProvider<T> displayCameraProvider;
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

	/**
	 * 获取单例CUI实例。如果不是单例模式，将不保证每次调用返回的是同一个实例。<br>
	 * Get the singleton CUI instance. If it is not in singleton mode, it is not
	 * guaranteed that the same instance will be returned each time.
	 * 
	 * @return CUI实例。<br>
	 *         CUI instance.
	 */
	public @NotNull CUIInstance<T> getInstance() {
		if (instances.isEmpty()) {
			return createInstance();
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

	public @NotNull CUIInstance<T> createInstance() {
		return createInstance(Context.BACKGROUND);
	}

	public @NotNull CUIInstance<T> createInstance(Context context) {
		if (singleton && !instances.isEmpty()) {
			throw new IllegalStateException("Singleton CUI `" + key + "` already exists");
		}
		var id = nextId++;
		var handler = chestUI.createCUIInstanceHandler(context);
		var cui = new CUIInstance<>(cuiPlugin, this, handler, id);
		instances.put(id, cui);
		return cui;
	}

	public boolean canDisplay() {
		return displayCameraProvider != null;
	}

	/**
	 * 尝试向玩家一键展示CUI。如果该CUI没有配置默认展示方式，将返回null。<br>
	 * Try to display the CUI to the player in one click. If the CUI does not have a
	 * default display method, null will be returned.
	 * 
	 * @param player
	 *            玩家<br>
	 *            Player
	 * @return 摄像头<br>
	 *         Camera
	 */
	public @Nullable Camera<T> display(Player player, boolean asChild) {
		if (displayCameraProvider == null) {
			return null;
		}

		var event = new CUIDisplayEvent<>(this, player, asChild);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled()) {
			return null;
		}

		return trigger(displayCameraProvider, player, event.isAsChild());
	}

	private Camera<T> trigger(CameraProvider<T> cameraProvider, Player player, boolean asChild) {
		Camera<T> camera = cameraProvider.provide(this, player, asChild);
		if (camera == null) {
			return null;
		}
		camera.open(player, asChild);
		return camera;
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

		public Editor triggerByBlock(Function<PlayerInteractEvent, @NotNull CameraProvider<T>> trigger) {
			Bukkit.getPluginManager().registerEvents(new Listener() {
				@EventHandler(priority = EventPriority.MONITOR)
				public void onBlockClick(PlayerInteractEvent event) {
					if (!event.hasBlock()) {
						return;
					}
					var cameraProvider = trigger.apply(event);
					trigger(cameraProvider, event.getPlayer(), false);
				}
			}, cuiPlugin);
			return this;
		}

		public Editor triggerByDisplay(@NotNull CameraProvider<T> cameraProvider) {
			displayCameraProvider = cameraProvider;
			return this;
		}
	}
}
