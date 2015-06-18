/*
 * Jicofo, the Jitsi Conference Focus.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.jicofo;

import net.java.sip.communicator.impl.protocol.jabber.extensions.colibri.*;
import net.java.sip.communicator.impl.protocol.jabber.extensions.rayo.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.Logger;

import org.jitsi.impl.protocol.xmpp.extensions.*;
import org.jitsi.jicofo.log.*;
import org.jitsi.protocol.xmpp.*;
import org.jitsi.util.*;

import org.jitsi.videobridge.eventadmin.*;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.packet.Message; //disambiguation
import org.jivesoftware.smack.provider.*;
import org.jivesoftware.smackx.packet.*;

/**
 * Class handles various Jitsi Meet extensions IQs like {@link MuteIq} and
 * Colibri for recording.
 *
 * @author Pawel Domas
 * @author Boris Grozev
 */
public class MeetExtensionsHandler
    implements PacketFilter,
               PacketListener
{
    /**
     * The logger
     */
    private final static Logger logger
        = Logger.getLogger(MeetExtensionsHandler.class);

    /**
     * Parent conference.
     */
    private final JitsiMeetConference conference;

    /**
     * Operation set that provider XMPP connection.
     */
    private OperationSetDirectSmackXmpp smackXmpp;

    /**
     * Creates new instance of {@link MeetExtensionsHandler}.
     * @param conference parent conference for which newly created instance
     *                   will be listening for service extensions packets.
     */
    public MeetExtensionsHandler(JitsiMeetConference conference)
    {
        this.conference = conference;

        MuteIqProvider muteIqProvider = new MuteIqProvider();
        muteIqProvider.registerMuteIqProvider(
            ProviderManager.getInstance());

        RayoIqProvider rayoIqProvider = new RayoIqProvider();
        rayoIqProvider.registerRayoIQs(
                ProviderManager.getInstance());
    }

    /**
     * Initializes this instance and bind packet listeners.
     */
    public void init()
    {
        this.smackXmpp
            = conference.getXmppProvider().getOperationSet(
                    OperationSetDirectSmackXmpp.class);

        smackXmpp.addPacketHandler(this, this);
    }

    /**
     * Disposes this instance and stop listening for extensions packets.
     */
    public void dispose()
    {
        if (smackXmpp != null)
        {
            smackXmpp.removePacketHandler(this);
            smackXmpp = null;
        }
    }

    @Override
    public boolean accept(Packet packet)
    {
        return acceptMuteIq(packet)
                || acceptColibriIQ(packet)
                || acceptRayoIq(packet)
                || acceptMessage(packet)
                || acceptPresence(packet);
    }

    @Override
    public void processPacket(Packet packet)
    {
        if (smackXmpp == null)
        {
            logger.error("Not initialized");
            return;
        }

        if (packet instanceof ColibriConferenceIQ)
        {
            handleColibriIq((ColibriConferenceIQ) packet);
        }
        else if (packet instanceof MuteIq)
        {
            handleMuteIq((MuteIq) packet);
        }
        else if (packet instanceof RayoIqProvider.DialIq)
        {
            handleRayoIQ((RayoIqProvider.DialIq) packet);
        }
        else if (packet instanceof Message)
        {
            handleMessage((Message) packet);
        }
        else if (packet instanceof Presence)
        {
            handlePresence((Presence) packet);
        }
        else
        {
            logger.error("Unexpected packet: " + packet.toXML());
        }
    }

    private boolean acceptColibriIQ(Packet packet)
    {
        String bridgeJid = conference.getServices().getVideobridge();
        return packet instanceof ColibriConferenceIQ
            // We're interested in packets from outside the world and not the JVB
            && (bridgeJid == null || !bridgeJid.equals(packet.getFrom()))
            // And with recording element
            && ((ColibriConferenceIQ)packet).getRecording() != null;
    }

    private void handleColibriIq(ColibriConferenceIQ colibriIQ)
    {
        ColibriConferenceIQ.Recording recording = colibriIQ.getRecording();

        boolean recordingState =
            conference.modifyRecordingState(
                colibriIQ.getFrom(),
                recording.getToken(),
                recording.getState(),
                recording.getDirectory());

        ColibriConferenceIQ response = new ColibriConferenceIQ();

        response.setType(IQ.Type.RESULT);
        response.setPacketID(colibriIQ.getPacketID());
        response.setTo(colibriIQ.getFrom());
        response.setFrom(colibriIQ.getTo());

        response.setRecording(
            new ColibriConferenceIQ.Recording(recordingState));

        smackXmpp.getXmppConnection().sendPacket(response);
    }

    private boolean acceptMuteIq(Packet packet)
    {
        return packet instanceof MuteIq;
    }

    private void handleMuteIq(MuteIq muteIq)
    {
        Boolean doMute = muteIq.getMute();
        String jid = muteIq.getJid();

        if (doMute == null || StringUtils.isNullOrEmpty(jid))
            return;

        IQ result;

        if (conference.handleMuteRequest(muteIq.getFrom(), jid, doMute))
        {
            result = IQ.createResultIQ(muteIq);

            if (!muteIq.getFrom().equals(jid))
            {
                MuteIq muteStatusUpdate = new MuteIq();
                muteStatusUpdate.setType(IQ.Type.SET);
                muteStatusUpdate.setTo(jid);

                muteStatusUpdate.setMute(doMute);

                smackXmpp.getXmppConnection().sendPacket(muteStatusUpdate);
            }
        }
        else
        {
            result = IQ.createErrorResponse(
                muteIq,
                new XMPPError(XMPPError.Condition.interna_server_error));
        }

        smackXmpp.getXmppConnection().sendPacket(result);
    }

    private boolean acceptRayoIq(Packet p)
    {
        return p instanceof RayoIqProvider.DialIq;
    }

    private void handleRayoIQ(RayoIqProvider.DialIq dialIq)
    {
        String initiatorJid = dialIq.getFrom();

        ChatRoomMemberRole role = conference.getRoleForMucJid(initiatorJid);

        if (role == null)
        {
            // Only room members are allowed to send requests
            IQ error = createErrorResponse(
                dialIq, new XMPPError(XMPPError.Condition.forbidden));

            smackXmpp.getXmppConnection().sendPacket(error);

            return;
        }

        if (ChatRoomMemberRole.MODERATOR.compareTo(role) < 0)
        {
            // Moderator permission is required
            IQ error = createErrorResponse(
                dialIq, new XMPPError(XMPPError.Condition.not_allowed));

            smackXmpp.getXmppConnection().sendPacket(error);

            return;
        }

        // Check if Jigasi is available
        String jigasiJid = conference.getServices().getSipGateway();

        if (StringUtils.isNullOrEmpty(jigasiJid))
        {
            // Not available
            IQ error = createErrorResponse(
                dialIq, new XMPPError(XMPPError.Condition.service_unavailable));

            smackXmpp.getXmppConnection().sendPacket(error);

            return;
        }

        // Redirect original request to Jigasi component
        String originalPacketId = dialIq.getPacketID();

        dialIq.setFrom(null);
        dialIq.setTo(jigasiJid);
        dialIq.setPacketID(IQ.nextID());

        IQ reply
            = (IQ) smackXmpp.getXmppConnection().sendPacketAndGetReply(dialIq);

        // Send Jigasi response back to the client
        reply.setFrom(null);
        reply.setTo(initiatorJid);
        reply.setPacketID(originalPacketId);

        smackXmpp.getXmppConnection().sendPacket(reply);
    }

    private boolean acceptMessage(Packet packet)
    {
        if (packet != null && packet instanceof Message)
        {
            for (PacketExtension pe : packet.getExtensions())
                if (pe instanceof LogPacketExtension)
                    return true;
        }
        return false;
    }

    /**
     * Handles "message" stanzas.
     */
    private void handleMessage(Message message)
    {
        for (PacketExtension ext : message.getExtensions())
            if (ext instanceof LogPacketExtension)
                handleLogRequest((LogPacketExtension) ext, message.getFrom());
    }

    /**
     * Handles XEP-0337 "log" extensions.
     */
    private void handleLogRequest(LogPacketExtension log, String jid)
    {
        Participant participant = conference.findParticipantForRoomJid(jid);
        if (participant != null)
        {
            EventAdmin eventAdmin = FocusBundleActivator.getEventAdmin();
            if (eventAdmin != null)
            {
                if (LogUtil.LOG_ID_PC_STATS.equals(log.getID()))
                {
                    String content = LogUtil.getContent(log);
                    if (content != null)
                    {
                        Event event =
                            EventFactory.peerConnectionStats(
                                conference.getColibriConference().getConferenceId(),
                                participant.getEndpointId(),
                                content);
                        if (event != null)
                            eventAdmin.sendEvent(event);
                    }
                }
                else
                {
                    if (logger.isInfoEnabled())
                        logger.info("Ignoring log request with an unknown ID:"
                                            + log.getID());
                }
            }
        }
        else
        {
            logger.info("Ignoring log request from an unknown JID: " + jid);
        }
    }

    private boolean acceptPresence(Packet packet)
    {
        return packet instanceof Presence;
    }

    /**
     * Handles presence stanzas
     * @param presence
     */
    private void handlePresence(Presence presence)
    {
        // unavailable is sent when user leaves the room
        if (!presence.isAvailable())
        {
            return;
        }

        Participant participant
                = conference.findParticipantForRoomJid(presence.getFrom());
        if (participant != null)
        {
            // Check if this conference is valid
            String conferenceId
                = conference.getColibriConference().getConferenceId();
            if (StringUtils.isNullOrEmpty(conferenceId))
            {
                logger.error(
                    "Unable to send DisplayNameChanged event" +
                            " - no conference id");
                return;
            }

            // Check for changes to the display name
            String oldDisplayName = participant.getDisplayName();
            String newDisplayName = null;
            for (PacketExtension pe : presence.getExtensions())
            {
                if (pe instanceof Nick)
                {
                    newDisplayName = ((Nick) pe).getName();
                    break;
                }
            }

            if ((oldDisplayName == null && newDisplayName != null)
                || (oldDisplayName != null
                        && !oldDisplayName.equals(newDisplayName)))
            {
                participant.setDisplayName(newDisplayName);

                // Prevent NPE when adding to event hashtable
                if (newDisplayName == null)
                {
                    newDisplayName = "";
                }
                EventAdmin eventAdmin = FocusBundleActivator.getEventAdmin();
                if (eventAdmin != null)
                {
                    eventAdmin.sendEvent(
                        EventFactory.endpointDisplayNameChanged(
                            conferenceId,
                            participant.getEndpointId(),
                            newDisplayName));
                }
            }
        }

    }

    /**
     * FIXME: replace with IQ.createErrorResponse
     * Prosody does not allow to include request body in error
     * response. Replace this method with IQ.createErrorResponse once fixed.
     */
    private IQ createErrorResponse(IQ request, XMPPError error)
    {
        if (!(request.getType() == IQ.Type.GET
                || request.getType() == IQ.Type.SET))
        {
            throw new IllegalArgumentException(
                "IQ must be of type 'set' or 'get'. Original IQ: "
                        + request.toXML());
        }
        final IQ result = new IQ()
        {
            public String getChildElementXML()
            {
                return "";
            }
        };
        result.setType(IQ.Type.ERROR);
        result.setPacketID(request.getPacketID());
        result.setFrom(request.getTo());
        result.setTo(request.getFrom());
        result.setError(error);
        return result;
    }
}
