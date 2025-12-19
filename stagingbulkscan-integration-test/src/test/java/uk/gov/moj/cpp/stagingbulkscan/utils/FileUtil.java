package uk.gov.moj.cpp.stagingbulkscan.utils;

import static java.nio.charset.Charset.defaultCharset;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Map;

import javax.json.JsonObject;

import com.google.common.io.Resources;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtil.class);

    public static String getPayload(final String path) {
        String request = null;
        try {
            request = Resources.toString(Resources.getResource(path), defaultCharset());
        } catch (final Exception e) {
            LOGGER.error("Error consuming file from location {}", path, e);
            fail("Error consuming file from location " + path);
        }
        return request;
    }

    public static String getFileContent(final String path, final Map<String, Object> namedPlaceholders) {
        return new StrSubstitutor(namedPlaceholders).replace(getPayload(path));
    }

    public static JsonObject getFileContentAsJson(final String path, final Map<String, Object> namedPlaceholders) {
        return JsonHelper.getJsonObject(getFileContent(path, namedPlaceholders));
    }
}
