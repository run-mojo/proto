import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
//    application
    kotlin("jvm")
}

//application {
////    mainClassName = "cli.Main"
//}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}


kotlin {
    experimental.coroutines = Coroutines.ENABLE
}

dependencies {
    compile(project(":core"))
    compile(kotlin("stdlib"))
}