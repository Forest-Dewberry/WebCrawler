package assignment;

import java.io.*;
import java.net.*;
import java.util.*;

import org.attoparser.simple.*;
import org.attoparser.ParseException;
import org.attoparser.config.ParseConfiguration;

/**
 * The entry-point for WebCrawler; takes in a list of URLs to start crawling from and saves an index
 * to index.db.
 */
public class WebCrawler {

    /**
    * The WebCrawler's main method starts crawling a set of pages.  You can change this method as
    * you see fit, as long as it takes URLs as inputs and saves an Index at "index.db".
    */
    public static void main(String[] args) {
        // Basic usage information
        if (args.length == 0) {
            System.err.println("Error: No URLs specified.");
            System.exit(1);
        }

        // We'll throw all of the args into a queue for processing.
        Queue<URL> remaining = new LinkedList<>();
        for (String url : args) {
            try {
                remaining.add(new URL(new URL("file:"),url));
            } catch (MalformedURLException e) {
                // Throw this one out!
                System.err.printf("Error: URL '%s' was malformed and will be ignored!%n", url);
            }
        }
        
        HashSet<String> visited = new HashSet<String>();

        // Create a parser from the attoparser library, and our handler for markup.
        ISimpleMarkupParser parser = new SimpleMarkupParser(ParseConfiguration.htmlConfiguration());
        CrawlingMarkupHandler handler = new CrawlingMarkupHandler();

        // Try to start crawling, adding new URLS as we see them.
        try {
            while (!remaining.isEmpty()) {
            		URL current = remaining.poll();
            		// make sure we haven't visited before
            		if (visited.contains(current.getPath().toLowerCase()))
            			continue;

            		handler.updateCurrentURL(current);

                // Parse the next URL's page
            		try {
            			parser.parse(new InputStreamReader(current.openStream()), handler);
            			visited.add(current.getPath().toLowerCase());
            		} catch (FileNotFoundException e) {
            			// don't want to add urls with missing files
            			continue;
            		} catch (ParseException e) {
            			// thrown with images, don't want to add those
            			continue;
            		} catch (UnknownServiceException e) {
            			// occasionally thrown with .jpg files
            			continue;
            		}

                // Add any new URLs
            		remaining.addAll(handler.newURLs());
            }
            System.out.println("Finished crawling websites, saving index...");
            handler.getIndex().save("index.db");
        } catch (IOException e) {
        		// we should never get here, all URL and parse exceptions should be caught
        		System.err.println("Invalid input file!");
        		System.exit(1);
        }
        System.out.println("Saved!");
        
    }
}
