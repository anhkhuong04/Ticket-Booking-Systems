package com.lak.moviebooking.voucher.infrastructure;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import com.lak.moviebooking.common.application.error.ApplicationException;
import com.lak.moviebooking.voucher.application.AppliedVoucher;
import com.lak.moviebooking.voucher.application.VoucherRedemption;
import com.lak.moviebooking.voucher.application.VoucherRedemptionCommand;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class JdbcVoucherRedemption implements VoucherRedemption {

    private static final String ACTIVE = "ACTIVE";
    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    JdbcVoucherRedemption(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    @Override
    @Transactional
    public AppliedVoucher redeem(VoucherRedemptionCommand command) {
        validate(command);
        Instant now = clock.instant();
        VoucherRow voucher = lockByCode(command.voucherCode());
        requireApplicable(voucher, command, now);
        long discount = discount(voucher, command.subtotal());
        incrementUsage(voucher, now);
        jdbcTemplate.update("""
                INSERT INTO voucher_redemptions (id,voucher_id,booking_id,user_id,discount_amount,redeemed_at)
                VALUES (?,?,?,?,?,?)
                """, UUID.randomUUID(), voucher.id(), command.bookingId(), command.userId(), discount, atUtc(now));
        return new AppliedVoucher(voucher.id(), voucher.code(), discount);
    }

    private VoucherRow lockByCode(String code) {
        return jdbcTemplate.query("""
                SELECT id,code,discount_type,discount_value,max_discount_amount,min_order_amount,usage_limit,usage_count,
                       per_user_limit,starts_at,ends_at,status
                FROM vouchers WHERE code=? FOR UPDATE
                """, this::mapVoucher, code).stream().findFirst()
                .orElseThrow(() -> notApplicable());
    }

    private void requireApplicable(VoucherRow voucher, VoucherRedemptionCommand command, Instant now) {
        if (!ACTIVE.equals(voucher.status())
                || (voucher.startsAt() != null && now.isBefore(voucher.startsAt()))
                || (voucher.endsAt() != null && !now.isBefore(voucher.endsAt()))
                || command.subtotal() < voucher.minOrderAmount()) {
            throw notApplicable();
        }
        if (voucher.usageLimit() != null && voucher.usageCount() >= voucher.usageLimit()) {
            throw ApplicationException.businessRule("VOUCHER_USAGE_LIMIT_REACHED", "Voucher usage limit has been reached");
        }
        if (voucher.perUserLimit() != null && redemptionsByUser(voucher.id(), command.userId()) >= voucher.perUserLimit()) {
            throw ApplicationException.businessRule("VOUCHER_PER_USER_LIMIT_REACHED", "Voucher usage limit has been reached for this user");
        }
    }

    private int redemptionsByUser(UUID voucherId, UUID userId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM voucher_redemptions WHERE voucher_id=? AND user_id=?
                """, Integer.class, voucherId, userId);
        return count == null ? 0 : count;
    }

    private long discount(VoucherRow voucher, long subtotal) {
        try {
            long amount = "PERCENT".equals(voucher.discountType())
                    ? Math.multiplyExact(subtotal, voucher.discountValue()) / 100
                    : voucher.discountValue();
            if (voucher.maxDiscountAmount() != null) {
                amount = Math.min(amount, voucher.maxDiscountAmount());
            }
            return Math.min(amount, subtotal);
        }
        catch (ArithmeticException exception) {
            throw ApplicationException.businessRule("VOUCHER_DISCOUNT_INVALID", "Voucher discount is invalid");
        }
    }

    private void incrementUsage(VoucherRow voucher, Instant now) {
        int updated = jdbcTemplate.update("""
                UPDATE vouchers SET usage_count=usage_count+1,updated_at=?
                WHERE id=? AND usage_count=?
                """, atUtc(now), voucher.id(), voucher.usageCount());
        if (updated != 1) {
            throw ApplicationException.conflict("VOUCHER_USAGE_CONFLICT", "Voucher usage could not be recorded");
        }
    }

    private void validate(VoucherRedemptionCommand command) {
        if (command == null || command.userId() == null || command.bookingId() == null
                || command.voucherCode() == null || command.voucherCode().isBlank() || command.subtotal() <= 0) {
            throw ApplicationException.businessRule("INVALID_VOUCHER_REDEMPTION", "Voucher redemption is invalid");
        }
    }

    private ApplicationException notApplicable() {
        return ApplicationException.businessRule("VOUCHER_NOT_APPLICABLE", "Voucher is not applicable");
    }

    private VoucherRow mapVoucher(java.sql.ResultSet resultSet, int rowNumber) throws java.sql.SQLException {
        return new VoucherRow(resultSet.getObject("id", UUID.class), resultSet.getString("code"),
                resultSet.getString("discount_type"), resultSet.getLong("discount_value"),
                resultSet.getObject("max_discount_amount", Long.class), resultSet.getLong("min_order_amount"),
                resultSet.getObject("usage_limit", Integer.class), resultSet.getInt("usage_count"),
                resultSet.getObject("per_user_limit", Integer.class), nullableInstant(resultSet, "starts_at"),
                nullableInstant(resultSet, "ends_at"), resultSet.getString("status"));
    }

    private Instant nullableInstant(java.sql.ResultSet resultSet, String column) throws java.sql.SQLException {
        OffsetDateTime value = resultSet.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }

    private OffsetDateTime atUtc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }

    private record VoucherRow(
            UUID id, String code, String discountType, long discountValue, Long maxDiscountAmount,
            long minOrderAmount, Integer usageLimit, int usageCount, Integer perUserLimit,
            Instant startsAt, Instant endsAt, String status) {
    }
}
