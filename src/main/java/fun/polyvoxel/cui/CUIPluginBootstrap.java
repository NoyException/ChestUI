package fun.polyvoxel.cui;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.data.EnchantmentRegistryEntry;
import io.papermc.paper.registry.event.RegistryEvents;
import io.papermc.paper.registry.set.RegistrySet;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings({"UnstableApiUsage", "unused"})
public class CUIPluginBootstrap implements PluginBootstrap {
	public static final TypedKey<Enchantment> GLOW = TypedKey.create(RegistryKey.ENCHANTMENT,
			Key.key("chestui", "glow"));

	@Override
	public void bootstrap(@NotNull BootstrapContext context) {
		context.getLifecycleManager().registerEventHandler(RegistryEvents.ENCHANTMENT.freeze().newHandler(event -> {
			event.registry().register(GLOW,
					b -> b.description(Component.empty()).supportedItems(RegistrySet.keySet(RegistryKey.ITEM))
							.anvilCost(0).maxLevel(1).weight(1).activeSlots(new EquipmentSlotGroup[0])
							.minimumCost(EnchantmentRegistryEntry.EnchantmentCost.of(0, 0))
							.maximumCost(EnchantmentRegistryEntry.EnchantmentCost.of(0, 0)));
		}));
	}
}
