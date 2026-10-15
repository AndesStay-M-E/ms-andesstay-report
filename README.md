# ms-andesstay-report

Microservicio de reportes: consume comandos de generación de vouchers.

| Elemento | Valor |
|---|---|
| Puerto | 8084 |
| Exchange origen | `andesstay.commands` (direct) |
| Routing key | `voucher.gen` |
| Cola | `report.voucher.q` |
| DLQ | `report.voucher.dlq` (vía `andesstay.dlx`) |

## Manejo de mensajes

- ACK manual (`acknowledge-mode: manual`).
- Error no recuperable (datos inválidos): NACK sin reencolar, el mensaje va a la DLQ y se registra en el log.
- Error recuperable: se reintenta hasta `andesstay.messaging.retry.max-attempts` (3) y luego va a la DLQ.
- JSON inválido: el contenedor lo rechaza sin reencolar (`default-requeue-rejected: false`) y va a la DLQ.

## Base de datos

- Perfil `local` (por defecto): H2 en `./data/report`.
- Perfil `cloud`: AWS RDS por variables de entorno `DB_URL`, `DB_USERNAME` y `DB_PASSWORD`.

Tabla: `vouchers`.

## API REST (JWT de Azure AD, rol ADMIN)

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/reports/summary` | Totales, emitidos hoy, clientes distintos y vouchers de los últimos 7 días |
| GET | `/api/reports/vouchers?reservationId=12` | Listar vouchers |
| GET | `/api/reports/vouchers/{code}` | Detalle (404 si no existe) |

Cada comando `voucher.gen` recibido por RabbitMQ guarda un voucher con código `VCH-<reserva>-<fecha>-<id>`. Un mensaje repetido (mismo `eventId`) no genera un segundo voucher.

## Ejecutar

1. Levantar RabbitMQ con el repositorio `infra-andesstay-messaging` (`docker compose up -d`).
2. `./mvnw spring-boot:run` (Windows: `mvnw.cmd spring-boot:run`).

## Pruebas

`./mvnw test`
