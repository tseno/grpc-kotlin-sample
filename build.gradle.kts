import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.plugins
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

plugins {
    kotlin("jvm") version "1.4.30"
    // GradleでProtocal Buffersを使うためのプラグイン
    id("com.google.protobuf") version "0.8.15"
    id("idea")
}

repositories {
    gradlePluginPortal()
    jcenter()
    google()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")

    // gRPCでサーバーと通信するクライアント部分の実装をするKotlinのライブラリ
    implementation("io.grpc:grpc-kotlin-stub:1.0.0")
    // NettyでサーバーでgPRCアプリケーションを立ち上げるために必要なライブラリ
    implementation("io.grpc:grpc-netty:1.35.0")

    compileOnly("javax.annotation:javax.annotation-api:1.3.2")
}

// protocでのコード生成を実行するGradleタスク
protobuf {
    // protocのパッケージ、バージョンを指定
    protoc { artifact = "com.google.protobuf:protoc:3.15.1" }
    plugins {
        // それぞれJava、Kotlin用のgRPCプラグインを指定
        // Kotlinで生成したコードは、Javaで生成したコードを呼び出すため、Javaも必要
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.36.0"
        }
        id("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:1.0.0:jdk7@jar"
        }
    }
    // コード生成を実行するタスク
    generateProtoTasks {
        all().forEach {
            it.plugins {
                id("grpc")
                id("grpckt")
            }
        }
    }
}