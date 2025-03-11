package org.ankane.disco;

import java.io.FileReader;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Data.
 */
public abstract class Data {
    public static Dataset<Integer, String> loadMovieLens() throws Exception {
        String itemPath = downloadFile(
            "ml-100k/u.item",
            "https://files.grouplens.org/datasets/movielens/ml-100k/u.item",
            "553841ebc7de3a0fd0d6b62a204ea30c1e651aacfb2814c7a6584ac52f2c5701"
        );

        String dataPath = downloadFile(
            "ml-100k/u.data",
            "https://files.grouplens.org/datasets/movielens/ml-100k/u.data",
            "06416e597f82b7342361e41163890c81036900f418ad91315590814211dca490"
        );

        Map<String, String> movies = new HashMap<>();
        try (FileReader reader = new FileReader(itemPath)) {
            Scanner scanner = new Scanner(reader);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] row = line.split("\\|");
                // TODO fix encoding
                movies.put(row[0], row[1]);
            }
        }

        Dataset<Integer, String> data = new Dataset<>();
        try (FileReader reader = new FileReader(dataPath)) {
            Scanner scanner = new Scanner(reader);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] row = line.split("\t");
                data.add(Integer.parseInt(row[0]), movies.get(row[1]), Float.parseFloat(row[2]));
            }
        }

        return data;
    }

    private static String downloadFile(String filename, String url, String fileHash) throws Exception {
        String home = System.getProperty("user.home");

        Path dest = Paths.get(home, ".disco", filename);
        if (Files.exists(dest)) {
            return dest.toString();
        }

        if (!Files.exists(dest.getParent())) {
            Files.createDirectories(dest.getParent());
        }

        System.out.printf("Downloading data from %s\n", url);
        byte[] contents;
        try (InputStream in = new URI(url).toURL().openStream()) {
            contents = in.readAllBytes();
        }

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        String checksum = toHex(md.digest(contents));
        if (!checksum.equals(fileHash)) {
            throw new Exception(String.format("Bad checksum: %s", checksum));
        }

        Files.write(dest, contents);

        return dest.toString();
    }

    private static String toHex(byte[] bytes) {
        StringBuilder s = new StringBuilder();
        for (byte b : bytes) {
            s.append(String.format("%02x", b));
        }
        return s.toString();
    }
}
