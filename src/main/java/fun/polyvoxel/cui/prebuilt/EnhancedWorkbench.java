package fun.polyvoxel.cui.prebuilt;

import fun.polyvoxel.cui.crafting.CraftingTable;
import fun.polyvoxel.cui.crafting.CraftingTableType;
import fun.polyvoxel.cui.crafting.Recipe;
import fun.polyvoxel.cui.crafting.consumer.ShapedConsumer;
import fun.polyvoxel.cui.crafting.consumer.ShapelessConsumer;
import fun.polyvoxel.cui.crafting.consumer.ingredient.BucketIngredient;
import fun.polyvoxel.cui.crafting.consumer.ingredient.ExactIngredient;
import fun.polyvoxel.cui.crafting.consumer.ingredient.MaterialMatchedIngredient;
import fun.polyvoxel.cui.crafting.producer.ShapelessProducer;
import fun.polyvoxel.cui.crafting.producer.product.ExactProduct;
import fun.polyvoxel.cui.layer.Layer;
import fun.polyvoxel.cui.slot.Button;
import fun.polyvoxel.cui.ui.*;
import fun.polyvoxel.cui.ui.source.BlockDisplaySource;
import fun.polyvoxel.cui.ui.source.DisplaySource;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Boat;
import org.bukkit.event.block.Action;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 实验性质：相同方块的界面共享
 */
@CUI(name = "workbench", icon = Material.CRAFTING_TABLE)
public class EnhancedWorkbench implements ChestUI<EnhancedWorkbench> {
	private CUIType<EnhancedWorkbench> type;
	private CraftingTableType enhancedWorkbench;
	private final Map<Block, CUIInstance<EnhancedWorkbench>> byBlock = new HashMap<>();
	private boolean isProvidersEnabled = false;

	@Override
	public void onInitialize(CUIType<EnhancedWorkbench> type) {
		var woods = Arrays.stream(Boat.Type.values()).map(Boat.Type::getMaterial)
				.collect(Collectors.toUnmodifiableSet());
		enhancedWorkbench = CraftingTableType.builder().mode(CraftingTableType.Mode.AUTO).addInput(5, 5).addOutput(5, 3)
				// 木棍
				.addRecipe(Recipe.builder().strict(true)
						.addConsumer(ShapedConsumer.builder().strict(true).pattern("W", "W")
								.set('W', new MaterialMatchedIngredient(woods::contains)).build())
						.addProducer(ShapelessProducer.builder().add(new ExactProduct(Material.STICK, 4)).build())
						.build())
				// 打火石
				.addRecipe(Recipe.builder().strict(true).addConsumer(ShapelessConsumer.builder().strict(true)
						.add(new ExactIngredient(Material.IRON_INGOT)).add(new ExactIngredient(Material.FLINT)).build())
						.addProducer(
								ShapelessProducer.builder().add(new ExactProduct(Material.FLINT_AND_STEEL)).build())
						.build())
				// 蛋糕
				.addRecipe(Recipe.builder().strict(true).addConsumer(ShapedConsumer.builder().strict(true)
						.pattern("MMM", "SES", "WWW").set('M', new BucketIngredient(Material.MILK_BUCKET))
						.set('S', new ExactIngredient(Material.SUGAR)).set('E', new ExactIngredient(Material.EGG))
						.set('W', new ExactIngredient(Material.WHEAT)).build())
						.addProducer(ShapelessProducer.builder().add(new ExactProduct(Material.CAKE)).build()).build())
				.build();

		this.type = type.edit().defaultTitle("Enhanced Workbench").done();
	}

	public void enableProviders() {
		if (isProvidersEnabled) {
			return;
		}
		isProvidersEnabled = true;
		this.type.edit()
				.provideByBlock(event -> event.getAction() == Action.RIGHT_CLICK_BLOCK
						&& event.getClickedBlock().getType() == Material.ENCHANTING_TABLE
						&& event.getClickedBlock().getRelative(BlockFace.DOWN).getType() == Material.CRAFTING_TABLE)
				.provideByItemStack(event -> event.getItem().getType() == Material.CRAFTING_TABLE && !event.hasBlock());
	}

	public CUIInstance<EnhancedWorkbench> getCUIInstance(DisplaySource<?> source) {
		if (source instanceof BlockDisplaySource s) {
			return byBlock.computeIfAbsent(s.getSource(),
					block -> type.createInstance(new InstanceHandler()).edit().keepAlive(true).done());
		}
		return type.createInstance(new InstanceHandler()).edit().keepAlive(true).done();
	}

	@Override
	public @Nullable <S> Camera<EnhancedWorkbench> getDisplayedCamera(DisplayContext<S> context) {
		return getCUIInstance(context.source()).createCamera(camera -> {
		});
	}

	private class InstanceHandler implements CUIInstanceHandler<EnhancedWorkbench> {
		private final CraftingTable craftingTable = enhancedWorkbench.createInstance();

		@Override
		public void onInitialize(CUIInstance<EnhancedWorkbench> cui) {
			var inputLayer = craftingTable.generateInputLayer(0);
			var outputLayer = craftingTable.generateOutputLayer(0).edit().marginLeft(6).done();
			cui.edit()
					.layer(2,
							new Layer(5, 9).edit()
									.column(5,
											row -> Button.builder().material(Material.BLACK_STAINED_GLASS_PANE)
													.displayName(" ").build())
									.slot(2, 5,
											() -> Button.builder().material(Material.CRAFTING_TABLE)
													.displayName("Enhanced Workbench").build())
									.done())
					.layer(1, inputLayer).layer(0, outputLayer).done();
		}

		@Override
		public void onCreateCamera(Camera<EnhancedWorkbench> camera) {
			camera.edit().rowSize(5);
		}
	}
}
