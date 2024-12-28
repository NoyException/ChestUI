package fun.polyvoxel.cui.crafting;

import fun.polyvoxel.cui.crafting.consumer.Consumer;
import fun.polyvoxel.cui.crafting.producer.Producer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;

public class Recipe {
	private boolean strict;
	private Consumer[] consumer;
	private Producer[] producer;
	private final List<OnApplyListener> onApplyListeners = new LinkedList<>();

	private Recipe() {
	}

	public CraftingTableType.@Nullable IO use(@NotNull CraftingContext context, @NotNull CraftingTableType.IO io) {
		context = context.withRecipe(this);
		var inputs = io.inputs();
		var maxInputs = inputs.getMaxIndex();
		if (maxInputs < consumer.length) {
			return null;
		}
		if (strict && maxInputs > consumer.length) {
			return null;
		}

		var outputs = io.outputs();
		var maxOutputs = outputs.getMaxIndex();
		if (maxOutputs < producer.length) {
			return null;
		}
		if (strict && maxOutputs > producer.length) {
			return null;
		}

		var inputsAfter = new CraftingTableType.Inputs();
		for (int i = 0; i < consumer.length; i++) {
			var result = consumer[i].consume(context, inputs.getInput(i));
			if (result == null) {
				return null;
			}
			inputsAfter.addInput(result);
		}
		var outputsAfter = new CraftingTableType.Outputs();
		for (int i = 0; i < producer.length; i++) {
			var result = producer[i].produce(context, outputs.getOutput(i));
			if (result == null) {
				return null;
			}
			outputsAfter.addOutput(result);
		}
		return new CraftingTableType.IO(inputsAfter, outputsAfter);
	}

	public void onApply(Player user) {
		onApplyListeners.removeIf(listener -> listener.onApply(user));
	}

	public interface OnApplyListener {
		/**
		 * 当配方应用成功时调用<br>
		 * This method is called when the recipe is successfully applied
		 *
		 * @param user
		 *            使用者<br>
		 *            the user
		 * @return true表示移除本监听器，false表示保留<br>
		 *         true means remove this listener, false means keep it
		 */
		boolean onApply(@Nullable Player user);
	}

	public void addOnApply(OnApplyListener onApplyListener) {
		onApplyListeners.add(onApplyListener);
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private boolean strict = false;
		private final List<Consumer> consumers = new LinkedList<>();
		private final List<Producer> producers = new LinkedList<>();

		private Builder() {
		}

		/**
		 * 是否要求输入/输出的层数和Consumer/Producer的数量严格一致。如果不要求，将允许前者多于后者。<br>
		 * Whether the number of layers of input/output must be strictly consistent with
		 * the number of Consumer/Producer. If not, the former will be allowed to be
		 * more than the latter.
		 *
		 * @param strict
		 *            是否严格匹配。默认为false<br>
		 *            Whether to match strictly. The default is false
		 * @return this
		 */
		public Builder strict(boolean strict) {
			this.strict = strict;
			return this;
		}

		public Builder addConsumer(Consumer consumer) {
			consumers.add(consumer);
			return this;
		}

		public Builder addProducer(Producer producer) {
			producers.add(producer);
			return this;
		}

		public Recipe build() {
			var recipe = new Recipe();
			recipe.strict = strict;
			recipe.consumer = consumers.toArray(new Consumer[0]);
			recipe.producer = producers.toArray(new Producer[0]);
			return recipe;
		}
	}
}
