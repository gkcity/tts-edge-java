package com.github.gkcity.tts;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.github.gkcity.tts.bean.Voice;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;

public class TTSVoice {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static List<Voice> voices;

    static {
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        ClassLoader classLoader = TTSVoice.class.getClassLoader();
        StringBuilder sb = new StringBuilder();
        try (InputStream inputStream = classLoader.getResourceAsStream("voicesList.json");
             BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            // 使用 Jackson 解析 JSON 数组
            voices = OBJECT_MAPPER.readValue(
                    sb.toString(),
                    new TypeReference<List<Voice>>() {}
            );
        } catch (Exception e) {
            e.printStackTrace();

            voices = Collections.emptyList();
        }
    }

    public static List<Voice> provides() {
        return voices;
    }
}
