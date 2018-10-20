package run.mojo.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** */
@Retention(RetentionPolicy.RUNTIME)
public @interface ImportPackage {

  Class[] messages() default {};

  ImportAction[] actions() default {};
}
