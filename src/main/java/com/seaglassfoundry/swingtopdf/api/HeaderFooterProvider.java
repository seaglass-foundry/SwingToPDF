package com.seaglassfoundry.swingtopdf.api;

import java.util.Objects;

/**
 * Hook interface for supplying a {@link HeaderFooter} band that can vary per
 * page. Useful when the same band should not appear on every page -- for
 * example, a cover-style header on page 1 and a smaller standard header on the
 * remaining pages, or omitting the page-number footer on page 1.
 *
 * <p>The provider is invoked once per page during rendering. The 1-based
 * {@code page} number and the final {@code pages} count (the total number of
 * pages in the document) are both known at call time. Returning {@code null}
 * for a given page suppresses the band on that page.
 *
 * <p>Token substitution ({@code {page}} and {@code {pages}}) is applied by the
 * rendering pipeline against the band returned by the provider; implementations
 * do not need to resolve tokens themselves.
 *
 * <p>Register a provider via
 * {@link com.seaglassfoundry.swingtopdf.SwingPdfExporter#header(HeaderFooterProvider)}
 * or
 * {@link com.seaglassfoundry.swingtopdf.SwingPdfExporter#footer(HeaderFooterProvider)}.
 *
 * <h2>Example -- cover page on page 1, standard band on the rest</h2>
 * <pre>{@code
 * HeaderFooter cover    = HeaderFooter.of("COVER -- Quarterly Report").fontSize(14f);
 * HeaderFooter standard = HeaderFooter.of("Quarterly Report").align(Alignment.LEFT);
 *
 * exporter.header((page, pages) -> page == 1 ? cover : standard)
 *         .footer((page, pages) -> page == 1 ? null
 *                                             : HeaderFooter.of("Page {page} of {pages}"));
 * }</pre>
 *
 * <p>Implementations should not throw. If a provider needs to react to an
 * unexpected condition, return {@code null} (suppress the band) rather than
 * propagating an exception, since exceptions thrown from the provider surface
 * to the caller of {@link com.seaglassfoundry.swingtopdf.SwingPdfExporter#export}.
 *
 * @since 1.3.0
 */
@FunctionalInterface
public interface HeaderFooterProvider {

    /**
     * Return the band to render on the given page, or {@code null} to omit the
     * band on this page.
     *
     * @param page  the 1-based page number being rendered
     * @param pages the total page count for the document (final at call time)
     * @return the band to render, or {@code null} to suppress the band on this page
     */
    HeaderFooter get(int page, int pages);

    /**
     * Return a provider that yields the same {@code band} on every page. Useful
     * for adapting a constant band to the provider-shaped API, and used
     * internally to back the
     * {@link com.seaglassfoundry.swingtopdf.SwingPdfExporter#header(HeaderFooter)}
     * / {@link com.seaglassfoundry.swingtopdf.SwingPdfExporter#footer(HeaderFooter)}
     * convenience overloads.
     *
     * @param band the band to return for every page; must not be {@code null}
     * @return a provider that always returns {@code band}
     * @throws NullPointerException if {@code band} is {@code null}
     */
    static HeaderFooterProvider of(HeaderFooter band) {
        Objects.requireNonNull(band, "band must not be null");
        return (page, pages) -> band;
    }
}
