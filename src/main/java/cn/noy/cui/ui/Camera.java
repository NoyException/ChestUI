package cn.noy.cui.ui;

import cn.noy.cui.event.CUIAddItemEvent;
import cn.noy.cui.event.CUIClickEvent;
import cn.noy.cui.layer.Layer;
import cn.noy.cui.util.ItemStacks;
import cn.noy.cui.util.Position;

import com.google.common.collect.HashBiMap;
import net.kyori.adventure.text.Component;

import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;

public class Camera<T extends CUIHandler<T>> {
	// TODO: 将Manager从静态改为Plugin的成员变量
	public static class Manager {
		private static final HashBiMap<String, Camera<?>> CAMERAS = HashBiMap.create();
		private static final Map<Player, Stack<Camera<?>>> BY_PLAYER = new HashMap<>();

		public static void forEachCamera(Consumer<? super Camera<?>> action) {
			new ArrayList<>(CAMERAS.values()).forEach(action);
		}

		public static List<Camera<?>> getCameras() {
			return new ArrayList<>(CAMERAS.values());
		}

		public static Camera<?> getCamera(Player player) {
			var stack = BY_PLAYER.computeIfAbsent(player, p -> new Stack<>());
			if (stack.empty())
				return null;
			return stack.peek();
		}

		public static Camera<?> getCamera(String name) {
			return CAMERAS.get(name);
		}

		public static boolean closeTop(Player player, boolean force) {
			var stack = BY_PLAYER.computeIfAbsent(player, p -> new Stack<>());
			if (stack.empty())
				return true;
			var closed = stack.peek().close(player, force);
			if (!closed)
				return false;
			// 切换回来
			if (!stack.empty())
				stack.peek().viewers.add(player);
			return true;
		}

		public static boolean closeAll(Player player, boolean force) {
			var stack = BY_PLAYER.computeIfAbsent(player, p -> new Stack<>());
			if (stack.empty())
				return true;
			while (!stack.empty()) {
				var closed = stack.peek().close(player, force);
				if (!closed)
					return false;
			}
			BY_PLAYER.remove(player);
			return true;
		}
	}

	private final ChestUI<T> chestUI;
	private final int id;
	private final HashSet<Player> viewers = new HashSet<>();
	/**
	 * 用于保活的计数，玩家使用该相机则计数+1，不使用则计数-1，从本相机跳到别的相机计数不变（因为会跳回来）
	 */
	private final HashMap<Player, Integer> keepAliveCount = new HashMap<>();
	private final TreeMap<Integer, LayerWrapper> layers = new TreeMap<>();
	private final HashMap<Layer, Integer> layerPriority = new HashMap<>();
	private final InventoryHolder holder = new DummyHolder();
	private Position position;
	private int rowSize = 3, columnSize = 9;
	private HorizontalAlign horizontalAlign;
	private VerticalAlign verticalAlign;
	private boolean keepAlive;
	private boolean closable = true;
	private Component title;
	private Inventory inventory;

	private boolean recreate = true;
	private boolean dirty = true;
	private boolean destroyed;

	public Camera(ChestUI<T> chestUI, int id, Position position, int rowSize, int columnSize,
			HorizontalAlign horizontalAlign, VerticalAlign verticalAlign, Component title) {
		this.chestUI = chestUI;
		this.id = id;
		edit().setPosition(position).setRowSize(rowSize).setColumnSize(columnSize).setHorizontalAlign(horizontalAlign)
				.setVerticalAlign(verticalAlign).setTitle(title);
		Manager.CAMERAS.put(getName(), this);
	}

	public Editor edit() {
		return new Editor();
	}

	public ChestUI<T> getChestUI() {
		return chestUI;
	}

	public String getName() {
		return chestUI.getName() + "#" + id;
	}

	/**
	 * 获取所有正在使用该Camera或暂时切换到其他CUI的玩家<br>
	 * Get all players who are using this Camera or temporarily switched to other
	 * CUIs
	 *
	 * @return 玩家列表<br>
	 *         Player list
	 */
	public List<Player> getViewers() {
		return new ArrayList<>(viewers);
	}

