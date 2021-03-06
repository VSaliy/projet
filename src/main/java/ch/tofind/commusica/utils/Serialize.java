package ch.tofind.commusica.utils;

import ch.tofind.commusica.media.EphemeralPlaylist;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * This class can get properties from a configuration file
 */
public class Serialize {

    //! Builder for conversion.
    private static final GsonBuilder builder = new GsonBuilder();

    //! Google JSON convertor.
    private static final Gson gson = builder
            .registerTypeAdapter(EphemeralPlaylist.class, new EphemeralPlaylistSerializer())
            .create();

    /**
     * Serialize an object to JSON.
     *
     * @param object The object to serialize.
     *
     * @return The object form in JSON.
     */
    public static String serialize(Object object) {
        return gson.toJson(object);
    }

    /**
     * Unserialize an object from JSON.
     *
     * @param jsonObject The object to unserialize.
     * @param objectClass The destination class for the object.
     *
     * @return The object from JSON.
     */
    public static <T> T unserialize(String jsonObject, Class<T> objectClass) {
        return gson.fromJson(jsonObject, objectClass);
    }
}