# Migración de récords v1 a historial v2

- Archivo principal en Windows: `%LOCALAPPDATA%\MemoryDesktop\records.json`.
- Primera escritura sobre un archivo v1: se crea `records.json.v1.bak` en la misma carpeta, sin sobrescribir una copia anterior.
- La lectura de un archivo v1 **no lo modifica**. El cambio a v2 sucede al guardar una partida o borrar marcas.
- V2 contiene `version: 2`, `legacyDifficulties` (lista de dificultades importadas desde v1), `records` (top 10 por dificultad) e `history` (todas las partidas conocidas).
- Los récords v1 se importan tanto al top como al historial. Las partidas antiguas que nunca entraron al top no se pueden reconstruir; la interfaz lo explica.
- Desde v2, **toda partida terminada se guarda**, incluso cuando no entra al ranking.
- «Borrar récords» elimina solo las entradas del top de esa dificultad. «Borrar historial» elimina el historial y el top de esa dificultad.
- El código mantiene la escritura mediante archivo temporal y reemplazo atómico cuando el sistema lo permite.
- No se incorporó límite de historial: en un proyecto futuro, para volúmenes altos de partidas, conviene evaluar SQLite en lugar de reescribir un JSON completo por cada resultado.

## Antes de instalar

Haz una copia del `records.json` existente. La aplicación creará otra copia `.v1.bak` automáticamente en la primera escritura v2.
