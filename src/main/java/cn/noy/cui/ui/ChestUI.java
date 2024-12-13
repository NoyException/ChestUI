package cn.noy.cui.ui;

import cn.noy.cui.layer.Layer;
import cn.noy.cui.util.Position;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ChestUI<T extends CUIHandler<T>> {
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
	private final int id;
	private final Trigger trigger;
	private boolean dirty;
	private boolean synced;

	ChestUI(@NotNull T handler, int id) {
		this.handler = handler;
		this.id = id;
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

	public String getName() {
		return getHandlerClass().getName() + '#' + id;
	}

	public Camera<T> getDefaultCamera() {
		return defaultCamera;
	}

	public Camera<T> newCamera() {
		var camera = defaultCamera.deepClone();
		cameras.add(camera);
		return camera;
	}

	public Camera<T> newCamera(Position position, int rowSize, int columnSize, Camera.HorizontalAlign horizontalAlign,
			Camera.VerticalAlign verticalAlign) {
		var camera = new Camera<>(this, position, rowSize, columnSize, horizontalAlign, verticalAlign, defaultTitle);
		cameras.add(camera);
		return camera;
	}

	public List<Camera<T>> getCameras() {
		return new ArrayList<>(cameras);
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

	public boolean isClosable() {
		return closable;
	}

	/**
	 * 销毁CUI，这会使得所有玩家强制关闭CUI。<br>
	 * Destroy CUI, which will force all players to close CUI.
	 */
	public void destroy() {
		handler.onDestroy();
		new ArrayList<>(cameras).forEach(Camera::destroy);
		CUIManager.getInstance().notifyDestroy(this);
	}

	public Editor edit() {
		return new Editor();
	}

	@Override
	public String toString() {
		return "ChestUI{" + "name=" + getName() + ", defaultTitle=" + defaultTitle + ", state=" + state + ", ticks="
				+ ticks + '}';
	}

	public enum State {
		UNINITIALIZED, READY
	}

	public class Editor {
		private Editor() {
		}

		public ChestUI<T> finish() {
			return ChestUI.this;
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
			if (!keepAlive && cameras.isEmpty()) {
				destroy();
			}
			synced = false;
		}

		private boolean sync(boolean force) {
			if (!force && synced) {
				return false;
			}
			// 深度高的先display
			var activeLayers = getActiveLayers(false);
			if (!force && !dirty) {
				if (activeLayers.stream().noneMatch(Layer::isDirty))
					return false;
			}
			var contents = new CUIContents(maxRow, maxColumn);
			activeLayers.forEach(layer -> layer.display(contents, 0, 0));
			ChestUI.this.contents = contents;
			dirty = false;
			synced = true;
			return true;
		}

		/**
		 * 更新并同步界面状态<br>
		 * Update and synchronize interface status
		 */
		public boolean update() {
			var updated = sync(state == State.UNINITIALIZED);
			state = State.READY;
			return updated;
		}

		void notifyReleaseCamera(Camera<T> camera) {
			cameras.remove(camera);
		}
	}
}
