package test;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.regex.*;
import assignment.*;

public class MyTests {
	
	/**
	 * Tests the getPage helper method in WebIndex that is used
	 * for other testing purposes.
	 * @return 
	 */
	WebQueryEngine queryTest = WebQueryEngine.fromIndex(new WebIndex());
	
	@Test
	public void getPageTest() {
		String perfect = "https://www.google.com";
		String perfect2 = "http://www.google.com";
		String www = "www.google.com";
		String google = "google.com";
		String malformed = "googlecom";
		
		assertNotNull(WebIndex.getPage(perfect));
		assertNotNull(WebIndex.getPage(perfect2));
		assertNull(WebIndex.getPage(www));
		assertNull(WebIndex.getPage(google));
		assertNull(WebIndex.getPage(malformed));
	}
	
	/**
	 * Tests that adding and retrieving urls associated with keywords
	 * works as intended and that case for keywords and URLs are
	 * ignored.
	 */
	@Test
	public void WebIndexAddTest() {
		Page perfect = WebIndex.getPage("https://www.google.com");
		Page perfect2 = WebIndex.getPage("http://www.google.com");

		WebIndex index = new WebIndex();
		index.add("google", "", perfect);
		
		// check for different case
		for (Page p : index.getUrls("Google"))
			assertEquals(p, perfect);
		

		index.add("Google", "", perfect2);
		boolean onFirst = true;
		for (Page p : index.getUrls("Google"))
			if (onFirst) {
				assertTrue(p.equals(perfect));
				onFirst = false;
			}
			else
				assertTrue(p.equals(perfect2));
		
	}
	
	/**
	 * This tests the regex string used in Crawling Markup
	 * Handler to remove punctuation.
	 */
	@Test
	public void removePunctTest() {
		String test1 = "hello.";
		String test2 = "hello,";
        String test3 = "(hello!)";
        String test4 = "hello...";
        String test5 = "!?,hel;lo";
        String test6 = "hel-lo";
        String test7 = "hello; !hel(lo";
        String test8 = "hel\rlo\n\n";
        
        assertEquals("hello.", removePunct(test1));
        assertEquals("hello", removePunct(test2));
        assertEquals("hello", removePunct(test3));
        assertEquals("hello...", removePunct(test4));
        assertEquals("hello", removePunct(test5));
        assertEquals("hel-lo", removePunct(test6));
        assertEquals("hello hello", removePunct(test7));
        assertEquals("hello", removePunct(test8));

	}
	
	/**
	 * A miniature version of the punctuation
	 * removal used in CrawlingMarkupHandler. This 
	 * is used to test the regex.
	 */
	public String removePunct(String input) {
		char[] inputs = input.toCharArray();
		String result = "";
		for (Character c : inputs) {
			String letter = c+"";
			if (letter.matches(CrawlingMarkupHandler.onlyLetters))
				result += letter;
		}
		return result;
	}
	
	/**
	 * This tests the method which adds explicit ands where two consecutive
	 * words implies it.
	 */
	@Test
	public void explicitANDTest() {
		String query = "hello goodbye";
		String query2 = "hello goodbye hello";
		String query3 = "( hello | ( goodbye | hello bye ) )";
		
		String[] queryResults = queryTest.addExplicitAND(query.split(" "));
		String[] query2Results = queryTest.addExplicitAND(query2.split(" "));
		String[] query3Results = queryTest.addExplicitAND(query3.split(" "));
		
		assertTrue(queryResults[1].equals("&"));
		assertTrue(query2Results[1].equals("&"));
		assertTrue(query2Results[3].equals("&"));
		assertTrue(query3Results[7].equals("&"));
	}
	
