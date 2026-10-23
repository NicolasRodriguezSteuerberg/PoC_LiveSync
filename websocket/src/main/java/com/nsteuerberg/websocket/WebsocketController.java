package com.nsteuerberg.websocket;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.concurrent.atomic.AtomicInteger;

@Controller
public class WebsocketController {
    private AtomicInteger counter = new AtomicInteger(0);
    private final SimpMessagingTemplate messagingTemplate;

    public WebsocketController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /*
    @MessageMapping("/sendMessage")
    // Solo manda el mensaje desde donde se recibe y devolviendo el objeto a enviar
    @SendTo("/topic/messages")
    public Tablero sendMessage(Request request) {
        int count = counter.addAndGet(request.value());

        // se manda solo gracias al SendTo
        return new Tablero(
            count,
            "Incrementado",
            System.currentTimeMillis()
        );
    }
     */

    // Solo se puede recibir mensaje a través de un @Controller
    @MessageMapping("/sendMessage")
    public void onMessage(Request request) {
        sendMsg(request);
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
}
