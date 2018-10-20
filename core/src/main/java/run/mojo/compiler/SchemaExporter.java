package run.mojo.compiler;

import com.google.common.base.Charsets;
import com.squareup.wire.FieldEncoding;
import com.squareup.wire.WireCompiler;
import com.squareup.wire.schema.ProtoType;
import run.mojo.wire.type.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Generates a protocol buffer schema ".proto" from a model schema. */
@SuppressWarnings("all")
public class SchemaExporter {

  private File outputDir;
  private Output output;
  private ReflectionSchema registry;
  private String packagePrefix = "";

  private PbFile root;

  private TreeMap<String, PbFile> files = new TreeMap<>();
  private TreeMap<String, PbFile> typeToFile = new TreeMap<>();
  private TreeMap<String, PbEnum> enumsByJavaName = new TreeMap<>();
  private TreeMap<String, PbMessage> messagesByJavaName = new TreeMap<>();

  public static Output java() {
    return new WireOutput();
  }

  public static Output dart() {
    return new WireOutput();
  }

  public static SchemaExporter create(File outputDir, ReflectionSchema registry, Output output) {
    SchemaExporter schema = new SchemaExporter();
    schema.outputDir = outputDir;
    schema.registry = registry;
    schema.output = output;
    return schema;
  }

  static ProtoType underscoreNamespace(ProtoType type) {
    if (type.isScalar()) {
      return type;
    }
    if (type.isMap()) {
      ProtoType key = underscoreNamespace(type.keyType());
      ProtoType value = underscoreNamespace(type.valueType());
      if (type.keyType() != key || type.valueType() != value) {
        return ProtoType.get("map<" + key.toString() + ", " + value.toString() + ">");
      } else {
        // Scalar map.
        return type;
      }
    }
    // Replace '.' with '_'
    return ProtoType.get(type.toString().replace(".", "_"));
  }

  private boolean isGenerated(String packageName) {
    return (!packageName.startsWith("java")
        && !packageName.startsWith("sun")
        && !packageName.isEmpty());
  }

  private String originalPkgToProtoFile(String pkgName) {
    return pkgName.replace(".", "_") + ".proto";
  }

  private ProtoType of(PbFile file, TypeDesc descriptor) {
    if (descriptor instanceof PrimitiveDesc) {
      switch (descriptor.javaType) {
        case BOOL:
        case BOXED_BOOL:
          return ProtoType.BOOL;

        case BYTE:
        case BOXED_BYTE:
        case SHORT:
        case BOXED_SHORT:
        case CHAR:
        case BOXED_CHAR:
        case INT:
        case BOXED_INT:
          return ProtoType.INT32;

        case LONG:
        case BOXED_LONG:
          return ProtoType.INT64;

        case FLOAT:
        case BOXED_FLOAT:
          return ProtoType.FLOAT;

        case DOUBLE:
        case BOXED_DOUBLE:
          return ProtoType.DOUBLE;

        default:
          return ProtoType.get("UNKNOWN");
      }
    } else if (descriptor instanceof BoxedPrimitiveDesc) {
      switch (descriptor.javaType) {
        case BOOL:
        case BOXED_BOOL:
          return ProtoType.BOOL;

        case BYTE:
        case BOXED_BYTE:
        case SHORT:
        case BOXED_SHORT:
        case CHAR:
        case BOXED_CHAR:
        case INT:
        case BOXED_INT:
          return ProtoType.INT32;

        case LONG:
        case BOXED_LONG:
          return ProtoType.INT64;

        case FLOAT:
        case BOXED_FLOAT:
          return ProtoType.FLOAT;

        case DOUBLE:
        case BOXED_DOUBLE:
          return ProtoType.DOUBLE;

        default:
          return ProtoType.get("UNKNOWN");
      }
    } else if (descriptor instanceof BigDecimalDesc) {
      BigDecimalDesc d = (BigDecimalDesc) descriptor;
      return ProtoType.STRING;
    } else if (descriptor instanceof ListDesc) {
      ListDesc d = (ListDesc) descriptor;
      return of(file, d.component);
    } else if (descriptor instanceof MapDesc) {
      MapDesc d = (MapDesc) descriptor;
      return ProtoType.get("map<" + of(file, d.key) + ", " + of(file, d.value) + ">");
    } else if (descriptor instanceof StringDesc) {
      StringDesc d = (StringDesc) descriptor;
      return ProtoType.STRING;
    } else if (descriptor instanceof EnumDesc) {
      EnumDesc d = (EnumDesc) descriptor;
      return ProtoType.get(protoNameOf(d.getName()));
    } else if (descriptor instanceof MessageDesc) {
      MessageDesc d = (MessageDesc) descriptor;
      //            if (descriptor instanceof ObjectDescriptor.Impl) {
      //                return ProtoType.get(protoNameOf(d.name()));
      //            }
      return ProtoType.get(protoNameOf(d.getName()));
    } else if (descriptor instanceof BytesDesc) {
      BytesDesc d = (BytesDesc) descriptor;
      return ProtoType.BYTES;
    } else if (descriptor instanceof DurationDesc) {
      DurationDesc d = (DurationDesc) descriptor;
      return ProtoType.STRING;
    } else if (descriptor instanceof DateDesc) {
      DateDesc d = (DateDesc) descriptor;
      return ProtoType.STRING;
    } else {
      return ProtoType.get("UNKNOWN");
    }
  }

