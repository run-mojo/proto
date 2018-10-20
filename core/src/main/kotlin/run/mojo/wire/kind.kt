package run.mojo.wire

/**
 * Protocol Buffers wire data type.
 */
enum class ProtoKind {
    BOOL,
    INT32,
    UINT32,
    SINT32,
    FIXED32,
    INT64,
    UINT64,
    SINT64,
    FIXED64,
    FLOAT,
    DOUBLE,
    STRING,
    BYTES,
    ENUM,
    MESSAGE,
    ONEOF
}


/**  */
enum class JsonKind {
    STRING,
    NUMBER,
    BOOL,
    ARRAY,
    OBJECT
}
