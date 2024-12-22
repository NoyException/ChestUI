package fun.polyvoxel.cui.ui;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DefaultCamera {
	int rowSize() default 3;

	int columnSize() default 9;

	int row() default 0;

	int column() default 0;

	Camera.HorizontalAlign horizontalAlign() default Camera.HorizontalAlign.LEFT;

	Camera.VerticalAlign verticalAlign() default Camera.VerticalAlign.TOP;
}
