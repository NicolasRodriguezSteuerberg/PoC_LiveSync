# PoC - Actualización de datos en tiempo real

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