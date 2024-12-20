package cn.noy.cui.crafting;

import cn.noy.cui.layer.Layer;
import cn.noy.cui.slot.Storage;
import cn.noy.cui.ui.Camera;
import cn.noy.cui.util.ItemStacks;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class CraftingTable {
	private Mode mode;
	private List<Recipe> recipes;
	private IOs ios;

	private CraftingTable() {
	}

	public Recipe match(@Nullable Camera<?> camera, @Nullable Player player) {
		var matched = new AtomicReference<Pair<Recipe, IO>>();
		var current = ios.getCurrent(camera);
		recipes.parallelStream().forEach(recipe -> {
			var result = recipe.use(current, camera, player);
			if (result != null) {
				matched.set(Pair.of(recipe, result));
			}
		});
		if (matched.get() == null)
			return null;
		ios.setResult(camera, matched.get().getRight());
		return matched.get().getLeft();
	}

	public boolean apply(@Nullable Camera<?> camera) {
		return ios.applyResult(camera);
	}

	public void cancel(@Nullable Camera<?> camera) {
		ios.clearResult(camera);
	}

	public Layer generateInputLayer(int index, @Nullable Camera<?> camera) {
		var input = ios.getCurrent(camera).inputs.getInput(index);

		return new Layer(input.length, input[0].length).edit().editAll((slotHandler, row, column) -> {
			slotHandler.storage(builder -> builder.source(new InputSource(index, camera, row, column)).build());
		}).finish();
	}

	public Layer generateOutputLayer(int index, @Nullable Camera<?> camera) {
		var output = ios.getCurrent(camera).outputs.getOutput(index);

		return new Layer(output.length, output[0].length).edit().editAll((slotHandler, row, column) -> {
			slotHandler.storage(builder -> builder.source(new OutputSource(index, camera, row, column)).build());
		}).finish();
	}

	private abstract static class Source implements Storage.Source {
		protected final int index;
		protected final Camera<?> camera;
		protected final int row;
		protected final int column;
		private Runnable dirtyMarker;

		private Source(int index, Camera<?> camera, int row, int column) {
			this.index = index;
			this.camera = camera;
			this.row = row;
			this.column = column;
		}

		protected abstract ItemStack[][] getMatrix();

		@Override
		public ItemStack get() {
			return getMatrix()[row][column];
		}

		@Override
		public void set(ItemStack itemStack, @Nullable Player player) {
			getMatrix()[row][column] = itemStack;
			dirtyMarker.run();
		}

		@Override
		public void setDirtyMarker(Runnable dirtyMarker) {
			this.dirtyMarker = dirtyMarker;
		}
	}

	private final class InputSource extends Source {
		private InputSource(int index, Camera<?> camera, int row, int column) {
			super(index, camera, row, column);
		}

		@Override
		protected ItemStack[][] getMatrix() {
			return ios.getCurrent(camera).inputs.getInput(index);
		}

		@Override
		public void set(ItemStack itemStack, @Nullable Player player) {
			super.set(itemStack, player);
			if (mode == Mode.AUTO) {
				cancel(camera);
				match(camera, player);
			}
		}

		@Override
		public Storage.Source deepClone() {
			return new InputSource(index, camera, row, column);
		}
	}

	private final class OutputSource extends Source {
		private OutputSource(int index, Camera<?> camera, int row, int column) {
			super(index, camera, row, column);
		}

		@Override
		protected ItemStack[][] getMatrix() {
			if (mode == Mode.AUTO) {
				var result = ios.getResult(camera);
				if (result != null) {
					return result.outputs.getOutput(index);
				}
			}
			return ios.getCurrent(camera).outputs.getOutput(index);
		}

		@Override
		public void set(ItemStack itemStack, @Nullable Player player) {
			var dirty = !ItemStacks.isSame(get(), itemStack);
			super.set(itemStack, player);
			if (mode == Mode.AUTO && dirty) {
				apply(camera);
				match(camera, player);
			}
		}

		@Override
		public Storage.Source deepClone() {
			return new OutputSource(index, camera, row, column);
		}

		@Override
		public boolean placeable(ItemStack itemStack) {
			if (mode == Mode.AUTO) {
				return false;
			}
			return super.placeable(itemStack);
		}

		@Override
		public boolean bidirectional() {
			return mode != Mode.AUTO;
		}
	}

	public static Builder builder() {
		return new CraftingTable.Builder();
	}

	public enum Mode {
		// 自动展示合成结果，一但点击结果就立即合成并消耗材料
		AUTO,
		// 需要主动调用match、apply和cancel来触发合成相关操作
		MANUAL,
	}

	public enum IOType {
		// 每个摄像头拥有各自的输入输出区
		CAMERA,
		// 共用输入输出区
		SHARED,
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

		public Inputs deepClone() {
			var clone = new Inputs();
			inputs.forEach(input -> {
				var cloned = new ItemStack[input.length][input[0].length];
				for (int i = 0; i < input.length; i++) {
					for (int j = 0; j < input[0].length; j++) {
						cloned[i][j] = ItemStacks.clone(input[i][j]);
					}
				}
				clone.addInput(cloned);
			});
			return clone;
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

		public Outputs deepClone() {
			var clone = new Outputs();
			outputs.forEach(output -> {
				var cloned = new ItemStack[output.length][output[0].length];
				for (int i = 0; i < output.length; i++) {
					for (int j = 0; j < output[0].length; j++) {
						cloned[i][j] = ItemStacks.clone(output[i][j]);
					}
				}
				clone.addOutput(cloned);
			});
			return clone;
		}
	}

	public record IO(Inputs inputs, Outputs outputs) {
		public IO deepClone() {
			return new IO(inputs.deepClone(), outputs.deepClone());
		}
	}

	private static class IOs {
		private final Map<Object, IO> current = new HashMap<>();
		private final Map<Object, IO> result = new HashMap<>();
		private final IOType type;
		private final IO initial;

		private IOs(IOType type, IO initial) {
			this.type = type;
			this.initial = initial;
		}

		public IO getCurrent(Camera<?> camera) {
			var obj = switch (type) {
				case CAMERA -> camera;
				case SHARED -> null;
			};
			return current.computeIfAbsent(obj, k -> initial.deepClone());
		}

		public void setResult(Camera<?> camera, IO result) {
			var obj = switch (type) {
				case CAMERA -> camera;
				case SHARED -> null;
			};
			this.result.put(obj, result);
		}

		public IO getResult(Camera<?> camera) {
			var obj = switch (type) {
				case CAMERA -> camera;
				case SHARED -> null;
			};
			return result.get(obj);
		}

		public void clearResult(Camera<?> camera) {
			var obj = switch (type) {
				case CAMERA -> camera;
				case SHARED -> null;
			};
			result.remove(obj);
		}

		public boolean applyResult(Camera<?> camera) {
			var obj = switch (type) {
				case CAMERA -> camera;
				case SHARED -> null;
			};
			var res = result.remove(obj);
			if (res == null)
				return false;
			current.put(obj, res);
			return true;
		}
	}

	public static class Builder {
		private Mode mode = Mode.AUTO;
		private IOType type = IOType.SHARED;
		private final List<Recipe> recipes = new ArrayList<>();
		private final Inputs inputs = new Inputs();
		private final Outputs outputs = new Outputs();

		private Builder() {
		}

		public CraftingTable build() {
			var table = new CraftingTable();
			table.mode = mode;
			table.ios = new IOs(type, new IO(inputs, outputs));
			table.recipes = recipes;
			return table;
		}

		public Builder mode(Mode mode) {
			this.mode = mode;
			return this;
		}

		public Builder ioType(IOType type) {
			this.type = type;
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
			this.inputs.addInput(maxRow, maxColumn);
			return this;
		}

		public Builder addOutput(int maxRow, int maxColumn) {
			this.outputs.addOutput(maxRow, maxColumn);
			return this;
		}
	}
}
