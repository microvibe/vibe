/*
 * Copyright (c) 2016-8-31
 * created by alex
 */

package io.microvibe.booster.commons.validation;

import io.microvibe.booster.commons.validation.validator.MinLengthValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

/**
 * 最小长度注解
 *
 * @author huangyong
 * @since 1.0.0
 */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MinLengthValidator.class)
public @interface MinLength {

	String message() default "min_length";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

	int value();
}
