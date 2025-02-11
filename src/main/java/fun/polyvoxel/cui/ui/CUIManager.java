package fun.polyvoxel.cui.ui;

import com.destroystokyo.paper.event.server.ServerTickStartEvent;
import fun.polyvoxel.cui.CUIPlugin;
import fun.polyvoxel.cui.event.CUIRegisterEvent;
import fun.polyvoxel.cui.serialize.CUIData;
import fun.polyvoxel.cui.serialize.SerializableChestUI;
import fun.polyvoxel.cui.util.ItemStacks;

import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class CUIManager implements Listener {
	private final CUIPlugin plugin;

	public CUIManager(CUIPlugin plugin) {
		this.plugin = plugin;
	}

	private boolean initialized = false;
	private final HashMap<NamespacedKey, CUIType<?>> cuiTypes = new HashMap<>();
	private final HashMap<Class<?>, CUIType<?>> cuiTypesByClass = new HashMap<>();

	@SuppressWarnings({"rawtypes", "unchecked"})
	public List<CUIInstance<?>> getCUIInstances() {
		return (List) cuiTypes.values().stream().flatMap(type -> type.getInstances().stream()).toList();
	}

	public List<NamespacedKey> getRegisteredCUINames() {
		return new ArrayList<>(cuiTypes.keySet());
	}

	public List<CUIType<?>> getRegisteredCUITypes() {
		return new ArrayList<>(cuiTypes.values());
	}

	public List<NamespacedKey> getRegisteredCUINames(Plugin plugin) {
		return cuiTypes.keySet().stream()
				.filter(namespacedKey -> namespacedKey.getNamespace().equalsIgnoreCase(plugin.getName())).toList();
	}

	@SuppressWarnings("unchecked")
	public <T extends ChestUI<T>> CUIType<T> getCUIType(NamespacedKey key) {
		return (CUIType<T>) cuiTypes.get(key);
	}

	@SuppressWarnings("unchecked")
	public <T extends ChestUI<T>> CUIType<T> getCUIType(Plugin plugin, String name) {
		return (CUIType<T>) cuiTypes.get(NamespacedKey.fromString(name, plugin));
	}

	@SuppressWarnings("unchecked")
	public <T extends ChestUI<T>> CUIType<T> getCUIType(Class<T> clazz) {
		return (CUIType<T>) cuiTypesByClass.get(clazz);
	}

	private void tickStart() {
		cuiTypes.values().forEach(CUIType::tickStart);
	}

	private void tick() {
		cuiTypes.values().forEach(CUIType::tick);
	}

	private void tickEnd() {
		cuiTypes.values().forEach(CUIType::tickEnd);
		plugin.getCameraManager().tickEnd();
	}

	public void setup() {
		if (initialized) {
			throw new IllegalStateException("CUIManager has already been initialized");
		}
		initialized = true;
		// 如果不存在的话，创建目录./json
		var cuiFolder = new File(plugin.getDataFolder(), "json");
		if (!cuiFolder.exists()) {
			if (!cuiFolder.mkdirs()) {
				plugin.getLogger().warning("Failed to create json folder");
				plugin.getLogger().warning("Json related operations will be disabled and may throw exceptions");
			}
		}
		Bukkit.getPluginManager().registerEvents(this, plugin);
		scanPlugin(plugin);
		registerCUIsFromFiles();
	}

	public void teardown() {
		if (!initialized) {
			throw new IllegalStateException("CUIManager has not been initialized");
		}
		getCUIInstances().forEach(CUIInstance::destroy);
		for (CUIType<?> cuiType : getRegisteredCUITypes()) {
			if (!cuiType.isSerializable()) {
				return;
			}
		}
	}

	public void unregisterCUI(NamespacedKey key) {
		CUIType<?> cuiType = cuiTypes.remove(key);
		if (cuiType != null) {
			cuiType.onUnregister();
		}
	}

	private void register(NamespacedKey key, CUIType<?> type) {
		var event = new CUIRegisterEvent<>(type);
		Bukkit.getPluginManager().callEvent(event);

		cuiTypes.put(key, type);
		if (!type.isSerializable()) {
			cuiTypesByClass.put(type.getChestUI().getClass(), cuiTypes.get(key));
		}
		type.onRegister();
	}

	/**
	 * 如果你的{@link ChestUI}注解了{@link CUI}，则应当会被自动注册。如果因为某些原因未被注册，可以手动调用此方法注册。<br>
	 * If your {@link ChestUI} is annotated with {@link CUI}, it should be
	 * automatically registered. If not, you can manually
	 *
	 * @param chestUIClass
	 *            ChestUI的实现类<br>
	 *            The implementation class of ChestUI
	 * @param plugin
	 *            调用注册的插件<br>
	 *            The plugin that calls the registration
	 * @param name
	 *            CUI的名称<br>
	 *            The name of the CUI
	 * @param singleton
	 *            是否为单例<br>
	 *            Whether it is a singleton
	 * @param <T>
	 *            ChestUI的实现类<br>
	 *            The implementation class of ChestUI
	 */
	public <T extends ChestUI<T>> void registerCUI(Class<T> chestUIClass, Plugin plugin, String name, boolean singleton,
			Material icon) {
		var key = NamespacedKey.fromString(name, plugin);
		if (cuiTypes.containsKey(key)) {
			throw new IllegalArgumentException("CUI `" + key + "` has already been registered");
		}
		T chestUI;
		try {
			Constructor<T> constructor = chestUIClass.getConstructor();
			chestUI = constructor.newInstance();
		} catch (NoSuchMethodException | InstantiationException | IllegalAccessException
				| InvocationTargetException e) {
			throw new RuntimeException(
					"ChestUI `" + chestUIClass.getCanonicalName() + "` must have a public no-args constructor");
		}
		register(key, new CUIType<>(this.plugin, plugin, chestUI, key, singleton, icon));
	}

	@ApiStatus.Experimental
	@ApiStatus.Internal
	public SerializableChestUI registerCUI(CUIData data) {
		if (cuiTypes.containsKey(data.getKey())) {
			throw new IllegalArgumentException("CUI `" + data.getKey() + "` has already been registered");
		}
		var cui = new SerializableChestUI(data);
		register(data.getKey(), new CUIType<>(plugin, null, cui, data.getKey(), data.singleton, data.icon));
		return cui;
	}

	@ApiStatus.Experimental
	public SerializableChestUI registerCUI(File file) {
		if (file.getName().endsWith(".json")) {
			try (FileReader reader = new FileReader(file)) {
				var data = CUIData.fromJson(reader);
				return registerCUI(data);
			} catch (IOException e) {
				plugin.getLogger().warning("Failed to read CUI file: " + file.getName());
				e.printStackTrace();
			}
		}
		return null;
	}

	private void registerCUIsFromFiles() {
		// 遍历./json/下的json文件
		var folder = new File(plugin.getDataFolder(), "json");
		if (!folder.exists()) {
			return;
		}
		if (!folder.isDirectory()) {
			plugin.getLogger().warning("json folder is not a directory");
			return;
		}
		var files = folder.listFiles();
		if (files == null) {
			return;
		}
		var count = 0;
		for (File file : files) {
			if (!file.getName().endsWith(".json")) {
				continue;
			}
			try {
				registerCUI(file);
			} catch (Exception e) {
				plugin.getLogger().warning("Failed to register CUI from json file: " + file.getName());
				e.printStackTrace();
				continue;
			}
			count++;
		}
		plugin.getLogger().info("Registered " + count + " CUIs from json files");
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
			// 判断是否实现了ChestUI
			if (!ChestUI.class.isAssignableFrom(clazz)) {
				plugin.getLogger().warning("Class `" + clazz.getCanonicalName() + "` does not implement ChestUI");
				continue;
			}
			// 处理带有@CUI注解的类
			var annotation = clazz.getAnnotation(CUI.class);
			if (annotation.autoRegister()) {
				registerCUI((Class) clazz, plg, annotation.name(), annotation.singleton(), annotation.icon());
			}
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
	public void onTickStart(ServerTickStartEvent event) {
		tickStart();
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onTickEnd(ServerTickEndEvent event) {
		tick();
		tickEnd();
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onClose(InventoryCloseEvent event) {
		if (!(event.getPlayer() instanceof Player player))
			return;

		// TODO: fix double close
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
			// 点击的是上方界面
			var rawSlot = event.getRawSlot();
			camera.click(player, event.getClick(), event.getAction(), rawSlot / 9, rawSlot % 9, event.getCursor());
			event.setCancelled(true);
		} else {
			// 点击的是玩家背包
			// 防刷物品
			if (ItemStacks.hasTag(event.getCursor(), "cui")) {
				event.getWhoClicked().setItemOnCursor(ItemStack.empty());
				event.setCancelled(true);
				return;
			}
			if (ItemStacks.hasTag(event.getCurrentItem(), "cui")) {
				event.setCurrentItem(ItemStack.empty());
				event.setCancelled(true);
				return;
			}
			// 处理与CUI有关交互
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
		var cursor = ItemStacks.clone(event.getOldCursor());
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
			// 拖拽式放置时，如果都放置失败，则Layer不会置脏，但由于事件未被cancel，所以会显示放在了箱子里，因此需要刷新
			camera.markDirty();
			var remaining = camera.place(player, itemStack, rawSlot / 9, rawSlot % 9);
			if (!ItemStacks.isEmpty(remaining)) {
				amount.addAndGet(remaining.getAmount());
			}
		});
		cursor.setAmount(amount.get());
		event.setCursor(cursor);
	}
}
