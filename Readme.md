# PoC - Actualización de datos en tiempo real

## Descripción
Esta prueba de concepto (PoC) explora diferentes mecanismos de comunicación para
enviar actualizaciones en tiempo real a los clientes, utilizando Spring Boot

Se estudian tres tecnologías:

- `SSE` (Server-Sent Events): Comunicación unidireccional del servidor al cliente.
- `WebSocket + STOMP`: Comunicación bidireccional mediante conexiones persistentes
y mensajería estructurada
- `Redis Pub/Sub`: distribución de mensajes entre distintas instancias de la aplicación.

## Objetivos
- Implementar y comparar diferentes mecanismos de comunicación en tiempo real.
- Comprobar cómo reciben los clientes las actualizaciones del servidor.
- Distribuir los mensajes entre varias instancias de Spring Boot mediante Redis Pub/Sub.
- Verificar que un cliente conectado a una instancia recibe las actualizaciones 
generadas en otra.

## Arquitectura
La PoC utiliza dos niveles de comunicación:
1. **Comunicación con los clientes**: SSE o WebSocket, según la implementación.
2. **Comunicación entre instancias**: Redis Pub/Sub permite distribuir mensajes
entre las instancias de Spring Boot.

## Comparativa de Tecnologías
| Tecnología      | Comunicación                              | Uso en la PoC                                          |
|-----------------|-------------------------------------------|--------------------------------------------------------|
| *SSE*           | Servidor -> Cliente                       | Enviar actualizaciones mediante HTTP.                  |
| *WS + STOPM*    | Bidireccional                             | Recibir peticiones y enviar mensajes.                  |
| *Redis Pub/Sub* | Publicación/Suscripción entre componentes | Distribuir mensajes entre instancias de la aplicación. |

Redis Pub/Sub no sustituye a SSE ni a WebSocket. En esta prueba se utiliza como mecanismo
complementario para distribuir las actualizaciones entre instancias.

## SSE (Server-Sent Events)
**SSE** es un protocolo de comunicación unidireccional que permite al servidor enviar actualizaciones
a un cliente en tiempo real. Mantiene una conexión HTTP abierta y persistente para trasmitir eventos.

### Estructura de eventos
Los mensajes SSE siguen una estructura basada en campos de texto:

```text
event: messageText
id: 12345
data: data
```

- *event* (Opcional): Nombre del evento, permite al cliente escuchar tipos de eventos específicos.
- *id* (Opcional): Identificador del evento
- *data*: Contenido del evento. Puede ser texto o JSON serializado.

### Configuración
Utiliza la dependencia de `spring-boot-starter-web.
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
<summary>Establecer una conexión SSE</summary>

```java
@GetMapping()
public SseEmitter subscribe() {
    // Configuramos cuanto tiempo queremos que se mantenga abierta la conexión
    SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
    
    // Guardamos la conexión para enviarle actualizaciones
    emitters.add(emitter);
    
    // Liberamos los recursos cuando termina la conexión
    emitter.onCompletion(() -> emitters.remove(emitter));
    emitter.onTimeout(() -> emitters.remove(emitter));
    emitter.onError(error -> emitters.remove(emitter));

    return emitter;
}
```

</details>

<details>
<summary>Enviar eventos</summary>

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

En esta PoC se está utilizando WebSocket junto a STOMP, un protocolo de mensajería
que permite organizar las comunicaciones mediante destinos, suscripciones y mensajes.

### Estructura de los mensajes
Se utilizan dos estructuras JSON: una para las peticiones y otra para las actualizaciones.

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
Utiliza la dependencia de `spring-boot-starter-websocket`.

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

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebsocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Destinos de salida para los clientes suscritos
        registry.enableSimpleBroker("/topic");
        
        // Prefijo de los mensajes dirigidos a la aplicación
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint de conexión WebSocket
        registry.addEndpoint("/test-ws");
    }
}
```

</details>

<details>
<summary>Recibir Mensajes</summary>

Los mensajes dirigidos a la aplicación se procesan mediante métodos anotados
con @MessageMapping.
```java
@MessageMapping("/sendMessage")
public void onMessage(Request request) {
    doSomething(request);
}
```
Con el prefijo /app configurado anteriormente, el cliente envía el mensaje a 
/app/sendMessage

</details>

<details>
<summary>Enviar mensajes</summary>
Existen os alternativas principales para publicar mensajes en los destinos STOMP.

<details>
<summary>Opción 1: utilizar @SendTo</summary>

Si el mensaje se genera como respuesta al procesamiento de una petición,
se puede utilizar @SendTo.

