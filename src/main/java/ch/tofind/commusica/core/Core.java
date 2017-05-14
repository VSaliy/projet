package ch.tofind.commusica.core;

import ch.tofind.commusica.network.NetworkProtocol;

import java.net.InetAddress;
import java.util.ArrayList;

public class Core implements ICore {

    //! Shared instance of the object for all the application.
    private static AbstractCore instance = null;

    //! Interface to use for Multicast.
    private InetAddress interfaceToUse;

    public static String execute(String command, ArrayList<Object> args) {
        return instance.execute(command, args);
    }

    public Core(InetAddress interfaceToUse) {
        this.interfaceToUse = interfaceToUse;
    }

    public AbstractCore getInstance() {
        return instance;
    }

    public void setupAsServer(String name) {
        instance = new ServerCore(name, NetworkProtocol.MULTICAST_ADDRESS, NetworkProtocol.MULTICAST_PORT, interfaceToUse, NetworkProtocol.UNICAST_PORT);
    }

    public void setupAsClient() {
        instance = new ClientCore(NetworkProtocol.MULTICAST_ADDRESS, NetworkProtocol.MULTICAST_PORT, interfaceToUse);
    }

    @Override
    public void sendUnicast(InetAddress hostname, String message) {
        instance.sendUnicast(hostname, message);
    }

    @Override
    public void sendMulticast(String message) {
        instance.sendMulticast(message);
    }

    @Override
    public void stop() {
        instance.stop();
    }

}
