package com.careerscout.career.service;

import com.careerscout.common.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

@Component
public class CareerUrlValidator {
    public String normalize(String value) {
        final URI uri;
        try {
            uri = new URI(value.trim()).normalize();
        } catch (URISyntaxException exception) {
            throw new BadRequestException("Career URL is not a valid URL");
        }

        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            throw new BadRequestException("Career URL must use HTTP or HTTPS");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank() || uri.getUserInfo() != null) {
            throw new BadRequestException("Career URL must contain a valid public host and no embedded credentials");
        }
        String hostForValidation = host.toLowerCase(Locale.ROOT);
        while (hostForValidation.endsWith(".")) {
            hostForValidation = hostForValidation.substring(0, hostForValidation.length() - 1);
        }
        if (hostForValidation.equals("localhost") || hostForValidation.endsWith(".localhost")
                || hostForValidation.endsWith(".local")
                || hostForValidation.equals("metadata.google.internal")) {
            throw new BadRequestException("Career URL must point to a public internet host");
        }
        rejectPrivateAddress(hostForValidation);

        int port = uri.getPort();
        if (port < -1 || port > 65535 || port == 0) {
            throw new BadRequestException("Career URL port is invalid");
        }
        String normalizedScheme = scheme.toLowerCase(Locale.ROOT);
        String normalizedHost = hostForValidation;
        int normalizedPort = (normalizedScheme.equals("http") && port == 80)
                || (normalizedScheme.equals("https") && port == 443) ? -1 : port;
        String path = uri.getRawPath();
        if (path == null || path.isBlank()) path = "/";
        if (path.length() > 1 && path.endsWith("/")) path = path.substring(0, path.length() - 1);

        try {
            return new URI(normalizedScheme, null, normalizedHost, normalizedPort,
                    path, uri.getQuery(), null).toASCIIString();
        } catch (URISyntaxException exception) {
            throw new BadRequestException("Career URL is not a valid URL");
        }
    }

    private static void rejectPrivateAddress(String host) {
        if (!host.matches("[0-9.]+") && !host.contains(":")) return;
        try {
            InetAddress address = InetAddress.getByName(host);
            if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress() || address.isMulticastAddress()) {
                throw new BadRequestException("Career URL must point to a public internet host");
            }
        } catch (java.net.UnknownHostException exception) {
            throw new BadRequestException("Career URL host is invalid");
        }
    }
}
