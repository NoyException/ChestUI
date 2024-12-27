package fun.polyvoxel.cui.prebuilt.cui;

import fun.polyvoxel.cui.event.CUIRegisterEvent;
import fun.polyvoxel.cui.layer.Layer;

import fun.polyvoxel.cui.slot.Button;
import fun.polyvoxel.cui.ui.*;
import fun.polyvoxel.cui.util.Context;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@CUI(name = "monitor", singleton = true, icon = Material.BARRIER)
public class CUIMonitor implements ChestUI<CUIMonitor> {
	private CUIType<CUIMonitor> type;

	@Override
	public void onInitialize(CUIType<CUIMonitor> type) {
		this.type = type.edit().defaultTitle(Component.text("CUI Monitor", NamedTextColor.RED)).triggerByDisplayCommand(
				player -> new CUIType.TriggerResult<>(CUIType.TriggerResultType.CREATE_NEW_CAMERA, camera -> {
				})).done();
	}

	@Override
	public @NotNull CUIInstanceHandler<CUIMonitor> createCUIInstanceHandler(Context context) {
		return new InstanceHandler();
	}

	private class InstanceHandler implements CUIInstanceHandler<CUIMonitor> {
		private CUIInstance<CUIMonitor> cui;
		private int size;

		@Override
		public void onInitialize(CUIInstance<CUIMonitor> cui) {
			this.cui = cui.edit().keepAlive(true).done();
			refreshCUIList();
			Bukkit.getPluginManager().registerEvents(new Listener() {
				@EventHandler
				public void onRegister(CUIRegisterEvent<?> event) {
					refreshCUIList();
				}
			}, cui.getCUIPlugin());
		}

		private void refreshCUIList() {
			var cuiTypes = cui.getCUIPlugin().getCUIManager().getRegisteredCUITypes();
			size = cuiTypes.size();
			cuiTypes.remove(type);

			cui.edit().layer(1, new Layer(0, 9).edit().marginTop(1).tile(size, true, index -> {
				if (index == 0) {
					return Button.builder().material(Material.BARRIER).displayName("Change mode")
							.lore("To be implemented...").build();
				}
				var cuiType = cuiTypes.get(index - 1);
				var lore = new ArrayList<Component>(List.of(Component.text("Left click to manage")));
				if (cuiType.canDisplay()) {
					lore.add(Component.text("Right click to display", NamedTextColor.GREEN));
				}
				if (cuiType.isSingleton()) {
					lore.add(Component.text("- ", NamedTextColor.GRAY)
							.append(Component.text("Singleton", NamedTextColor.RED)));
				}
				if (cuiType.isSerializable()) {
					lore.add(Component.text("- ", NamedTextColor.GRAY)
							.append(Component.text("Loaded from json", NamedTextColor.BLUE)));
				} else if (cuiType.getPlugin() != null) {
					lore.add(Component.text("- ", NamedTextColor.GRAY).append(Component.text("Registered by plugin: "))
							.append(Component.text(cuiType.getPlugin().getName(), NamedTextColor.AQUA)));
				}
				lore.addAll(List.of(
						Component.text("- Default title: ", NamedTextColor.GRAY).append(cuiType.getDefaultTitle()),
						Component.text("- Current instance count: ", NamedTextColor.GRAY)
								.append(Component.text(cuiType.getInstanceCount(), NamedTextColor.AQUA))));
				return Button.builder().material(cuiType.getIcon())
						.displayName(Component.text(cuiType.getKey().toString(), NamedTextColor.GOLD)).lore(lore)
						.clickHandler(event -> {
							Player player = event.getPlayer();
							if (event.getClickType().isLeftClick()) {
								var camera = cui.createCamera().edit()
										.title(Component.text("Managing ").append(
												Component.text(cuiType.getKey().toString(), NamedTextColor.AQUA)))
										.done();
								var handler = (CameraHandler) camera.getHandler();
								handler.cuiType = cuiType;
								handler.refresh();
								camera.open(player, true);
							} else if (event.getClickType().isRightClick()) {
								if (cuiType.canDisplay()) {
									cuiType.display(player, true);
								}
							}
						}).build();
			}).done()).done();
		}

		@Override
		public void onTick() {
			if (cui.getTicksLived() % 4 != 0) {
				return;
			}
			refreshCUIList();
		}

		@Override
		public @NotNull fun.polyvoxel.cui.ui.CameraHandler<CUIMonitor> createCameraHandler(Context context) {
			return new CameraHandler();
		}

		private class CameraHandler implements fun.polyvoxel.cui.ui.CameraHandler<CUIMonitor> {
			private Camera<CUIMonitor> camera;
			private CUIType<?> cuiType;
			private CUIInstance<?> cuiInstance;
			private int size;

			@Override
			public void onInitialize(Camera<CUIMonitor> camera) {
				this.camera = camera.edit().rowSize(6)
						.layer(0,
								new Layer(1, 9).edit().relative(true)
										.all((row, column) -> Button.builder()
												.material(Material.BLACK_STAINED_GLASS_PANE).displayName(" ").build())
										.slot(0, 0, () -> Button.builder().material(Material.RED_STAINED_GLASS_PANE)
												.displayName("Previous").clickHandler(event -> {
													var position = camera.getPosition();
													if (position.row() <= 0)
														return;
													camera.edit().move(-5, 0);
												}).build())
										.slot(0, 8, () -> Button.builder().material(Material.GREEN_STAINED_GLASS_PANE)
												.displayName("Next").clickHandler(event -> {
													var position = camera.getPosition();
													if ((position.row() / 5 + 1) * 45 >= size)
														return;
													camera.edit().move(5, 0);
												}).build())
										.done())
						.done();
			}

