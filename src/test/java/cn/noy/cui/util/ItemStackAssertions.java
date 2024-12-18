package cn.noy.cui.util;

import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AssertionFailureBuilder;
import org.opentest4j.AssertionFailedError;

public class ItemStackAssertions {
	public static void assertEmpty(ItemStack itemStack) {
		assertEmpty(itemStack, "ItemStack is not empty");
	}

	public static void assertEmpty(ItemStack itemStack, String message) {
		var result = ItemStacks.isEmpty(itemStack);
		if (!result) {
			AssertionFailureBuilder.assertionFailure().message(message).expected("[NOT EMPTY]").actual(itemStack)
					.buildAndThrow();
		}
	}

	public static void assertFull(ItemStack itemStack) {
		assertFull(itemStack, "ItemStack is not full");
	}

	public static void assertFull(ItemStack itemStack, String message) {
		var result = ItemStacks.isFull(itemStack);
		if (!result) {
			if (ItemStacks.isEmpty(itemStack)) {
				AssertionFailureBuilder.assertionFailure().message(message).expected("[NOT EMPTY]").actual(itemStack)
						.buildAndThrow();
			}
			AssertionFailureBuilder.assertionFailure().message(message).expected(itemStack.getMaxStackSize())
					.actual(itemStack.getAmount()).buildAndThrow();
		}
	}

	public static void assertSimilar(ItemStack expected, ItemStack actual) {
		assertSimilar(expected, actual, "ItemStacks are not similar");
	}

	public static void assertSimilar(ItemStack expected, ItemStack actual, String message) {
		var result = ItemStacks.isSimilar(expected, actual);
		if (!result) {
			AssertionFailureBuilder.assertionFailure().message(message).expected(expected).actual(actual)
					.buildAndThrow();
		}
	}

	public static void assertSame(ItemStack expected, ItemStack actual) {
		assertSame(expected, actual, "ItemStacks are not the same");
	}

	public static void assertSame(ItemStack expected, ItemStack actual, String message) {
		var result = ItemStacks.isSame(expected, actual);
		if (!result) {
			AssertionFailureBuilder.assertionFailure().message(message).expected(expected).actual(actual)
					.buildAndThrow();
		}
	}

}
