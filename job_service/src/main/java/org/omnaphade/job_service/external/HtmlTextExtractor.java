package org.omnaphade.job_service.external;

import org.jsoup.Jsoup;

/**
 * Strips HTML markup down to readable plain text. Several providers (Remotive, Arbeitnow, Himalayas,
 * Freehire, Adzuna, AI Dev Jobs, Jobicy) return {@code description}/{@code excerpt} fields as HTML
 * fragments — sometimes with embedded export artifacts like Remotive's Notion-exported
 * {@code <!-- notionvc: ... -->} comments. Without this, tags and comments render as literal visible text,
 * since the frontend only ever renders {@code description} as plain, escaped text (never
 * {@code dangerouslySetInnerHTML}).
 */
public final class HtmlTextExtractor {

    private HtmlTextExtractor() {
    }

    public static String toPlainText(String html) {
        if (html == null || html.isBlank()) {
            return html;
        }
        return Jsoup.parse(html).text();
    }

}
