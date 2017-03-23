package ch.tofind.commusica.network;

import java.io.*;
import java.net.*;
import java.sql.Timestamp;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * For now you a client can connet to the server (by using telnet for example), say its id (for test) and the session
 * will be created or not if the id is already stored
 * The management of obsolete sessions doesn't work well. It only runs one time.
 */

public class Server extends NetPort {
    private static int nextId = 0;
    private SessionManager sm = new SessionManager();

    /**
     * Constructor
     *
     * @param port the port to listen on
     */
    public Server(int port) {
        super(port);

    }

    /**
     * This method initiates the process. The server creates a socket and binds it
     * to the previously specified port. It then waits for clients in a infinite
     * loop.
     */
    public void serveClients() {
        LOG.info("Starting the Receptionist Worker on a new thread...");

        try {
            InetAddress address = InetAddress.getLocalHost();
            NetworkInterface nwi = NetworkInterface.getByInetAddress(address);
            byte mac[] = nwi.getHardwareAddress();
            for(byte b : mac) {
                System.out.printf("%02X:", b);
            }
        } catch (Exception e) {

        }

        ScheduledExecutorService scheduledExecutorService =
                Executors.newScheduledThreadPool(1);
        scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            public void run() {
                try {
                    sm.deleteObsoleteSessions();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        },0, 1, TimeUnit.SECONDS);

        new Thread(new ReceptionistWorker()).start();
    }


    /**
     * This inner class implements the behavior of the "receptionist", whose
     * responsibility is to listen for incoming connection requests. As soon as a
     * new client has arrived, the receptionist delegates the processing to a
     * "servant" who will execute on its own thread.
     */
    private class ReceptionistWorker implements Runnable {

        @Override
        public void run() {
            ServerSocket serverSocket;

            try {
                serverSocket = new ServerSocket(port);
            } catch (IOException ex) {
                LOG.log(Level.SEVERE, null, ex);
                return;
            }

            while (true) {
                LOG.log(Level.INFO, "Waiting (blocking) for a new client on port {0}", port);
                try {
                    Socket clientSocket = serverSocket.accept();

                    new Thread(new ServantWorker(clientSocket)).start();
                } catch (IOException ex) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        }


        /**
         * This inner class implements the behavior of the "servants", whose
         * responsibility is to take care of clients once they have connected. This
         * is where we implement the application protocol logic, i.e. where we read
         * data sent by the client and where we generate the responses.
         */
        private class ServantWorker implements Runnable {

            Socket clientSocket;

            public ServantWorker(Socket clientSocket) {
                try {
                    this.clientSocket = clientSocket;
                    in = new BufferedReader (new InputStreamReader(clientSocket.getInputStream()));
                    out = new PrintWriter(clientSocket.getOutputStream());
                } catch (IOException ex) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            @Override
            public void run() {
                String line;
                boolean shouldRun = true;

                out.println("Welcome to the Multi-Threaded Server.");
                out.flush();
                try {
                    send(SEND_ID);
                    int id = Integer.parseInt(receive());
                    if (!sm.idAlreadyStored(id)) {
                        sm.storeSession(new Session(id, new Timestamp(System.currentTimeMillis())));
                        send(Integer.toString(id));
                    } else {
                        sm.updateSession(id);
                    }

                    LOG.info("Cleaning up resources...");
                    clientSocket.close();
                    in.close();
                    out.close();

                } catch (IOException ex) {
                    if (in != null) {
                        try {
                            in.close();
                        } catch (IOException ex1) {
                            LOG.log(Level.SEVERE, ex1.getMessage(), ex1);
                        }
                    }
                    if (out != null) {
                        out.close();
                    }
                    if (clientSocket != null) {
                        try {
                            clientSocket.close();
                        } catch (IOException ex1) {
                            LOG.log(Level.SEVERE, ex1.getMessage(), ex1);
                        }
                    }
                    LOG.log(Level.SEVERE, ex.getMessage(), ex);
                }
            }
        }
    }
}
