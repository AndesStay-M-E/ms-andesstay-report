package cl.duoc.andesstay.report.voucher.dto;

import cl.duoc.andesstay.report.voucher.Voucher;

import java.time.LocalDateTime;

public record VoucherResponse(
        Long id,
        String code,
        Long reservationId,
        String recipient,
        String documentType,
        LocalDateTime generatedAt
) {

    public static VoucherResponse from(Voucher voucher) {
        return new VoucherResponse(
                voucher.getId(),
                voucher.getCode(),
                voucher.getReservationId(),
                voucher.getRecipient(),
                voucher.getDocumentType(),
                voucher.getGeneratedAt()
        );
    }
}
