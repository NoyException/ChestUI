package fun.polyvoxel.cui.ui;

import fun.polyvoxel.cui.CUIPlugin;
import fun.polyvoxel.cui.event.CUIDisplayEvent;
import fun.polyvoxel.cui.serialize.SerializableChestUI;
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
	private Function<Player, TriggerResult<T>> onDisplay;
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

	public int getInstancesCount() {
		return instances.size();
	}

	public @NotNull CUIInstance<T> createInstance() {
		if (singleton && !instances.isEmpty()) {
			throw new IllegalStateException("Singleton CUI `" + key + "` already exists");
		}
		var id = nextId++;
		var cui = new CUIInstance<>(cuiPlugin, this, id);
		instances.put(id, cui);
		return cui;
	}

	public boolean canDisplay() {
		return onDisplay != null;
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
		if (onDisplay == null) {
			return null;
		}

		var event = new CUIDisplayEvent<>(this, player, asChild);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled()) {
			return null;
		}

		var result = onDisplay.apply(player);
		if (result == null) {
			return null;
		}
		return trigger(result, player, event.isAsChild());
	}

	private Camera<T> trigger(TriggerResult<T> result, Player player, boolean asChild) {
		Camera<T> camera = switch (result.type) {
			case USE_DEFAULT_CAMERA -> getInstance().getCamera();
			case CREATE_NEW_CAMERA -> getInstance().createCamera();
			case CREATE_NEW_CUI_INSTANCE -> createInstance().createCamera();
			default -> null;
		};
		if (camera == null) {
			return null;
		}
		result.onTrigger.accept(camera);
		camera.open(player, asChild);
		return camera;
	}

	public record TriggerResult<T extends ChestUI<T>>(TriggerResultType type, Consumer<Camera<T>> onTrigger) {
	}

	public enum TriggerResultType {
		REJECTED, USE_DEFAULT_CAMERA, CREATE_NEW_CAMERA, CREATE_NEW_CUI_INSTANCE
	}

	public Editor edit() {
		return new Editor();
	}

	public class Editor {
		private Editor() {
		}

		public Editor defaultTitle(String title) {
			defaultTitle = LegacyComponentSerializer.legacyAmpersand().deserialize(title);
			return this;
		}

		public Editor defaultTitle(Component title) {
			defaultTitle = title;
			return this;
		}

		public Editor triggerByBlock(Function<PlayerInteractEvent, TriggerResult<T>> trigger) {
			Bukkit.getPluginManager().registerEvents(new Listener() {
				@EventHandler(priority = EventPriority.MONITOR)
				public void onBlockClick(PlayerInteractEvent event) {
					if (!event.hasBlock()) {
						return;
					}
					var result = trigger.apply(event);
					trigger(result, event.getPlayer(), false);
				}
			}, cuiPlugin);
			return this;
		}

		public Editor triggerByDisplayCommand(Function<Player, TriggerResult<T>> trigger) {
			onDisplay = trigger;
			return this;
		}
	}
}
