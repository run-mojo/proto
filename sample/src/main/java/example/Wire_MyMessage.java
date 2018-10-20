package example;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.squareup.wire.FieldEncoding;
import com.squareup.wire.ProtoAdapter;
import com.squareup.wire.ProtoReader;
import com.squareup.wire.ProtoWriter;
import run.mojo.builder.MessageBuilder;

import javax.annotation.Nullable;
import java.io.IOException;

/**
 *
 */
public class Wire_MyMessage {

    public static final Proto PROTO = new Proto();

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends MessageBuilder<MyMessage, Builder> {

        public Builder() {
        }

        @Override
        public MyMessage build() {
            return null;
        }

        public ProtoAdapter<MyMessage> adapter() {
            return PROTO;
        }
    }

    public static class Descriptor {

    }

    public static class Proto extends ProtoAdapter<MyMessage> {

        public Proto() {
            super(FieldEncoding.LENGTH_DELIMITED, MyMessage.class);
        }

        @Override
        public int encodedSize(MyMessage value) {
            return 0;
        }

        @Override
        public void encode(ProtoWriter writer, MyMessage value) throws IOException {

        }

        @Override
        public MyMessage decode(ProtoReader reader) throws IOException {
            return null;
        }
    }

    public static class Json extends JsonAdapter<MyMessage> {
        @Nullable
        @Override
        public MyMessage fromJson(JsonReader reader) throws IOException {
            return null;
        }

        @Override
        public void toJson(JsonWriter writer, @Nullable MyMessage value) throws IOException {

        }
    }
}