	/**
	 * inventory随时都可能被刷新重建，故该方法仅用于测试<br>
	 * The inventory may be refreshed and rebuilt at any time, so this method is
	 * only used for testing
	 *
	 * @return inventory
	 */
	public Inventory getInventory() {
		return inventory;
	}

	public boolean isDefault() {
		return chestUI.getDefaultCamera() == this;
	}

	public NavigableMap<Integer, Layer> getActiveLayers() {
		var map = chestUI.getActiveLayers();
		layers.forEach((priority, wrapper) -> {
			if (wrapper.active) {
				map.put(priority, wrapper.layer);
			}
		});
		return map;
	}

	public @NotNull Position getPosition() {
		return position;
	}

	public int getRowSize() {
		return rowSize;
	}

	public int getColumnSize() {
		return columnSize;
	}

	public HorizontalAlign getHorizontalAlign() {
		return horizontalAlign;
	}

	public VerticalAlign getVerticalAlign() {
		return verticalAlign;
	}

	public boolean isClosable() {
		return closable;
	}

	public void setClosable(boolean closable) {
		this.closable = closable;
	}

	public boolean isKeepAlive() {
		return keepAlive;
	}

	public void setKeepAlive(boolean keepAlive) {
		this.keepAlive = keepAlive;
	}

	public Component getTitle() {
		return title;
	}

	public int getPriority(Layer layer) {
		return layerPriority.getOrDefault(layer, -1);
	}

	public Position getTopLeft() {
		var row = switch (verticalAlign) {
			case TOP -> position.row();
			case MIDDLE -> position.row() - rowSize / 2;
			case BOTTOM -> position.row() - rowSize;
		};
		var column = switch (horizontalAlign) {
			case LEFT -> position.column();
			case MIDDLE -> position.column() - columnSize / 2;
			case RIGHT -> position.column() - columnSize;
		};
		return new Position(row, column);
	}

	public boolean open(Player viewer) {
		return open(viewer, false);
	}

	// TODO: 测试切换Camera
	public boolean open(Player viewer, boolean asChild) {
		if (destroyed) {
			throw new IllegalStateException("Camera has been destroyed");
		}
		if (!asChild) {
			boolean success = Manager.closeAll(viewer, false);
			if (!success) {
				return false;
			}
		}
		viewers.add(viewer);
		keepAliveCount.compute(viewer, (player, count) -> count == null ? 1 : count + 1);
		var stack = Manager.BY_PLAYER.computeIfAbsent(viewer, player -> new Stack<>());
		if (!stack.empty()) {
			var parent = stack.peek();
			if (parent != null) {
				// 切换出去
				parent.viewers.remove(viewer);
			}
		}
		stack.push(this);
		return true;
	}

	/**
	 * 尝试为玩家关闭Camera。如果玩家不在看这个Camera，或者Camera不可关闭，则返回false<br>
	 *
	 * @param viewer
	 *            玩家
	 * @param force
	 *            是否强制关闭
	 * @return 是否成功关闭
	 */
	public boolean close(Player viewer, boolean force) {
		if (!force && !closable) {
			return false;
		}

		var exist = viewers.remove(viewer);
		if (!exist) {
			return false;
		}

		var stack = Manager.BY_PLAYER.get(viewer);
		if (stack == null || stack.empty()) {
			return false;
		}
		var popped = stack.pop();
		if (popped != this) {
			throw new RuntimeException("Camera not match");
		}
		if (!stack.empty()) {
			// 切换回来
			stack.peek().viewers.add(viewer);
		}

		keepAliveCount.compute(viewer, (player, count) -> {
			if (count == null) {
				return 0;
			}
			if (count <= 0) {
				throw new RuntimeException("Keep alive count is less than 0");
			}
			return count - 1;
		});
		keepAliveCount.remove(viewer, 0);

		if (viewer.getOpenInventory().getTopInventory().getHolder() == holder) {
			viewer.closeInventory();
		}
		return true;
	}

