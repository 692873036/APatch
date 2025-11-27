import com.android.build.api.dsl.ApplicationDefaultConfig
import com.android.build.api.dsl.CommonExtension
import com.android.build.gradle.api.AndroidBasePlugin

plugins {
    alias(libs.plugins.agp.app) apply false
    alias(libs.plugins.agp.lib) apply false
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.kotlin.compose.compiler) apply false
    // 解决kotlin-parcelize插件版本问题（仅添加这一行）
    id("org.jetbrains.kotlin.plugin.parcelize") version libs.versions.kotlin get() apply false
}

// 全局配置（固定版本号为888888，与你的APatch项目适配）
project.ext.set("kernelPatchVersion", "0.12.2")
val androidMinSdkVersion = 26
val androidTargetSdkVersion = 36
val androidCompileSdkVersion = 36
val androidBuildToolsVersion = "36.0.0"
val androidCompileNdkVersion = "29.0.14206865"
val managerVersionCode by extra(888888) // 固定版本号
val managerVersionName by extra("888888") // 固定版本名
val branchname by extra(getbranch())

// 分支名称获取逻辑（保留原功能，异常时默认main）
fun Project.exec(command: String) = providers.exec {
    commandLine(command.split(" "))
}.standardOutput.asText.get().trim()

fun getbranch(): String {
    return try {
        exec("git rev-parse --abbrev-ref HEAD").trim()
    } catch (e: Exception) {
        "main"
    }
}

// 版本号验证任务（可运行 ./gradlew printVersion 查看是否为888888）
tasks.register("printVersion") {
    doLast {
        println("Version code: $managerVersionCode")
        println("Version name: $managerVersionName")
    }
}

// 子项目全局配置（统一SDK版本，避免构建警告）
subprojects {
    plugins.withType(AndroidBasePlugin::class.java) {
        extensions.configure(CommonExtension::class.java) {
            compileSdk = androidCompileSdkVersion
            buildToolsVersion = androidBuildToolsVersion
            ndkVersion = androidCompileNdkVersion

            defaultConfig {
                minSdk = androidMinSdkVersion
                if (this is ApplicationDefaultConfig) {
                    targetSdk = androidTargetSdkVersion
                    versionCode = managerVersionCode
                    versionName = managerVersionName
                }
            }

            lint {
                abortOnError = true
                checkReleaseBuilds = false
            }
        }
    }
}
