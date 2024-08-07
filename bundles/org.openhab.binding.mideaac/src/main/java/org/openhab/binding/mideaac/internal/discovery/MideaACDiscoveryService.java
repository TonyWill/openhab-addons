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
package org.openhab.binding.mideaac.internal.discovery;

import static org.openhab.binding.mideaac.internal.MideaACBindingConstants.*;

import java.io.IOException;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.mideaac.internal.Utils;
import org.openhab.binding.mideaac.internal.handler.CommandBase;
import org.openhab.binding.mideaac.internal.security.CloudProvider;
import org.openhab.binding.mideaac.internal.security.Security;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.thing.ThingUID;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Discovery service for Midea AC.
 *
 * @author Jacek Dobrowolski - Initial contribution
 */
@NonNullByDefault
@Component(service = DiscoveryService.class, configurationPid = "discovery.mideaac")
public class MideaACDiscoveryService extends AbstractDiscoveryService {

    private static int discoveryTimeoutSeconds = 5; // 5; //
    private final int receiveJobTimeout = 20000;
    private final int udpPacketTimeout = receiveJobTimeout - 50;
    private final String mideaacNamePrefix = "MideaAC";

    private final Logger logger = LoggerFactory.getLogger(MideaACDiscoveryService.class);

    ///// Network
    private byte[] buffer = new byte[512];
    @Nullable
    private DatagramSocket discoverSocket;

    // private @Nullable MideaACHandler mideaACHandler;

    @SuppressWarnings("unused")
    private boolean fullDiscovery = false;
    @Nullable
    DiscoveryHandler discoveryHandler;

    private Security security;

    public MideaACDiscoveryService() {
        super(SUPPORTED_THING_TYPES_UIDS, discoveryTimeoutSeconds, false);
        this.security = new Security(CloudProvider.getCloudProvider(""));
    }

    /*
     * public void setHandler(MideaACHandler mideaACHandler) {
     * this.mideaACHandler = mideaACHandler;
     * }
     */

    @Override
    protected void startScan() {
        logger.debug("Start scan for Midea AC devices.");
        discoverThings();
    }

    @Override
    protected void stopScan() {
        logger.debug("Stop scan for Midea AC devices.");
        closeDiscoverSocket();
        super.stopScan();
    }

    /**
     * Performs the actual discovery of Midea AC devices (things).
     */
    @SuppressWarnings("null")
    private void discoverThings() {
        try {
            final DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
            // No need to call close first, because the caller of this method already has done it.
            startDiscoverSocket();
            // Runs until the socket call gets a time out and throws an exception. When a time out is triggered it means
            // no data was present and nothing new to discover.
            while (true) {
                // Set packet length in case a previous call reduced the size.
                receivePacket.setLength(buffer.length);
                if (discoverSocket == null) {
                    break;
                } else {
                    discoverSocket.receive(receivePacket);
                }
                logger.debug("Midea AC device discovery returned package with length {}", receivePacket.getLength());
                if (receivePacket.getLength() > 0) {
                    thingDiscovered(receivePacket);
                }
            }
        } catch (SocketTimeoutException e) {
            logger.debug("Discovering poller timeout...");
        } catch (IOException e) {
            // logger.debug("Error during discovery: {}", e.getMessage());
        } finally {
            closeDiscoverSocket();
            removeOlderResults(getTimestampOfLastScan());
        }
    }

    /**
     * Performs the actual discovery of Midea AC devices (things).
     */
    @SuppressWarnings("null")
    public void discoverThing(String ipAddress, DiscoveryHandler discoveryHandler) {
        try {
            final DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
            // No need to call close first, because the caller of this method already has done it.
            startDiscoverSocket(ipAddress, discoveryHandler);
            // Runs until the socket call gets a time out and throws an exception. When a time out is triggered it means
            // no data was present and nothing new to discover.
            while (true) {
                // Set packet length in case a previous call reduced the size.
                receivePacket.setLength(buffer.length);
                if (discoverSocket == null) {
                    break;
                } else {
                    discoverSocket.receive(receivePacket);
                }
                logger.debug("Midea AC device discovery returned package with length {}", receivePacket.getLength());
                if (receivePacket.getLength() > 0) {
                    thingDiscovered(receivePacket);
                }
            }
        } catch (SocketTimeoutException e) {
            logger.debug("Discovering poller timeout...");
        } catch (IOException e) {
            // logger.debug("Error during discovery: {}", e.getMessage());
        } finally {
            closeDiscoverSocket();
        }
    }

    /**
     * Opens a {@link DatagramSocket} and sends a packet for discovery of Midea AC devices.
     *
     * @throws SocketException
     * @throws IOException
     */
    private void startDiscoverSocket() throws SocketException, IOException {
        fullDiscovery = true;
        startDiscoverSocket("255.255.255.255", null);
    }

