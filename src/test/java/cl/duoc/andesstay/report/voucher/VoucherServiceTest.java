package cl.duoc.andesstay.report.voucher;

import cl.duoc.andesstay.report.exception.NonRecoverableMessageException;
import cl.duoc.andesstay.report.messaging.model.MessageEnvelope;
import cl.duoc.andesstay.report.messaging.model.VoucherCommandPayload;
import cl.duoc.andesstay.report.voucher.dto.ReportSummaryResponse;
import cl.duoc.andesstay.report.voucher.dto.VoucherResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VoucherServiceTest {

    private static final ZoneId ZONE = ZoneId.of("America/Santiago");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 15);

    private VoucherRepository repository;
    private VoucherService service;

    @BeforeEach
    void setUp() {
        repository = mock(VoucherRepository.class);
        when(repository.save(any(Voucher.class))).thenAnswer(call -> call.getArgument(0));

        Clock clock = Clock.fixed(TODAY.atTime(12, 0).atZone(ZONE).toInstant(), ZONE);
        service = new VoucherService(repository, clock);
    }

    @Test
    void generaYGuardaElVoucher() {
        UUID eventId = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
        when(repository.findByEventId(eventId)).thenReturn(Optional.empty());

        VoucherResponse response = service.process(envelope(eventId, "RESERVATION_VOUCHER"));

        assertThat(response.code()).isEqualTo("VCH-000012-20261015-3FA8");
        assertThat(response.recipient()).isEqualTo("cliente@andesstay.cl");
        verify(repository).save(any(Voucher.class));
    }

    @Test
    void noDuplicaUnMensajeYaProcesado() {
        UUID eventId = UUID.randomUUID();
        Voucher existing = voucher("VCH-000012-20261015-AAAA", TODAY.atTime(9, 0));
        when(repository.findByEventId(eventId)).thenReturn(Optional.of(existing));

        VoucherResponse response = service.process(envelope(eventId, "RESERVATION_VOUCHER"));

        assertThat(response.code()).isEqualTo("VCH-000012-20261015-AAAA");
        verify(repository, never()).save(any());
    }

    @Test
    void tipoDeDocumentoDesconocidoEsNoRecuperable() {
        assertThatThrownBy(() -> service.process(envelope(UUID.randomUUID(), "FACTURA")))
                .isInstanceOf(NonRecoverableMessageException.class);
    }

    @Test
    void resumenIncluyeLosSieteDiasAunqueNoHayaVouchers() {
        Voucher today = voucher("A", TODAY.atTime(10, 0));
        Voucher twoDaysAgo = voucher("B", TODAY.minusDays(2).atTime(18, 0));
        when(repository.findAll()).thenReturn(List.of(today, twoDaysAgo));
        when(repository.findByGeneratedAtGreaterThanEqual(TODAY.minusDays(6).atStartOfDay()))
                .thenReturn(List.of(today, twoDaysAgo));

        ReportSummaryResponse summary = service.summary();

        assertThat(summary.totalVouchers()).isEqualTo(2);
        assertThat(summary.vouchersToday()).isEqualTo(1);
        assertThat(summary.distinctRecipients()).isEqualTo(1);
        assertThat(summary.lastSevenDays()).hasSize(7);
        assertThat(summary.lastSevenDays().get(6).date()).isEqualTo(TODAY);
        assertThat(summary.lastSevenDays().get(4).count()).isEqualTo(1);
        assertThat(summary.lastSevenDays().get(5).count()).isZero();
    }

    private MessageEnvelope<VoucherCommandPayload> envelope(UUID eventId, String type) {
        return new MessageEnvelope<>(
                "voucher.gen", eventId, Instant.now(), "trace-1", "reservation-12",
                new VoucherCommandPayload(12L, "cliente@andesstay.cl", type)
        );
    }

    private Voucher voucher(String code, LocalDateTime generatedAt) {
        Voucher voucher = new Voucher();
        voucher.setCode(code);
        voucher.setReservationId(12L);
        voucher.setRecipient("cliente@andesstay.cl");
        voucher.setDocumentType("RESERVATION_VOUCHER");
        voucher.setGeneratedAt(generatedAt);
        return voucher;
    }
}
