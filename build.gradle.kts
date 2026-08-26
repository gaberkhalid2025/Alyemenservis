// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.secrets) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  id("com.google.gms.google-services") version "4.4.2" apply false
}

tasks.register<Copy>("copyApk") {
    from("app/build/outputs/apk/debug") {
        include("app-debug.apk")
    }
    from("app/build/outputs/apk/release") {
        include("app-release.apk")
    }
    into(rootDir)
    doLast {
        copy {
            from("app/build/outputs/apk/release") {
                include("app-release.apk")
            }
            into(file(".build-outputs"))
        }
        copy {
            from("app/build/outputs/apk/release") {
                include("app-release.apk")
            }
            into(file("assets"))
        }
        copy {
            from("app/build/outputs/apk/release") {
                include("app-release.apk")
            }
            into(file("app/src/main/assets"))
        }
    }
}

