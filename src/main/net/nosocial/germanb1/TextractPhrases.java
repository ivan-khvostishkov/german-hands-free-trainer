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
import software.amazon.awssdk.services.textract.TextractClient;
import software.amazon.awssdk.services.textract.model.*;

import java.io.File;
import java.io.PrintWriter;
import java.util.List;

public class TextractPhrases {
    public static final String S3_PATH = "goethe_de/extract/b1-textract-blocks.json";
    public static final String JSON_FILE = "out/b1-textract-blocks.json";

    public static void main(String[] args) {
        System.out.println("German B1 Hands-Free Trainer (c) 2013-2025 by NoSocial.Net");

        try (TextractClient client = TextractClient.builder()
                .region(Region.EU_WEST_1)
                .build()) {

            StartDocumentAnalysisResponse result = client.startDocumentAnalysis(
                    StartDocumentAnalysisRequest.builder()
                            .featureTypes(FeatureType.TABLES, FeatureType.FORMS)
                            .documentLocation(DocumentLocation.builder()
                                    .s3Object(S3Object.builder()
                                            .name(DownloadWordList.PATH)
                                            .bucket(DownloadWordList.BUCKET_NAME)
                                            .build())
                                    .build())
                            .build());

            String jobId = result.jobId();
            System.out.println("JobId: " + jobId);

            JSONArray allBlocks = getAllBlocks(client, jobId);

            System.out.println("Saving " + allBlocks.length() + " blocks to " + JSON_FILE);
            try (PrintWriter out = new PrintWriter(JSON_FILE)) {
                out.print(allBlocks.toString(2));
            }

            uploadToS3();

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.out.println("Done.");
    }

    private static void uploadToS3() {
        try (S3Client s3Client = S3Client.builder()
                .region(Region.EU_WEST_1)
                .build()) {

            s3Client.putObject(PutObjectRequest.builder()
                            .bucket(DownloadWordList.BUCKET_NAME)
                            .key(S3_PATH)
                            .build(),
                    RequestBody.fromFile(new File(JSON_FILE)));

            System.out.println("Uploaded to S3: " + S3_PATH);
        }
    }

    private static JSONArray getAllBlocks(TextractClient client, String jobId) {
        GetDocumentAnalysisResponse getDocResult = null;

        long startTime = System.currentTimeMillis();
        long endTime = startTime + (5 * 60 * 1000);

        while (System.currentTimeMillis() < endTime) {
            getDocResult = client.getDocumentAnalysis(GetDocumentAnalysisRequest.builder()
                    .jobId(jobId)
                    .build());
            String jobStatus = getDocResult.jobStatusAsString();
            System.out.println("Job status: " + jobStatus);
            if (jobStatus.equals("SUCCEEDED")) {
                break;
            }
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (getDocResult == null || getDocResult.blocks() == null) {
            throw new IllegalStateException("No result");
        }

        JSONArray allBlocks = new JSONArray();
        String nextToken;
        do {
            convertBlocksToJson(getDocResult.blocks(), allBlocks);
            nextToken = getDocResult.nextToken();
            if (nextToken != null) {
                getDocResult = client.getDocumentAnalysis(GetDocumentAnalysisRequest.builder()
                        .jobId(jobId)
                        .nextToken(nextToken)
                        .build());
            }
        } while (nextToken != null);

        return allBlocks;
    }

    private static void convertBlocksToJson(List<Block> blocks, JSONArray jsonArray) {
        for (Block block : blocks) {
            JSONObject blockJson = new JSONObject();
            blockJson.put("blockType", block.blockType().toString());
            blockJson.put("text", block.text());

            if (block.geometry() != null && block.geometry().boundingBox() != null) {
                BoundingBox bb = block.geometry().boundingBox();
                JSONObject geometry = new JSONObject();
                geometry.put("left", bb.left());
                geometry.put("top", bb.top());
                geometry.put("width", bb.width());
                geometry.put("height", bb.height());
                blockJson.put("geometry", geometry);
            }

            jsonArray.put(blockJson);
        }
    }
}
