package me.phoenix.wnm;

import me.phoenix.wnm.exceptions.MissingArgumentException;
import me.phoenix.wnm.exceptions.NoConnectionException;
import me.phoenix.wnm.generators.*;

import javax.swing.*;
import java.io.File;

public class Main {
    private final static Main APPLICATION = new Main();

    private final static ImageGenerator
            IG = new ImageGenerator(),
            OIG = new OfflineImageGenerator(),
            IIG = new InfiniteImageGenerator();

    public static void main(String[] args) {
        int count;
        int threads;
        ImageGenerator ig = IG;

        try {
            count = Integer.parseInt(args[0]);
        } catch (NumberFormatException ignored) {
            switch (args[0]) {
                case "offline": count = 1; ig = OIG;
                case "indefinite": ig = IIG;
                default: throw new IllegalArgumentException();
            }
        } catch (IndexOutOfBoundsException e) {
            throw new RuntimeException(e);
        }

        try {
            threads = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            throw new RuntimeException(e);
        } catch (IndexOutOfBoundsException e) {
            throw new MissingArgumentException();
        }

        long startTime = System.currentTimeMillis();
        APPLICATION.produce(count, ig, threads);
        long finishTime = System.currentTimeMillis();

        System.out.println("Average time per word: " + (finishTime-startTime)/1000);
    }

    public void produce(int count, ImageGenerator generator, File directory) {
        produce(count, generator, directory, count);
    }
    public void produce(int count, ImageGenerator generator, int threads) {
        JFileChooser jfc = new JFileChooser();
        jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        jfc.showOpenDialog(null);

        produce(count, generator, jfc.getSelectedFile(), threads);
    }
    public void produce(int count, ImageGenerator generator, File directory, int threads) {
        try {
            System.out.println(generator.getGeneratorName() + " is starting...");
            generator.createImages(count, directory, threads);
        } catch (NoConnectionException e) {
            generator = new OfflineImageGenerator();
            System.err.println("Switching to offline generator: No internet connection.");
            System.out.println(generator.getGeneratorName() + " has been shut down.");
            produce(count, generator, directory);
        }
    }
}