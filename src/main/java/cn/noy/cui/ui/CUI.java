package cn.noy.cui.ui;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 在{@link CUIHandler}的实现类上标记，指定该UI的名称和所属插件。如果没有使用该注解标记，则不会被自动加入到CUI类型列表中，无法被外界获取，仅能自己手动创建。<br>
 * Mark on the implementation class of {@link CUIHandler}, specify the name and
 * plugin of this UI. If not marked with this annotation, it will not be
 * automatically added to the CUI type list and cannot be obtained from the
 * outside, only can be created manually.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CUI {
	/**
	 * CUI的名称，用于标识一个CUI的类型。<br>
	 * The name of the CUI, used to identify a type of CUI.
	 */
	String value();
}
