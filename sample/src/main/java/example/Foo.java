package example;

import com.google.auto.value.AutoValue;
import run.mojo.wire.Wire;

/** */
@AutoValue
public abstract class Foo {
  @Wire
  abstract String getName();
}
