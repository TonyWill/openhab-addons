/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.mideaac.internal.handler;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * This {@link CommandSet} class handles the allowed changes originating from
 * the items linked to the Midea AC channels. Not all devices
 * support all commands. The general process is to clear the
 * bit(s) the set them to the commanded value.
 *
 * @author Jacek Dobrowolski - Initial contribution
 */
@NonNullByDefault
public class CommandSet extends CommandBase {

    public CommandSet() {
        data[0x01] = (byte) 0x23;
        data[0x09] = (byte) 0x02;
        // Set up Mode
        data[0x0a] = (byte) 0x40;

        byte[] extra = { 0x00, 0x00, 0x00 };
        byte[] newData = new byte[data.length + 3];
        System.arraycopy(data, 0, newData, 0, data.length);
        newData[data.length] = extra[0];
        newData[data.length + 1] = extra[1];
        newData[data.length + 2] = extra[2];
        data = newData;
    }

    public static CommandSet fromResponse(Response response) {
        CommandSet commandSet = new CommandSet();

        commandSet.setPowerState(response.getPowerState());
        commandSet.setTargetTemperature(response.getTargetTemperature());
        commandSet.setOperationalMode(response.getOperationalMode());
        commandSet.setFanSpeed(response.getFanSpeed());
        commandSet.setFahrenheit(response.getFahrenheit());
        commandSet.setTurboMode(response.getTurboMode());
        commandSet.setSwingMode(response.getSwingMode());
        commandSet.setScreenDisplay(response.getNightLight());
        commandSet.setEcoMode(response.getEcoMode());
        commandSet.setSleepMode(response.getSleepFunction());

        return commandSet;
    }

    public void setPromptTone(boolean feedbackEnabled) {
        if (!feedbackEnabled) {
            data[0x0b] &= ~(byte) 0x40; // Clear
        } else {
            data[0x0b] |= (byte) 0x40; // Set
        }
    }

    public void setPowerState(boolean state) {
        if (!state) {
            data[0x0b] &= ~0x01;
        } else {
            data[0x0b] |= 0x01;
        }
    }

    /*
     * For Testing assertion get result
     */
    public boolean getPowerState() {
        return (data[0x0b] & 0x1) > 0;
    }

    public void setOperationalMode(OperationalMode mode) {
        data[0x0c] &= ~(byte) 0xe0; // Clear
        data[0x0c] |= ((byte) mode.getId() << 5) & (byte) 0xe0;
    }

    /*
     * For Testing assertion get result
     */
    public int getOperationalMode() {
        return data[0x0c] &= (byte) 0xe0;
    }

    public void setTargetTemperature(float temperature) {
        // Clear the temperature bits.
        data[0x0c] &= ~0x0f;
        // Clear the temperature bits, except the 0.5 bit, which will be set properly in all cases
        data[0x0c] |= (int) (Math.round(temperature * 2) / 2) & 0xf;
        // set the +0.5 bit
        setTemperatureDot5((Math.round(temperature * 2)) % 2 != 0);
    }

    /*
     * For Testing assertion get result
     */
    public float getTargetTemperature() {
        return (data[0x0c] & 0xf) + 16.0f + (((data[0x0c] & 0x10) > 0) ? 0.5f : 0.0f);
    }

    public void setFanSpeed(FanSpeed speed) {
        data[0x0d] = (byte) (speed.getId());
    }

    /*
     * For Testing assertion get result
     */
    public int getFanSpeed() {
        return data[0x0d];
    }

    public void setEcoMode(boolean ecoModeEnabled) {
        if (!ecoModeEnabled) {
            data[0x13] &= ~0x80; // Clear the Eco bit (if set)
        } else {
            data[0x13] |= 0x80;
        }
    }

    public void setSwingMode(SwingMode mode) {
        data[0x11] &= ~0x3f; // Clear the mode bits
        data[0x11] |= mode.getId() & 0x3f;
    }

    /*
     * For Testing assertion get result
     */
    public int getSwingMode() {
        return data[0x11];
    }

    public void setSleepMode(boolean sleepModeEnabled) {
        if (sleepModeEnabled) {
            data[0x14] |= 0x01;
        } else {
            data[0x14] &= (~0x01);
        }
    }

    public void setTurboMode(boolean turboModeEnabled) {
        if (turboModeEnabled) {
            data[0x14] |= 0x02;
        } else {
            data[0x14] &= (~0x02);
        }
    }

    /*
     * Set the display to Fahrenheit from Celsius
     */
    public void setFahrenheit(boolean fahrenheitEnabled) {
        if (fahrenheitEnabled) {
            data[0x14] |= 0x04;
        } else {
            data[0x14] &= (~0x04);
        }
    }

    /*
     * The LED lights on the AC are too bright during sleep
     */
    public void setScreenDisplay(boolean screenDisplayEnabled) {
        if (screenDisplayEnabled) {
            data[0x14] |= 0x10;
        } else {
            data[0x14] &= (~0x10);
        }
    }

    /*
     * Add 0.5C to the temperature value. If needed
     * Target_temperature setter calls this method
     */
    private void setTemperatureDot5(boolean temperatureDot5Enabled) {
        if (temperatureDot5Enabled) {
            data[0x0c] |= 0x10;
        } else {
            data[0x0c] &= (~0x10);
        }
    }
}
