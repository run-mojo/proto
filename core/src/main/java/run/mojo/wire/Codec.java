package run.mojo.wire;

import com.squareup.moshi.JsonAdapter;
import com.squareup.wire.ProtoAdapter;

/** */
public interface Codec<M, B> {
  B newBuilder();

  M build(B builder);

  ProtoAdapter<M> proto();

  JsonAdapter<M> json();
}
