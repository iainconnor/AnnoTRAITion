# AnnoTRAITions

AnnoTRAITions brings the flexibility of [PHP 5.4's Traits](http://php.net/traits) to Java by way of an annotation and an associated processor.

## Introduction

Lovingly borrowed from the PHP documentation on the subject, "Traits are a mechanism for code reuse in single inheritance".

In practice, I've found Traits most usefull as a way to add functionality to an existing Class, without interrupting the logical flow of a hierarchy.

By way of an example, say you have a few Classes;

```Java
public class Apple {
	...
}
	
public class Orange {
	...
}
```

And some snippets of code for shaing a representation of an Object to Facebook or Twitter;

```Java
public void postToTwitter(Object toPost) {
	String postMessage = "Hey, Twitter, check out, '" + toPost.toString() + "'";
	...
}

public void postToFacebook(Object toPost) {
	String postMessage = "Hey, Facebook, check out, '" + toPost.toString() + "'";
	...
}
```
	
Now, if you wanted `Apple` and `Orange` to both benifit from all the hard work you put in to write the `postToTwitter` and `postToFacebook` methods, you'd have a few options;

1. Change the methods to be static and wrap them in a class. Then call something like;
	
	```Java
	Apple apple = new Apple();
	Twitter.postToTwitter(apple);
	```
2. Have `Apple` and `Orange` at some point extend a common Class that has both methods;
	
	```Java
	Apple apple = new Apple();
	apple.postToTwitter(apple);
	```
3. Traits add an additional option. You can say that all `Apple`s use a Trait called `Tweetable`. They allow you to inject the `postToTwitter` mechanism into your Class without altering your hierarchy, but while still maintaining a relationship between `Apple` and `Tweetable`.

## Installing in Gradle

1. Add the repository to your `build.gradle` file;

	```Groovy
	repositories {
		mavenCentral()
    	maven {
        	url 'https://raw.github.com/iainconnor/annoTRAITion/master/maven/'
    	}
	}
	```
2. And add the dependency;
	
	```Groovy
	dependencies {
		compile 'com.iainconnor:annotraition:1.0.0'
	}
	```
	
3. Add the annotation processor.

	If you're writing an Android appplication, I'd recommend using the [android-apt](https://bitbucket.org/hvisser/android-apt/overview) plugin. Otherwise, the [gradle-apt](https://github.com/Jimdo/gradle-apt-plugin) plugin should work. Either way, you'll need to add the processor as an additional dependency;
	
	```Groovy
	dependencies {
		apt 'com.iainconnor.annotraition:processor:1.0.1'
		compile 'com.iainconnor:annotraition:1.0.0'
	}
	```
	
## Installing in other build tools

1. Download the `.jar` for the latest version [from this repository](https://github.com/iainconnor/AnnoTRAITion/tree/master/maven/com/iainconnor/annotraition).
2. And the `.jar` for the latest processor [from this repository](https://github.com/iainconnor/AnnoTRAITion/tree/master/maven/com/iainconnor/annotraition/processor).
3. Add both to your porject and find out how to add these flags to the javac compilation;
		
	```
	-proc:only -processor com.iainconnor.annotraition.processor.Processor
	```

## Usage

1. Create your Trait. Returning to the example from above, you'll need to add the `@Trait` annotation, have it extend `com.iainconnor.annotraition.MasterTrait`, and implement the Constructor from that Class;

	```Java
	@Trait
	public class Tweetable extends com.iainconnor.annotraition.MasterTrait {
		public Tweetable(Object traitedObject) {
			super(traitedObject);
		}
		
		public void postToTwitter() {
			String postMessage = "Hey, Twitter, check out, '" + traitedObject.toString() + "'";
			...
		}
	}
	```
	
	Note that `MasterTrait` adds an `Object traitedObject`. You can use this Object to get the instance that has the Trait applied to it.
	
2. Have your Classes that want to use a Trait do so through the `@Use` annotation;

	```Java
	@Use (Tweetable.class)
	public class Apple {
		...
	}
	```

3. Or your Classes that want to use more than one Trait do so through the `@Uses` annotation;

	```Java
	@Uses ({@Use (Tweetable.class), @Use (Facebookable.class)})
	public class Orange {
		...
	}
	```

4. In the rest of your code, use the traited versions of your Classes. After the annotation processor has run, two new classes will be built with the names `_AppleTraited` and `_OrangeTraited`;

	```Java
	_AppleTraited apple = new _AppleTraited();
	apple.postToTwitter();
	```

### FAQs

1. Whats really going on here?

	Good question. I find the easiest way to understand what's at work is to look at the output of our example above. This is the contents of `_OrangeTraited.java` after being annotated;
	
	```Java
	// Generated by AnnoTRAITion.
	// Do not edit, your changes will be overridden.
	public class _OrangeTraited extends Orange {
		protected Facebookable facebookable;
		protected Tweetable tweetable;
		
		public _OrangeTraited() {
			super();
			this.facebookable = new Facebookable(this);
			this.tweetable = new Tweetable(this);
		}

		/**
		 * Passes through to `Facebookable.postToFacebook`.
		 */
		public void postToFacebook() {
			this.facebookable.postToFacebook();
		}

		/**
		 * Passes through to `Tweetable.postToTwitter`.
		 */
		public void postToTwitter() {
			tweetable.postToTwitter();
		}
	}
	```
  	  	
	As you can see, local instances of the Traits have been added to this subclass, and any methods in those Traits are wrapped by proxy methods.

2. I heard that annotations slow down your code execution!

	Many annotations use run-time reflection, which can slow down your application, which is why AnnoTRAITions runs exclusively at compile time. No additional code except the example you see above is added to your run-time processing.

## Contact

Love it? Hate it? Want to make changes to it? Contact me at [@iainconnor](http://www.twitter.com/iainconnor) or [iainconnor@gmail.com](mailto:iainconnor@gmail.com).