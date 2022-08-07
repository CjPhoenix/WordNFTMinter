package me.phoenix.wnm.generators;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import io.quicktype.Definition;
import io.quicktype.*;
import me.phoenix.wnm.exceptions.NoConnectionException;
import me.phoenix.wnm.exceptions.NoDefintionException;
import me.phoenix.wnm.exceptions.NoExampleException;
import me.phoenix.wnm.exceptions.NoPhoneticsException;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class ImageGenerator {
    List<ImageConstructInfo> queue = new ArrayList<>();
    List<ImageConstructInfo> tmpQueue = new ArrayList<>();
    List<String> invalidatedWords = new ArrayList<>();
    HashMap<String,Integer> produced = new HashMap<>();

    static final int WIDTH = 2160, HEIGHT = 1080, MAX_CHARS = 52, LINE_SPACING = 0;

    static final Font WORD_FONT = new Font("Baskerville-SemiBold", Font.PLAIN, 172);
    static final Font PHONETICS_FONT = new Font("AmericanTypewriter", Font.PLAIN, 54);
    static final Font DEFINITION_FONT = new Font("Cochin", Font.PLAIN, 80);
    static final Font EXAMPLE_FONT = new Font("Didot-Italic",Font.PLAIN, 52);

    public void createImages(int count, File destinationFolder, int threads) throws NoConnectionException {
        populateQueue(count, threads);
        produceFromQueue(destinationFolder);
    }

    public void populateQueue(int count, int threads) throws NoConnectionException {
        for (int i = 0; i < threads-1; i++) {
            Thread qp = new Thread(() -> {
                while (queue.size() < count) {
                    loadTmpQueueToQueue();
                    try {
                        reloadTmpQueue();
                    } catch (NoConnectionException ignored) {}
                }
            });
            qp.start();
        }
        while (queue.size() < count) {
            loadTmpQueueToQueue();
            try {
                reloadTmpQueue();
            } catch (NoConnectionException ignored) {}
        }
    }
    public void produceFromQueue(File destinationFolder) {
        System.out.println("Producing from queue...");
        for (ImageConstructInfo ici :
                queue) {
            produceFromICI(ici, destinationFolder);
        }
    }

    public void reloadTmpQueue() throws NoConnectionException {
        String word = getWord();

        DictionaryResponse dr = null;
        do {
            try {
                dr = getDefinition(word);
            } catch (NoDefintionException e) {
                word = getWord();
            }
        } while (dr == null);

        for (Meaning meaning:
                dr.meanings) {
            if (dr.phonetics.length == 0) {
                return;
            }
            tmpQueue.add(new ImageConstructInfo(word.substring(0,1).toUpperCase()+word.substring(1).toLowerCase(),
                    meaning.partOfSpeech,
                    dr.phonetics[0].text,
                    meaning.definitions[0],
                    new Color((int) (Math.random()*120.0)+120, (int) (Math.random()*120.0)+120, (int) (Math.random()*120.0)+120)));
        }
    }
    public void loadTmpQueueToQueue() {
        tmpQueue.removeIf((ici) -> invalidatedWords.contains(ici.word));

        while (!tmpQueue.isEmpty()) {
            ImageConstructInfo ici = tmpQueue.stream().findFirst().get();
            tmpQueue.remove(0);

            try {
                verifyICI(ici);
                queue.add(ici);
                System.out.println("Word added: " + ici.word + " - Queue count: " + queue.size());
                return;
            } catch (NoDefintionException | NoPhoneticsException e) {
                invalidatedWords.add(ici.word);
            } catch (NoExampleException e) {
            }

        }
    }

    public void produceFromICI(ImageConstructInfo ici) {
        produceFromICI(ici, null);
    }
    public void produceFromICI(ImageConstructInfo ici, File destinationFolder) {
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = img.createGraphics();

        g2d.setColor(ici.bkgd);
        g2d.fillRect(0,0,WIDTH,HEIGHT);

        List<ImageItem> imageItems = new ArrayList<>();

        imageItems.add(new ImageItem(ici.word, WORD_FONT, calculateFontColor(ici.bkgd), 0, 260)); // Word

        if (ici.phonetics != null)
            imageItems.add(new ImageItem(ici.phonetics + "  -  " + ici.partOfSpeech, PHONETICS_FONT, calculateFontColorFade(ici.bkgd), 0, 350)); // Phonetics and Part of Speech

        imageItems.add(new ImageItem(ici.definition.definition, DEFINITION_FONT, calculateFontColor(ici.bkgd), 0, 500)); // Definition

        if (ici.definition.example != null) {
            int booster1 = (ici.definition.definition.length() / MAX_CHARS + 1);
            int booster2 = ((g2d.getFontMetrics().getHeight() + 15) *4 + LINE_SPACING);
            int booster = booster1 * booster2;
            imageItems.add(new ImageItem("\"" + ici.definition.example + "\"", EXAMPLE_FONT, calculateFontColor(ici.bkgd), 0, 500 + booster)); // Usage Example
        }

        for (ImageItem item: imageItems) {
            if (item.text == null) continue;

            g2d.setFont(item.font);
            g2d.setColor(item.color);
            AtomicInteger xd = new AtomicInteger(g2d.getFontMetrics().stringWidth(item.text));

            if (item.text.length() > MAX_CHARS) {
                AtomicReference<Integer> linesWritten = new AtomicReference<>(0);
                AtomicReference<String> charsToWrite = new AtomicReference<>("");

                Arrays.stream(item.text.split(" ")).toList().forEach((string) -> {
                    if ((charsToWrite.get() + string).length() <= MAX_CHARS) {
                        charsToWrite.set(charsToWrite.get() + " " + string);
                    } else {
                        xd.set(g2d.getFontMetrics().stringWidth(charsToWrite.get()));
                        int x = (WIDTH - xd.get())/2+item.xDelta;
                        int y = item.yDelta + linesWritten.get() * (g2d.getFontMetrics().getHeight() + LINE_SPACING);
                        g2d.drawString(charsToWrite.get(), x, y);
                        charsToWrite.set(string);
                        linesWritten.set(linesWritten.get() + 1);
                    }
                });
                xd.set(g2d.getFontMetrics().stringWidth(charsToWrite.get()));
                int x = (WIDTH - xd.get())/2+item.xDelta;
                int y = item.yDelta + linesWritten.get() * (g2d.getFontMetrics().getHeight() + LINE_SPACING);
                g2d.drawString(charsToWrite.get(), x, y);
            } else {
                g2d.drawString(item.text, (WIDTH - xd.get())/2+item.xDelta, item.yDelta);
            }
        }

        g2d.dispose();

        int count = produced.getOrDefault(ici.word,0) + 1;
        produced.put(ici.word,count);

        try {
            saveImage(img, ici.word + "_" + count + ".jpg", destinationFolder);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void verifyICI(ImageConstructInfo ici) throws NoDefintionException, NoPhoneticsException, NoExampleException {
        if (ici.phonetics == null) {
            throw new NoPhoneticsException(ici.word);
        }
        if (ici.definition == null) {
            throw new NoDefintionException(ici.word);
        }
        if (ici.definition.example == null) {
            throw new NoExampleException(ici.word);
        }
    }

    public void saveImage(RenderedImage image, String filename, File destinationFolder) throws IOException {
        File file = new File(destinationFolder+"/"+filename);
        ImageIO.write(image,"jpg",file);
    }

    public static Color calculateFontColor(Color bkgd) {
        int red = bkgd.getRed();
        int green = bkgd.getGreen();
        int blue = bkgd.getBlue();
        return (red+green+blue > 186) ? Color.black : Color.white;
    }
    public static Color calculateFontColorFade(Color bkgd) {
        int red = bkgd.getRed();
        int green = bkgd.getGreen();
        int blue = bkgd.getBlue();
        return (red+green+blue > 186) ? Color.darkGray : Color.lightGray;
    }

    public String getWord() throws NoConnectionException {
        URI uri = URI.create("https://random-word-api.herokuapp.com/word");
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(uri)
                .header("accept","application/json")
                .GET()
                .build();
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new NoConnectionException();
        }

        String word;
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            word = gson.fromJson(response.body(), String[].class)[0];
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Convert response to a string
        return word;
    }
    public DictionaryResponse getDefinition(String word) throws NoDefintionException {
        URI uri = URI.create("https://api.dictionaryapi.dev/api/v2/entries/en/"+word);
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(uri)
                .header("accept","application/json")
                .GET()
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            return gson.fromJson(response.body(), DictionaryResponse[].class)[0];
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        } catch (JsonSyntaxException ignored) {
            throw new NoDefintionException(word);
        }
    }

    public String getGeneratorName() {
        return "Image Generator";
    }

    static class ImageConstructInfo {
        String word;
        String partOfSpeech;
        String phonetics;
        Definition definition;
        Color bkgd;

        public ImageConstructInfo(String word, String partOfSpeech, String phonetics, Definition definition, Color bkgd) {
            this.word = word;
            this.partOfSpeech = partOfSpeech;
            this.phonetics = phonetics;
            this.definition = definition;
            this.bkgd = bkgd;
        }
    }
    static class ImageItem {
        String text;
        Font font;
        Color color;

        int xDelta, yDelta;

        public ImageItem(String text, Font font, Color color, int xDelta, int yDelta) {
            this.text = text;
            this.font = font;
            this.color = color;

            this.xDelta = xDelta;
            this.yDelta = yDelta;
        }
    }
}