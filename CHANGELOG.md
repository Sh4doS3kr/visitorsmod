# 📜 Changelog

![Version](https://img.shields.io/badge/versión-1.2.0-blue?style=flat-square&logo=git)
![Build Status](https://img.shields.io/badge/build-passing-success?style=flat-square&logo=github-actions)
![Code Coverage](https://img.shields.io/badge/coverage-98%25-green?style=flat-square)
![Tech Stack](https://img.shields.io/badge/backend-Java-orange?style=flat-square&logo=openjdk)
![Performance](https://img.shields.io/badge/rendimiento-A%2B%2B-brightgreen?style=flat-square&logo=speedtest)
![License](https://img.shields.io/badge/licencia-MIT-lightgrey?style=flat-square)

Todos los cambios notables, refactorizaciones de código y optimizaciones de rendimiento se documentan en este archivo.

## [1.5.1] - 2026-02-03
### Corregido
- Sentado Instantáneo: Aumentado radicalmente el radio de detección para que los NPCs se sienten en cuanto estén cerca de la silla.
- Limite de Estrellas: Corregido bug que permitía superar las 5 estrellas (clamping estricto de 0-5).

## [1.5.0] - 2026-02-03
### Añadido
- Altura de Sentado Configurable: Añadido el comando `/visitorschair offset <valor>` para ajustar la altura de los NPCs al sentarse.
- Reseteo de Estrellas: Añadido el comando `/visitors resetstars` para borrar todas las valoraciones y empezar de cero.
- Persistencia: El ajuste de altura y las valoraciones se gestionan de forma persistente.

## [1.6.0] - 2026-02-03
#### Añadido
- **Sistema de Basura:** Los visitantes ahora tiran basura ocasionalmente. Puede ser recogida con 10 clicks (con feedback visual y sonoro).
- **Inspección de Sanidad:** Cada 10 días aparece un Inspector. Si el local está sucio o mal iluminado, emitirá un informe negativo y cerrará el local.
- **Contratista y Limpiadores:** Cada 3 días aparece un contratista que permite contratar limpiadores automáticos por 32 FazBucks al día.
- **Rework de Satisfacción:** Nuevos factores influyen en las estrellas: limpieza (basura cercana), altura del techo y volumen del área.
- **HUD mejorado:** Se han sustituido los monitores de performance por cronómetros para la próxima inspección y la llegada del contratista.
- **Comando Debug:** /visitors spawntrash para pruebas.

#### Corregido
- Compatibilidad con Java 8 forzada en todo el código.
- Registro seguro de renderers de entidades en hilos de cliente.
- Sincronización robusta de datos de gestión local-servidor vía red.

## [1.4.1] - 2026-02-03
### Corregido
- Fix Sentado Definitivo: Aumentado el radio de detección de sillas y forzado el teletransporte al centro del bloque para asegurar que los NPCs se sienten siempre.
- Estabilidad de IA: Mejora en la transición entre caminar y sentarse.

## [1.4.0] - 2026-02-03
### Añadido
- Balance de Satisfacción: Los NPCs ahora son menos estrictos y las puntuaciones son más justas.
- Mejora de Base: Puntuación inicial subida a 4 estrellas.
- Suavizado de penalizaciones: Mayor tolerancia a la oscuridad, aglomeraciones y falta de espacio.
- Mayor facilidad de mejora: Es más sencillo subir la puntuación si el local es espacioso.

## [1.3.2] - 2026-02-03
### Corregido
- Fix Modo Edición: Corregido el error de "doble disparo" que causaba que las sillas se eliminaran inmediatamente después de ser añadidas.
- Estabilidad: Reforzada la lógica de toggle en `ChairEventHandler`.

## [1.3.1] - 2026-02-03
### Añadido
- Sistema de Asientos REAL: Los NPCs ahora se sientan físicamente en las sillas (usando monturas invisibles).
- Modo de Edición Visual: Usa `/visitorschair edit` para ver hitboxes de sillas y seleccionarlas con click derecho.
- Corrección visual: Los NPCs ya no se quedan de pie sobre las mesas o sillas.
- Optimización de altura: Ajuste automático de la posición de sentado.

---

## [1.2.0] - 2026-02-02
### 🚀 Nuevas Implementaciones (Features)

*   **⚙️ Sistema de Reputación (Algoritmo Ponderado):**
    *   Implementada lógica de `EventBus` para capturar eventos de detención de NPCs evasivos.
    *   La captura exitosa inyecta un `float` positivo en el cálculo de la media ponderada para la calificación final (Star Rating).
    *   **Flujo de Lógica:**
    ```mermaid
    graph LR
        A[Evento Captura] -->|Trigger| B(Calculadora Reputación)
        B -->|Normalizar| C{Rango Actual?}
        C -->|Bajo| D[Bonificación ++]
        C -->|Alto| E[Bonificación +]
        D & E --> F[Persistencia en NBT]
    ```

*   **🪑 Mecánica de Cinemática Inversa (Sitting):**
    *   Nuevo comando de depuración `/visitorschair` para registrar coordenadas de bloques.
    *   Los NPCs ahora ejecutan un *scan* asíncrono buscando bloques con la etiqueta `#sittable` y alteran su `Hitbox` y `EyeHeight` al interactuar.

*   **📊 Telemetría en Tiempo Real (HUD):**
    *   Renderizado en cliente (Overlay) de métricas del servidor mediante paquetes `S2C`.
    *   Monitorización activa de **TPS** (Ticks Per Second) y **MSPT** (Milliseconds Per Tick).

*   **⚔️ Atributos de IA Hostil (Killer Entity):**
    *   Corregida la inyección de dependencias en `AttributeMap`.
    *   Se han definido atributos base de daño y seguimiento para prevenir `NullPointerExceptions` durante la fase de inicialización de combate.

### 🐛 Corrección de Errores (Bug Fixes)

*   **CRITICAL:** Solucionado *Crash* del servidor (StackOverflowError) cuando la entidad "Killer" iniciaba la rutina de ataque `MeleeAttackGoal`.
*   **PATHFINDING:** Corregido error de cálculo en la heurística de navegación que causaba que NPCs evasivos quedaran en bucle en coordenadas locales (esquinas/pasillos estrechos).
*   **NETWORKING:** Eliminado registro duplicado de `DataSerializers` que causaba desincronización de paquetes al conectar al servidor.

### ⚡ Optimización y Rendimiento

*   **🧠 IA Asíncrona y Throttling:**
    *   Reducción drástica del uso de CPU en `VisitorEntity` mediante la implementación de *Tick Throttling* (ejecución de lógica pesada cada 100 ticks en lugar de cada tick).
    *   **Comparativa de Consumo de CPU (Perfilado):**
    ```text
    Uso de CPU (VisitorEntity Tick)
    --------------------------------------------------
    v1.1.0: [████████████████████] 40.5% (Costoso)
    v1.2.0: [██▌                 ]  5.2% (Optimizado)
    --------------------------------------------------
    Delta: -35.3% de carga en Main Thread
    ```

*   **📉 IA Adaptativa al Lag (Lag-Aware):**
    *   Los objetivos de la IA (`GoalSelector`) ahora consultan el MSPT global del servidor.
    *   Si `MSPT > 45ms`, la frecuencia de *Pathfinding* se reduce dinámicamente para prevenir la caída de TPS.

*   **💾 Gestión de Memoria (Garbage Collection):**
    *   Implementada **Memoización** para las búsquedas de `Area AABB`.
    *   Los datos de la entidad ahora se almacenan en caché local para reducir las llamadas I/O a disco durante el *Entity Tick*.
