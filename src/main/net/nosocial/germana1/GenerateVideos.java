/**
 * Copyright (c) 2023 Ivan Khvostishkov & NoSocial.Net
 */
package net.nosocial.germana1;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import java.io.IOException;
import java.util.List;

public class GenerateVideos {
    public static final String S3_PATH = "goethe_de/narrate/";
    public static final String S3_MP4_PATH = "goethe_de/videos/";

    public static final AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
            .withRegion("eu-west-1")
            .build();

    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("German A1 Trainer Tool (c) 2023 by NoSocial.Net");

        System.out.println("Generating videos with FFMpeg...");

        int count = countNarratedFiles();
        int totalPhrases = count / 9;
        System.out.println("Found " + count + " files for " + totalPhrases + " phrases");

        System.out.println("Downloading files...");
        if (!downloadFiles()) return;

        System.out.println("Generating videos...");

        for (int i = 0; i < totalPhrases; i++) {
            generateVideoWithSubtitles(String.format(NarratePhrases.MP3_FILE_NAME_DE, i + 1));
            generateVideoWithSubtitles(String.format(NarratePhrases.MP3_FILE_NAME_EN, i + 1));
            generateVideoWithSubtitles(String.format(NarratePhrases.MP3_FILE_NAME_DE_SLOW, i + 1));
        }

        System.out.println("Uploading videos to S3...");

        if (!uploadFiles()) return;

        System.out.println("Done");
    }

    static Integer countNarratedFiles() {
        ObjectListing objectListing = s3Client.listObjects(DownloadWordList.BUCKET_NAME, S3_PATH);
        int count = 0;
        while (true) {
            List<S3ObjectSummary> objectSummaries = objectListing.getObjectSummaries();
            for (S3ObjectSummary objectSummary : objectSummaries) {
                if (objectSummary.getSize() > 0) {
                    count++;
                }
            }
            if (objectListing.isTruncated()) {
                objectListing = s3Client.listNextBatchOfObjects(objectListing);
            } else {
                break;
            }
        }
        if (count % 9 != 0) {
            System.out.println("Files count is not divisible by 9");
            System.exit(1);
        }
        return count;
    }

    private static void generateVideoWithSubtitles(String mp3FileName) throws IOException, InterruptedException {
        String mp4FileName = mp3FileName.replace(".mp3", ".mp4");
        System.out.println("Generating " + mp4FileName + " from " + mp3FileName);

        @SuppressWarnings("SpellCheckingInspection")
        ProcessBuilder builder = new ProcessBuilder(
                "ffmpeg", "-y", "-i", "./out/narrate/" + mp3FileName,
                "-loop", "1", "-i", "./img/german-a1-trainer.png",
                "-vf", "subtitles=./out/narrate/" + mp3FileName.replace(".mp3", ".srt") + ":force_style='MaxLines=3,WrapStyle=2,MarginV=30'",
                "-c:v", "libx264", "-tune", "stillimage", "-crf", "0", "-c:a", "copy", "-shortest",
                "./out/videos/" + mp4FileName
        );
        // start the process
        Process p = builder.start();
        if (p.waitFor() != 0) {
            System.out.println("Error generating video");
            // fetch stdout and stderr
            System.out.println(new String(p.getInputStream().readAllBytes()));
            System.out.println(new String(p.getErrorStream().readAllBytes()));
            System.exit(1);
        }
    }

    private static boolean downloadFiles() throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(
                "aws", "s3", "sync",
                "s3://" + DownloadWordList.BUCKET_NAME + "/" + S3_PATH,
                "./out/narrate/"
        );
        // start the process
        Process p = builder.start();
        if (p.waitFor() != 0) {
            System.out.println("Error downloading files");
            return false;
        }
        return true;
    }

    private static boolean uploadFiles() throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(
                "aws", "s3", "sync",
                "./out/videos/",
                "s3://" + DownloadWordList.BUCKET_NAME + "/" + S3_MP4_PATH
                );
        // start the process
        Process p = builder.start();
        if (p.waitFor() != 0) {
            System.out.println("Error uploading files");
            return false;
        }
        return true;
    }
}
