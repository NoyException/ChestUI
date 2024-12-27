package fun.polyvoxel.cui.util;

import org.jetbrains.annotations.NotNull;

public class ValueContext implements Context {
	private final Context parent;
	private final String key;
	private final Object value;

	protected ValueContext(Context parent, String key, Object value) {
		this.parent = parent;
		this.key = key;
		this.value = value;
	}

	public <T> ValueContext withValue(@NotNull String key, T value) {
		return new ValueContext(this, key, value);
	}

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
