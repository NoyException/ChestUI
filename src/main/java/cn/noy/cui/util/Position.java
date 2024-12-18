package cn.noy.cui.util;

import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Objects;

public record Position(int row, int column) implements Comparable<Position> {
	public Position add(int row, int column) {
		return new Position(this.row + row, this.column + column);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		Position position = (Position) o;
		return row == position.row && column == position.column;
	}

	@Override
	public int hashCode() {
		return Objects.hash(row, column);
	}

	@Override
	public int compareTo(@NotNull Position position) {
		if (row != position.row)
			return Integer.compare(row, position.row);
		return Integer.compare(column, position.column);
	}
}
