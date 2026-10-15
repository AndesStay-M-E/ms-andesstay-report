package cl.duoc.andesstay.report.voucher;

import cl.duoc.andesstay.report.exception.NonRecoverableMessageException;
import cl.duoc.andesstay.report.messaging.model.MessageEnvelope;
import cl.duoc.andesstay.report.messaging.model.VoucherCommandPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Genera el voucher (documento) de una reserva confirmada.
 * La generación del archivo queda simulada con un código y un log.
 */
@Service
public class VoucherService {

    private static final Logger log =
            LoggerFactory.getLogger(VoucherService.class);

    private static final String SUPPORTED_TYPE = "RESERVATION_VOUCHER";

    public String process(MessageEnvelope<VoucherCommandPayload> envelope) {
        VoucherCommandPayload payload = envelope.requirePayload();

        if (payload.reservationId() == null || payload.reservationId() <= 0) {
            throw new NonRecoverableMessageException(
                    "El voucher no indica una reserva válida"
            );
        }

        if (payload.recipient() == null || payload.recipient().isBlank()) {
            throw new NonRecoverableMessageException(
                    "El voucher no indica el destinatario"
            );
        }

        if (!SUPPORTED_TYPE.equals(payload.documentType())) {
            throw new NonRecoverableMessageException(
                    "Tipo de documento no soportado: " + payload.documentType()
            );
        }

        String voucherCode = "VCH-%06d-%s".formatted(
                payload.reservationId(),
                LocalDate.now().toString().replace("-", "")
        );

        log.info(
                "Voucher generado codigo={} reserva={} destinatario={} traceId={}",
                voucherCode,
                payload.reservationId(),
                payload.recipient(),
                envelope.traceId()
        );

        return voucherCode;
    }
}