			private void manageCUI(CUIType<?> cuiType) {
				var instances = cuiType.getInstances();
				size = instances.size() + 1;
				camera.edit().layer(1, new Layer(0, 9).edit().marginTop(1).tile(size, true, index -> {
					if (index == 0) {
						if (cuiType.isSingleton() && size > 1) {
							return Button.builder().material(Material.RED_CONCRETE)
									.displayName(Component.text("New instance", NamedTextColor.RED)).lore(Component
											.text("Singleton ChestUI can only have one instance", NamedTextColor.RED))
									.build();
						}
						return Button.builder().material(Material.GREEN_CONCRETE)
								.displayName(Component.text("New instance", NamedTextColor.GREEN))
								.lore(Component.text("Click to create a new instance"),
										Component.text("Noticed that this will be kept alive", NamedTextColor.RED),
										Component.text("until you destroy it manually", NamedTextColor.RED))
								.clickHandler(event -> cuiType.createInstance().edit().keepAlive(true)).build();
					}
					var instance = instances.get(index - 1);
					var lore = new ArrayList<Component>(List.of(Component.text("Left click to manage"),
							Component.text("Drop(Q) to destroy", NamedTextColor.RED)));
					if (instance.isKeepAlive()) {
						lore.add(Component.text("- ", NamedTextColor.GRAY)
								.append(Component.text("Keep Alive", NamedTextColor.RED)));
					}
					lore.addAll(List.of(
							Component.text("- State: ", NamedTextColor.GRAY)
									.append(Component.text(instance.getState().name(), NamedTextColor.AQUA)),
							Component.text("- Default title: ", NamedTextColor.GRAY)
									.append(instance.getType().getDefaultTitle()),
							Component.text("- Current camera count: ", NamedTextColor.GRAY)
									.append(Component.text(instance.getCameraCount(), NamedTextColor.AQUA)),
							Component.text("- Ticks lived: ", NamedTextColor.GRAY)
									.append(Component.text(instance.getTicksLived(), NamedTextColor.AQUA))));
					return Button.builder().material(cuiType.getIcon())
							.displayName(Component.text("#" + instance.getId(), NamedTextColor.GOLD)).lore(lore)
							.clickHandler(event -> {
								if (event.getClickType().isLeftClick()) {
									var camera = cui.createCamera().edit()
											.title(Component.text("Managing ")
													.append(Component.text(instance.getName(), NamedTextColor.AQUA)))
											.done();
									var handler = (CameraHandler) camera.getHandler();
									handler.cuiInstance = instance;
									handler.refresh();
									camera.open(event.getPlayer(), true);
								} else if (event.getClickType() == ClickType.DROP) {
									instance.destroy();
								}
							}).build();
				}).done());
			}

			private void manageCUIInstance(CUIInstance<?> instance) {
				var cameras = instance.getCameras();
				size = cameras.size() + 1;
				camera.edit().layer(1, new Layer(0, 9).edit().marginTop(1).tile(size, true, index -> {
					if (index == 0) {
						return Button.builder().material(Material.GREEN_CONCRETE)
								.displayName(Component.text("New camera", NamedTextColor.GREEN))
								.lore(Component.text("Click to create a new camera"),
										Component.text("Noticed that this will be kept alive", NamedTextColor.RED),
										Component.text("until you destroy it manually", NamedTextColor.RED))
								.clickHandler(event -> instance.createCamera().edit().keepAlive(true)).build();
					}
					var camera = cameras.get(index - 1);
					var lore = new ArrayList<Component>(List.of(Component.text("Left click to open"),
							Component.text("Drop(Q) to destroy", NamedTextColor.RED)));
					if (camera.isKeepAlive()) {
						lore.add(Component.text("- ", NamedTextColor.GRAY)
								.append(Component.text("Keep Alive", NamedTextColor.RED)));
					}
					if (!camera.isClosable()) {
						lore.add(Component.text("- ", NamedTextColor.GRAY)
								.append(Component.text("Not Closable", NamedTextColor.RED)));
					}
					lore.addAll(List.of(
							Component.text("- State: ", NamedTextColor.GRAY)
									.append(Component.text(camera.getState().name(), NamedTextColor.AQUA)),
							Component.text("- Title: ", NamedTextColor.GRAY).append(camera.getTitle()),
							Component.text("- Current viewer count: ", NamedTextColor.GRAY)
									.append(Component.text(camera.getViewerCount(), NamedTextColor.AQUA)),
							Component.text("- Ticks lived: ", NamedTextColor.GRAY)
									.append(Component.text(camera.getTicksLived(), NamedTextColor.AQUA))));

					return Button.builder().material(Material.SPYGLASS)
							.displayName(Component.text("#" + camera.getId(), NamedTextColor.GOLD)).lore(lore)
							.clickHandler(event -> {
								if (event.getClickType().isLeftClick()) {
									camera.open(event.getPlayer(), true);
								} else if (event.getClickType() == ClickType.DROP) {
									camera.destroy();
								}
							}).build();
				}).done());
			}

			private void refresh() {
				if (cuiType != null) {
					manageCUI(cuiType);
				} else if (cuiInstance != null) {
					manageCUIInstance(cuiInstance);
				} else {
					size = InstanceHandler.this.size;
				}
			}

			@Override
			public void onTick() {
				if (camera.getTicksLived() % 4 != 0) {
					return;
				}
				if (camera.getViewerCount() == 0) {
					return;
				}
				refresh();
			}
		}
	}
}
