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
