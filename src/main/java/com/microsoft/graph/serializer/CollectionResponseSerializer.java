package com.microsoft.graph.serializer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.ArrayList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.microsoft.graph.http.BaseCollectionResponse;
import com.microsoft.graph.logger.ILogger;

/** Specialized serializer to handle collection responses */
public class CollectionResponseSerializer {
    private static DefaultSerializer serializer;
    /**
     * Not available for instantiation
     */
    private CollectionResponseSerializer() {}
    /**
     * Deserializes the JsonElement
     *
     * @param json the source CollectionResponse's Json
     * @param typeOfT The type of the CollectionResponse to deserialize to
     * @param logger the logger
     * @param <T1> the entity type for the collection
     * @throws JsonParseException the parse exception
     * @return    the deserialized CollectionResponse
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public static <T1> BaseCollectionResponse<T1> deserialize(@Nonnull final JsonElement json, @Nonnull final Type typeOfT, @Nonnull final ILogger logger) throws JsonParseException {
        if (json == null || !json.isJsonObject()| !typeOfT.getClass().equals(Class.class)) {
            return null;
        }
        serializer = new DefaultSerializer(logger);
        final JsonObject jsonAsObject = json.getAsJsonObject();
        final JsonArray sourceArray = jsonAsObject.get("value").getAsJsonArray();
        final ArrayList<T1> list = new ArrayList<>(sourceArray.size());
        /** eg: com.microsoft.graph.requests.AttachmentCollectionResponse */
        /** eg: com.microsoft.graph.requests.DriveItemDeltaCollectionResponse */
        /** eg: com.microsoft.graph.requests.DriveItemGetActivitiesByIntervalCollectionResponse */
        final Class<?> responseClass = ((Class<?>)typeOfT);
        /** eg: com.microsoft.graph.http.BaseCollectionResponse<com.microsoft.graph.models.DriveItem> */
        final String genericResponseName = responseClass.getGenericSuperclass().toString();
        final int indexOfGenericMarker = genericResponseName.indexOf('<');
        /** eg: com.microsoft.graph.models.Attachment */
        final String baseEntityClassCanonicalName = genericResponseName
                                                        .substring(indexOfGenericMarker + 1, genericResponseName.length() - 1);
        Class<?> baseEntityClass;
        try {
            baseEntityClass = Class.forName(baseEntityClassCanonicalName);
        } catch (ClassNotFoundException ex) {
            //it is possible we can't find the parent base class depending on the response class name, see examples
            baseEntityClass = null;
            logger.logDebug("could not find class" + baseEntityClassCanonicalName);
        }
        try {
            for(JsonElement sourceElement : sourceArray) {
                if(sourceElement.isJsonObject()) {
                    final JsonObject sourceObject = sourceElement.getAsJsonObject();
                    Class<?> entityClass = serializer.getDerivedClass(sourceObject, baseEntityClass);
                    if(entityClass == null && baseEntityClass != null)
                        entityClass = baseEntityClass; // it is possible the odata type is absent or we can't find the derived type (not in SDK yet)
                    final T1 targetObject = (T1)serializer.deserializeObject(sourceObject, entityClass);
                    ((IJsonBackedObject)targetObject).setRawObject(serializer, sourceObject);
                    list.add(targetObject);
                }
            }
            final BaseCollectionResponse<T1> response = (BaseCollectionResponse<T1>)responseClass.getConstructor().newInstance();
            response.value = list;
            response.setRawObject(serializer, jsonAsObject);
            return response;
        } catch(NoSuchMethodException | InstantiationException | InvocationTargetException ex) {
            logger.logError("Could not instanciate type during deserialization", ex);
        } catch(IllegalAccessException ex) {
            logger.logError("Unable to set field value during deserialization", ex);
        }
        return null;
    }
}
