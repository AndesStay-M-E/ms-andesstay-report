package cl.duoc.andesstay.report.exception;

/**
 * Error permanente (datos inválidos). Reintentar no sirve:
 * el mensaje se envía directo a la DLQ.
 */
public class NonRecoverableMessageException extends RuntimeException {

    public NonRecoverableMessageException(String message) {
        super(message);
    }
}
