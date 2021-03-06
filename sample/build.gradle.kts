import org.jetbrains.kotlin.gradle.dsl.Coroutines

plugins {
    application
    kotlin("jvm")
    kotlin("kapt")
}

application {
    mainClassName = "example.Main"
}

kotlin {
    experimental.coroutines = Coroutines.ENABLE
}

dependencies {
    compile(project(":core"))
    compile(kotlin("stdlib"))
    annotationProcessor(project(":processor"))
    kapt(project(":processor"))

    compile(group= "com.google.guava", name = "guava", version = "26.0-jre")
    compileOnly(group= "org.projectlombok", name = "lombok", version = "1.16.16")
//    annotationProcessor(group= "org.projectlombok", name = "lombok", version = "1.16.16")
    compile(group = "com.google.auto.value", name = "auto-value-annotations", version = "1.6.2")
    compile(group = "com.google.auto.value", name = "auto-value", version = "1.6.2")
}


kapt {
    correctErrorTypes = true

    javacOptions {
//        option("BuilderMode", "ALWAYS")
    }

    arguments {
//        arg("BuilderMode", "ALWAYS")
//        arg("GenInterfaces", "ALWAYS")
        arg("wire_lombok_data", "Y")
        arg("wire_action_annotation",
                "action,io.movemedical.server.annotations.Action," +
                "io.movemedical.server.essentials.verticle.MoveAction,0,1" +
                ";queue,io.movemedical.server.annotations.Queue," +
                "io.movemedical.server.essentials.verticle.MoveQueue,0,-1")
    }
}