package com.openroof.openroof.scheduler;

import com.openroof.openroof.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionScheduler {

    private final SubscriptionService subscriptionService;

    @Scheduled(cron = "0 0 2 * * *", zone = "UTC")
    public void expireSubscriptions() {
        log.info("Iniciando expiración de suscripciones vencidas...");
        int expired = subscriptionService.deactivateExpired();
        log.info("Expiración finalizada. Suscripciones marcadas como EXPIRED: {}", expired);
    }
}
