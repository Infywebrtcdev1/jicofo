/*
 * Jicofo, the Jitsi Conference Focus.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jitsi.jicofo.util;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import org.jitsi.protocol.xmpp.*;

import java.util.*;

/**
 * Utility class for feature discovery.
 *
 * @author Pawel Domas
 */
public class DiscoveryUtil
{
    /**
     * The logger
     */
    private final static Logger logger 
        = Logger.getLogger(DiscoveryUtil.class);

    /**
     * Audio RTP feature name.  
     */
    public final static String FEATURE_AUDIO
            = "urn:xmpp:jingle:apps:rtp:audio";

    /**
     * Video RTP feature name.  
     */
    public final static String FEATURE_VIDEO
            = "urn:xmpp:jingle:apps:rtp:video";

    /**
     * ICE feature name.  
     */
    public final static String FEATURE_ICE
            = "urn:xmpp:jingle:transports:ice-udp:1";

    /**
     * DTLS/SCTP feature name.  
     */
    public final static String FEATURE_SCTP
            = "urn:xmpp:jingle:transports:dtls-sctp:1";

    /**
     * The Jingle DTLS feature name (XEP-0320).
     */
    public final static String FEATURE_DTLS = "urn:xmpp:jingle:apps:dtls:0";

    /**
     * RTCP mux feature name.  
     */
    public final static String FEATURE_RTCP_MUX = "urn:ietf:rfc:5761";

    /**
     * RTP bundle feature name. 
     */
    public final static String FEATURE_RTP_BUNDLE = "urn:ietf:rfc:5888";

    /**
     * Gets the list of features supported by participant. If we fail to 
     * obtain it due to network failure default feature list is returned. 
     * @param protocolProvider protocol provider service instance that will 
     *        be used for discovery.
     * @param address XMPP address of the participant.
     */
    public static List<String> discoverParticipantFeatures
        (ProtocolProviderService protocolProvider, String address)
    {
        OperationSetSimpleCaps disco 
            = protocolProvider.getOperationSet(OperationSetSimpleCaps.class);
        if (disco == null)
        {
            logger.error(
                "Service discovery not supported by " + protocolProvider);
            return getDefaultParticipantFeatureSet();
        }
        
        // Discover participant feature set
        List<String> participantFeatures = disco.getFeatures(address);
        if (participantFeatures == null)
        {
            logger.error(
                "Failed to discover features for "+ address 
                        + " assuming default feature set.");
            
            return getDefaultParticipantFeatureSet();
        }

        logger.info(address +", features: ");
        for (String feature : participantFeatures)
        {
            logger.info(feature);
        }

        return participantFeatures;
    }

    /**
     * Returns default participant feature set.
     */
    static public List<String> getDefaultParticipantFeatureSet()
    {
        ArrayList<String> features = new ArrayList<String>(4);
        features.add(FEATURE_AUDIO);
        features.add(FEATURE_VIDEO);
        features.add(FEATURE_ICE);
        features.add(FEATURE_SCTP);
        features.add(FEATURE_DTLS);
        return features;
    }

    /**
     * Checks if all of the features given on <tt>reqFeatures</tt> array exist
     * on declared list of <tt>capabilities</tt>.
     * @param reqFeatures array of required features to check.
     * @param capabilities the list of features supported by the client.
     * @return <tt>true</tt> if all features from <tt>reqFeatures</tt> array
     *         exist on <tt>capabilities</tt> list.
     */
    static public boolean checkFeatureSupport(String[] reqFeatures,
                                              List<String> capabilities)
    {
        for (String toCheck : reqFeatures)
        {
            if (!capabilities.contains(toCheck))
                return false;
        }
        return true;
    }

    /**
     * Returns <tt>true</tt> if <tt>list1</tt> and <tt>list2</tt> contain the
     * same elements where items order is not relevant.
     * @param list1 the first list of <tt>String</tt> to be compared against
     *              the second list.
     * @param list2 the second list of <tt>String</tt> to be compared against
     *              the first list.
     */
    static public boolean areTheSame(List<String> list1, List<String> list2)
    {
        return list1.size() == list2.size() && list2.containsAll(list1);
    }
}
