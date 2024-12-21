package cn.noy.cui.ui;

import cn.noy.cui.CUIPlugin;
import cn.noy.cui.layer.Layer;
import cn.noy.cui.util.Position;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class ChestUI<T extends CUIHandler<T>> {
	private final CUIPlugin plugin;
	private final HashMap<Integer, Camera<T>> cameras = new HashMap<>();
	private State state = State.UNINITIALIZED;

	private final int maxRow;
	private final int maxColumn;
	private final int maxDepth;
	private final TreeMap<Integer, LayerWrapper> layers = new TreeMap<>();
	private final HashMap<Layer, Integer> layerDepths = new HashMap<>();
	private final Camera<T> defaultCamera;
	private int nextCameraId = 0;
	private Component defaultTitle;

	private boolean closable = true;
	private boolean keepAlive;
	private long ticks;

	private final T handler;
	private final String name;
	private final int id;

	ChestUI(CUIPlugin plugin, @NotNull T handler, String name, int id) {
		this.plugin = plugin;
		this.handler = handler;
		this.name = name;
		this.id = id;

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
			defaultCamera = new Camera<>(this, getNextCameraId(), new Position(def.row(), def.column()), def.rowSize(),
					def.columnSize(), def.horizontalAlign(), def.verticalAlign(), defaultTitle);
		} else {
			defaultCamera = new Camera<>(this, getNextCameraId(), new Position(0, 0), 3, 9, Camera.HorizontalAlign.LEFT,
					Camera.VerticalAlign.TOP, defaultTitle);
		}
		cameras.put(defaultCamera.getId(), defaultCamera);
		handler.onCreateCamera(defaultCamera);

		handler.onInitialize(this);
		state = State.READY;
	}

	public CUIPlugin getPlugin() {
		return plugin;
	}

	@SuppressWarnings("unchecked")
	public Class<T> getHandlerClass() {
		return (Class<T>) handler.getClass();
	}

	public String getName() {
		return name + '#' + id;
	}

	int getNextCameraId() {
		return nextCameraId++;
	}

	public Camera<T> getDefaultCamera() {
		return defaultCamera;
	}

	public Camera<T> createCamera() {
		var camera = defaultCamera.deepClone();
		cameras.put(camera.getId(), camera);
		handler.onCreateCamera(camera);
		return camera;
	}

	public Camera<T> createCamera(Position position, int rowSize, int columnSize,
			Camera.HorizontalAlign horizontalAlign, Camera.VerticalAlign verticalAlign) {
		var camera = new Camera<>(this, getNextCameraId(), position, rowSize, columnSize, horizontalAlign,
				verticalAlign, defaultTitle);
		cameras.put(camera.getId(), camera);
		handler.onCreateCamera(camera);
		return camera;
	}

	public List<Camera<T>> getCameras() {
		return new ArrayList<>(cameras.values());
	}

	public Camera<T> getCamera(int cameraId) {
		return cameras.get(cameraId);
	}

	public int getCameraCount() {
		return cameras.size();
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

	public NavigableMap<Integer, Layer> getActiveLayers() {
		return layers.entrySet().stream().filter(entry -> entry.getValue().active)
				.map(entry -> Map.entry(entry.getKey(), entry.getValue().layer))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, TreeMap::new));
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

	public boolean isClosable() {
		return closable;
	}

	/**
	 * 销毁CUI，这会使得所有玩家强制关闭CUI。<br>
	 * Destroy CUI, which will force all players to close CUI.
	 */
	public void destroy() {
		state = State.DESTROYING;
		handler.onDestroy();
		new ArrayList<>(cameras.values()).forEach(Camera::destroy);
		state = State.DESTROYED;
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
		UNINITIALIZED, READY, DESTROYING, DESTROYED
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
			if (depth < 0) {
				throw new IllegalArgumentException("depth must be greater than or equal to 0");
			}
			if (maxDepth >= 0 && depth >= maxDepth)
				throw new IllegalArgumentException("depth must be between 0 and " + maxDepth);

			if (layer == null) {
				return removeLayer(depth);
			}

			var legacy = layers.put(depth, new LayerWrapper(layer));
			if (legacy != null) {
				layerDepths.remove(legacy.layer);
			}
			layerDepths.put(layer, depth);
			return this;
		}

		public Editor removeLayer(int depth) {
			var legacy = layers.remove(depth);
			if (legacy != null) {
				layerDepths.remove(legacy.layer);
			}
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

	public void tick() {
		ticks++;
		for (LayerWrapper wrapper : layers.values()) {
			if (wrapper.active) {
				wrapper.layer.tick();
			}
		}
		cameras.values().forEach(Camera::tick);
		handler.onTick();
	}

	void notifyReleaseCamera(Camera<T> camera) {
		cameras.remove(camera.getId());
		handler.onDestroyCamera(camera);
		if (camera == defaultCamera) {
			destroy();
		}
	}

	@Override
	public int hashCode() {
		return getName().hashCode();
	}
}
