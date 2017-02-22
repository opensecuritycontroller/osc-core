package org.osc.core.broker.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.osc.core.broker.model.entities.IscEntity;
import org.osc.core.broker.service.exceptions.VmidcBrokerInvalidEntryException;
import org.osc.core.broker.service.exceptions.VmidcBrokerInvalidRequestException;

import com.google.common.net.InetAddresses;

public class ValidateUtil {

    public static final int DEFAULT_MAX_LEN = 155;

    public static void checkForNullFields(Map<String, Object> map) throws VmidcBrokerInvalidEntryException {

        // Map will contain a map of (fieldName, fieldValue) pairs to be checked
        // for null/empty values

        for (Entry<String, Object> entry : map.entrySet()) {

            String field = entry.getKey();
            Object value = entry.getValue();

            if (value == null || value.equals("")) {

                throw new VmidcBrokerInvalidEntryException(field + " should not have an empty value.");
            }
        }

    }

    public static void validateFieldsAreNull(Map<String, Object> map) throws VmidcBrokerInvalidEntryException {

        // Map will contain a map of (fieldName, fieldValue) pairs to be checked
        // for null/empty values

        for (Entry<String, Object> entry : map.entrySet()) {

            String field = entry.getKey();
            Object value = entry.getValue();

            if (value == null) {
                continue;
            } else if (!value.equals("")) {
                throw new VmidcBrokerInvalidEntryException(field + " should not have a value set.");
            }
        }

    }

    public static void checkForValidIpAddressFormat(String ipAddress) throws VmidcBrokerInvalidEntryException {
        if (!InetAddresses.isInetAddress(ipAddress)) {
            throw new VmidcBrokerInvalidEntryException("IP Address: '" + ipAddress + "' has invalid format.");
        }
    }

    public static boolean validateDaName(String name) {

        char ch = name.charAt(0);
        if (name.length() > 13 || name.length() < 1) {
            return false;
        }
        if (!(ch >= 65 && ch <= 90 || ch >= 97 && ch <= 122)) {
            return false;
        }
        if (hasInvalidCharacters(name, new String[] {"-"})) {
            return false;
        }
        return true;
    }

    public static void validateFieldLength(Map<String, String> map, int maxLen) throws VmidcBrokerInvalidEntryException {

        for (Entry<String, String> entry : map.entrySet()) {

            String field = entry.getKey();
            String value = entry.getValue();

            if (value != null && value.length() > maxLen) {

                throw new VmidcBrokerInvalidEntryException(field + " length should not exceed " + maxLen
                        + " characters. The provided field exceeds this limit by " + (value.length() - maxLen)
                        + " characters.");
            }
        }

    }

    private static boolean hasInvalidCharacters(String str, String[] allowedSpecialCharacters) {
        boolean bReturn = false;
        ArrayList<String> list = null;
        if (allowedSpecialCharacters != null && allowedSpecialCharacters.length > 0) {
            list = new ArrayList<>();
            list.addAll(Arrays.asList(allowedSpecialCharacters));
        }
        if (str != null) {
            int iLength = str.length();
            for (int i = 0; i < iLength; i++) {
                char ch = str.charAt(i);
                if (ch >= 48 && ch <= 57 || ch >= 65 && ch <= 90 || ch >= 97 && ch <= 122 || list != null
                        && list.contains(ch + "")) {
                    continue;
                } else {
                    bReturn = true;
                    break;
                }
            }
        }
        return bReturn;
    }

    public static void checkForValidEmailAddress(String email) throws VmidcBrokerInvalidEntryException {
        String pattern = "^([a-zA-Z0-9_\\.\\-+])+@(([a-zA-Z0-9-])+\\.)+([a-zA-Z0-9]{2,4})+$";
        if (!email.matches(pattern)) {
            throw new VmidcBrokerInvalidEntryException("Email: " + email + " has invalid format.");
        }
    }

    public static void checkForValidUsername(String username) throws VmidcBrokerInvalidEntryException {
        String pattern = "([a-zA-Z0-9_]{0,30})";
        if (!username.matches(pattern)) {
            throw new VmidcBrokerInvalidEntryException(
                    "Username: "
                            + username
                            + " has invalid format. Username must be at least 1 character but not more than 30 characters and can contain lower case letter, upper case letter, digits and a special character underscore ( _ ) only with no white spaces allowed.");
        }
    }

    public static void checkForValidPassword(String password) throws VmidcBrokerInvalidEntryException {
        String pattern = "(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!_])(?=\\S+$).{8,155}";
        if (!password.matches(pattern)) {
            throw new VmidcBrokerInvalidEntryException(
                    "Password must be at least 8 characters but not more than 155 characters and contain at least one lower case letter, one upper case letter, one digit and one special character (!@#$%^&+=_) with no white spaces allowed.");
        }
    }

    public static void checkForValidFqdn(String smtpServer) throws VmidcBrokerInvalidEntryException {
        String pattern = "^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])$";
        if (!smtpServer.matches(pattern)) {
            throw new VmidcBrokerInvalidEntryException("SMTP Server should be an IP address or a FQDN.");
        }
    }

    public static void checkForValidPortNumber(String port) throws VmidcBrokerInvalidEntryException {
        if (!StringUtils.isNumeric(port) || Integer.parseInt(port) > 65535 || Integer.parseInt(port) < 1) {
            throw new VmidcBrokerInvalidEntryException(
                    "SMTP port should have a numerical value and should be between 1 and 65535.");
        }
    }

    public static void checkMarkedForDeletion(IscEntity entity, String name) throws Exception {
        if (entity.getMarkedForDeletion()) {
            throw new VmidcBrokerInvalidRequestException("Invalid Request '" + name + "' is marked for Deleted");
        }
    }

    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }
}
