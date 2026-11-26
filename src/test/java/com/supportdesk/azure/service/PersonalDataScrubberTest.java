package com.supportdesk.azure.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** E5-S1: nothing personal leaves the portal, even when agents type it into free text. */
class PersonalDataScrubberTest
{
    @Test
    void cifNationalIdMobileAndNameAreRemoved()
    {
        String text = "Ahmed Ali (CIF-100101, cif 100101) national ID 28803031234569, mobile 01001234567 "
                + "or +201001234567; ahmed ali called twice.";

        String scrubbed = PersonalDataScrubber.scrub(text, "Ahmed Ali");

        assertThat(scrubbed)
                .doesNotContain("100101", "28803031234569", "01001234567", "+201001234567")
                .doesNotContainIgnoringCase("ahmed ali")
                .contains("[CIF removed]", "[national ID removed]", "[mobile removed]", "[customer]", "called twice");
    }

    @Test
    void ordinaryTextAndReferencesStay()
    {
        String text = "Transfer TRX-2026-00981 of 1500 EGP failed at 10:42 with ERR_AUTH_503.";

        assertThat(PersonalDataScrubber.scrub(text, "Sara Mahmoud")).isEqualTo(text);
        assertThat(PersonalDataScrubber.scrub(null, "x")).isNull();
    }
}
