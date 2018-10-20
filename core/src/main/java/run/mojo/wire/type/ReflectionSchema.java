package run.mojo.wire.type;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.TypeToken;
import run.mojo.unsafe.UnsafeHelper;
import run.mojo.wire.JavaKind;

import java.io.IOException;
import java.lang.reflect.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 */
@SuppressWarnings("all")
// @Slf4j
public class ReflectionSchema {

  public final Function<Class, Boolean> whitelist;
  public final Function<Class, Boolean> blacklist;
  public final HashSet<String> prefixWhitelist = new HashSet<>();
  public final TreeMap<String, PackageDesc> packages = new TreeMap<>();
  public final TreeMap<String, TypeDesc> descriptors = new TreeMap<>();
  public final TreeMap<String, RpcDesc> actions = new TreeMap<>();
  // Message templates.
  public final TreeMap<String, MessageDesc.Template> templates = new TreeMap<>();
  public final Function<Class<?>, RpcInfo> rpc;

  private ReflectionSchema(
      Function<Class, Boolean> whitelist,
      Function<Class, Boolean> blacklist,
      Function<Class<?>, RpcInfo> rpc) {
    this.whitelist = whitelist;
    this.blacklist = blacklist;
    this.rpc = rpc;
  }

  public static ClassPath all() {
    try {
      return ClassPath.from(Thread.currentThread().getContextClassLoader());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @param cp
   * @param adapter
   * @param whitelist
   * @param blacklist
   * @param rpc
   * @return
   */
  public static ReflectionSchema create(
      Stream<ClassPath.ClassInfo> cp,
      Function<ClassPath.ClassInfo, Entry> adapter,
      Function<Class, Boolean> whitelist,
      Function<Class, Boolean> blacklist,
      Function<Class<?>, RpcInfo> rpc) {
    if (whitelist == null) {
      whitelist = (cls) -> true;
    }
    if (blacklist == null) {
      blacklist = (cls) -> false;
    }

    final ReflectionSchema builder = new ReflectionSchema(whitelist, blacklist, rpc == null ? (cls) -> null : rpc);

    cp.map(info -> adapter.apply(info))
        .collect(Collectors.toList())
        .stream()
        .map(entry -> builder.register(entry))
        .collect(Collectors.toList());

    return builder;
  }

  private Entry register(Entry entry) {
    if (entry instanceof TypeEntry) {
      registerType((TypeEntry) entry);
    } else if (entry instanceof RpcEntry) {
      registerAction((RpcEntry) entry);
    }
    return entry;
  }

  private void registerType(TypeEntry entry) {
    registerType(entry.cls);
  }

  public boolean isWhitelisted(Class cls) {
    if (whitelist == null) {
      return true;
    }
    return whitelist.apply(cls);
  }

  public boolean isBlacklisted(Class cls) {
    if (blacklist == null) {
      return true;
    }
    return blacklist.apply(cls);
  }

  /**
   * @param entry
   * @return
   */
  public RpcDesc registerAction(RpcEntry entry) {
    if (entry == null || entry.handler == null) {
      return null;
    }
    RpcDesc action = actions.get(entry.handler.getCanonicalName());
    if (action != null) {
      return action;
    }

    action = new RpcDesc();
    action.processorClass = entry.processor;
    action.handlerClass = entry.handler;
    if (entry.request != null) {
      action.request = registerType(entry.request);
    }
    if (entry.response != null) {
      action.response = registerType(entry.response);
    }

    try {
      if (entry.request != null) {
        try {
          action.rpc = rpc.apply(action.request.compiled);
        } catch (Throwable e) {
          // Ignore.
        }

        if (action.rpc == null && action.handlerClass != null) {
          try {
            action.rpc = rpc.apply(action.handlerClass);
          } catch (Throwable e) {
            // Ignore.
          }
        }
      }
      PackageDesc pkg = packages.get(entry.handler.getPackage().getName());
      if (pkg == null) {
        pkg = new PackageDesc();
        pkg.model = entry.handler.getPackage();
        packages.put(entry.handler.getPackage().getName(), pkg);
      }

      pkg.actions.put(action.handlerClass.getSimpleName(), action);
      actions.put(entry.handler.getCanonicalName(), action);
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }

    return action;
  }

  /**
   * @param concreteClass
   * @param typeParameter
   * @return
   */
  private Class resolveTypeParam(Class concreteClass, Type typeParameter) {
    try {
      return Class.forName(typeParameter.getTypeName());
    } catch (ClassNotFoundException e) {
      return TypeToken.of(concreteClass).resolveType(typeParameter).getRawType();
    }
  }

  /**
   * @param cls
   * @return
   */
  private String packageOf(Class cls) {
    try {
      return cls.getPackage().getName();
    } catch (Throwable e) {
      return "";
    }
  }

  /**
   * @param cls
   * @return
   */
  private PackageDesc namespaceOf(Class cls) {
    String name = packageOf(cls);
    PackageDesc namespace = packages.get(name);
    if (name == null && !name.isEmpty()) {
      namespace = new PackageDesc();
      namespace.model = cls.getPackage();
      packages.put(name, namespace);
    }
    return namespace;
  }

  public TypeDesc registerType(Class klass) {
    return registerType(null, null, null, klass);
  }

  /**
   * @param sourceClass
   * @param containingClass
   * @param generic
   * @param cls
   * @return
   */
  public TypeDesc registerType(
      Class sourceClass, Class containingClass, ParameterizedType generic, Class cls) {
    if (cls == null) {
      return null;
    }

    String pkgName = packageOf(cls);
    PackageDesc pkg = packages.get(pkgName);
    if (pkg == null && !pkgName.isEmpty()) {
      pkg = new PackageDesc();
      pkg.model = cls.getPackage();
      packages.put(pkgName, pkg);
    }

    final JavaKind kind = JavaKind.of(cls);

    switch (kind) {
      case BOOL:
      case BYTE:
      case SHORT:
      case CHAR:
      case INT:
      case LONG:
      case FLOAT:
      case DOUBLE:
        {
          TypeDesc model = new PrimitiveDesc();
          descriptors.put(cls.getCanonicalName(), model);
          model.compiled = cls;
          model.javaType = kind;
          return model;
        }

      case BOXED_BOOL:
      case BOXED_BYTE:
      case BOXED_SHORT:
      case BOXED_CHAR:
      case BOXED_INT:
      case BOXED_LONG:
      case BOXED_FLOAT:
      case BOXED_DOUBLE:
        {
          TypeDesc model = new BoxedPrimitiveDesc();
          descriptors.put(cls.getCanonicalName(), model);
          model.compiled = cls;
          model.javaType = kind;
          return model;
        }

      case BIG_DECIMAL:
        {
          TypeDesc model = new BigDecimalDesc();
          descriptors.put(cls.getCanonicalName(), model);
          model.compiled = cls;
          model.javaType = kind;
          return model;
        }

      case DURATION:
        {
          TypeDesc model = new DurationDesc();
          descriptors.put(cls.getCanonicalName(), model);
          model.compiled = cls;
          model.javaType = kind;
          return model;
        }

      case DATE:
        {
          DateDesc model = new DateDesc();
          descriptors.put(cls.getCanonicalName(), model);
          model.compiled = cls;
          model.javaType = kind;
          return model;
        }

      case BYTES:
        {
          BytesDesc model = new BytesDesc();
          model.compiled = cls;
          model.javaType = kind;
          model.buffer = ByteBuffer.class.isAssignableFrom(cls);
          return model;
        }

      case LIST:
        {
          if (cls.isArray()) {
            ListDesc model = new ListDesc(JavaKind.ARRAY);
            model.compiled = cls;
            model.javaType = kind;
            model.pkg = namespaceOf(cls);
            model.componentClass = cls.getComponentType();
            model.component = registerType(model.componentClass);
            return model;
          } else {
            Class elementClass = Object.class;

            if (generic != null && generic.getActualTypeArguments().length > 0) {
              elementClass = resolveTypeParam(sourceClass, generic.getActualTypeArguments()[0]);
            }

            ListDesc model = new ListDesc(JavaKind.LIST);
            model.compiled = cls;
            model.javaType = kind;
            model.pkg = namespaceOf(cls);
            model.componentClass = elementClass;
            model.component = registerType(elementClass);
          }
        }

      case SET:
        {
          Class elementClass = Object.class;

          if (generic != null && generic.getActualTypeArguments().length > 0) {
            elementClass = resolveTypeParam(sourceClass, generic.getActualTypeArguments()[0]);
          }

          ListDesc model = new ListDesc(JavaKind.SET);
          model.compiled = cls;
          model.javaType = kind;
          model.pkg = namespaceOf(cls);
          model.componentClass = elementClass;
          model.component = registerType(elementClass);

          return model;
        }

      case MAP:
        {
          Class keyClass = Object.class;
          Class valueClass = Object.class;

          if (generic != null && generic.getActualTypeArguments().length > 0) {
            keyClass = resolveTypeParam(sourceClass, generic.getActualTypeArguments()[0]);
            valueClass = resolveTypeParam(sourceClass, generic.getActualTypeArguments()[0]);
          }

          MapDesc model = new MapDesc();
          model.compiled = cls;
          model.javaType = kind;
          model.pkg = namespaceOf(cls);
          descriptors.put(cls.getCanonicalName(), model);
          model.keyClass = keyClass;
          model.key = registerType(keyClass);
          model.valueClass = valueClass;
          model.value = registerType(valueClass);

          return model;
        }

      case STRING:
        {
          TypeDesc model = new StringDesc();
          model.compiled = cls;
          model.javaType = kind;
          model.pkg = namespaceOf(cls);
          descriptors.put(cls.getCanonicalName(), model);
          return model;
        }

      case ENUM:
        {
          EnumDesc model = new EnumDesc();
          model.compiled = cls;
          model.javaType = kind;
          model.pkg = namespaceOf(cls);
          descriptors.put(cls.getCanonicalName(), model);
          Object[] constants = cls.getEnumConstants();
          EnumConstant[] values = new EnumConstant[constants.length];
          for (int i = 0; i < values.length; i++) {
            EnumConstant value = new EnumConstant();
            value.name = constants[i].toString();
            value.tag = i;
            values[i] = value;
          }
          model.values = values;

          //                if (pkg != null) {
          //                    pkg.descriptors.put(compiled.getSimpleName(), type);
          //                }

          enclosingWalk(cls, pkg, model);

          return model;
        }

      case OBJECT:
        {
          if (Modifier.isAbstract(cls.getModifiers())) {
            System.out.println(cls.getCanonicalName());
          }
          TypeDesc model = descriptors.get(cls.getCanonicalName());
          if (model != null) {
            // Maybe cached?
            if (!(model instanceof EnclosingDesc)) {
              // A concrete javaKind exists. Let's see if it's a template.
              if (model instanceof MessageDesc.Template) {
                // Generate Impl.
                return generateImpl(pkg, sourceClass, generic, (MessageDesc.Template) model);
              }

              // Return cached.
              return model;
            }

            // Looks like we have an enclosing descriptor that needs an upgrade!
          }

          if (generic != null && generic.getActualTypeArguments().length > 0) {
            model = new MessageDesc.Template();
            model.compiled = cls;
            model.javaType = kind;
            model.pkg = namespaceOf(cls);
            ((MessageDesc.Template) model).generic = generic;
            descriptors.put(cls.getCanonicalName(), model);
            templates.put(cls.getCanonicalName(), (MessageDesc.Template) model);
            enclosingWalk(cls, pkg, model);
            ((MessageDesc.Template) model).props = extractProps(model);

            // Generate the impl.
            model = generateImpl(pkg, sourceClass, generic, (MessageDesc.Template) model);
          } else {
            model = new MessageDesc();
            model.compiled = cls;
            model.javaType = kind;
            model.pkg = namespaceOf(cls);
            descriptors.put(cls.getCanonicalName(), model);
            enclosingWalk(cls, pkg, model);
            ((MessageDesc) model).props = extractProps(model);
          }

          try {
            model.rpc = rpc.apply(cls);
          } catch (Throwable e) {
            // Ignore.
          }

          return model;
        }
      case ENCLOSING:
        return null;
    }

    return null;
  }

  /**
   * @param namespace
   * @param declaringClass
   * @param spec
   * @param template
   * @return
   */
  private MessageDesc.Impl generateImpl(
      PackageDesc namespace,
      Class declaringClass,
      ParameterizedType spec,
      MessageDesc.Template template) {
    final String simpleName = ((Class) spec.getRawType()).getSimpleName() + "Impl";
    final String name = declaringClass.getCanonicalName() + "." + simpleName;

    MessageDesc.Impl impl = template.impl(name, simpleName, declaringClass);

    template
        .props
        .values()
        .forEach(
            p -> {
              Type t = p.field.getGenericType();
              if (t instanceof TypeVariable) {
                TypeVariable typeVar = (TypeVariable) t;
                Class resolved = TypeToken.of(spec).resolveType(typeVar).getRawType();
                FieldDesc implProp = p.impl(resolved);
                implProp.typeDescriptor = registerType(resolved);
                impl.props.put(implProp.name, implProp);
              } else {
                final FieldDesc implProp = p.impl(p.dataClass);
                try {
                  namespace.dependsOn.put(
                      implProp.typeDescriptor.pkg.model.getName(),
                      implProp.typeDescriptor.pkg.model);
                } catch (Throwable e) {
                  // Ignore.
                }
                impl.props.put(implProp.name, implProp);
              }
            });

    return impl;
  }

  /**
   * Walks up the declaring class path until it reaches the package. It registers the enclosing
   * class if no registration is found. That registration may be overriden if there is a hard
   * dependency for the declaring javaKind. Otherwise, it's just an Enclosing javaKind that is used
   * only for organizing the pkg / package.
   */
  private void enclosingWalk(Class klass, PackageDesc pkg, TypeDesc model) {
    Class declaringClass = klass.getDeclaringClass();
    // If compiled is inside another class then it is used as it's "Package".
    if (declaringClass != null) {
      TypeDesc declaringModel = descriptors.get(declaringClass.getCanonicalName());
      if (declaringModel == null) {
        // Create a pkg typeDescriptor.
        declaringModel = new EnclosingDesc();
        declaringModel.compiled = declaringClass;
        declaringModel.javaType = JavaKind.ENCLOSING;
        declaringModel.pkg = pkg;
        // Nest "compiled" typeDescriptor.
        declaringModel.nested.put(klass.getSimpleName(), model);
        // Add typeDescriptor.
        descriptors.put(declaringClass.getCanonicalName(), declaringModel);
        model.enclosing = declaringModel;

        // Walk up.
        TypeDesc nextDeclaringModel = null;
        declaringClass = declaringClass.getDeclaringClass();
        while (declaringClass != null) {
          // Get next typeDescriptor.
          nextDeclaringModel = descriptors.get(declaringClass.getCanonicalName());

          if (nextDeclaringModel != null) {
            nextDeclaringModel.nested.put(declaringModel.compiled.getSimpleName(), declaringModel);
            break;
          }

          nextDeclaringModel = new EnclosingDesc();
          nextDeclaringModel.compiled = declaringClass;
          nextDeclaringModel.javaType = JavaKind.ENCLOSING;
          nextDeclaringModel.pkg = pkg;
          declaringModel.enclosing = nextDeclaringModel;
          nextDeclaringModel.nested.put(declaringModel.compiled.getSimpleName(), model);
          descriptors.put(nextDeclaringModel.compiled.getCanonicalName(), model);

          declaringClass = declaringClass.getDeclaringClass();
          declaringModel = nextDeclaringModel;
        }

        if (pkg != null) {
          pkg.models.put(declaringModel.compiled.getSimpleName(), declaringModel);
        }
      } else {
        model.enclosing = declaringModel;
        declaringModel.nested.put(klass.getSimpleName(), model);
      }
    } else {
      if (pkg != null) {
        if (model != null) {
          model.pkg = pkg;
          pkg.models.put(klass.getSimpleName(), model);
        }
      }
    }

    if (model != null) {
      try {
        model.jsonSerialize = (JsonSerialize) klass.getAnnotation(JsonSerialize.class);
      } catch (Throwable e) {
        // Ignore.
      }
      try {
        model.jsonDeserialize = (JsonDeserialize) klass.getAnnotation(JsonSerialize.class);
      } catch (Throwable e) {
        // Ignore.
      }
    }
  }

  /**
   * @param model
   * @return
   */
  public LinkedHashMap<String, FieldDesc> extractProps(TypeDesc model) {
    if (model.compiled.isEnum()
        || model.compiled.isAnnotation()
        || model.compiled.isInterface()
        || model.compiled.equals(Object.class)) {
      return new LinkedHashMap<>();
    }

    if (Modifier.isAbstract(model.compiled.getModifiers())) {
      return new LinkedHashMap<>();
    }

    if (Enum.class.isAssignableFrom(model.compiled)) {
      return new LinkedHashMap<>();
    }

    if (!isWhitelisted(model.compiled)) {
      return new LinkedHashMap<>();
    }

    Class superclass = model.compiled.getSuperclass();
    final List<Class> inheritance = new ArrayList<>(4);
    while (superclass != null && isWhitelisted(superclass)) {
      inheritance.add(superclass);
      superclass = superclass.getSuperclass();
    }
    inheritance.add(model.compiled);

    int index = 0;
    final LinkedHashMap<String, FieldDesc> props = new LinkedHashMap<>();
    for (Class containingClass : inheritance) {
      // Collect all declared fields.
      final LinkedHashMap<String, Field> fields =
          Arrays.stream(containingClass.getDeclaredFields())
              .filter(f -> !Modifier.isStatic(f.getModifiers()))
              .collect(
                  Collectors.toMap(
                      Field::getName, Function.identity(), (o, o2) -> o2, LinkedHashMap::new));

      // Collect all declared methods.
      final LinkedHashMap<String, Method> methods =
          Arrays.stream(containingClass.getDeclaredMethods())
              .collect(
                  Collectors.toMap(
                      Method::getName, Function.identity(), (o, o2) -> o2, LinkedHashMap::new));

      // Iterate through the fields.
      for (Field field : fields.values()) {
        String name = field.getName();
        // Let's search for a getter.
        String getterName = "get" + name.substring(0, 1).toUpperCase() + name.substring(1);
        Method getter = methods.remove(getterName);
        Method setter = null;
        // Any luck?
        if (getter == null) {
          // Let's try this...
          getterName = "is" + name.substring(0, 1).toUpperCase() + name.substring(1);
          getter = methods.remove(getterName);

          // Any luck?
          if (getter == null) {
            // OK maybe "fluentNaming" style?
            getter = methods.remove(name);

            // Any luck?
            if (getter != null) {
              // Was this really a fluentNaming setter?
              if (getter.getParameterCount() == 1) {
                setter = getter;

                // Let's search for a fluentNaming getter now.
                getter =
                    Arrays.stream(containingClass.getDeclaredMethods())
                        .filter(m -> m.getParameterCount() == 0 && name.equals(m.getName()))
                        .findFirst()
                        .orElse(null);
              }
            }
          }
        }

        if (setter == null) {
          String setterName = "set" + name.substring(0, 1).toUpperCase() + name.substring(1);
          setter = methods.remove(setterName);
        }

        // Create prop descriptor.
        FieldDesc prop = new FieldDesc();
        prop.field = field;
        try {
          // Get field pointer offset.
          prop.offset = UnsafeHelper.objectFieldOffset(field);
        } catch (Throwable e) {
          // Ignore.
          System.err.println("could not get native field offset");
          //          log.warn("could not get native field offset", e);
        }
        prop.containingClass = containingClass;

        // Member name.
        prop.name = name;
        Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType) {
          prop.generic = (ParameterizedType) genericType;
          prop.dataClass = field.getType();
        } else {
          prop.dataClass = field.getType();
        }
        prop.getter = getter;
        prop.setter = setter;
        prop.jsonName = name;
        prop.index = index++;
        prop.tag = index;
        prop.javaKind = JavaKind.of(prop.dataClass);

        // Register it's javaKind.
        prop.typeDescriptor =
            registerType(model.compiled, prop.containingClass, prop.generic, prop.dataClass);

        // Was it an Impl?
        if (prop.typeDescriptor instanceof MessageDesc.Impl) {
          final MessageDesc.Impl impl = (MessageDesc.Impl) prop.typeDescriptor;
          impl.pkg = model.pkg;
          impl.enclosing = model;
          model.nested.put(impl.simpleName, impl);
          // Add dep.
          model.pkg.dependsOn.put(impl.pkg.name(), impl.pkg.model);
        } else if (prop.typeDescriptor instanceof MessageDesc) {
          final MessageDesc d = (MessageDesc) prop.typeDescriptor;
          // Add dep.
          model.pkg.dependsOn.put(d.pkg.name(), d.pkg.model);
        } else if (prop.typeDescriptor instanceof MapDesc) {
          final MapDesc d = (MapDesc) prop.typeDescriptor;
          // Add key dep.
          model.pkg.dependsOn.put(d.key.pkg.name(), d.key.pkg.model);
          // Add value dep.
          model.pkg.dependsOn.put(d.value.pkg.name(), d.value.pkg.model);
        } else if (prop.typeDescriptor instanceof ListDesc) {
          // Add component dep.
          final ListDesc d = (ListDesc) prop.typeDescriptor;
          model.pkg.dependsOn.put(d.component.pkg.name(), d.component.pkg.model);
        } else if (prop.typeDescriptor instanceof EnumDesc) {
          // Add dep.
          final EnumDesc d = (EnumDesc) prop.typeDescriptor;
          model.pkg.dependsOn.put(d.pkg.name(), d.pkg.model);
        }

        JsonProperty jsonProperty = field.getAnnotation(JsonProperty.class);
        if (jsonProperty == null) {
          if (getter != null) {
            jsonProperty = getter.getAnnotation(JsonProperty.class);
            if (setter != null) {
              jsonProperty = setter.getAnnotation(JsonProperty.class);
            }
          } else if (setter != null) {
            jsonProperty = setter.getAnnotation(JsonProperty.class);
          }
        }

        if (jsonProperty != null) {

          if (jsonProperty.index() > 0) {
            prop.tagSource = FieldDesc.TagSource.JSON_PROPERTY;
            prop.tag = jsonProperty.index();
          }
          prop.jsonProperty = jsonProperty;
          if (!jsonProperty.value().isEmpty()) {
            prop.jsonName = jsonProperty.value();
          }
        }

        JsonIgnore jsonIgnore = field.getAnnotation(JsonIgnore.class);
        if (jsonIgnore == null) {
          if (getter != null) {
            jsonIgnore = getter.getAnnotation(JsonIgnore.class);
            if (setter != null) {
              jsonIgnore = setter.getAnnotation(JsonIgnore.class);
            }
          } else if (setter != null) {
            jsonIgnore = setter.getAnnotation(JsonIgnore.class);
          }
        }

        prop.jsonIgnore = jsonIgnore != null && jsonIgnore.value();

        try {
          prop.jsonSerialize = field.getAnnotation(JsonSerialize.class);
        } catch (Throwable e) {
          // Ignore.
        }
        try {
          prop.jsonDeserialize = field.getAnnotation(JsonDeserialize.class);
        } catch (Throwable e) {
          // Ignore.
        }

        if (props.containsKey(prop.name)) {
          System.out.println("duplicate descriptor '" + prop.name + "'");
        }

        props.put(prop.name, prop);
      }
    }
    return props;
  }

  /** */
  public abstract static class Entry {}

  /** */
  public static class TypeEntry extends Entry {

    public Class cls;

    public static TypeEntry create(Class cls) {
      final TypeEntry entry = new TypeEntry();
      entry.cls = cls;
      return entry;
    }
  }

  /** */
  public static class RpcEntry extends Entry {

    public static final RpcEntry NONE = new RpcEntry();

    Class processor;
    Class handler;
    Class request;
    Class response;
    boolean streamRequests;
    boolean streamResponses;

    public static RpcEntry create(
        Class processorClass, Class handlerClass, Class requestClass, Class responseClass) {
      final RpcEntry builder = new RpcEntry();
      builder.processor = processorClass;
      builder.handler = handlerClass;
      builder.request = requestClass;
      builder.response = responseClass;
      return builder;
    }
  }
}