    @SuppressWarnings("null")
    public void startDiscoverSocket(String ipAddress, @Nullable DiscoveryHandler discoveryHandler)
            throws SocketException, IOException {
        logger.trace("Discovering: {}", ipAddress);
        this.discoveryHandler = discoveryHandler;
        discoverSocket = new DatagramSocket(new InetSocketAddress(Connection.MIDEAAC_RECEIVE_PORT));
        discoverSocket.setBroadcast(true);
        discoverSocket.setSoTimeout(udpPacketTimeout);
        final InetAddress broadcast = InetAddress.getByName(ipAddress);
        {
            final DatagramPacket discoverPacket = new DatagramPacket(CommandBase.discover(),
                    CommandBase.discover().length, broadcast, Connection.MIDEAAC_SEND_PORT1);
            discoverSocket.send(discoverPacket);
            if (logger.isTraceEnabled()) {
                logger.trace("Broadcast discovery package sent to port: {}", Connection.MIDEAAC_SEND_PORT1);
            }
        }
        {
            final DatagramPacket discoverPacket = new DatagramPacket(CommandBase.discover(),
                    CommandBase.discover().length, broadcast, Connection.MIDEAAC_SEND_PORT2);
            discoverSocket.send(discoverPacket);
            if (logger.isTraceEnabled()) {
                logger.trace("Broadcast discovery package sent to port: {}", Connection.MIDEAAC_SEND_PORT2);
            }
        }
    }

    /**
     * Closes the discovery socket and cleans the value. No need for synchronization as this method is called from a
     * synchronized context.
     */
    @SuppressWarnings("null")
    private void closeDiscoverSocket() {
        if (discoverSocket != null) {
            discoverSocket.close();
            discoverSocket = null;
        }
    }

    /**
     * Register a device (thing) with the discovered properties.
     *
     * @param packet containing data of detected device
     */
    @SuppressWarnings("null")
    private void thingDiscovered(DatagramPacket packet) {
        DiscoveryResult dr = discoveryPacketReceived(packet);
        if (dr != null) {
            if (discoveryHandler != null) {
                discoveryHandler.discovered(dr);
            } else {
                thingDiscovered(dr);
            }
        }
    }

