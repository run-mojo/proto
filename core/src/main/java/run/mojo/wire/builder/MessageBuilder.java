package run.mojo.wire.builder;

import com.squareup.wire.FieldEncoding;
import com.squareup.wire.ProtoAdapter;
import com.squareup.wire.ProtoWriter;
import okio.Buffer;
import okio.ByteString;

import java.io.IOException;

/** Superclass for protocol buffer message builders. */
public abstract class MessageBuilder<T, B extends MessageBuilder<T, B>> {
  // Lazily-instantiated buffer and writer of this message's unknown fields.
  Buffer unknownFieldsBuffer;
  ProtoWriter unknownFieldsWriter;

  protected MessageBuilder() {}

  public final MessageBuilder<T, B> addUnknownFields(ByteString unknownFields) {
    if (unknownFields.size() > 0) {
      if (unknownFieldsWriter == null) {
        unknownFieldsBuffer = new Buffer();
        unknownFieldsWriter = new ProtoWriter(unknownFieldsBuffer);
      }
      try {
        unknownFieldsWriter.writeBytes(unknownFields);
      } catch (IOException e) {
        throw new AssertionError();
      }
    }
    return this;
  }

  public final MessageBuilder<T, B> addUnknownField(
      int tag, FieldEncoding fieldEncoding, Object value) {
    if (unknownFieldsWriter == null) {
      unknownFieldsBuffer = new Buffer();
      unknownFieldsWriter = new ProtoWriter(unknownFieldsBuffer);
    }
    try {
      ProtoAdapter<Object> protoAdapter = (ProtoAdapter<Object>) fieldEncoding.rawProtoAdapter();
      protoAdapter.encodeWithTag(unknownFieldsWriter, tag, value);
    } catch (IOException e) {
      throw new AssertionError();
    }
    return this;
  }

  public final MessageBuilder<T, B> clearUnknownFields() {
    unknownFieldsWriter = null;
    unknownFieldsBuffer = null;
    return this;
  }

  /**
   * Returns a byte string with this message's unknown fields. Returns an empty byte string if this
   * message has no unknown fields.
   */
  public final ByteString buildUnknownFields() {
    return unknownFieldsBuffer != null
        ? unknownFieldsBuffer.clone().readByteString()
        : ByteString.EMPTY;
  }

  /** Returns a Message based on the fields that set in this builder. */
  public abstract T build();
}
