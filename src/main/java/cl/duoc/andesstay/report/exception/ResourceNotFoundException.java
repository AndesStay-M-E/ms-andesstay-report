package cl.duoc.andesstay.report.exception;

/** El recurso pedido no existe (HTTP 404). */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
