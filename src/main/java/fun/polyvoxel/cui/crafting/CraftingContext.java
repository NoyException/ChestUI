package fun.polyvoxel.cui.crafting;

import fun.polyvoxel.cui.ui.Camera;
import fun.polyvoxel.cui.util.Context;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;

public class CraftingContext extends Context {
	private final @NotNull Recipe recipe;
	private final @Nullable Camera<?> camera;
	private final @Nullable Player player;
	private LinkedList<Runnable> onRecipeApplied;
	private LinkedList<Runnable> onRecipeFailed;

	public CraftingContext(@NotNull Recipe recipe, @Nullable Camera<?> camera, @Nullable Player player) {
		this.recipe = recipe;
		this.camera = camera;
		this.player = player;
	}

	public @NotNull Recipe getRecipe() {
		return recipe;
	}

	public @Nullable Camera<?> getCamera() {
		return camera;
	}

	public @Nullable Player getPlayer() {
		return player;
	}

	public void onRecipeApplied(Runnable runnable) {
		if (onRecipeApplied == null) {
			onRecipeApplied = new LinkedList<>();
		}
		onRecipeApplied.add(runnable);
	}

	void applyRecipe() {
		if (onRecipeApplied == null) {
			return;
		}
		for (var runnable : onRecipeApplied) {
			runnable.run();
		}
	}

	public void onRecipeFailed(Runnable runnable) {
		if (onRecipeFailed == null) {
			onRecipeFailed = new LinkedList<>();
		}
		onRecipeFailed.add(runnable);
	}

	void failRecipe() {
		if (onRecipeFailed == null) {
			return;
		}
		for (var runnable : onRecipeFailed) {
			runnable.run();
		}
	}
}
