package cn.noy.cui.crafting.consumer;

import cn.noy.cui.crafting.CraftingContext;
import cn.noy.cui.crafting.consumer.ingredient.Ingredient;
import cn.noy.cui.util.Array2Ds;
import cn.noy.cui.util.ItemStacks;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * 按照一定的形状消耗物品，允许平移（比如2*2的配方在3*3的表格中，在任意一个角落摆放都可以）。<br>
 * Consume items according to a certain shape, allowing translation (for
 * example, a 2*2 recipe in a 3*3 table, can be placed in any corner).
 */
public class ShapedConsumer implements Consumer {
	private boolean strict = true;
	private Pattern pattern;
	private final Map<Character, Ingredient> byChar = new HashMap<>();

	@Override
	public ItemStack[][] consume(@NotNull CraftingContext ctx, @NotNull ItemStack[][] input) {
		var trim = ItemStacks.trim(input);
		var leftTop = trim.getLeft();
		var rightBottom = trim.getRight();
		var width = rightBottom.column() - leftTop.column() + 1;
		var height = rightBottom.row() - leftTop.row() + 1;
		if (width != pattern.width || height != pattern.height) {
			return null;
		}
		var inputAfter = new ItemStack[input.length][input[0].length];
		for (int row = 0; row < height; row++) {
			for (int column = 0; column < width; column++) {
				var c = pattern.pattern[row][column];
				var ingredient = byChar.get(c);
				var itemStack = input[row + leftTop.row()][column + leftTop.column()];
				if (ingredient == null) {
					if (c != ' ') {
						throw new IllegalArgumentException("No ingredient for character " + c);
					}
					if (!ItemStacks.isEmpty(itemStack)) {
						if (strict) {
							return null;
						} else {
							inputAfter[row + leftTop.row()][column + leftTop.column()] = itemStack.clone();
						}
					}
					continue;
				}
				var afterConsume = ingredient.consume(ctx, itemStack);
				if (afterConsume == null) {
					return null;
				}
				inputAfter[row + leftTop.row()][column + leftTop.column()] = afterConsume;
			}
		}
		return inputAfter;
	}

	public static Builder builder() {
		return new ShapedConsumer.Builder();
	}

	public static class Builder {
		private final ShapedConsumer consumer = new ShapedConsumer();

		Builder() {
		}

		/**
		 * 是否允许在空格位置放置物品。<br>
		 * Whether to allow items to be placed in the space position.
		 * 
		 * @param strict
		 *            是否严格匹配。默认为true。<br>
		 *            Whether to match strictly. The default is true.
		 * @return this
		 */
		public Builder strict(boolean strict) {
			consumer.strict = strict;
			return this;
		}

		public Builder pattern(String... pattern) {
			consumer.pattern = new Pattern(pattern);
			return this;
		}

		public Builder set(char c, Ingredient ingredient) {
			if (c == ' ') {
				throw new IllegalArgumentException("Cannot map space character");
			}
			consumer.byChar.put(c, ingredient);
			return this;
		}

		public ShapedConsumer build() {
			return consumer;
		}
	}

	private static class Pattern {
		private final char[][] pattern;
		private final int width;
		private final int height;

		public Pattern(String[] lines) {
			var raw = new Character[lines.length][];
			for (int i = 0; i < lines.length; i++) {
				raw[i] = lines[i].chars().mapToObj(c -> (char) c).toArray(Character[]::new);
			}
			var trim = Array2Ds.trim(raw, c -> c == ' ');
			var leftTop = trim.getLeft();
			var rightBottom = trim.getRight();
			width = rightBottom.column() - leftTop.column() + 1;
			height = rightBottom.row() - leftTop.row() + 1;
			pattern = new char[height][width];
			for (int i = 0; i < height; i++) {
				for (int j = 0; j < width; j++) {
					pattern[i][j] = raw[i + leftTop.row()][j + leftTop.column()];
				}
			}
		}
	}
}
