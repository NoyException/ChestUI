package cn.noy.cui.ui;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CUISize {
    int maxRow() default -1;
    int maxColumn() default -1;
    int maxDepth() default -1;
}