	public boolean closeCascade(Player viewer, boolean force) {
		var count = keepAliveCount.getOrDefault(viewer, 0);
		if (count <= 0) {
			return false;
		}
		var stack = Manager.BY_PLAYER.get(viewer);
		if (stack == null || stack.empty()) {
			return false;
		}
		while (count > 0) {
			try {
				var camera = stack.peek();
				var success = camera.close(viewer, force);
				if (!success) {
					return false;
				}
				if (camera == this) {
					count--;
				}
			} catch (EmptyStackException e) {
				chestUI.getPlugin().getComponentLogger()
						.error(Component.text("Player " + viewer.getName() + " has no camera to close"), e);
				return false;
			}
		}
		return true;
	}

	/**
	 * 销毁Camera。这会导致所有正在看这个Camera的玩家级联关闭。如果{@link #isDefault()}为真，则会同时销毁ChestUI。<br>
	 * Destroy the Camera. This will cause all players who are watching this Camera
	 * to close cascade. If {@link #isDefault()} is true, the ChestUI will also be
	 * destroyed.
	 */
	public void destroy() {
		new ArrayList<>(viewers).forEach(player -> closeCascade(player, true));
		chestUI.getTrigger().notifyReleaseCamera(this);
		Manager.CAMERAS.inverse().remove(this);
		destroyed = true;
	}

	public void sync(boolean force) {
		var activeLayers = getActiveLayers();
		if (!recreate && !dirty && !force) {
			if (activeLayers.values().stream().noneMatch(Layer::isDirty))
				return;
		}
		if (recreate) {
			inventory = Bukkit.createInventory(holder, rowSize * columnSize, title);
			recreate = false;
		}
		dirty = false;

		var contents = new CUIContents<>(this);
		activeLayers.descendingMap().values().forEach(layer -> layer.display(contents));

		for (int row = 0; row < rowSize; row++) {
			for (int column = 0; column < columnSize; column++) {
				var index = row * columnSize + column;
				inventory.setItem(index, contents.getItem(row, column));
			}
		}
	}

	public void update() {
		if (!keepAlive && keepAliveCount.isEmpty()) {
			// 如果是默认Camera，只要CUI保活，或者还有其他Camera存活，那么即使无人使用也不能销毁
			// 因为一但默认Camera被销毁，ChestUI会同步销毁
			if (!isDefault() || (!chestUI.isKeepAlive() && chestUI.getCameraCount() <= 1)) {
				destroy();
				return;
			}
		}

		if (viewers.isEmpty()) {
			// Layer的dirty只在某一tick生效，所以在没有玩家的情况下，需要手动置脏
			dirty = true;
			return;
		}

		sync(false);

		// openInventory可能会触发事件，从而导致ConcurrentModificationException
		new ArrayList<>(viewers).forEach(player -> {
			if (player.getOpenInventory().getTopInventory() != inventory) {
				player.openInventory(inventory);
			}
		});
	}

	public void tick() {
		for (LayerWrapper wrapper : layers.values()) {
			if (wrapper.active) {
				wrapper.layer.tick();
			}
		}
	}

	/**
	 * 点击事件处理<br>
	 * Click event processing
	 *
	 * @param player
	 *            玩家<br>
	 *            Player
	 * @param clickType
	 *            点击类型<br>
	 *            Click type
	 * @param action
	 *            点击行为<br>
	 *            Click action
	 * @param row
	 *            相对于Camera的行<br>
	 *            Relative to Camera row
	 * @param column
	 *            相对于Camera的列<br>
	 *            Relative to Camera column
	 * @param cursor
	 *            鼠标上的物品<br>
	 *            Item on cursor
	 */
	public void click(Player player, ClickType clickType, InventoryAction action, int row, int column,
			ItemStack cursor) {
		var topLeft = getTopLeft();
		var event = new CUIClickEvent<>(this, player, clickType, action,
				new Position(row + topLeft.row(), column + topLeft.column()), cursor);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled()) {
			return;
		}

