package ch.tofind.commusica.core;

import ch.tofind.commusica.file.FileManager;
import ch.tofind.commusica.media.Track;
import ch.tofind.commusica.network.MulticastClient;
import ch.tofind.commusica.network.NetworkProtocol;
import ch.tofind.commusica.network.UnicastClient;
import ch.tofind.commusica.session.ServerSessionManager;
import ch.tofind.commusica.utils.Logger;
import ch.tofind.commusica.utils.Serialize;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Objects;

import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.TagException;

/**
 * @brief This class represents the client side of the application.
 */
public class ClientCore extends AbstractCore implements ICore {

    //! Logger for debugging.
    private static final Logger LOG = new Logger(ClientCore.class.getSimpleName());

    //! Client to use for multicast.
    private MulticastClient multicast;

    //! Client to use for unicast.
    private UnicastClient client;

    //! File to send to the server.
    private File fileToSend;

    //! Server Session Manager.
    ServerSessionManager serverSessionManager;

    /**
     * @brief Setup the core as a client.
     *
     * @param multicastAddress Multicast address.
     * @param port Multicast port.
     * @param interfaceToUse Interface to use for the multicast.
     */
    public ClientCore(String multicastAddress, int port, InetAddress interfaceToUse) {
        multicast = new MulticastClient(multicastAddress, port, interfaceToUse);
        new Thread(multicast).start();

        serverSessionManager = ServerSessionManager.getInstance();
    }


    /**
     * @brief method which is invoked when the server sends an END_OF_COMMUNICATION command
     *
     * @param args
     * @return an empty String
     */
    public String END_OF_COMMUNICATION(ArrayList<Object> args) {
        System.out.println("End of communication client side.");
        return "";
    }


    /**
     * @brief Method invoked when the server sends a PLAYLIST_UPDATE command
     * it has two purposes:
     * -Update the client playlist if the server who sends the update is the one saved by the client
     * -Update the available servers list of the client
     *
     * @param args
     *
     * @return an empty String
     */
    public String PLAYLIST_UPDATE(ArrayList<Object> args) {

        Integer serverId = Integer.valueOf((String) args.remove(0));
        InetAddress serverAddress = Serialize.unserialize((String) args.remove(0), InetAddress.class);
        String serverName = (String) args.remove(0);
        String playlistJson = (String) args.remove(0);

        LOG.info("Playlist JSON: " + playlistJson);

        if (Objects.isNull(ApplicationProtocol.serverId)) {
            ApplicationProtocol.serverId = serverId;
            ApplicationProtocol.serverName = serverName;
            ApplicationProtocol.serverAddress = serverAddress;
        }

        if (Objects.equals(serverId, ApplicationProtocol.serverId)) {
            // TODO: fix this =(
            //EphemeralPlaylist playlistUpdated = Serialize.unserialize(playlistJson, EphemeralPlaylist.class);
            //PlaylistManager.getInstance().loadPlaylist(playlistUpdated);
        }

        // We add the server to the available servers list
        serverSessionManager.store(serverAddress, serverName, serverId);

        return "";
    }


    /**
     * Entry point to ask the server for a track request. This method does the first check to ensure
     * the track is in a supported format and then send the command TRACK_REQUES  with all the
     * information available about the track.
     * It also setup the Unicast client for the time of the "transaction" since the UnicastCLient
     * isn't set anywhere in the client.
     *
     * @param args
     *
     * @return an empty String if the checks are good, the command END_OF_COMMUNICATION otherwise.
     */
    public String SEND_TRACK_REQUEST(ArrayList<Object> args) {
        LOG.info("In SEND_TRACK_REQUEST");

        fileToSend = new File((String) args.get(0));

        FileManager fileManager = FileManager.getInstance();

        // Ends the communication if the extension is not found (format not supported)
        try {
            fileManager.getFormatExtension(fileToSend);
        } catch (Exception e) {
            LOG.error(e);

            return NetworkProtocol.END_OF_COMMUNICATION + NetworkProtocol.END_OF_LINE +
                    ApplicationProtocol.myId + NetworkProtocol.END_OF_LINE +
                    NetworkProtocol.END_OF_COMMAND;
        }

        Track track = null;
        try {
            track = new Track(AudioFileIO.read(fileToSend));
        } catch (CannotReadException e) {
            LOG.error(e);
        } catch (IOException e) {
            LOG.error(e);
        } catch (TagException e) {
            LOG.error(e);
        } catch (ReadOnlyFileException e) {
            LOG.error(e);
        } catch (InvalidAudioFrameException e) {
            LOG.error(e);
        }

        String trackJson = Serialize.serialize(track);

        String command = ApplicationProtocol.TRACK_REQUEST + NetworkProtocol.END_OF_LINE +
                ApplicationProtocol.myId + NetworkProtocol.END_OF_LINE +
                trackJson + NetworkProtocol.END_OF_LINE +
                NetworkProtocol.END_OF_COMMAND;

        sendUnicast(ApplicationProtocol.serverAddress, command);

        return "";
    }


