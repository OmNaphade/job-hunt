package org.omnaphade.job_service.service;

/** Outcome of one external-job import run, across all configured providers. */
public record ImportSummary(int fetched, int created, int updated, int skipped) {
}
