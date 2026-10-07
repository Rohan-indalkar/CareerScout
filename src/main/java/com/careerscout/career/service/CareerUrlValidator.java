package com.careerscout.career.service;

import com.careerscout.common.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
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
        String hostForValidation = unbracket(host).toLowerCase(Locale.ROOT);
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

    public void validatePublicDestination(String value) {
        final URI uri;
        try {
            uri = new URI(value);
        } catch (URISyntaxException exception) {
            throw new BadRequestException("Career URL is not a valid URL");
        }
        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))
                || host == null || host.isBlank() || uri.getUserInfo() != null) {
            throw new BadRequestException("Career URL must point to a public HTTP or HTTPS destination");
        }
        String normalizedHost = unbracket(host).toLowerCase(Locale.ROOT);
        while (normalizedHost.endsWith(".")) {
            normalizedHost = normalizedHost.substring(0, normalizedHost.length() - 1);
        }
        if (normalizedHost.equals("localhost") || normalizedHost.endsWith(".localhost")
                || normalizedHost.endsWith(".local") || normalizedHost.equals("metadata.google.internal")) {
            throw new BadRequestException("Career URL must point to a public internet host");
        }
        try {
            InetAddress[] addresses = InetAddress.getAllByName(normalizedHost);
            if (addresses.length == 0) {
                throw new BadRequestException("Career URL host did not resolve to a public address");
            }
            for (InetAddress address : addresses) {
                if (!isPublicAddress(address)) {
                    throw new BadRequestException("Career URL must point only to public internet addresses");
                }
            }
        } catch (UnknownHostException exception) {
            throw new BadRequestException("Career URL host could not be resolved");
        }
    }

    private static void rejectPrivateAddress(String host) {
        if (!host.matches("[0-9.]+") && !host.contains(":")) return;
        try {
            InetAddress address = InetAddress.getByName(host);
            if (!isPublicAddress(address)) {
                throw new BadRequestException("Career URL must point to a public internet host");
            }
        } catch (java.net.UnknownHostException exception) {
            throw new BadRequestException("Career URL host is invalid");
        }
    }

    private static String unbracket(String host) {
        if (host.startsWith("[") && host.endsWith("]")) {
            return host.substring(1, host.length() - 1);
        }
        return host;
    }

    private static boolean isPublicAddress(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()) {
            return false;
        }
        byte[] bytes = address.getAddress();
        if (bytes.length == 4) {
            int first = Byte.toUnsignedInt(bytes[0]);
            int second = Byte.toUnsignedInt(bytes[1]);
            return first != 0 && first != 10 && first != 127 && first < 224
                    && !(first == 100 && second >= 64 && second <= 127)
                    && !(first == 169 && second == 254)
                    && !(first == 172 && second >= 16 && second <= 31)
                    && !(first == 192 && (second == 0 || second == 168))
                    && !(first == 198 && (second == 18 || second == 19 || second == 51))
                    && !(first == 203 && second == 0);
        }
        return (Byte.toUnsignedInt(bytes[0]) & 0xE0) == 0x20
                && !(Byte.toUnsignedInt(bytes[0]) == 0x20
                && Byte.toUnsignedInt(bytes[1]) == 0x01
                && Byte.toUnsignedInt(bytes[2]) == 0x0D
                && Byte.toUnsignedInt(bytes[3]) == 0xB8);
    }
}
