package fun.polyvoxel.cui.ui;

import fun.polyvoxel.cui.ui.source.DisplaySource;
import fun.polyvoxel.cui.util.context.Context;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class DisplayContext<S> {
	private final Player viewer;
	private final DisplaySource<S> source;
	private final boolean asChild;
	private Context context;

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

	public Player getViewer() {
		return viewer;
	}

	public DisplaySource<S> getSource() {
		return source;
	}

	public boolean isAsChild() {
		return asChild;
	}

	public Context getContext() {
		return context;
	}

	public void setContext(Context context) {
		this.context = context;
	}
}
