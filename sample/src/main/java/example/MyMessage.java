package example;

import lombok.Data;
import run.mojo.Wire;
import run.mojo.compiler.ModelTransformer;
import run.mojo.compiler.Assembler;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;

/** */
// @Data
@Data
public class MyMessage extends MySuper<String, Long> {
  // public class MyMessage {

  @Wire(tag = 12)
  public String name;

  public List<String> options;
  //    public Records<Integer> results;
  //  public Records<Long> other;

  public static void main(String[] args) {
    //    System.out.println(new Records<>(10, Collections.<Integer>emptyList()).toString());

    //    Arrays.stream(Records.class.getConstructors())
    //        .forEach(
    //            ctor ->
    //                Arrays.stream(ctor.getParameters()).forEach(p -> System.err.println(p)));

    ModelTransformer processor = new ModelTransformer();
    processor.register(com.squareup.wire.schema.internal.parser.MessageElement.class);

    Method[] methods = Foo.class.getDeclaredMethods();
    Constructor[] ctors = Foo.class.getDeclaredConstructors();
    ctors = AutoValue_Foo.class.getDeclaredConstructors();

    ctors = AutoValue_Foo.class.getDeclaredConstructors();
    Parameter p = ctors[0].getParameters()[0];

    ctors = MyMessage.class.getDeclaredConstructors();

    //    compiler.register(MyOtherData.class);
    processor.register(Foo.class);
    processor.register(MyMessage.class);
    processor.register(Api.Request.class);
    processor.register(Api.Response.class);

    Wire_MyMessage.builder().build();

    Assembler schema = Assembler.Companion.create(processor);

    schema.build();

    System.out.println(schema);
    //    WireMessage model = compiler.register(Api.Request.class);

    //    System.out.println(model);

    //    model = compiler.register(Records.class);

    //    System.out.println(model);
    //
    //    model = compiler.register(int[].class);
    //    System.out.println(model);
    //    MyMessage.builder().name("").build();
  }

  interface Api {
    enum Code {
      SUCCESS,
    }

    @Data
    class Request {
      String id;

      public Request(String id) {
        this.id = id;
      }
    }

    @Data
    class Response {
      Code code;
    }
  }

  //  @Wire
  //  static class MyOtherData {
  //    public int id;
  //
  //    public MyOtherData(int id) {}
  //  }

  static class MyData {
    public int id;
    public String name;

    public MyData(int id) {
      this.id = id;
    }

    public MyData(int id, String name) {
      this.id = id;
      this.name = name;
    }
  }
}
