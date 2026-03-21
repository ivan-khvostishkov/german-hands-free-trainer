/**
 * Copyright (c) 2023 Ivan Khvostishkov & NoSocial.Net
 */
package net.nosocial.germanb1;

import net.nosocial.util.ProcessUtil;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.io.IOException;

public class AssembleFinalVideos {
    private static final String[] FILE_NAMES = {
        "german-b1-phrases-part1.mp4",
        "german-b1-phrases-part2.mp4",
        "german-b1-phrases-part3.mp4"
    };

    public static final S3Client s3Client = S3Client.builder()
            .region(Region.EU_WEST_1)
            .build();

    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("German Hands-free Trainer (c) 2023-2026 by NoSocial.Net");

        for (int part = 0; part < 3; part++) {
            System.out.println("Processing part " + (part + 1) + "...");
            
            System.out.println("Generating randomized file list for part " + (part + 1) + "...");

            int count = GenerateVideos.countNarratedFiles(part);
            int totalPhrases = count / 3;
            System.out.println("Found " + count + " files for " + totalPhrases + " phrases (part " + (part + 1) + ")");

            // generate array of numbers from 1 to totalPhrases and randomize it
            int[] randomized = new int[totalPhrases];
            for (int i = 0; i < totalPhrases; i++) {
                randomized[i] = i + 1;
            }

            int seed = 42 + part;
            System.out.println("Random seed: " + seed);
            java.util.Random random = new java.util.Random(seed);

            for (int k = 0; k < 100; k++) {
                for (int i = 0; i < totalPhrases; i++) {
                    int j = (int) (random.nextDouble() * totalPhrases);
                    int temp = randomized[i];
                    randomized[i] = randomized[j];
                    randomized[j] = temp;
                }
            }

            File fileList = new File("./out/mp4-file-list-part" + (part + 1) + ".txt");

            try {
                java.io.PrintWriter pw = new java.io.PrintWriter(fileList);
                int limit = Math.min(1600, totalPhrases);
                for (int i = 0; i < limit; i++) {
                    pw.println(String.format("file ./videos-part" + (part + 1) + "/" +
                            NarratePhrases.MP3_FILE_NAME_DE.replace(".mp3", ".mp4"), part + 1, randomized[i]));
                    pw.println(String.format("file ./videos-part" + (part + 1) + "/" +
                            NarratePhrases.MP3_FILE_NAME_EN.replace(".mp3", ".mp4"), part + 1, randomized[i]));
                    pw.println(String.format("file ./videos-part" + (part + 1) + "/" +
                            NarratePhrases.MP3_FILE_NAME_DE_SLOW.replace(".mp3", ".mp4"), part + 1, randomized[i]));
                }
                pw.close();
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }

            assembleFinalVideo(part);

            System.out.println("Uploading " + FILE_NAMES[part] + " to S3...");
            s3Client.putObject(
                    PutObjectRequest.builder().bucket(DownloadWordList.BUCKET_NAME)
                            .key("goethe_de/" + FILE_NAMES[part]).build(),
                    RequestBody.fromFile(new File("out/" + FILE_NAMES[part]).toPath())
            );
        }

        System.out.println("Done!");
    }

    private static void assembleFinalVideo(int part) throws IOException, InterruptedException {
        System.out.println("Generating " + FILE_NAMES[part] + "...");

        ProcessBuilder builder = new ProcessBuilder(
                "ffmpeg", "-y", "-f", "concat", "-safe", "0",
                "-i", "./out/mp4-file-list-part" + (part + 1) + ".txt",
                "-c", "copy",
                "./out/" + FILE_NAMES[part]
        );
        if (!ProcessUtil.checkOutput(builder)) {
            System.out.println("Error assembling video");
            System.exit(1);
        }
    }
}