```java
@MessageMapping("/sendMessage")
@SendTo("/topic/messages")
public Tablero sendMessage(Request request) {
    int count = counter.addAndGet(
        request.increment()
            ? request.value()
            : -request.value()
    );

    // se manda solo gracias al SendTo
    return new Tablero(
        count,
        "Actualizado",
        System.currentTimeMillis()
    );
}
```
El valor devuelto se envía automáticamente a `/topic/messages`.

</details>

<details>
<summary>Opción 2: Utilizar SimpMessagingTemplate</summary>

Cuando se necesita enviar mensajes desde distintos componentes, por ejemplo un
servicio, una petición HTTP o un listener de Redis, se puede utilizar
`SimpMessagingTemplate`.

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
            "Actualizado",
            System.currentTimeMillis()
        )
    );
}
```

</details>


</details>

### Eventos de conexión y desconexión
Spring permite detectar eventos relacionados con las sesiones WebSocket.

<details>
<summary>Listener de eventos WebSocket</summary>

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


## Redis Pub/Sub
**Redis** es un sistema de mensajería basado en el modelo publicación/suscripción.
Los publicadores envían mensajes a canales y Redis los distribuye a los suscriptores
que estén escuchando esos canales.

En esta PoC se utiliza para distribuir mensajes entre distintas instancias de Spring Boot.
Cuando una instancia publica una actualización en Redis, las instancias suscritas
pueden recibirla y reenviarla a sus clientes mediante WebSocket.

### Flujo de comunicación
1. Un cliente envía una peticion a una instancia de Spring Boot.
2. La instancia procesa la petición y publica la actualización en el canal Redis `counter`.
3. Redis distribuye el mensaje a las instanias suscritas al canal.
4. Cada listener recibe la actualización y la reenvia a sus clientes mediante `WS` o `SSE`.

### Estructura de mensajes
Las actualizaciones utilizan la estructura Tablero:
```json
{
  "counter": 12,
  "message": "Contenido de mensaje",
  "timestamp": 12354545
}
```

El objeto se publica en Redis y se convierte al formato configurado para la aplicación.

### Configuración
Se utiliza `spring-boot-starter-data-redis`.

<details>
<summary>pom.xml</summary>

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

</details>

<details>
<summary>application.yml</summary>

```yaml
spring:
  redis:
    host: localhost
    port: 6379
```

</details>

<details>
<summary>Configurar el listener de Redis</summary>

```java
@Configuration
@EnableRedisListeners
public class RedisConfig {
    // Configuración adicional
}
```

</details>

<details>
<summary>Recibir mensajes</summary>

```java
@RedisListener(topic = "counter")
public void onCounterMsg(Tablero tablero) {
    doSomething(tablero);
}
```


</details>

<details>
<summary>Publicar mensajes</summary>

Se utiliza `RedisMessageSendigTemplate` para publicar objetos sin tener que convertirlos
manualmente a cadenas.
```java
@Configuration
@EnableRedisListeners
public class RedisConfig {

    @Bean
    public RedisMessageSendingTemplate redisMessageSendingTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);
        redisTemplate.afterPropertiesSet();

        return new RedisMessageSendingTemplate(redisTemplate);
    }
}
```

Ejemplo de publicación:
```java
@Service
public class RedisPublisher {
    private final RedisMessageSendingTemplate redisTemplate;
    
    // Dejamos que redis se encargue de inyectarlo
    public RedisPublisher(RedisMessageSendingTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void publish(int count) {
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
}
```

</details>

## Ejecución y pruebas
### Requisitos
- Java
- Maven
- Docker / instancia de Redis

### Prueba con varias instancias
1. Arrancar Redis 
```shell
docker compose up -d
```
2. Iniciar la aplicación WS/SSE en un puerto, ejemplo 8080.
3. Iniciar una segunda instancia en otro puerto, ejemplo 8081.
4. Conectarse a cada instancia (WS tiene pagina web integrada http://localhost:8080, para SSE
habría que crear pagina web o conectarse por Postman).
5. Enviar una petición (para SSE utilizar POST http://localhost:8080).
6. Comprobar que la actualización llega a los clientes de ambas instancias.

### Resultado esperado
Cuando una instancia publica una actualización en Redis, el listener de cada instancia
suscrita recibe el mensaje y lo reenvía a los clientes conectados a ella.

Esto permite comprobar la distribución de actualizaciones entre distintas instancias
de Spring Boot.

## Conclusiones
Esta prueba de concepto permite explorar distintos mecanismos de comunicación en
tiempo real y sus responsabilidades:

- SSE simplifica el envío de actualizaciones unidireccionales.
- WebSocket con STOMP permite intercambiar mensajes bidireccionales y organizar
las suscripciones de los clientes.
- Redis Pub/Sub permite distribuir eventos entre instancias, complementando los
mecanismos de comunicación con los clientes.
