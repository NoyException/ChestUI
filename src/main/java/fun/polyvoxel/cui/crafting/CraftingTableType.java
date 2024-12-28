package fun.polyvoxel.cui.crafting;

import org.bukkit.inventory.ItemStack;
import org.joml.Vector2i;

import java.util.*;

public class CraftingTableType {
	private Mode mode;
	private List<Recipe> recipes;
	private List<Vector2i> inputsSize;
	private List<Vector2i> outputsSize;

	private CraftingTableType() {
	}

	public Mode getMode() {
		return mode;
	}

	public List<Recipe> getRecipes() {
		return recipes;
	}

	public CraftingTable createInstance() {
		var initial = new IO(new Inputs(), new Outputs());
		for (var size : inputsSize) {
			initial.inputs().addInput(size.x, size.y);
		}
		for (var size : outputsSize) {
			initial.outputs().addOutput(size.x, size.y);
		}
		return new CraftingTable(this, initial);
	}

	public static Builder builder() {
		return new CraftingTableType.Builder();
	}

	public enum Mode {
		// 自动展示合成结果，一但点击结果就立即合成并消耗材料
		AUTO,
		// 需要主动调用match、apply和cancel来触发合成相关操作
		MANUAL,
	}

	public static class Inputs {
		private final List<ItemStack[][]> inputs = new ArrayList<>();

		public void addInput(int maxRow, int maxColumn) {
			var input = new ItemStack[maxRow][maxColumn];
			inputs.add(input);
		}

		public void addInput(ItemStack[][] input) {
			inputs.add(input);
		}

		public ItemStack[][] getInput(int index) {
			return inputs.get(index);
		}

		public int getMaxIndex() {
			return inputs.size();
		}
	}

	public static class Outputs {
		private final List<ItemStack[][]> outputs = new ArrayList<>();

		public void addOutput(int maxRow, int maxColumn) {
			var output = new ItemStack[maxRow][maxColumn];
			outputs.add(output);
		}

		public void addOutput(ItemStack[][] output) {
			outputs.add(output);
		}

		public int getMaxIndex() {
			return outputs.size();
		}

		public ItemStack[][] getOutput(int index) {
			return outputs.get(index);
		}
	}

	public record IO(Inputs inputs, Outputs outputs) {
	}

	public static class Builder {
		private Mode mode = Mode.AUTO;
		private final List<Recipe> recipes = new LinkedList<>();
		private final List<Vector2i> inputsSize = new LinkedList<>();
		private final List<Vector2i> outputsSize = new LinkedList<>();

		private Builder() {
		}

		public CraftingTableType build() {
			var table = new CraftingTableType();
			table.mode = mode;
			table.recipes = new ArrayList<>(recipes);
			table.inputsSize = new ArrayList<>(inputsSize);
			table.outputsSize = new ArrayList<>(outputsSize);
			return table;
		}

		public Builder mode(Mode mode) {
			this.mode = mode;
			return this;
		}

		public Builder addRecipe(Recipe recipe) {
			this.recipes.add(recipe);
			return this;
		}

		public Builder addRecipes(Collection<Recipe> recipes) {
			this.recipes.addAll(recipes);
			return this;
		}

		public Builder addInput(int maxRow, int maxColumn) {
			this.inputsSize.add(new Vector2i(maxRow, maxColumn));
			return this;
		}

		public Builder addOutput(int maxRow, int maxColumn) {
			this.outputsSize.add(new Vector2i(maxRow, maxColumn));
			return this;
		}
	}
}
