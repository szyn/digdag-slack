package io.digdag.plugin.slack;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;

class SlackPayload
{
    static String convertToJson(String yamlString)
    {
        ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());

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
}
