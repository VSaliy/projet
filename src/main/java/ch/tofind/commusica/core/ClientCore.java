package ch.tofind.commusica.core;

import ch.tofind.commusica.database.DatabaseManager;
import ch.tofind.commusica.file.FileManager;
import ch.tofind.commusica.media.EphemeralPlaylist;
import ch.tofind.commusica.media.Player;
import ch.tofind.commusica.media.Track;
import ch.tofind.commusica.network.MulticastClient;
import ch.tofind.commusica.network.NetworkProtocol;
import ch.tofind.commusica.network.UnicastClient;
import ch.tofind.commusica.playlist.PlaylistManager;
import ch.tofind.commusica.session.ServerSessionManager;
import ch.tofind.commusica.ui.UIController;
import ch.tofind.commusica.utils.Logger;
import ch.tofind.commusica.utils.Serialize;
import javafx.application.Platform;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.TagException;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.hibernate.Session;
import org.hibernate.query.Query;
/**
 * This class represents the client side of the application.
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

    //! Track to send to the server
    private Track trackToSend;

    //! The manager that saves availables servers.
    private ServerSessionManager serverSessionManager;

    //! Thread pool for the unicast clients
    private ExecutorService threadPool;

    /**
     * Setup the core as a client.
     */
    public ClientCore() {

        multicast = new MulticastClient(NetworkProtocol.MULTICAST_ADDRESS, NetworkProtocol.MULTICAST_PORT, NetworkProtocol.interfaceToUse);
        new Thread(multicast).start();

        serverSessionManager = ServerSessionManager.getInstance();

        serverSessionManager.clear();

        this.threadPool = Executors.newCachedThreadPool();
    }

    /**
     * Method which is invoked when the server sends an END_OF_COMMUNICATION command
     *
     * @param args Args of the command.
     *             
     * @return An empty String.
     */
    public String END_OF_COMMUNICATION(ArrayList<Object> args) {
        LOG.info("End of communication client side.");
        return "";
    }

    /**
     * Method invoked when the server sends a PLAYLIST_UPDATE command
     * it has two purposes:
     * -Update the client playlist if the server who sends the update is the one saved by the client
     * -Update the available servers list of the client
     *
     * @param args Args of the command.
     *
     * @return An empty String.
     */
    public String PLAYLIST_UPDATE(ArrayList<Object> args) {

        Integer serverId = Integer.valueOf((String) args.remove(0));
        InetAddress serverAddress = Serialize.unserialize((String) args.remove(0), InetAddress.class);
        String serverName = (String) args.remove(0);
        String playlistJson = (String) args.remove(0);

        LOG.info("Receiving playlist: " + playlistJson);

        if (Objects.equals(serverId, ApplicationProtocol.serverId)) {
            // Delegate work to JavaFX thread.
            Platform.runLater(() -> {
                Serialize.unserialize(playlistJson, EphemeralPlaylist.class);

                // Refresh playlist at each reception.
                PlaylistManager.getInstance().getPlaylist().save();
                UIController.getController().refreshPlaylist();
                UIController.getController().refreshPlaylistsList();
            });
        }

        // We add the server to the available servers list
        serverSessionManager.store(serverId, serverAddress, serverName);

        return "";
    }

    /**
     * Entry point to notify the server the client want to connect to it. It sends the NEW_ACTIVE_CLIENT command
     * by Unicast to the server chosen.
     *
     * @param args Args of the command.
     *
     * @return An empty String
     */
    public String SEND_FIRST_CONNECTION(ArrayList<Object> args) {
        LOG.info("Dans SEND_FIRST_CONNECTION");

        String command = ApplicationProtocol.NEW_ACTIVE_CLIENT + NetworkProtocol.END_OF_LINE +
                ApplicationProtocol.myId + NetworkProtocol.END_OF_LINE +
                NetworkProtocol.END_OF_COMMAND;

        sendUnicast(ApplicationProtocol.serverAddress, command);

        return "";
    }
    
    /**
     * Entry point to ask the server for a track request. This method does the first check to ensure
     * the track is in a supported format and then send the command TRACK_REQUEST with all the
     * information available about the track.
     * It also setup the Unicast client for the time of the "transaction" since the UnicastCLient
     * isn't set anywhere in the client.
     *
     * @param args Args of the command.
     *
     * @return An empty String if the checks are good, the command END_OF_COMMUNICATION otherwise.
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

        try {
            trackToSend = new Track(AudioFileIO.read(fileToSend));
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

        String trackToSendJson = Serialize.serialize(trackToSend);

        String command = ApplicationProtocol.TRACK_REQUEST + NetworkProtocol.END_OF_LINE +
                ApplicationProtocol.myId + NetworkProtocol.END_OF_LINE +
                trackToSendJson + NetworkProtocol.END_OF_LINE +
                NetworkProtocol.END_OF_COMMAND;

        sendUnicast(ApplicationProtocol.serverAddress, command);

        return "";
    }

    /**
     * Method invoked when the server sends the TRACK_ACCEPTED command. It is in this method that
     * the file is sent by Unicast after the command SENDING_TRACK was sent to the server to make
     * it go into file reception mode.
     *
     * @param args Args of the command.
     *
     * @return An empty String.
     */
    public String TRACK_ACCEPTED(ArrayList<Object> args) {

        LOG.info("In TRACK_ACCEPTED");

        String trackToSendJson = Serialize.serialize(trackToSend);

        String result = ApplicationProtocol.SENDING_TRACK + NetworkProtocol.END_OF_LINE +
                ApplicationProtocol.myId + NetworkProtocol.END_OF_LINE +
                fileToSend.length() + NetworkProtocol.END_OF_LINE +
                trackToSendJson + NetworkProtocol.END_OF_LINE +
                NetworkProtocol.END_OF_COMMAND;

        client.send(result);
        client.send(fileToSend);
        return "";
    }

    /**
     * Method invoked when the server sends the TRACK_REFUSED command. It can happen if the
     * track was already on the server or in the database.
     *
     * @param args Args of the command.
     * 
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
     * Method invoked when the server sends the TRACK_SAVED command. It notify that the
     * track was saved in the server side.
     *
     * @param args Args of the command.
     * 
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
     * Entry point to send the PLAY_PAUSE_REQUEST command.
     *
     * @param args Args of the command.
     *
     * @return PLAY_PAUSE_REQUEST command.
     */
    public String SEND_PLAY_PAUSE_REQUEST(ArrayList<Object> args) {

        LOG.info("Ask the server for to play/pause the current track.");

        String result = ApplicationProtocol.PLAY_PAUSE_REQUEST + NetworkProtocol.END_OF_LINE +
                ApplicationProtocol.myId + NetworkProtocol.END_OF_LINE +
                NetworkProtocol.END_OF_COMMAND;

        sendUnicast(ApplicationProtocol.serverAddress, result);

        return "";
    }

    /**
     * Entry point to send the NEXT_TRACK_REQUEST command.
     *
     * @param args Args of the command.
     *
     * @return NEXT_TRACK_REQUEST command.
     */
    public String SEND_NEXT_TRACK_REQUEST(ArrayList<Object> args) {

        LOG.info("Ask the server for the next song.");

        String result = ApplicationProtocol.NEXT_TRACK_REQUEST + NetworkProtocol.END_OF_LINE +
                ApplicationProtocol.myId + NetworkProtocol.END_OF_LINE +
                NetworkProtocol.END_OF_COMMAND;

        sendUnicast(ApplicationProtocol.serverAddress, result);

        return "";
    }

    /**
     * Entry point to send the PREVIOUS_TRACK_REQUEST command.
     *
     * @param args Args of the command.
     *
     * @return PREVIOUS_TRACK_REQUEST command.
     */
    public String SEND_PREVIOUS_TRACK_REQUEST(ArrayList<Object> args) {

        LOG.info("Ask the server for to play/pause the current track.");

        String result = ApplicationProtocol.PREVIOUS_TRACK_REQUEST + NetworkProtocol.END_OF_LINE +
                ApplicationProtocol.myId + NetworkProtocol.END_OF_LINE +
                NetworkProtocol.END_OF_COMMAND;

        sendUnicast(ApplicationProtocol.serverAddress, result);

        return "";
    }

    /**
     * Entry point to send the TURN_VOLUME_UP_REQUEST command.
     *
     * @param args Args of the command.
     *
     * @return TURN_VOLUME_UP_REQUEST command.
     */
    public String SEND_TURN_VOLUME_UP_REQUEST(ArrayList<Object> args) {

        LOG.info("Ask the server to turn up the volume.");

        String result = ApplicationProtocol.TURN_VOLUME_UP_REQUEST + NetworkProtocol.END_OF_LINE +
                ApplicationProtocol.myId + NetworkProtocol.END_OF_LINE +
                NetworkProtocol.END_OF_COMMAND;

        sendUnicast(ApplicationProtocol.serverAddress, result);

        return "";
    }

    /**
     * Entry point to send the TURN_VOLUME_DOWN_REQUEST command.
     *
     * @param args Args of the command.
     *
     * @return TURN_VOLUME_DOWN_REQUEST command.
     */
    public String SEND_TURN_VOLUME_DOWN_REQUEST(ArrayList<Object> args) {

        LOG.info("Ask the next song to the server.");

        String result = ApplicationProtocol.TURN_VOLUME_DOWN_REQUEST + NetworkProtocol.END_OF_LINE +
                ApplicationProtocol.myId + NetworkProtocol.END_OF_LINE +
                NetworkProtocol.END_OF_COMMAND;

        sendUnicast(ApplicationProtocol.serverAddress, result);

        return "";
    }

    /**
     * Entry point to send the UPVOTE_TRACK_REQUEST command. It retrieves by the args
     * the id of the Track to upvote and sends it to the server.
     *
     * @param args Args of the command.
     *
     * @return UPVOTE_TRACK_REQUEST command with the track id
     */
    public String SEND_UPVOTE_TRACK_REQUEST(ArrayList<Object> args) {
        LOG.info("In SEND_UPVOTE_TRACK_REQUEST");

        String trackToDownvoteId = (String) args.remove(0);

        String result = ApplicationProtocol.UPVOTE_TRACK_REQUEST + NetworkProtocol.END_OF_LINE +
                ApplicationProtocol.myId + NetworkProtocol.END_OF_LINE +
                trackToDownvoteId + NetworkProtocol.END_OF_LINE +
                NetworkProtocol.END_OF_COMMAND;

        sendUnicast(ApplicationProtocol.serverAddress, result);

        return "";
    }

    /**
     * Entry point to send the DOWNVOTE_TRACK_REQUEST command. It retrieves by the args
     * the id of the Track to downvote and sends it to the server.
     *
     * @param args Args of the command.
     *
     * @return DOWNVOTE_TRACK_REQUEST command with the track id
     */
    public String SEND_DOWNVOTE_TRACK_REQUEST(ArrayList<Object> args) {
        LOG.info("In SEND_DOWNVOTE_TRACK_REQUEST");
        String trackToDownvoteId = (String) args.remove(0);

        String result = ApplicationProtocol.DOWNVOTE_TRACK_REQUEST + NetworkProtocol.END_OF_LINE +
                ApplicationProtocol.myId + NetworkProtocol.END_OF_LINE +
                trackToDownvoteId + NetworkProtocol.END_OF_LINE +
                NetworkProtocol.END_OF_COMMAND;

        sendUnicast(ApplicationProtocol.serverAddress, result);

        return "";
    }

    /**
     * Method invoked when the server sends the TRACK_UPVOTED command. It notify that the
     * track was updated in the server side.
     *
     * @param args Args of the command.
     *
     * @return END_OF_COMMUNICATION command
     */
    public String TRACK_UPVOTED(ArrayList<Object> args) {
        LOG.info("In TRACK_UPVOTED");

        String result = NetworkProtocol.END_OF_COMMUNICATION + NetworkProtocol.END_OF_LINE +
                ApplicationProtocol.myId + NetworkProtocol.END_OF_LINE +
                NetworkProtocol.END_OF_COMMAND;
        return result;
    }

    /**
     * Method invoked when the server sends the TRACK_DOWNVOTED command. It notify that the
     * track was updated in the server side.
     *
     * @param args Args of the command.
     *
     * @return END_OF_COMMUNICATION command
     */
    public String TRACK_DOWNVOTED(ArrayList<Object> args) {
        LOG.info("In TRACK_DOWNVOTED");

        String result = NetworkProtocol.END_OF_COMMUNICATION + NetworkProtocol.END_OF_LINE +
                ApplicationProtocol.myId + NetworkProtocol.END_OF_LINE +
                NetworkProtocol.END_OF_COMMAND;
        return result;
    }

    /**
     * Method invoked when the server sends the ERROR command. It notify that an ERROR occurred on
     * the server side
     *
     * @param args Args of the command.
     *
     * @return END_OF_COMMUNICATION command
     */
    public String ERROR(ArrayList<Object> args) {
        LOG.info("In ERROR");

        // remove the id
        args.remove(0);
        // remove the socket
        args.remove(0);

        String errorMessage = (String) args.remove(0);

        LOG.warning(errorMessage);

        String result = NetworkProtocol.END_OF_COMMUNICATION + NetworkProtocol.END_OF_LINE +
                ApplicationProtocol.myId + NetworkProtocol.END_OF_LINE +
                NetworkProtocol.END_OF_COMMAND;
        return result;
    }

    /**
     * Method invoked when the server sends the SUCCESS command. It notify that an action
     * succeed.
     *
     * @param args Args of the command.
     *
     * @return END_OF_COMMUNICATION command
     */
    public String SUCCESS(ArrayList<Object> args) {
        LOG.info("In SUCESS");

        // remove the id
        args.remove(0);
        // remove the socket
        args.remove(0);

        String successMessage = (String) args.remove(0);

        LOG.info(successMessage);

        String result = NetworkProtocol.END_OF_COMMUNICATION + NetworkProtocol.END_OF_LINE +
                ApplicationProtocol.myId + NetworkProtocol.END_OF_LINE +
                NetworkProtocol.END_OF_COMMAND;
        return result;
    }

    @Override
    public void sendUnicast(InetAddress hostname, String message) {

        // Check if a server has been set
        if (ApplicationProtocol.serverAddress != null) {

            client = new UnicastClient(hostname, NetworkProtocol.UNICAST_PORT);

            Thread threadedClient = new Thread(client);
            threadPool.submit(threadedClient);

            client.send(message);

        } else {
            LOG.warning("Server was not set !");
        }
    }

    @Override
    public void sendMulticast(String message) {
        multicast.send(message);
    }

    @Override
    public void stop() {
        // Stop the executors
        serverSessionManager.stop();

        // Stop the network elements
        multicast.stop();

        // Try to stop all remaining threads
        threadPool.shutdown();

        // Wait 5 seconds before killing everyone
        try {
            threadPool.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOG.error(e);
        } finally {
            if (!threadPool.isTerminated()) {
                LOG.error("The thread pool can't be stopped !");
            }
            threadPool.shutdownNow();
        }

        // Delete the unplayed tracks from the database
        Session session = DatabaseManager.getInstance().getSession();
        Query query = session.createQuery("DELETE Track WHERE date_played IS NULL");
        DatabaseManager.getInstance().execute(query);

        // Close the database connection
        DatabaseManager.getInstance().close();
    }

    @Override
    public boolean isServer() {
        return false;
    }

}