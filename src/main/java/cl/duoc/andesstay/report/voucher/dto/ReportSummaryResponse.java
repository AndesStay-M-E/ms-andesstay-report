package cl.duoc.andesstay.report.voucher.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Resumen de reportería de vouchers.
 *
 * lastSevenDays trae siempre 7 entradas (de la más antigua a hoy),
 * incluso los días sin vouchers (count = 0).
 */
public record ReportSummaryResponse(
        long totalVouchers,
        long vouchersToday,
        long distinctRecipients,
        LocalDateTime lastVoucherAt,
        List<DailyCount> lastSevenDays
) {

    public record DailyCount(
            LocalDate date,
            long count
    ) {
    }
}
