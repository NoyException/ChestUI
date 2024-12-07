package cn.noy.cui.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

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

	public record PlaceResult(ItemStack placed, ItemStack remaining) {
	}

	public static PlaceResult place(ItemStack target, ItemStack source) {
		if (isEmpty(source)) {
			return new PlaceResult(target, null);
		}
		if (isEmpty(target)) {
			return new PlaceResult(source.clone(), null);
		}
		if (isSimilar(target, source)) {
			var max = target.getMaxStackSize();
			var amount = target.getAmount() + source.getAmount();
			if (amount <= max) {
				target.setAmount(amount);
				return new PlaceResult(target, null);
			}
			target.setAmount(max);
			source.setAmount(amount - max);
			return new PlaceResult(target, source);
		}
		return new PlaceResult(target, source);
	}

	public static @NotNull Component cleanComponent(Component component) {
		if (component.decoration(TextDecoration.ITALIC) == TextDecoration.State.NOT_SET)
			component = component.decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE);
		if (component.color() == null)
			component = component.color(NamedTextColor.WHITE);
		return component;
	}
}
