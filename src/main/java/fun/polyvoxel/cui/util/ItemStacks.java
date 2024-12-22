package fun.polyvoxel.cui.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ItemStacks {
	public static boolean isEmpty(ItemStack itemStack) {
		return itemStack == null || itemStack.isEmpty();
	}

	public static boolean isFull(ItemStack itemStack) {
		if (isEmpty(itemStack)) {
			return false;
		}
		return itemStack.getAmount() >= itemStack.getMaxStackSize();
	}

	public static int getAmount(ItemStack itemStack) {
		return isEmpty(itemStack) ? 0 : itemStack.getAmount();
	}

	public static boolean isSimilar(ItemStack a, ItemStack b) {
		var aEmpty = isEmpty(a);
		var bEmpty = isEmpty(b);
		if (aEmpty && bEmpty) {
			return true;
		}
		if (aEmpty || bEmpty) {
			return false;
		}
		return a.isSimilar(b);
	}

	public static boolean isSame(ItemStack a, ItemStack b) {
		var aEmpty = isEmpty(a);
		var bEmpty = isEmpty(b);
		if (aEmpty && bEmpty) {
			return true;
		}
		if (aEmpty || bEmpty) {
			return false;
		}
		return a.isSimilar(b) && a.getAmount() == b.getAmount();
	}

	public static ItemStack clone(ItemStack itemStack) {
		return itemStack == null ? null : itemStack.clone();
	}

	public record PlaceResult(ItemStack placed, ItemStack remaining, boolean success) {
	}

	/**
	 * 尝试将source放入target中，返回放入后的target和剩余的source。无法放入则返回原target和source。<br>
	 * Try to place source into target, return the placed target and the remaining
	 * source. If it cannot be placed, return the original target and source.
	 * 
	 * @param target
	 *            放入的目标物品<br>
	 *            The target ItemStack to place into
	 * @param source
	 *            放入的源物品<br>
	 *            The source ItemStack to place
	 * @param ignoreAmountLimit
	 *            是否忽略物品堆叠上限<br>
	 *            Whether to ignore the ItemStack limit
	 * @return 放入后的target和剩余的source<br>
	 *         The placed target and the remaining source
	 */
	public static PlaceResult place(ItemStack target, ItemStack source, boolean ignoreAmountLimit) {
		source = isEmpty(source) ? null : source.clone();
		target = isEmpty(target) ? null : target.clone();
		if (source == null) {
			return new PlaceResult(target, source, true);
		}
		if (target == null) {
			return new PlaceResult(source, target, true);
		}
		if (!isSimilar(target, source)) {
			return new PlaceResult(target, source, false);
		}
		var amount = target.getAmount() + source.getAmount();
		if (ignoreAmountLimit) {
			target.setAmount(amount);
			return new PlaceResult(target, null, true);
		} else {
			var max = target.getMaxStackSize();
			if (amount <= max) {
				target.setAmount(amount);
				return new PlaceResult(target, null, true);
			}
			target.setAmount(max);
			source.setAmount(amount - max);
			return new PlaceResult(target, source, true);
		}
	}

	public static @NotNull Component cleanComponent(Component component) {
		if (component.decoration(TextDecoration.ITALIC) == TextDecoration.State.NOT_SET)
			component = component.decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE);
		if (component.color() == null)
			component = component.color(NamedTextColor.WHITE);
		return component;
	}

	public static Pair<Position, Position> trim(ItemStack[][] itemStacks) {
		return Array2Ds.trim(itemStacks, ItemStacks::isEmpty);
	}
}
