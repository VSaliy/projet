package ch.tofind.commusica.ui;

import ch.tofind.commusica.media.Playlist;

import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * @brief UI controller.
 *
 * Controller meant to interact with the interface.
 */
public class UIController extends Application implements Initializable {

    private static FXMLLoader loader = new FXMLLoader();

    private static Logger LOG = Logger.getLogger(UIController.class.getName());

    private static final String FXFILE = "ui/main.fxml";

    /*
     * JavaFX components.
     */
    @FXML
    private TracksListView tracksListView;

    /**
     * @brief Returns the controller for the current interface.
     *
     * @return The controller for the current interface.
     */
    public static UIController getController() {
        return loader.getController();
    }

    public void start(Stage stage) {
        URL fileURL = getClass().getClassLoader().getResource(FXFILE);

        if (fileURL == null) {
            throw new NullPointerException("FXML file not found.");
        }

        Parent root = null;

        try {
            root = loader.load(fileURL);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Scene scene = new Scene(root);
        scene.getStylesheets().add("ui/styles/main.css");

        stage.setTitle("Commusica");
        stage.setScene(scene);
        stage.sizeToScene();

        stage.show();

        stage.setMinHeight(stage.getHeight());
        stage.setMinWidth(stage.getWidth());
    }

    /**
     * @brief
     * @param playlist
     */
    public void loadPlaylist(Playlist playlist) {
        tracksListView.loadPlaylist(playlist);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loader.setController(this);
    }
}
