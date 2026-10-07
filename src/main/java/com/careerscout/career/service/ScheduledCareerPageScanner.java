package com.careerscout.career.service;

import com.careerscout.career.repository.CareerSourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledCareerPageScanner {
    private static final Logger logger = LoggerFactory.getLogger(ScheduledCareerPageScanner.class);

    private final CareerSourceRepository careerSources;
    private final CareerPageScanService scanService;

    public ScheduledCareerPageScanner(CareerSourceRepository careerSources,
                                      CareerPageScanService scanService) {
        this.careerSources = careerSources;
        this.scanService = scanService;
    }

    @Scheduled(
            initialDelayString = "${app.crawler.scheduler.initial-delay:PT30S}",
            fixedDelayString = "${app.crawler.scheduler.poll-interval:PT1M}")
    public void scanDueSources() {
        for (var source : careerSources.findAllByActiveTrueOrderByIdAsc()) {
            try {
                scanService.scanScheduled(source.getId());
            } catch (RuntimeException exception) {
                logger.error("Scheduled career-page scan failed for source {}", source.getId(), exception);
            }
        }
    }
}
