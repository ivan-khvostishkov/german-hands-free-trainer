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
    public static final String[] INPUT_FILES = {
        "out/b1-phrases-edited-part1.txt",
        "out/b1-phrases-edited-part2.txt",
        "out/b1-phrases-edited-part3.txt"
    };

    public static final String[] OUTPUT_FILES = {
        "out/b1-phrases-edited-English-Part-1.txt",
        "out/b1-phrases-edited-English-Part-2.txt",
        "out/b1-phrases-edited-English-Part-3.txt"
    };

    public static final String[] S3_PATHS = {
        "goethe_de/extract/b1-phrases-edited-English-Part-1.txt",
        "goethe_de/extract/b1-phrases-edited-English-Part-2.txt",
        "goethe_de/extract/b1-phrases-edited-English-Part-3.txt"
    };

    public static void main(String[] args) throws IOException {
        System.out.println("German Hands-free Trainer (c) 2023-2026 by NoSocial.Net");
        System.out.println("Translating phrases with Amazon Translate...");

        TranslateClient translate = TranslateClient.builder()
                .region(Region.EU_WEST_1)
                .build();

        S3Client s3Client = S3Client.builder()
                .region(Region.EU_WEST_1)
                .build();

        for (int part = 0; part < 3; part++) {
            System.out.println("Processing part " + (part + 1) + "...");
            
            try (
                    java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(INPUT_FILES[part]));
                    java.io.BufferedWriter bw = new java.io.BufferedWriter(new java.io.FileWriter(OUTPUT_FILES[part]))
            ) {
                String line;
                while ((line = br.readLine()) != null) {
                    String translatedText = getTranslatedText(translate, line);
                    bw.write(translatedText);
                    bw.newLine();
                    bw.flush();
                }
            }

            s3Client.putObject(PutObjectRequest.builder()
                            .bucket(DownloadWordList.BUCKET_NAME)
                            .key(S3_PATHS[part])
                            .build(),
                    RequestBody.fromFile(Paths.get(OUTPUT_FILES[part])));
        }

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
