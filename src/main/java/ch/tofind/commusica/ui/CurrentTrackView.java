package ch.tofind.commusica.ui;

import ch.tofind.commusica.media.Player;
import ch.tofind.commusica.media.Track;
import ch.tofind.commusica.playlist.PlaylistManager;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;

/**
 * This class represents the current track view.
 */
public class CurrentTrackView extends GridPane {

    //! Component CSS class.
    public static final String CSS_CLASS = "current-track-view";

    //! CSS file to apply to the view.
    private static final String CSS_FILE = "ui/styles/CurrentTrackView.css";

    //! Path to the file containing the 'fav_empty' image.
    private static final String FAV_EMPTY_IMAGE = "ui/icons/fav_empty.png";

    //! Path to the file containing the 'fav_full' image.
    private static final String FAV_FULL_IMAGE = "ui/icons/fav_full.png";

    //! FXML file to use for the view.
    private static final String FXML_FILE = "ui/CurrentTrackView.fxml";

    //! Commusica player.
    private static final Player player = Player.getCurrentPlayer();

    //! SavedPlaylist manager.
    private static final PlaylistManager playlistManager = PlaylistManager.getInstance();

    @FXML
    private Label albumLabel;

    @FXML
    private Label artistLabel;

    @FXML
    private ProgressBar durationBar;

    @FXML
    private Label durationLabel;

    @FXML
    private ImageView favoriteImageView;

    @FXML
    private Label titleLabel;

    @FXML
    private Label votesLabel;

    /**
     * View constructor.
     */
    public CurrentTrackView() {
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

        // What to do when the current track changes.
        player.getCurrentTrackProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                albumLabel.setText("-");
                artistLabel.setText("-");
                titleLabel.setText("-");
                votesLabel.setText("-");
                favoriteImageView.setImage(new Image(FAV_EMPTY_IMAGE));
            } else {
                Track track = newValue.getTrack();
                albumLabel.setText(track.getAlbum());
                artistLabel.setText(track.getArtist());
                titleLabel.setText(track.getTitle());
                votesLabel.setText(String.valueOf(newValue.getVotesProperty().intValue()));

                favoriteImageView.setImage((new Image(track.getFavoritedProperty().getValue() ? FAV_FULL_IMAGE : FAV_EMPTY_IMAGE)));
                track.getFavoritedProperty().addListener(((obs, oldTrack, newTrack) -> {
                    favoriteImageView.setImage((new Image(newTrack ? FAV_FULL_IMAGE : FAV_EMPTY_IMAGE)));
                }));

                newValue.getVotesProperty().addListener((obs, oldCount, newCount) -> {
                    votesLabel.setText(String.valueOf(newCount.intValue()));
                });
            }
        });

        // What to do when the elapsed time for the current track changes.
        player.getCurrentTimeProperty().addListener((observable, oldValue, newValue) -> {
            if (player.getCurrentTrackProperty().getValue() != null) {
                int total = player.getCurrentTrack().getLength();
                int current = newValue.intValue();
                int diff = total - current;

                durationLabel.setText(String.format("-%02d:%02d", diff / 60, diff % 60));
                durationBar.setProgress((double) current / (double) total);
            } else {
                durationLabel.setText("00:00");
                durationBar.setProgress(0.0);
            }
        });
    }

    @FXML
    private void downvote(MouseEvent e) {
        // Left blank for possible future use.
    }

    @FXML
    private void favorite(MouseEvent e) {
        if(player.getCurrentTrack() != null) {
            player.getCurrentTrack().getFavoritedProperty().setValue(!player.getCurrentTrack().getFavoritedProperty().getValue());
        }
    }

    @FXML
    private void upvote(MouseEvent e) {
        // Left blank for possible future use.
    }
}
