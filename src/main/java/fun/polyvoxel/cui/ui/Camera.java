package fun.polyvoxel.cui.ui;

import fun.polyvoxel.cui.event.*;
import fun.polyvoxel.cui.layer.Layer;
import fun.polyvoxel.cui.slot.Slot;
import fun.polyvoxel.cui.ui.source.DisplaySource;
import fun.polyvoxel.cui.util.ItemStacks;
import fun.polyvoxel.cui.util.Position;

import net.kyori.adventure.text.Component;

import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class Camera<T extends ChestUI<T>> extends Viewable {
	private final CameraManager manager;
	private final CUIInstance<T> cuiInstance;
	private final CameraHandler<T> handler;
	private final int id;
	private final HashMap<Player, ViewerInfo> viewers = new HashMap<>();
	private final TreeMap<Integer, LayerWrapper> layers = new TreeMap<>();
	private final HashMap<Layer, Integer> layerDepths = new HashMap<>();
	private final InventoryHolder holder = new DummyHolder();
	private Position position = new Position(0, 0);
	private int rowSize = 6, columnSize = 9;
	private HorizontalAlign horizontalAlign = HorizontalAlign.LEFT;
	private VerticalAlign verticalAlign = VerticalAlign.TOP;
	private boolean keepAlive;
	private boolean closable = true;
	private Component title;
	private CUIContents<T> contents;
	private Inventory inventory;

	private long ticksLived;
	private boolean recreate = true;
	private boolean dirty = true;
	private State state = State.UNINITIALIZED;

	Camera(CUIInstance<T> cuiInstance, CameraHandler<T> handler, int id) {
		super(cuiInstance.getCUIPlugin());
		this.cuiInstance = cuiInstance;
		this.id = id;
		this.title = cuiInstance.getDefaultTitle();
		this.manager = cuiInstance.getCUIPlugin().getCameraManager();
		this.manager.registerCamera(this);
		this.handler = handler;
		this.handler.onInitialize(this);
		sync(true);
		state = State.READY;
	}

	public CameraManager getManager() {
		return manager;
	}

	public CUIInstance<T> getCUIInstance() {
		return cuiInstance;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return cuiInstance.getName() + "#" + id;
	}

	public CameraHandler<T> getHandler() {
		return handler;
	}

	public void markDirty() {
		dirty = true;
	}

	/**
	 * 获取所有正在使用该Camera的玩家。需要注意的是，切换到别的摄像头的玩家也会在列表里。<br>
	 * Get all players who are using this Camera. It should be noted that players
	 * who switch to other cameras will also be in the list.
	 *
	 * @return 玩家列表<br>
	 *         Player list
	 */
	public List<Player> getViewers() {
		return new ArrayList<>(viewers.keySet());
	}

	public int getViewerCount() {
		return viewers.size();
	}

	/**
	 * 获取Camera看到的内容快照。任何对其的修改均不会反映到实际。<br>
	 * Get a snapshot of the contents seen by the Camera. Any modifications to it
	 * will not be reflected in reality.
	 * 
	 * @return 内容快照<br>
	 *         Contents snapshot
	 */
	public CUIContents<T> getContents() {
		return contents;
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

	public NavigableMap<Integer, Layer> getActiveLayers() {
		var map = cuiInstance.getActiveLayers();
		layers.forEach((depth, wrapper) -> {
			if (wrapper.active) {
				map.put(depth, wrapper.layer);
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

	public long getTicksLived() {
		return ticksLived;
	}

	public State getState() {
		return state;
	}

	public Component getTitle() {
		return title;
	}

	public int getDepth(Layer layer) {
		return layerDepths.getOrDefault(layer, -1);
	}

	public Layer getLayer(int depth) {
		var wrapper = layers.get(depth);
		return wrapper == null ? null : wrapper.layer;
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

	@Override
	public void notifySwitchOut(Player viewer) {
		var info = viewers.get(viewer);
		if (info == null) {
			getCUIPlugin().getLogger().warning("ViewerInfo is null");
			return;
		}
		info.viewing = false;
	}

	@Override
	public boolean canOpen(Player viewer) {
		return state == State.READY && handler.canOpen(viewer);
	}

	protected void doOpen(Player viewer, boolean asChild, @Nullable DisplaySource<?> source) {
		handler.onOpen(viewer);
		ViewerInfo info = viewers.computeIfAbsent(viewer, ViewerInfo::new);
		info.sources.push(source);
		info.viewing = true;
	}

	@Override
	public void notifySwitchBack(Player viewer) {
		var info = viewers.get(viewer);
		if (info == null) {
			getCUIPlugin().getLogger().warning("ViewerInfo is null");
			return;
		}
		info.viewing = true;
	}

	@Override
	public boolean canClose(Player viewer) {
		return closable && handler.canClose(viewer);
	}

	protected void doClose(Player viewer) {
		handler.onClose(viewer);
		var info = viewers.get(viewer);
		if (info == null) {
			getCUIPlugin().getLogger().warning("ViewerInfo is null");
			return;
		}
		info.sources.pop();
		info.viewing = false;
		if (info.sources.empty()) {
			viewers.remove(viewer);
		}

		Inventory topInventory = viewer.getOpenInventory().getTopInventory();
		// MockBukkit中topInventory可能为null
		if (topInventory != null && topInventory.getHolder() == holder) {
			viewer.closeInventory(InventoryCloseEvent.Reason.PLUGIN);
		}
	}

	public boolean closeCompletely(Player viewer, boolean force) {
		var info = viewers.get(viewer);
		if (info == null) {
			getCUIPlugin().getLogger().warning("ViewerInfo is null");
			return true;
		}
		while (!info.sources.empty()) {
			if (!close(viewer, force, true)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 销毁Camera。这会导致所有正在看这个Camera的玩家级联关闭。<br>
	 * Destroy the Camera. This will cause all players who are watching this Camera
	 * to close cascade.
	 */
	public void destroy() {
		if (state == State.DESTROYING || state == State.DESTROYED) {
			return;
		}
		state = State.DESTROYING;
		handler.onDestroy();
		getViewers().forEach(player -> closeCompletely(player, true));
		manager.unregisterCamera(this);
		state = State.DESTROYED;
	}

	public void sync(boolean force) {
		var activeLayers = getActiveLayers();
		if (!recreate && !dirty && !cuiInstance.isDirty() && !force) {
			if (activeLayers.values().stream().noneMatch(Layer::isDirty))
				return;
		}
		if (recreate) {
			inventory = Bukkit.createInventory(holder, rowSize * columnSize, title);
			recreate = false;
		}
		dirty = false;

		contents = new CUIContents<>(this);
		activeLayers.descendingMap().values().forEach(layer -> layer.display(contents));

		for (int row = 0; row < rowSize; row++) {
			for (int column = 0; column < columnSize; column++) {
				var index = row * columnSize + column;
				inventory.setItem(index, contents.getItem(row, column));
			}
		}
	}

	public void tickStart() {
		ticksLived++;
		handler.onTickStart();

		for (LayerWrapper wrapper : layers.values()) {
			if (wrapper.active) {
				wrapper.layer.tickStart();
			}
		}
	}

	public void tick() {
		if (!keepAlive && viewers.isEmpty()) {
			destroy();
			return;
		}

		handler.onTick();

		for (LayerWrapper wrapper : layers.values()) {
			if (wrapper.active) {
				wrapper.layer.tick();
			}
		}
	}

	public void tickEnd() {
		handler.onTickEnd();

		if (viewers.values().stream().noneMatch(info -> info.viewing)) {
			// Layer的dirty只在某一tick生效，所以在没有玩家的情况下，需要手动置脏
			dirty = true;
		} else {
			sync(false);
		}

		for (LayerWrapper wrapper : layers.values()) {
			if (wrapper.active) {
				wrapper.layer.tickEnd();
			}
		}
	}

	@Override
	public InventoryView keepOpening(Player viewer) {
		if (state != State.READY) {
			return null;
		}
		var info = viewers.get(viewer);
		if (info == null) {
			getCUIPlugin().getLogger().warning("ViewerInfo is null");
			return null;
		}
		if (!info.viewing) {
			return null;
		}
		var view = viewer.getOpenInventory();
		if (view.getTopInventory() != inventory) {
			handler.onOpenInventory(viewer, inventory);
			viewer.openInventory(inventory);
			return view;
		}
		return null;
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
		if (state != State.READY) {
			throw new IllegalStateException("Camera is not ready");
		}

		var event = new CUIClickEvent<>(this, player, clickType, action, new Position(row, column), cursor);

		// 深度低的先click
		for (Layer layer : getActiveLayers().values()) {
			var slot = layer.getRelativeSlot(this, row, column);
			if (slot != null && slot.prepareClick(event)) {
				break;
			}
		}

		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled() || event.getSlot() == null) {
			return;
		}

		event.getSlot().click(event);
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
		if (state != State.READY) {
			throw new IllegalStateException("Camera is not ready");
		}

		if (ItemStacks.isEmpty(itemStack)) {
			return null;
		}

		var event = new CUIPlaceItemEvent<>(this, player, Position.of(row, column), itemStack);
		// 深度低的先place
		for (Layer layer : getActiveLayers().values()) {
			var slot = layer.getRelativeSlot(this, row, column);
			if (slot != null && slot.preparePlace(event)) {
				break;
			}
		}

		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled()) {
			return itemStack;
		}
		if (event.getSlot() == null) {
			return event.getItemStack();
		}
		return event.getSlot().place(event);
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
		if (state != State.READY) {
			throw new IllegalStateException("Camera is not ready");
		}

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

		for (int row = 0; row < rowSize; row++) {
			for (int column = 0; column < columnSize; column++) {
				itemStack = place(player, itemStack, row, column);
				if (ItemStacks.isEmpty(itemStack)) {
					return null;
				}
			}
		}
		return itemStack;
	}

	/**
	 * 捡起物品<br>
	 * Pick item
	 *
	 * @param player
	 *            玩家<br>
	 *            Player
	 * @param row
	 *            相对于Camera的行<br>
	 *            Relative to Camera row
	 * @param column
	 *            相对于Camera的列<br>
	 *            Relative to Camera column
	 * @param cursor
	 *            鼠标上的物品<br>
	 *            Item on cursor
	 * @return 获得的物品<br>
	 *         Obtained ItemStack
	 */
	public ItemStack pick(Player player, int row, int column, ItemStack cursor) {
		if (state != State.READY) {
			throw new IllegalStateException("Camera is not ready");
		}

		var event = new CUIPickItemEvent<>(this, player, Position.of(row, column), cursor);
		for (Layer layer : getActiveLayers().values()) {
			var slot = layer.getRelativeSlot(this, row, column);
			if (slot != null && slot.preparePick(event)) {
				break;
			}
		}

		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled()) {
			return cursor;
		}
		if (event.getSlot() == null) {
			return event.getCursor();
		}
		return event.getSlot().pick(event);
	}

	/**
	 * 收集物品（双击物品时触发），仅收集Camera视野范围内的物品。显示数量少的槽位将会被优先收集。<br>
	 * Collect items (triggered when double-clicking items), only collect items
	 * within the Camera's field of view. Slots with fewer displayed items will be
	 * collected first.
	 *
	 * @param player
	 *            玩家<br>
	 *            The player
	 * @param cursor
	 *            鼠标上的物品<br>
	 *            Item on cursor
	 * @param includeBackpack
	 *            是否收集背包中的物品<br>
	 *            Whether to collect items in the backpack
	 * @return 收集到的物品，如果没有收集到则返回null<br>
	 *         The collected item, if there is no collected item, return null
	 */
	public ItemStack collect(Player player, ItemStack cursor, boolean includeBackpack) {
		if (state != State.READY) {
			throw new IllegalStateException("Camera is not ready");
		}

		if (ItemStacks.isEmpty(cursor)) {
			return null;
		}

		var event = new CUICollectItemEvent<>(this, player, cursor, includeBackpack);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled()) {
			return cursor;
		}
		cursor = event.getCursor();
		includeBackpack = event.isIncludeBackpack();

		@FunctionalInterface
		interface Collectable {
			ItemStack collect(ItemStack itemStack);
		}
		var list = new ArrayList<Pair<Integer, Collectable>>();

		for (int row = 0; row < rowSize; row++) {
			for (int column = 0; column < columnSize; column++) {
				var amount = ItemStacks.getAmount(contents.getItem(row, column));
				if (amount == 0) {
					continue;
				}
				int finalRow = row;
				int finalColumn = column;
				list.add(Pair.of(amount, itemStack1 -> pick(player, finalRow, finalColumn, itemStack1)));
			}
		}

		if (includeBackpack) {
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
			cursor = pair.getRight().collect(cursor);
			if (ItemStacks.isFull(cursor)) {
				return cursor;
			}
		}
		return cursor;
	}

	public void dropAll(@NotNull Location location) {
		dropAll(null, location);
	}

	public void dropAll(@NotNull Player player) {
		dropAll(player, player.getEyeLocation());
	}

	public void dropAll(@Nullable Player player, @NotNull Location location) {
		var activeLayers = getActiveLayers().values();
		var drops = new HashMap<Slot, ItemStack>();
		var event = new CUIDropAllEvent<>(this, player, location, drops);
		for (Layer layer : activeLayers) {
			layer.prepareDrop(event);
		}

		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled()) {
			return;
		}
		location = event.getLocation();

		event.getDrops().keySet().forEach(slot -> slot.set(null));
		for (ItemStack itemStack : event.getDrops().values()) {
			if (ItemStacks.isEmpty(itemStack)) {
				continue;
			}
			location.getWorld().dropItemNaturally(location, itemStack);
		}
	}

	public enum HorizontalAlign {
		LEFT, MIDDLE, RIGHT
	}

	public enum VerticalAlign {
		TOP, MIDDLE, BOTTOM
	}

	public enum State {
		UNINITIALIZED, READY, DESTROYING, DESTROYED
	}

	private static class LayerWrapper {
		private final Layer layer;
		private boolean active = true;

		LayerWrapper(Layer layer) {
			this.layer = layer;
		}

		@Deprecated
		public LayerWrapper deepClone() {
			var clone = new LayerWrapper(layer.deepClone());
			clone.active = active;
			return clone;
		}
	}

	private static class ViewerInfo {
		private final Player player;
		/**
		 * 是否正在查看。玩家有可能在查看时切换到别的相机，此时该值为false。<br>
		 * Whether viewing. The player may switch to another camera while viewing, in
		 * which case this value is false.
		 */
		private boolean viewing;
		private final Stack<DisplaySource<?>> sources = new Stack<>();

		private ViewerInfo(Player player) {
			this.player = player;
		}
	}

	private class DummyHolder implements InventoryHolder {
		@Override
		public @NotNull Inventory getInventory() {
			return inventory;
		}
	}

	public Editor edit() {
		return new Editor();
	}

	public class Editor {
		private Editor() {
		}

		public Camera<T> done() {
			return Camera.this;
		}

		public Editor position(Position position) {
			Camera.this.position = position;
			dirty = true;
			return this;
		}

		public Editor move(int row, int column) {
			Camera.this.position = position.add(row, column);
			dirty = true;
			return this;
		}

		public Editor rowSize(int rowSize) {
			if (rowSize < 1 || rowSize > 6) {
				throw new IllegalArgumentException("Row size must be between 1 and 6");
			}
			Camera.this.rowSize = rowSize;
			recreate = true;
			dirty = true;
			return this;
		}

		public Editor columnSize(int columnSize) {
			if (columnSize != 9) {
				throw new IllegalArgumentException("Column size must be 9");
			}
			Camera.this.columnSize = columnSize;
			recreate = true;
			dirty = true;
			return this;
		}

		public Editor horizontalAlign(HorizontalAlign horizontalAlign) {
			Camera.this.horizontalAlign = horizontalAlign;
			dirty = true;
			return this;
		}

		public Editor verticalAlign(VerticalAlign verticalAlign) {
			Camera.this.verticalAlign = verticalAlign;
			dirty = true;
			return this;
		}

		public Editor title(Component title) {
			Camera.this.title = title;
			recreate = true;
			dirty = true;
			return this;
		}

		public Editor layer(int depth, Layer layer) {
			var legacy = layers.put(depth, new LayerWrapper(layer));
			if (legacy != null) {
				layerDepths.remove(legacy.layer);
			}
			layerDepths.put(layer, depth);
			dirty = true;
			return this;
		}

		public Editor removeLayer(int depth) {
			var wrapper = layers.remove(depth);
			if (wrapper != null) {
				layerDepths.remove(wrapper.layer);
			}
			dirty = true;
			return this;
		}

		public Editor layerActive(int depth, boolean active) {
			var wrapper = layers.get(depth);
			if (wrapper != null) {
				wrapper.active = active;
				dirty = true;
			}
			return this;
		}

		public Editor layerActive(Layer layer, boolean active) {
			var depth = getDepth(layer);
			if (depth >= 0) {
				layers.get(depth).active = active;
				dirty = true;
			}
			return this;
		}

		public Editor keepAlive(boolean keepAlive) {
			Camera.this.keepAlive = keepAlive;
			return this;
		}

		public Editor recreate(boolean recreate) {
			Camera.this.recreate = recreate;
			dirty = true;
			return this;
		}
	}
}
