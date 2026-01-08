package com.trading.scheduler;

import com.trading.service.SettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for running daily settlement jobs
 * The schedule can be configured via application.yml using:
 * trading.settlement.cron property (default: "0 0 0 * * ?")
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SettlementScheduler {

    private final SettlementService settlementService;

    /**
     * Run daily settlement job
     * Default schedule: every day at midnight (00:00)
     * Can be configured via trading.settlement.cron property
     */
    @Scheduled(cron = "${trading.settlement.cron:0 0 0 * * ?}")
    public void runDailySettlement() {
        log.info("Starting scheduled daily settlement job");
        try {
            settlementService.runDailySettlement();
            log.info("Scheduled daily settlement job completed successfully");
        } catch (Exception e) {
            log.error("Scheduled daily settlement job failed: {}", e.getMessage(), e);
        }
    }
}
