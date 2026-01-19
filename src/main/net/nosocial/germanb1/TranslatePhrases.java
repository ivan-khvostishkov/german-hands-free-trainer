/**
 * Copyright (c) 2023 Ivan Khvostishkov & NoSocial.Net
 */
package net.nosocial.germanb1;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.translate.TranslateClient;
import software.amazon.awssdk.services.translate.model.TranslateTextRequest;
import software.amazon.awssdk.services.translate.model.TranslateTextResponse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

public class TranslatePhrases {
    /**
     * NOTE: we manually review and edit the extracted phrases to join multi-line phrases lines into a single line
     */
    public static final String EDITED_FILE_NAME = "b1-phrases-edited-Part-1.txt";
    public static final String LOCAL_PATH_IN = "out/" + EDITED_FILE_NAME;

    public static final String OUTPUT_FILE_NAME = "b1-phrases-edited-English-Part-1.txt";
    public static final String LOCAL_PATH_OUT = "out/" + OUTPUT_FILE_NAME;
    public static final String S3_PATH = "goethe_de/extract/" + OUTPUT_FILE_NAME;

    public static void main(String[] args) throws IOException {
        System.out.println("German Hands-free Trainer (c) 2023-2026 by NoSocial.Net");

        System.out.println("Translating phrases with Amazon Translate...");

        TranslateClient translate = TranslateClient.builder()
                .region(Region.EU_WEST_1)
                .build();

        // Translate all phrases from edited file line by line
        // and write to output file.
        try (
                java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(LOCAL_PATH_IN));
                java.io.BufferedWriter bw = new java.io.BufferedWriter(new java.io.FileWriter(LOCAL_PATH_OUT))
        ) {
            String line;
            while ((line = br.readLine()) != null) {
                String translatedText = getTranslatedText(translate, line);
                bw.write(translatedText);
                bw.newLine();
                bw.flush();
            }
        }

        // Upload output file to S3
        S3Client s3Client = S3Client.builder()
                .region(Region.EU_WEST_1)
                .build();

        s3Client.putObject(PutObjectRequest.builder()
                        .bucket(DownloadWordList.BUCKET_NAME)
                        .key(S3_PATH)
                        .build(),
                RequestBody.fromFile(Paths.get(LOCAL_PATH_OUT)));

        System.out.println("Done.");
    }

    private static String getTranslatedText(TranslateClient translate, String textToTranslate) {
        TranslateTextRequest request = TranslateTextRequest.builder()
                .text(textToTranslate)
                .sourceLanguageCode("de")
                .targetLanguageCode("en")
                .build();
        TranslateTextResponse result = translate.translateText(request);
        return result.translatedText();
    }
}
