/**
 * Copyright (c) 2013-2025 Ivan Khvostishkov & NoSocial.Net
 */
package net.nosocial.germanb1;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class DownloadWordList {

    public static final String BUCKET_NAME = "german-a1-trainer";
    public static final String PATH = "goethe_de/Goethe-Zertifikat_B1_Wortliste.pdf";
    public static final String FILE_URL = "https://www.goethe.de/pro/relaunch/prf/de/Goethe-Zertifikat_B1_Wortliste.pdf";

    public static void main(String[] args) throws IOException, URISyntaxException {
        System.out.println("German B1 Hands-Free Trainer (c) 2013-2025 by NoSocial.Net");

        try (S3Client s3Client = S3Client.builder()
                .region(Region.EU_WEST_1)
                .build()) {

            List<Bucket> buckets = s3Client.listBuckets().buckets();

            boolean bucketExists = buckets.stream()
                    .anyMatch(b -> b.name().equals(BUCKET_NAME));

            if (!bucketExists) {
                System.out.println("Bucket " + BUCKET_NAME + " does not exist. Creating...");
                s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET_NAME).build());
            } else {
                System.out.println("Bucket " + BUCKET_NAME + " exists.");
            }

            System.out.println("Downloading file to S3...");

            Path tempFile = Files.createTempFile("download", ".pdf");
            try (InputStream inputStream = new URI(FILE_URL).toURL().openStream()) {
                Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }
            var response = s3Client.putObject(
                    PutObjectRequest.builder().bucket(BUCKET_NAME).key(PATH).build(),
                    RequestBody.fromFile(tempFile)
            );

            System.out.println("Upload response: " + response.eTag());
            Files.delete(tempFile);
        }

        System.out.println("Done.");
    }
}
