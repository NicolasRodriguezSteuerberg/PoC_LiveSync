# PoC - Actualización de datos en tiempo real
En esta prueba se utilizan diferentes medios de comunicación para notificar al cliente en tiempo real.

- `SSE` (Server-Sent Events): Comunicación unidireccional.
- `WS` (WebSockets): Comunicación bidireccional.
- `Redis PUB/SUB` como intermediario entre instancias.

## Objetivo
Comprobar que las actualizaciones llegan en tiempo real a los clientes con las diferentes tecnologías.

## SSE (Server-Sent Events)
**SSE** es un protocolo de comunicación unidireccional que permite al servidor enviar actualizaciones
a un cliente en tiempo real. Mantiene una conexión HTTP abierta y persistente para trasmitir datos
de forma continua.

### Estructura de datos
```text
event: messageText
id: 12345
data: data
```

- *event* (Opcional): El nombre del evento, permite que el cliente escuche eventos específicos
- *id* (Opcional): Identificador del evento
- *data*: Texto con el contenido del evento

### Configuración
Utiliza la dependencia de `SpringWeb` para funcionar
<details>
<summary>pom.xml</summary>

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

</details>

<details>
<summary>Conexión SSE</summary>

```java
@GetMapping()
public SseEmitter subscribe() {
    // Configuramos cuanto tiempo queremos que se mantenga abierta la conexión
    SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
    // Guardamos el emitter para poder enviarle mensajes
    emitters.add(emitter);
    
    // Que hacer cuando termine la conexión
    emitter.onCompletion(() -> emitters.remove(emitter));
    // Que hacer cuando termine el timeout
    emitter.onTimeout(() -> emitters.remove(emitter));

    return emitter;
}
```

</details>

<details>
<summary>Enviar Evento</summary>

```java
private void publish(int counterValue) {
    // Creamos el evento a enviar (Spring serializará automáticamente el objeto con Jackson
    Tablero updateTablero = new Tablero(
        counterValue,
        "Actualizado",
        System.currentTimeMillis()
    );
    
    for (SseEmitter emitter: emitters) {
        try {
            emitter.send(
                // construimos el evento
                SseEmitter.event()
                    .id("msg-1") // agregamos el id (Opcional)
                    .name("counter") // agregamos el nombre al evento (Opcional)
                    .data(updateTablero) // agregamos los datos
            );
        } catch (IOException e) {
            // finalizamos la conexión con error
            emitter.completeWithError(e); 
        }
    }
    }
```

</details>