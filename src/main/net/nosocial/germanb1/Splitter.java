/**
 * Copyright (c) 2023 Ivan Khvostishkov & NoSocial.Net
 */
package net.nosocial.germanb1;

import java.io.*;
import java.util.*;

public class Splitter {
    public static final String INPUT_FILE = "out/b1-phrases-edited.txt";
    public static final String[] OUTPUT_FILES = {
        "out/b1-phrases-edited-part1.txt",
        "out/b1-phrases-edited-part2.txt", 
        "out/b1-phrases-edited-part3.txt"
    };

    public static void main(String[] args) throws IOException {
        System.out.println("German Hands-Free Trainer (c) 2023-2026 by NoSocial.Net");
        System.out.println("Splitting phrases into 3 parts...");

        List<String> phrases = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(INPUT_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    phrases.add(line);
                }
            }
        }

        System.out.println("Found " + phrases.size() + " phrases");

        // Shuffle phrases
        Random random = new Random(42);
        Collections.shuffle(phrases, random);

        // Split into 3 parts
        int partSize = phrases.size() / 3;
        for (int part = 0; part < 3; part++) {
            int start = part * partSize;
            int end = (part == 2) ? phrases.size() : (part + 1) * partSize;
            
            try (PrintWriter pw = new PrintWriter(OUTPUT_FILES[part])) {
                for (int i = start; i < end; i++) {
                    pw.println(phrases.get(i));
                }
            }
            System.out.println("Part " + (part + 1) + ": " + (end - start) + " phrases");
        }

        System.out.println("Done.");
    }
}