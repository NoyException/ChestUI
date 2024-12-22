package fun.polyvoxel.cui.ui;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 在{@link CUIHandler}的实现类上标记，指定该UI的名称和所属插件。你必须在实现类上标记这个注解。<br>
 * Mark on the implementation class of {@link CUIHandler}, specify the name and
 * plugin of this UI. You MUST mark this annotation on the implementation class.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CUI {
	/**
	 * CUI的名称，用于标识一个CUI的类型。<br>
	 * The name of the CUI, used to identify a type of CUI.
	 */
	String name();

	/**
	 * 是否为单例。如果为true，则只会存在一个该类型的CUI实例，并且自动创建。<br>
	 * Whether it is a singleton. If true, there will only be one instance of this,
	 * and it will be created automatically.
	 */
	boolean singleton() default false;
}
