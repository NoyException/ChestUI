package cn.noy.cui.util;

import java.util.Objects;

public record Position(int row, int column) {
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
}
