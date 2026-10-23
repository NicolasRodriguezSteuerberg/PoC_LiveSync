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


## WebSocket
**WebSocket** es un protocolo de comunicación bidireccional que permite al
servidor y al cliente comunicarse en tiempo real. Mantiene una conexión
abierta y persistente para transmitir datos de forma continua.

En esta prueba de concepto se está utilizando WebSocket con STOMP, lo que nos permite
tener un sistema de mensajería estructurado (Pub/Sub) y enrutar mensajes directamente
a nuestros controladores.

### Estructura de datos
En este protocolo utilizamos dos estructuras, una para recibir y otra para
enviar. Estos datos se envían en texto convertible a JSON.

#### Request
```json
{
  "increment": true,
  "value": 2
}
```
- *increment*: Si incrementar o decrementar
- *value*: Valor a incrementar o decrementar

#### Response
```json
{
  "counter": 12,
  "message": "Contenido de mensaje",
  "timestamp": 12354545
}
```
- *counter*: El valor del contador.
- *message*: Mensaje informativo.
- *timestamp*: Fecha de la última actualización de contador.

### Configuración
Utiliza la dependencia de `Spring Websocket` para funcionar.

<details>

<summary>pom.xml</summary>

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```
</details>

<details>

<summary>Configuración Websocket</summary>

Habilitar Websocket y configurar
```java
@Configuration
@EnableWebSocketMessageBroker
public class WebsocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // configurar el prefijo predeterminado donde publicar los mensajes
        registry.enableSimpleBroker("/topic");
        // configurar el prefijo predetirminado donde recibir los mensajes
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Configuración de endpoints de conexión WebSocket.
        registry.addEndpoint("/test-ws");
    }
}
```

</details>

<details>
<summary>Recibir Mensajes</summary>

Solo se puede recibir mensajes a través de un @Controller
```java
// Topico donde se reciben los mensajes
@MessageMapping("/sendMessage")
public void onMessage(Request request) {
    doSomething(request);
}
```
</details>

<details>
<summary>Enviar mensajes</summary>
Si solo se va a responder desde donde llega el mensaje se puede utilizar la
anotación `@SendTo("/topic/messages")` y devolver en la función lo que se
quiera enviar. Esto se haría en el propio método donde se recibe el mensaje

```java
@MessageMapping("/sendMessage")
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
```

Si queremos poder enviarlo desde otros lados (por si se puede modificar por
eventos o peticiones HTTP) tenemos que usar el template de `SimpMessagingTemplate`.

```java
// Spring se encarga de inyectarlo
private final SimpMessagingTemplate messagingTemplate;

public WebsocketController(SimpMessagingTemplate messagingTemplate) {
    this.messagingTemplate = messagingTemplate;
}


// Ejemplo de envío
private void sendMsg(Long count) {
    messagingTemplate.convertAndSend(
            "/topic/messages",
            new Tablero(
                    count,
                    "Incrementado",
                    System.currentTimeMillis()
            )
    );
}
```

</details>

<details>
<summary>Reaccionar a clientes conectados/desconectados</summary>

```java
@Component
public class WebSocketEventListener {

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        System.out.println("Nuevo cliente conectado");
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        System.out.println("Cliente desconectado");
    }
}
```

</details>