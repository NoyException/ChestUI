package fun.polyvoxel.cui.ui.source;

import org.bukkit.plugin.Plugin;

public class PluginDisplaySource implements DisplaySource<Plugin> {
	private final Plugin plugin;

	public PluginDisplaySource(Plugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public Plugin getSource() {
		return plugin;
	}
}
