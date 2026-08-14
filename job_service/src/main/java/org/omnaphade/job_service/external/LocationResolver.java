package org.omnaphade.job_service.external;

/**
 * Falls back from a provider's explicit location string to a generic label derived from its remote flag.
 * Several providers leave location blank for fully-remote roles instead of writing "Remote" into it.
 */
public final class LocationResolver {

    private LocationResolver() {
    }

    public static String resolve(String explicitLocation, boolean remote) {
        if (explicitLocation != null && !explicitLocation.isBlank()) {
            return explicitLocation;
        }
        return remote ? "Remote" : "Not specified";
    }

}
