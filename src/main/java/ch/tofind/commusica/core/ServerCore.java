package ch.tofind.commusica.core;

import ch.tofind.commusica.file.FileManager;
import ch.tofind.commusica.media.Playlist;
import ch.tofind.commusica.media.Track;
import ch.tofind.commusica.network.MulticastClient;
import ch.tofind.commusica.network.NetworkProtocol;
import ch.tofind.commusica.network.NetworkUtils;
import ch.tofind.commusica.network.Server;
import ch.tofind.commusica.playlist.PlaylistManager;
import ch.tofind.commusica.session.ServerSession;
import ch.tofind.commusica.utils.Network;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Map;

public class ServerCore extends AbstractCore implements ICore {

    //! Name of the server.
    String name;

    //! Multicast client to send commands via multicast.
    MulticastClient multicast;

    //! The server.
    Server server;

    //!
    private Gson json;

    //!
    private Track trackReceived;

    public ServerCore(String name, String multicastAddress, int multicastPort, InetAddress interfaceToUse, int unicastPort) {

        this.name = name;

        multicast = new MulticastClient(multicastAddress, multicastPort, interfaceToUse);

        server = new Server(unicastPort);

        new Thread(multicast).start();
        new Thread(server).start();

        json = new GsonBuilder().create();
    }

    public String END_OF_COMMUNICATION(ArrayList<Object> args) {
        System.out.println("End of communication server side.");
        return "";
    }

    public String SEND_PLAYLIST_UPDATE(ArrayList<Object> args) {


        String inetaddressJson = json.toJson(NetworkUtils.INTERFACE_TO_USE);
        String playlistJson = json.toJson(PlaylistManager.getInstance().getPlaylist());

        String command = ApplicationProtocol.PLAYLIST_UPDATE + NetworkProtocol.END_OF_LINE +
                ApplicationProtocol.myId + NetworkProtocol.END_OF_LINE +
                inetaddressJson + NetworkProtocol.END_OF_LINE +
                name + NetworkProtocol.END_OF_LINE +
                playlistJson + NetworkProtocol.END_OF_LINE +
                NetworkProtocol.END_OF_COMMAND;

        multicast.send(command);

        return "";
    }

    public String TRACK_REQUEST(ArrayList<Object> args) {

        System.out.println("In TRACK_REQUEST");
        trackReceived = json.fromJson((String)args.get(0), Track.class);
        String result = ApplicationProtocol.TRACK_ACCEPTED + NetworkProtocol.END_OF_LINE +
                ApplicationProtocol.myId + NetworkProtocol.END_OF_LINE +
                NetworkProtocol.END_OF_COMMAND;
        return result;
    }

    public String SEND_TRACK(ArrayList<Object> args) {

        System.out.println("In SEND_TRACK");
        // Delegate the job to the FileManager
        String URI = "";
        try {
            int fileSize = Integer.parseInt((String)args.get(2));
            System.out.println(fileSize);
            System.out.println("Delegating to FM");
            URI = FileManager.getInstance().retrieveFile(((Socket)args.get(1)).getInputStream(), fileSize);
            trackReceived.setUri(URI);
        } catch (IOException e) {
            e.printStackTrace();
        }

        trackReceived = null;

        String result = ApplicationProtocol.TRACK_SAVED + NetworkProtocol.END_OF_LINE +
                ApplicationProtocol.myId + NetworkProtocol.END_OF_LINE +
                NetworkProtocol.END_OF_COMMAND;
        return result;
    }

    @Override
    public String commandNotFound() {
        return END_OF_COMMUNICATION(null);
    }

    @Override
    public void sendUnicast(InetAddress hostname, String message) {

    }

    @Override
    public void sendMulticast(String message) {
        multicast.send(message);
    }

    @Override
    public void stop() {
        multicast.stop();
        server.stop();
    }
}
