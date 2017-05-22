package ch.tofind.commusica.core;

import java.net.InetAddress;

/**
 * @brief This class represents the application protocol for communication.
 */
public final class ApplicationProtocol {

    //! The ID of the current instance.
    public static Integer myId = null;

    //! The server's ID with which we are currently connected to.
    public static Integer serverId = null;

    //! The server's address with which we are currently connected to.
    public static InetAddress serverAddress = null;

    //! The server's name with which we are currently connected to.
    public static String serverName = null;

    //! Commands
    public static final String SUCCESS = "SUCCESS";

    public static final String ERROR = "ERROR";

    public static final String SEND_FIRST_CONNECTION = "SEND_FIRST_CONNECTION";

    public static final String TRACK_REQUEST = "TRACK_REQUEST";

    public static final String TRACK_ACCEPTED = "TRACK_ACCEPTED";

    public static final String TRACK_REFUSED = "TRACK_REFUSED";

    public static final String TRACK_SAVED = "TRACK_SAVED";

    public static final String SENDING_TRACK = "SENDING_TRACK";

    public static final String SEND_UPVOTE_TRACK_REQUEST = "SEND_UPVOTE_TRACK_REQUEST";

    public static final String SEND_DOWNVOTE_TRACK_REQUEST = "SEND_DOWNVOTE_TRACK_REQUEST";

    public static final String UPVOTE_TRACK_REQUEST = "UPVOTE_TRACK_REQUEST";

    public static final String DOWNVOTE_TRACK_REQUEST = "DOWNVOTE_TRACK_REQUEST";

    public static final String TRACK_DOWNVOTED = "TRACK_DOWNVOTED";

    public static final String TRACK_UPVOTED = "TRACK_UPVOTED";

    public static final String PLAYLIST_UPDATE = "PLAYLIST_UPDATE";

    public static final String NEW_ACTIVE_CLIENT = "NEW_ACTIVE_CLIENT";

    public static final String SEND_PLAYLIST_UPDATE = "SEND_PLAYLIST_UPDATE";

    public static final String SEND_TRACK_REQUEST = "SEND_TRACK_REQUEST";

    public static final String SEND_NEXT_TRACK_REQUEST = "SEND_NEXT_TRACK_REQUEST";

    public static final String NEXT_TRACK_REQUEST = "NEXT_TRACK_REQUEST";

    public static final String SEND_TURN_VOLUME_UP_REQUEST = "SEND_TURN_VOLUME_UP_REQUEST";

    public static final String TURN_VOLUME_UP_REQUEST = "TURN_VOLUME_UP_REQUEST";

    public static final String SEND_TURN_VOLUME_DOWN_REQUEST = "SEND_TURN_VOLUME_DOWN_REQUEST";

    public static final String TURN_VOLUME_DOWN_REQUEST = "TURN_VOLUME_DOWN_REQUEST";

}
