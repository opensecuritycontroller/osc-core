package org.osc.core.broker.rest.client.openstack.vmidc.notification;

import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.osc.core.broker.model.entities.BaseEntity;
import org.osc.core.broker.rest.client.openstack.vmidc.notification.listener.OsNotificationListener;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * 
 * Class used to handle parse Json Messages coming from RabbitMQ server
 * 
 */
public class OsNotificationUtil {

    private final static String EVENT_TYPE = "event_type";
    private static final JsonParser parser = new JsonParser();

    /**
     * This method returns value the given property as String from the provided Json message
     * 
     * @param message
     *            Json Message received from OpenStack Server
     * @param key
     *            Json Element Key
     * @return
     *         Value of the provided Json Key as String or null if the key id not present in the given message
     */
    public static String getPropertyFromNotificationMessage(String message, String key) {
        String value = null;
        // If message have oslo in it then get the payload..
        if (message.contains("oslo.message")) {
            message = getOsloPayload(message);
        }

        JsonObject object = parser.parse(message).getAsJsonObject();
        if (object != null) {
            if (key.equals(OsNotificationKeyType.SUBNET_ID.toString())) {
                object = getSubnetIdFromFixedIpElement(object);
            }
            JsonElement element = getProperty(key, object);
            value = element.getAsString();
        }
        return value;
    }

    private static JsonObject getSubnetIdFromFixedIpElement(JsonObject object) {
        // get Fixed IP element to find Subnet from it..
        JsonArray fixedIpArray = getProperty(OsNotificationKeyType.FIXED_IPS.toString(), object).getAsJsonArray();
        JsonObject obj = fixedIpArray.get(0).getAsJsonObject();
        return obj;
    }

    /**
     * 
     * This method returns value of Key "event_type" from given message. We introduced this message instead of the using
     * getProperty as this will eb frequently used by RabbitMQ clients and Notification listeners
     * 
     * @param message
     *            Json Message received from OpenStack Server
     * @return
     *         null if the "event_type" does not exist in the given msg or the value of the "event_type"
     */
    public static String getEventTypeFromMessage(String message) {
        int index = message.indexOf(EVENT_TYPE);
        String str = message.substring(index + 14, message.length());
        int secondIndex = str.indexOf(',');
        if (message.contains("\\")) { // change index if message have escape characters
            return (str.substring(2, secondIndex - 2));
        } else {
            return (str.substring(0, secondIndex - 1));
        }
    }

    /**
     * 
     * This method iterates over the given list of objectIds and verified that the given messaged contains any of the
     * ids we are interested in it.
     * 
     * @param message
     *            Json Message received from OpenStack Server
     * 
     * @param objectIdList
     *            List of objecyID (UUID) which were registered while creating Notification Listener
     * 
     * @param key
     *            Json Element Key
     * @return
     * 
     *         relevant Object ID from the message else return null
     */
    public static String isMessageRelevant(String message, List<String> objectIdList, String property) {
        for (String objectId : objectIdList) {
            String value = getPropertyFromNotificationMessage(message, property);
            if (value != null && objectId.equals(value)) {
                return objectId;
            }
        }
        return null;
    }

    /**
     * This method performs a top-down recursion on a json tree to find given key
     * 
     * @param key
     *            : property whose value we need to retrieve
     * @param jsonObject
     *            : json object to traverse
     * @return JsonElement if found or null if element not found
     */
    public static JsonElement getProperty(String key, JsonObject jsonObject) {

        // check current level for the key before descending
        if (jsonObject.has(key)) {
            return jsonObject.get(key);
        }

        // get all entry sets
        Set<Entry<String, JsonElement>> entries = jsonObject.entrySet();

        for (Entry<String, JsonElement> entry : entries) {
            // cache the current value since retrieval is done so much
            JsonElement curVal = entry.getValue();

            if (curVal.isJsonArray()) {
                for (JsonElement el : curVal.getAsJsonArray()) {
                    // recursively traverse the sub-tree
                    if (el.isJsonObject()) {
                        JsonElement res = getProperty(key, (JsonObject) el);
                        if (res != null) {
                            return res;
                        }
                    }
                }
            } else if (curVal.isJsonObject()) {
                // traverse sub-node
                return getProperty(key, curVal.getAsJsonObject());
            }
        }
        return null;
    }

    /**
     * 
     * This message is very specific to Oslo Message received from environments like JStack and not devstack
     * This will return the pay load of the notification message which can be parsed as a JSON Object to fins the key of
     * interest
     * 
     * @param message
     *            oslo.message received from jStack
     * @return
     *         pay load JSON Object as a string
     */
    private static String getOsloPayload(String message) {

        // Remove JSON Primitive Element else get Property will return null
        message = message.substring(18, message.length() - 25);

        // remove all escape char from the stripped message to make it well formed JSON again
        message = StringUtils.replace(message, "\\", "");

        return message;
    }

    /**
     * This method will check the different between new Id list and Previous ID list
     * If they are same does nothing
     * If different will replace old listener object ID list with the new one
     * 
     * @param listener
     *            OsNotification Listener which needs to be updated
     * @param newMemberList
     *            Updated member ID list..
     * 
     */
    public static void updateListener(OsNotificationListener listener, BaseEntity entity, List<String> newMemberList) {
        List<String> oldMemberList = listener.getObjectIdList();
        if (isMemberListModified(oldMemberList, newMemberList)) {
            listener.setObjectIdList(newMemberList);
        }

        // update attached entity on update
        listener.setEntity(entity);
    }

    /**
     * 
     * Method compares two list of strings..
     * 
     * @param oldMemberList
     * @param newMemberList
     * @return
     *         true if they are not same
     *         false if they are same
     */
    private static boolean isMemberListModified(List<String> oldMemberList, List<String> newMemberList) {
        return !(oldMemberList.containsAll(newMemberList) && newMemberList.containsAll(oldMemberList));
    }
}
