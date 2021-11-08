package com.bluecc.generic;

import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

import static com.google.gson.FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_TIME;

public class Helper {
//    static class LocalDateAdapter implements JsonSerializer<LocalDate> {
//        public JsonElement serialize(LocalDate date, Type typeOfSrc, JsonSerializationContext context) {
//            return new JsonPrimitive(date.format(DateTimeFormatter.ISO_LOCAL_DATE)); // "yyyy-mm-dd"
//        }
//    }
//
//    static class LocalDateTimeAdapter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
//        public JsonElement serialize(LocalDateTime date, Type typeOfSrc, JsonSerializationContext context) {
//            return new JsonPrimitive(date.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)); // "yyyy-mm-ddTxxxxxx"
//        }
//        @Override
//        public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
//            return null;
//        }
//    }

    /**
     * A simpler implementation. Add null support to by registering the nullSafe() wrapped version
     * ref: https://stackoverflow.com/questions/39192945/serialize-java-8-localdate-as-yyyy-mm-dd-with-gson
     */
    public static final class LocalDateTimeAdapter extends TypeAdapter<LocalDateTime> {
        @Override
        public void write(final JsonWriter jsonWriter, final LocalDateTime localDate ) throws IOException {
            jsonWriter.value(localDate.toString());
        }

        @Override
        public LocalDateTime read( final JsonReader jsonReader ) throws IOException {
            return LocalDateTime.parse(jsonReader.nextString());
        }
    }

    public static final class LocalDateTimeAdapterWithFormat extends TypeAdapter<LocalDateTime> {
        @Override
        public void write(final JsonWriter jsonWriter, final LocalDateTime localDate ) throws IOException {
            jsonWriter.value(localDate.toString());
        }

        static final DateTimeFormatter LOCAL_DATE_TIME = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .append(ISO_LOCAL_DATE)
                .appendLiteral(' ')
                .append(ISO_LOCAL_TIME).toFormatter();
        @Override
        public LocalDateTime read( final JsonReader jsonReader ) throws IOException {
            // DateTimeFormatter parser = ISODateTimeFormat.dateTimeNoMillis();
            // return LocalDateTime.parse(jsonReader.nextString(),
            //         DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
            // return parser.parseDateTime(jsonReader.nextString()).toInstant();
            return LocalDateTime.parse(jsonReader.nextString(), LOCAL_DATE_TIME);
        }
    }

    public static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(LOWER_CASE_WITH_UNDERSCORES)
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
//            .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter().nullSafe())
            .setPrettyPrinting()
            .create();

    public static void pretty(Object o){
        System.out.println(GSON.toJson(o));
    }

    public static void collectEntityData(Multimap<String, JsonObject> dataList, String xmlContent) {
        NodeList nodeList = getNodeList(xmlContent);
        for (int i = 0; i < nodeList.getLength(); ++i) {
            if (nodeList.item(i) instanceof Element) {
                Element element = (Element) nodeList.item(i);
                dataList.put(element.getTagName(), convertElement(element));
            }
        }
    }

    private static JsonObject convertElement(Element element) {
        JsonObject jsonObject = new JsonObject();
        NamedNodeMap attrs = element.getAttributes();
        for (int i = 0; i < attrs.getLength(); ++i) {
            Node node = attrs.item(i);
            jsonObject.addProperty(node.getNodeName(), node.getNodeValue());
        }
        return jsonObject;
    }

    private static NodeList getNodeList(String xmlContent) {
        try {
            // Get Document Builder
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            // Build Document
            Document document = builder.parse(new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)));

            // Normalize the XML Structure; It's just too important !!
            document.getDocumentElement().normalize();

            // Here comes the root node
            Element root = document.getDocumentElement();
            // System.out.println(root.getNodeName());
            return root.getChildNodes();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

}

