package ch.tofind.commusica.utils;

import ch.tofind.commusica.playlist.PlaylistTrack;
import ch.tofind.commusica.playlist.VoteComparator;
import javafx.collections.ObservableListBase;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents all the PlaylistTrack in the UI.
 */
public class ObservableSortedPlaylistTrackList extends ObservableListBase<PlaylistTrack> {

    //! The list of PlaylistTrack.
    private final List<PlaylistTrack> delegate = new ArrayList<>();

    //! Know which PlaylistTrack is prior to another PlaylistTrack.
    private VoteComparator comparator = new VoteComparator();

    //! Number of tracks for the list of PlaylistTracks.
    private int count = 0;

    @Override
    public boolean add(PlaylistTrack track) {
        if(delegate.contains(track)) {
            return false;
        }

        beginChange();

        boolean result = delegate.add(track);

        track.getVotesProperty().addListener(((observable, oldValue, newValue) -> {
            beginChange();
            sort();
            endChange();
        }));

        sort();

        endChange();

        return result;
    }

    @Override
    public void clear() {
        beginChange();

        delegate.clear();

        endChange();
    }

    @Override
    public PlaylistTrack get(int index) {
        return delegate.get(index);
    }

    public PlaylistTrack getNextTrack() {
        if(count >= size()) {
            return null;
        }

        PlaylistTrack nextTrack = get(count++);
        nextTrack.update();

        return nextTrack;
    }

    public void remove(PlaylistTrack track) {
        beginChange();

        delegate.remove(track);

        endChange();
    }

    @Override
    public int size() {
        return delegate.size();
    }

    private void sort() {
        if(count < size()) {
            // WARNING: Second parameter is exclusive!
            delegate.subList(count, size()).sort(comparator);
        }
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("");
        for (PlaylistTrack playlistTrack : delegate) {
            result.append(playlistTrack + "\n");
        }

        return result.toString();
    }
}