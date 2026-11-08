# Reports

Trabajos Excel, PDF y CSV por cola, generación y almacenamiento de archivos con descarga autorizada.

Excel (.xlsx) y PDF tienen resumen, identidad visual HydroSmart y detalle legible.
Excel conserva fechas/números tipados, filtros y totales; PDF añade gráfico y paginación.
CSV conserva datos simples y declara `sep=;` para abrir sus columnas en Excel.
La migración V5 amplía el formato a XLSX sin borrar reportes existentes; corrige
también el tipo MIME guardado de los documentos anteriores. La API omitiendo
formato mantiene CSV; la web selecciona Excel. Los archivos ya generados no se reescriben.
El XLSX usa ZIP/XML estándar de Java; no se añaden dependencias ni servicios externos.

DDD: application/internal, domain/model y domain/services, infrastructure, interfaces/acl e interfaces/rest.
La base es exclusiva de este contexto. Consultas de configuración y colaboración entre servicios usan RabbitMQ RPC;
los cambios pasan por outbox y los consumidores confirman después de procesar.

## Operación en MAIN

En la red de Compose, el servicio escucha en `reports:8080` y utiliza la base `hs_reports`.

| Comprobación | Ruta interna |
|---|---|
| Proceso | `GET /actuator/health/liveness` |
| Disponibilidad | `GET /actuator/health/readiness` |

Readiness verifica PostgreSQL y RabbitMQ. Revisa el resultado en el [panel de MAIN](http://127.0.0.1:8099/). Si una solicitud queda pendiente, revisa el consumidor de reportes. El estado del trabajo se guarda en report_jobs y el archivo terminado en report_artifacts.

Construye y ejecuta las pruebas del Dockerfile desde este clon con `docker build -t hydrosmart-reports .`. Para la prueba conjunta, ejecuta `./scripts/Test-Local.ps1 -DatabaseMode Hosted` desde la carpeta de orquestación.
