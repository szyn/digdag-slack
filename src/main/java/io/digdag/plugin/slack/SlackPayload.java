package io.digdag.plugin.slack;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.digdag.client.config.ConfigException;

import java.io.IOException;

class SlackPayload
{
    private static ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
    static String convertToJson(String yamlString)
    {
        validate(yamlString);

        Object obj = null;
        try {
            obj = yamlReader.readValue(yamlString, Object.class);
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        String result = null;
        ObjectMapper jsonWriter = new ObjectMapper();
        try {
            result = jsonWriter.writeValueAsString(obj);
        }
        catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return result;
    }
    private static void validate (String yamlString)
    {
        JsonNode root = null;
        try {
            root = yamlReader.readTree(yamlString);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (!root.has("text") && !root.has("attachments")) {
            throw new ConfigException("'text' or 'attachments' is required for template for slack's payload");
        }
    }
}
