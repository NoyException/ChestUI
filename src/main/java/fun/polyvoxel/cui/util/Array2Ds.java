package fun.polyvoxel.cui.util;

import org.apache.commons.lang3.tuple.Pair;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class Array2Ds {
	public static <T> Pair<Position, Position> trim(T[][] array, Predicate<T> isEmpty) {
		BiPredicate<T[][], Integer> isRowEmpty = (matrix, row) -> {
			for (var line : matrix[row]) {
				if (!isEmpty.test(line)) {
					return false;
				}
			}
			return true;
		};
		BiPredicate<T[][], Integer> isColumnEmpty = (matrix, column) -> {
			for (var line : matrix) {
				if (!isEmpty.test(line[column])) {
					return false;
				}
			}
			return true;
		};
		var rowStart = 0;
		var rowEnd = array.length - 1;
		while (rowStart < array.length && isRowEmpty.test(array, rowStart)) {
			rowStart++;
		}
		if (rowStart >= array.length) {
			return Pair.of(new Position(0, 0), new Position(0, 0));
		}
		while (rowEnd >= 0 && isRowEmpty.test(array, rowEnd)) {
			rowEnd--;
		}
		var columnStart = 0;
		var columnEnd = array[0].length - 1;
		while (columnStart < array[0].length && isColumnEmpty.test(array, columnStart)) {
			columnStart++;
		}
		while (columnEnd >= 0 && isColumnEmpty.test(array, columnEnd)) {
			columnEnd--;
		}
		return Pair.of(new Position(rowStart, columnStart), new Position(rowEnd, columnEnd));
	}
}
