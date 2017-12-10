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
    
    public WebIndex() {
    		pages = new HashSet<Page>();
    		wordsToPage = new HashMap<String, Set<Page>>();
    		links = new HashMap<Page, ArrayList<String>>();
    }
    
    /**
     * Adds an association between a String keyword and a URL to this
     * WebIndex.
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
    			// if no previous is specified, insert gibberish
    			if (prev.equals(""))
    				links.get(url).add("*********");
    			else
    				assert links.get(url).get(links.get(url).size()-1).equals(prev) : "Indexing Error: Last saved word not equal to given!";
            links.get(url).add(keyword);
    		} else {
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
     * Returns the set of URLS associated with a given keyword.
     * Returns null if the keyword doesn't have any associated keywords.
     */
    public Set<Page> getUrls(String keyword) {
    		assert keyword != null : "WebIndex: getUrls can't take null input!";
    		keyword = keyword.toLowerCase();

    		if (keyword.contains("+")) {
    			if (keyword.startsWith("!")) {
    				keyword = keyword.substring(1);
    				return getUrls(keyword.split("\\+"), pages, true);
    			}
    			return getUrls(keyword.split("\\+"), pages, false);
    		}
    		boolean not = keyword.startsWith("!");
    		// remove the potential !
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
    				return wordsToPage.get(keyword);
    			}
    		}
    		else {
    			// if notted, return all pages
    			if (not) {
    				return pages;
    			}
    			return new HashSet<Page>();
    		}
    }
    
    /**
     * Returns the set of URLS associated with a given keyword in 
     * the list of URLS given.
     */
    public Set<Page> getUrls(String keyword, Set<Page> urls) {
    		assert keyword != null && urls != null : "WebIndex: getUrls can't take null inputs!";
    		keyword = keyword.toLowerCase();
    		boolean not = false;
    		
    		if (keyword.contains("+")) {
    			if (keyword.startsWith("!")) {
    				keyword = keyword.substring(1);
    				return getUrls(keyword.split("+"), urls, true);
    			}
    			return getUrls(keyword.split("+"), urls, false);
    		}
    		
    		if (keyword.startsWith("!")) {
    			not = true;
    			keyword = keyword.substring(1);
    			assert keyword.length() > 0 : "WebIndex: can't search for '!' term!";

    		}

    		// if we don't have this word, then every page passes the requirement
    		if (!wordsToPage.containsKey(keyword)) {
    			return urls;
    		}

    		Set<Page> newUrls = new HashSet<Page>();
    		
        /* if we're looking for pages WITH this word, then add
    		   if we're looking for pages WITHOUT this word and this page doesn't
    		   have it, then add */
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
     * Returns the set of Urls associated with a given phrase.
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
    						return pages;
    					return new HashSet<Page>();
    				}
    			}
    			// any other word
            if (wordsToPage.containsKey(word)) {
                newUrls.retainAll(wordsToPage.get(word));
            }
    			// phrase doesn't exists if word doesn't exist
    			else {
    				if (not)
    					return pages;
    				return new HashSet<Page>();
    			}
    		}
    		
    		// ensure every word is linked
    		if (newUrls.isEmpty()) {
    			return pages;
    		} else {
    			HashSet<Page> finalUrls = new HashSet<Page>();
    			for (Page url : newUrls) {
    				assert links.containsKey(url) : "WebIndex : Links not properly generated!";
    				ArrayList<String> words = links.get(url);
    				for (int i = 0; i < words.size(); i++) {
    					int cur = 0;
    					while (cur < keyword.length && words.get(i+cur).equals(keyword[cur])) {
    						cur++;
    					}
    					if (cur == keyword.length) {
    						finalUrls.add(url);
    						break;
    					}
    				}
    			}
    			if (not) {
    				HashSet<Page> all = new HashSet<Page>(pages);
    				all.removeAll(finalUrls);
    				return all;
    			}
    			return finalUrls;
    		}
    }
    
    
    /**
     * Returns a collection of all the pages in this index.
     */
    public Set<Page> allPages() {
    		return pages;
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
}
