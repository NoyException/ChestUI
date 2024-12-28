package fun.polyvoxel.cui.util.context;

import org.jetbrains.annotations.NotNull;

public record ValueContext(Context parent, String key, Object value) implements Context {
	@SuppressWarnings("unchecked")
	public <T> T get(@NotNull String key) {
		if (key.equals(this.key)) {
			return (T) value;
		}
		return parent.get(key);
	}

	public boolean has(@NotNull String key) {
		return key.equals(this.key) || parent.has(key);
	}
}
