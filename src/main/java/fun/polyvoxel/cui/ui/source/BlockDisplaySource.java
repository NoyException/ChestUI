package fun.polyvoxel.cui.ui.source;

import org.bukkit.block.Block;

public class BlockDisplaySource implements DisplaySource<Block> {
	private final Block block;

	public BlockDisplaySource(Block block) {
		this.block = block;
	}

	@Override
	public Block getSource() {
		return block;
	}
}
