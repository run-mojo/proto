import org.jetbrains.kotlin.daemon.common.configureDaemonJVMOptions
import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    kotlin("jvm")
    kotlin("kapt")
}

application {
    mainClassName = "cli.Main"
    group = "io.movemedical.proto.cli"
    version = "2.0"
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<JavaCompile> {
    options.compilerArgs = listOf(
        "-AmojoBuilderMode=io.movemedical.server.annotations.Action",
        "-Awire_action_annotation=" +
                "action,io.movemedical.server.annotations.Action," +
                "io.movemedical.server.essentials.verticle.MoveAction,0,1" +
                ";queue,io.movemedical.server.annotations.Queue," +
                "io.movemedical.server.essentials.verticle.MoveQueue,0,-1",
        "-Awire_action_resolve=action@io.movemedical.server.essentials.verticle.MoveAction<REQUEST, RESPONSE>;action@io.movemedical.server.essentials.verticle.MoveQueue<REQUEST>",
        "-Awire_action_dispatcher=NONE"
    )
    options.isFork = true // required for above?
    options.forkOptions.executable = "javac" // required for above?
}


kotlin {

    experimental.coroutines = Coroutines.ENABLE
}

dependencies {
    compile(project(":core"))
    compile(kotlin("stdlib"))
    annotationProcessor(project(":processor"))
    kapt(project(":processor"))

    implementation(group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version = "0.30.2")

    compile(group = "io.grpc", name = "grpc-netty-shaded", version = "1.15.1")
    compile(group = "io.grpc", name = "grpc-protobuf", version = "1.15.1")
    compile(group = "io.grpc", name = "grpc-stub", version = "1.15.1")
    compile(group = "com.movemedical", name = "move-server-app", version = "2.2.0-+")
}