		// 深度低的先click
		for (Layer layer : getActiveLayers().values()) {
			layer.click(event);
			if (event.isCancelled()) {
				player.setItemOnCursor(event.getCursor());
				return;
			}
		}
	}

	/**
	 * 放置物品<br>
	 * Place item
	 *
	 * @param player
	 *            玩家<br>
	 *            Player
	 * @param itemStack
	 *            物品<br>
	 *            ItemStack
	 * @param row
	 *            相对于Camera的行<br>
	 *            Relative to Camera row
	 * @param column
	 *            相对于Camera的列<br>
	 *            Relative to Camera column
	 * @return 剩余物品<br>
	 *         Remaining ItemStack
	 */
	public ItemStack place(Player player, ItemStack itemStack, int row, int column) {
		if (ItemStacks.isEmpty(itemStack)) {
			return null;
		}

		// 拖拽式放置时，如果都放置失败，则Layer不会置脏，但由于事件未被cancel，所以会显示放在了箱子里，因此需要刷新
		dirty = true;
		// 深度低的先place
		// TODO: 改为事件驱动
		for (Layer layer : getActiveLayers().values()) {
			var slot = layer.getRelativeSlot(this, row, column);
			if (slot == null) {
				continue;
			}
			itemStack = slot.place(itemStack, player);
			if (ItemStacks.isEmpty(itemStack)) {
				return null;
			}
		}
		return itemStack;
	}

	/**
	 * 尝试添加物品。仅添加到Camera视野范围内的物品槽<br>
	 * Try to add items. Only add to the item slots within the Camera's field of
	 * view
	 *
	 * @param player
	 *            玩家<br>
	 *            Player
	 * @param itemStack
	 *            物品<br>
	 *            ItemStack
	 * @return 剩余物品，如果没有剩余则返回null<br>
	 *         Remaining ItemStack, return null if there is no remaining
	 */
	public ItemStack addItem(Player player, ItemStack itemStack) {
		if (ItemStacks.isEmpty(itemStack)) {
			return null;
		}

		var event = new CUIAddItemEvent<>(this, player, itemStack);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled()) {
			return itemStack;
		}
		itemStack = event.getItemStack();
		if (ItemStacks.isEmpty(itemStack)) {
			return null;
		}

		// 深度低的先addItem
		var activeLayers = getActiveLayers().values();
		for (int row = 0; row < rowSize; row++) {
			for (int column = 0; column < columnSize; column++) {
				for (Layer layer : activeLayers) {
					var slot = layer.getRelativeSlot(this, row, column);
					if (slot == null) {
						continue;
					}
					itemStack = slot.place(itemStack, player);
					if (ItemStacks.isEmpty(itemStack)) {
						return null;
					}
				}
			}
		}
		return itemStack;
	}

	/**
	 * 收集物品（双击物品时触发），仅收集Camera视野范围内的物品<br>
	 * Collect items (triggered when double-clicking items), only collect items
	 * within the Camera's field of view
	 *
	 * @param player
	 *            玩家<br>
	 *            The player
	 * @param itemStack
	 *            待收集的物品<br>
	 *            The item to collect
	 * @param collectBackpack
	 *            是否收集背包中的物品<br>
	 *            Whether to collect items in the backpack
	 * @return 收集到的物品，如果没有收集到则返回null<br>
	 *         The collected item, if there is no collected item, return null
	 */
	public ItemStack collect(Player player, ItemStack itemStack, boolean collectBackpack) {
		if (ItemStacks.isEmpty(itemStack)) {
			return null;
		}
		interface Collectable {
			ItemStack collect(ItemStack itemStack);
		}
		var list = new ArrayList<Pair<Integer, Collectable>>();

		// TODO: 改为事件驱动
		var activeLayers = getActiveLayers().values();
		for (int row = 0; row < rowSize; row++) {
			for (int column = 0; column < columnSize; column++) {
				for (Layer layer : activeLayers) {
					var slot = layer.getRelativeSlot(this, row, column);
					if (slot == null) {
						continue;
					}
					var inSlot = slot.get();
					if (ItemStacks.isEmpty(inSlot)) {
						continue;
					}
					list.add(Pair.of(inSlot.getAmount(), itemStack1 -> slot.collect(itemStack1, player)));
				}
			}
		}

		if (collectBackpack) {
			var inventory = player.getInventory();
			for (int slot = 0; slot < inventory.getSize(); slot++) {
				var item = inventory.getItem(slot);
				if (ItemStacks.isEmpty(item)) {
					continue;
				}
				int finalSlot = slot;
				list.add(Pair.of(item.getAmount(), itemStack1 -> {
					var result = ItemStacks.place(itemStack1, item, false);
					itemStack1 = result.placed();
					inventory.setItem(finalSlot, result.remaining());
					return itemStack1;
				}));
			}
		}

		list.sort(Comparator.comparingInt(Pair::getLeft));
		for (var pair : list) {
			itemStack = pair.getRight().collect(itemStack);
			if (ItemStacks.isFull(itemStack)) {
				return itemStack;
			}
		}
		return itemStack;
	}

	public Camera<T> deepClone() {
		var clone = new Camera<>(chestUI, chestUI.getNextCameraId(), position, rowSize, columnSize, horizontalAlign,
				verticalAlign, title);
		layers.forEach((priority, layerWrapper) -> clone.edit().setLayer(priority, layerWrapper.layer.deepClone()));
		return clone;
	}

	public enum HorizontalAlign {
		LEFT, MIDDLE, RIGHT
	}

	public enum VerticalAlign {
		TOP, MIDDLE, BOTTOM
	}

	private static class LayerWrapper {
		private final Layer layer;
		private boolean active = true;

		LayerWrapper(Layer layer) {
			this.layer = layer;
		}
	}

	public class Editor {
		public Camera<T> finish() {
			return Camera.this;
		}

		public Editor setPosition(Position position) {
			Camera.this.position = position;
			dirty = true;
			return this;
		}

		public Editor move(int row, int column) {
			Camera.this.position = position.add(row, column);
			dirty = true;
			return this;
		}

		public Editor setRowSize(int rowSize) {
			if (rowSize < 1 || rowSize > 6) {
				throw new IllegalArgumentException("Row size must be between 1 and 6");
			}
			Camera.this.rowSize = rowSize;
			recreate = true;
			dirty = true;
			return this;
		}

		public Editor setColumnSize(int columnSize) {
			if (columnSize != 9) {
				throw new IllegalArgumentException("Column size must be 9");
			}
			Camera.this.columnSize = columnSize;
			recreate = true;
			dirty = true;
			return this;
		}

		public Editor setHorizontalAlign(HorizontalAlign horizontalAlign) {
			Camera.this.horizontalAlign = horizontalAlign;
			dirty = true;
			return this;
		}

		public Editor setVerticalAlign(VerticalAlign verticalAlign) {
			Camera.this.verticalAlign = verticalAlign;
			dirty = true;
			return this;
		}

		public Editor setTitle(Component title) {
			Camera.this.title = title;
			recreate = true;
			dirty = true;
			return this;
		}

		public Editor setLayer(int priority, Layer layer) {
			var legacy = layers.put(priority, new LayerWrapper(layer));
			if (legacy != null) {
				layerPriority.remove(legacy.layer);
			}
			layerPriority.put(layer, priority);
			dirty = true;
			return this;
		}

		public Editor removeMask(int priority) {
			var wrapper = layers.remove(priority);
			if (wrapper != null) {
				layerPriority.remove(wrapper.layer);
			}
			dirty = true;
			return this;
		}

		public Editor setActive(int priority, boolean active) {
			var wrapper = layers.get(priority);
			if (wrapper != null) {
				wrapper.active = active;
				dirty = true;
			}
			return this;
		}

		public Editor setActive(Layer layer, boolean active) {
			var priority = getPriority(layer);
			if (priority >= 0) {
				layers.get(priority).active = active;
				dirty = true;
			}
			return this;
		}

		public Editor setKeepAlive(boolean keepAlive) {
			Camera.this.keepAlive = keepAlive;
			return this;
		}

		public Editor setRecreate(boolean recreate) {
			Camera.this.recreate = recreate;
			dirty = true;
			return this;
		}
	}

	private class DummyHolder implements InventoryHolder {
		@Override
		public @NotNull Inventory getInventory() {
			return inventory;
		}
	}
}
