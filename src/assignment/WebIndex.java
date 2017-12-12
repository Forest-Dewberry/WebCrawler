package assignment;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

/**
 * A web-index which efficiently stores information about pages. Serialization is done automatically
 * via the superclass "Index" and Java's Serializable interface.
 */
public class WebIndex extends Index {
    /**
     * Needed for Serialization (provided by Index) - don't remove this!
     */
    private static final long serialVersionUID = 1L;

    // public for testing purpose
    public Set<Page> pages;
    public HashMap<String, Set<Page>> wordsToPage;
    public HashMap<Page, ArrayList<String>> links;
    
    /**
     * Initializes all three datatypes for linking
     * word -> webpage.
     */
    public WebIndex() {
    		pages = new HashSet<Page>();
    		wordsToPage = new HashMap<String, Set<Page>>();
    		links = new HashMap<Page, ArrayList<String>>();
    }
    
    /**
     * Adds an association between a String keyword and a URL to this
     * WebIndex. The previous word is used to link the words together
     * and identify phrases. If prev is the empty String, it is 
     * assumed that this word is the beginning of a line.
     * 
     * @param keyword		the word to associate url with
     * @param prev			the word that came before it
     * @param url			the url to associate word with
     */
    public void add(String keyword, String prev, Page url) {
    		assert keyword != null && url != null : "WebIndex: Add can't take null inputs!";
    		// ignore case
    		keyword = keyword.toLowerCase();
    		prev = prev.toLowerCase();
    		
    		// check if page exists
    		if (pages.contains(url)) {
    			/* if no previous page is specified, insert gibberish to 
    			 * separate this word from the last word on the previous block.
    			 */
    			if (prev.equals(""))
    				links.get(url).add("*********");
    			else
    				assert links.get(url).get(links.get(url).size()-1).equals(prev) : "Indexing Error: Last saved word not equal to given!";
            links.get(url).add(keyword);
    		} else {
    			// if it doesn't exist, initialize a new linked list
    			pages.add(url);
    			links.put(url, new ArrayList<String>(Arrays.asList(keyword)));
    		}
    		
    		
    		// check if string exists
    		if (wordsToPage.containsKey(keyword)) {
    			wordsToPage.get(keyword).add(url);
    		} else {
    			wordsToPage.put(keyword, new HashSet<Page>(Arrays.asList(url)));
    		}
    }
    
    /**
     * Returns the set of URLS associated with a given keyword. This method
     * takes advantage of Hashing to return the URLS associated with a 
     * single word in O(n) time.
     */
    public Set<Page> getUrls(String keyword) {
    		assert keyword != null : "WebIndex: getUrls can't take null input!";
    		
    		// ignore case
    		keyword = keyword.toLowerCase();

    		// phrases are stored as lorem+ipsum, so redirect those to
    		// another method
    		if (keyword.contains("+")) {
    			// remove the not and pass it in as a boolean flag
    			if (keyword.startsWith("!")) {
    				keyword = keyword.substring(1);
    				return getUrls(keyword.split("\\+"), pages, true);
    			}
    			return getUrls(keyword.split("\\+"), pages, false);
    		}

    		boolean not = keyword.startsWith("!");
    		// remove the ! and set it as a flag 
    		if (not)
    			keyword = keyword.substring(1);

    		if (wordsToPage.containsKey(keyword)) {
    			// if notted, return the set of all pages w/o term
    			if (not) {
    				Set<Page> allPages = new HashSet<Page>(pages);
    				allPages.removeAll(wordsToPage.get(keyword));
    				return allPages;
    			} else {
    				// otherwise just return the term
    				return new HashSet<Page>(wordsToPage.get(keyword));
    			}
    		}
    		else {
    			// if notted, return all pages
    			if (not) {
    				return new HashSet<Page>(pages);
    			} // else just return an empty set
    			return new HashSet<Page>();
    		}
    }
    
