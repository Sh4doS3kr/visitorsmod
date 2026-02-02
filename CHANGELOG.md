# Historial de Cambios (Changelog)

Todos los cambios notables en este proyecto serán documentados en este archivo.

## [1.2.0] - 2026-02-02
### Añadido
- **Sistema de Reputación**: Capturar NPCs que intentan escapar ahora otorga un bono de reputación que influye en las futuras calificaciones de estrellas.
- **Mecánica de Sentarse**: Nuevo comando `/visitorschair` para designar puntos de asiento. Los NPCs ahora buscarán y ocuparán sillas durante su estancia.
- **Monitor de Rendimiento**: El HUD del cliente ahora muestra los TPS y MSPT del servidor para un monitoreo en tiempo real.
- **Atributos de IA "Killer"**: Corregidos los atributos faltantes que causaban bloqueos del servidor durante el combate.

### Corregido
- Corregido un error crítico que cerraba el servidor cuando los NPCs tipo "Killer" intentaban atacar a los jugadores.
- Corregido el problema de los NPCs que se quedaban atascados en esquinas o pasillos estrechos al intentar escapar.
- Corregido el registro duplicado de datos sincronizados por red.

### Optimizado
- **Rendimiento de la IA**: Reducción del uso de CPU de `VisitorEntity` del 40% a aproximadamente el 5% mediante la limitación de chequeos frecuentes (intervalos de 5 segundos).
- **IA Consciente del Lag**: Los objetivos de movimiento ahora adaptan su frecuencia de búsqueda de rutas según el MSPT actual del servidor.
- **Gestión de Memoria**: Almacenamiento en caché de los límites (AABB) del área y de las búsquedas de datos guardados dentro del tick de la entidad.

## [1.1.0] - 2026-01-15
- Lanzamiento inicial estable.
- Añadido el sistema de fiestas de cumpleaños.
- Añadidas las variantes de visitantes bebé.