package run.mojo.annotations;

import io.grpc.MethodDescriptor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ImportAction {
  Class request();

  Class response() default void.class;

  /** */
  MethodDescriptor.MethodType type() default MethodDescriptor.MethodType.UNKNOWN;

  String module() default "";

  String name() default "";

  String fullName() default "";
}