	/**
	 * Tests that conversion to postfix notation is done correctly.
	 */
	@Test
	public void postFixTest() {
		String query = "( ( !hello | goodbye & hello ) & cya )";
		String query2 = "( hello & goodbye | foo & ( foo & bar ) )";
		
		// this test verifies that implict AND's are added
		String query3 = "( testing the waters | ( am & I ) | oh my )";
		String query4 = "a b c d e";
		
		String queryAns = "!hello goodbye hello & | cya &";
		String query2Ans = "hello goodbye & foo foo bar & & |";
		String query3Ans = "testing the & waters & am I & | oh my & |";
		String query4Ans = "a b & c & d & e &";
		
		assertEquals(queryAns, String.join(" ", queryTest.convertToPostfix(query.split(" "))));
		assertEquals(query2Ans, String.join(" ", queryTest.convertToPostfix(query2.split(" "))));
		assertEquals(query3Ans, String.join(" ", queryTest.convertToPostfix(queryTest.addExplicitAND(query3.split(" ")))));
		assertEquals(query4Ans, String.join(" ", queryTest.convertToPostfix(queryTest.addExplicitAND(query4.split(" ")))));
	}
	
	/** 
	 * Tests the functionality of the isValidQuery method!
	 */
	@Test
	public void isValidQueryTest() {
		String query1 = "< nice & cool ";
		String query2 = "( hello & !!cool )";
		String query3 = "( hello | !cool )";
		
		assertFalse(queryTest.isValidQuery(query1));
		assertFalse(queryTest.isValidQuery(query2));
		assertTrue(queryTest.isValidQuery(query3));

		// test num of brackets
		query1 = "( nice & cool )";
		query2 = "( ( nice & cool )";
		query3 = "( hello & my | ( name | is ) | bar & ( hi )";
		
		assertTrue(queryTest.isValidQuery(query1));
		assertFalse(queryTest.isValidQuery(query2));
		assertFalse(queryTest.isValidQuery(query3));
		
		// test phrases (+ used to indicate phrase)
		query1 = "hello+hi+my";
		query2 = "hello+contains+my";
		query3 = "( my+name & hello | ( oh my ))";
		
		assertTrue(queryTest.isValidQuery(query1));
		assertTrue(queryTest.isValidQuery(query2));
		assertTrue(queryTest.isValidQuery(queryTest.fixSpacing(query3)));
	}
	
	/**
	 * Tests the regulate whitespace function in the query parser.
	 */
	@Test
	public void fixWhiteSpaceTest() {
		String query1 = "((hello))";
		String query2 = "\"hello my\"";
		String query3 = "( hello & goodbye) ";
		
		String query1Result = "( ( hello ) )";
		String query2Result = "\" hello my \"";
		String query3Result = "( hello & goodbye )";
		
		
		assertEquals(query1Result, queryTest.fixSpacing(query1));
		assertEquals(query2Result, queryTest.fixSpacing(query2));
		assertEquals(query3Result, queryTest.fixSpacing(query3));
	}
	
	/**
	 * Tests the phrase concatenation method which takes a given phrase
	 * " hello my " and converts it to hello+my.
	 */
	@Test
	public void phraseConcatTest() {
		String query1 = "\" hello my \"";
		String query2 = "\" I am the king \"";
		String query3 = "\" yes you are \" ";
		
		String query1Result = "hello+my";
		String query2Result = "I+am+the+king";
		String query3Result = "yes+you+are";
		
		assertEquals(query1Result, queryTest.fixPhrases(query1));
		assertEquals(query2Result, queryTest.fixPhrases(query2));
		assertEquals(query3Result, queryTest.fixPhrases(query3));
	}
	
	/**
	 * Tests all query handling and conversion to postfix to process!
	 */
	@Test
	public void queryTest() {
		String query1 = "(\"hello my name is\" & my | (!no & yes))";
		String query2 = "(hello & (\"yes sir\") | !no)";
		
		String query1Result = "hello+my+name+is my & !no yes & |";
		String query2Result = "hello yes+sir & !no |";
		
		assertEquals(query1Result, String.join(" ", queryTest.getPostFix(query1)));
		assertEquals(query2Result, String.join(" ", queryTest.getPostFix(query2)));
	}
	 
}
