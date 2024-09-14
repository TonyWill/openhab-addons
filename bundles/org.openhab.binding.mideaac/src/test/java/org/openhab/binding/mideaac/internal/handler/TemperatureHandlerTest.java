package org.openhab.binding.mideaac.internal.handler;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class TemperatureHandlerTest {
    @Test
    public void testFilterTemperature() {
        TemperatureHandler handler = new TemperatureHandler();
        // System.out.println("Hi there");
        // Test data with various scenarios
        float[] inputValues = { 25, 30, 28, 27, 32, 10.0f, 35.0f, 40.0f, 32, 31, 50.0f, 30, 26, 34.0f, 28, 42, 0, 0, 0,
                0, 0, 32, 32.4f, 31.8f, 40, 45, 31.4f };
        float[] expectedOutputs = { 25, 30, 28, 27, 32, 27.66f, 35.0f, 40, 32, 31, 34f, 30, 26, 34f, 28, 30.6f, 0, 0, 0,
                0, 0, 32, 32.4f, 31.8f, 40, 30.68f, 31.4f }; // Assuming a threshold of 10

        for (int i = 0; i < inputValues.length; i++) {
            // System.out.println("input - expected output: "+inputValues[i]+" - "+expectedOutputs[i]);
            float filteredTemperature = handler.filterTemperature(inputValues[i]);
            // System.out.println("actual output: "+filteredTemperature);
            assertEquals(expectedOutputs[i], filteredTemperature, 0.01f); // Adjust tolerance as needed
        }
    }

    @Test
    public void testZeroFilterTemperature() {
        TemperatureHandler handler = new TemperatureHandler();

        // Test data with various scenarios
        float[] inputValues = { 0, 0, 0, 40.0f, 32, 31, 40.0f, 30, 20, 34.0f, 28, 42, 0, 0, 0, 0, 0, 32, 32.4f, 31.8f,
                50, 45.3f, 45.28f, 45.29f };
        float[] expectedOutputs = { 0, 0, 0, 40.0f, 32, 31, 40.0f, 30, 34.33f, 34f, 28, 42, 0, 0, 0, 0, 0, 32, 32.4f,
                31.8f, 33.68f, 33.68f, 33.68f, 33.68f }; // Assuming a threshold of 10

        for (int i = 0; i < inputValues.length; i++) {
            // System.out.println("input - expected output: "+inputValues[i]+" - "+expectedOutputs[i]);
            float filteredTemperature = handler.filterTemperature(inputValues[i]);
            // System.out.println("actual output: "+filteredTemperature);
            assertEquals(expectedOutputs[i], filteredTemperature, 0.01f); // Adjust tolerance as needed
        }
    }

    @Test
    public void testInvalidBaselineFilterTemperature() {
        TemperatureHandler handler = new TemperatureHandler();

        // Test data with various scenarios
        float[] inputValues = { -19, 35, 30, -19, 35, 0, 0, 40.0f, 32, 31, 40.0f, 30, 30, 34.0f, 28, 42, 0, 0, 0, 0, 0,
                32, 32.4f, 31.8f, 50, 45.3f, 43.68f, 45.29f };
        float[] expectedOutputs = { -19, 35, 30, -19, 35, 0, 0, 40.0f, 32, 31, 40.0f, 30, 30, 34f, 28, 42, 0, 0, 0, 0,
                0, 32, 32.4f, 31.8f, 33.68f, 33.68f, 43.68f, 33.68f }; // Assuming a threshold of 10

        for (int i = 0; i < inputValues.length; i++) {
            // System.out.println("input - expected output: "+inputValues[i]+" - "+expectedOutputs[i]);
            float filteredTemperature = handler.filterTemperature(inputValues[i]);
            // System.out.println("actual output: "+filteredTemperature);
            assertEquals(expectedOutputs[i], filteredTemperature, 0.01f); // Adjust tolerance as needed
        }
    }

    @Test
    public void testMultipleFilterTemperature() {
        TemperatureHandler indoorTemperatureHandler = new TemperatureHandler();
        TemperatureHandler outdoorTemperatureHandler = new TemperatureHandler();

        // Test data with various scenarios
        float[] inputValues_1 = { 25, 30, 28, 27, 32, 10.0f, 35.0f, 50.0f, 32, 31, 40.0f, 30, 26, 34.0f, 28, -19, 0, 0,
                0, 0, 0, 32, 32.4f, 31.8f, 40, 45, 31.4f };
        float[] expectedOutputs_1 = { 25, 30, 28, 27, 32, 27.66f, 35.0f, 30.4f, 32, 31, 40f, 30, 26, 34f, 28, 32.2f, 0,
                0, 0, 0, 0, 32, 32.4f, 31.8f, 40, 32.48f, 31.4f }; // Assuming a threshold of 10
        float[] inputValues_2 = { 35, 32, 28, 29, 32, 10.0f, 34.0f, 48.0f, 32, 31, 40.0f, 30, 26, 34.0f, 28, 49, 0, 0,
                0, 0, 0, 32, 32.4f, 31.8f, 40, 45, 31.4f };
        float[] expectedOutputs_2 = { 35, 32, 28, 29, 32, 31.66f, 34.0f, 31.0f, 32, 31, 40.0f, 30, 26, 34f, 28, 32.2f,
                0, 0, 0, 0, 0, 32, 32.4f, 31.8f, 40, 32.48f, 31.4f }; // Assuming a threshold of 10

        for (int i = 0; i < inputValues_1.length; i++) {
            // System.out.println("indoor input - expected output: "+inputValues_1[i]+" - "+expectedOutputs_1[i]);
            float filteredTemperature = indoorTemperatureHandler.filterTemperature(inputValues_1[i]);
            // System.out.println("indoor actual output: "+filteredTemperature);
            assertEquals(expectedOutputs_1[i], filteredTemperature, 0.01f); // Adjust tolerance as needed

            // System.out.println("outdoor input - expected output: "+inputValues_2[i]+" - "+expectedOutputs_2[i]);
            filteredTemperature = outdoorTemperatureHandler.filterTemperature(inputValues_2[i]);
            // System.out.println("outdoor actual output: "+filteredTemperature);
            assertEquals(expectedOutputs_2[i], filteredTemperature, 0.01f); // Adjust tolerance as needed
        }
    }
}
