package assignment;

import java.util.*;
import java.net.*;
import org.attoparser.simple.*;

/**
 * A markup handler which is called by the Attoparser markup parser as it parses the input;
 * responsible for building the actual web index.
 */
public class CrawlingMarkupHandler extends AbstractSimpleMarkupHandler {

	WebIndex index;
	Set<String> urls;
	URL currentURL;
	Page currentPage;
	String currentTag;

	/**
	 * Initializes the WebIndex and set of found URLs.
	 */
    public CrawlingMarkupHandler() {
    		index = new WebIndex();
    		urls = new HashSet<String>();
    }
    
    /**
     * Sets the current URL of this crawler.
     */
    public void updateCurrentURL(URL currentURL) {
    		this.currentURL = currentURL;
    		this.currentPage = new Page(currentURL);
    }

    /**
    * This method returns the complete index that has been crawled thus far when called.
    */
    public Index getIndex() {
        return index;
    }

    /**
    * This method returns any new URLs found to the Crawler; upon being called, the set of new URLs
    * should be cleared.
    */
    public List<URL> newURLs() {
    		assert currentURL != null : "Current URL must be set via updateCurrentURL!";
    		// convert unique String urls to URL objects
    		LinkedList<URL> newUrls = new LinkedList<URL>();
    		for (String url : urls) {
    			try {
    				URL urlObject = new URL(currentURL, url);
    				newUrls.add(urlObject);
    			} catch (MalformedURLException e) {
    				// Invalid URLs should have already been caught, so this should
    				// never occudr
    				System.err.println("FATAL: invalid URL identified during crawl!");
    				System.exit(-1);
    			}
    		}
    		// reset set of found URLs
    		urls = new HashSet<String>();
    		return newUrls;
    }

    /**
    * These are some of the methods from AbstractSimpleMarkupHandler.
    * All of its method implementations are NoOps, so we've added some things
    * to do; please remove all the extra printing before you turn in your code.
    *
    * Note: each of these methods defines a line and col param, but you probably
    * don't need those values. You can look at the documentation for the
    * superclass to see all of the handler methods.
    */

    /**
    * Called when the parser first starts reading a document.
    * @param startTimeNanos  the current time (in nanoseconds) when parsing starts
    * @param line            the line of the document where parsing starts
    * @param col             the column of the document where parsing starts
    */
    public void handleDocumentStart(long startTimeNanos, int line, int col) {
    		assert currentURL != null : "Current URL must be set via updateCurrentURL!";
    }

    /**
    * Called when the parser finishes reading a document.
    * @param endTimeNanos    the current time (in nanoseconds) when parsing ends
    * @param totalTimeNanos  the difference between current times at the start
    *                        and end of parsing
    * @param line            the line of the document where parsing ends
    * @param col             the column of the document where the parsing ends
    */
    public void handleDocumentEnd(long endTimeNanos, long totalTimeNanos, int line, int col) {
    		// do nothing
    }

    /**
    * Called at the start of any tag. This method is used primarily to
    * identify and store and URLs stored in href tags.
    * @param elementName the element name (such as "div")
    * @param attributes  the element attributes map, or null if it has no attributes
    * @param line        the line in the document where this elements appears
    * @param col         the column in the document where this element appears
    */
    public void handleOpenElement(String elementName, Map<String, String> attributes, int line, int col) {
    		assert currentURL != null : "Current URL must be set via updateCurrentURL!";
    		currentTag = elementName.toLowerCase();

        if (attributes == null)
        		return;

        // urls stored as attributes to key href
        for (String key : attributes.keySet()) {
            String potentialURL = attributes.get(key).toLowerCase();

        		// only use urls ending in .html
            if (!potentialURL.endsWith("html") && !potentialURL.endsWith("htm"))
            		continue;

            try {
                URL url = new URL(currentURL, potentialURL);
                /*
                * If we can get to this code that means the URL is valid,
                * now I can add to Set of Strings, not URLs, to ensure 
                * there aren't repeats.
                */
                urls.add(potentialURL.toLowerCase());
            } catch (MalformedURLException e) {} // do nothing
        }
    }

    /**
    * Called at the end of any tag.
    * @param elementName the element name (such as "div").
    * @param line        the line in the document where this elements appears.
    * @param col         the column in the document where this element appears.
    */
    public void handleCloseElement(String elementName, int line, int col) {
    		// do nothing
    }

    /**
    * Called whenever characters are found inside a tag. Note that the parser is not
    * required to return all characters in the tag in a single chunk. Whitespace is
    * also returned as characters.
    * 
    * This method is used to identify words on the page and store them
    * in this object's WebIndex.
    * 
    * @param ch      buffer containint characters; do not modify this buffer
    * @param start   location of 1st character in ch
    * @param length  number of characters in ch
    */
    public void handleText(char ch[], int start, int length, int line, int col) {
    		String words = "";
    		
    		// the style/script tags always have irrelevant text, don't 
    		// want to index it
    		if (currentTag.equals("style") || currentTag.equals("script"))
    			return;
    		
    		
        for(int i = start; i < start + length; i++) {
        		// only add letters and apostrophes to String, separate
        		if (Character.isLetter(ch[i]) || ch[i] == '\'')
        			words += ch[i];
        		else
        			words += " ";
        }

        // get rid of nbsp
        words = words.replaceAll("nbsp", "");

        // get rid of extra whitespace; lowercase string
        words = words.replaceAll("\\s+", " ").trim().toLowerCase();

        String prev = "";
        // sometimes empty string will end up as ' ', so ignore those strings as well
        if (!words.isEmpty() && !words.equals(" ")) {
        		for (String word : words.split(" ")) {
        			// Add to index!!
        			index.add(word, prev, currentPage);
        			prev = word;
        		}
        }
    }
}
