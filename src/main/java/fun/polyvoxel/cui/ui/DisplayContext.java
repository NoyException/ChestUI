package fun.polyvoxel.cui.ui;

import fun.polyvoxel.cui.ui.source.DisplaySource;
import fun.polyvoxel.cui.util.context.Context;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class DisplayContext<S> {
	private final Player viewer;
	private final DisplaySource<S> source;
	private final boolean asChild;
	private final Context context;

	public DisplayContext(Player viewer, boolean asChild) {
		this(viewer, asChild, null);
	}

	public DisplayContext(Player viewer, boolean asChild, @Nullable DisplaySource<S> source) {
		this(viewer, asChild, source, Context.background());
	}

	public DisplayContext(Player viewer, boolean asChild, @Nullable DisplaySource<S> source, Context context) {
		this.viewer = viewer;
		this.source = source;
		this.asChild = asChild;
		this.context = context;
	}

	public DisplayContext<S> withContext(Context context) {
		return new DisplayContext<>(viewer, asChild, source, context);
	}

	public Player viewer() {
		return viewer;
	}

	public DisplaySource<S> source() {
		return source;
	}

	public boolean asChild() {
		return asChild;
	}

	public Context context() {
		return context;
	}
}
