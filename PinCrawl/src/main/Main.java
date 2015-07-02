package main;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.imageio.ImageIO;

/**
 * A simple app to download any Pinterest user's pins to a local directory.
 */
public class Main {

    private static final int TIMEOUT = 10000;
    private static String _username;
    private static String rootDir = "PinCrawl Results";

    /**
     * Verify arguments, and handle some errors
     *
     * @param args arguments (needs a string for username or abort)
     */
    public static void main(final String[] args)  {
        System.out.println("Welcome to PinCrawl, this may take a while...");

        // get username
        if (args.length > 0) {
            _username = args[0];
        } else {
            System.out.println("ERROR: please enter a user name, aborting.");
            return;
        }

        try {
            process();
        } catch (IOException e) {
            System.out.println("ERROR: IOException, probably a messed up URL.");
        }
    }

    /**
     * All main logic
     *
     * @throws  IOException if bad URL
     */
    private static void process() throws IOException {
        // validate username and connect to their page
        Document doc;
        try {
            doc = Jsoup.connect("https://www.pinterest.com/" + _username + "/").timeout(TIMEOUT).get();
        } catch (HttpStatusException e) {
            System.out.println("ERROR: not a valid user name, aborting.");
            return;
        }
        // list of board urls
        final Elements boardLinks = doc.select("a[href].boardLinkWrapper");

        // make root directory
        rootDir += " for " + _username;
        if(!makeDir(rootDir))
            return;
        System.out.println("Downloading all pins to '" + rootDir + "'...");

        for (final Element boardLink : boardLinks) {
            // connect to board via url and get all page urls
            final Document boardDoc = Jsoup.connect(boardLink.absUrl("href")).timeout(TIMEOUT).get();
            final Elements pageLinks = boardDoc.select("a[href].pinImageWrapper");

            // parse and format board name and make its directory
            String boardName = boardLink.attr("title");
            boardName = boardName.substring(10, boardName.length()); // remove "more from" part
            boardName = URLEncoder.encode(boardName, "UTF-8");
            boardName = boardName.replace('+',' ');
            if(!makeDir(rootDir + "\\" + boardName))
                return;

            System.out.println("...Downloading '" + boardName + "'...");
            int imgCount = 1;
            for (final Element pageLink : pageLinks) {
                // connect to image page and get direct link to image then save it
                final Document pageDoc = Jsoup.connect(pageLink.absUrl("href")).timeout(TIMEOUT).get();
                final Elements imgLinks = pageDoc.select("img[src].pinImage");
                for (final Element imgLink : imgLinks) {
                    saveImage(imgLink.absUrl("src"), rootDir + "\\" + boardName, imgCount);
                }
                imgCount++;
            }
        }

        System.out.println("All pins downloaded, to " + System.getProperty("user.dir")
                + "\\"  + rootDir + "\\");
        System.out.println("Thanks for using PinCrawl!");
    }

    /**
     * Makes a directory with the filename provided, fails if it already exists
     * TODO: allow arguments for overwrite, subtractive, and additive changes
     *
     * @param name name of the file
     */
    public static boolean makeDir(String name) {
        File file = new File(name);
        if (!file.exists()) {
            if (file.mkdir()) {
                return true;
            } else {
                System.out.println("ERROR: Failed to create directory '" + name + "', aborting.");
            }
        } else {
            System.out.println("ERROR: Directory '" + name + "' already exists, aborting.");
        }
        return false;
    }

    /**
     * Saves an image from the specified URL to the path with the name count
     * TODO: allow gifs, maybe
     *
     * @param srcUrl url of image
     * @param path path to save image (in root\board)
     * @param count count to name image since I don't want to use the long and tedious names on pinterest
     * @throws IOException
     */
    public static void saveImage(String srcUrl, String path, int count) throws IOException {
        BufferedImage image;
        URL url = new URL(srcUrl);
        if(srcUrl.endsWith(".gif"))
            System.out.println("ERROR: .gifs not supported, continuing");
        try {
            image = ImageIO.read(url);
            ImageIO.write(image, "png", new File(path + "\\" + count + ".png"));
        } catch (ArrayIndexOutOfBoundsException ex) {
            System.out.println("ERROR: Image too big, probably a .gif that didn't end with .gif, continuing");
        }
    }
}
