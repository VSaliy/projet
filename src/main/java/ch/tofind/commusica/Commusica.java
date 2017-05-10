package ch.tofind.commusica;

import ch.tofind.commusica.core.ApplicationProtocol;
import ch.tofind.commusica.core.Core;
import ch.tofind.commusica.network.NetworkProtocol;
import ch.tofind.commusica.utils.Network;

import java.net.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Commusica {

    public static void main(String[] args) {

        Integer uniqueID;
        Scanner scanner = new Scanner(System.in);
        InetAddress interfaceToUse;

        TreeMap<String, InetAddress> networkInterfaces = Network.getIPv4Interfaces();

        if (networkInterfaces.size() > 1) {
            String interfaceChoice = "";
            while (!networkInterfaces.containsKey(interfaceChoice)) {
                System.out.println("Which interface to use for the multicast ?");
                for (Map.Entry<String, InetAddress> networkInterface : networkInterfaces.entrySet()) {
                    System.out.println(networkInterface.getKey() + " - " + networkInterface.getValue());
                }
                System.out.print("> ");
                interfaceChoice = scanner.next();
            }

            interfaceToUse = networkInterfaces.get(interfaceChoice);
        } else {
            interfaceToUse = networkInterfaces.firstEntry().getValue();
        }

        uniqueID = Arrays.hashCode(Network.getMacAddress(interfaceToUse));

        int launchChoice = -1;
        while (launchChoice != 0) {

            System.out.println("How would you like to launch the program ?");
            System.out.println("  [0] Quit.");
            System.out.println("  [1] As the server.");
            System.out.println("  [2] As the client.");
            System.out.print("> ");
            launchChoice = scanner.nextInt();

            if (launchChoice == 1) { // Launch as server

                Core core = new Core(interfaceToUse);
                core.setupAsServer("Soirée de Lulu 4");

                int actionChoice = -1;
                while (actionChoice != 0) {
                    System.out.println("Actions");
                    System.out.println("  [0] Disconnect");
                    System.out.println("  [1] Send message via Multicast");
                    System.out.print("> ");

                    actionChoice = scanner.nextInt();

                    switch (actionChoice) {
                        case 0:
                            core.stop();
                            break;
                        case 1:
                            String command = "Bonsoir" + NetworkProtocol.END_OF_LINE +
                                    uniqueID + NetworkProtocol.END_OF_LINE +
                                    NetworkProtocol.END_OF_COMMAND;
                            core.sendMulticast(command);
                            break;
                        default:
                            System.out.println("Action not supported.");
                            break;
                    }
                }

            } else if (launchChoice == 2) { // Launch as client

                Core core = new Core(interfaceToUse);
                core.setupAsClient();

                // Discovery servers every schedule
                ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

                scheduledExecutorService.scheduleAtFixedRate(() -> {
                    System.out.println(new Date() + " - Coucou");
                    core.execute(ApplicationProtocol.DISCOVER_SERVER, null);
                }, 0, 5, TimeUnit.SECONDS);


                InetAddress hostname = null;
                try {
                    hostname = InetAddress.getByName("localhost");
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }

                int actionChoice = -1;
                while (actionChoice != 0) {

                    System.out.println("Actions");
                    System.out.println("  [0] Quit");
                    System.out.println("  [1] Send track to Unicast");
                    System.out.print("> ");

                    actionChoice = scanner.nextInt();

                    switch (actionChoice) {
                        case 0:
                            core.stop();
                            break;
                        case 1:
                            String command = ApplicationProtocol.TRACK_REQUEST + NetworkProtocol.END_OF_LINE +
                                    uniqueID + NetworkProtocol.END_OF_LINE +
                                    "{json représentant la track}" + NetworkProtocol.END_OF_LINE +
                                    NetworkProtocol.END_OF_COMMAND;
                            core.sendUnicast(hostname, command);
                            break;
                        default:
                            System.out.println("Action not supported.");
                            break;
                    }
                }
            }
        }
    }

    /*
    public static void main(String[] args) {
        launch(args);
    }

    public static void dropDatabase() {
        String filePath = "commusica.db";
        File dbFile = new File(filePath);

        if (dbFile.exists()) {
            dbFile.delete();
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        System.out.println("App démarrée :)");

        try {
            Configuration.getInstance().load("commusica.properties");
        } catch (IOException e) {
            e.printStackTrace();
        }

        dropDatabase();

        Playlist playlist1 = new Playlist("Test1");
        Playlist playlist2 = new Playlist("Test2");

        Track track1 = new Track("Test1", "Test", "Test", 123, "/home/ludelafo/Music/Temp/sample1.wav");
        Track track2 = new Track("Test2", "Test", "Test", 132, "/home/ludelafo/Music/Temp/sample2.wav");
        Track track3 = new Track("Test3", "Test", "Test", 321, "/home/ludelafo/Music/Temp/sample3.wav");

        PlaylistTrack pt11 = new PlaylistTrack(playlist1, track1);
        PlaylistTrack pt12 = new PlaylistTrack(playlist1, track2);
        PlaylistTrack pt13 = new PlaylistTrack(playlist1, track3);

        PlaylistTrack pt21 = new PlaylistTrack(playlist2, track1);
        PlaylistTrack pt22 = new PlaylistTrack(playlist2, track3);

        DatabaseManager.getInstance().save(playlist1);
        DatabaseManager.getInstance().save(playlist2);

        DatabaseManager.getInstance().save(track1);
        DatabaseManager.getInstance().save(track2);
        DatabaseManager.getInstance().save(track3);

        DatabaseManager.getInstance().save(pt11);
        DatabaseManager.getInstance().save(pt12);
        DatabaseManager.getInstance().save(pt13);

        DatabaseManager.getInstance().save(pt21);
        DatabaseManager.getInstance().save(pt22);

        Player player = new Player(Double.parseDouble(Configuration.getInstance().get("DEFAULT_VOLUME_STEP")));

        PlaylistManager playlistManager = PlaylistManager.getInstance();

        playlistManager.loadPlaylist(playlist1);

        pt11.downvote();
        pt11.downvote();
        pt12.upvote();

        playlistManager.addPlaylistTrack(pt11);
        playlistManager.addPlaylistTrack(pt12);

        BorderPane root = new BorderPane() {{
            VBox vbox = new VBox() {{

                HBox hbox = new HBox() {{

                    Button playButton = new Button("Play");
                    Button pauseButton = new Button("Pause");
                    Button stopButton = new Button("Stop");
                    Button nextButton = new Button("Next");
                    Button volUpButton = new Button("Volume up");
                    Button volDownButton = new Button("Volume down");

                    playButton.setOnAction(e -> {
                        player.play();

                    });

                    pauseButton.setOnAction(e -> {
                        player.pause();

                    });

                    nextButton.setOnAction(e -> {
                        player.stop();
                        player.load();
                        player.play();
                    });

                    stopButton.setOnAction(e -> {
                        player.stop();
                    });

                    volUpButton.setOnAction(e -> {
                        player.riseVolume();
                    });

                    volDownButton.setOnAction(e -> {
                        player.lowerVolume();
                    });

                    getChildren().addAll(playButton, pauseButton, stopButton, nextButton, volUpButton, volDownButton);
                }};

                getChildren().add(hbox);

            }};

            setCenter(vbox);

        }};

        Scene scene = new Scene(root, 400, 100);

        primaryStage.setScene(scene);
        primaryStage.show();

        //UIController.launch(UIController.class);

        System.out.println("Bisoir");
        DatabaseManager.getInstance().close();
    }
    */
}
