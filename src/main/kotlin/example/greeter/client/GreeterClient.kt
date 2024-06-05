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