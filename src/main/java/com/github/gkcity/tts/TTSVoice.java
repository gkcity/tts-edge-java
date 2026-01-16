package com.github.gkcity.tts;

import com.alibaba.fastjson.JSON;
import com.github.gkcity.tts.bean.Voice;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;

public class TTSVoice {

    private static List<Voice> voices;

    static {
        ClassLoader classLoader = TTSVoice.class.getClassLoader();
        StringBuilder sb = new StringBuilder();
        try (InputStream inputStream = classLoader.getResourceAsStream("voicesList.json");
             BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            voices = JSON.parseArray(sb.toString(), Voice.class);
        } catch (Exception e) {
            e.printStackTrace();

            voices = Collections.emptyList();
        }
    }

    public static List<Voice> provides() {
        return voices;
    }
}
