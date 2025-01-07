package fun.polyvoxel.cui.ui.source;

import org.bukkit.entity.Player;

public class PlayerDisplaySource implements DisplaySource<Player> {
	private final Player player;

	public PlayerDisplaySource(Player player) {
		this.player = player;
	}

	@Override
	public Player getSource() {
		return player;
	}
}
