package example;

import com.google.common.collect.ImmutableList;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.Value;

import java.util.ArrayList;

/**
 *
 */
@Data
@Builder
public class MyMessage {

    public int id;
    @Default
    public String name = "default";
    public ImmutableList<String> options;
      Records<Integer> records;

  public static void main(String[] args) {
    new Records<String>(10, null);
//    MyMessage.builder().name("").build();
  }
}
