package org.omnaphade.application_service.entities;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum ApplicationStatus {
    APPLIED,
    SHORTLISTED,
    HIRED,
    REJECTED,
    WITHDRAWN;

    private Set<ApplicationStatus> allowedTransitions = Collections.emptySet();

    static {
        APPLIED.allowedTransitions = EnumSet.of(SHORTLISTED, REJECTED);
        SHORTLISTED.allowedTransitions = EnumSet.of(HIRED, REJECTED);
        HIRED.allowedTransitions = EnumSet.noneOf(ApplicationStatus.class);
        REJECTED.allowedTransitions = EnumSet.noneOf(ApplicationStatus.class);
        WITHDRAWN.allowedTransitions = EnumSet.noneOf(ApplicationStatus.class);
    }

    public boolean canTransitionTo(ApplicationStatus next) {
        return this.allowedTransitions.contains(next);
    }

    public Set<ApplicationStatus> getAllowedTransitions() {
        return Collections.unmodifiableSet(allowedTransitions);
    }
}