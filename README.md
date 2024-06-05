# KotlinでgRPC通信

## gRPCとは

- Googleが開発しているPRC (Remote Procedure Call) フレームワーク
- HTTP/2
- Protocol Buffers
  - IDL（インターフェース定義言語）を使用する
- ハイパフォーマンス
- マイクロサービスでのサービス間通信

## build.gradle.ktsの設定（Java8）

- 各ライブラリのバージョンの指定が難しいので、ここでは確実に動作するバージョンを指定している
- Intellij IDEAでのバージョンの指定は以下の３カ所変更する必要がある
  - JAVA_HOME
  - ファイル→プロジェクト構造→プロジェクト→SDK
  - ファイル→設定→ビルド、実行、デプロイメント→ビルドツール→Gradle→Gradle JVM
- `./gradlew -v` でのgradleバージョンは、6.6.1

```kotlin:build.gradle.kts
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
```

## IDLの定義

- `.proto` という拡張子
- Kotlin、Java、Go、C#、Pythonなどへのコード生成には、`protoc`というツールを使用する

```protobuf:src\main\proto\greeter.proto
// Protocol Buffersのバージョン指定（デフォルトは2系）
syntax = "proto3";

// 生成されるファイルのクラスのパッケージ
package example.greeter;

// 各種オプションを定義する
// ファイルを分割して出力する
option java_multiple_files = true;

// インタフェースを定義する（Spring Bootで例えると、Controllerのような部分）
service Greeter {
  // `rpc メソッド名 (リクエストの型) returns レスポンスの型`
  rpc Hello (HelloRequest) returns (HelloResponse);
}

// リクエスト、レスポンスのデータの定義
// `message リクエスト、レスポンスの型`
message HelloRequest {
  // フィールドの番号（ここでは1）は、そのフィールドの一意の識別子
  string name = 1;
}

message HelloResponse {
  string text = 1;
}
```


## Protol Buffersによるコード生成

```shell
$ ./gradlew generateProto
```

### 生成されるファイル

![image.png](https://qiita-image-store.s3.ap-northeast-1.amazonaws.com/0/88686/3fb63713-9a23-01a3-e091-ee81452a4ca1.png)

- grpc配下・・・　gRPC通信を実現するためのインタフェース
  - GreeterGrpc.kt
- grpckt配下・・・データのシリアライズ、デシリアライズなど
  - GreeterOuterClassGrpcKt.kt
- java配下・・・デフォルトで生成されるコード、リクエスト、レスポンスとそのビルダー
  - GreeterOuterClass.java
  - HelloRequest.java
  - HelloRequestOrBuilder.java
  - HelloResponse.java
  - HelloResponseOrBuilder.java

## gRPCサーバーの実装

- Hello関数を実装

```kotlin:src\main\kotlin\example\greeter\server\GreeterService.kt
package example.greeter.server

import example.greeter.GreeterGrpcKt
import example.greeter.HelloRequest
import example.greeter.HelloResponse

class GreeterService : GreeterGrpcKt.GreeterCoroutineImplBase() {
    override suspend fun hello(request: HelloRequest) = HelloResponse.newBuilder()
        // リクエストに対して、`Hello`を付けて返す
        .setText("Hello ${request.name}")
        .build()
}
```

- サーバーの起動

```kotlin:src\main\kotlin\example\greeter\server\GreeterServer.kt
package example.greeter.server

import io.grpc.ServerBuilder

private const val PORT = 50051

fun main() {
    // サーバーのオブジェクトを生成
    val server = ServerBuilder
        .forPort(PORT)
        // 起動対象のサービスを登録
        .addService(GreeterService())
        .build()

    // サーバーの起動
    server.start()
    println("Started. port:$PORT")

    // アプリケーションが停止されるまでサーバーのリクエストを受け付ける
    server.awaitTermination()
}
```

## gRPCクライアントの実装

```kotlin:src\main\kotlin\example\greeter\client\GreeterClient.kt
package example.greeter.client

import example.greeter.GreeterGrpcKt
import example.greeter.HelloRequest
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit

private const val HOST = "localhost"
private const val PORT = 50051

fun main() = runBlocking {
    // Channelを生成して、gRPCサーバーに接続
    val channel = ManagedChannelBuilder.forAddress(HOST, PORT)
        // SSLを無効化（SSLが必要であればこの項目を削除する）
        .usePlaintext()
        .build()

    try {
        // サーバーに対してリクエストを送信するためのStubを生成
        // Coroutineで非同期処理を行う
        val stub = GreeterGrpcKt.GreeterCoroutineStub(channel)

        val name = "Kotlin"
        // リクエストを生成 HelloRequestのnameに`Kotlin`を設定
        val request = HelloRequest.newBuilder().setName(name).build()
        // helloはsuspend関数なので、runBlocking内でasyncを使って非同期処理を行う
        val response = async { stub.hello(request) }

        println("Response Text: ${response.await().text}")
    } finally {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
    }
}
```

## 動作確認

### サーバーの起動

- `GreeterServer.kt` の `main` 関数を実行

```log
Started. port:50051
```

### クライアントの起動

- `GreeterClient.kt` の `main` 関数を実行

```log
Response Text: Hello Kotlin
```

## ソースコード

https://github.com/tseno/grpc-kotlin-sample

## 参考

[Kotlin サーバーサイドプログラミング実践開発](https://direct.gihyo.jp/view/item/000000001458)
