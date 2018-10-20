package run.mojo.wire.type;

import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** */
public class PackageDesc {

  public Package model;
  public TreeMap<String, TypeDesc> models = new TreeMap<>();
  public TreeMap<String, RpcDesc> actions = new TreeMap<>();
  public TreeMap<String, Package> dependsOn = new TreeMap<>();

  public String name() {
    return model.getName();
  }

  public Stream<String> dependsOn(boolean includeSelf) {
    if (includeSelf) {
      return dependsOn.keySet().stream();
    } else {
      return dependsOn.keySet().stream().filter(s -> !s.equals(name()));
    }
  }

  public List<EnclosingDesc> namespaces() {
    return models
        .values()
        .stream()
        .filter(d -> d instanceof EnclosingDesc)
        .map(d -> (EnclosingDesc) d)
        .collect(Collectors.toList());
  }

  public List<MessageDesc> objects() {
    return models
        .values()
        .stream()
        .filter(d -> d instanceof MessageDesc)
        .map(d -> (MessageDesc) d)
        .collect(Collectors.toList());
  }

  public List<EnumDesc> enums() {
    return models
        .values()
        .stream()
        .filter(d -> d instanceof EnumDesc)
        .map(d -> (EnumDesc) d)
        .collect(Collectors.toList());
  }
}
