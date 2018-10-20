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
}


kapt {
    correctErrorTypes = true

    javacOptions {
//        option("BuilderMode", "ALWAYS")
    }

    arguments {
        arg("BuilderMode", "ALWAYS")
        arg("GenInterfaces", "ALWAYS")
    }
}