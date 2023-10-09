plugins {
    `java-library`
}

allprojects {
    apply(plugin = "java-library")

    repositories {
        mavenCentral()
    }

    dependencies {
        api("org.apache.commons:commons-math3:3.6.1")

        implementation("com.google.guava:guava:31.1-jre")
    }

    testing {
        suites {
            val test by getting(JvmTestSuite::class) {
                useJUnit("4.13.2")
            }
        }
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(8))
        }
    }
}