    /**
     * Returns the set of URLS associated with a given keyword in 
     * the list of URLS given. 
     */
    public Set<Page> getUrls(String keyword, Set<Page> urls) {
    		assert keyword != null && urls != null : "WebIndex: getUrls can't take null inputs!";
    		// ignore case
    		keyword = keyword.toLowerCase();
    		boolean not = false;
    		
    		// move phrases to another method
    		if (keyword.contains("+")) {
    			// remove and pass the ! in as a flag
    			if (keyword.startsWith("!")) {
    				keyword = keyword.substring(1);
    				return getUrls(keyword.split("\\+"), urls, true);
    			}
    			return getUrls(keyword.split("\\+"), urls, false);
    		}
    		
    		// remove and set the ! as a flag
    		if (keyword.startsWith("!")) {
    			not = true;
    			keyword = keyword.substring(1);
    			assert keyword.length() > 0 : "WebIndex: can't search for '!' term!";

    		}

    		if (!wordsToPage.containsKey(keyword)) {
    			// if we're looking for pages without the word, and no page has
    			// the word, then every page passes
    			if (not) {
    				return new HashSet<Page>(urls);
    			} else {
    				// no page passes
    				return new HashSet<Page>();
    			}
    		}

    		Set<Page> newUrls = new HashSet<Page>();
    		if (not) {
    			// look through all pages without word
    			HashSet<Page> pagesWithout = new HashSet<Page>(pages);
    			pagesWithout.removeAll(wordsToPage.get(keyword));
    			
    			// set intersection of pages without and within the considered urls
    			pagesWithout.retainAll(urls);
    			return pagesWithout;
    		} else {
    			// if we're looking for pages with the word
    			for (Page p : wordsToPage.get(keyword)) {
    				if (urls.contains(p))
    					newUrls.add(p);
    			}
    		}

    		return newUrls;
    }
    
    /**
     * Returns the set of Urls associated with a given phrase. This method
     * first finds all the pages with every word in them, then searches those pages linearly
     * to discover if the word exists.
     * @param String[] keyword		an array of the words in the phrase
     * @param urls					the domain of urls
     * @param not					whether or not the phrase is notted
     */
    public Set<Page> getUrls(String[] keyword, Collection<Page> urls, boolean not) {
    		Set<Page> newUrls = null;
    		
    		// first find pages with all words in them
    		for (String word : keyword) {
    			// first word
    			if (newUrls == null) {
    				if (wordsToPage.containsKey(word)) {
    					newUrls = new HashSet<Page>();
    					newUrls.addAll(wordsToPage.get(word));
    				}
    				// phrase doesn't exists if first word doesn't exist
    				else {
    					if (not)
    						return new HashSet<Page>(urls);
    					return new HashSet<Page>();
    				}
    			}
    			// any other word
    			else if (wordsToPage.containsKey(word)) {
                newUrls.retainAll(wordsToPage.get(word));
            }
    			// phrase doesn't exists if word doesn't exist
    			else {
    				if (not)
    					return new HashSet<Page>(urls);
    				return new HashSet<Page>();
    			}
    		}
    		
    		// if there are no pages, with all the words, then return prematurely
    		if (newUrls.isEmpty()) {
    			if (not)
    				return new HashSet<Page>(urls);
    			return new HashSet<Page>();
    		// else, search linearly for the words
    		} else {
    			HashSet<Page> finalUrls = new HashSet<Page>();
    			for (Page url : newUrls) {
    				assert links.containsKey(url) : "WebIndex : Links not properly generated!";
    				ArrayList<String> words = links.get(url);
    				for (int i = 0; i < words.size(); i++) {
    					int cur = 0;
    					// increment through the words starting at i, checking for the phrase
    					while (cur < keyword.length && words.get(i+cur).equals(keyword[cur])) {
    						cur++;
    					}
    					// if we reached the end of the phrase, that means it exists
    					if (cur == keyword.length) {
    						finalUrls.add(url);
    						break;
    					}
    				}
    			}
    			// if not, return all pages without
    			if (not) {
    				HashSet<Page> all = new HashSet<Page>(pages);
    				all.removeAll(finalUrls);
    				all.retainAll(urls);
    				return all;
    			}
    			finalUrls.retainAll(urls);
    			return finalUrls;
    		}
    }
    
    
    /**
     * Returns a collection of all the pages in this index.
     */
    public Set<Page> allPages() {
    		return new HashSet<Page>(pages);
    }
    
    /**
     * Converts a String URL into a Page object and returns 
     * it. Returns null if the URL is malformed.
     */
    public static Page getPage(String url) {
    		assert url != null : "WebIndex: getPage can't take null input!";

    		Page page;
    		try {
    			page = new Page(new URL(url));
    		} catch (MalformedURLException e) {
    			return null;
    		}
    		return page;
    }
    
    /**
     * Determines whether a String is a word given. This method
     * is used extensively in other classes to allow
     * for Chinese characters ect.
     */
    public static boolean isWord(String word) {
    		word = word.toLowerCase();
    		// okay for word to begin with negation
    		if (word.startsWith("!"))
    			word = word.substring(1);
    		for (String s : word.split("\\+")) {
    			// means '+' occurs at beginning or end
    			if (s.isEmpty())
    				return false;
    			for (Character c : s.toCharArray()) {
    				// characters of all languages plus apostrophe are allowed
    				if (!Character.isLetter(c) && c != '\'')
    					return false;
    			}
    		}
    		return true;
    }
}
