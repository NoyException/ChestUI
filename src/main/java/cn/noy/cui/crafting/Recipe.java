package cn.noy.cui.crafting;

import cn.noy.cui.crafting.consumer.Consumer;
import cn.noy.cui.crafting.producer.Producer;
import cn.noy.cui.ui.Camera;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;

public class Recipe {
	private boolean strict;
	private Consumer[] consumer;
	private Producer[] producer;

	private Recipe() {
	}

	public CraftingTable.@Nullable IO use(@NotNull CraftingTable.IO io, @Nullable Camera<?> camera,
			@Nullable Player player) {
		var ctx = new CraftingContext(this, camera, player);
		var result = use(ctx, io);
		if (result == null) {
			ctx.failRecipe();
			return null;
		}
		ctx.applyRecipe();
		return result;
	}

	private @Nullable CraftingTable.IO use(CraftingContext ctx, @NotNull CraftingTable.IO io) {
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

		var inputsAfter = new CraftingTable.Inputs();
		for (int i = 0; i < consumer.length; i++) {
			var result = consumer[i].consume(ctx, inputs.getInput(i));
			if (result == null) {
				return null;
			}
			inputsAfter.addInput(result);
		}
		var outputsAfter = new CraftingTable.Outputs();
		for (int i = 0; i < producer.length; i++) {
			var result = producer[i].produce(ctx, outputs.getOutput(i));
			if (result == null) {
				return null;
			}
			outputsAfter.addOutput(result);
		}
		return new CraftingTable.IO(inputsAfter, outputsAfter);
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
