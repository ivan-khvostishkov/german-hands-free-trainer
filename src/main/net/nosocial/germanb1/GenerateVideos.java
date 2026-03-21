/**
 * Copyright (c) 2023 Ivan Khvostishkov & NoSocial.Net
 */
package net.nosocial.germanb1;

import net.nosocial.util.ProcessUtil;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class GenerateVideos {
    public static final String[] S3_PATHS = {
        "goethe_de/narrate-b1-part1/",
        "goethe_de/narrate-b1-part2/",
        "goethe_de/narrate-b1-part3/"
    };

    public static final String[] S3_MP4_PATHS = {
        "goethe_de/videos-b1-part1/",
        "goethe_de/videos-b1-part2/",
        "goethe_de/videos-b1-part3/"
    };

    public static final S3Client s3Client = S3Client.builder()
            .region(Region.EU_WEST_1)
            .build();

    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("German Hands-free Trainer (c) 2023-2026 by NoSocial.Net");
        System.out.println("Generating videos with FFMpeg...");

        for (int part = 0; part < 3; part++) {
            System.out.println("Processing part " + (part + 1) + "...");
            
            int count = countNarratedFiles(part);
            int totalPhrases = count / 3;
            System.out.println("Found " + count + " files for " + totalPhrases + " phrases (part " + (part + 1) + ")");

            System.out.println("Downloading files for part " + (part + 1) + "...");
            if (!downloadFiles(part)) {
                System.out.println("Error downloading files for part " + (part + 1));
                return;
            }

            System.out.println("Generating videos for part " + (part + 1) + "...");
            
            ExecutorService executor = Executors.newFixedThreadPool(12);
            
            for (int i = 0; i < totalPhrases; i++) {
                final int phraseIndex = i;
                int finalPart = part;
                executor.submit(() -> {
                    try {
                        generateVideoWithSubtitles(String.format(NarratePhrases.MP3_FILE_NAME_DE, finalPart + 1, phraseIndex + 1), finalPart);
                        generateVideoWithSubtitles(String.format(NarratePhrases.MP3_FILE_NAME_EN, finalPart + 1, phraseIndex + 1), finalPart);
                        generateVideoWithSubtitles(String.format(NarratePhrases.MP3_FILE_NAME_DE_SLOW, finalPart + 1, phraseIndex + 1), finalPart);
                    } catch (IOException | InterruptedException e) {
                        System.err.println("Error generating videos for phrase " + (phraseIndex + 1) + ": " + e.getMessage());
                        throw new RuntimeException(e);
                    }
                });
            }
            
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.MINUTES)) {
                    System.err.println("Video generation timed out for part " + (part + 1));
                    executor.shutdownNow();
                    return;
                }
            } catch (InterruptedException e) {
                System.err.println("Video generation interrupted for part " + (part + 1));
                executor.shutdownNow();
                return;
            }

            System.out.println("Uploading videos to S3 for part " + (part + 1) + "...");
            if (!uploadFiles(part)) {
                System.out.println("Error uploading files for part " + (part + 1));
                return;
            }
        }

        System.out.println("Done");
    }

    static Integer countNarratedFiles(int part) {
        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(DownloadWordList.BUCKET_NAME)
                .prefix(S3_PATHS[part])
                .build();

        int count = 0;
        ListObjectsV2Response response;
        do {
            response = s3Client.listObjectsV2(request);
            for (S3Object s3Object : response.contents()) {
                if (s3Object.size() > 0) {
                    count++;
                }
            }
            request = request.toBuilder().continuationToken(response.nextContinuationToken()).build();
        } while (response.isTruncated());

        if (count % 3 != 0) {
            System.out.println("Files count is not divisible by 3 for part " + (part + 1));
            System.exit(1);
        }
        return count;
    }

    private static void generateVideoWithSubtitles(String mp3FileName, int part) throws IOException, InterruptedException {
        String partDir = "./out/videos-part" + (part + 1);
        if (new java.io.File(partDir).mkdirs()) {
            System.out.println("Created output directory " + partDir);
        }

        String mp4FileName = mp3FileName.replace(".mp3", ".mp4");
        System.out.println("Generating " + mp4FileName + " from " + mp3FileName);

        @SuppressWarnings("SpellCheckingInspection")
        ProcessBuilder builder = new ProcessBuilder(
                "ffmpeg", "-y", "-i", "./out/narrate-part" + (part + 1) + "/" + mp3FileName,
                "-loop", "1", "-i", "./img/german-b1-trainer-part" + (part + 1) + ".png",
                "-vf", "subtitles=./out/narrate-part" + (part + 1) + "/" + mp3FileName.replace(".mp3", ".srt") + ":force_style='FontName=Gratimo Classic,MaxLines=3,WrapStyle=2,MarginV=30'",
                "-c:v", "libx264", "-tune", "stillimage", "-crf", "0", "-c:a", "copy", "-shortest",
                partDir + "/" + mp4FileName
        );

        if (!ProcessUtil.checkOutput(builder)) {
            System.out.println("Error generating video");
            System.exit(1);
        }
    }

    private static boolean downloadFiles(int part) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(
                "aws", "s3", "sync",
                "s3://" + DownloadWordList.BUCKET_NAME + "/" + S3_PATHS[part],
                "./out/narrate-part" + (part + 1) + "/"
        );

        return ProcessUtil.checkOutput(builder);
    }

    private static boolean uploadFiles(int part) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(
                "aws", "s3", "sync",
                "./out/videos-part" + (part + 1) + "/",
                "s3://" + DownloadWordList.BUCKET_NAME + "/" + S3_MP4_PATHS[part]
        );
        return ProcessUtil.checkOutput(builder);
    }
}
