package fun.polyvoxel.cui.ui;

import fun.polyvoxel.cui.CUIPlugin;
import fun.polyvoxel.cui.layer.Layer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.*;
import java.util.stream.Collectors;

public class CUIInstance<T extends ChestUI<T>> {
	private final CUIPlugin plugin;
	private final HashMap<Integer, Camera<T>> cameras = new HashMap<>();
	private State state = State.UNINITIALIZED;

	private final TreeMap<Integer, LayerWrapper> layers = new TreeMap<>();
	private final HashMap<Layer, Integer> layerDepths = new HashMap<>();
	private int nextCameraId = 0;
	private Component defaultTitle = Component.text("ChestUI");

	private boolean closable = true;
	private boolean keepAlive;
	private long ticks;

	private final CUIType<T> type;
	private final ChestUI.Handler<T> handler;
	private final String name;
	private final int id;

	CUIInstance(CUIPlugin plugin, CUIType<T> type, ChestUI.Handler<T> handler, String name, int id) {
		this.plugin = plugin;
		this.type = type;
		this.name = name;
		this.id = id;
		this.handler = handler;
		handler.onInitialize(this);
		state = State.READY;
	}

	public CUIPlugin getPlugin() {
		return plugin;
	}

	public CUIType<T> getType() {
		return type;
	}

	public ChestUI.Handler<T> getHandler() {
		return handler;
	}

	public String getName() {
		return name + '#' + id;
	}

	int getNextCameraId() {
		return nextCameraId++;
	}

	public Camera<T> createCamera() {
		var camera = new Camera<>(this, getNextCameraId());
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

	public void tick() {
		if (!keepAlive && cameras.isEmpty()) {
			destroy();
			return;
		}

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
	}

	public enum State {
		UNINITIALIZED, READY, DESTROYING, DESTROYED
	}

	public Editor edit() {
		return new Editor();
	}

	public class Editor {
		private Editor() {
		}

		public CUIInstance<T> finish() {
			return CUIInstance.this;
		}

		public Editor setDefaultTitle(String title) {
			return setDefaultTitle(LegacyComponentSerializer.legacyAmpersand().deserialize(title));
		}

		public Editor setDefaultTitle(Component title) {
			CUIInstance.this.defaultTitle = title;
			return this;
		}

		// TODO: test
		public Editor setClosable(boolean closable) {
			CUIInstance.this.closable = closable;
			return this;
		}

		public Editor setKeepAlive(boolean keepAlive) {
			CUIInstance.this.keepAlive = keepAlive;
			return this;
		}

		public Editor setLayer(int depth, Layer layer) {
			if (depth < 0) {
				throw new IllegalArgumentException("depth must be greater than or equal to 0");
			}

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

	@Override
	public String toString() {
		return "ChestUI{" + "name=" + getName() + ", defaultTitle=" + defaultTitle + ", state=" + state + ", ticks="
				+ ticks + '}';
	}

	@Override
	public int hashCode() {
		return getName().hashCode();
	}
}
