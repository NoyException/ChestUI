package cn.noy.cui.ui;

import cn.noy.cui.layer.Layer;
import cn.noy.cui.util.Position;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ChestUI<T extends CUIHandler> {
	private final HashMap<Player, PlayerInfo> viewers = new HashMap<>();
	private final HashSet<Camera<T>> cameras = new HashSet<>();
	private State state = State.UNINITIALIZED;

	private final int maxRow;
	private final int maxColumn;
	private final int maxDepth;
	private final TreeMap<Integer, LayerWrapper> layers = new TreeMap<>();
	private final HashMap<Layer, Integer> layerDepths = new HashMap<>();
	private final Camera<T> defaultCamera;
	private Component defaultTitle;
	private CUIContents contents;

	private boolean closable = true;
	private boolean keepAlive;
	private long ticks;

	private final T handler;
	private final Trigger trigger;
	private boolean dirty;

	ChestUI(@NotNull T handler) {
		this.handler = handler;
		this.trigger = new Trigger();

		var clazz = handler.getClass();
		if (clazz.isAnnotationPresent(CUISize.class)) {
			var chestSize = clazz.getAnnotation(CUISize.class);
			maxRow = chestSize.maxRow();
			maxColumn = chestSize.maxColumn();
			maxDepth = chestSize.maxDepth();
		} else {
			maxRow = -1;
			maxColumn = -1;
			maxDepth = -1;
		}
		if (clazz.isAnnotationPresent(CUITitle.class)) {
			var chestTitle = clazz.getAnnotation(CUITitle.class);
			defaultTitle = LegacyComponentSerializer.legacyAmpersand().deserialize(chestTitle.value());
		} else {
			defaultTitle = Component.empty();
		}
		if (clazz.isAnnotationPresent(DefaultCamera.class)) {
			var def = clazz.getAnnotation(DefaultCamera.class);
			defaultCamera = new Camera<>(this, new Position(def.row(), def.column()), def.rowSize(), def.columnSize(),
					def.horizontalAlign(), def.verticalAlign(), defaultTitle);
		} else {
			defaultCamera = new Camera<>(this, new Position(0, 0), 3, 9, Camera.HorizontalAlign.LEFT,
					Camera.VerticalAlign.TOP, defaultTitle);
		}
		cameras.add(defaultCamera);
	}

	public Trigger getTrigger() {
		return trigger;
	}

	@SuppressWarnings("unchecked")
	public Class<T> getHandlerClass() {
		return (Class<T>) handler.getClass();
	}

	public Camera<T> getDefaultCamera() {
		return defaultCamera;
	}

	public Camera<T> newCamera(Position position, int rowSize, int columnSize, Camera.HorizontalAlign horizontalAlign,
			Camera.VerticalAlign verticalAlign) {
		var camera = new Camera<>(this, position, rowSize, columnSize, horizontalAlign, verticalAlign, defaultTitle);
		cameras.add(camera);
		return camera;
	}

	public Camera<T> getCamera(Player viewer) {
		var info = viewers.get(viewer);
		if (info == null) {
			return null;
		}
		return info.camera;
	}

	public List<Camera<T>> getCameras() {
		return new ArrayList<>(cameras);
	}

	public List<Player> getViewers() {
		return new ArrayList<>(viewers.keySet());
	}

	public Component getDefaultTitle() {
		return defaultTitle;
	}

	public State getState() {
		return state;
	}

	public int getMaxRow() {
		return maxRow;
	}

	public int getMaxColumn() {
		return maxColumn;
	}

	public int getMaxDepth() {
		return maxDepth;
	}

	public Layer getLayer(int index) {
		return layers.get(index).layer;
	}

	public int getLayerDepth(Layer layer) {
		return layerDepths.getOrDefault(layer, -1);
	}

	public List<Layer> getActiveLayers(boolean ascending) {
		if (ascending) {
			return layers.values().stream().filter(layerWrapper -> layerWrapper.active)
					.map(layerWrapper -> layerWrapper.layer).toList();
		}
		return layers.descendingMap().values().stream().filter(layerWrapper -> layerWrapper.active)
				.map(layerWrapper -> layerWrapper.layer).toList();
	}

	public boolean isActive(int depth) {
		return layers.get(depth).active;
	}

	public boolean isActive(Layer layer) {
		var depth = getLayerDepth(layer);
		return depth >= 0 && isActive(depth);
	}

	public boolean isKeepAlive() {
		return keepAlive;
	}

	public long getTicksLived() {
		return ticks;
	}

	CUIContents getContents() {
		return contents.clone();
	}

	/**
	 * 打开CUI。如果newCamera为true，则克隆并使用默认Camera。<br>
	 * Open CUI. If newCamera is true, clone and use the default camera.
	 *
	 * @param viewer
	 *            玩家<br>
	 *            Player
	 * @param newCamera
	 *            是否使用新的Camera<br>
	 *            Whether to use a new camera
	 * @return 是否成功打开<br>
	 *         Whether it is successfully opened
	 */
	public boolean open(Player viewer, boolean newCamera) {
		if (viewers.containsKey(viewer)) {
			return false;
		}
		if (!handler.onOpen(viewer)) {
			return false;
		}
		var camera = newCamera ? defaultCamera.deepClone() : defaultCamera;
		if (newCamera) {
			cameras.add(camera);
		}
		viewers.put(viewer, new PlayerInfo(camera));
		CUIManager.getInstance().notifyOpen(viewer, this);
		return true;
	}

	private boolean openFrom(Player viewer, ChestUI<?> from, boolean newCamera) {
		if (!open(viewer, newCamera)) {
			return false;
		}

		var info = viewers.get(viewer);
		if (info == null) {
			return false;
		}

		info.from = from;
		return true;
	}

	/**
	 * TODO: test<br>
	 * 切换到另一个CUI，当该CUI关闭时会自动切换回来。<br>
	 * Switch to another CUI, and it will automatically switch back when the CUI is
	 * closed.
	 *
	 * @param viewer
	 *            玩家<br>
	 *            Player
	 * @param to
	 *            目标CUI<br>
	 *            Target CUI
	 * @param newCamera
	 *            是否使用新的Camera<br>
	 *            Whether to use a new camera
	 * @return 是否成功切换<br>
	 *         Whether it is successfully switched
	 */
	public boolean switchTo(Player viewer, ChestUI<?> to, boolean newCamera) {
		var info = viewers.get(viewer);
		if (info == null) {
			return false;
		}
		if (!handler.onSwitchTo(viewer, to)) {
			return false;
		}

		if (!to.openFrom(viewer, this, newCamera)) {
			return false;
		}

		info.to = to;
		info.state = PlayerInfo.State.VIEWING_OTHER;
		return true;
	}

	public boolean isClosable() {
		return closable;
	}

	private boolean switchBack(Player viewer) {
		var info = viewers.get(viewer);
		if (info == null) {
			Bukkit.getLogger().severe("Player " + viewer.getName() + " is not viewing " + this);
			return false;
		}
		if (!handler.onSwitchBack(viewer, info.to)) {
			return false;
		}
		info.to = null;
		info.state = PlayerInfo.State.VIEWING;
		CUIManager.getInstance().notifyOpen(viewer, this);
		return true;
	}

	/**
	 * 关闭CUI。在不强制的情况下，closable为false、当前正切换到其他CUI、handler拒绝关闭、来源CUI拒绝切换回去时将无法关闭。<br>
	 * Close CUI. It cannot be closed if closable is false, currently switching to
	 * another CUI, the handler refuses to close, and the source CUI refuses to
	 * switch back without force.
	 *
	 * @param viewer
	 *            玩家<br>
	 *            Player
	 * @param force
	 *            是否强制关闭<br>
	 *            Whether to force close
	 * @return 是否成功关闭<br>
	 *         Whether it is successfully closed
	 */
	public boolean close(Player viewer, boolean force) {
		if (!closable && !force) {
			return false;
		}

		var info = viewers.get(viewer);
		if (info == null) {
			return false;
		}

		if (info.to != null && !force) {
			return false;
		}

		var oldState = info.state;
		info.state = PlayerInfo.State.CLOSING;

		if (!handler.onClose(viewer)) {
			info.state = oldState;
			return false;
		}

		if (info.from != null && !info.from.switchBack(viewer)) {
			info.state = oldState;
			return false;
		}

		if (info.to != null) {
			info.to.close(viewer, true);
		}
		viewers.remove(viewer);
		CUIManager.getInstance().notifyClose(viewer, this);
		info.camera.close(viewer);
		return true;
	}

	/**
	 * 尝试关闭所有CUI，返回第一个无法关闭的CUI<br>
	 * Try to close all CUIs and return the first CUI that cannot be closed
	 *
	 * @param viewer
	 *            玩家<br>
	 *            Player
	 * @param force
	 *            是否强制关闭<br>
	 *            Whether to force close
	 * @return 无法关闭的CUI，如果所有CUI都关闭了则返回null<br>
	 *         CUI that cannot be closed, return null if all CUIs are closed
	 */
	public ChestUI<?> closeAll(Player viewer, boolean force) {
		var info = viewers.get(viewer);
		if (info == null) {
			return this;
		}

		if (!close(viewer, force)) {
			return this;
		}

		if (info.from != null) {
			return info.from.closeAll(viewer, force);
		}
		return null;
	}

	/**
	 * 销毁CUI，这会使得所有玩家强制关闭CUI。<br>
	 * Destroy CUI, which will force all players to close CUI.
	 *
	 * @see #close(Player, boolean)
	 */
	public void destroy() {
		handler.onDestroy();
		var players = new ArrayList<>(viewers.keySet());
		players.forEach(player -> close(player, true));
		CUIManager.getInstance().notifyDestroy(this);
	}

	public Editor edit() {
		return new Editor();
	}

	@Override
	public String toString() {
		return "ChestUI{" + "handler=" + handler.getClass().getCanonicalName() + ", defaultTitle=" + defaultTitle
				+ ", state=" + state + ", ticks=" + ticks + '}';
	}

	public enum State {
		UNINITIALIZED, READY
	}

	public class Editor {
		private Editor() {
		}

		@SuppressWarnings("unchecked")
		public <T2 extends CUIHandler> ChestUI<T2> finish() {
			return (ChestUI<T2>) ChestUI.this;
		}

		public Editor setDefaultTitle(String title) {
			return setDefaultTitle(LegacyComponentSerializer.legacyAmpersand().deserialize(title));
		}

		public Editor setDefaultTitle(Component title) {
			ChestUI.this.defaultTitle = title;
			return this;
		}

		// TODO: test
		public Editor setClosable(boolean closable) {
			ChestUI.this.closable = closable;
			return this;
		}

		// TODO: test
		public Editor setKeepAlive(boolean keepAlive) {
			ChestUI.this.keepAlive = keepAlive;
			return this;
		}

		public Editor setLayer(int depth, Layer layer) {
			if (maxDepth >= 0 && (depth < 0 || depth >= maxDepth))
				throw new IllegalArgumentException("depth must be between 0 and " + maxDepth);

			if (layer == null) {
				return removeLayer(depth);
			}

			var legacy = layers.put(depth, new LayerWrapper(layer));
			if (legacy != null) {
				layerDepths.remove(legacy.layer);
			}
			layerDepths.put(layer, depth);
			dirty = true;
			return this;
		}

		public Editor removeLayer(int depth) {
			var legacy = layers.remove(depth);
			if (legacy != null) {
				layerDepths.remove(legacy.layer);
			}
			dirty = true;
			return this;
		}

		public Editor setActive(int depth, boolean active) {
			LayerWrapper wrapper = layers.get(depth);
			if (wrapper == null) {
				return this;
			}
			if (wrapper.active == active)
				return this;
			wrapper.active = active;
			dirty = true;
			return this;
		}

		public Editor setActive(Layer layer, boolean active) {
			var depth = getLayerDepth(layer);
			if (depth < 0)
				return this;
			return setActive(depth, active);
		}
	}

	private static class LayerWrapper {
		private final Layer layer;
		private boolean active = true;

		LayerWrapper(Layer layer) {
			this.layer = layer;
		}
	}

	public class Trigger {
		private Trigger() {
		}

		public void tick() {
			ticks++;
			handler.onTick();
			if (!keepAlive && viewers.isEmpty()) {
				destroy();
			}
		}

		private boolean sync(boolean force) {
			// 深度高的先display
			List<Layer> activeLayers = getActiveLayers(false);
			if (!force && !dirty) {
				if (activeLayers.stream().noneMatch(Layer::isDirty))
					return false;
			}
			var contents = new CUIContents(maxRow, maxColumn);
			activeLayers.forEach(layer -> layer.display(contents, 0, 0));
			ChestUI.this.contents = contents;
			dirty = false;
			return true;
		}

		/**
		 * 更新并同步界面状态<br>
		 * Update and synchronize interface status
		 */
		public void update() {
			var synced = sync(state == State.UNINITIALIZED);
			cameras.forEach(camera -> camera.sync(synced));
			state = State.READY;
			// openInventory可能会触发事件，从而导致ConcurrentModificationException
			var copy = new HashMap<>(viewers);
			copy.forEach((player, info) -> {
				if (info.state != PlayerInfo.State.VIEWING) {
					return;
				}

				info.camera.open(player);
			});
		}

		void notifyUseCamera(Player viewer, Camera<T> camera) {
			var info = viewers.get(viewer);
			if (info == null) {
				Bukkit.getLogger().severe("Player " + viewer.getName() + " is not viewing " + ChestUI.this);
				return;
			}
			if (info.camera != camera) {
				info.camera.close(viewer);
			}
			info.camera = camera;
		}

		void notifyReleaseCamera(Camera<T> camera) {
			cameras.remove(camera);
		}
	}

	public class PlayerInfo {
		private @Nullable ChestUI<?> from;
		private @Nullable ChestUI<?> to;
		private @NotNull Camera<T> camera;
		private State state = State.VIEWING;

		public PlayerInfo(@NotNull Camera<T> camera) {
			this.camera = camera;
		}

		public enum State {
			CLOSING, VIEWING,
			/**
			 * 玩家正在查看其他CUI，但是当前CUI仍然处于打开状态。这通常发生在玩家从一个CUI切换到另一个CUI时。<br>
			 * Player is viewing another CUI, but the current CUI is still open. This
			 * usually happens when a player switches from one CUI to another.
			 */
			VIEWING_OTHER,
		}
	}
}
