/*
 * Copyright (C) 2011 Klaus Reimer <k@ailis.de>
 * See LICENSE.txt for licensing information.
 */

package de.ailis.usb4java.jsr80;

import static de.ailis.usb4java.USB.usb_init;

import javax.usb.UsbDevice;
import javax.usb.UsbException;
import javax.usb.UsbHub;
import javax.usb.UsbServices;
import javax.usb.event.UsbServicesEvent;
import javax.usb.event.UsbServicesListener;


/**
 * usb4java implementation of JSR-80 UsbServices
 *
 * @author Klaus Reimer (k@ailis.de)
 */

public final class UsbServicesImpl implements UsbServices
{
    /** The implementation description. */
    private static final String IMP_DESCRIPTION = "usb4java JSR-80 implementation";

    /** The implementation version. */
    private static final String IMP_VERSION = "0.1.12-1";

    /** The API version. */
    private static final String API_VERSION = "1.0.1";

    /** The USB services listeners. */
    private final UsbServicesListenerList listeners = new UsbServicesListenerList();

    /** The virtual USB root hub. */
    private final VirtualRootHub rootHub;

    /** The USB device scanner. */
    private final UsbDeviceScanner deviceScanner;


    /**
     * Constructor.
     */

    public UsbServicesImpl()
    {
        usb_init();
        this.rootHub = new VirtualRootHub();
        this.deviceScanner = new UsbDeviceScanner(this, this.rootHub);
        this.deviceScanner.start();
    }


    /**
     * @see UsbServices#getRootUsbHub()
     */

    @Override
    public UsbHub getRootUsbHub() throws UsbException, SecurityException
    {
        this.deviceScanner.firstScan();
        return this.rootHub;
    }


    /**
     * @see UsbServices#addUsbServicesListener(UsbServicesListener)
     */

    @Override
    public void addUsbServicesListener(final UsbServicesListener listener)
    {
        this.listeners.add(listener);
    }


    /**
     * @see UsbServices#removeUsbServicesListener(UsbServicesListener)
     */

    @Override
    public void removeUsbServicesListener(final UsbServicesListener listener)
    {
        this.listeners.remove(listener);
    }


    /**
     * @see UsbServices#getApiVersion()
     */

    @Override
    public String getApiVersion()
    {
        return API_VERSION;
    }


    /**
     * @see UsbServices#getImpVersion()
     */

    @Override
    public String getImpVersion()
    {
        return IMP_VERSION;
    }


    /**
     * @see UsbServices#getImpDescription()
     */

    @Override
    public String getImpDescription()
    {
        return IMP_DESCRIPTION;
    }


    /**
     * Informs listeners about a new attached device.
     *
     * @param device The new attached device.
     */

    void usbDeviceAttached(final UsbDevice device)
    {
        this.listeners.usbDeviceAttached(new UsbServicesEvent(this, device));
    }


    /**
     * Informs listeners about a detached device.
     *
     * @param device The detached device.
     */

    void usbDeviceDetached(final UsbDevice device)
    {
        this.listeners.usbDeviceDetached(new UsbServicesEvent(this, device));
    }
}
