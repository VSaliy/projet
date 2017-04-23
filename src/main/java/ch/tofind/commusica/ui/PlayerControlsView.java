package ch.tofind.commusica.ui;

import ch.tofind.commusica.media.Player;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Logger;

public class PlayerControlsView extends AnchorPane implements Initializable {

    //! CSS class.
    public static final String CSS_CLASS = "player-controls-view";

    private static final String CSS_FILE = "ui/styles/PlayerControlsView.css";

    private static final String FXML_FILE = "ui/PlayerControlsView.fxml";

    private static final Logger LOG = Logger.getLogger(PlayerControlsView.class.getName());

    private static Player player = Player.getCurrentPlayer();

    @FXML
    private ImageView playPauseImageView;

    @FXML
    private Slider volumeSlider;

    public PlayerControlsView() {
        FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource(FXML_FILE));
        loader.setRoot(this);
        loader.setController(this);

        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        getStyleClass().add(CSS_CLASS);
        getStylesheets().add(CSS_FILE);

        if(player.isPlaying()) {
            playPauseImageView.setImage(new Image("ui/icons/pause.png"));
        } else {
            playPauseImageView.setImage(new Image("ui/icons/play.png"));
        }
    }

    @FXML
    private void next(MouseEvent e) {
        LOG.info("Asked for next track.");
    }

    @FXML
    private void playPause(MouseEvent e) {
        if(player.isPlaying()) {
            player.pause();
            playPauseImageView.setImage(new Image("ui/icons/play.png"));
        } else {
            player.play();
            playPauseImageView.setImage(new Image("ui/icons/pause.png"));
        }

        player.setVolume(volumeSlider.getValue());
    }

    @FXML
    private void previous(MouseEvent e) {
        LOG.info("Asked for previous track.");
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        volumeSlider.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                player.setVolume(newValue.doubleValue());
            }
        });
    }
}
