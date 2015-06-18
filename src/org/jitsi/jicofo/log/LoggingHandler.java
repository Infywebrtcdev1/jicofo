/*
 * Jicofo, the Jitsi Conference Focus.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.jicofo.log;

import org.jitsi.service.configuration.*;
import org.jitsi.videobridge.eventadmin.*;
import org.jitsi.videobridge.influxdb.*;

/**
 * Extends <tt>org.jitsi.videobridge.influxdb.LoggingHandler</tt> with
 * jicofo-specific functionality.
 *
 * @author Boris Grozev
 * @author Pawel Domas
 */
public class LoggingHandler
    extends org.jitsi.videobridge.influxdb.LoggingHandler
{
    /**
     * The names of the columns of an "endpoint display name" event.
     */
    private static final String[] ENDPOINT_DISPLAY_NAME_COLUMNS
            = new String[]
            {
                    EventFactory.CONFERENCE_ID_KEY,
                    EventFactory.ENDPOINT_ID_KEY,
                    EventFactory.DISPLAY_NAME_KEY
            };

    
    private static final String[] ENDPOINT_DISPLAY_NAME_ROOM_NAME_COLUMNS
    = new String[]
    {
            EventFactory.CONFERENCE_ID_KEY,
            EventFactory.ENDPOINT_ID_KEY,
            EventFactory.DISPLAY_NAME_KEY,
            EventFactory.ROOM_JID_KEY
            
    };

    
    
    
    
    /**
     * The names of the columns of a "peer connection stats" event.
     */
     static final String[] PEER_CONNECTION_STATS_COLUMNS
            = new String[]
            {
                    "time",
                    EventFactory.CONFERENCE_ID_KEY,
                    EventFactory.ENDPOINT_ID_KEY,
                    /*
                    "group_name",
                    "type",
                    "stat",
                    */
                    "value"
            };

    /**
     * The names of the columns of a "conference room" event.
     */
    private static final String[] CONFERENCE_ROOM_COLUMNS
            = new String[]
            {
                    EventFactory.CONFERENCE_ID_KEY,
                    EventFactory.ROOM_JID_KEY,
                    EventFactory.FOCUS_ID_KEY,
                    EventFactory.BRIDGE_JID_KEY
            };

    /**
     * The name of InfluxDb series for "authentication session created" event.
     */
    private static final String AUTH_SESSION_CREATED = "auth_session_created";

    /**
     * The names of the columns of an "authentication session created" event.
     */
    public static final String[] AUTHENTICATION_SESSION_COLUMNS
            = new String[]
            {
                    EventFactory.AUTH_SESSION_ID_KEY,
                    EventFactory.USER_IDENTITY_KEY,
                    EventFactory.MACHINE_UID_KEY,
                    EventFactory.AUTH_PROPERTIES_KEY
            };

    /**
     * The name of InfluxDb series for "authentication session" destroyed event.
     */
    private static final String AUTH_SESSION_DESTROYED
            = "auth_session_destroyed";

    /**
     * The names of the columns of an "authentication session destroyed" event.
     */
    public static final String[] AUTH_SESSION_DESTROYED_COLUMNS
            = new String[]
            {
                    EventFactory.AUTH_SESSION_ID_KEY
            };

    /**
     * The name of InfluxDb series for "endpoint authenticated" event.
     */
    private static final String ENDPOINT_AUTHENTICATED
            = "endpoint_authenticated";

    /**
     * The names of the columns of a "" event.
     */
    public static final String[] ENDPOINT_AUTHENTICATED_COLUMNS
            = new String[]
            {
                    EventFactory.AUTH_SESSION_ID_KEY,
                    EventFactory.FOCUS_ID_KEY,
                    EventFactory.ENDPOINT_ID_KEY
            };

    /**
     * The name of InfluxDb series for "focus created" event.
     */
    private static final String FOCUS_CREATED = "focus_created";

    /**
     * The names of the columns of a "focus created" event.
     */
    public static final String[] FOCUS_CREATED_COLUMNS
            = new String[]
            {
                    EventFactory.FOCUS_ID_KEY,
                    EventFactory.ROOM_JID_KEY
            };

    /**
     * The name of InfluxDb series for "focus destroyed" event.
     */
    private static final String FOCUS_DESTROYED = "focus_destroyed";

    /**
     * The names of the columns of a "focus destroyed" event.
     */
    public static final String[] FOCUS_DESTROYED_COLUMNS
            = new String[]
            {
                    EventFactory.FOCUS_ID_KEY,
                    EventFactory.ROOM_JID_KEY
            };

    /**
     * Initializes a new <tt>LoggingHandler</tt> instance. Exposes the
     * constructor as public.
     */
    public LoggingHandler(ConfigurationService cfg)
        throws Exception
    {
        super(cfg);
    }

    @Override
    public void handleEvent(Event event)
    {
        if (event == null)
            return;

        String topic = event.getTopic();
        if (EventFactory.CONFERENCE_ROOM_TOPIC.equals(topic))
        {
            conferenceRoom(event.getProperty(EventFactory.CONFERENCE_ID_KEY),
                           event.getProperty(EventFactory.ROOM_JID_KEY),
                           event.getProperty(EventFactory.FOCUS_ID_KEY),
                           event.getProperty(EventFactory.BRIDGE_JID_KEY));

        }
        if (EventFactory.CONFERENCE_ROOM_TOPIC.equals(topic))
        {
        	endPointDisplayNameWithRoomName(event.getProperty(EventFactory.CONFERENCE_ID_KEY),
                    						event.getProperty(EventFactory.ENDPOINT_ID_KEY),
                    	                    event.getProperty(EventFactory.DISPLAY_NAME_KEY),
                    	                    event.getProperty(EventFactory.ROOM_JID_KEY));
        			
        			
        }
        
        else if (EventFactory.PEER_CONNECTION_STATS_TOPIC.equals(topic))
        {
            logEvent(
                (InfluxDBEvent) event.getProperty(EventFactory.EVENT_SOURCE));

        }
        else if (EventFactory.ENDPOINT_DISPLAY_NAME_CHANGED_TOPIC.equals(topic))
        {
            endpointDisplayNameChanged(
                    event.getProperty(EventFactory.CONFERENCE_ID_KEY),
                    event.getProperty(EventFactory.ENDPOINT_ID_KEY),
                    event.getProperty(EventFactory.DISPLAY_NAME_KEY));
        }
        else if (EventFactory.FOCUS_CREATED_TOPIC.equals(topic))
        {
            logEvent(new InfluxDBEvent(
                FOCUS_CREATED,
                FOCUS_CREATED_COLUMNS,
                new Object[]
                    {
                        event.getProperty(EventFactory.FOCUS_ID_KEY),
                        event.getProperty(EventFactory.ROOM_JID_KEY)
                    }
            ));
        }
        else if (EventFactory.FOCUS_DESTROYED_TOPIC.equals(topic))
        {
            logEvent(new InfluxDBEvent(
                FOCUS_DESTROYED,
                FOCUS_DESTROYED_COLUMNS,
                new Object[]
                    {
                        event.getProperty(EventFactory.FOCUS_ID_KEY),
                        event.getProperty(EventFactory.ROOM_JID_KEY)
                    }
            ));
        }
        else if (EventFactory.AUTH_SESSION_CREATED_TOPIC.equals(topic))
        {
            logEvent(new InfluxDBEvent(
                AUTH_SESSION_CREATED,
                AUTHENTICATION_SESSION_COLUMNS,
                new Object[]
                    {
                        event.getProperty(EventFactory.AUTH_SESSION_ID_KEY),
                        event.getProperty(EventFactory.USER_IDENTITY_KEY),
                        event.getProperty(EventFactory.MACHINE_UID_KEY),
                        event.getProperty(EventFactory.AUTH_PROPERTIES_KEY)
                    }
            ));
        }
        else if (EventFactory.AUTH_SESSION_DESTROYED_TOPIC.equals(topic))
        {
            logEvent(new InfluxDBEvent(
                AUTH_SESSION_DESTROYED,
                AUTH_SESSION_DESTROYED_COLUMNS,
                new Object[]
                    {
                        event.getProperty(EventFactory.AUTH_SESSION_ID_KEY)
                    }
            ));
        }
        else if (EventFactory.ENDPOINT_AUTHENTICATED_TOPIC.equals(topic))
        {
            logEvent(new InfluxDBEvent(
                ENDPOINT_AUTHENTICATED,
                ENDPOINT_AUTHENTICATED_COLUMNS,
                new Object[]
                    {
                        event.getProperty(EventFactory.AUTH_SESSION_ID_KEY),
                        event.getProperty(EventFactory.FOCUS_ID_KEY),
                        event.getProperty(EventFactory.ENDPOINT_ID_KEY)
                    }
            ));
        }
        else
        {
            super.handleEvent(event);
        }
    }

    private void endPointDisplayNameWithRoomName(Object conferenceID,
    		Object endpointId,
            Object displayName, Object roomJid) {
		
    	logEvent(new InfluxDBEvent("endpoint_room",
    			ENDPOINT_DISPLAY_NAME_ROOM_NAME_COLUMNS,
                new Object[]
                        {
    						conferenceID,
    						endpointId,
    						displayName,
    						roomJid
                        }));
		
	}

	private void conferenceRoom(
            Object conferenceId,
            Object roomJid,
            Object focus,
            Object bridgeJid)
    {
        logEvent(new InfluxDBEvent("conference_room",
                                   CONFERENCE_ROOM_COLUMNS,
                                   new Object[]
                                           {
                                               conferenceId,
                                               roomJid,
                                               focus,
                                               bridgeJid
                                           }));
    }

    private void endpointDisplayNameChanged(Object conferenceId,
                                            Object endpointId,
                                            Object displayName)
    {
        logEvent(new InfluxDBEvent("endpoint_display_name",
                                   ENDPOINT_DISPLAY_NAME_COLUMNS,
                                   new Object[]
                                           {
                                                   conferenceId,
                                                   endpointId,
                                                   displayName
                                           }));
    }
}
