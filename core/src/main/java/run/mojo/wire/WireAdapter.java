package run.mojo.wire;

import com.squareup.moshi.JsonAdapter;
import com.squareup.wire.ProtoAdapter;

/** */
public interface WireAdapter<T> {

  ProtoAdapter<T> proto();

  JsonAdapter<T> json();
}
