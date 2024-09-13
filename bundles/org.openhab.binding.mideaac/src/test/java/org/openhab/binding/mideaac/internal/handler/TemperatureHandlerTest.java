package org.openhab.binding.mideaac.internal.handler;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class TemperatureHandlerTest {
    @Test
    public void testFilterTemperature() {
        TemperatureHandler handler = new TemperatureHandler();
        // System.out.println("Hi there");
        // Test data with various scenarios
        float[] inputValues = { 25, 30, 28, 27, 32, 10.0f, 35.0f, 40.0f, 32, 31, 40.0f, 30, 26, 34.0f, 28, 42, 0, 0, 0,
                0, 0, 32, 32.4f, 31.8f, 40, 45, 31.4f };
        float[] expectedOutputs = { 25, 30, 28, 27, 32, 28.4f, 35.0f, 28.4f, 32, 31, 28.4f, 30, 26, 34f, 28, 30.8f, 0,
                0, 0, 0, 0, 32, 32.4f, 31.8f, 40, 30.439999f, 31.4f }; // Assuming a threshold of 10

        for (int i = 0; i < inputValues.length; i++) {
            float filteredTemperature = handler.filterTemperature(inputValues[i]);
            assertEquals(expectedOutputs[i], filteredTemperature, 0.01f); // Adjust tolerance as needed
        }
    }

    @Test
    public void testZeroFilterTemperature() {
        TemperatureHandler handler = new TemperatureHandler();

        // Test data with various scenarios
        float[] inputValues = { 0, 0, 0, 0, 0, 0, 0, 40.0f, 32, 31, 40.0f, 30, 20, 34.0f, 28, 42, 0, 0, 0, 0, 0, 32,
                32.4f, 31.8f, 50, 45.3f, 45.28f, 45.29f };
        float[] expectedOutputs = { 0, 0, 0, 0, 0, 0, 0, 40.0f, 32, 31, 40.0f, 30, 34.6f, 34f, 28, 42, 0, 0, 0, 0, 0,
                32, 32.4f, 31.8f, 35.28f, 35.28f, 45.28f, 35.28f }; // Assuming a threshold of 10

        for (int i = 0; i < inputValues.length; i++) {
            float filteredTemperature = handler.filterTemperature(inputValues[i]);
            assertEquals(expectedOutputs[i], filteredTemperature, 0.01f); // Adjust tolerance as needed
        }
    }

    @Test
    public void testInvalidBaselineFilterTemperature() {
        TemperatureHandler handler = new TemperatureHandler();

        // Test data with various scenarios
        float[] inputValues = { -19, 35, 30, -19, 35, 0, 0, 40.0f, 32, 31, 40.0f, 30, 30, 34.0f, 28, 42, 0, 0, 0, 0, 0,
                32, 32.4f, 31.8f, 50, 45.3f, 45.28f, 45.29f };
        float[] expectedOutputs = { -19, 35, 30, -19, 35, 0, 0, 40.0f, 32, 31, 40.0f, 30, 30, 34f, 28, 42, 0, 0, 0, 0,
                0, 32, 32.4f, 31.8f, 33.28f, 33.28f, 33.28f, 33.28f }; // Assuming a threshold of 10

        for (int i = 0; i < inputValues.length; i++) {
            float filteredTemperature = handler.filterTemperature(inputValues[i]);
            assertEquals(expectedOutputs[i], filteredTemperature, 0.01f); // Adjust tolerance as needed
        }
    }

    @Test
    public void testMultipleFilterTemperature() {
        TemperatureHandler indoorTemperatureHandler = new TemperatureHandler();
        TemperatureHandler outdoorTemperatureHandler = new TemperatureHandler();

        // Test data with various scenarios
        float[] inputValues_1 = { 25, 30, 28, 27, 32, 10.0f, 35.0f, 40.0f, 32, 31, 40.0f, 30, 26, 34.0f, 28, 42, 0, 0,
                0, 0, 0, 32, 32.4f, 31.8f, 40, 45, 31.4f };
        float[] expectedOutputs_1 = { 25, 30, 28, 27, 32, 28.4f, 35.0f, 28.4f, 32, 31, 28.4f, 30, 26, 34f, 28, 30.8f, 0,
                0, 0, 0, 0, 32, 32.4f, 31.8f, 40, 30.439999f, 31.4f }; // Assuming a threshold of 10
        float[] inputValues_2 = { 35, 32, 28, 29, 32, 10.0f, 34.0f, 48.0f, 32, 31, 40.0f, 30, 26, 34.0f, 28, 49, 0, 0,
                0, 0, 0, 32, 32.4f, 31.8f, 40, 45, 31.4f };
        float[] expectedOutputs_2 = { 35, 32, 28, 29, 32, 31.2f, 34.0f, 31.2f, 32, 31, 40.0f, 30, 26, 34f, 28, 33.4f, 0,
                0, 0, 0, 0, 32, 32.4f, 31.8f, 40, 32.48f, 31.4f }; // Assuming a threshold of 10

        for (int i = 0; i < inputValues_1.length; i++) {
            float filteredTemperature = indoorTemperatureHandler.filterTemperature(inputValues_1[i]);
            assertEquals(expectedOutputs_1[i], filteredTemperature, 0.01f); // Adjust tolerance as needed

            filteredTemperature = outdoorTemperatureHandler.filterTemperature(inputValues_2[i]);
            assertEquals(expectedOutputs_2[i], filteredTemperature, 0.01f); // Adjust tolerance as needed
        }
    }
}
