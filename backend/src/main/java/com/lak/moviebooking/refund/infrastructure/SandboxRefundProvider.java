package com.lak.moviebooking.refund.infrastructure;

import com.lak.moviebooking.refund.application.RefundProvider;
import com.lak.moviebooking.refund.application.RefundProviderException;
import com.lak.moviebooking.refund.application.RefundProviderRequest;
import com.lak.moviebooking.refund.application.RefundProviderResult;
import org.springframework.stereotype.Component;

/** Local provider adapter; a stable reference makes retried requests safe. */
@Component
class SandboxRefundProvider implements RefundProvider {

    @Override
    public String name() {
        return "sandbox";
    }

    @Override
    public RefundProviderResult refund(RefundProviderRequest request) throws RefundProviderException {
        if (request.amount() <= 0 || !"VND".equals(request.currency())) {
            throw RefundProviderException.permanent("REFUND_REQUEST_INVALID");
        }
        return new RefundProviderResult("SBR-" + request.refundId().toString().replace("-", "").toUpperCase(java.util.Locale.ROOT));
    }
}
