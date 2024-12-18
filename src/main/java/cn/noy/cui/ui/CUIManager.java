package cn.noy.cui.ui;

import cn.noy.cui.CUIPlugin;
import cn.noy.cui.util.ItemStacks;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

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
	private final HashSet<ChestUI<?>> cuis = new HashSet<>();
	private final HashMap<Class<?>, Integer> maxId = new HashMap<>();
	private final Queue<Runnable> pending = new LinkedList<>();

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
		var cui = new ChestUI<>(plugin, handler, id);
		handler.onInitialize(cui);
		cuis.add(cui);
		return cui;
	}

	public List<ChestUI<?>> getCUIs() {
		return new ArrayList<>(cuis);
	}

	private void tick() {
		while (true) {
			var task = pending.poll();
			if (task == null) {
				break;
			}
			task.run();
		}

		cuis.forEach(cui -> cui.getTrigger().tick());
		Camera.Manager.forEachCamera(Camera::update);
	}

	public void setup() {
		if (task != null) {
			throw new IllegalStateException("CUIManager has already been initialized");
		}
		task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 0, 1);
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	public void teardown() {
		if (task == null) {
			throw new IllegalStateException("CUIManager has not been initialized");
		}
		task.cancel();
		task = null;
		new ArrayList<>(cuis).forEach(ChestUI::destroy);
	}

	void notifyDestroy(ChestUI<?> cui) {
		pending.add(() -> cuis.remove(cui));
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
