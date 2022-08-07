package me.phoenix.wnm.generators;

import me.phoenix.wnm.exceptions.NoConnectionException;

import java.io.File;

public class InfiniteImageGenerator extends ImageGenerator {
    @Override
    public void createImages(int count, File destinationFolder, int threads) throws NoConnectionException {
        while (true) {
            populateQueue(1, threads);
            produceFromQueue(destinationFolder);
        }
    }
}
