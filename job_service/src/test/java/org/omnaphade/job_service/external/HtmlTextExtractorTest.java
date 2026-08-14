package org.omnaphade.job_service.external;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlTextExtractorTest {

    @Test
    void stripsTagsAndKeepsReadableText() {
        String html = "<p>Are you a talented <strong>Senior DevOps</strong>?</p><ul><li>4+ years experience</li></ul>";

        assertThat(HtmlTextExtractor.toPlainText(html))
                .isEqualTo("Are you a talented Senior DevOps? 4+ years experience");
    }

    @Test
    void dropsHtmlComments() {
        String html = "<p>About the role</p><!-- notionvc: 9b231391-b97c-4ae2-b256-ce17bc98e9a7 -->"
                + "<p>More details</p>";

        assertThat(HtmlTextExtractor.toPlainText(html)).isEqualTo("About the role More details");
    }

    @Test
    void plainTextWithoutMarkupPassesThroughUnchanged() {
        assertThat(HtmlTextExtractor.toPlainText("Just a plain description, no markup here."))
                .isEqualTo("Just a plain description, no markup here.");
    }

    @Test
    void nullAndBlankPassThroughUnchanged() {
        assertThat(HtmlTextExtractor.toPlainText(null)).isNull();
        assertThat(HtmlTextExtractor.toPlainText("")).isEmpty();
    }

}
