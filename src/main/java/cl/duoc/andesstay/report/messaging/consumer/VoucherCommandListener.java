package cl.duoc.andesstay.report.messaging.consumer;

import cl.duoc.andesstay.report.exception.NonRecoverableMessageException;
import cl.duoc.andesstay.report.exception.RecoverableMessageException;
import cl.duoc.andesstay.report.messaging.model.MessageEnvelope;
import cl.duoc.andesstay.report.messaging.model.VoucherCommandPayload;
import cl.duoc.andesstay.report.messaging.support.MessageAcknowledger;
import cl.duoc.andesstay.report.voucher.VoucherService;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Consumidor de la cola report.voucher.q (routing key voucher.gen).
 *
 * Solo traduce el mensaje y decide ACK / reintento / DLQ.
 * La lógica de negocio está en VoucherService.
 */
@Component
public class VoucherCommandListener {

    private static final Logger log =
            LoggerFactory.getLogger(VoucherCommandListener.class);

    private final VoucherService voucherService;
    private final MessageAcknowledger acknowledger;

    public VoucherCommandListener(
            VoucherService voucherService,
            MessageAcknowledger acknowledger
    ) {
        this.voucherService = voucherService;
        this.acknowledger = acknowledger;
    }

    @RabbitListener(
            id = "report-voucher-listener",
            queues = "${andesstay.messaging.route.queue}"
    )
    public void onVoucherMessage(
            @Payload MessageEnvelope<VoucherCommandPayload> envelope,
            Message message,
            Channel channel
    ) {
        String reference = envelope.reference();

        log.info(
                "Mensaje recibido type={} eventId={} correlationId={} intento={}",
                envelope.type(),
                reference,
                envelope.correlationId(),
                acknowledger.currentAttempt(message) + 1
        );

        try {
            voucherService.process(envelope);
            acknowledger.ack(channel, message, reference);

        } catch (NonRecoverableMessageException exception) {
            acknowledger.deadLetter(channel, message, reference, exception.getMessage());

        } catch (RecoverableMessageException exception) {
            acknowledger.retryOrDeadLetter(channel, message, reference, exception.getMessage());

        } catch (RuntimeException exception) {
            // Error no previsto: se trata como temporal y se reintenta.
            acknowledger.retryOrDeadLetter(
                    channel,
                    message,
                    reference,
                    "Error inesperado: " + exception.getMessage()
            );
        }
    }
}
