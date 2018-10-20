plugins {
    base
//   kotlin("jvm") version "1.3.0-rc-146" apply true
    kotlin("jvm") version "1.2.71" apply false
    kotlin("kapt") version "1.2.71" apply false
}

allprojects {
    group = "io.movemedical.proto"
    version = "1.0-SNAPSHOT"

    repositories {
        maven {
            setUrl("https://repo.axismedtech.com/content/repositories/movemedical")

            credentials {
                username = ""
                password = "u8xdAaT5keVnx88XMLBr"
            }
        }

        jcenter()
        maven { setUrl("http://dl.bintray.com/kotlin/kotlin-eap") }
        maven { setUrl("http://dl.bintray.com/kotlin/kotlin") }

        mavenCentral()
        mavenLocal()
        maven {
            setUrl("http://maven.aspose.com/repository/repo")
        }
    }
}

dependencies {
    // Make the root project archives configuration depend on every subproject
    subprojects.forEach {
        archives(it)
    }
}