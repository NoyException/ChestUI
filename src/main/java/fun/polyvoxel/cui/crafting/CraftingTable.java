package fun.polyvoxel.cui.crafting;

import fun.polyvoxel.cui.layer.Layer;
import fun.polyvoxel.cui.slot.Storage;
import fun.polyvoxel.cui.util.ItemStacks;
import fun.polyvoxel.cui.util.context.Context;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;

public class CraftingTable {
	private final CraftingTableType type;
	private CraftingTableType.IO current;
	private CraftingTableType.IO result;
	private Recipe recipe;
	private Player user;

	public CraftingTable(CraftingTableType type, CraftingTableType.IO initial) {
		this.type = type;
		current = initial;
	}

	public CraftingTableType getType() {
		return type;
	}

	public boolean match(@NotNull Context context, @Nullable Player player) {
		var ctx = new CraftingContext(this).withContext(context).withPlayer(player);
		var matched = new AtomicReference<Pair<Recipe, CraftingTableType.IO>>();
		type.getRecipes().parallelStream().forEach(recipe -> {
			var result = recipe.use(ctx, current);
			if (result != null) {
				matched.set(Pair.of(recipe, result));
			}
		});
		if (matched.get() == null)
			return false;
		setResult(matched.get().getRight(), matched.get().getLeft(), player);
		return true;
	}

	public boolean match(@Nullable Player player) {
		return match(Context.background(), player);
	}

	public CraftingTableType.IO getCurrent() {
		return current;
	}

	public CraftingTableType.IO getResult() {
		return result;
	}

	public Recipe getMatchedRecipe() {
		return recipe;
	}

	public void setResult(CraftingTableType.IO result, Recipe recipe, Player user) {
		this.result = result;
		this.recipe = recipe;
		this.user = user;
	}

	public void clearResult() {
		result = null;
		recipe = null;
		user = null;
	}

	public boolean apply() {
		if (result == null)
			return false;
		current = result;
		recipe.onApply(user);
		clearResult();
		return true;
	}

	public Layer generateInputLayer(int index) {
		var input = current.inputs().getInput(index);

		return new Layer(input.length, input[0].length).edit()
				.all((row, column) -> Storage.builder().source(new InputSource(index, row, column)).build()).done();
	}

	public Layer generateOutputLayer(int index) {
		var output = current.outputs().getOutput(index);

		return new Layer(output.length, output[0].length).edit()
				.all((row, column) -> Storage.builder().source(new OutputSource(index, row, column)).build()).done();
	}

	private abstract static class Source implements Storage.Source {
		protected final int index;
		protected final int row;
		protected final int column;
		private Runnable dirtyMarker;

		private Source(int index, int row, int column) {
			this.index = index;
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
		private InputSource(int index, int row, int column) {
			super(index, row, column);
		}

		@Override
		protected ItemStack[][] getMatrix() {
			return current.inputs().getInput(index);
		}

		@Override
		public void set(ItemStack itemStack, @Nullable Player player) {
			super.set(itemStack, player);
			if (type.getMode() == CraftingTableType.Mode.AUTO) {
				clearResult();
				match(player);
			}
		}

		@Override
		public Storage.Source deepClone() {
			return new InputSource(index, row, column);
		}
	}

	private final class OutputSource extends Source {
		private OutputSource(int index, int row, int column) {
			super(index, row, column);
		}

		@Override
		protected ItemStack[][] getMatrix() {
			if (type.getMode() == CraftingTableType.Mode.AUTO) {
				if (result != null) {
					return result.outputs().getOutput(index);
				}
			}
			return current.outputs().getOutput(index);
		}

		@Override
		public void set(ItemStack itemStack, @Nullable Player player) {
			var dirty = !ItemStacks.isSame(get(), itemStack);
			super.set(itemStack, player);
			if (type.getMode() == CraftingTableType.Mode.AUTO && dirty) {
				apply();
				match(player);
			}
		}

		@Override
		public Storage.Source deepClone() {
			return new OutputSource(index, row, column);
		}

		@Override
		public boolean placeable(ItemStack itemStack) {
			if (type.getMode() == CraftingTableType.Mode.AUTO) {
				return false;
			}
			return super.placeable(itemStack);
		}

		@Override
		public boolean bidirectional() {
			return type.getMode() != CraftingTableType.Mode.AUTO;
		}
	}
}
