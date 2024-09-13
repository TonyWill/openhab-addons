package org.openhab.binding.mideaac.internal.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
* We need to identify temperature readings that deviate significantly from historical data.
* We want to maintain a consistent temperature trend, avoiding sudden, unexpected spikes or dips.
* A moving average filter is a simple yet effective method for smoothing data and reducing noise.
* It calculates the average of a specific number of data points over a sliding window.
*/

public class TemperatureHandler {
    private final Logger logger;
    private int TEMPERATURE_BUFFER_SIZE = 5;
    private int TEMPERATURE_THRESHOLD = 10;
    private int MAX_BAD_READINGS = 1;

    private float[] temperatureBuffer;
    private int bufferIndex;
    private boolean isBaselineEstablished;
    private float baselineAverage;
    private int readingsCount;
    private int consecutiveBadReadings;

    public TemperatureHandler() {
        this.logger = LoggerFactory.getLogger(TemperatureHandler.class);
        temperatureBuffer = new float[TEMPERATURE_BUFFER_SIZE];
        bufferIndex = 0;
        isBaselineEstablished = false;
        baselineAverage = 0.0f;
        readingsCount = 0;
        this.logger.trace("TemperatureHandler Created");
    }

    public int getTemperatureBufferSize() {
        return TEMPERATURE_BUFFER_SIZE;
    }

    public void setTemperatureBufferSize(int temperatureBufferSize) {
        TEMPERATURE_BUFFER_SIZE = temperatureBufferSize;
    }

    public int getTemperatureThreshold() {
        return TEMPERATURE_THRESHOLD;
    }

    public void setTemperatureThreshold(int temperatureThreshold) {
        TEMPERATURE_THRESHOLD = temperatureThreshold;
    }

    public Float filterTemperature(float newReading) {

        if (logger != null)
            logger.trace("Filtering {} baseline {} readingsCount {}", newReading, baselineAverage, readingsCount);
        // Update baseline average periodically
        if (isBaselineEstablished && readingsCount >= (TEMPERATURE_BUFFER_SIZE)) {
            baselineAverage = calculateAverage(temperatureBuffer, TEMPERATURE_BUFFER_SIZE);
            readingsCount = 0;
            if (logger != null)
                logger.trace("Current Baseline: {}", baselineAverage);
        }

        // Establish baseline average
        if (!isBaselineEstablished && readingsCount < TEMPERATURE_BUFFER_SIZE) {

            if (readingsCount == 0) {
                baselineAverage = newReading;
                temperatureBuffer[readingsCount] = newReading;
                readingsCount++;
            } else {
                // Check for outliers and update consecutiveBadReadings
                if (newReading != 0 && Math.abs(newReading - baselineAverage) > TEMPERATURE_THRESHOLD) {
                    consecutiveBadReadings++;
                    if (consecutiveBadReadings >= MAX_BAD_READINGS) {
                        // Too many consecutive bad readings, discard the current baseline
                        isBaselineEstablished = false;
                        baselineAverage = 0.0f;
                        readingsCount = 0;
                        consecutiveBadReadings = 0;
                        logger.warn("Baseline discarded due to too many bad readings.");
                    } else {
                        temperatureBuffer[readingsCount] = newReading;
                        readingsCount++;
                        consecutiveBadReadings = 0;
                        baselineAverage = calculateAverage(temperatureBuffer, readingsCount);
                    }
                } else {
                    temperatureBuffer[readingsCount] = newReading;
                    readingsCount++;
                    consecutiveBadReadings = 0;
                    baselineAverage = calculateAverage(temperatureBuffer, readingsCount);
                }

                if (logger != null)
                    logger.trace("Current Baseline: {} Current Count: {}", baselineAverage, readingsCount);

                if (readingsCount >= TEMPERATURE_BUFFER_SIZE) {
                    baselineAverage = calculateAverage(temperatureBuffer, readingsCount);
                    isBaselineEstablished = true;
                    readingsCount = 0;
                    if (logger != null)
                        logger.debug("New Baseline Temperature Established: {}", baselineAverage);
                }
            }

            return newReading;
        }

        // Check for outliers but accept a zero reading for situations where the outside
        // temperature is returning 0
        if (newReading != 0 && Math.abs(newReading - baselineAverage) > TEMPERATURE_THRESHOLD) {
            if (baselineAverage == 0) {
                temperatureBuffer[bufferIndex] = newReading;
                bufferIndex = (bufferIndex + 1) % temperatureBuffer.length;
                readingsCount++; // Valid reading increments reading count
                return newReading;
            } else
                return baselineAverage; // Return the baseline average instead of the outlier
        }

        // Add the new reading to the buffer if it is not 0
        // We do not want a zero baseline, or for 0 to through off the actual baseline calculation
        if (newReading != 0) {
            temperatureBuffer[bufferIndex] = newReading;
            bufferIndex = (bufferIndex + 1) % temperatureBuffer.length;
            readingsCount++; // Valid reading increments reading count
        } else {
            bufferIndex = 0;
        }
        return newReading;
    }

    private float calculateAverage(float[] temperatureBuffer, int COUNT) {
        float sum = 0.0f;
        try {
            for (int i = 0; i < COUNT; i++) {
                sum += temperatureBuffer[i];
            }
            if ((sum / COUNT) == 0)
                return baselineAverage;
            else
                return (sum / COUNT);
        } catch (ArithmeticException e) {
            logger.error("Error calculating average: {}", e.getMessage());
            return baselineAverage;
        }
    }
}
