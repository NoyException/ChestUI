package fun.polyvoxel.cui.prebuilt;

import fun.polyvoxel.cui.CUIPlugin;
import fun.polyvoxel.cui.layer.Layer;

import fun.polyvoxel.cui.slot.Button;
import fun.polyvoxel.cui.slot.Storage;
import fun.polyvoxel.cui.ui.*;
import fun.polyvoxel.cui.util.ItemStacks;
import fun.polyvoxel.cui.util.Position;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.WeatherType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

@CUI(name = "pm", icon = Material.PLAYER_HEAD)
public class PlayerMonitor implements ChestUI<PlayerMonitor> {
	private CUIPlugin plugin;
	private CUIType<PlayerMonitor> type;
	private CUIInstance<PlayerMonitor> allPlayers;
	private final Map<Player, CUIInstance<PlayerMonitor>> byPlayer = new HashMap<>();
	private final Map<Player, CUIInstance<PlayerMonitor>> backpacks = new HashMap<>();
	private final Map<Player, CUIInstance<PlayerMonitor>> armors = new HashMap<>();

	@Override
	public void onInitialize(CUIType<PlayerMonitor> type) {
		this.plugin = type.getCUIPlugin();
		this.type = type.edit().defaultTitle("Inventory Monitor").done();
	}

	@Override
	public @Nullable <S> Camera<PlayerMonitor> getDisplayedCamera(DisplayContext<S> context) {
		return getAllPlayers().createCamera(camera -> {
		});
	}

	private CUIInstance<PlayerMonitor> getAllPlayers() {
		if (allPlayers == null) {
			allPlayers = type.createInstance(new MonitorAllPlayersHandler());
		}
		return allPlayers;
	}

	private CUIInstance<PlayerMonitor> getByPlayer(Player player) {
		return byPlayer.computeIfAbsent(player, p -> type.createInstance(new MonitorPlayerHandler(player)));
	}

	private CUIInstance<PlayerMonitor> getBackpack(Player player) {
		return backpacks.computeIfAbsent(player, p -> type.createInstance(new MonitorBackpackHandler(player)));
	}

