package fun.polyvoxel.cui.ui.source;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;

public class CommandDisplaySource implements DisplaySource<CommandSender> {
	private final CommandSender sender;
	private final Entity executor;
	private final Location location;

	public CommandDisplaySource(CommandSender sender) {
		this.sender = sender;
		if (sender instanceof Entity entity) {
			this.executor = entity;
			this.location = entity.getLocation();
		} else {
			this.executor = null;
			this.location = null;
		}
	}

	public CommandDisplaySource(CommandSourceStack source) {
		this.sender = source.getSender();
		this.executor = source.getExecutor();
		this.location = source.getLocation();
	}

	@Override
	public CommandSender getSource() {
		return sender;
	}

	public Entity getExecutor() {
		return executor;
	}

	public Location getLocation() {
		return location;
	}
}
