/*
 * Jicofo, the Jitsi Conference Focus.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.jicofo;

import net.java.sip.communicator.service.shutdown.*;
import net.java.sip.communicator.util.*;

import org.jitsi.jicofo.osgi.*;
import org.jitsi.jicofo.xmpp.*;
import org.jitsi.service.configuration.*;

import org.jivesoftware.whack.*;

import org.osgi.framework.*;

import org.xmpp.component.*;


/**
 * Provides the <tt>main</tt> entry point of Jitsi Meet conference focus.
 *
 * @author Pawel Domas
 */
public class Main
{
    private static final Logger logger = Logger.getLogger(Main.class);

    /**
     * The name of the command-line argument which specifies the XMPP domain
     * to use for the XMPP client connection.
     */
    private static final String DOMAIN_ARG_NAME = "--domain=";

    /**
     * The sync root used to hold the main thread until the exit procedure
     * is not started.
     */
    private static final Object exitSynRoot = new Object();

    /**
     * The name of the command-line argument which specifies the password
     * used by focus XMPP user to login. If not provided then focus will use
     * anonymous authentication method.
     */
    private static final String LOGIN_PASSWORD_ARG_NAME = "--password=";

    /**
     * The name of the command-line argument which specifies the IP address or
     * the name of the XMPP host to connect to.
     */
    private static final String HOST_ARG_NAME = "--host=";

    /**
     * The default value of the {@link #HOST_ARG_NAME} command-line argument if
     * it is not explicitly provided.
     */
    private static final String HOST_ARG_VALUE = "localhost";

    /**
     * The name of the command-line argument which specifies the port of the
     * XMPP host to connect on.
     */
    private static final String PORT_ARG_NAME = "--port=";

    /**
     * The default value of the {@link #PORT_ARG_NAME} command-line argument if
     * it is not explicitly provided.
     */
    private static final int PORT_ARG_VALUE = 5347;

    /**
     * The name of the command-line argument which specifies the secret key for
     * the sub-domain of the Jabber component implemented by this application
     * with which it is to authenticate to the XMPP server to connect to.
     */
    private static final String SECRET_ARG_NAME = "--secret=";

    /**
     * The name of the command-line argument which specifies sub-domain name for
     * the focus component.
     */
    private static final String SUBDOMAIN_ARG_NAME = "--subdomain=";

    /**
     * The name of the command-line argument which specifies sub-domain name for
     * the focus component.
     */
    private static final String SUBDOMAIN_ARG_VALUE = "focus";

    /**
     * Program entry point.
     * @param args command-line arguments.
     */
    public static void main(String[] args)
    {
        // XMPP host
        String host = HOST_ARG_VALUE;
        // Focus user
        String focusDomain = null;
        String focusPassword = null;
        // Focus XMPP component
        String componentSubDomain = SUBDOMAIN_ARG_VALUE;
        int port = PORT_ARG_VALUE;
        String secret = null;

        for (String arg : args)
        {
            if (arg.startsWith(DOMAIN_ARG_NAME))
            {
                focusDomain = arg.substring(DOMAIN_ARG_NAME.length());
            }
            else if (arg.startsWith(HOST_ARG_NAME))
            {
                host = arg.substring(HOST_ARG_NAME.length());
            }
            else if (arg.startsWith(LOGIN_PASSWORD_ARG_NAME))
            {
                focusPassword = arg.substring(LOGIN_PASSWORD_ARG_NAME.length());
            }
            else if (arg.startsWith(PORT_ARG_NAME))
            {
                port = Integer.parseInt(arg.substring(PORT_ARG_NAME.length()));
            }
            else if (arg.startsWith(SECRET_ARG_NAME))
            {
                secret = arg.substring(SECRET_ARG_NAME.length());
            }
            else if (arg.startsWith(SUBDOMAIN_ARG_NAME))
            {
                componentSubDomain = arg.substring(SUBDOMAIN_ARG_NAME.length());
            }
        }

        if (host == null)
            host = (focusDomain == null) ? HOST_ARG_VALUE : focusDomain;

        if (secret == null)
        {
            System.err.println("Missing required argument " + SECRET_ARG_NAME);
            return;
        }


        // FIXME: properties used for debug purposes
        // jigasi-home will be create in current directory (from where the
        // process is launched). It must contain sip-communicator.properties
        // with one XMPP and one SIP account configured.
        System.setProperty(
            ConfigurationService.PNAME_SC_HOME_DIR_LOCATION,
            System.getProperty("user.dir"));
        System.setProperty(
            ConfigurationService.PNAME_SC_HOME_DIR_NAME,
            "focus-home");

        // FIXME: Always trust mode - prevent failures because there's no GUI
        // to ask the user, but do we always want to trust ?
        System.setProperty(
            "net.java.sip.communicator.service.gui.ALWAYS_TRUST_MODE_ENABLED",
            "true");

        /*
         * Start OSGi. It will invoke the application programming interfaces
         * (APIs) of Jitsi Videobridge. Each of them will keep the application
         * alive.
         */
        BundleActivator activator =
            new BundleActivator()
            {
                @Override
                public void start(BundleContext bundleContext)
                    throws Exception
                {
                    registerShutdownService(bundleContext);
                }

                @Override
                public void stop(BundleContext bundleContext)
                    throws Exception
                {
                    // TODO Auto-generated method stub
                }
            };

        OSGi.start(activator);


        ExternalComponentManager componentManager
            = new ExternalComponentManager(host, port);

        componentManager.setSecretKey(componentSubDomain, secret);

        componentManager.setServerName(host);

        FocusComponent component
            = new FocusComponent(host, focusDomain, focusPassword);

        boolean stop = false;

        try
        {
            componentManager.addComponent(componentSubDomain, component);
        }
        catch (ComponentException e)
        {
            logger.error(e, e);
            stop = true;
        }

        component.init();

        if (!stop)
        {
            try
            {
                synchronized (exitSynRoot)
                {
                    startQKeyHandler();

                    exitSynRoot.wait();
                }
            }
            catch (Exception e)
            {
                logger.error(e, e);
            }
        }
        component.shutdown();
        try
        {
            componentManager.removeComponent(componentSubDomain);
        }
        catch (ComponentException e)
        {
            logger.error(e, e);
        }

        component.dispose();

        OSGi.stop();
    }

    /**
     * FIXME Used for the time being for convenience - to be removed
     */
    private static void startQKeyHandler()
    {

        Thread handler = new Thread(new Runnable(){
            @Override
            public void run()
            {
                do
                {
                    try
                    {
                        if(System.in.read() == 'q')
                            break;

                        Thread.sleep(100);
                    }
                    catch (Exception e)
                    {
                        logger.error(e, e);
                        break;
                    }
                }
                while (true);
                synchronized (exitSynRoot)
                {
                    exitSynRoot.notifyAll();
                }
            }
        }, "q-key-handler");
        handler.setDaemon(true);
        handler.start();
    }

    /**
     * Registers {@link ShutdownService} implementation that releases the main
     * thread and exits the app.
     *
     * @param context the OSGi context on which shutdown service will
     *                be registered.
     */
    private static void registerShutdownService(BundleContext context)
    {
        context.registerService(
            ShutdownService.class,
            new ShutdownService()
            {
                @Override
                public void beginShutdown()
                {
                    synchronized (exitSynRoot)
                    {
                        exitSynRoot.notifyAll();
                    }
                }
            }, null
        );
    }
}