	private CUIInstance<PlayerMonitor> getArmor(Player player) {
		return armors.computeIfAbsent(player, p -> type.createInstance(new MonitorArmorHandler(player)));
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

		@Override
		public void onDestroy() {
			allPlayers = null;
		}

		private void refresh() {
			var players = Bukkit.getOnlinePlayers().stream().sorted((a, b) -> {
				var aName = a.getName();
				var bName = b.getName();
				return aName.compareTo(bName);
			}).toArray(Player[]::new);
			size = players.length;

			cui.edit().layer(1, new Layer(0, 9).edit().marginTop(1).tile(size, true, index -> {
				var player = players[index];
				var health = String.format("%.1f", player.getHealth());
				var maxHealth = String.format("%.1f", player.getMaxHealth());
				var lore = new ArrayList<Component>();
				if (player.isOp()) {
					lore.add(Component.text("- ", NamedTextColor.GRAY)
							.append(Component.text("[OP]", NamedTextColor.RED, TextDecoration.BOLD)));
				}
				if (player.getAllowFlight()) {
					lore.add(Component.text("- ", NamedTextColor.GRAY)
							.append(Component.text("Allow Fly", NamedTextColor.RED)));
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
						Component.text("- Health: ", NamedTextColor.GRAY)
								.append(Component.text(health, NamedTextColor.AQUA)).append(Component.text("/"))
								.append(Component.text(maxHealth, NamedTextColor.AQUA)),
						Component.text("- Food: ", NamedTextColor.GRAY)
								.append(Component.text(player.getFoodLevel(), NamedTextColor.AQUA)),
						Component.text("- Level: ", NamedTextColor.GRAY)
								.append(Component.text(player.getLevel(), NamedTextColor.AQUA)),
						Component.text("- Exp: ", NamedTextColor.GRAY)
								.append(Component.text(player.getExp(), NamedTextColor.AQUA))));
				return Button.builder().skull(player).displayName(player.displayName()).lore(lore)
						.click(cuiClickEvent -> {
							if (cuiClickEvent.getClickType().isLeftClick()) {
								getByPlayer(player).createCamera(camera -> {
								}).open(cuiClickEvent.getPlayer(), true);
							}
						}).build();
			}).done()).done();
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
			this.operatorsLayer = new Layer(4, 49).edit().marginTop(2).marginLeft(2).done();
			this.cui = cui.edit().layer(0, new Layer(6, 9).edit().relative(true).row(1,
					column -> Button.builder().material(Material.BLACK_STAINED_GLASS_PANE).displayName(" ").build())
					.column(1,
							row -> Button.builder().material(Material.BLACK_STAINED_GLASS_PANE).displayName(" ")
									.build())
					.slot(0, 0, () -> Button.builder().skull(player).displayName(player.displayName()).build())
					.slot(2, 0, () -> Button.builder().material(Material.BUNDLE)
							.displayName(Component.text("Backpack", TextColor.color(140, 81, 25))).click(event -> {
								getBackpack(player).createCamera(camera -> {
								}).open(event.getPlayer(), true);
							}).build())
					.slot(3, 0, () -> Button.builder().material(Material.ARMOR_STAND)
							.displayName(Component.text("Armor", NamedTextColor.GRAY)).click(event -> {
								getArmor(player).createCamera(camera -> {
								}).open(event.getPlayer(), true);
							}).build())
					.slot(4, 0, () -> Button.builder().material(Material.ENDER_CHEST)
							.displayName(Component.text("Ender Chest", NamedTextColor.DARK_PURPLE)).click(event -> {
								plugin.getTools().wrapInventory(player.getEnderChest()).open(event.getPlayer(), true);
							}).build())
					// TODO: 跟踪该玩家看UI的视角
					.slot(0, 2,
							() -> Button.builder().material(Material.CRAFTING_TABLE)
									.displayName(Component.text("Common Operations", NamedTextColor.GREEN))
									.click(cuiClickEvent -> {
										if (cuiClickEvent.getClickType().isLeftClick()) {
											cuiClickEvent.getCamera().edit().position(new Position(0, 0));
										}
									}).build())
					.slot(0, 3, () -> Button.builder().material(Material.GRASS_BLOCK)
							.displayName(Component.text("GameMode", NamedTextColor.GOLD)).click(cuiClickEvent -> {
								if (cuiClickEvent.getClickType().isLeftClick()) {
									cuiClickEvent.getCamera().edit().position(new Position(0, 7));
								}
							}).build())
					.slot(0, 4,
							() -> Button.builder().material(Material.REDSTONE_BLOCK)
									.displayName(Component.text("Dangerous Operations", NamedTextColor.RED))
									.click(cuiClickEvent -> {
										if (cuiClickEvent.getClickType().isLeftClick()) {
											cuiClickEvent.getCamera().edit().position(new Position(0, 14));
										}
									}).build())
					.slot(0, 5, () -> Button.builder().material(Material.CLOCK)
							.displayName(Component.text("Time", NamedTextColor.LIGHT_PURPLE)).click(cuiClickEvent -> {
								if (cuiClickEvent.getClickType().isLeftClick()) {
									cuiClickEvent.getCamera().edit().position(new Position(0, 21));
								}
							}).build())
					.slot(0, 6, () -> Button.builder().material(Material.WATER_BUCKET)
							.displayName(Component.text("Weather", NamedTextColor.BLUE)).click(cuiClickEvent -> {
								if (cuiClickEvent.getClickType().isLeftClick()) {
									cuiClickEvent.getCamera().edit().position(new Position(0, 28));
								}
							}).build())
					.slot(0, 7, () -> Button.builder().material(Material.ARMOR_STAND)
							.displayName(Component.text("Pose", NamedTextColor.GRAY)).click(cuiClickEvent -> {
								if (cuiClickEvent.getClickType().isLeftClick()) {
									cuiClickEvent.getCamera().edit().position(new Position(0, 35));
								}
							}).build())
					.slot(0, 8, () -> Button.builder().material(Material.EXPERIENCE_BOTTLE)
							.displayName(Component.text("Level and Exp", NamedTextColor.GRAY)).click(cuiClickEvent -> {
								if (cuiClickEvent.getClickType().isLeftClick()) {
									cuiClickEvent.getCamera().edit().position(new Position(0, 42));
								}
							}).build())
					.done()).layer(1, operatorsLayer).done();

			addButton(0, 0,
					Button.builder().material(Material.COMPASS)
							.displayName(Component.text("Teleport", NamedTextColor.BLUE))
							.click(event -> event.getPlayer().teleport(player)),
					() -> null);
			addButton(0, 1,
					Button.builder().material(Material.ELYTRA)
							.displayName(Component.text("Allow Fly", NamedTextColor.AQUA))
							.click(event -> player.setAllowFlight(!player.getAllowFlight())),
					player::getAllowFlight);
			addButton(0, 2,
					Button.builder().material(Material.SHIELD)
							.displayName(Component.text("Invulnerable", NamedTextColor.LIGHT_PURPLE))
							.click(event -> player.setInvulnerable(!player.isInvulnerable())),
					player::isInvulnerable);
			addButton(0, 3,
					Button.builder().material(Material.POTION)
							.meta(itemMeta -> ((PotionMeta) itemMeta).setBasePotionType(PotionType.INVISIBILITY))
							.displayName(Component.text("Invisible", NamedTextColor.AQUA))
							.click(event -> player.setInvisible(!player.isInvisible())),
					player::isInvisible);
			addButton(0, 4,
					Button.builder().material(Material.GLOWSTONE)
							.displayName(Component.text("Glow", NamedTextColor.YELLOW))
							.click(event -> player.setGlowing(!player.isGlowing())),
					player::isGlowing);
			addButton(0, 5,
					Button.builder().material(Material.CAMPFIRE)
							.displayName(Component.text("Visual Fire", NamedTextColor.RED))
							.click(event -> player.setVisualFire(!player.isVisualFire())),
					player::isVisualFire);
			addButton(0, 6,
					Button.builder().material(Material.FEATHER)
							.displayName(Component.text("Gravity", NamedTextColor.WHITE))
							.click(event -> player.setGravity(!player.hasGravity())),
					player::hasGravity);
			addButton(0, 7, Button.builder().material(Material.SADDLE)
					.displayName(Component.text("Ride", NamedTextColor.WHITE)).click(event -> {
						if (player == event.getPlayer()) {
							return;
						}
						player.addPassenger(event.getPlayer());
					}), () -> !player.getPassengers().isEmpty());
			addButton(0, 8,
					Button.builder().material(Material.TURTLE_HELMET)
							.displayName(Component.text("Eject", NamedTextColor.GREEN)).click(event -> player.eject()),
					() -> player.getPassengers().isEmpty());
			addButton(0, 9,
					Button.builder().material(Material.MINECART)
							.displayName(Component.text("Leave Vehicle", NamedTextColor.WHITE))
							.click(event -> player.leaveVehicle()),
					() -> !player.isInsideVehicle());
			addButton(0, 10,
					Button.builder().material(Material.LIGHTNING_ROD)
							.displayName(Component.text("Summon Lightning", NamedTextColor.YELLOW))
							.click(event -> player.getWorld().strikeLightning(player.getLocation())),
					() -> null);
			addButton(0, 11,
					Button.builder().material(Material.TNT).displayName(Component.text("Explode", NamedTextColor.RED))
							.click(event -> player.getWorld().createExplosion(player.getLocation(), 4, false, false)),
					() -> null);
			addButton(0, 12,
					Button.builder().material(Material.ARROW)
							.displayName(Component.text("Arrows in Body", NamedTextColor.WHITE))
							.click(event -> player.setArrowsInBody(player.getArrowsInBody() == 0 ? 64 : 0, false)),
					() -> player.getArrowsInBody() > 0);

			addButton(1, 0,
					Button.builder().material(Material.GRASS_BLOCK)
							.displayName(Component.text("Creative", NamedTextColor.GOLD))
							.click(event -> player.setGameMode(GameMode.CREATIVE)),
					() -> player.getGameMode() == GameMode.CREATIVE);
			addButton(1, 1,
					Button.builder().material(Material.IRON_SWORD)
							.displayName(Component.text("Survival", NamedTextColor.GREEN))
							.click(event -> player.setGameMode(GameMode.SURVIVAL)),
					() -> player.getGameMode() == GameMode.SURVIVAL);
			addButton(1, 2,
					Button.builder().material(Material.MAP)
							.displayName(Component.text("Adventure", NamedTextColor.YELLOW))
							.click(event -> player.setGameMode(GameMode.ADVENTURE)),
					() -> player.getGameMode() == GameMode.ADVENTURE);
			addButton(1, 3,
					Button.builder().material(Material.ENDER_EYE)
							.displayName(Component.text("Spectator", NamedTextColor.BLUE))
							.click(event -> player.setGameMode(GameMode.SPECTATOR)),
					() -> player.getGameMode() == GameMode.SPECTATOR);

			addButton(2, 0,
					Button.builder().material(Material.NETHER_STAR)
							.displayName(Component.text("OP", NamedTextColor.GOLD, TextDecoration.BOLD))
							.click(event -> player.setOp(!player.isOp())),
					player::isOp);
			addButton(2, 1,
					Button.builder().material(Material.BARRIER).displayName(Component.text("Ban", NamedTextColor.RED))
							.click(event -> player.ban(null, (Date) null, null, true)),
					player::isBanned);
			addButton(2, 2,
					Button.builder().material(Material.LEATHER_BOOTS)
							.displayName(Component.text("Kick", NamedTextColor.RED)).click(event -> player.kick()),
					() -> !player.isOnline());
			addButton(2, 3, Button.builder().material(Material.NETHERITE_SWORD)
					.displayName(Component.text("Kill", NamedTextColor.RED)).click(event -> player.setHealth(0)),
					player::isDead);
			addButton(2, 4,
					Button.builder().material(Material.CHEST)
							.displayName(Component.text("Clear Inventory", NamedTextColor.RED))
							.click(event -> player.getInventory().clear()),
					() -> player.getInventory().isEmpty());
			addButton(2, 5,
					Button.builder().material(Material.ENDER_CHEST)
							.displayName(Component.text("Clear Ender Chest", NamedTextColor.RED))
							.click(event -> player.getEnderChest().clear()),
					() -> player.getEnderChest().isEmpty());

			addButton(3, 0,
					Button.builder().material(Material.COMMAND_BLOCK)
							.displayName(Component.text("Reset", NamedTextColor.WHITE))
							.lore(Component.text("Sync with server time", NamedTextColor.GRAY))
							.click(event -> player.resetPlayerTime()),
					player::isPlayerTimeRelative);
			addButton(3, 1,
					Button.builder().material(Material.CANDLE).displayName(Component.text("Day", NamedTextColor.YELLOW))
							.click(event -> player.setPlayerTime(1000, false)),
					() -> player.getPlayerTimeOffset() == 1000);
			addButton(3, 2,
					Button.builder().material(Material.YELLOW_CANDLE)
							.displayName(Component.text("Noon", NamedTextColor.GOLD))
							.click(event -> player.setPlayerTime(6000, false)),
					() -> player.getPlayerTimeOffset() == 6000);
			addButton(3, 3,
					Button.builder().material(Material.LIGHT_GRAY_CANDLE)
							.displayName(Component.text("Night", NamedTextColor.GRAY))
							.click(event -> player.setPlayerTime(13000, false)),
					() -> player.getPlayerTimeOffset() == 13000);
			addButton(3, 4,
					Button.builder().material(Material.BLACK_CANDLE)
							.displayName(Component.text("Midnight", NamedTextColor.BLACK))
							.click(event -> player.setPlayerTime(18000, false)),
					() -> player.getPlayerTimeOffset() == 18000);

			addButton(4, 0,
					Button.builder().material(Material.COMMAND_BLOCK)
							.displayName(Component.text("Reset", NamedTextColor.WHITE))
							.lore(Component.text("Sync with server weather", NamedTextColor.GRAY))
							.click(event -> player.resetPlayerWeather()),
					() -> player.getPlayerWeather() == null);
			addButton(4, 1,
					Button.builder().material(Material.SUNFLOWER)
							.displayName(Component.text("Clear", NamedTextColor.YELLOW))
							.click(event -> player.setPlayerWeather(WeatherType.CLEAR)),
					() -> player.getPlayerWeather() == WeatherType.CLEAR);
			addButton(4, 2,
					Button.builder().material(Material.TRIDENT)
							.displayName(Component.text("Downfall", NamedTextColor.AQUA))
							.click(event -> player.setPlayerWeather(WeatherType.DOWNFALL)),
					() -> player.getPlayerWeather() == WeatherType.DOWNFALL);

			addButton(5, 0,
					Button.builder().material(Material.COMMAND_BLOCK)
							.displayName(Component.text("Reset", NamedTextColor.WHITE))
							.click(event -> player.setPose(Pose.STANDING, false)),
					() -> player.getPose() == Pose.STANDING && !player.hasFixedPose());
			addButton(5, 1,
					Button.builder().material(Material.ELYTRA)
							.displayName(Component.text("Fall Flying", NamedTextColor.AQUA))
							.click(event -> player.setPose(Pose.FALL_FLYING, true)),
					() -> player.getPose() == Pose.FALL_FLYING && player.hasFixedPose());
			addButton(5, 2,
					Button.builder().material(Material.WHITE_BED)
							.displayName(Component.text("Sleeping", NamedTextColor.GRAY))
							.click(event -> player.setPose(Pose.SLEEPING, true)),
					() -> player.getPose() == Pose.SLEEPING && player.hasFixedPose());
			addButton(5, 3,
					Button.builder().material(Material.WATER_BUCKET)
							.displayName(Component.text("Swimming", NamedTextColor.BLUE))
							.click(event -> player.setPose(Pose.SWIMMING, true)),
					() -> player.getPose() == Pose.SWIMMING && player.hasFixedPose());
			addButton(5, 4,
					Button.builder().material(Material.TRIDENT)
							.displayName(Component.text("Spin Attack", NamedTextColor.AQUA))
							.click(event -> player.setPose(Pose.SPIN_ATTACK, true)),
					() -> player.getPose() == Pose.SPIN_ATTACK && player.hasFixedPose());
			addButton(5, 5,
					Button.builder().material(Material.SCULK_SENSOR)
							.displayName(Component.text("Sneaking", NamedTextColor.DARK_GRAY))
							.click(event -> player.setPose(Pose.SNEAKING, true)),
					() -> player.getPose() == Pose.SNEAKING && player.hasFixedPose());
			addButton(5, 6,
					Button.builder().material(Material.SLIME_BLOCK)
							.displayName(Component.text("Long Jumping", NamedTextColor.GREEN))
							.click(event -> player.setPose(Pose.LONG_JUMPING, true)),
					() -> player.getPose() == Pose.LONG_JUMPING && player.hasFixedPose());
			addButton(5, 7,
					Button.builder().material(Material.SKELETON_SKULL)
							.displayName(Component.text("Dying", NamedTextColor.RED))
							.click(event -> player.setPose(Pose.DYING, true)),
					() -> player.getPose() == Pose.DYING && player.hasFixedPose());
			addButton(5, 8,
					Button.builder().material(Material.MINECART)
							.displayName(Component.text("Sitting", NamedTextColor.GRAY))
							.click(event -> player.setPose(Pose.SITTING, true)),
					() -> player.getPose() == Pose.SITTING && player.hasFixedPose());
			addButton(5, 9,
					Button.builder().material(Material.BOW).displayName(Component.text("Shooting", NamedTextColor.GRAY))
							.click(event -> player.setPose(Pose.SHOOTING, true)),
					() -> player.getPose() == Pose.SHOOTING && player.hasFixedPose());

			addButton(6, 0, Button.builder().material(Material.GLASS_BOTTLE)
					.displayName(Component.text("Clear Level and Exp", NamedTextColor.GRAY)).click(event -> {
						player.setLevel(0);
						player.setExp(0);
					}), () -> player.getLevel() == 0 && player.getExp() == 0);
			addButton(6, 1,
					Button.builder().material(Material.DRAGON_BREATH)
							.displayName(Component.text("Reduce 10 Levels", NamedTextColor.DARK_RED))
							.click(event -> player.setLevel(player.getLevel() - 10)),
					() -> null);
			addButton(6, 2,
					Button.builder().material(Material.DRAGON_BREATH)
							.displayName(Component.text("Reduce Level", NamedTextColor.RED))
							.click(event -> player.setLevel(player.getLevel() - 1)),
					() -> null);
			addButton(6, 3,
					Button.builder().material(Material.EXPERIENCE_BOTTLE)
							.displayName(Component.text("Increase Level", NamedTextColor.GREEN))
							.click(event -> player.setLevel(player.getLevel() + 1)),
					() -> null);
			addButton(6, 4,
					Button.builder().material(Material.EXPERIENCE_BOTTLE)
							.displayName(Component.text("Increase 10 Levels", NamedTextColor.DARK_GREEN))
							.click(event -> player.setLevel(player.getLevel() + 10)),
					() -> null);
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
	}
}
