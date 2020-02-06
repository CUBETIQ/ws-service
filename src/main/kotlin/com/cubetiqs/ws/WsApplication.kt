package com.cubetiqs.ws

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.messaging.converter.MappingJackson2MessageConverter
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.messaging.simp.annotation.SendToUser
import org.springframework.messaging.simp.annotation.SubscribeMapping
import org.springframework.messaging.simp.stomp.StompFrameHandler
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.messaging.simp.stomp.StompSession
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.messaging.WebSocketStompClient
import org.springframework.web.socket.sockjs.client.SockJsClient
import org.springframework.web.socket.sockjs.client.WebSocketTransport
import java.lang.reflect.Type

@SpringBootApplication
class WsApplication : CommandLineRunner {
    override fun run(vararg args: String?) {
        val client = StandardWebSocketClient()
        val transports = listOf(WebSocketTransport(client))
        val sockJsClient = SockJsClient(transports)

        val stompClient = WebSocketStompClient(sockJsClient)
        stompClient.messageConverter = MappingJackson2MessageConverter()
        val url = "ws://localhost:8181/wired"
        val sessionHandler = MyStompSessionHandler()
        val session = stompClient.connect(url, sessionHandler).get()

        session.subscribe("/topic/welcome", object : StompFrameHandler {
            override fun getPayloadType(headers: StompHeaders): Type {
                return String::class.java
            }

            override fun handleFrame(headers: StompHeaders, payload: Any?) {
                System.err.println(payload.toString())
            }
        })

        if (session.isConnected) {
            Thread.sleep(5000)
            session.send("/send/messaging/hello", "{hello tester}")
        }

    }
}

class MyStompSessionHandler : StompSessionHandlerAdapter() {
    override fun afterConnected(
        session: StompSession,
        connectedHeaders: StompHeaders
    ) {
        println("Connected!")
    }
}

fun main(args: Array<String>) {
    runApplication<WsApplication>(*args)
}

@RestController
class TestController @Autowired constructor(
    private val simpMessagingTemplate: SimpMessagingTemplate
){
    @GetMapping("/hello")
    fun hello(): String {
        simpMessagingTemplate.convertAndSend("/topic/welcome", "hello")
        return "sent"
    }
}

@Controller
class TopicController {
    @MessageMapping("/messaging/{pub}") // For Publisher
    @SendTo("/topic/welcome") // For Subscriber
    fun hi(
        @DestinationVariable("pub") pub: String,
        message: String
    ): String {
		println("Publisher $pub")
        println(message)
        return message
    }
}