    /**
     * @brief Method invoked when the server sends the TRACK_ACCEPTED command. It is in this method that
     * the file is sent by Unicast after the command SENDING_TRACK was sent to the server to make
     * it go into file reception mode.
     *
     * @param args
     *
     * @return an empty String
     */
    public String TRACK_ACCEPTED(ArrayList<Object> args) {
        LOG.info("In TRACK_ACCEPTED");

        String result = ApplicationProtocol.SENDING_TRACK + NetworkProtocol.END_OF_LINE +
                ApplicationProtocol.myId + NetworkProtocol.END_OF_LINE +
                fileToSend.length() + NetworkProtocol.END_OF_LINE +
                NetworkProtocol.END_OF_COMMAND;

        client.send(result);
        client.send(fileToSend);
        return "";
    }

    /**
     * @brief Method invoked when the server sends the TRACK_REFUSED command. It can happen if the
     * track was already on the server or in the database.
     *
     * @param args
     * @return END_OF_COMMUNICATION command
     */
    public String TRACK_REFUSED(ArrayList<Object> args) {
        LOG.info("In TRACK_REFUSED");

        String result = NetworkProtocol.END_OF_COMMUNICATION + NetworkProtocol.END_OF_LINE +
                ApplicationProtocol.myId + NetworkProtocol.END_OF_LINE +
                NetworkProtocol.END_OF_COMMAND;
        return result;
    }


    /**
     * @brief Method invoked when the server sends the TRACK_SAVED command. It notify that the
     * track was saved in the server side.
     *
     * @param args
     * @return END_OF_COMMUNICATION command
     */
    public String TRACK_SAVED(ArrayList<Object> args) {
        LOG.info("In TRACK_SAVED");

        String result = NetworkProtocol.END_OF_COMMUNICATION + NetworkProtocol.END_OF_LINE +
                ApplicationProtocol.myId + NetworkProtocol.END_OF_LINE +
                NetworkProtocol.END_OF_COMMAND;
        return result;
    }

    /**
     * @brief Entry point to send the UPVOTE_TRACK_REQUEST command. It retrieves by the args
     * the id of the Track to upvote and sends it to the server.
     *
     * @param args
     *
     * @return UPVOTE_TRACK_REQUEST command with the track id
     */
    public String UPVOTE_TRACK(ArrayList<Object> args) {
        String trackToUpvoteId = (String) args.remove(0);

        String result = ApplicationProtocol.UPVOTE_TRACK_REQUEST + NetworkProtocol.END_OF_LINE +
                ApplicationProtocol.myId + NetworkProtocol.END_OF_LINE +
                trackToUpvoteId + NetworkProtocol.END_OF_LINE +
                NetworkProtocol.END_OF_COMMAND;

        return result;
    }

    /**
     * @brief Entry point to send the DOWNVOTE_TRACK_REQUEST command. It retrieves by the args
     * the id of the Track to upvote and sends it to the server.
     *
     * @param args
     *
     * @return DOWNVOTE_TRACK_REQUEST command with the track id
     */
    public String DOWNVOTE_TRACK(ArrayList<Object> args) {
        String trackToDownvoteId = (String) args.remove(0);

        String result = ApplicationProtocol.DOWNVOTE_TRACK_REQUEST + NetworkProtocol.END_OF_LINE +
                ApplicationProtocol.myId + NetworkProtocol.END_OF_LINE +
                trackToDownvoteId + NetworkProtocol.END_OF_LINE +
                NetworkProtocol.END_OF_COMMAND;

        return result;
    }




    /**
     * @brief Entry point to set the UnicastClient and start the unicast communication.
     *
     * @param hostname IP address of the hostname.
     * @param message Message to send to the hostname.
     */
    @Override
    public void sendUnicast(InetAddress hostname, String message) {

        // Check if a server has been set
        if (ApplicationProtocol.serverAddress != null) {

            client = new UnicastClient(hostname, NetworkProtocol.UNICAST_PORT);
            new Thread(client).start();
            client.send(message);

        } else {
            LOG.error(new RuntimeException("Server was not set !"));
        }
    }


    /**
     * @brief sends a message by multicast to the multicast group set.
     *
     * @param message Message to send.
     */
    @Override
    public void sendMulticast(String message) {
        multicast.send(message);
    }

    /**
     * @brief Stop the MulticastClient
     */
    @Override
    public void stop() {
        multicast.stop();
    }
}
