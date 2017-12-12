package test;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.regex.*;
import assignment.*;

/**
 * All tests are made to run on with an index.db
 * file of RHF. The file 'grepTest.txt' must be in 
 * the path!
 * @author Arvind Raghavan
 */
public class MyTests {
	
	/**
	 * Tests the getPage helper method in WebIndex that is used
	 * for other testing purposes.
	 */
	static WebQueryEngine queryTest; 
	
	@Rule
	public ExpectedException exception = ExpectedException.none();
	
	@BeforeClass
	public static void loadWebIndex() throws Exception{
		queryTest = WebQueryEngine.fromIndex((WebIndex) Index.load("index.db"));
	}

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
        String test9 = "(hello')";
        
        assertEquals("hello", removePunct(test1));
        assertEquals("hello", removePunct(test2));
        assertEquals("hello", removePunct(test3));
        assertEquals("hello", removePunct(test4));
        assertEquals("hello", removePunct(test5));
        assertEquals("hello", removePunct(test6));
        assertEquals("hello hello", removePunct(test7));
        assertEquals("hel lo  ", removePunct(test8));
        assertEquals("hello'", removePunct(test9));

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
			if (Character.isLetter(c) || c == '\'')
				result += c;
			else if (Character.isWhitespace(c))
				result += " ";
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
		String query3 = "( hello ( goodbye | hello bye ) )";
		
		String queryResults = "hello & goodbye";
		String query2Results = "hello & goodbye & hello";
		String query3Results = "( hello & ( goodbye | hello & bye ) )";

		assertEquals(queryResults, String.join(" ", queryTest.addExplicitAND(query.split(" "))));
		assertEquals(query2Results, String.join(" ", queryTest.addExplicitAND(query2.split(" "))));
		assertEquals(query3Results, String.join(" ", queryTest.addExplicitAND(query3.split(" "))));
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
		
		exception.expect(IllegalArgumentException.class);
		queryTest.isValidQuery(query1);
		queryTest.isValidQuery(query2);

		exception = ExpectedException.none();
		queryTest.isValidQuery(query3);

		// test num of brackets
		query1 = "( nice & cool )";
		query2 = "( ( nice & cool )";
		query3 = "( hello & my | ( name | is ) | bar & ( hi )";
		
		queryTest.isValidQuery(query1);
		exception.expect(IllegalArgumentException.class);
		queryTest.isValidQuery(query2);
		queryTest.isValidQuery(query3);
		
		// test phrases (+ used to indicate phrase)
		query1 = "hello+hi+my";
		query2 = "hello+contains+my";
		query3 = "( my+name & hello | ( oh my ))";
		
		exception = ExpectedException.none();
		queryTest.isValidQuery(query1);
		queryTest.isValidQuery(query2);
		queryTest.isValidQuery(queryTest.fixSpacing(query3));
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
		String query3 = "\"bob dylan\" ( big boy | toy ) & \"named troy\"";
		String query4 = "test \"quan what does\"";
		
		String query1Result = "hello+my+name+is my & !no yes & |";
		String query2Result = "hello yes+sir & !no |";
		String query3Result = "bob+dylan big boy & toy | & named+troy &";
		String query4Result = "test quan+what+does &";
		
