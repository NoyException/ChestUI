package fun.polyvoxel.cui.crafting;

import fun.polyvoxel.cui.util.context.Context;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CraftingContext {
	private final CraftingTable craftingTable;
	private final @Nullable Player player;
	private final Recipe recipe;
	private final @NotNull Context context;

	public CraftingContext(CraftingTable craftingTable) {
		this(craftingTable, null, null, Context.background());
	}

	public CraftingContext(CraftingTable craftingTable, @Nullable Player player, Recipe recipe,
			@NotNull Context context) {
		this.craftingTable = craftingTable;
		this.player = player;
		this.recipe = recipe;
		this.context = context;
	}

	public CraftingContext withPlayer(Player player) {
		return new CraftingContext(craftingTable, player, recipe, context);
	}

	public CraftingContext withRecipe(Recipe recipe) {
		return new CraftingContext(craftingTable, player, recipe, context);
	}

	public CraftingContext withContext(Context context) {
		return new CraftingContext(craftingTable, player, recipe, context);
	}

	public CraftingTable craftingTable() {
		return craftingTable;
	}

	public @Nullable Player player() {
		return player;
	}

	public Recipe recipe() {
		return recipe;
	}

	public @NotNull Context context() {
		return context;
	}
}
