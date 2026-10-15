package cl.duoc.andesstay.report.voucher;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {

    Optional<Voucher> findByEventId(UUID eventId);

    Optional<Voucher> findByCode(String code);

    List<Voucher> findAllByOrderByGeneratedAtDesc();

    List<Voucher> findByReservationIdOrderByGeneratedAtDesc(Long reservationId);

    List<Voucher> findByGeneratedAtGreaterThanEqual(LocalDateTime from);
}
