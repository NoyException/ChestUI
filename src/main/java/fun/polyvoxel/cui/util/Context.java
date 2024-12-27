package fun.polyvoxel.cui.util;

import java.util.HashMap;

public class Context {
	private HashMap<String, Object> data;

	public Context() {
	}

	public <T> void set(String key, T value) {
		if (data == null) {
			data = new HashMap<>();
		}
		data.put(key, value);
	}

	@SuppressWarnings("unchecked")
	public <T> T get(String key) {
		if (data == null) {
			return null;
		}
		return (T) data.get(key);
	}
}
