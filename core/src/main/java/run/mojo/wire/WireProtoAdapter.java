package run.mojo.wire;

import com.squareup.wire.FieldEncoding;
import com.squareup.wire.ProtoAdapter;

/** */
public abstract class WireProtoAdapter<T> extends ProtoAdapter<T> {

  public WireProtoAdapter(FieldEncoding fieldEncoding, Class<T> javaType) {
    super(fieldEncoding, javaType);
  }
}
