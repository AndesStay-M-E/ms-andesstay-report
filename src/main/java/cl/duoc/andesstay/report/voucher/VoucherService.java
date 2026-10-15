package cl.duoc.andesstay.report.voucher;

import cl.duoc.andesstay.report.exception.NonRecoverableMessageException;
import cl.duoc.andesstay.report.exception.ResourceNotFoundException;
import cl.duoc.andesstay.report.messaging.model.MessageEnvelope;
import cl.duoc.andesstay.report.messaging.model.VoucherCommandPayload;
import cl.duoc.andesstay.report.voucher.dto.ReportSummaryResponse;
import cl.duoc.andesstay.report.voucher.dto.VoucherResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Vouchers de reservas confirmadas y su reportería.
 *
 * process() se invoca desde el consumidor RabbitMQ; el resto, desde la API REST.
 */
@Service
@Transactional
public class VoucherService {

    private static final Logger log =
            LoggerFactory.getLogger(VoucherService.class);

    private static final String SUPPORTED_TYPE = "RESERVATION_VOUCHER";

    private static final DateTimeFormatter CODE_DATE =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final int SUMMARY_DAYS = 7;

    private final VoucherRepository repository;
    private final Clock clock;

    @Autowired
    public VoucherService(VoucherRepository repository) {
        this(repository, Clock.systemDefaultZone());
    }

    VoucherService(VoucherRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /**
     * Genera y guarda el voucher. Si el mensaje ya fue procesado
     * (mismo eventId), devuelve el voucher existente.
     *
     * Errores de datos -> NonRecoverableMessageException (va a la DLQ).
     * Errores de base de datos -> se propagan y el consumidor reintenta.
     */
    public VoucherResponse process(MessageEnvelope<VoucherCommandPayload> envelope) {
        VoucherCommandPayload payload = envelope.requirePayload();
        validate(envelope, payload);

        Optional<Voucher> existing = repository.findByEventId(envelope.eventId());
        if (existing.isPresent()) {
            log.warn("Mensaje {} ya procesado: voucher {} existente", envelope.reference(), existing.get().getCode());
            return VoucherResponse.from(existing.get());
        }

        LocalDateTime now = LocalDateTime.now(clock);

        Voucher voucher = new Voucher();
        voucher.setEventId(envelope.eventId());
        voucher.setReservationId(payload.reservationId());
        voucher.setRecipient(payload.recipient().trim());
        voucher.setDocumentType(payload.documentType());
        voucher.setTraceId(envelope.traceId());
        voucher.setGeneratedAt(now);
        voucher.setCode(buildCode(payload.reservationId(), now, envelope));

        Voucher saved = repository.save(voucher);

        log.info(
                "Voucher generado codigo={} reserva={} destinatario={} traceId={}",
                saved.getCode(),
                saved.getReservationId(),
                saved.getRecipient(),
                envelope.traceId()
        );

        return VoucherResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<VoucherResponse> findAll(Long reservationId) {
        List<Voucher> vouchers = reservationId == null
                ? repository.findAllByOrderByGeneratedAtDesc()
                : repository.findByReservationIdOrderByGeneratedAtDesc(reservationId);

        return vouchers.stream().map(VoucherResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public VoucherResponse findByCode(String code) {
        return repository.findByCode(code.toUpperCase(Locale.ROOT))
                .map(VoucherResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el voucher " + code));
    }

    @Transactional(readOnly = true)
    public ReportSummaryResponse summary() {
        LocalDate today = LocalDate.now(clock);
        LocalDate firstDay = today.minusDays(SUMMARY_DAYS - 1L);

        List<Voucher> all = repository.findAll();

        Map<LocalDate, Long> perDay = repository
                .findByGeneratedAtGreaterThanEqual(firstDay.atStartOfDay())
                .stream()
                .collect(Collectors.groupingBy(v -> v.getGeneratedAt().toLocalDate(), Collectors.counting()));

        List<ReportSummaryResponse.DailyCount> lastSevenDays = new ArrayList<>();
        for (LocalDate day = firstDay; !day.isAfter(today); day = day.plusDays(1)) {
            lastSevenDays.add(new ReportSummaryResponse.DailyCount(day, perDay.getOrDefault(day, 0L)));
        }

        LocalDateTime lastVoucherAt = all.stream()
                .map(Voucher::getGeneratedAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        long distinctRecipients = all.stream()
                .map(v -> v.getRecipient().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet())
                .size();

        return new ReportSummaryResponse(
                all.size(),
                perDay.getOrDefault(today, 0L),
                distinctRecipients,
                lastVoucherAt,
                lastSevenDays
        );
    }

    /** Ejemplo: VCH-000012-20261015-3FA8 (los 4 últimos caracteres vienen del eventId). */
    private String buildCode(Long reservationId, LocalDateTime when, MessageEnvelope<?> envelope) {
        String suffix = envelope.eventId().toString().replace("-", "")
                .substring(0, 4)
                .toUpperCase(Locale.ROOT);

        return "VCH-%06d-%s-%s".formatted(reservationId, when.format(CODE_DATE), suffix);
    }

    private void validate(MessageEnvelope<VoucherCommandPayload> envelope, VoucherCommandPayload payload) {
        if (envelope.eventId() == null) {
            throw new NonRecoverableMessageException("El mensaje no trae eventId");
        }

        if (payload.reservationId() == null || payload.reservationId() <= 0) {
            throw new NonRecoverableMessageException("El voucher no indica una reserva válida");
        }

        if (payload.recipient() == null || payload.recipient().isBlank()) {
            throw new NonRecoverableMessageException("El voucher no indica el destinatario");
        }

        if (!SUPPORTED_TYPE.equals(payload.documentType())) {
            throw new NonRecoverableMessageException("Tipo de documento no soportado: " + payload.documentType());
        }
    }
}
