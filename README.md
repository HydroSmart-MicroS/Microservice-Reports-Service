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

El Dockerfile compila y ejecuta sus pruebas. Iniciar desde la raíz de orquestación con .env privado.

[Funcionalidad y arranque](../README.md) · [Guía RabbitMQ](../docs/rabbitmq-guide.md) · [Contratos](../docs/contracts.md).
