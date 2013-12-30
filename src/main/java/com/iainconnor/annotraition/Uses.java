package com.iainconnor.annotraition;

import java.lang.annotation.*;

@Documented
@Target (ElementType.TYPE)
@Retention (RetentionPolicy.SOURCE)
public @interface Uses {
	Use[] value ();
}
