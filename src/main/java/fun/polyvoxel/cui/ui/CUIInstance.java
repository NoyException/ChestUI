package fun.polyvoxel.cui.ui;

import fun.polyvoxel.cui.CUIPlugin;
import fun.polyvoxel.cui.layer.Layer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class CUIInstance<T extends ChestUI<T>> {
	private final CUIPlugin cuiPlugin;
	private final HashMap<Integer, Camera<T>> cameras = new HashMap<>();
	private State state = State.UNINITIALIZED;

	private final TreeMap<Integer, LayerWrapper> layers = new TreeMap<>();
	private final HashMap<Layer, Integer> layerDepths = new HashMap<>();
	private int nextCameraId = 0;
	private Component defaultTitle;

	private boolean dirty;
	private boolean keepAlive;
	private long ticksLived;

	private final CUIType<T> type;
	private final ChestUI.InstanceHandler<T> handler;
	private final int id;

	CUIInstance(CUIPlugin cuiPlugin, CUIType<T> type, int id) {
		this.cuiPlugin = cuiPlugin;
		this.type = type;
		this.id = id;
		this.defaultTitle = type.getDefaultTitle();
		this.handler = type.getChestUI().createInstanceHandler();
		handler.onInitialize(this);
		state = State.READY;
	}

	public CUIPlugin getCUIPlugin() {
		return cuiPlugin;
	}

	public CUIType<T> getType() {
		return type;
	}

	public ChestUI.InstanceHandler<T> getHandler() {
		return handler;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return type.getKey().toString() + '#' + id;
	}

	boolean isDirty() {
		return dirty;
	}

	int getNextCameraId() {
		return nextCameraId++;
	}

	public Camera<T> createCamera() {
		var camera = new Camera<>(this, getNextCameraId());
		cameras.put(camera.getId(), camera);
		return camera;
	}

	public List<Camera<T>> getCameras() {
		return new ArrayList<>(cameras.values());
	}

	/**
	 * 获取一个摄像头。不保证每次调用返回的是同一个摄像头。<br>
	 * Get a camera. It is not guaranteed that the same camera will be returned each
	 * time.
	 * 
	 * @return 摄像头。<br>
	 *         Camera.
	 */
	public @NotNull Camera<T> getCamera() {
		if (cameras.isEmpty()) {
			return createCamera();
		}
		return cameras.values().iterator().next();
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
		return ticksLived;
	}

	/**
	 * 销毁CUI，这会使得所有玩家强制关闭CUI。<br>
	 * Destroy CUI, which will force all players to close CUI.
	 */
	public void destroy() {
		if (state == State.DESTROYING || state == State.DESTROYED) {
			return;
		}
		state = State.DESTROYING;
		handler.onDestroy();
		new ArrayList<>(cameras.values()).forEach(Camera::destroy);
		state = State.DESTROYED;
	}

	public void tickStart() {
		dirty = false;
		ticksLived++;
		for (LayerWrapper wrapper : layers.values()) {
			if (wrapper.active) {
				wrapper.layer.tickStart();
			}
		}
		cameras.values().forEach(Camera::tickStart);
	}

	public void tick() {
		if (!keepAlive && cameras.isEmpty()) {
			destroy();
			return;
		}

		handler.onTick();
		for (LayerWrapper wrapper : layers.values()) {
			if (wrapper.active) {
				wrapper.layer.tick();
			}
		}
		cameras.values().forEach(Camera::tick);
	}

	void notifyReleaseCamera(Camera<T> camera) {
		cameras.remove(camera.getId());
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

		public Editor defaultTitle(String title) {
			return defaultTitle(LegacyComponentSerializer.legacyAmpersand().deserialize(title));
		}

		public Editor defaultTitle(Component title) {
			CUIInstance.this.defaultTitle = title;
			return this;
		}

		public Editor keepAlive(boolean keepAlive) {
			CUIInstance.this.keepAlive = keepAlive;
			return this;
		}

		public Editor layer(int depth, Layer layer) {
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

		public Editor layerActive(int depth, boolean active) {
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

		public Editor layerActive(Layer layer, boolean active) {
			var depth = getLayerDepth(layer);
			if (depth < 0)
				return this;
			return layerActive(depth, active);
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
				+ ticksLived + '}';
	}

	@Override
	public int hashCode() {
		return getName().hashCode();
	}
}
