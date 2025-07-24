/**
 * Copyright (c) 2023 Ivan Khvostishkov & NoSocial.Net
 */
package net.nosocial.germanb1;

import org.json.JSONArray;
import org.json.JSONObject;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class ParseTextractData {
    public static final String JSON_FILE = "out/b1-textract-blocks.json";
    public static final String OUTPUT_FILE = "out/b1-phrases.txt";
    public static final String S3_PATH = "goethe_de/extract/b1-phrases.txt";

    public static void main(String[] args) {
        System.out.println("German B1 Hands-Free Trainer (c) 2013-2025 by NoSocial.Net");

        try {
            String jsonContent = Files.readString(new File(JSON_FILE).toPath());
            JSONArray blocks = new JSONArray(jsonContent);

            List<String> phrases = extractPhrases(blocks);

            System.out.println("Found " + phrases.size() + " phrases");

            savePhrases(phrases, OUTPUT_FILE);

            uploadToS3();

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.out.println("Done.");
    }

    private static List<String> extractPhrases(JSONArray blocks) {
        List<String> phrases = new ArrayList<>();
        int page = 0;

        // First pass: left column
        for (int i = 0; i < blocks.length(); i++) {
            JSONObject block = blocks.getJSONObject(i);
            if ("PAGE".equals(block.getString("blockType"))) {
                page++;
            }
            if ("LINE".equals(block.getString("blockType")) && page >= 16 && page <= 102) {
                if (block.has("geometry")) {
                    JSONObject geometry = block.getJSONObject("geometry");
                    float left = geometry.getFloat("left");
                    float top = geometry.getFloat("top");
                    float bottom = top + geometry.getFloat("height");
                    float right = left + geometry.getFloat("width");

                    System.out.printf("%d %.2f %.2f - %s\n", page, left, top, block.optString("text", ""));

                    if ((left > 0.22 && right < 0.50) && top > 0.100 && bottom < 0.93) {
                        phrases.add(block.optString("text", ""));
                    }
                }
            }
        }

        // Second pass: right column
        page = 0;
        for (int i = 0; i < blocks.length(); i++) {
            JSONObject block = blocks.getJSONObject(i);
            if ("PAGE".equals(block.getString("blockType"))) {
                page++;
            }
            if ("LINE".equals(block.getString("blockType")) && page >= 16 && page <= 102) {
                if (block.has("geometry")) {
                    JSONObject geometry = block.getJSONObject("geometry");
                    float left = geometry.getFloat("left");
                    float top = geometry.getFloat("top");
                    float bottom = top + geometry.getFloat("height");

                    System.out.printf("%d %.2f %.2f - %s\n", page, left, top, block.optString("text", ""));

                    if (left > 0.689 && top > 0.100 && bottom < 0.93) {
                        phrases.add(block.optString("text", ""));
                    }
                }
            }
        }

        return phrases;
    }

    private static void savePhrases(List<String> phrases, String fileName) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (String phrase : phrases) {
            sb.append(phrase).append("\n");
        }
        try (PrintWriter out = new PrintWriter(fileName)) {
            out.print(sb);
        }
        System.out.println("Saved " + sb.length() + " characters to " + fileName);
    }

    private static void uploadToS3() {
        try (S3Client s3Client = S3Client.builder()
                .region(Region.EU_WEST_1)
                .build()) {

            s3Client.putObject(PutObjectRequest.builder()
                            .bucket(net.nosocial.germana2.DownloadWordList.BUCKET_NAME)
                            .key(S3_PATH)
                            .build(),
                    RequestBody.fromFile(new File(OUTPUT_FILE)));

            System.out.println("Uploaded to S3: " + S3_PATH);
        }
    }
}
