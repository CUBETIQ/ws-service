package com.cubetiqs.ws

import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.messaging.converter.MappingJackson2MessageConverter
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.messaging.simp.stomp.StompFrameHandler
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.messaging.simp.stomp.StompSession
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter
import org.springframework.stereotype.Controller
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
		val url = "ws://localhost:8080/ws"
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

		session.send("/app/hi", "hello")
	}
}

class MyStompSessionHandler : StompSessionHandlerAdapter() {
	private fun showHeaders(headers: StompHeaders) {
		for ((key, value) in headers) {
			System.err.print("  $key: ")
			var first = true
			for (v in value) {
				if (!first) System.err.print(", ")
				System.err.print(v)
				first = false
			}
			System.err.println()
		}
	}

	private fun sendJsonMessage(session: StompSession) {
		session.send("/app/hi", "ji ji ji")
	}

	private fun subscribeTopic(topic: String, session: StompSession) {
		session.subscribe(topic, object : StompFrameHandler {
			override fun getPayloadType(headers: StompHeaders): Type {
				return String::class.java
			}

			override fun handleFrame(headers: StompHeaders,
									 payload: Any?) {
				System.err.println(payload.toString())
			}
		})
	}

	override fun afterConnected(session: StompSession,
								connectedHeaders: StompHeaders) {
		System.err.println("Connected! Headers:")
		showHeaders(connectedHeaders)
		subscribeTopic("/topic/welcome", session)
		sendJsonMessage(session)
	}
}
fun main(args: Array<String>) {
    runApplication<WsApplication>(*args)
}

@Controller
class TopicController {
    @MessageMapping("/hi/{topic}")
    @SendTo("/topic/welcome")
    fun hi(@DestinationVariable("topic") topic: String,
           message: String): String {
        println(message)
        return message
    }
}