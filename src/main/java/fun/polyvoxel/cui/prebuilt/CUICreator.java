package fun.polyvoxel.cui.prebuilt;

import fun.polyvoxel.cui.layer.Layer;
import fun.polyvoxel.cui.serialize.CUIData;
import fun.polyvoxel.cui.serialize.SerializableChestUI;
import fun.polyvoxel.cui.slot.Button;
import fun.polyvoxel.cui.ui.*;
import fun.polyvoxel.cui.util.context.Context;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.jetbrains.annotations.NotNull;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;

@CUI(name = "creator", icon = Material.COMMAND_BLOCK)
public class CUICreator implements ChestUI<CUICreator> {
	private CUIType<CUICreator> type;

	@Override
	public void onInitialize(CUIType<CUICreator> type) {
		this.type = type.edit().defaultTitle(Component.text("CUI Creator", NamedTextColor.LIGHT_PURPLE))
				.triggerByDisplay(CameraProvider.createCameraInNewCUIInstance()).done();
	}

	@Override
	public @NotNull CUIInstanceHandler<CUICreator> createCUIInstanceHandler(Context context) {
		return new DisplayAllJsonHandler();
	}

	public class DisplayAllJsonHandler implements CUIInstanceHandler<CUICreator> {
		private CUIInstance<CUICreator> cui;
		private int size;

		@Override
		public void onInitialize(CUIInstance<CUICreator> cui) {
			this.cui = cui.edit()
					.layer(0,
							new Layer(1, 9).edit().relative(true)
									.all((row, column) -> Button.builder().material(Material.BLACK_STAINED_GLASS_PANE)
											.displayName(" ").build())
									.slot(0, 0, () -> Button.builder().material(Material.RED_STAINED_GLASS_PANE)
											.displayName("Previous").click(event -> {
												var position = event.getCamera().getPosition();
												if (position.row() <= 0)
													return;
												event.getCamera().edit().move(-5, 0);
											}).build())
									.slot(0, 8, () -> Button.builder().material(Material.GREEN_STAINED_GLASS_PANE)
											.displayName("Next").click(event -> {
												var position = event.getCamera().getPosition();
												if ((position.row() / 5 + 1) * 45 >= size)
													return;
												event.getCamera().edit().move(5, 0);
											}).build())
									.done())
					.done();
			refresh();
		}

		private void refresh() {
			var types = type.getCUIPlugin().getCUIManager().getRegisteredCUITypes().stream()
					.filter(CUIType::isSerializable).toArray(CUIType[]::new);
			size = types.length + 1;
			cui.edit().layer(0, new Layer(0, 9).edit().tile(size, true, index -> {
				if (index == 0) {
					return Button.builder().displayName(Component.text("Create new chest UI", NamedTextColor.GREEN))
							.click(cuiClickEvent -> {
								cui.getCUIPlugin().getTools().createAnvilTextInput(cuiClickEvent.getPlayer(), key -> {
									Path path;
									try {
										path = Path.of(cui.getCUIPlugin().getDataFolder().getAbsolutePath(),
												key + ".json");
									} catch (InvalidPathException e) {
										cuiClickEvent.getPlayer()
												.sendMessage(Component.text("Invalid key", NamedTextColor.RED));
										return;
									}
									if (path.toFile().exists()) {
										cuiClickEvent.getPlayer().sendMessage(
												Component.text("The key already exists", NamedTextColor.RED));
										return;
									}
									cuiClickEvent.getPlayer().sendMessage(Component.text("Key: " + key));
									var chestUI = new SerializableChestUI(new CUIData(), path);
									refresh();
								}).open(cuiClickEvent.getPlayer(), true);
							}).build();
				}
				var type = types[index - 1];
				var chestUI = (SerializableChestUI) type.getChestUI();
				return Button.builder().displayName(type.getKey().toString()).lore(Component.text("Left click to edit"),
						Component.text("Drop(Q) to delete json file", NamedTextColor.RED)).click(event -> {
							if (event.getClickType() == ClickType.DROP) {
								var path = chestUI.getPath();
								if (path != null) {
									if (path.toFile().delete()) {
										refresh();
									} else {
										event.getPlayer().sendMessage(
												Component.text("Failed to delete the json file", NamedTextColor.RED));
									}
								}
							} else if (event.getClickType().isLeftClick()) {

							}
						}).build();
			}).done());
		}

		@Override
		public @NotNull CameraHandler<CUICreator> createCameraHandler(Context context) {
			return camera -> {
			};
		}
	}
}
