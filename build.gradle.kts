// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.secrets) apply false
}

tasks.register<Copy>("copyApk") {
    from("app/build/outputs/apk/debug") {
        include("app-debug.apk")
    }
    into(rootDir)
    doLast {
        copy {
            from("app/build/outputs/apk/debug") {
                include("app-debug.apk")
            }
            into(file(".build-outputs"))
        }
    }
}

