buildscript {
    val kotlin_version by extra("1.9.24")

    repositories {
        google()
        mavenCentral()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:8.2.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version")
    }
}


tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}