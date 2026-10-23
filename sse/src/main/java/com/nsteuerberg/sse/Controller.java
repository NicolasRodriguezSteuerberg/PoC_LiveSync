package com.nsteuerberg.sse;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/sse")
public class Controller {
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private AtomicInteger counter = new AtomicInteger(0);

    @GetMapping()
    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));

        return emitter;
    }

    @PatchMapping
    public String increment() {
        int value = counter.incrementAndGet();
        publish(value);
        return "Incrementado con éxito";
    }

    private void publish(int counterValue) {
        Tablero updateTablero = new Tablero(
            counterValue,
            "Actualizado",
            System.currentTimeMillis()
        );

        for (SseEmitter emitter: emitters) {
            try {
                emitter.send(
                    SseEmitter
                        .event()
                        .name("counter")
                        .data(updateTablero)
                );
            } catch (IOException e) {
                System.out.println(e);
                emitter.completeWithError(e);
            }
        }
    }
}
