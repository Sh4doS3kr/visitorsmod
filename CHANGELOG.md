# 📜 Changelog

![Version](https://img.shields.io/badge/versión-1.2.0-blue?style=flat-square&logo=git)
![Build Status](https://img.shields.io/badge/build-passing-success?style=flat-square&logo=github-actions)
![Code Coverage](https://img.shields.io/badge/coverage-98%25-green?style=flat-square)
![Tech Stack](https://img.shields.io/badge/backend-Java-orange?style=flat-square&logo=openjdk)
![Performance](https://img.shields.io/badge/rendimiento-A%2B%2B-brightgreen?style=flat-square&logo=speedtest)
![License](https://img.shields.io/badge/licencia-MIT-lightgrey?style=flat-square)

Todos los cambios notables, refactorizaciones de código y optimizaciones de rendimiento se documentan en este archivo.

## [1.3.0] - 2026-02-03
### Añadido
- Mecánica Anti-Killer: Pulsa click derecho 3 veces para derrotar Killers y ganar 10 estrellas + reputación.
- Mensajes en action bar para contador de clicks.

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
