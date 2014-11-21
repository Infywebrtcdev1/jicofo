package mock;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import org.osgi.framework.*;

/**
 *
 */
public class ProviderListener
    implements ServiceListener
{
    private final BundleContext context;

    private ProtocolProviderService pps;

    public ProviderListener(BundleContext context)
    {
        this.context = context;

        pps = ServiceUtils.getService(context, ProtocolProviderService.class);

        if (pps == null)
        {
            context.addServiceListener(this);
        }
    }

    @Override
    public void serviceChanged(ServiceEvent event)
    {
        if (event.getType() != ServiceEvent.REGISTERED)
            return;

        Object service = context.getService(event.getServiceReference());
        if (service instanceof ProtocolProviderService)
        {
            context.removeServiceListener(this);

            synchronized (this)
            {
                pps = (ProtocolProviderService) service;

                this.notifyAll();
            }
        }
    }

    public synchronized ProtocolProviderService obtainProvider(long timeout)
    {
        if (pps != null)
            return pps;

        try
        {
            this.wait(timeout);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }

        if (pps == null)
            throw new RuntimeException("Failed to get protocol provider");

        return pps;
    }
}
