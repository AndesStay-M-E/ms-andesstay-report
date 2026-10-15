package cl.duoc.andesstay.report.voucher;

import cl.duoc.andesstay.report.voucher.dto.ReportSummaryResponse;
import cl.duoc.andesstay.report.voucher.dto.VoucherResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * GET /api/reports/summary                       resumen (totales y últimos 7 días)
 * GET /api/reports/vouchers?reservationId=12     listar vouchers
 * GET /api/reports/vouchers/{code}               detalle de un voucher
 *
 * Requieren rol ADMIN.
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final VoucherService service;

    public ReportController(VoucherService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    public ReportSummaryResponse summary() {
        return service.summary();
    }

    @GetMapping("/vouchers")
    public List<VoucherResponse> findAll(
            @RequestParam(required = false) Long reservationId
    ) {
        return service.findAll(reservationId);
    }

    @GetMapping("/vouchers/{code}")
    public VoucherResponse findByCode(
            @PathVariable String code
    ) {
        return service.findByCode(code);
    }
}