		assertEquals(query1Result, String.join(" ", queryTest.getPostFix(query1)));
		assertEquals(query2Result, String.join(" ", queryTest.getPostFix(query2)));
		assertEquals(query3Result, String.join(" ", queryTest.getPostFix(query3)));
		assertEquals(query4Result, String.join(" ", queryTest.getPostFix(query4)));
	}
	
	/**
	 * Verifies the results of a grep search match the results
	 * given by the crawler for the word 'insane'.
	 */
	@Test
	public void singleWordGrepTest1() throws Exception {
		// word is 'insane'
		HashSet<String> grepFound = loadGrepResults("insane");
		Collection<Page> index = queryTest.query("insane");
		HashSet<String> indexFound = new HashSet<String>();

		for (Page p : index)
			indexFound.add(p.getURL().getPath().toLowerCase());
		
		// tests set equality with cardinality and one subset
		assertEquals(indexFound.size(), grepFound.size());
		
		HashSet<String> difference = new HashSet<String>(grepFound);
		difference.removeAll(indexFound);
		assertEquals(0, difference.size());
	}
	
	/**
	 * Verifies the results of a grep search match the results
	 * given by the crawler for the word 'crazy'.
	 */
	@Test
	public void singleWordGrepTest2() throws Exception {
		// word is 'crazy'
		HashSet<String> grepFound = loadGrepResults("crazy");
		Collection<Page> index = queryTest.query("crazy");
		HashSet<String> indexFound = new HashSet<String>();

		for (Page p : index)
			indexFound.add(p.getURL().getPath().toLowerCase());
		
		// tests set equality 
		assertEquals(indexFound, grepFound);
	}
	
	/**
	 * Verifies the results of a grep search match the results
	 * given by the crawler for the word 'mistress'.
	 */
	@Test
	public void singleWordGrepTest3() throws Exception {
		// word is 'mistress'
		HashSet<String> grepFound = loadGrepResults("mistress");
		Collection<Page> index = queryTest.query("mistress");
		HashSet<String> indexFound = new HashSet<String>();

		for (Page p : index)
			indexFound.add(p.getURL().getPath().toLowerCase());
		
		// tests set equality 
		assertEquals(indexFound, grepFound);
	}
	
	/**
	 * Verifies the results of a grep search match the results
	 * given by the crawler for the word 'undocumented'.
	 */
	@Test
	public void singleWordGrepTest4() throws Exception {
		// word is 'undocumented'
		HashSet<String> grepFound = loadGrepResults("undocumented");
		Collection<Page> index = queryTest.query("undocumented");
		HashSet<String> indexFound = new HashSet<String>();

		for (Page p : index)
			indexFound.add(p.getURL().getPath().toLowerCase());
		
		// tests set equality 
		assertEquals(indexFound, grepFound);
	}
	
	
	/**
	 * Verifies the results of a grep search match the results
	 * given by the crawler for the query '!end'
	 */
	@Test
	public void notWordGrepTest() throws Exception{
		// word is 'unconstitutional'
		HashSet<String> grepFound = loadGrepResults("unconstitutional");
		Collection<Page> index = queryTest.query("!unconstitutional");
		HashSet<String> indexFound = new HashSet<String>();

		for (Page p : index)
			indexFound.add(p.getURL().getPath().toLowerCase());
		
		// 9359 valid files in RHF
		assertEquals(indexFound.size(), 9359 - grepFound.size());
		
		// check that the sets are disjoint
		for (String s : indexFound)
			assertFalse(grepFound.contains(s));
		for (String s : grepFound)
			assertFalse(indexFound.contains(s));
	}
	
	/**
	 * Verifies the results of a grep search for phrase queries.
	 */
	@Test
	public void phraseGrepTest1() throws Exception {
		// phrase is "I am forced"
		HashSet<String> grepFound = loadGrepResults("i+am+forced");
		Collection<Page> index = queryTest.query("\"I am Forced\"");
		HashSet<String> indexFound = new HashSet<String>();

		for (Page p : index)
			indexFound.add(p.getURL().getPath().toLowerCase());
		
		// tests set equality 
		assertEquals(indexFound, grepFound);
	}

	/**
	 * Verifies the results of a grep search for not-phrase queries.
	 */
	@Test
	public void notPhraseGrepTest() throws Exception {
		// phrase is "I am forced"
		HashSet<String> grepFound = loadGrepResults("!i+am+forced");
		Collection<Page> index = queryTest.query("! \"I am Forced\"");
		HashSet<String> indexFound = new HashSet<String>();

		for (Page p : index)
			indexFound.add(p.getURL().getPath().toLowerCase());
		assertEquals(grepFound, indexFound);
	}

	/**
	 * Verifies the results of an & query with grep.
	 */
	@Test
	public void andGrepTest() throws Exception {
		// & for 'undocumented' and 'mistress'
		HashSet<String> grepFound = loadGrepResults("undocumented");
		HashSet<String> grep1 = loadGrepResults("mistress");
		grepFound.retainAll(grep1);
		
		Collection<Page> index = queryTest.query("mistress & undocumented");
		HashSet<String> indexFound = new HashSet<String>();

		for (Page p : index)
			indexFound.add(p.getURL().getPath().toLowerCase());
		
		assertEquals(indexFound, grepFound);
	}
	
	/**
	 * Verifies the results of an | query with grep.
	 */
	@Test
	public void orGrepTest() throws Exception {
		// | for 'insane' and 'mistress'
		HashSet<String> grep1 = loadGrepResults("mistress");
		HashSet<String> grepFound = loadGrepResults("insane");
		grepFound.addAll(grep1);

		Collection<Page> index = queryTest.query("mistress | insane");
		HashSet<String> indexFound = new HashSet<String>();

		for (Page p : index)
			indexFound.add(p.getURL().getPath().toLowerCase());

		assertEquals(indexFound, grepFound);

	}
	
	/**
	 * Integration Test #1
	 * Query: 'mistress & undocumented | (!\"I am forced"\")'
	 */
	@Test
	public void integrationTest1() throws Exception {
		HashSet<String> mistress = loadGrepResults("mistress");
		HashSet<String> undocumented = loadGrepResults("undocumented");
		HashSet<String> forced = loadGrepResults("!i+am+forced");
		
		HashSet<String> grepFound = new HashSet<String>(mistress);
		grepFound.retainAll(undocumented);
		grepFound.addAll(forced);
		
		Collection<Page> index = queryTest.query("mistress & undocumented | (!\"I am forced\")");
		HashSet<String> indexFound = new HashSet<String>();

		for (Page p : index)
			indexFound.add(p.getURL().getPath().toLowerCase());
		
		assertEquals(indexFound, grepFound);
	}
	
	/**
	 * Integration Test #2
	 * Query: 'insane | crazy & !\"I am forced\" | mistress & undocumented'
	 */
	@Test
	public void integrationTest2() throws Exception {
		HashSet<String> mistress = loadGrepResults("mistress");
		HashSet<String> undocumented = loadGrepResults("undocumented");
		HashSet<String> insane = loadGrepResults("insane");
		HashSet<String> crazy = loadGrepResults("crazy");
		HashSet<String> forced = loadGrepResults("!i+am+forced");
		
		HashSet<String> grepFound = new HashSet<String>(mistress);
		grepFound.retainAll(undocumented);
		HashSet<String> temp = new HashSet<String>(crazy);
		temp.retainAll(forced);
		grepFound.addAll(temp);
		grepFound.addAll(insane);
		
		Collection<Page> index = queryTest.query("insane | crazy & !\"I am forced\" | mistress & undocumented");
		HashSet<String> indexFound = new HashSet<String>();

		for (Page p : index)
			indexFound.add(p.getURL().getPath().toLowerCase());
		
		assertEquals(indexFound, grepFound);
	}
	
	/**
	 * Makes sure that all of these bad queries throw an illegalArgumentException
	 * and not anything else.
	 */
	@Test
	public void badQueryTest() {
		exception.expect(IllegalArgumentException.class);
		
		// numbers not allowed
		queryTest.query("lfhgkljaflkgjlkjlkj9f8difj3j98ouijsflkedfj90");
		// unbalanced brackets
		queryTest.query("( hello & goodbye | goodbye ( or hello )");
		// & ) not legal
		queryTest.query("A hello & goodbye | goodbye ( or hello &)");
		// unbalanced quote
		queryTest.query("kdf ksdfj (\") kjdf");
		// empty quotes
		queryTest.query("kdf ksdfj (\"\") kjdf");
		// invalid text in quotes
		queryTest.query("kdf ksdfj \"()\" kjdf");
		// double negation invalid (decision)
		queryTest.query("!!and");
		
		// gibberish
		queryTest.query("kjlkfgj! ! ! !!! ! !");
		queryTest.query("klkjgi & df & | herllo");
		queryTest.query("kjdfkj &");
		
		// single negation
		queryTest.query("( ! )");
		queryTest.query("!");
		
		// quotes and parenthesis interspersed
		queryTest.query("our lives | coulda ( \" been so ) \" but momma had to \" it all up wow\"");
	}
	
	/**
	 * Asserts different language queries are not invalid.
	 */
	@Test
	public void languageTest() {
		assertNotNull(queryTest.query("漢語"));
		assertNotNull(queryTest.query("ンサートは"));
		assertNotNull(queryTest.query("Ζ Θ Ι ΚΛ"));
		assertNotNull(queryTest.query("Ζ Θ Ι ΚΛ"));
		assertNotNull(queryTest.query("nalità umana ed al rafforzamento del rispetto dei diritti umani e delle libertà f"));
	}

	/**
	 * Loads a HashSet of Pages from Grep results stored in the
	 * file grepTest.txt. The word indicates which files to load.
	 */
	public HashSet<String> loadGrepResults(String word) throws Exception {
		BufferedReader br = new BufferedReader(new FileReader("grepTest.txt"));
		String line;
		boolean foundSection = false;
		HashSet<String> found = new HashSet<String>();

		while ((line = br.readLine()) != null) {
			if (foundSection & line.contains("Word: "))
				break;
			if (line.contains("Word:") && line.contains(word)) {
				foundSection = true;
				continue;
			}
			if (!foundSection)
				continue;
			found.add(line.toLowerCase());
		}
		// fails if section wasn't found
		assertTrue(foundSection);
		return found;
	}
	
}
