package com.openroof.openroof.scheduler;

import com.openroof.openroof.service.PaymentGatewayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Red de seguridad de la spec Bancard: pagos PENDING con checkout iniciado hace
 * más de 10 minutos sin confirmación → get_single_buy_confirmation; si la
 * pasarela no la registra → single_buy_rollback y el pago queda REJECTED.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentReconciliationScheduler {

    private final PaymentGatewayService paymentGatewayService;

    @Scheduled(cron = "${payments.reconciliation.cron:0 */5 * * * *}")
    @SchedulerLock(name = "reconcilePendingGatewayPayments", lockAtMostFor = "9m", lockAtLeastFor = "30s")
    public void reconcile() {
        paymentGatewayService.reconcilePendingPayments();
    }
}
