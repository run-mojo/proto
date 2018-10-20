package example;

import java.util.List;

/** */
public class MySuper<S, G> extends MySuperSuper<G, Long> {
  public S subscribeTo;
  public List<? extends G> settings;
}
