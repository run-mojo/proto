plugins {
    kotlin("jvm")
}

dependencies {
    compile(kotlin("stdlib"))

    compile(group = "javax.inject", name = "javax.inject", version = "1")
    compile(group = "com.google.code.findbugs", name = "annotations", version = "3.0.1")
    compile(group = "com.google.flatbuffers", name = "flatbuffers-java", version = "1.9.0")
    compile(group = "com.squareup.okio", name = "okio", version = "2.1.0")
    compile(group = "com.squareup", name = "javapoet", version = "1.11.1")
    compile(group = "com.squareup", name = "kotlinpoet", version = "1.0.0-RC1")
    compile(group = "com.squareup.wire", name = "wire-runtime", version = "2.2.0")
    compile(group = "com.squareup.wire", name = "wire-compiler", version = "2.2.0")
    compile(group = "com.squareup.moshi", name = "moshi", version = "1.7.0")
    compile(group = "com.fasterxml.jackson.core", name = "jackson-annotations", version = "2.9.6")
    compile(group = "com.fasterxml.jackson.core", name = "jackson-databind", version = "2.9.6")
    compile(group = "com.google.auto.value", name = "auto-value-annotations", version = "1.6.2")

    compile(group = "io.grpc", name = "grpc-netty-shaded", version = "1.15.1")
    compile(group = "io.grpc", name = "grpc-protobuf", version = "1.15.1")
    compile(group = "io.grpc", name = "grpc-stub", version = "1.15.1")

    implementation(group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version = "0.30.2")

    testImplementation("junit:junit:4.12")
}