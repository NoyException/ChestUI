package fun.polyvoxel.cui.ui;

import fun.polyvoxel.cui.CUIPlugin;
import com.google.common.collect.HashBiMap;
import fun.polyvoxel.cui.ui.source.DisplaySource;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.function.Consumer;

public final class CameraManager {
	private final CUIPlugin plugin;
	private final HashBiMap<String, Camera<?>> cameras = HashBiMap.create();
	private final Map<Player, Stack<Viewable>> byPlayer = new HashMap<>();

	public CameraManager(CUIPlugin plugin) {
		this.plugin = plugin;
	}

	void registerCamera(Camera<?> camera) {
		cameras.put(camera.getName(), camera);
	}

	void unregisterCamera(Camera<?> camera) {
		cameras.inverse().remove(camera);
	}

	private Stack<Viewable> getViewingStack(Player player) {
		return byPlayer.computeIfAbsent(player, p -> new Stack<>());
	}

	public Viewable getViewing(Player player) {
		var stack = getViewingStack(player);
		if (stack.empty())
			return null;
		return stack.peek();
	}

	public void forEachCamera(Consumer<? super Camera<?>> action) {
		new ArrayList<>(cameras.values()).forEach(action);
	}

	public List<Camera<?>> getCameras() {
		return new ArrayList<>(cameras.values());
	}

	public Camera<?> getCamera(Player player) {
		return getViewing(player) instanceof Camera<?> camera ? camera : null;
	}

	public Camera<?> getCamera(String name) {
		return cameras.get(name);
	}

	public boolean open(Viewable viewable, Player viewer, DisplaySource<?> source, boolean asChild) {
		if (!viewable.canOpen(viewer)) {
			return false;
		}
		if (!asChild) {
			boolean success = closeAll(viewer, false);
			if (!success) {
				return false;
			}
		}
		var stack = getViewingStack(viewer);
		if (!stack.empty()) {
			var parent = stack.peek();
			if (parent != null) {
				parent.notifySwitchOut(viewer);
			}
		}
		stack.push(viewable);
		viewable.doOpen(viewer, asChild, source);
		return true;
	}

	public boolean close(Viewable viewable, Player viewer, boolean force, boolean cascade) {
		var stack = getViewingStack(viewer);
		Viewable peek = stack.peek();
		if (!cascade && peek != viewable) {
			return false;
		}

		while (!stack.empty()) {
			if (!force && !peek.canClose(viewer)) {
				return false;
			}
			peek.doClose(viewer);
			stack.pop();
			if (!stack.empty()) {
				var tmp = stack.peek();
				tmp.notifySwitchBack(viewer);
				if (peek != viewable) {
					peek = tmp;
				} else {
					break;
				}
			} else {
				break;
			}
		}
		return true;
	}

	public boolean closeTop(Player player, boolean force) {
		var stack = getViewingStack(player);
		if (stack.empty()) {
			return true;
		}
		return stack.peek().close(player, force);
	}

	public boolean closeAll(Player player, boolean force) {
		var stack = getViewingStack(player);
		if (stack.empty())
			return true;

		if (!force) {
			for (Viewable viewable : stack) {
				if (!viewable.canClose(player)) {
					return false;
				}
			}
		}
		while (!stack.empty()) {
			var closed = stack.peek().close(player, true);
			if (!closed) {
				return false;
			}
		}
		return true;
	}

	public void tickEnd() {
		byPlayer.keySet().forEach(player -> {
			if (!player.isOnline()) {
				closeAll(player, true);
			}
		});
		byPlayer.values().removeIf(Stack::empty);
		byPlayer.forEach((player, stack) -> {
			if (stack.empty())
				return;
			stack.peek().keepOpening(player);
		});
	}
}
