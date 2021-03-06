package ch.tofind.commusica.core;

import ch.tofind.commusica.network.MulticastClient;
import ch.tofind.commusica.utils.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.ArrayList;

/**
 * This class represents an abstract core to execute code asked from the network.
 */
abstract class AbstractCore {

    //! Logger for debugging.
    private static final Logger LOG = new Logger(AbstractCore.class.getSimpleName());

    /**
     * Execute a command on the available core.
     *
     * @param command Command to execute.
     * @param args Args of the command.
     *
     * @return The output of the command.
     */
    public synchronized String execute(String command, ArrayList<Object> args) {

        String result = "";

        try {
            Method method = this.getClass().getMethod( command, ArrayList.class);
            result = (String) method.invoke(this, args);
        } catch (NoSuchMethodException e) {
            // Do nothing
        } catch (IllegalAccessException | InvocationTargetException e) {
            LOG.error(e);
        }

        return result;
    }

    /**
     * Send a request by unicast to the hostname.
     *
     * @param hostname IP address of the hostname.
     * @param message Message to send to the hostname.
     */
    abstract void sendUnicast(InetAddress hostname, String message);

    /**
     * Send a request by multicast.
     *
     * @param message Message to send.
     */
    abstract void sendMulticast(String message);

    /**
     * Stop the core.
     */
    abstract void stop();

    abstract boolean isServer();

}
