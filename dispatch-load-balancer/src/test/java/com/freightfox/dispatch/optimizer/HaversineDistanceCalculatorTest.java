package com.freightfox.dispatch.optimizer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HaversineDistanceCalculatorTest {

    private final HaversineDistanceCalculator calculator = new HaversineDistanceCalculator();

    @Test
    void samePointIsZero() {
        assertEquals(0.0, calculator.distanceKm(28.6139, 77.2090, 28.6139, 77.2090));
    }

    @Test
    void connaughtPlaceToKarolBagh() {
        assertEquals(14.44, calculator.distanceKm(28.6139, 77.2090, 28.7041, 77.1025), 0.05);
    }

    @Test
    void bangaloreToChennai() {
        assertEquals(290.17, calculator.distanceKm(12.9716, 77.5946, 13.0827, 80.2707), 0.5);
    }

    @Test
    void delhiToMumbai() {
        assertEquals(1148.1, calculator.distanceKm(28.6139, 77.2090, 19.0760, 72.8777), 1.0);
    }

    @Test
    void isSymmetric() {
        double ab = calculator.distanceKm(12.9716, 77.5946, 28.6139, 77.2090);
        double ba = calculator.distanceKm(28.6139, 77.2090, 12.9716, 77.5946);
        assertEquals(ab, ba, 1e-9);
    }

    @Test
    void quarterOfEquatorIsAboutTenThousandKm() {
        assertEquals(10007.5, calculator.distanceKm(0, 0, 0, 90), 1.0);
    }

    @Test
    void handlesAntipodalPointsWithoutNaN() {
        double d = calculator.distanceKm(0, 0, 0, 180);
        assertEquals(20015.1, d, 2.0);
    }
}
