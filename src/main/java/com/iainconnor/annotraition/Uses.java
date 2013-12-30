package com.iainconnor.annotraition;

import java.lang.annotation.*;

@Documented
@Target (ElementType.TYPE)
@Inherited
@Retention (RetentionPolicy.SOURCE)
public @interface Uses {
	Use[] value ();
}
