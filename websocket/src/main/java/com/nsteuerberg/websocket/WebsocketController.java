package com.nsteuerberg.websocket;

import org.springframework.data.redis.annotation.RedisListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.messaging.RedisMessageSendingTemplate;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Controller
public class WebsocketController {
    private AtomicInteger counter = new AtomicInteger(0);
    private final SimpMessagingTemplate messagingTemplate;
    private final RedisMessageSendingTemplate redisTemplate;

    public WebsocketController(SimpMessagingTemplate messagingTemplate, RedisMessageSendingTemplate redisTemplate) {
        this.messagingTemplate = messagingTemplate;
        this.redisTemplate = redisTemplate;
    }

    /*
    @MessageMapping("/sendMessage")
    // Envia automáticamente el mensaje al devolver el objeto en el metodo
    @SendTo("/topic/messages")
    public Tablero sendMessage(Request request) {
        int count = counter.addAndGet(
            request.increment()
                ? request.value()
                : -request.value()
        );

        return new Tablero(
            count,
            "Actualizar",
            System.currentTimeMillis()
        );
    }
     */

    // Solo se puede recibir mensaje a través de un @Controller
    @MessageMapping("/sendMessage")
    public void onMessage(Request request) {
        //sendMsg(request);
        int count = counter.addAndGet(request.value());
        redisTemplate.convertAndSend(
                "counter",
                new Tablero(
                        count,
                        "incrementado",
                        System.currentTimeMillis()
                ),
                Map.of(MessageHeaders.CONTENT_TYPE, "application/json")
        );
    }

    // De esta forma podemos enviar mensajes desde cualquier lado, sin pasar por el @Controller
    private void sendMsg(Request request) {
        int count = counter.addAndGet(request.value());

        messagingTemplate.convertAndSend(
            "/topic/messages",
            new Tablero(
                    count,
                    "Incrementado",
                    System.currentTimeMillis()
            )
        );
    }

    @RedisListener(topic = "counter")
    public void onCounterMsg(Tablero tablero) {
        counter.set(tablero.counter());
        messagingTemplate.convertAndSend(
            "/topic/messages",
            tablero
        );
    }
}
