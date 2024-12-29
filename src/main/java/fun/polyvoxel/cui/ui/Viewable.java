package fun.polyvoxel.cui.ui;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;

public interface Viewable {
	default boolean open(Player viewer) {
		return open(viewer, false);
	}

	boolean open(Player viewer, boolean asChild);

	boolean close(Player viewer, boolean force);

	@ApiStatus.Internal
	void notifySwitchOut(Player viewer);

	@ApiStatus.Internal
	void notifySwitchBack(Player viewer);

	@ApiStatus.Internal
	void checkOpen(Player viewer);
}
