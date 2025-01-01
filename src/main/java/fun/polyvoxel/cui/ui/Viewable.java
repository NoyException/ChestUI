package fun.polyvoxel.cui.ui;

import fun.polyvoxel.cui.CUIPlugin;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;

public abstract class Viewable {
	private final CUIPlugin plugin;

	protected Viewable(CUIPlugin plugin) {
		this.plugin = plugin;
	}

	public CUIPlugin getCUIPlugin() {
		return plugin;
	}

	public boolean canOpen(Player viewer) {
		return true;
	}

	public final boolean open(Player viewer) {
		return open(viewer, false);
	}

	/**
	 * 打开视图。如果不作为子视图打开，则会尝试关闭当前正在查看的视图。<br>
	 * Open the view. If not open as a child view, it will try to close the current
	 * view.
	 * 
	 * @param viewer
	 *            玩家<br>
	 *            The player
	 * @param asChild
	 *            是否作为子视图打开<br>
	 *            Whether to open as a child view
	 * @return 是否成功打开<br>
	 *         Whether it is successfully opened
	 */
	public final boolean open(Player viewer, boolean asChild) {
		return plugin.getCameraManager().open(this, viewer, asChild);
	}

	protected abstract void doOpen(Player viewer, boolean asChild);

	public boolean canClose(Player viewer) {
		return true;
	}

	public final boolean close(Player viewer) {
		return close(viewer, false);
	}

	public final boolean close(Player viewer, boolean force) {
		return close(viewer, force, false);
	}

	/**
	 * 关闭视图。如果不允许级联关闭，则只有当前正在查看本视图时才会关闭，反之一直关闭直到切换回本视图再关闭。<br>
	 * Close the view. If cascade close is not allowed, this view will only be
	 * closed when the player is currently viewing this view, otherwise it will be
	 * closed until switching back to this view and then close.
	 * 
	 * @param viewer
	 *            玩家<br>
	 *            The player
	 * @param force
	 *            是否强制关闭<br>
	 *            Whether to force close
	 * @param cascade
	 *            是否级联关闭<br>
	 *            Whether to cascade close
	 * @return 是否成功关闭<br>
	 *         Whether it is successfully closed
	 */
	public final boolean close(Player viewer, boolean force, boolean cascade) {
		return plugin.getCameraManager().close(this, viewer, force, cascade);
	}

	protected abstract void doClose(Player viewer);

	protected void notifySwitchOut(Player viewer) {
	}

	protected void notifySwitchBack(Player viewer) {
	}

	/**
	 * 检查并保持打开状态。如果发现玩家并没有打开该视图，则打开并返回该视图。如果玩家无法打开或已经打开了该视图，则返回 null。<br>
	 * Check and keep the view open. If the player is found not opening the view,
	 * open it and return the view. If the player cannot open or has already opened
	 * the view, return null.
	 * 
	 * @param viewer
	 *            玩家<br>
	 *            The player
	 * @return 保持打开的视图<br>
	 *         The view that is kept opening
	 */
	protected abstract InventoryView keepOpening(Player viewer);
}
