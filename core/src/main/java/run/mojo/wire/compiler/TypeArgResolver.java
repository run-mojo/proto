package run.mojo.wire.compiler;

/** */
import run.mojo.wire.model.*;
import run.mojo.wire.model.TypeVarModel;

import javax.tools.Diagnostic;
import java.util.List;

/**
 * Standardized Type Argument resolver that unifies a single model between reflection and java compiler model.
 */
public class TypeArgResolver {
  public final MessageModel message;
  public final List<TypeVarModel> typeArgs;
  public final ModelBuilder processor;

  public TypeArgResolver(MessageModel message, ModelBuilder processor) {
    this.message = message;
    this.typeArgs = message.getSuperType().getTypeVars();
    this.processor = processor;
  }

  public WireModel resolve(WireModel kindMirror) {
    if (kindMirror instanceof WildcardModel) {

      WildcardModel wildcardModel = (WildcardModel) kindMirror;

      if (wildcardModel.getResolved() != null) {
        processor.printMessage(Diagnostic.Kind.WARNING,
            "RESOLVED WILDCARD W/O TEMPLATE: "
                + wildcardModel.getType()
                + " to "
                + wildcardModel.toString());
        return wildcardModel.getResolved();
      }

      TypeVarModel typeVar =
          wildcardModel.getTypeVars()
              .stream()
              .findFirst()
              .orElse(null);

      if (typeVar == null) {
        processor.printMessage(Diagnostic.Kind.ERROR,"could not resolve wildcard: " + wildcardModel);
        return null;
      }

      int index = typeVar.getIndex();

      if (index < 0 || index >= typeArgs.size()) {
        processor.printMessage(Diagnostic.Kind.ERROR,"could not resolve wildcard: " + wildcardModel);
        return null;
      }

      TypeVarModel capturedVar = typeArgs.get(index);
      final Object capturedArg = capturedVar.getType();

      if (capturedVar.isVariable()) {
        processor.printMessage(Diagnostic.Kind.WARNING,"REMAPPED WILDCARD TYPE VAR: " + wildcardModel + " to " + capturedArg.toString());
        return new TemplateModel(capturedVar);
      } else {
        processor.printMessage(Diagnostic.Kind.WARNING,
                "RESOLVED WILDCARD VAR '"
                        + typeVar.getName()
                        //                      + "' on '"
                        //                      + typeVar.getGenericDeclaration().toString()
                        + "' AS '"
                        + capturedArg.toString()
                        + "'");

        // Matched!
        return processor.register(capturedArg);
      }
    } else if (kindMirror instanceof TemplateModel) {
      TypeVarModel typeVar = ((TemplateModel) kindMirror).getTypeVar();
      TypeVarModel resolved = typeArgs.stream().filter(t -> t.isMatch(typeVar)).findFirst().orElse(null);

      int index = typeVar.getIndex();
      if (index < 0) {
        processor.printMessage(Diagnostic.Kind.WARNING,"could not resolve javaKind var '" + typeVar + "'");
        //              messager.printMessage(
        //                  Diagnostic.Kind.ERROR, "could not resolve javaKind var '" +
        // typeVar + "'");
        return null;
      }

      TypeVarModel capturedVar = typeArgs.get(index);
      final Object capturedArg = capturedVar.getType();

      if (capturedVar.isVariable()) {
        processor.printMessage(Diagnostic.Kind.WARNING,"REMAPPED TYPE VAR: " + typeVar + " to " + capturedArg.toString());
        return new TemplateModel(capturedVar);
      } else {
        processor.printMessage(Diagnostic.Kind.WARNING,
            "RESOLVED TYPE VAR '"
                + typeVar.getName()
                //                      + "' on '"
                //                      + typeVar.getGenericDeclaration().toString()
                + "' AS '"
                + capturedArg.toString()
                + "'");

        // Matched!
        return processor.register(capturedArg);
      }
    } else if (kindMirror instanceof ListModel) {
      ListModel actualKind = (ListModel) kindMirror;
      WireModel resolved = resolve(actualKind.getComponent());
      return new ListModel(kindMirror.getJavaKind(), resolved);
    } else if (kindMirror instanceof MapModel) {
      MapModel actualKind = (MapModel) kindMirror;
      WireModel key = resolve(actualKind.getKey());
      WireModel value = resolve(actualKind.getKey());
      return new MapModel(key, value);
    } else {
      return kindMirror;
    }
  }
}
