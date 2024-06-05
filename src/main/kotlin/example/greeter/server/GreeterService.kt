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
