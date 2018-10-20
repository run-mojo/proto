package run.mojo.wire.type;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.TreeMap;

/** */
public class FlatBufferSchema {
  private File outputDir;
  private ReflectionSchema registry;

  private FbFile root;

  private TreeMap<String, FbFile> files = new TreeMap<>();
  private TreeMap<String, FbFile> typeToFile = new TreeMap<>();

  public static FlatBufferSchema create(File outputDir, ReflectionSchema registry) {
    FlatBufferSchema schema = new FlatBufferSchema();
    schema.outputDir = outputDir;
    schema.registry = registry;
    return schema;
  }

  private Path pathOf(String namespace) {
    Path p =
        Paths.get(outputDir.toPath().toString(), namespace.replace(".", File.separator))
            .toAbsolutePath();
    return p;
  }

  public void run() {
    outputDir.mkdirs();

    //        root = new FbFile(outputDir.toPath(), "");

    registry.packages.forEach(
        (n, namespace) -> {
          register(namespace);

          namespace.models.forEach((name, model) -> {});
        });
  }

  private FbFile register(PackageDesc namespace) {
    final FbFile file = new FbFile();
    file.namespaceName = namespace.model.getName();
    file.namespaceNameParts = file.namespaceName.split("[.]");
    file.path = pathOf(file.namespaceName);
    file.simpleName = file.path.getFileName().toString();
    file.fbsPath = Paths.get(file.path.toAbsolutePath().toString(), file.simpleName + ".fbs");

    final TreeMap<String, TypeDesc> nested = new TreeMap<>();

    namespace
        .enums()
        .forEach(
            e -> {
              file.enums.add(FbEnum.create(e));
            });

    namespace
        .objects()
        .forEach(
            object -> {
              //            file.messages.add(FbTable.create(object));

              collectNested(nested, object);
            });

    namespace
        .namespaces()
        .forEach(
            descriptor -> {
              //            final TreeMap<String, Descriptor> nested = new TreeMap<>();
              collectNested(nested, descriptor);
            });

    nested
        .values()
        .forEach(
            descriptor -> {
              if (descriptor instanceof MessageDesc) {
                file.tables.add(FbTable.create((MessageDesc) descriptor));
              } else if (descriptor instanceof EnumDesc) {
                file.enums.add(FbEnum.create((EnumDesc) descriptor));
              }
            });

    return file;
  }

  private void createSub(FbFile parent, EnclosingDesc descriptor) {}

  private void registerModel(FbFile file, TypeDesc descriptor) {}

  private String relativeName(TypeDesc descriptor) {
    final String n = descriptor.getRelativeName();
    return n.replace(".", "_");
  }

  private void collectNested(TreeMap<String, TypeDesc> nested, TypeDesc descriptor) {
    if (descriptor instanceof MessageDesc) {
      nested.put(relativeName(descriptor), descriptor);
    } else if (descriptor instanceof EnumDesc) {
      nested.put(relativeName(descriptor), descriptor);
    }
    descriptor
        .nested
        .values()
        .forEach(
            d -> {
              collectNested(nested, d);
            });
  }

  static class FbFile {
    Path path;
    String namespaceName;
    String[] namespaceNameParts;
    String simpleName;
    FbTable rootType;
    Path fbsPath;

    ArrayList<FbEnum> enums = new ArrayList<>();
    ArrayList<FbTable> tables = new ArrayList<>();
    TreeMap<String, FbFile> includes = new TreeMap<>();

    public String includePath(String[] to) {
      final StringBuilder sb = new StringBuilder();
      for (int i = 0; i < to.length; i++) {
        sb.append("../");
      }
      sb.append(fbsPath.getFileName());
      return sb.toString();
    }
  }

  static class FbEnum {
    EnumDesc descriptor;

    public static FbEnum create(EnumDesc descriptor) {
      final FbEnum e = new FbEnum();
      e.descriptor = descriptor;
      return e;
    }
  }

  static class FbUnion {
    FbTable[] options;
  }

  static class FbStruct {}

  static class FbTable {
    MessageDesc descriptor;
    FbField[] fields;

    public static FbTable create(MessageDesc descriptor) {
      final ArrayList<FbField> fields = new ArrayList<>();
      descriptor
          .props
          .values()
          .forEach(
              prop -> {
                if (prop.jsonIgnore) {
                  return;
                }
                fields.add(FbField.create(prop));
              });

      FbTable table = new FbTable();
      table.descriptor = descriptor;
      table.fields = fields.toArray(new FbField[0]);
      return table;
    }
  }

  static class FbField {
    FieldDesc prop;

    public static FbField create(FieldDesc descriptor) {
      final FbField field = new FbField();
      field.prop = descriptor;
      return field;
    }

    public void writeDeclaration(StringBuilder writer) {}
  }
}
