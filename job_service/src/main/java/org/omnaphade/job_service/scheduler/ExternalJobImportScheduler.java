package org.omnaphade.job_service.scheduler;

import lombok.RequiredArgsConstructor;
import org.omnaphade.job_service.service.IExternalJobImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExternalJobImportScheduler {

    private static final Logger log = LoggerFactory.getLogger(ExternalJobImportScheduler.class);

    private final IExternalJobImportService importService;

    @Scheduled(fixedDelayString = "${adzuna.import-interval-ms:21600000}")
    public void runScheduledImport() {
        log.info("Starting scheduled external job import");
        importService.importAll();
    }

}
