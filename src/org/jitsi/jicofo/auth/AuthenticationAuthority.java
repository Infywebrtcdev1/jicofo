/*
 * Jicofo, the Jitsi Conference Focus.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.jicofo.auth;

import org.jitsi.impl.protocol.xmpp.extensions.*;
import org.jivesoftware.smack.packet.*;

/**
 * FIXME work in progress, still have to integrate OAuth
 *
 * This interface is intended to encapsulate authorization method like
 * Shibboleth, OAuth or XMPP domain.
 *
 * @author Pawel Domas
 */
public interface AuthenticationAuthority
{
    /**
     * Creates the URL that should be visited by the user in order to login.
     *
     * @param machineUID unique identifier of the user's machine that will be
     *                   associated with the session created.
     * @param peerFullJid peer's JID that will be associated with the session
     *                    (but it's up to the implementation).
     * @param roomName name of the conference room in the form of
     *                 <room_name}@{muc_address}.
     * @param popup <tt>true</tt> if this URL will be opened in the popup
     *              (used for live authentication once the conference has
     *              been started).
     *
     * @return the URL that must be visited by the user in order to login to
     *         the authentication system.
     */
    String createLoginUrl(
            String machineUID, String peerFullJid,
            String roomName, boolean popup);

    /**
     * Process given <tt>ConferenceIq</tt> query in order to verify user's
     * authentication session and eventual permissions for creating new room.
     *
     * @param query the <tt>ConferenceIq</tt> requested that should be
     *              authenticated.
     * @param response the <tt>ConferenceIq</tt> response that will be
     *                 returned to the user. Implementing classes can fill
     *                 some information in order to describe authentication
     *                 session.
     * @return XMPP error if <tt>query</tt> has failed authentication process.
     */
    IQ processAuthentication(ConferenceIq query, ConferenceIq response);


    /**
     * Process given <tt>LogoutIq</tt> and eventually destroy
     * <tt>AuthenticationSession</tt> described in the request.
     *
     * @param iq the <tt>LogoutIq</tt> that described authentication session
     *           to be destroyed.
     *
     * @return XMPP error that will be sent to the user if we do not accept
     *         the request.
     */
    IQ processLogoutIq(LogoutIq iq);

    /**
     * Registers to the list of <tt>AuthenticationListener</tt>s.
     * @param l the <tt>AuthenticationListener</tt> to be added to listeners
     *          list.
     */
    void addAuthenticationListener(AuthenticationListener l);

    /**
     * Unregisters from the list of <tt>AuthenticationListener</tt>s.
     * @param l the <tt>AuthenticationListener</tt> that will be removed from
     *          authentication listeners list.
     */
    void removeAuthenticationListener(AuthenticationListener l);

    /**
     * Returns <tt>true</tt> if user is authenticated in given conference room.
     * @param jabberId the Jabber ID of the user to be verified.
     */
    boolean isUserAuthenticated(String jabberId);

    /**
     * Returns user login associated with given <tt>jabberId</tt>.
     * @param jabberId the Jabber ID of the connection for which we want to
     *                 get user's identity.
     * @return user login associated with given <tt>jabberId</tt> or
     *         <tt>null</tt>
     */
    String getUserIdentity(String jabberId);

    /**
     * Returns <tt>true</tt> if this is external authentication method that
     * requires user to visit authentication URL.
     */
    boolean isExternal();

    void start();

    void stop();
}
