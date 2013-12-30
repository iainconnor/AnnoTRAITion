package com.iainconnor.annotraition;

import java.lang.annotation.*;

@Documented
@Target (ElementType.TYPE)
@Retention (RetentionPolicy.SOURCE)
public @interface Use {
	Class value ();

	String localVariable () default "";
}
