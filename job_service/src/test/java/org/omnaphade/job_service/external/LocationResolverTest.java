package org.omnaphade.job_service.external;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LocationResolverTest {

    @Test
    void prefersExplicitLocation() {
        assertThat(LocationResolver.resolve("Berlin, Germany", true)).isEqualTo("Berlin, Germany");
        assertThat(LocationResolver.resolve("Berlin, Germany", false)).isEqualTo("Berlin, Germany");
    }

    @Test
    void fallsBackToRemoteFlagWhenLocationBlank() {
        assertThat(LocationResolver.resolve(null, true)).isEqualTo("Remote");
        assertThat(LocationResolver.resolve("", true)).isEqualTo("Remote");
        assertThat(LocationResolver.resolve("   ", true)).isEqualTo("Remote");
    }

    @Test
    void fallsBackToNotSpecifiedWhenLocationBlankAndNotRemote() {
        assertThat(LocationResolver.resolve(null, false)).isEqualTo("Not specified");
        assertThat(LocationResolver.resolve("", false)).isEqualTo("Not specified");
    }

}