    @Nullable
    public DiscoveryResult discoveryPacketReceived(DatagramPacket packet) {
        final String ipAddress = packet.getAddress().getHostAddress();
        byte[] data = Arrays.copyOfRange(packet.getData(), 0, packet.getLength());

        logger.debug("Midea AC discover data ({}) from {}: '{}'", data.length, ipAddress, Utils.bytesToHex(data));

        if (data.length >= 104 && (Utils.bytesToHex(Arrays.copyOfRange(data, 0, 2)).equals("5A5A")
                || Utils.bytesToHex(Arrays.copyOfRange(data, 8, 10)).equals("5A5A"))) {
            logger.trace("Device supported");
            String mSmartId, mSmartVersion = "", mSmartip = "", mSmartPort = "", mSmartSN = "", mSmartSSID = "",
                    mSmartType = "";
            if (Utils.bytesToHex(Arrays.copyOfRange(data, 0, 2)).equals("5A5A")) {
                mSmartVersion = "2";
            }
            if (Utils.bytesToHex(Arrays.copyOfRange(data, 0, 2)).equals("8370")) {
                mSmartVersion = "3";
            }
            if (Utils.bytesToHex(Arrays.copyOfRange(data, 8, 10)).equals("5A5A")) {
                data = Arrays.copyOfRange(data, 8, data.length - 16);
            }

            logger.trace("Version: {}", mSmartVersion);

            byte[] id = Arrays.copyOfRange(data, 20, 26);
            logger.trace("Id Bytes: {}", Utils.bytesToHex(id));

            // Reverse the subarray in-place
            // This level 3 issue replaces level 2 forbidden package
            // import org.apache.commons.lang3.ArrayUtils;
            // ArrayUtils.reverse(id);
            for (int i = 0; i < id.length / 2; i++) {
                byte temp = id[i];
                id[i] = id[id.length - i - 1];
                id[id.length - i - 1] = temp;
            }

            BigInteger bigId = new BigInteger(id);
            mSmartId = bigId.toString();

            logger.debug("Id: '{}'", mSmartId);

            byte[] encryptData = Arrays.copyOfRange(data, 40, data.length - 16);
            logger.debug("Encrypt data: '{}'", Utils.bytesToHex(encryptData));

            // byte[] reply = mideaACHandler.getSecurity().aesDecrypt(encryptData);
            byte[] reply = security.aesDecrypt(encryptData);
            logger.debug("Length: {}, Reply: '{}'", reply.length, Utils.bytesToHex(reply));

            mSmartip = Byte.toUnsignedInt(reply[3]) + "." + Byte.toUnsignedInt(reply[2]) + "."
                    + Byte.toUnsignedInt(reply[1]) + "." + Byte.toUnsignedInt(reply[0]);
            logger.debug("IP: '{}'", mSmartip);

            mSmartPort = String.valueOf(bytes2port(Arrays.copyOfRange(reply, 4, 8)));
            logger.debug("Port: '{}'", mSmartPort);

            mSmartSN = new String(reply, 8, 40 - 8, StandardCharsets.UTF_8);
            logger.debug("SN: '{}'", mSmartSN);

            logger.trace("SSID length: '{}'", Byte.toUnsignedInt(reply[40]));

            mSmartSSID = new String(reply, 41, reply[40], StandardCharsets.UTF_8);
            logger.debug("SSID: '{}'", mSmartSSID);

            mSmartType = mSmartSSID.split("_")[1];
            logger.debug("Type: '{}'", mSmartType);

            String thingName = createThingName(packet.getAddress().getAddress(), mSmartId, mSmartSSID);
            ThingUID thingUID = new ThingUID(THING_TYPE_MIDEAAC, thingName.toLowerCase());

            return DiscoveryResultBuilder.create(thingUID).withLabel(thingName)
                    .withRepresentationProperty(CONFIG_IP_ADDRESS).withThingType(THING_TYPE_MIDEAAC)
                    .withProperties(collectProperties(ipAddress, mSmartVersion, mSmartId, mSmartPort, mSmartSN,
                            mSmartSSID, mSmartType))
                    .build();
        } else if (Utils.bytesToHex(Arrays.copyOfRange(data, 0, 6)).equals("3C3F786D6C20")) {
            logger.debug("Midea AC v1 device was detected, supported, but not implemented yet.");
            // Possible code for AC1 device from ??:
            // if data[:6].hex() == '3c3f786d6c20':
            // mSmartVersion = 'V1'
            // root=ET.fromstring(data.decode(encoding="utf-8", errors="replace"))
            // child = root.find('body/device')
            // m=child.attrib
            // mSmartPort, mSmartSN, mSmartType = m['port'], m['apc_sn'], str(hex(int(m['apc_type'])))[2:]
            // response = get_device_info(mSmartip, int(mSmartPort))
            // mSmartId = get_id_fromSmartresponse(response)
            //
            // _LOGGER.info(
            // "*** Found a {} device - type: '0x{}' - version: {} - ip: {} - port: {} - id: {} - sn: {} - ssid:
            // {}".format(mSmartsupport, mSmartType, mSmartVersion, mSmartip, mSmartPort, mSmartId, mSmartSN,
            // mSmartSSID))
            return null;
        } else {
            logger.debug(
                    "Midea AC device was detected, but the retrieved data is incomplete or not supported. Device not registered");
            return null;
        }
    }

    private int bytes2port(byte[] bytes) {
        int b = 0;
        int i = 0;
        while (b < 4) {
            int b1;
            if (b < bytes.length) {
                b1 = bytes[b] & 0xFF;
            } else {
                b1 = 0;
            }

            i |= b1 << b * 8;
            b += 1;
        }
        return i;
    }

    /**
     * Creates a name for the Midea AC device.
     *
     * @param byteMac mac address in bytes
     * @return the name for the device
     */
    private String createThingName(final byte[] byteIP, String id, String ssid) {
        return mideaacNamePrefix + "__" + Byte.toUnsignedInt(byteIP[0]) + "_" + Byte.toUnsignedInt(byteIP[1]) + "_"
                + Byte.toUnsignedInt(byteIP[2]) + "_" + Byte.toUnsignedInt(byteIP[3]) + "__" + id + "__" + ssid;
    }

    /**
     * Collects properties into a map.
     *
     * @param ipAddress IP address of the thing
     * @param mac mac address of the thing
     * @return map with properties
     */
    private Map<String, Object> collectProperties(String ipAddress, String version, String id, String port, String sn,
            String ssid, String type) {
        final Map<String, Object> properties = new TreeMap<>();
        properties.put(CONFIG_IP_ADDRESS, ipAddress);
        properties.put(CONFIG_IP_PORT, port);
        properties.put(CONFIG_DEVICEID, id);
        properties.put(CONFIG_POLLING_TIME, 60);
        properties.put(CONFIG_PROMPT_TONE, false);
        properties.put(PROPERTY_VERSION, version);
        properties.put(PROPERTY_SN, sn);
        properties.put(PROPERTY_SSID, ssid);
        properties.put(PROPERTY_TYPE, type);

        return properties;
    }
}
