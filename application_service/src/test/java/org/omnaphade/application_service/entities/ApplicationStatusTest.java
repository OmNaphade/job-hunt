package org.omnaphade.application_service.entities;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApplicationStatusTest {

    @Test
    void applied_canOnlyTransitionToShortlistedOrRejected() {
        assertThat(ApplicationStatus.APPLIED.canTransitionTo(ApplicationStatus.SHORTLISTED)).isTrue();
        assertThat(ApplicationStatus.APPLIED.canTransitionTo(ApplicationStatus.REJECTED)).isTrue();
        assertThat(ApplicationStatus.APPLIED.canTransitionTo(ApplicationStatus.HIRED)).isFalse();
        assertThat(ApplicationStatus.APPLIED.canTransitionTo(ApplicationStatus.WITHDRAWN)).isFalse();
        assertThat(ApplicationStatus.APPLIED.canTransitionTo(ApplicationStatus.APPLIED)).isFalse();
    }

    @Test
    void shortlisted_canOnlyTransitionToHiredOrRejected() {
        assertThat(ApplicationStatus.SHORTLISTED.canTransitionTo(ApplicationStatus.HIRED)).isTrue();
        assertThat(ApplicationStatus.SHORTLISTED.canTransitionTo(ApplicationStatus.REJECTED)).isTrue();
        assertThat(ApplicationStatus.SHORTLISTED.canTransitionTo(ApplicationStatus.APPLIED)).isFalse();
        assertThat(ApplicationStatus.SHORTLISTED.canTransitionTo(ApplicationStatus.WITHDRAWN)).isFalse();
    }

    @Test
    void hiredRejectedWithdrawn_areTerminal() {
        for (ApplicationStatus terminal : new ApplicationStatus[]{
                ApplicationStatus.HIRED, ApplicationStatus.REJECTED, ApplicationStatus.WITHDRAWN}) {
            for (ApplicationStatus target : ApplicationStatus.values()) {
                assertThat(terminal.canTransitionTo(target))
                        .as("%s should never transition to %s", terminal, target)
                        .isFalse();
            }
        }
    }

    @Test
    void getAllowedTransitions_isUnmodifiable() {
        var transitions = ApplicationStatus.APPLIED.getAllowedTransitions();
        assertThatThrownBy(() -> transitions.add(ApplicationStatus.HIRED))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
