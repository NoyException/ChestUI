package fun.polyvoxel.cui.prebuilt;

import fun.polyvoxel.cui.layer.Layer;

import fun.polyvoxel.cui.slot.Button;
import fun.polyvoxel.cui.slot.Storage;
import fun.polyvoxel.cui.ui.*;
import fun.polyvoxel.cui.util.ItemStacks;
import fun.polyvoxel.cui.util.Position;
import fun.polyvoxel.cui.util.context.Context;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.WeatherType;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

@CUI(name = "pm", icon = Material.PLAYER_HEAD)
public class PlayerMonitor implements ChestUI<PlayerMonitor> {
	private CUIType<PlayerMonitor> type;
	private CUIInstance<PlayerMonitor> allPlayers;
	private final Map<Player, CUIInstance<PlayerMonitor>> byPlayer = new HashMap<>();
	private final Map<Player, CUIInstance<PlayerMonitor>> backpacks = new HashMap<>();
	private final Map<Player, CUIInstance<PlayerMonitor>> armors = new HashMap<>();

	@Override
	public void onInitialize(CUIType<PlayerMonitor> type) {
		this.type = type.edit().defaultTitle("Inventory Monitor")
				.triggerByDisplay((cuiType, player, asChild) -> getAllPlayers().createCamera()).done();
	}

	@Override
	public @NotNull CUIInstanceHandler<PlayerMonitor> createCUIInstanceHandler(Context context) {
		var player = context.player();
		if (player == null) {
			return new MonitorAllPlayersHandler();
		}
		String monitor = context.get("monitor");
		switch (monitor) {
			case "backpack" -> {
				return new MonitorBackpackHandler(player);
			}
			case "armor" -> {
				return new MonitorArmorHandler(player);
			}
			case null, default -> {
				return new MonitorPlayerHandler(player);
			}
		}
	}

	private CUIInstance<PlayerMonitor> getAllPlayers() {
		if (allPlayers == null) {
			allPlayers = type.createInstance(Context.background());
		}
		return allPlayers;
	}

	private CUIInstance<PlayerMonitor> getByPlayer(Player player) {
		return byPlayer.computeIfAbsent(player, p -> type.createInstance(Context.background().withPlayer(p)));
	}

	private CUIInstance<PlayerMonitor> getBackpack(Player player) {
		return backpacks.computeIfAbsent(player,
				p -> type.createInstance(Context.background().withPlayer(p).withValue("monitor", "backpack")));
	}

	private CUIInstance<PlayerMonitor> getArmor(Player player) {
		return armors.computeIfAbsent(player,
				p -> type.createInstance(Context.background().withPlayer(p).withValue("monitor", "armor")));
	}

