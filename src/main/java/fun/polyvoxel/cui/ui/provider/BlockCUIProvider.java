package fun.polyvoxel.cui.ui.provider;

import fun.polyvoxel.cui.CUIPlugin;
import fun.polyvoxel.cui.ui.ChestUI;
import fun.polyvoxel.cui.ui.DisplayContext;
import fun.polyvoxel.cui.ui.source.BlockDisplaySource;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.Predicate;

public class BlockCUIProvider<T extends ChestUI<T>> extends CUIProvider<T> implements Listener {
	private final Predicate<PlayerInteractEvent> predicate;

	public BlockCUIProvider(Predicate<PlayerInteractEvent> predicate) {
		this.predicate = predicate;
	}

	@Override
	protected void onEnable() {
		Bukkit.getPluginManager().registerEvents(this, JavaPlugin.getPlugin(CUIPlugin.class));
	}

	@Override
	protected void onDisable() {
		PlayerInteractEvent.getHandlerList().unregister(this);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onBlockClick(PlayerInteractEvent event) {
		if (!event.hasBlock()) {
			return;
		}
		if (!predicate.test(event)) {
			return;
		}
		event.setCancelled(true);
		getCUIType().display(
				new DisplayContext<>(event.getPlayer(), false, new BlockDisplaySource(event.getClickedBlock())));
	}
}
