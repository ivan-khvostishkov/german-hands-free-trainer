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

public class GenerateVideos {
    public static final String S3_PATH = "goethe_de/narrate-b1/";
    public static final String S3_MP4_PATH = "goethe_de/videos-b1/";

    public static final S3Client s3Client = S3Client.builder()
            .region(Region.EU_WEST_1)
            .build();

    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("German Hands-free Trainer (c) 2023-2026 by NoSocial.Net");

        System.out.println("Generating videos with FFMpeg...");

        int count = countNarratedFiles();
        int totalPhrases = count / 9;
        System.out.println("Found " + count + " files for " + totalPhrases + " phrases");

        System.out.println("Downloading files...");
        if (!downloadFiles()) {
            System.out.println("Error downloading files");
            return;
        }

        System.out.println("Generating videos...");

        for (int i = 0; i < totalPhrases; i++) {
            generateVideoWithSubtitles(String.format(NarratePhrases.MP3_FILE_NAME_DE, i + 1));
            generateVideoWithSubtitles(String.format(NarratePhrases.MP3_FILE_NAME_EN, i + 1));
            generateVideoWithSubtitles(String.format(NarratePhrases.MP3_FILE_NAME_DE_SLOW, i + 1));
        }

        System.out.println("Uploading videos to S3...");

        if (!uploadFiles()) {
            System.out.println("Error uploading files");
            return;
        }

        System.out.println("Done");
    }

    static Integer countNarratedFiles() {
        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(DownloadWordList.BUCKET_NAME)
                .prefix(S3_PATH)
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

        if (count % 9 != 0) {
            System.out.println("Files count is not divisible by 9");
            System.exit(1);
        }
        return count;
    }

    private static void generateVideoWithSubtitles(String mp3FileName) throws IOException, InterruptedException {
        if (new java.io.File("./out/videos").mkdirs()) {
            System.out.println("Created output directory ./out/videos");
        }

        String mp4FileName = mp3FileName.replace(".mp3", ".mp4");
        System.out.println("Generating " + mp4FileName + " from " + mp3FileName);

        @SuppressWarnings("SpellCheckingInspection")
        ProcessBuilder builder = new ProcessBuilder(
                "ffmpeg", "-y", "-i", "./out/narrate/" + mp3FileName,
                "-loop", "1", "-i", "./img/german-b1-trainer.png",
                "-vf", "subtitles=./out/narrate/" + mp3FileName.replace(".mp3", ".srt") + ":force_style='FontName=Gratimo Classic'",
                "-c:v", "libx264", "-tune", "stillimage", "-crf", "0", "-c:a", "copy", "-shortest",
                "./out/videos/" + mp4FileName
        );

        if (!ProcessUtil.checkOutput(builder)) {
            System.out.println("Error generating video");
            System.exit(1);
        }
    }

    private static boolean downloadFiles() throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(
                "aws", "s3", "sync",
                "s3://" + DownloadWordList.BUCKET_NAME + "/" + S3_PATH,
                "./out/narrate/"
        );

        return ProcessUtil.checkOutput(builder);
    }

    private static boolean uploadFiles() throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(
                "aws", "s3", "sync",
                "./out/videos/",
                "s3://" + DownloadWordList.BUCKET_NAME + "/" + S3_MP4_PATH
        );
        return ProcessUtil.checkOutput(builder);
    }
}
