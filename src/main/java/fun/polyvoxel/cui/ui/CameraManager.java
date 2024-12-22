package fun.polyvoxel.cui.ui;

import fun.polyvoxel.cui.CUIPlugin;
import com.google.common.collect.HashBiMap;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.function.Consumer;

public class CameraManager {
	private final CUIPlugin plugin;
	private final HashBiMap<String, Camera<?>> cameras = HashBiMap.create();
	private final Map<Player, Stack<Camera<?>>> byPlayer = new HashMap<>();

	public CameraManager(CUIPlugin plugin) {
		this.plugin = plugin;
	}

	void registerCamera(Camera<?> camera) {
		cameras.put(camera.getName(), camera);
	}

	void unregisterCamera(Camera<?> camera) {
		cameras.inverse().remove(camera);
	}

	Stack<Camera<?>> getCameraStack(Player player) {
		return byPlayer.computeIfAbsent(player, p -> new Stack<>());
	}

	public void forEachCamera(Consumer<? super Camera<?>> action) {
		new ArrayList<>(cameras.values()).forEach(action);
	}

	public List<Camera<?>> getCameras() {
		return new ArrayList<>(cameras.values());
	}

	public Camera<?> getCamera(Player player) {
		var stack = byPlayer.computeIfAbsent(player, p -> new Stack<>());
		if (stack.empty())
			return null;
		return stack.peek();
	}

	public Camera<?> getCamera(String name) {
		return cameras.get(name);
	}

	public boolean closeTop(Player player, boolean force) {
		var stack = byPlayer.computeIfAbsent(player, p -> new Stack<>());
		if (stack.empty())
			return true;
		return stack.peek().close(player, force);
	}

	public boolean closeAll(Player player, boolean force) {
		var stack = byPlayer.computeIfAbsent(player, p -> new Stack<>());
		if (stack.empty())
			return true;
		while (!stack.empty()) {
			var closed = stack.peek().close(player, force);
			if (!closed)
				return false;
		}
		byPlayer.remove(player);
		return true;
	}
}
