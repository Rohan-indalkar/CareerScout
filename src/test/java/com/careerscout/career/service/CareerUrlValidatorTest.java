package com.careerscout.career.service;

import com.careerscout.common.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class CareerUrlValidatorTest {
    private final CareerUrlValidator validator = new CareerUrlValidator();

    @Test
    void rejectsPrivateNetworkDestinationsAtScanTime() {
        assertThrows(BadRequestException.class,
                () -> validator.validatePublicDestination("http://127.0.0.1:8080/jobs"));
        assertThrows(BadRequestException.class,
                () -> validator.validatePublicDestination("http://192.168.1.10/jobs"));
        assertThrows(BadRequestException.class,
                () -> validator.validatePublicDestination("http://localhost/jobs"));
        assertThrows(BadRequestException.class,
                () -> validator.validatePublicDestination("http://[::1]/jobs"));
    }
}
