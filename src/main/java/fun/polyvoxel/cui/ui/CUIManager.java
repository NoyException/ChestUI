package fun.polyvoxel.cui.ui;

import fun.polyvoxel.cui.CUIPlugin;
import fun.polyvoxel.cui.serialize.CUIData;
import fun.polyvoxel.cui.serialize.SerializableCUITypeHandler;
import fun.polyvoxel.cui.util.ItemStacks;

import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CUIManager implements Listener {
	private final CUIPlugin plugin;

	public CUIManager(CUIPlugin plugin) {
		this.plugin = plugin;
	}

	private boolean initialized = false;
	private final HashMap<NamespacedKey, CUITypeHandler<?>> cuiTypes = new HashMap<>();
	private final HashMap<Class<?>, CUITypeHandler<?>> cuiTypesByClass = new HashMap<>();

	@SuppressWarnings({"rawtypes", "unchecked"})
	public List<ChestUI<?>> getCUIs() {
		return (List) cuiTypes.values().stream().flatMap(type -> type.getInstances().stream()).toList();
	}

	public List<NamespacedKey> getRegisteredCUINames() {
		return new ArrayList<>(cuiTypes.keySet());
	}

	public List<NamespacedKey> getRegisteredCUINames(Plugin plugin) {
		return cuiTypes.keySet().stream()
				.filter(namespacedKey -> namespacedKey.getNamespace().equalsIgnoreCase(plugin.getName())).toList();
	}

	public CUITypeHandler<?> getCUITypeHandler(NamespacedKey key) {
		return cuiTypes.get(key);
	}

	public CUITypeHandler<?> getCUITypeHandler(Plugin plugin, String name) {
		return cuiTypes.get(NamespacedKey.fromString(name, plugin));
	}

	@SuppressWarnings("unchecked")
	public <T extends CUIHandler<T>> CUITypeHandler<T> getCUITypeHandler(Class<T> clazz) {
		return (CUITypeHandler<T>) cuiTypesByClass.get(clazz);
	}

	private void tick() {
		cuiTypes.values().forEach(CUITypeHandler::tick);
		plugin.getCameraManager().forEachCamera(Camera::update);
	}

	public void setup() {
		if (initialized) {
			throw new IllegalStateException("CUIManager has already been initialized");
		}
		initialized = true;
		Bukkit.getPluginManager().registerEvents(this, plugin);
		scanPlugin(plugin);
		registerCUIsFromFiles();
	}

	public void teardown() {
		if (!initialized) {
			throw new IllegalStateException("CUIManager has not been initialized");
		}
		getCUIs().forEach(ChestUI::destroy);
	}

	public record ParseResult(CUITypeHandler<?> typeHandler, Integer instanceId, Integer cameraId) {
	}

	public ParseResult parse(String name) throws IllegalArgumentException {
		var split = name.split("#");
		if (split.length < 1) {
			throw new IllegalArgumentException("Invalid CUI name: " + name);
		}
		var key = NamespacedKey.fromString(split[0]);
		var typeHandler = getCUITypeHandler(key);
		if (typeHandler == null) {
			return new ParseResult(null, null, null);
		}
		if (split.length < 2) {
			return new ParseResult(typeHandler, null, null);
		}
		var instanceId = Integer.parseInt(split[1]);
		if (split.length < 3) {
			return new ParseResult(typeHandler, instanceId, null);
		}
		var cameraId = Integer.parseInt(split[2]);
		return new ParseResult(typeHandler, instanceId, cameraId);
	}

	/**
	 * 如果你的CUIHandler注解了{@link CUI}，则应当会被自动注册。如果因为某些原因未被注册，可以手动调用此方法注册。<br>
	 * If your CUIHandler is annotated with {@link CUI}, it should be automatically
	 * registered. If not, you can manually
	 *
	 * @param handlerClass
	 *            CUIHandler的实现类
	 * @param plugin
	 *            注册的插件
	 * @param name
	 *            CUI的名称
	 * @param singleton
	 *            是否为单例
	 * @param <T>
	 *            CUIHandler的实现类
	 */
	public <T extends CUIHandler<T>> void registerCUI(Class<T> handlerClass, Plugin plugin, String name,
			boolean singleton) {
		var key = NamespacedKey.fromString(name, plugin);
		if (cuiTypes.containsKey(key)) {
			throw new IllegalArgumentException("CUI `" + key + "` has already been registered");
		}
		cuiTypes.put(key, new CUITypeHandler<>(this.plugin, handlerClass, key, singleton));
		cuiTypesByClass.put(handlerClass, cuiTypes.get(key));
	}

	@ApiStatus.Experimental
	public void registerCUI(CUIData data) {
		if (cuiTypes.containsKey(data.getKey())) {
			throw new IllegalArgumentException("CUI `" + data.getKey() + "` has already been registered");
		}
		var typeHandler = new SerializableCUITypeHandler(plugin, data);
		cuiTypes.put(data.getKey(), typeHandler);
	}

	@ApiStatus.Experimental
	public void registerCUI(File file) {
		if (file.getName().endsWith(".json")) {
			try (FileReader reader = new FileReader(file)) {
				var data = CUIData.fromJson(reader);
				registerCUI(data);
			} catch (IOException e) {
				plugin.getLogger().warning("Failed to read CUI file: " + file.getName());
				e.printStackTrace();
			}
		}
	}

	private void registerCUIsFromFiles() {
		// 遍历folder/cui下的json文件
		var cuiFolder = new File(plugin.getDataFolder(), "cui");
		if (!cuiFolder.exists()) {
			return;
		}
		if (!cuiFolder.isDirectory()) {
			plugin.getLogger().warning("cui folder is not a directory");
			return;
		}
		var files = cuiFolder.listFiles();
		if (files == null) {
			return;
		}
		var count = 0;
		for (File file : files) {
			if (!file.getName().endsWith(".json")) {
				continue;
			}
			registerCUI(file);
			count++;
		}
		plugin.getLogger().info("Registered " + count + " CUIs from files");
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private void scanPlugin(Plugin plg) {
		// 使用目标插件的 ClassLoader 创建 Reflections 实例
		Reflections reflections = new Reflections(
				new ConfigurationBuilder().setUrls(ClasspathHelper.forClassLoader(plg.getClass().getClassLoader()))
						.filterInputsBy(new FilterBuilder().includePackage(plg.getClass().getPackageName())));

		Set<Class<?>> annotatedClasses = reflections.getTypesAnnotatedWith(CUI.class);

		plugin.getLogger()
				.info("Found " + annotatedClasses.size() + " classes with @CUI annotation in " + plg.getName());
		for (Class<?> clazz : annotatedClasses) {
			// 判断是否实现了CUIHandler
			if (!CUIHandler.class.isAssignableFrom(clazz)) {
				plugin.getLogger().warning("Class `" + clazz.getCanonicalName() + "` does not implement CUIHandler");
				continue;
			}
			// 处理带有@CUI注解的类
			var annotation = clazz.getAnnotation(CUI.class);
			registerCUI((Class) clazz, plg, annotation.name(), annotation.singleton());
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPluginEnable(PluginEnableEvent event) {
		Plugin plg = event.getPlugin();
		// 检查插件是否依赖本插件
		if (!plg.getPluginMeta().getPluginDependencies().contains(plugin.getName())
				&& !plg.getPluginMeta().getPluginSoftDependencies().contains(plugin.getName())) {
			return;
		}
		scanPlugin(plg);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onTickEnd(ServerTickEndEvent event) {
		tick();
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onClose(InventoryCloseEvent event) {
		if (!(event.getPlayer() instanceof Player player))
			return;

		switch (event.getReason()) {
			case PLAYER -> plugin.getCameraManager().closeTop(player, false);
			case DISCONNECT -> plugin.getCameraManager().closeAll(player, true);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onClick(InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player player))
			return;

		var camera = plugin.getCameraManager().getCamera(player);
		if (camera == null)
			return;

		if (event.getClickedInventory() != player.getInventory()) {
			var rawSlot = event.getRawSlot();
			camera.click(player, event.getClick(), event.getAction(), rawSlot / 9, rawSlot % 9, event.getCursor());
			event.setCancelled(true);
		} else {
			switch (event.getAction()) {
				case MOVE_TO_OTHER_INVENTORY -> {
					var itemStack = event.getCurrentItem();
					var remaining = camera.addItem(player, itemStack);
					event.setCurrentItem(remaining);
					event.setCancelled(true);
				}
				case COLLECT_TO_CURSOR -> {
					var collected = camera.collect(player, event.getCursor(), true);
					player.setItemOnCursor(collected);
					event.setCancelled(true);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onDrag(InventoryDragEvent event) {
		if (!(event.getWhoClicked() instanceof Player player))
			return;

		var camera = plugin.getCameraManager().getCamera(player);
		if (camera == null)
			return;

		var view = event.getView();
		var cursor = event.getOldCursor().clone();
		var amount = new AtomicInteger();
		if (!ItemStacks.isEmpty(event.getCursor())) {
			amount.set(event.getCursor().getAmount());
		}

		event.getNewItems().forEach((rawSlot, itemStack) -> {
			if (view.convertSlot(rawSlot) != rawSlot) {
				return;
			}
			if (ItemStacks.isEmpty(itemStack)) {
				return;
			}

			itemStack.setAmount(itemStack.getAmount() - ItemStacks.getAmount(view.getItem(rawSlot)));
			var remaining = camera.place(player, itemStack, rawSlot / 9, rawSlot % 9);
			if (!ItemStacks.isEmpty(remaining)) {
				amount.addAndGet(remaining.getAmount());
			}
		});
		cursor.setAmount(amount.get());
		event.setCursor(cursor);
	}
}
