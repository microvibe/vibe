package io.microvibe.booster.core.api.annotation;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiIgnored {

	@AliasFor("ignore")
	boolean value() default true;

	@AliasFor("value")
	boolean ignore() default true;

}
