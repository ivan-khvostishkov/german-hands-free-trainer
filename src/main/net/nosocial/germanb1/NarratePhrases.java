/**
 * Copyright (c) 2023 Ivan Khvostishkov & NoSocial.Net
 * Requires software.amazon.awssdk:polly:2.29.15 or higher
 */
package net.nosocial.germanb1;

import org.json.JSONObject;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.polly.PollyClient;
import software.amazon.awssdk.services.polly.model.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.io.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class NarratePhrases {
    public static final String[] NARRATE_PATHS = {
        "goethe_de/narrate-b1-part1/",
        "goethe_de/narrate-b1-part2/",
        "goethe_de/narrate-b1-part3/"
    };

    public static final String MP3_FILE_NAME_DE = "b1-phrases-p%d-%03d-01-de.mp3";
    public static final String MP3_FILE_NAME_EN = "b1-phrases-p%d-%03d-02-en.mp3";
    public static final String MP3_FILE_NAME_DE_SLOW = "b1-phrases-p%d-%03d-03-de-slow.mp3";

    public static final S3Client s3Client = S3Client.builder()
            .region(Region.EU_WEST_1)
            .build();

    private static boolean fileExistsInS3(String s3Path) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(DownloadWordList.BUCKET_NAME)
                    .key(s3Path)
                    .build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }

    private static void narrate(PollyClient polly, Voice voice, String phraseSSML, String fileName, String phrase) throws IOException {
        synthesizeAudio(polly, voice, phraseSSML, fileName);
        synthesizeSpeechMarks(polly, voice, phraseSSML, fileName.replace(".mp3", ".json"));
        covertSpeechMarksToSubtitles(phrase, fileName.replace(".mp3", ".json"), fileName.replace(".mp3", ".srt"));
    }

    private static void covertSpeechMarksToSubtitles(String phrase, String jsonS3Path, String srtS3Path) throws IOException {
        System.out.println("Converting speech marks to subtitles...");
        String jsonLines = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                .bucket(DownloadWordList.BUCKET_NAME)
                .key(jsonS3Path)
                .build()).asUtf8String();
        String[] json = jsonLines.split("\n");
        JSONObject jsonStart = new JSONObject(json[0]);
        JSONObject jsonEnd = new JSONObject(json[1]);
        int startMillis = jsonStart.getInt("time");
        int endMillis = jsonEnd.getInt("time");

        LocalTime startTime = LocalTime.ofNanoOfDay(startMillis * 1000000L).truncatedTo(ChronoUnit.MILLIS);
        LocalTime endTime = LocalTime.ofNanoOfDay(endMillis * 1000000L).truncatedTo(ChronoUnit.MILLIS);

        Writer w = new StringWriter();
        try (BufferedWriter bw = new BufferedWriter(w)) {
            bw.write("1\n");
            bw.write(startTime.format(DateTimeFormatter.ofPattern("HH:mm:ss,SSS"))
                    + " --> "
                    + endTime.format(DateTimeFormatter.ofPattern("HH:mm:ss,SSS")) + "\n");
            bw.write(phrase + "\n");
        }

        s3Client.putObject(
                software.amazon.awssdk.services.s3.model.PutObjectRequest.builder()
                        .bucket(DownloadWordList.BUCKET_NAME)
                        .key(srtS3Path)
                        .build(),
                RequestBody.fromString(w.toString())
        );
    }

    private static String quoteForPolly(String text) {
        text = text.replace("\"", "&quot;");
        text = text.replace("'", "&apos;");
        text = text.replaceAll("\\(.+\\)", "");
        return text;
    }

    private static void synthesizeAudio(PollyClient polly, Voice voice, String text, String s3Path) {
        SynthesizeSpeechRequest synthReq = SynthesizeSpeechRequest.builder()
                .text(text)
                .textType(TextType.SSML)
                .voiceId(voice.id())
                .outputFormat(OutputFormat.MP3)
                .engine(Engine.NEURAL)
                .build();

        var synthRes = polly.synthesizeSpeech(synthReq);
        System.out.println("Saving audio to S3...");

        try {
            byte[] audioBytes = synthRes.readAllBytes();
            s3Client.putObject(
                    software.amazon.awssdk.services.s3.model.PutObjectRequest.builder()
                            .bucket(DownloadWordList.BUCKET_NAME)
                            .key(s3Path)
                            .build(),
                    RequestBody.fromBytes(audioBytes)
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void synthesizeSpeechMarks(PollyClient polly, Voice voice, String text, String s3Path) {
        SynthesizeSpeechRequest synthReq = SynthesizeSpeechRequest.builder()
                .text(text)
                .textType(TextType.SSML)
                .speechMarkTypes(SpeechMarkType.SSML)
                .voiceId(voice.id())
                .outputFormat(OutputFormat.JSON)
                .engine(Engine.NEURAL)
                .build();

        var synthRes = polly.synthesizeSpeech(synthReq);
        System.out.println("Saving speech marks to S3...");

        try {
            byte[] jsonBytes = synthRes.readAllBytes();
            s3Client.putObject(
                    software.amazon.awssdk.services.s3.model.PutObjectRequest.builder()
                            .bucket(DownloadWordList.BUCKET_NAME)
                            .key(s3Path)
                            .build(),
                    RequestBody.fromBytes(jsonBytes)
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public static void main(String[] args) throws IOException {
        System.out.println("German Hands-free Trainer (c) 2023-2026 by NoSocial.Net");
        System.out.println("Narrating phrases with Amazon Polly...");

        PollyClient polly = PollyClient.builder()
                .region(Region.EU_WEST_1)
                .build();

        DescribeVoicesRequest describeVoicesRequest = DescribeVoicesRequest.builder().build();
        DescribeVoicesResponse describeVoicesResult = polly.describeVoices(describeVoicesRequest);
        List<Voice> voices = describeVoicesResult.voices();

        Voice germanVoice = null;
        Voice englishVoice = null;
        for (Voice v : voices) {
            if (v.id().equals(VoiceId.DANIEL) && v.languageCode().equals(LanguageCode.DE_DE)
                    && v.gender().equals(Gender.MALE)
                    && v.supportedEngines().contains(Engine.NEURAL)) {
                germanVoice = v;
            }
            if (v.id().equals(VoiceId.AMY) && v.languageCode().equals(LanguageCode.EN_GB)
                    && v.gender().equals(Gender.FEMALE)
                    && v.supportedEngines().contains(Engine.NEURAL)) {
                englishVoice = v;
            }
        }

        System.out.println("Will use the German voice: " + germanVoice);
        System.out.println("Will use the English voice: " + englishVoice);

        if (germanVoice == null || englishVoice == null) {
            System.out.println("Voices not found!");
            return;
        }

        for (int part = 0; part < 3; part++) {
            System.out.println("Processing part " + (part + 1) + "...");
            
            File germanFile = new File(TranslatePhrases.INPUT_FILES[part]);
            File englishFile = new File(TranslatePhrases.OUTPUT_FILES[part]);

            String[] germanPhrases = new BufferedReader(new FileReader(germanFile)).lines().toArray(String[]::new);
            String[] englishPhrases = new BufferedReader(new FileReader(englishFile)).lines().toArray(String[]::new);

            if (germanPhrases.length != englishPhrases.length) {
                System.out.println("Phrases count mismatch for part " + (part + 1) + "!");
                return;
            }

            for (int i = 0; i < germanPhrases.length; i++) {
                String germanFileName = NARRATE_PATHS[part] + String.format(MP3_FILE_NAME_DE, part + 1, i + 1);
                String germanSlowFileName = NARRATE_PATHS[part] + String.format(MP3_FILE_NAME_DE_SLOW, part + 1, i + 1);
                String englishFileName = NARRATE_PATHS[part] + String.format(MP3_FILE_NAME_EN, part + 1, i + 1);

                if (fileExistsInS3(germanFileName) && fileExistsInS3(germanSlowFileName) && fileExistsInS3(englishFileName)) {
                    System.out.println("Skipping phrase " + (i + 1) + " of " + germanPhrases.length + " (part " + (part + 1) + ") - already processed");
                    continue;
                }

                System.out.println("Narrating phrase " + (i + 1) + " of " + germanPhrases.length + " (part " + (part + 1) + ")");

                String germanPhraseSSML = "<speak><mark name=\"sub_start\"/><prosody rate=\"medium\">\n"
                        + quoteForPolly(germanPhrases[i])
                        + "\n<break time=\"5s\"/>\n"
                        + "</prosody><mark name=\"sub_end\"/></speak>";

                String germanPhraseSlowSSML = "<speak><mark name=\"sub_start\"/><prosody rate=\"x-slow\">\n"
                        + quoteForPolly(germanPhrases[i])
                        + "\n<break time=\"5s\"/>\n"
                        + "</prosody><mark name=\"sub_end\"/></speak>";

                String englishPhraseSSML = "<speak><mark name=\"sub_start\"/><prosody rate=\"medium\">\n"
                        + quoteForPolly(englishPhrases[i])
                        + "\n<break time=\"5s\"/>\n"
                        + "</prosody><mark name=\"sub_end\"/></speak>";

                narrate(polly, germanVoice, germanPhraseSSML, germanFileName, germanPhrases[i]);
                narrate(polly, germanVoice, germanPhraseSlowSSML, germanSlowFileName, germanPhrases[i]);
                narrate(polly, englishVoice, englishPhraseSSML, englishFileName, englishPhrases[i]);
            }
        }

        System.out.println("Done.");
    }
}