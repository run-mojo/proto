package run.mojo.wire;

import com.squareup.moshi.JsonAdapter;
import com.squareup.wire.ProtoAdapter;

/** */
public abstract class AbstractCodec<M, B> implements Codec<M, B> {

  public final ProtoAdapter<M> proto;
  public final JsonAdapter<M> json;

  public AbstractCodec(ProtoAdapter<M> proto, JsonAdapter<M> json) {
    this.proto = proto;
    this.json = json;

  }

  @Override
  public ProtoAdapter<M> proto() {
    return proto;
  }

  @Override
  public JsonAdapter<M> json() {
    return json;
  }
}
