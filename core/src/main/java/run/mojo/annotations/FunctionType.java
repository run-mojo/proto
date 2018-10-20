package run.mojo.annotations;

public @interface FunctionType {

    Kind value();

    enum Kind {
        UNARY_ENQUEUE,

        UNARY,

        CLIENT_STREAMING,

        SERVER_STREAMING,

        DUPLEX_STREAMING,
    }
}
