/*******************************************************************************
 * Copyright (c) Intel Corporation
 * Copyright (c) 2017
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.osc.core.broker.util.log;

import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Collection;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.apache.commons.lang.StringEscapeUtils;
import org.osc.core.broker.service.annotations.VmidcLogHidden;
import org.osc.core.broker.service.api.server.LoggingApi;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

@Component
public final class LoggingUtil implements LoggingApi {

    private static Logger log = LogProvider.getLogger(LoggingUtil.class);

    private static final Gson gson = new GsonBuilder().setPrettyPrinting()
            .registerTypeAdapter(byte[].class, new ByteArrayTypeAdapter()).create();

    /**
     * Logs the pojo by converting it into Json and printing it out at info Level.
     *
     * @param pojo
     *            the pojo to convert to json
     */
    public static <T> void logPojoToJson(T pojo) { // for debug purpose only
        if (log.isInfoEnabled()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("\n");
            stringBuilder.append(pojoToJsonString(pojo));

            log.info(stringBuilder.toString());
        }
    }

    /**
     * Sanitizes the pojo by setting the pojo field values to hidden or setting them to empty.
     * The actual pojo is modified through reflection, so the original value is LOST. Assuming the caller sends in
     * a cloned object.
     *
     * @param pojo
     *            the pojo to sanitize
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     *
     */
    private static <T> void sanitizePojo(T pojo) throws Exception {
        for (Field field : pojo.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(VmidcLogHidden.class)) {
                if (String.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    field.set(pojo, "***hiddden***");
                } else if (Collection.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    for (Object item : (Collection<?>) field.get(pojo)) {
                        sanitizePojo(item);
                    }
                } else if (!field.getType().isPrimitive()) {
                    field.setAccessible(true);
                    sanitizePojo(field.get(pojo));
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> String pojoToJsonPrettyString(T pojo) {
        if (pojo == null) {
            return null;
        }
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping()
                    .registerTypeAdapter(byte[].class, new ByteArrayTypeAdapter()).create();

            T clonedPojoItem = (T) gson.fromJson(gson.toJson(pojo), pojo.getClass());

            sanitizePojo(clonedPojoItem);

            return gson.toJson(clonedPojoItem);
        } catch (Exception ex) {
            log.error("Problem converting the input to Json. This should not happen.", ex);
        }

        return null;
    }

    /**
     * Converts pojo into Json. Returns an empty string in case any exceptions happen. Suppresses ALL exceptions
     *
     * @param pojo
     *            the pojo to convert
     * @return the string representation of the pojo
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public static <T> String pojoToJsonString(T pojo) {
        if (pojo == null) {
            return null;
        }

        try {
            T clonedPojoItem = (T) gson.fromJson(gson.toJson(pojo), pojo.getClass());
            sanitizePojo(clonedPojoItem);

            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(clonedPojoItem);
        } catch (Exception ex) {
            log.error("Problem converting the input to Json. This should not happen.", ex);
        }

        return null;
    }

    private static class ByteArrayTypeAdapter implements JsonSerializer<byte[]>, JsonDeserializer<byte[]> {
        @Override
        public byte[] deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            return new byte[] {};
        }

        @Override
        public JsonElement serialize(byte[] src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive("***binary data***");
        }
    }

    public static <T> void logPojoToXml(T pojo) {
        log.info(pojoToXmlString(pojo));
    }

    public static <T> String pojoToXmlString(T pojo) {
        try {
            StringWriter writer = new StringWriter();

            JAXBContext context = JAXBContext.newInstance(pojo.getClass());
            Marshaller ms = context.createMarshaller();
            ms.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            ms.marshal(pojo, writer);
            return writer.toString();

        } catch (Exception ex) {
            log.error("Problem converting the input to Xml. This should not happen.", ex);
        }
        return null;
    }

    @Override
    public String removeCRLF(String message) {
        // See https://www.owasp.org/index.php/Category:Encoding
        String clean = StringEscapeUtils.escapeHtml(message.replace('\n', '_').replace('\r', '_'));
        if (!message.equals(clean)) {
            clean += " (Encoded)";
        }
        return clean;
    }

}