	private Button.Builder createButtonForPlayer(Player player) {
		var health = String.format("%.1f", player.getHealth());
		var maxHealth = String.format("%.1f", player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
		var lore = new ArrayList<Component>();
		if (player.isOp()) {
			lore.add(Component.text("- ", NamedTextColor.GRAY)
					.append(Component.text("[OP]", NamedTextColor.RED, TextDecoration.BOLD)));
		}
		if (player.getAllowFlight()) {
			lore.add(Component.text("- ", NamedTextColor.GRAY).append(Component.text("Allow Fly", NamedTextColor.RED)));
		}
		lore.addAll(List.of(
				Component.text("- GameMode: ", NamedTextColor.GRAY)
						.append(Component.text(player.getGameMode().name(), NamedTextColor.RED)),
				Component.text("- UUID: ", NamedTextColor.GRAY)
						.append(Component.text(player.getUniqueId().toString(), NamedTextColor.AQUA)),
				Component.text("- Name: ", NamedTextColor.GRAY)
						.append(Component.text(player.getName(), NamedTextColor.AQUA)),
				Component.text("- World: ", NamedTextColor.GRAY)
						.append(Component.text(player.getWorld().getName(), NamedTextColor.AQUA)),
				Component.text("- Location: ", NamedTextColor.GRAY)
						.append(Component.text(player.getLocation().getBlockX(), NamedTextColor.AQUA))
						.append(Component.text(","))
						.append(Component.text(player.getLocation().getBlockY(), NamedTextColor.AQUA))
						.append(Component.text(","))
						.append(Component.text(player.getLocation().getBlockZ(), NamedTextColor.AQUA)),
				Component.text("- Health: ", NamedTextColor.GRAY).append(Component.text(health, NamedTextColor.AQUA))
						.append(Component.text("/")).append(Component.text(maxHealth, NamedTextColor.AQUA)),
				Component.text("- Food: ", NamedTextColor.GRAY)
						.append(Component.text(player.getFoodLevel(), NamedTextColor.AQUA)),
				Component.text("- Level: ", NamedTextColor.GRAY)
						.append(Component.text(player.getLevel(), NamedTextColor.AQUA)),
				Component.text("- Exp: ", NamedTextColor.GRAY)
						.append(Component.text(player.getExp(), NamedTextColor.AQUA))));
		return Button.builder().skull(player).displayName(player.displayName()).lore(lore);
	}

	private class MonitorAllPlayersHandler implements CUIInstanceHandler<PlayerMonitor> {
		protected CUIInstance<PlayerMonitor> cui;
		protected int size;

		@Override
		public void onInitialize(CUIInstance<PlayerMonitor> cui) {
			this.cui = cui.edit()
					.layer(0,
							new Layer(1, 9).edit().relative(true)
									.all((row, column) -> Button.builder().material(Material.BLACK_STAINED_GLASS_PANE)
											.displayName(" ").build())
									.slot(0, 0, () -> Button.builder().material(Material.RED_STAINED_GLASS_PANE)
											.displayName("Previous").clickHandler(event -> {
												var position = event.getCamera().getPosition();
												if (position.row() <= 0)
													return;
												event.getCamera().edit().move(-5, 0);
											}).build())
									.slot(0, 8, () -> Button.builder().material(Material.GREEN_STAINED_GLASS_PANE)
											.displayName("Next").clickHandler(event -> {
												var position = event.getCamera().getPosition();
												if ((position.row() / 5 + 1) * 45 >= size)
													return;
												event.getCamera().edit().move(5, 0);
											}).build())
									.done())
					.done();
			refresh();
		}

		@Override
		public void onDestroy() {
			allPlayers = null;
		}

		@Override
		public @NotNull CameraHandler<PlayerMonitor> createCameraHandler(Context context) {
			return camera -> camera.edit().rowSize(6);
		}

		private void refresh() {
			var players = Bukkit.getOnlinePlayers().stream().sorted((a, b) -> {
				var aName = a.getName();
				var bName = b.getName();
				return aName.compareTo(bName);
			}).toArray(Player[]::new);
			size = players.length;

			cui.edit().layer(1, new Layer(0, 9).edit().marginTop(1)
					.tile(size, true, index -> createButtonForPlayer(players[index]).clickHandler(cuiClickEvent -> {
						if (cuiClickEvent.getClickType().isLeftClick()) {
							getByPlayer(players[index]).createCamera().open(cuiClickEvent.getPlayer(), true);
						}
					}).build()).done()).done();
		}

		@Override
		public void onTick() {
			if (cui.getTicksLived() % 4 != 0) {
				return;
			}
			refresh();
		}
	}

	private class MonitorPlayerHandler implements CUIInstanceHandler<PlayerMonitor> {
		private final Player player;
		private CUIInstance<PlayerMonitor> cui;
		private Layer operatorsLayer;
		private final Map<Position, Supplier<Boolean>> greenMap = new HashMap<>();

		private MonitorPlayerHandler(Player player) {
			this.player = player;
		}

		@Override
		public void onInitialize(CUIInstance<PlayerMonitor> cui) {
			this.operatorsLayer = new Layer(4, 42).edit().marginTop(2).marginLeft(2).done();
			this.cui = cui.edit().layer(0, new Layer(6, 9).edit().relative(true).row(1,
					column -> Button.builder().material(Material.BLACK_STAINED_GLASS_PANE).displayName(" ").build())
					.column(1,
							row -> Button
									.builder().material(Material.BLACK_STAINED_GLASS_PANE).displayName(" ").build())
					.slot(0, 0, () -> createButtonForPlayer(player).build())
					.slot(2, 0,
							() -> Button.builder().material(Material.BUNDLE)
									.displayName(Component.text("Backpack", TextColor.color(140, 81, 25)))
									.clickHandler(event -> {
										getBackpack(player).getCamera().open(event.getPlayer(), true);
									}).build())
					.slot(3, 0, () -> Button.builder().material(Material.ARMOR_STAND)
							.displayName(Component.text("Armor", NamedTextColor.GRAY)).clickHandler(event -> {
								getArmor(player).getCamera().open(event.getPlayer(), true);
							}).build())
					.slot(4, 0,
							() -> Button.builder().material(Material.ENDER_CHEST)
									.displayName(Component.text("EnderChest", NamedTextColor.DARK_PURPLE))
									.clickHandler(event -> {
										event.getCamera().getManager().closeAll(event.getPlayer(), true);
										event.getPlayer().openInventory(player.getEnderChest());
									}).build())
					.slot(0, 2,
							() -> Button.builder().material(Material.CRAFTING_TABLE)
									.displayName(Component.text("Common Operations", NamedTextColor.GREEN))
									.clickHandler(cuiClickEvent -> {
										if (cuiClickEvent.getClickType().isLeftClick()) {
											cuiClickEvent.getCamera().edit().position(new Position(0, 0));
										}
									}).build())
					.slot(0, 3,
							() -> Button.builder().material(Material.GRASS_BLOCK)
									.displayName(Component.text("GameMode", NamedTextColor.GOLD))
									.clickHandler(cuiClickEvent -> {
										if (cuiClickEvent.getClickType().isLeftClick()) {
											cuiClickEvent.getCamera().edit().position(new Position(0, 7));
										}
									}).build())
					.slot(0, 4,
							() -> Button.builder().material(Material.REDSTONE_BLOCK)
									.displayName(Component.text("Dangerous Operations", NamedTextColor.RED))
									.clickHandler(cuiClickEvent -> {
										if (cuiClickEvent.getClickType().isLeftClick()) {
											cuiClickEvent.getCamera().edit().position(new Position(0, 14));
										}
									}).build())
					.slot(0, 5,
							() -> Button.builder().material(Material.CLOCK)
									.displayName(Component.text("Time", NamedTextColor.LIGHT_PURPLE))
									.clickHandler(cuiClickEvent -> {
										if (cuiClickEvent.getClickType().isLeftClick()) {
											cuiClickEvent.getCamera().edit().position(new Position(0, 21));
										}
									}).build())
					.slot(0, 6, () -> Button.builder().material(Material.WATER_BUCKET)
							.displayName(Component.text("Weather", NamedTextColor.BLUE)).clickHandler(cuiClickEvent -> {
								if (cuiClickEvent.getClickType().isLeftClick()) {
									cuiClickEvent.getCamera().edit().position(new Position(0, 28));
								}
							}).build())
					.slot(0, 7, () -> Button.builder().material(Material.ARMOR_STAND)
							.displayName(Component.text("Pose", NamedTextColor.GRAY)).clickHandler(cuiClickEvent -> {
								if (cuiClickEvent.getClickType().isLeftClick()) {
									cuiClickEvent.getCamera().edit().position(new Position(0, 35));
								}
							}).build())
					.done()).layer(1, operatorsLayer).done();

			addButton(0, 0,
					Button.builder().material(Material.COMPASS)
							.displayName(Component.text("Teleport", NamedTextColor.BLUE))
							.clickHandler(event -> event.getPlayer().teleport(player)),
					() -> null);
			addButton(0, 1,
					Button.builder().material(Material.ELYTRA)
							.displayName(Component.text("Allow Fly", NamedTextColor.AQUA)).clickHandler(
									event -> player.setAllowFlight(!player.getAllowFlight())),
					player::getAllowFlight);
			addButton(0, 2,
					Button.builder().material(Material.SHIELD)
							.displayName(Component.text("Invulnerable", NamedTextColor.LIGHT_PURPLE)).clickHandler(
									event -> player.setInvulnerable(!player.isInvulnerable())),
					player::isInvulnerable);
			addButton(0, 3,
					Button.builder().material(Material.POTION)
							.meta(itemMeta -> ((PotionMeta) itemMeta).setBasePotionType(PotionType.INVISIBILITY))
							.displayName(Component.text("Invisible", NamedTextColor.AQUA))
							.clickHandler(event -> player.setInvisible(!player.isInvisible())),
					player::isInvisible);
			addButton(0, 4,
					Button.builder().material(Material.GLOWSTONE)
							.displayName(Component.text("Glow", NamedTextColor.YELLOW))
							.clickHandler(event -> player.setGlowing(!player.isGlowing())),
					player::isGlowing);
			addButton(0, 5,
					Button.builder().material(Material.CAMPFIRE)
							.displayName(Component.text("Visual Fire", NamedTextColor.RED))
							.clickHandler(event -> player.setVisualFire(!player.isVisualFire())),
					player::isVisualFire);
			addButton(0, 6,
					Button.builder().material(Material.FEATHER)
							.displayName(Component.text("Gravity", NamedTextColor.WHITE))
							.clickHandler(event -> player.setGravity(!player.hasGravity())),
					player::hasGravity);
			addButton(0, 7, Button.builder().material(Material.SADDLE)
					.displayName(Component.text("Ride", NamedTextColor.WHITE)).clickHandler(event -> {
						if (player == event.getPlayer()) {
							return;
						}
						player.addPassenger(event.getPlayer());
					}), () -> !player.getPassengers().isEmpty());

			addButton(1, 0,
					Button.builder().material(Material.GRASS_BLOCK)
							.displayName(Component.text("Creative", NamedTextColor.GOLD))
							.clickHandler(event -> player.setGameMode(GameMode.CREATIVE)),
					() -> player.getGameMode() == GameMode.CREATIVE);
			addButton(1, 1,
					Button.builder().material(Material.IRON_SWORD)
							.displayName(Component.text("Survival", NamedTextColor.GREEN))
							.clickHandler(event -> player.setGameMode(GameMode.SURVIVAL)),
					() -> player.getGameMode() == GameMode.SURVIVAL);
			addButton(1, 2,
					Button.builder().material(Material.MAP)
							.displayName(Component.text("Adventure", NamedTextColor.YELLOW))
							.clickHandler(event -> player.setGameMode(GameMode.ADVENTURE)),
					() -> player.getGameMode() == GameMode.ADVENTURE);
			addButton(1, 3,
					Button.builder().material(Material.ENDER_EYE)
							.displayName(Component.text("Spectator", NamedTextColor.BLUE))
							.clickHandler(event -> player.setGameMode(GameMode.SPECTATOR)),
					() -> player.getGameMode() == GameMode.SPECTATOR);

			addButton(2, 0,
					Button.builder().material(Material.NETHER_STAR)
							.displayName(Component.text("OP", NamedTextColor.GOLD))
							.clickHandler(event -> player.setOp(!player.isOp())),
					player::isOp);
			addButton(2, 1,
					Button.builder().material(Material.BARRIER).displayName(Component.text("Ban", NamedTextColor.RED))
							.clickHandler(event -> player.ban(null, (Date) null, null, true)),
					player::isBanned);
			addButton(2, 2, Button.builder().material(Material.LEATHER_BOOTS)
					.displayName(Component.text("Kick", NamedTextColor.RED)).clickHandler(event -> player.kick()),
					() -> !player.isOnline());
			addButton(2, 3, Button.builder().material(Material.NETHERITE_SWORD)
					.displayName(Component.text("Kill", NamedTextColor.RED)).clickHandler(event -> player.setHealth(0)),
					player::isDead);

			addButton(3, 0,
					Button.builder().material(Material.COMMAND_BLOCK)
							.displayName(Component.text("Reset", NamedTextColor.WHITE))
							.lore(Component.text("Sync with server time", NamedTextColor.GRAY))
							.clickHandler(event -> player.resetPlayerTime()),
					player::isPlayerTimeRelative);
			addButton(3, 1,
					Button.builder().material(Material.CANDLE).displayName(Component.text("Day", NamedTextColor.YELLOW))
							.clickHandler(event -> player.setPlayerTime(1000, false)),
					() -> player.getPlayerTimeOffset() == 1000);
			addButton(3, 2,
					Button.builder().material(Material.YELLOW_CANDLE)
							.displayName(Component.text("Noon", NamedTextColor.GOLD))
							.clickHandler(event -> player.setPlayerTime(6000, false)),
					() -> player.getPlayerTimeOffset() == 6000);
			addButton(3, 3,
					Button.builder().material(Material.LIGHT_GRAY_CANDLE)
							.displayName(Component.text("Night", NamedTextColor.GRAY))
							.clickHandler(event -> player.setPlayerTime(13000, false)),
					() -> player.getPlayerTimeOffset() == 13000);
			addButton(3, 4,
					Button.builder().material(Material.BLACK_CANDLE)
							.displayName(Component.text("Midnight", NamedTextColor.BLACK))
							.clickHandler(event -> player.setPlayerTime(18000, false)),
					() -> player.getPlayerTimeOffset() == 18000);

			addButton(4, 0,
					Button.builder().material(Material.COMMAND_BLOCK)
							.displayName(Component.text("Reset", NamedTextColor.WHITE))
							.lore(Component.text("Sync with server weather", NamedTextColor.GRAY)).clickHandler(
									event -> player.resetPlayerWeather()),
					() -> player.getPlayerWeather() == null);
			addButton(4, 1,
					Button.builder().material(Material.SUNFLOWER)
							.displayName(Component.text("Clear", NamedTextColor.YELLOW))
							.clickHandler(event -> player.setPlayerWeather(WeatherType.CLEAR)),
					() -> player.getPlayerWeather() == WeatherType.CLEAR);
			addButton(4, 2,
					Button.builder().material(Material.TRIDENT)
							.displayName(Component.text("Downfall", NamedTextColor.AQUA))
							.clickHandler(event -> player.setPlayerWeather(WeatherType.DOWNFALL)),
					() -> player.getPlayerWeather() == WeatherType.DOWNFALL);
		}

		@Override
		public void onDestroy() {
			byPlayer.remove(player);
		}

		public void addButton(int page, int index, Button.Builder button, Supplier<Boolean> isGreen) {
			var row = index / 7 * 2;
			var column = index % 7 + page * 7;
			operatorsLayer.edit().slot(row, column, button::build);
			greenMap.put(new Position(row + 1, column), isGreen);
		}

		private void refresh() {
			greenMap.forEach((position, isGreen) -> {
				Boolean b = isGreen.get();
				if (b == null) {
					operatorsLayer.edit().slot(position, () -> Button.builder().material(Material.YELLOW_STAINED_GLASS)
							.displayName(Component.text("✧", NamedTextColor.YELLOW)).build());
				} else if (b) {
					operatorsLayer.edit().slot(position, () -> Button.builder().material(Material.LIME_STAINED_GLASS)
							.displayName(Component.text("✓", NamedTextColor.GREEN)).build());
				} else {
					operatorsLayer.edit().slot(position, () -> Button.builder().material(Material.RED_STAINED_GLASS)
							.displayName(Component.text("✗", NamedTextColor.RED)).build());
				}
			});
		}

		@Override
		public void onTick() {
			if (cui.getTicksLived() % 4 != 0) {
				return;
			}
			refresh();
		}

		@Override
		public @NotNull CameraHandler<PlayerMonitor> createCameraHandler(Context context) {
			return camera -> camera.edit().rowSize(6);
		}
	}

	private class MonitorBackpackHandler implements CUIInstanceHandler<PlayerMonitor> {
		private final Player player;

		private MonitorBackpackHandler(Player player) {
			this.player = player;
		}

		@Override
		public void onInitialize(CUIInstance<PlayerMonitor> cui) {
			var inventory = player.getInventory();
			cui.edit()
					.layer(0,
							new Layer(1, 9).edit()
									.all((row, column) -> Button.builder().material(Material.BLACK_STAINED_GLASS_PANE)
											.displayName(" ").build())
									.done())
					.layer(1,
							new Layer(5, 9).edit().marginTop(1)
									.row(3, column -> Button.builder().material(Material.WHITE_STAINED_GLASS_PANE)
											.displayName(" ").build())
									.row(4, column -> Storage.builder().fromInventory(inventory, column).build())
									.rect(0, 0, 3, 9, (row, column) -> {
										int index = row * 9 + column;
										return Storage.builder().fromInventory(inventory, index + 9).build();
									}).done());
		}

		@Override
		public void onDestroy() {
			backpacks.remove(player);
		}

		@Override
		public @NotNull CameraHandler<PlayerMonitor> createCameraHandler(Context context) {
			return camera -> camera.edit().rowSize(6);
		}
	}

	private class MonitorArmorHandler implements CUIInstanceHandler<PlayerMonitor> {
		private final Player player;

		private MonitorArmorHandler(Player player) {
			this.player = player;
		}

		private static class CursorSource implements Storage.Source {
			private final Player player;
			private Runnable dirtyMarker;
			private ItemStack last;

			private CursorSource(Player player) {
				this.player = player;
			}

			@Override
			public ItemStack get() {
				return player.getItemOnCursor();
			}

			@Override
			public void set(ItemStack itemStack, @Nullable Player player) {
				this.player.setItemOnCursor(itemStack);
			}

			@Override
			public void setDirtyMarker(Runnable dirtyMarker) {
				this.dirtyMarker = dirtyMarker;
			}

			@Override
			public void tick() {
				var current = get();
				if (!ItemStacks.isSame(current, last)) {
					dirtyMarker.run();
					last = ItemStacks.clone(current);
				}
			}

			@Override
			public Storage.Source deepClone() {
				return new CursorSource(player);
			}
		}

		@Override
		public void onInitialize(CUIInstance<PlayerMonitor> cui) {
			var inventory = player.getInventory();
			cui.edit()
					.layer(1, new Layer(6, 9).edit()
							.all((row, column) -> Button.builder().material(Material.BLACK_STAINED_GLASS_PANE)
									.displayName(" ").build())
							.slot(1, 1,
									() -> Button.builder().material(Material.ITEM_FRAME)
											.displayName(Component.text("Helmet", NamedTextColor.GRAY)).build())
							.slot(2, 1,
									() -> Button.builder().material(Material.ITEM_FRAME).amount(2)
											.displayName(Component.text("Chestplate", NamedTextColor.GRAY)).build())
							.slot(3, 1,
									() -> Button.builder().material(Material.ITEM_FRAME).amount(3)
											.displayName(Component.text("Leggings", NamedTextColor.GRAY)).build())
							.slot(4, 1,
									() -> Button.builder().material(Material.ITEM_FRAME).amount(4)
											.displayName(Component.text("Boots", NamedTextColor.GRAY)).build())
							.slot(4, 7,
									() -> Button.builder().material(Material.ITEM_FRAME).amount(5)
											.displayName(Component.text("Off Hand", NamedTextColor.GRAY)).build())
							.slot(1, 7,
									() -> Button.builder().material(Material.ITEM_FRAME).amount(6)
											.displayName(Component.text("Cursor", NamedTextColor.GRAY)).build())
							.done())
					.layer(0, new Layer(6, 9).edit()
							.slot(1, 2, () -> Storage.builder().fromInventory(inventory, 36).build())
							.slot(2, 2, () -> Storage.builder().fromInventory(inventory, 37).build())
							.slot(3, 2, () -> Storage.builder().fromInventory(inventory, 38).build())
							.slot(4, 2, () -> Storage.builder().fromInventory(inventory, 39).build())
							.slot(4, 6, () -> Storage.builder().fromInventory(inventory, 40).build())
							.slot(1, 6, () -> Storage.builder().source(new CursorSource(player)).build()).done());
		}

		@Override
		public void onDestroy() {
			armors.remove(player);
		}

		@Override
		public @NotNull CameraHandler<PlayerMonitor> createCameraHandler(Context context) {
			return camera -> camera.edit().rowSize(6);
		}
	}
}
