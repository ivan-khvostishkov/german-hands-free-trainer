/**
 * Copyright (c) 2023 Ivan Khvostishkov & NoSocial.Net
 */
package net.nosocial.germanb1;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.polly.AmazonPolly;
import com.amazonaws.services.polly.AmazonPollyClientBuilder;
import com.amazonaws.services.polly.model.*;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.json.JSONObject;

import java.io.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class NarratePhrases {
    public static final String NARRATE_PATH = "goethe_de/narrate-b1/";
    public static final String MP3_FILE_NAME_DE = "b1-phrases-p1-%03d-01-de.mp3";
    public static final String S3_MP3_PATH_DE = NARRATE_PATH + MP3_FILE_NAME_DE;
    public static final String MP3_FILE_NAME_EN = "b1-phrases-p1-%03d-02-en.mp3";
    public static final String S3_MP3_PATH_EN = NARRATE_PATH + MP3_FILE_NAME_EN;
    public static final String MP3_FILE_NAME_DE_SLOW = "b1-phrases-p1-%03d-03-de-slow.mp3";
    public static final String S3_MP3_PATH_DE_SLOW = NARRATE_PATH + MP3_FILE_NAME_DE_SLOW;

    public static final AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
            .withRegion("eu-west-1")
            .build();


    public static void main(String[] args) throws IOException {
        System.out.println("German Hands-free Trainer (c) 2023-2026 by NoSocial.Net");

        System.out.println("Narrating phrases with Amazon Polly...");

        // Based on https://docs.aws.amazon.com/polly/latest/dg/examples-java.html

        AwsClientBuilder.EndpointConfiguration endpoint = new AwsClientBuilder.EndpointConfiguration(
                "https://polly.eu-west-1.amazonaws.com", "eu-west-1");
        AmazonPolly polly = AmazonPollyClientBuilder.standard()
                .withEndpointConfiguration(endpoint).build();

        DescribeVoicesRequest describeVoicesRequest = new DescribeVoicesRequest();

        // Synchronously ask Amazon Polly to describe available TTS voices.
        DescribeVoicesResult describeVoicesResult = polly.describeVoices(describeVoicesRequest);
        List<Voice> voices = describeVoicesResult.getVoices();

        Voice germanVoice = null;
        Voice englishVoice = null;
        for (Voice v : voices) {
            if (v.getId().equals("Daniel") && v.getLanguageCode().equals("de-DE")
                    && v.getGender().equals("Male")
                    && v.getSupportedEngines().contains("neural")) {
                germanVoice = v;
            }
            if (v.getId().equals("Amy") && v.getLanguageCode().equals("en-GB")
                    && v.getGender().equals("Female")
                    && v.getSupportedEngines().contains("neural")) {
                englishVoice = v;
            }
        }

        System.out.println("Will use the German germanVoice: " + germanVoice);
        System.out.println("Will use the English germanVoice: " + englishVoice);
        if (germanVoice == null || englishVoice == null) {
            System.out.println("Voices not found!");
            return;
        }

        File germanFile = new File(TranslatePhrases.LOCAL_PATH_IN);
        File englishFile = new File(TranslatePhrases.LOCAL_PATH_OUT);

        // Read text file line by line
        String[] germanPhrases = new BufferedReader(new FileReader(germanFile)).lines().toArray(String[]::new);
        String[] englishPhrases = new BufferedReader(new FileReader(englishFile)).lines().toArray(String[]::new);

        if (germanPhrases.length != englishPhrases.length) {
            System.out.println("Phrases count mismatch!");
            return;
        }

        for (int i = 0; i < germanPhrases.length; i++) {
            System.out.println("Narrating phrase " + (i + 1) + " of " + germanPhrases.length);

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

            String germanFileName = String.format(S3_MP3_PATH_DE, i + 1);
            narrate(polly, germanVoice, germanPhraseSSML, germanFileName, germanPhrases[i]);

            String germanSlowFileName = String.format(S3_MP3_PATH_DE_SLOW, i + 1);
            narrate(polly, germanVoice, germanPhraseSlowSSML, germanSlowFileName, germanPhrases[i]);

            String englishFileName = String.format(S3_MP3_PATH_EN, i + 1);
            narrate(polly, englishVoice, englishPhraseSSML, englishFileName, englishPhrases[i]);
        }

        System.out.println("Done.");
    }

    private static void narrate(AmazonPolly polly, Voice voice, String phraseSSML, String fileName, String phrase) throws IOException {
        synthesizeAudio(polly, voice, phraseSSML, fileName);
        synthesizeSpeechMarks(polly, voice, phraseSSML, fileName.replace(".mp3", ".json"));
        covertSpeechMarksToSubtitles(phrase, fileName.replace(".mp3", ".json"), fileName.replace(".mp3", ".srt"));
    }

    private static void covertSpeechMarksToSubtitles(String phrase, String jsonS3Path, String srtS3Path) throws IOException {
        System.out.println("Converting speech marks to subtitles...");
        String jsonLines = s3Client.getObjectAsString(DownloadWordList.BUCKET_NAME, jsonS3Path);
        String[] json = jsonLines.split("\n");
        JSONObject jsonStart = new JSONObject(json[0]);
        JSONObject jsonEnd = new JSONObject(json[1]);
        int startMillis = jsonStart.getInt("time");
        int endMillis = jsonEnd.getInt("time");

        LocalTime startTime = LocalTime.ofNanoOfDay(startMillis * 1000000L).truncatedTo(ChronoUnit.MILLIS);
        LocalTime endTime = LocalTime.ofNanoOfDay(endMillis * 1000000L).truncatedTo(ChronoUnit.MILLIS);

        // See https://www.matroska.org/technical/subtitles.html#srt-subtitles
        Writer w = new StringWriter();
        try (BufferedWriter bw = new BufferedWriter(w)) {
            bw.write("1\n");
            bw.write(startTime.format(DateTimeFormatter.ofPattern("HH:mm:ss,SSS"))
                    + " --> "
                    + endTime.format(DateTimeFormatter.ofPattern("HH:mm:ss,SSS")) + "\n");
            bw.write(phrase + "\n");
        }

        InputStream srtS3Stream = new ByteArrayInputStream(w.toString().getBytes());
        s3Client.putObject(DownloadWordList.BUCKET_NAME, srtS3Path, srtS3Stream, null);
    }

    private static String quoteForPolly(String text) {
        text = text.replace("\"", "&quot;");
        text = text.replace("'", "&apos;");
        text = text.replaceAll("\\(.+\\)", "");
        return text;
    }

    private static void synthesizeAudio(AmazonPolly polly, Voice voice, String text, String s3Path) {
        SynthesizeSpeechRequest synthReq =
                new SynthesizeSpeechRequest().withText(text).withTextType(TextType.Ssml)
                        .withVoiceId(voice.getId())
                        .withOutputFormat(OutputFormat.Mp3)
                        .withEngine("neural");
        SynthesizeSpeechResult synthRes = polly.synthesizeSpeech(synthReq);
        InputStream synthStream = synthRes.getAudioStream();

        System.out.println("Saving audio to S3...");

        s3Client.putObject(DownloadWordList.BUCKET_NAME, s3Path, synthStream, null);
    }

    private static void synthesizeSpeechMarks(AmazonPolly polly, Voice voice, String text, String s3Path) {
        SynthesizeSpeechRequest synthReq =
                new SynthesizeSpeechRequest().withText(text).withTextType(TextType.Ssml)
                        .withSpeechMarkTypes(SpeechMarkType.Ssml)
                        .withVoiceId(voice.getId())
                        .withOutputFormat(OutputFormat.Json)
                        .withEngine("neural");
        SynthesizeSpeechResult synthRes = polly.synthesizeSpeech(synthReq);
        InputStream synthStream = synthRes.getAudioStream();

        System.out.println("Saving speech marks to S3...");

        AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                .withRegion("eu-west-1")
                .build();
        s3Client.putObject(DownloadWordList.BUCKET_NAME, s3Path, synthStream, null);
    }
}