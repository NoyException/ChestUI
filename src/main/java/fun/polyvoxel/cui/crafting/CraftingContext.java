package fun.polyvoxel.cui.crafting;

import fun.polyvoxel.cui.util.context.Context;
import org.jetbrains.annotations.NotNull;

public class CraftingContext implements Context {
	private final @NotNull Context context;
	private final CraftingTable craftingTable;
	private final Recipe recipe;

	public CraftingContext(@NotNull Context context, CraftingTable craftingTable, Recipe recipe) {
		this.context = context;
		this.craftingTable = craftingTable;
		this.recipe = recipe;
	}

	public static CraftingContext background() {
		return new CraftingContext(Context.background(), null, null);
	}

	public static CraftingContext fromContext(Context context) {
		return new CraftingContext(context, null, null);
	}

	public CraftingContext withContext(Context context) {
		return new CraftingContext(context, craftingTable, recipe);
	}

	public CraftingContext withCraftingTable(CraftingTable craftingTable) {
		return new CraftingContext(context, craftingTable, recipe);
	}

	public CraftingContext withRecipe(Recipe recipe) {
		return new CraftingContext(context, craftingTable, recipe);
	}

	public CraftingTable craftingTable() {
		return craftingTable;
	}

	public Recipe recipe() {
		return recipe;
	}

	@Override
	public @NotNull Context parent() {
		return context;
	}
}