  /**
   * @param javaName
   * @return
   */
  private String protoNameOf(String javaName) {
    if (javaName == null || javaName.isEmpty() || javaName.equals(".")) {
      return packagePrefix;
    }
    if (packagePrefix.isEmpty()) {
      return javaName;
    }
    return packagePrefix + "." + javaName;
  }

  public void run() {
    outputDir.mkdirs();

    registry.packages.forEach(
        (n, namespace) -> {
          register(namespace);
        });

    files.values().forEach(PbFile::resolve);

    output.includeSelfImport = true;
    final Writer writer = new Writer(output, this);
    writer.run();

    final List<String> fileNames =
        files
            .values()
            .stream()
            .map(f -> f.protoPath.getFileName().toString())
            .collect(Collectors.toList());

    //        {
    //            ArrayList<String> typeArgs = new ArrayList<>();
    //            typeArgs.add("protoc");
    ////            typeArgs.add("--proto_path=.proto/java");
    //            typeArgs.add("--dart_out=../dart");
    //            typeArgs.add("-I.");
    //
    //            files.values().forEach(f -> typeArgs.add(f.protoPath.getFileName().toString()));
    //
    //            System.out.println(Joiner.on(" ").join(typeArgs));
    //        }

    {
      output.includeSelfImport = true;
      writer.run();

      ArrayList<String> args = new ArrayList<>();
      args.add("--proto_path=.proto/java");
      args.add("--java_out=.proto/java");
      //            typeArgs.add("--compact");

      files.values().forEach(f -> args.add(f.protoPath.toAbsolutePath().toString()));

      try {
        WireCompiler.main(args.toArray(new String[0]));
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    System.out.println("done");
  }

  /**
   * @param pkgDesc
   * @return
   */
  private PbFile register(PackageDesc pkgDesc) {
    if (pkgDesc.name().startsWith("java.")) {
      return null;
    }

    final PbFile file = new PbFile();
    file.schema = this;
    file.pkg = pkgDesc;
    file.originalPackage = pkgDesc.name();
    file.protoPackage = protoNameOf(pkgDesc.name());
    file.protoPath =
        Paths.get(outputDir.getAbsolutePath(), pkgDesc.name().replace(".", "_") + ".proto");

    files.put(file.originalPackage, file);

    pkgDesc
        .models
        .values()
        .forEach(
            descriptor -> {
              if (descriptor instanceof MessageDesc) {
                // Ignore templates "superclass".
                if (descriptor instanceof MessageDesc.Template) {
                  return;
                }
                PbMessage message = PbMessage.create(file, null, descriptor);
                file.messages.add(message);
                messagesByJavaName.put(message.protoName, message);
              } else if (descriptor instanceof EnumDesc) {
                PbEnum e = PbEnum.create(file, null, (EnumDesc) descriptor);
                file.enums.add(e);
                enumsByJavaName.put(e.protoName, e);
              } else if (descriptor instanceof EnclosingDesc) {
                PbMessage message = PbMessage.create(file, null, descriptor);
                file.messages.add(message);
                messagesByJavaName.put(message.protoName, message);
              }
            });

    return file;
  }

  /**
   * @param descriptor
   * @return
   */
  private String relativeName(TypeDesc descriptor) {
    final String n = descriptor.getRelativeName();
    return n.replace(".", "_");
  }

  /**
   * @param nested
   * @param descriptor
   */
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

  enum ProtoSyntax {
    PROTO2,
    PROTO3,
    ;
  }

  static class PbFile {

    SchemaExporter schema;
    String originalPackage;
    String protoPackage;
    PbMessage rootType;
    Path protoPath;
    PackageDesc pkg;

    ArrayList<PbEnum> enums = new ArrayList<>();
    ArrayList<PbMessage> messages = new ArrayList<>();

    public String includePath(String[] to) {
      final StringBuilder sb = new StringBuilder();
      for (int i = 0; i < to.length; i++) {
        sb.append("../");
      }
      sb.append(protoPath.getFileName());
      return sb.toString();
    }

    private void resolve() {
      messages.forEach(m -> m.resolve(schema, m));
    }
  }

  static class PbEnum {

    PbFile file;
    PbMessage parent;
    String originalName;
    String simpleName;
    String protoName;
    EnumDesc descriptor;
    PbEnumConstant[] constants;

    public static PbEnum create(PbFile file, PbMessage parent, EnumDesc descriptor) {
      final PbEnum e = new PbEnum();
      e.file = file;
      e.parent = parent;
      e.descriptor = descriptor;
      e.originalName = descriptor.getName();
      e.simpleName = descriptor.getSimpleName();
      e.protoName = file.schema.protoNameOf(e.originalName);
      e.constants = new PbEnumConstant[descriptor.values.length];
      final String prefix = descriptor.getRelativeName().replace(".", "_") + "_";
      for (int i = 0; i < descriptor.values.length; i++) {
        final PbEnumConstant constant = new PbEnumConstant();
        constant.parent = e;
        constant.descriptor = descriptor.values[i];
        constant.name = prefix + constant.descriptor.name;
        e.constants[i] = constant;
      }
      return e;
    }
  }

  static class PbEnumConstant {

    public PbEnum parent;
    public EnumConstant descriptor;
    public String name;
  }

  static class PbOneOf {

    PbMessage[] options;
  }

  static class PbMessage {

    PbFile file;
    PbMessage parent;
    String originalName;
    String protoName;
    String simpleName;
    TypeDesc descriptor;
    ProtoType protoType;
    PbField[] fields;
    ArrayList<PbEnum> enums = new ArrayList<>();
    ArrayList<PbMessage> messages = new ArrayList<>();

    @SuppressWarnings("all")
    public static PbMessage create(PbFile file, PbMessage parent, TypeDesc descriptor) {
      final PbMessage message = new PbMessage();
      message.file = file;
      message.descriptor = descriptor;
      message.originalName = descriptor.getName();
      message.simpleName = descriptor.getSimpleName();
      message.parent = parent;
      message.protoName = file.schema.protoNameOf(message.originalName);
      message.protoType = ProtoType.get(message.protoName);

      final ArrayList<PbField> fields = new ArrayList<>();

      if (descriptor instanceof MessageDesc) {
        ((MessageDesc) descriptor)
            .props
            .values()
            .forEach(
                prop -> {
                  //                    if (descriptor.jsonIgnore != null) {
                  //                        return;
                  //                    }
                  fields.add(PbField.create(file, message, prop));
                });
      }

      message.fields = fields.toArray(new PbField[0]);

      if (descriptor.nested != null) {
        descriptor
            .nested
            .values()
            .forEach(
                nested -> {
                  if (nested instanceof MessageDesc) {
                    // Ignore templates "superclass".
                    if (descriptor instanceof MessageDesc.Template) {
                      return;
                    }
                    PbMessage m = PbMessage.create(file, message, (MessageDesc) nested);
                    message.messages.add(m);
                    file.schema.messagesByJavaName.put(m.protoName, m);
                  } else if (nested instanceof EnumDesc) {
                    PbEnum e = PbEnum.create(file, message, (EnumDesc) nested);
                    message.enums.add(e);
                    file.schema.enumsByJavaName.put(e.protoName, e);
                  } else if (nested instanceof EnclosingDesc) {
                    PbMessage m = PbMessage.create(file, message, (EnclosingDesc) nested);
                    message.messages.add(m);
                    file.schema.messagesByJavaName.put(m.protoName, m);
                  }
                });
      }

      return message;
    }

    void resolve(SchemaExporter schema, PbMessage message) {
      if (message.fields != null) {
        for (PbField field : message.fields) {
          field.protoType = schema.of(field.file, field.descriptor.typeDescriptor);

          switch (field.descriptor.javaKind) {
            case BOOL:
            case BOXED_BOOL:
              break;

            case BYTE:
            case BOXED_BYTE:
            case SHORT:
            case BOXED_SHORT:
            case CHAR:
            case BOXED_CHAR:
            case INT:
            case BOXED_INT:
              break;

            case LONG:
            case BOXED_LONG:
              break;

            case FLOAT:
            case BOXED_FLOAT:
              break;

            case DOUBLE:
            case BOXED_DOUBLE:
              break;

            case BIG_DECIMAL:
              break;

            case DURATION:
              {
                //                            field.protoType = ProtoType.STRING;
                //                            field.protoType =
                // ProtoType.get(field.descriptor.dataClass.getCanonicalName());
              }
              break;
            case BYTES:
              field.protoType = ProtoType.BYTES;
              break;
            case LIST:
              {
                ListDesc m = (ListDesc) field.descriptor.typeDescriptor;
                field.keyOrElement =
                    schema.messagesByJavaName.get(schema.protoNameOf(m.component.getName()));
              }
              break;
            case SET:
              {
                ListDesc m = (ListDesc) field.descriptor.typeDescriptor;
                field.keyOrElement =
                    schema.messagesByJavaName.get(schema.protoNameOf(m.component.getName()));
              }
              break;
            case MAP:
              {
                MapDesc m = (MapDesc) field.descriptor.typeDescriptor;
                field.keyOrElement =
                    schema.messagesByJavaName.get(schema.protoNameOf(m.key.getName()));
                field.valueMessage = schema.messagesByJavaName.get(m.value.getName());
              }

              break;
            case ENUM:
              {
                EnumDesc e = (EnumDesc) field.descriptor.typeDescriptor;
                field.pbEnum = schema.enumsByJavaName.get(schema.protoNameOf(e.getName()));
              }
              break;
            case STRING:
              break;
            case OBJECT:
              //                            field.protoType =
              // ProtoType.get(schema.protoNameOf(field.descriptor.typeDescriptor.name()));
              field.type =
                  schema.messagesByJavaName.get(
                      schema.protoNameOf(field.descriptor.typeDescriptor.getName()));
              break;
            case ENCLOSING:
              break;
          }
        }
      }
      messages.forEach(m -> m.resolve(schema, m));
    }
  }

  /** */
  static class PbField {

    PbFile file;
    FieldDesc descriptor;
    PbMessage owner;
    // Type if not scalar
    PbMessage type;
    ProtoType protoType;
    PbEnum pbEnum;
    // Map or List
    PbMessage keyOrElement;
    PbMessage valueMessage;

    boolean repeated;
    FieldEncoding encoding;

    public static PbField create(PbFile file, PbMessage owner, FieldDesc descriptor) {
      com.squareup.wire.schema.EnumType enumType;
      final PbField field = new PbField();
      field.file = file;
      field.descriptor = descriptor;
      field.owner = owner;
      if (descriptor.typeDescriptor instanceof ListDesc) {
        field.repeated = true;
      }

      // Do we need to generate a javaKind?
      if (descriptor.typeDescriptor instanceof MessageDesc.Impl) {
        // Create javaKind.
        MessageDesc.Impl impl = (MessageDesc.Impl) descriptor.typeDescriptor;
        field.protoType = ProtoType.get(impl.getName());
      } else {
        field.protoType = file.schema.of(file, descriptor.typeDescriptor);
      }
      return field;
    }

    public String name() {
      return descriptor.name;
    }

    public int tag() {
      return descriptor.tag;
    }

    public boolean isMap() {
      return descriptor.typeDescriptor instanceof MapDesc;
    }
  }

  public static class Writer {

    private final StringBuilder sink = new StringBuilder();
    private final Output output;
    private final SchemaExporter schema;

    private int indents;
    private String indentValue = "    ";

    public Writer(Output output, SchemaExporter schema) {
      this.output = output;
      this.schema = schema;
    }

    public void run() {
      schema
          .files
          .values()
          .forEach(
              f -> {
                sink.setLength(0);

                try {
                  output.writeFile(f, this);
                  Files.write(
                      f.protoPath,
                      sink.toString().getBytes(Charsets.UTF_8),
                      StandardOpenOption.CREATE,
                      StandardOpenOption.TRUNCATE_EXISTING);
                } catch (Throwable e) {
                  e.printStackTrace();
                }
              });
    }

    public Writer indent(Runnable r) {
      indents++;
      r.run();
      indents--;
      return this;
    }

    public Writer line() {
      sink.append("\n");
      return this;
    }

    public Writer start(String value) {
      return writeIndent().write(value);
    }

    public Writer write(String value) {
      sink.append(value);
      return this;
    }

    public Writer writeEndLine(String value) {
      sink.append(value).append("\n");
      return this;
    }

    public Writer writeIndent() {
      for (int i = 0; i < indents; i++) {
        sink.append(indentValue);
      }
      return this;
    }

    public Writer line(String line) {
      writeIndent();
      sink.append(line).append("\n");
      return this;
    }
  }

  public static class Output {

    ProtoSyntax syntax = ProtoSyntax.PROTO2;
    boolean includeSelfImport = false;

    String deriveName(String fqn) {
      return fqn;
    }

    ProtoType deriveType(PbField field) {
      return field.protoType;
    }

    protected String javaNamespace(PbFile file) {
      return "proto." + file.protoPackage;
    }

    protected String csharpNamespace(PbFile file) {
      String name = file.protoPackage;
      StringBuilder sb = new StringBuilder();
      String[] parts = name.split("[.]");
      if (parts == null || parts.length == 0) {
        return null;
      }
      String part = parts[0];
      sb.append(part.substring(0, 1).toUpperCase()).append(part.substring(1));
      for (int i = 1; i < parts.length; i++) {
        part = parts[i];
        sb.append(".").append(part.substring(0, 1).toUpperCase()).append(part.substring(1));
      }
      return "WireMessage." + sb.toString();
    }

    void writeFile(PbFile file, Writer writer) {
      writePackage(file, writer);
      writeOptions(file, writer);
      writeImports(file, writer);

      file.enums.forEach(
          e -> {
            writeEnum(e, writer);
            writer.line();
          });
      file.messages.forEach(
          m -> {
            writeMessage(m, writer);
            writer.line();
          });
    }

    void writePackage(PbFile file, Writer writer) {
      switch (syntax) {
        case PROTO2:
          writer.line("syntax = \"proto2\";");
          break;
        case PROTO3:
          writer.line("syntax = \"proto3\";");
          break;
      }

      writer.line();
      writer.start("package ").write(deriveName(file.protoPackage)).writeEndLine(";");
      writer.line();

      // Create options.
      final String javaNamespace = javaNamespace(file);
      if (javaNamespace != null) {
        writer.start("option java_package = \"").write(javaNamespace).writeEndLine("\";");
        writer.line();
      }
      final String csharpNamespace = csharpNamespace(file);
      if (csharpNamespace != null) {
        writer.start("option csharp_namespace = \"").write(csharpNamespace).writeEndLine("\";");
        writer.line();
      }
    }

    void writeOptions(PbFile file, Writer writer) {}

    void writeImports(PbFile file, Writer writer) {
      // Write imports.
      file.pkg
          .dependsOn(includeSelfImport)
          .filter(s -> file.schema.isGenerated(s))
          .map(
              s ->
                  writer
                      .write("import \"")
                      .write(file.schema.originalPkgToProtoFile(s))
                      .writeEndLine("\";"))
          .collect(Collectors.toList())
          .stream()
          .findAny()
          .ifPresent(s -> writer.line());
    }

    void writeEnum(PbEnum e, Writer writer) {
      writer.start("enum ").write(e.simpleName).writeEndLine(" {");
      writer.indent(
          () -> {
            Arrays.stream(e.constants)
                .forEach(
                    constant ->
                        writer
                            .start(constant.name)
                            .write(" = ")
                            .write(Integer.toString(constant.descriptor.tag))
                            .writeEndLine(";"));
          });
      writer.line("}");
    }

    void writeMessage(PbMessage message, Writer writer) {
      // Write message declaration
      if (message.descriptor.rpc != null) {
        for (String path : message.descriptor.rpc.paths()) {
          writer.start("// REST: ").writeEndLine(path);
        }
      }
      writer.start("message ").write(message.simpleName).writeEndLine(" {");

      writer.indent(
          () -> {
            // Write fields.
            Arrays.stream(message.fields).forEach(field -> writeField(field, writer));

            // Write enums.
            message.enums.forEach(
                e -> {
                  writer.line();
                  writeEnum(e, writer);
                });
            // Write messages.
            message.messages.forEach(
                m -> {
                  writer.line();
                  writeMessage(m, writer);
                });
          });

      writer.line("}");
    }

    void writeField(PbField field, Writer writer) {
      switch (syntax) {
        case PROTO2:
          writeFieldProto2(field, writer);
          break;
        case PROTO3:
          writeFieldProto3(field, writer);
          break;
      }
    }

    void writeFieldProto2(PbField field, Writer writer) {
      if (field.repeated) {
        writer.start("repeated ");
      } else if (field.isMap()) {
        writer.start("");
      } else {
        // Always optional!
        writer.start("optional ");
      }

      // Write javaKind.
      writer.write(deriveType(field).toString()).write(" ");

      // Write name and getTag.
      writer
          .write(field.descriptor.name)
          .write(" = ")
          .write(Integer.toString(field.tag()))
          .writeEndLine(";");
    }

    void writeFieldProto3(PbField field, Writer writer) {
      if (field.repeated) {
        writer.start("repeated ").write(deriveType(field).toString()).write(" ");
      } else {
        // Write javaKind.
        writer.start(deriveType(field).toString()).write(" ");
      }

      // Write name and getTag.
      writer
          .write(field.descriptor.name)
          .write(" = ")
          .write(Integer.toString(field.tag()))
          .writeEndLine(";");
    }
  }

  public static class DartOutput extends Output {

    String deriveName(String fqn) {
      return fqn.replace(".", "_");
    }

    ProtoType deriveType(PbField field) {
      return underscoreNamespace(field.protoType);
    }
  }

  public static class SwiftOutput extends Output {}

  public static class WireOutput extends Output {}
}
