/*
 * Copyright (C) 2011 Klaus Reimer <k@ailis.de>
 * See LICENSE.txt for licensing information.
 */

package de.ailis.usb4java.topology;

import static de.ailis.usb4java.jni.USB.usb_set_altinterface;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.usb.UsbConfiguration;
import javax.usb.UsbConfigurationDescriptor;
import javax.usb.UsbDisconnectedException;
import javax.usb.UsbException;
import javax.usb.UsbInterface;

import de.ailis.usb4java.descriptors.LibUsbConfigurationDescriptor;
import de.ailis.usb4java.exceptions.LibUsbException;
import de.ailis.usb4java.jni.USB_Config_Descriptor;
import de.ailis.usb4java.jni.USB_Interface;
import de.ailis.usb4java.jni.USB_Interface_Descriptor;
import de.ailis.usb4java.support.UsbLock;


/**
 * usb4java implementation of JSR-80 UsbConfiguration.
 *
 * @author Klaus Reimer (k@ailis.de)
 */

public final class LibUsbConfiguration implements UsbConfiguration
{
    /** The USB configuration descriptor. */
    private final UsbConfigurationDescriptor descriptor;

    /** The USB device. */
    private final LibUsbDevice device;

    /**
     * The interfaces. This is a map from interface number to a map of alternate
     * settings which maps setting numbers to actual interfaces.
     */
    private final Map<Integer, Map<Integer, LibUsbInterface>> interfaces =
        new HashMap<Integer, Map<Integer, LibUsbInterface>>();

    /** This map contains the active USB interfaces. */
    private final Map<Integer, LibUsbInterface> activeSettings =
        new HashMap<Integer, LibUsbInterface>();


    /**
     * Constructor.
     *
     * @param device
     *            The USB device.
     * @param lowLevelDescriptor
     *            The low-level USB configuration descriptor.
     */

    public LibUsbConfiguration(final LibUsbDevice device,
        final USB_Config_Descriptor lowLevelDescriptor)
    {
        this.device = device;
        this.descriptor = new LibUsbConfigurationDescriptor(
            lowLevelDescriptor);

        // Build interfaces
        for (final USB_Interface iface : lowLevelDescriptor.iface())
        {
            for (final USB_Interface_Descriptor desc : iface.altsetting())
            {
                final int ifaceNumber = desc.bInterfaceNumber();
                final int settingNumber = desc.bAlternateSetting();

                Map<Integer, LibUsbInterface> settings = this.interfaces
                        .get(ifaceNumber);
                if (settings == null)
                {
                    settings = new HashMap<Integer, LibUsbInterface>();
                    this.interfaces.put(ifaceNumber, settings);
                }
                final LibUsbInterface usbInterface = new LibUsbInterface(this,
                    desc, device);

                // If we have no active setting for current interface number
                // yet or the alternate setting number is 0 (which marks the
                // default alternate setting) then set current interface as
                // the active setting.
                if (!this.activeSettings.containsKey(ifaceNumber)
                    || desc.bAlternateSetting() == 0)
                {
                    this.activeSettings.put(ifaceNumber, usbInterface);
                }

                // Add the interface to the settings list
                settings.put(settingNumber, usbInterface);
            }
        }
    }


    /**
     * Ensures that the device is connected.
     *
     * @throws UsbDisconnectedException
     *             When device has been disconnected.
     */

    private void checkConnected() throws UsbDisconnectedException
    {
        this.device.checkConnected();
    }


    /**
     * @see UsbConfiguration#isActive()
     */

    @Override
    public boolean isActive()
    {
        return this.device.getActiveUsbConfigurationNumber() == this.descriptor
                .bConfigurationValue();
    }


    /**
     * @see UsbConfiguration#getUsbInterfaces()
     */

    @Override
    public List<UsbInterface> getUsbInterfaces()
    {
        return Collections.unmodifiableList(new ArrayList<UsbInterface>(
            this.activeSettings.values()));
    }


    /**
     * Returns the alternate settings for the specified interface.
     *
     * @param number
     *            The interface number.
     * @return The alternate settings for the specified interface.
     */

    Map<Integer, LibUsbInterface> getSettings(final byte number)
    {
        return this.interfaces.get(number & 0xff);
    }


    /**
     * Returns the number of alternate settings of the specified interface.
     *
     * @param number
     *            The interface number.
     * @return The number of alternate settings.
     */

    int getNumSettings(final byte number)
    {
        return this.interfaces.get(number & 0xff).size();
    }


    /**
     * @see UsbConfiguration#getUsbInterface(byte)
     */

    @Override
    public LibUsbInterface getUsbInterface(final byte number)
    {
        return this.activeSettings.get((int) number);
    }


    /**
     * Sets the active USB interface setting.
     *
     * @param number
     *            THe interface number.
     * @param iface
     *            The interface setting to activate.
     * @throws UsbException
     *             When interface setting could not be set.
     */

    void setUsbInterface(final byte number, final LibUsbInterface iface)
        throws UsbException
    {
        if (this.activeSettings.get(number & 0xff) != iface)
        {
            UsbLock.acquire();
            try
            {
                final int result = usb_set_altinterface(this.device.open(),
                    iface.getUsbInterfaceDescriptor().bAlternateSetting());
                if (result < 0)
                    throw new LibUsbException(
                        "Unable to set alternate interface", result);
                this.activeSettings.put(number & 0xff, iface);
            }
            finally
            {
                UsbLock.release();
            }
        }
    }


    /**
     * @see UsbConfiguration#containsUsbInterface(byte)
     */

    @Override
    public boolean containsUsbInterface(final byte number)
    {
        return this.activeSettings.containsKey(number & 0xff);
    }


    /**
     * @see UsbConfiguration#getUsbDevice()
     */

    @Override
    public LibUsbDevice getUsbDevice()
    {
        return this.device;
    }


    /**
     * @see UsbConfiguration#getUsbConfigurationDescriptor()
     */

    @Override
    public UsbConfigurationDescriptor getUsbConfigurationDescriptor()
    {
        return this.descriptor;
    }


    /**
     * @see UsbConfiguration#getConfigurationString()
     */

    @Override
    public String getConfigurationString() throws UsbException,
        UnsupportedEncodingException, UsbDisconnectedException
    {
        checkConnected();
        final byte iConfiguration = this.descriptor.iConfiguration();
        if (iConfiguration == 0) return null;
        return this.device.getString(iConfiguration);
    }
}
