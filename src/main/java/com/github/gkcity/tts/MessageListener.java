
package com.github.gkcity.tts;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

public class MessageListener {

    private static final byte[] HEAD = new byte[]{0x50, 0x61, 0x74, 0x68, 0x3a, 0x61, 0x75, 0x64, 0x69, 0x6f, 0x0d, 0x0a};
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private String storage;
    private String filename;
    private Boolean findHeadHook;
    private SubMaker subMaker;

    /**
     * When a complete session ends, this sessionLatch will become 0.
     */
    private CountDownLatch sessionLatch = new CountDownLatch(1);

    public MessageListener(String storage, String filename, Boolean findHeadHook, boolean enableVttFile, boolean overwrite) {
        this.storage = storage;
        this.filename = filename;
        this.findHeadHook = findHeadHook;
        if (enableVttFile) {
            this.subMaker = new SubMaker(storage + File.separator + filename);
        }
        if (overwrite) {
            File voiceFile = new File(storage + File.separator + filename);
            File subFile = new File(storage + File.separator + filename + ".vtt");
            if (voiceFile.exists()) {
                voiceFile.delete();
            }
            if (subFile.exists()) {
                subFile.delete();
            }
        }
    }

//    public void onMessage(String message) {
//        if (message.contains("Path:turn.end")) {
//            if (subMaker != null) {
//                subMaker.generateSubs(10);
//            }
//            sessionLatch.countDown();
//        } else if (message.contains("\"Type\": \"WordBoundary\"")) {
//            JSONObject json = JSONObject.parseObject(message.substring(message.indexOf("{")));
//            JSONObject item = json.getJSONArray("Metadata").getJSONObject(0).getJSONObject("Data");
//            if (subMaker != null) {
//                subMaker.createSub(item.getDouble("Offset"), item.getDouble("Duration"), item.getJSONObject("text").getString("Text"));
//            }
//        }
//    }

    public void onMessage(String message) {
        if (message.contains("Path:turn.end")) {
            if (subMaker != null) {
                subMaker.generateSubs(10);
            }
            sessionLatch.countDown();
        } else if (message.contains("\"Type\": \"WordBoundary\"")) {
            try {
                // 提取JSON部分并解析
                String jsonStr = message.substring(message.indexOf("{"));
                JsonNode jsonNode = OBJECT_MAPPER.readTree(jsonStr);

                // 使用Jackson的树模型API替换Fastjson的链式调用
                JsonNode metadataArray = jsonNode.get("Metadata");
                if (metadataArray != null && metadataArray.isArray() && metadataArray.size() > 0) {
                    JsonNode firstMetadata = metadataArray.get(0);
                    JsonNode dataNode = firstMetadata.get("Data");

                    if (dataNode != null && subMaker != null) {
                        // 获取字段值，处理可能的null情况
                        double offset = dataNode.has("Offset") ? dataNode.get("Offset").asDouble() : 0.0;
                        double duration = dataNode.has("Duration") ? dataNode.get("Duration").asDouble() : 0.0;

                        JsonNode textNode = dataNode.get("text");
                        String text = (textNode != null && textNode.has("Text")) ?
                                textNode.get("Text").asText() : "";

                        subMaker.createSub(offset, duration, text);
                    }
                }
            } catch (JsonProcessingException e) {
                // Jackson的JSON解析异常
                System.err.println("Failed to parse JSON message: " + e.getMessage());
                e.printStackTrace();
            } catch (Exception e) {
                // 其他异常
                System.err.println("Error processing message: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public void onMessage(ByteBuffer originBytes) {
        if (findHeadHook) {
            findHeadHook(originBytes);
        } else {
            fixHeadHook(originBytes);
        }
    }

    public void startBlocking() throws InterruptedException {
        this.sessionLatch.await();
    }

    /**
     * This implementation method is more generic as it searches for the file header marker in the given file header and removes it. However, it may have lower efficiency.
     *
     * @param originBytes
     */
    private void findHeadHook(ByteBuffer originBytes) {
        byte[] origin = originBytes.array();
        int headIndex = -1;
        for (int i = 0; i < origin.length - HEAD.length; i++) {
            boolean match = true;
            for (int j = 0; j < HEAD.length; j++) {
                if (origin[i + j] != HEAD[j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                headIndex = i;
                break;
            }
        }
        if (headIndex != -1) {
            byte[] voiceBytesRemoveHead = Arrays.copyOfRange(origin, headIndex + HEAD.length, origin.length);
            try (FileOutputStream fos = new FileOutputStream(storage + File.separator + filename, true)) {
                fos.write(voiceBytesRemoveHead);
                fos.flush();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * This method directly specifies the file header marker, which makes it faster. However, if the format changes, it may become unusable.
     *
     * @param originBytes
     */
    public void fixHeadHook(ByteBuffer originBytes) {
        String str = new String(originBytes.array());
        byte[] origin = originBytes.array();
        int skip;
        if (str.contains("Content-Type")) {
            if (str.contains("audio/mpeg")) {
                skip = 130;
            } else if (str.contains("codec=opus")) {
                skip = 142;
            } else {
                skip = 0;
            }
        } else {
            skip = 105;
        }
        byte[] voiceBytesRemoveHead = Arrays.copyOfRange(origin, skip, origin.length);
        try (FileOutputStream fos = new FileOutputStream(storage + File.separator + filename, true)) {
            fos.write(voiceBytesRemoveHead);
            fos.flush();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


}
