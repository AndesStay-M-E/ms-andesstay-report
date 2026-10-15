package cl.duoc.andesstay.report.messaging.model;

public record VoucherCommandPayload(
        Long reservationId,
        String recipient,
        String documentType
) {
}
