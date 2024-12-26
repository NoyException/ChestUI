package fun.polyvoxel.cui.prebuilt.cui;

import fun.polyvoxel.cui.crafting.CraftingTable;
import fun.polyvoxel.cui.crafting.Recipe;
import fun.polyvoxel.cui.crafting.consumer.ShapedConsumer;
import fun.polyvoxel.cui.crafting.consumer.ShapelessConsumer;
import fun.polyvoxel.cui.crafting.consumer.ingredient.BucketIngredient;
import fun.polyvoxel.cui.crafting.consumer.ingredient.ExactIngredient;
import fun.polyvoxel.cui.crafting.consumer.ingredient.MaterialMatchedIngredient;
import fun.polyvoxel.cui.crafting.producer.ShapelessProducer;
import fun.polyvoxel.cui.crafting.producer.product.ExactProduct;
import fun.polyvoxel.cui.layer.Layer;
import fun.polyvoxel.cui.ui.*;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Boat;
import org.bukkit.event.block.Action;
import org.bukkit.metadata.FixedMetadataValue;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.stream.Collectors;

@CUI(name = "ew", singleton = true, icon = Material.CRAFTING_TABLE)
public class EnhancedWorkbench implements ChestUI<EnhancedWorkbench> {
	private CraftingTable craftingTable;

	@Override
	public void onInitialize(CUIType<EnhancedWorkbench> type) {
		var woods = Arrays.stream(Boat.Type.values()).map(Boat.Type::getMaterial)
				.collect(Collectors.toUnmodifiableSet());
		craftingTable = CraftingTable.builder().mode(CraftingTable.Mode.AUTO).addInput(5, 5).addOutput(5, 3)
				.ioType(CraftingTable.IOType.CAMERA)
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

		type.edit().defaultTitle("Enhanced Workbench").triggerByDisplayCommand(
				player -> new CUIType.TriggerResult<>(CUIType.TriggerResultType.CREATE_NEW_CAMERA, camera -> {
				})).triggerByBlock(event -> {
					if (event.getAction() != Action.RIGHT_CLICK_BLOCK
							|| !event.getClickedBlock().getState().hasMetadata("chestui:ew")) {
						return new CUIType.TriggerResult<>(CUIType.TriggerResultType.REJECTED, camera -> {
						});
					}
					return new CUIType.TriggerResult<>(CUIType.TriggerResultType.CREATE_NEW_CAMERA, camera -> {
					});
				});
	}

	@Override
	public @NotNull CUIInstanceHandler<EnhancedWorkbench> createCUIInstanceHandler() {
		return new InstanceHandler();
	}

	private class InstanceHandler implements CUIInstanceHandler<EnhancedWorkbench> {
		@Override
		public void onInitialize(CUIInstance<EnhancedWorkbench> cui) {
			cui.edit().layer(0, new Layer(5, 9).edit().column(5, (slotHandler, integer) -> {
				slotHandler.button(
						builder -> builder.material(Material.BLACK_STAINED_GLASS_PANE).displayName(" ").build());
			}).slot(2, 5, slotHandler -> slotHandler.button(builder -> builder.material(Material.CRAFTING_TABLE)
					.displayName("Put a workbench under your feet").clickHandler(cuiClickEvent -> {
						var player = cuiClickEvent.getPlayer();
						var block = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
						block.setType(Material.CRAFTING_TABLE);
						block.setMetadata("chestui:ew", new FixedMetadataValue(cui.getCUIPlugin(), true));
					}).build())).done()).done();
		}

		@Override
		public @NotNull fun.polyvoxel.cui.ui.CameraHandler<EnhancedWorkbench> createCameraHandler() {
			return new CameraHandler();
		}

		private class CameraHandler implements fun.polyvoxel.cui.ui.CameraHandler<EnhancedWorkbench> {
			@Override
			public void onInitialize(Camera<EnhancedWorkbench> camera) {
				camera.edit().rowSize(5);
				var inputLayer = craftingTable.generateInputLayer(0, camera);
				var outputLayer = craftingTable.generateOutputLayer(0, camera).edit().marginLeft(6).done();
				camera.edit().layer(-1, inputLayer).layer(-2, outputLayer).done();
			}
		}
	}
}
