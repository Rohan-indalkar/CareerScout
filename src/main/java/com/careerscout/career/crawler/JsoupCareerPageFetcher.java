package com.careerscout.career.crawler;

import com.careerscout.career.service.CareerUrlValidator;
import com.careerscout.common.exception.UpstreamServiceException;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JsoupCareerPageFetcher implements CareerPageFetcher {
    private static final int REQUEST_TIMEOUT_MILLIS = 8_000;
    private static final int MAX_RESPONSE_BYTES = 2_000_000;

    private final CareerUrlValidator urlValidator;

    public JsoupCareerPageFetcher(CareerUrlValidator urlValidator) {
        this.urlValidator = urlValidator;
    }

    @Override
    public String fetch(String url) {
        urlValidator.validatePublicDestination(url);
        try {
            Connection.Response response = Jsoup.connect(url)
                    .userAgent("CareerScoutBot/1.0 (+career-page job monitoring)")
                    .timeout(REQUEST_TIMEOUT_MILLIS)
                    .maxBodySize(MAX_RESPONSE_BYTES)
                    .followRedirects(false)
                    .ignoreHttpErrors(true)
                    .execute();
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new UpstreamServiceException(
                        "Career page returned HTTP " + response.statusCode() + "; redirects are not followed");
            }
            String contentType = response.contentType();
            if (contentType != null && !contentType.toLowerCase(java.util.Locale.ROOT).contains("html")) {
                throw new UpstreamServiceException("Career page did not return HTML content");
            }
            return response.body();
        } catch (IOException exception) {
            throw new UpstreamServiceException("Career page could not be fetched", exception);
        }
    }
}
