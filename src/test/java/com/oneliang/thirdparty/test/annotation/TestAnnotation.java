package com.oneliang.thirdparty.test.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import com.oneliang.util.common.StringUtil;

@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface TestAnnotation {

	public String value() default StringUtil.BLANK;
	public String test();
}
