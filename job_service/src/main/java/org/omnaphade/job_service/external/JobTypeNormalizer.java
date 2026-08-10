package org.omnaphade.job_service.external;

/**
 * Normalizes a provider's free-text employment-type field into this app's small, fixed
 * {@code Job.jobType} vocabulary (FULL_TIME/PART_TIME/CONTRACT). Every provider spells "contract work"
 * differently (contract, freelance, temporary, contractor, ...), so the match is a case-insensitive
 * substring check rather than an exact-value lookup.
 */
public final class JobTypeNormalizer {

    private JobTypeNormalizer() {
    }

    public static String normalize(String rawEmploymentType) {
        if (rawEmploymentType == null || rawEmploymentType.isBlank()) {
            return "FULL_TIME";
        }
        String normalized = rawEmploymentType.trim().toLowerCase();
        if (normalized.contains("part")) {
            return "PART_TIME";
        }
        if (normalized.contains("contract") || normalized.contains("freelance") || normalized.contains("temporary")) {
            return "CONTRACT";
        }
        return "FULL_TIME";
    }

}
