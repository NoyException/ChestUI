package cn.noy.cui.ui;

import cn.noy.cui.CUIPlugin;
import cn.noy.cui.util.ItemStacks;

import com.google.common.collect.HashBiMap;
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
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CUIManager implements Listener {
	private final CUIPlugin plugin;

	public CUIManager(CUIPlugin plugin) {
		this.plugin = plugin;
	}

	private BukkitTask task;
	private final HashBiMap<NamespacedKey, Class<?>> cuiClasses = HashBiMap.create();
	private final HashBiMap<String, ChestUI<?>> cuis = HashBiMap.create();
	private final HashMap<Class<?>, Integer> maxId = new HashMap<>();
	private final Queue<Runnable> pending = new LinkedList<>();

	public List<ChestUI<?>> getCUIs() {
		return new ArrayList<>(cuis.values());
	}

	public List<ChestUI<?>> getCUIs(NamespacedKey key) {
		return cuis.values().stream().filter(cui -> cui.getName().startsWith(key.toString())).toList();
	}

	public List<NamespacedKey> getRegisteredCUINames() {
		return new ArrayList<>(cuiClasses.keySet());
	}

	public List<NamespacedKey> getRegisteredCUINames(Plugin plugin) {
		return cuiClasses.keySet().stream()
				.filter(namespacedKey -> namespacedKey.getNamespace().equalsIgnoreCase(plugin.getName())).toList();
	}

	public ChestUI<?> createCUI(Plugin plugin, String name) {
		return createCUI(NamespacedKey.fromString(name, plugin));
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public ChestUI<?> createCUI(NamespacedKey key) {
		var clazz = cuiClasses.get(key);
		if (clazz == null) {
			return null;
		}
		return createCUI((Class) clazz);
	}

	public <T extends CUIHandler<T>> ChestUI<T> createCUI(@NotNull Class<T> handlerClass) {
		T handler;
		try {
			Constructor<T> constructor = handlerClass.getConstructor();
			handler = constructor.newInstance();
		} catch (NoSuchMethodException | InstantiationException | IllegalAccessException
				| InvocationTargetException e) {
			throw new RuntimeException(
					"CUI Handler `" + handlerClass.getCanonicalName() + "` must have a public no-args constructor");
		}
		var id = maxId.getOrDefault(handlerClass, 1);
		maxId.put(handlerClass, id + 1);
		var key = cuiClasses.inverse().get(handlerClass);
		var name = key != null ? key.toString() : handlerClass.getCanonicalName();
		var cui = new ChestUI<>(plugin, handler, name, id);
		cuis.put(cui.getName(), cui);
		return cui;
	}

	public ChestUI<?> getCUI(String name) {
		return cuis.get(name);
	}

	private void tick() {
		while (true) {
			var task = pending.poll();
			if (task == null) {
				break;
			}
			task.run();
		}

		cuis.values().forEach(cui -> cui.getTrigger().tick());
		Camera.Manager.forEachCamera(Camera::update);
	}

	public void setup() {
		if (task != null) {
			throw new IllegalStateException("CUIManager has already been initialized");
		}
		task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 0, 1);
		Bukkit.getPluginManager().registerEvents(this, plugin);
		scanPlugin(plugin);
	}

	public void teardown() {
		if (task == null) {
			throw new IllegalStateException("CUIManager has not been initialized");
		}
		task.cancel();
		task = null;
		getCUIs().forEach(ChestUI::destroy);
	}

	void notifyDestroy(ChestUI<?> cui) {
		pending.add(() -> cuis.inverse().remove(cui));
	}

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
			// 处理带有@CUISize注解的类
			CUI annotation = clazz.getAnnotation(CUI.class);
			cuiClasses.put(NamespacedKey.fromString(annotation.value(), plg), clazz);
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

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onClose(InventoryCloseEvent event) {
		if (!(event.getPlayer() instanceof Player player))
			return;

		switch (event.getReason()) {
			case PLAYER -> Camera.Manager.closeTop(player, false);
			case DISCONNECT -> Camera.Manager.closeAll(player, true);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onClick(InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player player))
			return;

		var camera = Camera.Manager.getCamera(player);
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

		var camera = Camera.Manager.getCamera(player);
		if (camera == null)
			return;

		var view = event.getView();
		var cursor = event.getOldCursor().clone();
		var amount = new AtomicInteger();
		if (!ItemStacks.isEmpty(event.getCursor())) {
			amount.set(event.getCursor().getAmount());
		}

		event.getNewItems().forEach((rawSlot, itemStack) -> {
			if (view.convertSlot(rawSlot) != rawSlot)
				return;

			var remaining = camera.place(player, itemStack, rawSlot / 9, rawSlot % 9);
			if (!ItemStacks.isEmpty(remaining)) {
				amount.addAndGet(remaining.getAmount());
			}
		});
		cursor.setAmount(amount.get());
		event.setCursor(cursor);
	}
}
