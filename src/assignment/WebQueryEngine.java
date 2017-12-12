package assignment;
import java.net.URL;
import java.util.*;

/**
 * A query engine which holds an underlying web index and can answer textual queries with a
 * collection of relevant pages.
 */
public class WebQueryEngine {
	
	WebIndex index;

	/**
	 * Constructs a WebQueryEngine object with the given 
	 * index.
	 */
	private WebQueryEngine(WebIndex index) {
		this.index = index;
	}

    /**
     * Returns a WebQueryEngine that uses the given Index to construct answers to queries.
     *
     * @param index The WebIndex this WebQueryEngine should use.
     * @return A WebQueryEngine ready to be queried.
     */
    public static WebQueryEngine fromIndex(WebIndex i) {
        return new WebQueryEngine(i);
    }

    /**
     * Returns a Collection of URLs (as Strings) of web pages satisfying the query expression.
     * This method throws an IllegalArgumentException if the query is invalid, though
     * it is not declared for compatability with automated testing.
     *
     * @param query A query expression.
     * @return A collection of web pages satisfying the query
     */

	public Collection<Page> query(String query) {
		if (query == null)
			throw new NullPointerException("query cannot take null input values!");

		// first convert the query to postfix to parse
    		Queue<String> postfix = getPostFix(query);

    		// parse query by adding terms to stack
    		Stack<Object> terms = new Stack<Object>();
    		
    		for (String token : postfix) {
    			if (WebIndex.isWord(token)) {
    				terms.push(token);
    			}
    			else {
    				if (token.equals("&")) {
    					// input was invalid if postfix failed to properly generate
    					if (terms.size() < 2)
    						throw new IllegalArgumentException("Operators must connect two distinct terms!");
    					
    					// pop the first two objects off the stack and & them
    					Object arg1 = terms.pop();
    					Object arg2 = terms.pop();
    					
    					// four cases: Str-str, str-list, list-list, list-str
    					// compute result, push back onto stack
    					if (arg1 instanceof String && arg2 instanceof String) {
    						Set<Page> result = new HashSet<Page>();
    						result = index.getUrls((String) arg1);
    						result = index.getUrls((String) arg2, result);
    						terms.push(result);
    					} else if (arg1 instanceof Set && arg2 instanceof String) {
    						arg1 = index.getUrls((String) arg2, (Set<Page>) arg1);
    						terms.push(arg1);
    					} else if (arg1 instanceof String && arg2 instanceof Set) {
    						arg2 = index.getUrls((String) arg1, (Set<Page>) arg2);
    						terms.push(arg2);
    					} else if (arg1 instanceof Set && arg2 instanceof Set) {
    						HashSet<Page> total = new HashSet<Page>();
    						total.addAll((Set<Page>) arg1);
    						total.retainAll((Set<Page>) arg2);
    						terms.push(total);
    					} else {
    						// error
    						System.err.println("WebQueryIndex: Fatal error at &!");
    						System.exit(1);
    					}
    					
    				} else if (token.equals("|")) {
    					// input was invalid if postfix failed to properly generate
    					if (terms.size() < 2)
    						throw new IllegalArgumentException("Operators must connect two distinct terms!");

    					// pop the first two objects off the stack and & them
    					Object arg1 = terms.pop();
    					Object arg2 = terms.pop();
    					
    					// four cases: Str-str, str-list, list-list, list-str
    					// compute result, push back onto stack
    					if (arg1 instanceof String && arg2 instanceof String) {
    						Set<Page> result = new HashSet<Page>();
    						result = index.getUrls((String) arg1);
    						result.addAll(index.getUrls((String) arg2));
    						terms.push(result);
    					} else if (arg1 instanceof Set && arg2 instanceof String) {
    						Set<Page> arg2Set = index.getUrls((String) arg2);
    						arg2Set.addAll((Set<Page>) arg1);
    						terms.push(arg2Set);
    					} else if (arg1 instanceof String && arg2 instanceof Set) {
    						Set<Page> arg1Set = index.getUrls((String) arg1);
    						arg1Set.addAll((Set<Page>) arg2);
    						terms.push(arg1Set);
    					} else if (arg1 instanceof Set && arg2 instanceof Set) {
    						HashSet<Page> total = new HashSet<Page>();
    						total.addAll((Set<Page>) arg1);
    						total.addAll((Set<Page>) arg2);
    						terms.push(total);
    					} else {
    						// error
    						System.err.println("WebQueryIndex: Fatal error at |!");
    						System.exit(1);
    					}
    				}
    			}
    		}
    		
    		// if postfix failed to reduce to answer, input was invalid
    		if (terms.size() != 1)
    			throw new IllegalArgumentException("Operators must reduce terms to one term!");

    		if (terms.peek() instanceof String) {
    			return index.getUrls((String) terms.pop());
    		}
    		return (Set<Page>) terms.pop();
    }
    
    /**
     * Does all formatting of Query and returns a
     * postfix queue of String or null if the query is
     * invalid.
     */
    public Queue<String> getPostFix(String query) {
    		query = query.toLowerCase();
    		
    		// I use + marks to represent phrases, so some queries with + marks
    		// might pass when they shouldn't, thus any query with a + is 
    		// instantly failed
    		if (query.contains("+"))
    			throw new IllegalArgumentException("'+' character not allowed in query!");
    		
    		// fix spacing such that each token is 1-space separated
    		query = fixSpacing(query);
    		
    		// change phrases from "hello my" to hello+my
    		query = fixPhrases(query);
    		
    		// fix spacing on !, need to do after fixing phrases
    		query = query.replaceAll("!\\s*", "!");

    		// validate given query, throws appropriate exception
    		// if not valid
    		isValidQuery(query);

    		// add explicit and, convert to postfix
    		return convertToPostfix(addExplicitAND(query.split(" ")));
    }

    /**
     * Adds spacing to quotes and operators such that
     * each is one-space-separated.
     */
    public String fixSpacing(String query) {
    		// pad valid operators with spaces
    		String temp = query.replaceAll("\\(", " ( ");
    		temp = temp.replaceAll("\\)", " ) ");
    		temp = temp.replaceAll("&", " & ");
    		temp = temp.replaceAll("\\|", " | ");
    		temp = temp.replaceAll("\"", " \" ");
    		
    		// trim all excess spaces to at max one space
        temp = temp.replaceAll("\\s+", " ");
        return temp.trim();
    }

    /**
     * Converts all phrases to concatenations with the + character.
     */
    public String fixPhrases(String input) {
    		// first check for balanced number of quotes
    		int quotes = 0;
    		for (int i = 0; i < input.length(); i++) {
    			if (input.charAt(i) == '"')
    				quotes++;
    		}
    		if (quotes % 2 != 0)
    			throw new IllegalArgumentException("Number of quotes must be balanced!");

    		ArrayList<String> tokens = new ArrayList<String>(Arrays.asList(input.split(" ")));
    		for (int i = 0; i < tokens.size()-1; i++) {
    			// check if there is open bracket
    			if (tokens.get(i).equals("\"")) {
    				tokens.remove(i);
    				String concat = "";
    				while (!tokens.get(i).equals("\"")) {
    					concat += tokens.get(i) + "+";
    					tokens.remove(i);
    				}
    				tokens.remove(i);
    				// if empty phrase, query is invalid
    				if (concat.isEmpty())
    					throw new IllegalArgumentException("Cannot parse a null phrase!");
    				tokens.add(i, concat.substring(0, concat.length()-1));
    			}
    		}
    		return String.join(" ",tokens);
    }

    /**
     * Adds the explicit and between any two adjacent non-operators.
     */
    public String[] addExplicitAND(String[] query) {
    		ArrayList<String> tokens = new ArrayList<String>(Arrays.asList(query));
    		Set<String> operators = new HashSet<String>(Arrays.asList("&", "|"));
		// add implicit AND's
		for (int i = 0; i < tokens.size()-1; i++) {
			// add &'s between two non-operators
			if (!operators.contains(tokens.get(i)) && !operators.contains(tokens.get(i+1))) {
				// filter out case ), ) and (, (
				if (tokens.get(i).equals("(") || tokens.get(i+1).equals(")")) {
					// do nothing
				} else
                    tokens.add(i+1, "&");
			}
		}
		return tokens.toArray(new String[0]);
    }
    
    /**
     * Given a query of 1-space separated tokens throws an 
     * Exception if the query is invalid.
     */
    public void isValidQuery(String query) {
    		// make sure every token is either operator or word
    		for (String token : query.split(" ")) {
    			if (!token.equals("(") && !token.equals(")")
    					&& !token.equals("&") && !token.equals("|")
    					&& !WebIndex.isWord(token))
    				throw new IllegalArgumentException("'" + token + "' token not allowed in query!");
    		}

    		// check for equal amount of open and close brackets
    		int brackets = 0;
    		for (int i = 0; i < query.length(); i++) {
    			if (query.charAt(i) == '(')
    				brackets++;
    			else if (query.charAt(i) == ')')
    				brackets--;
    			if (brackets < 0)
    				throw new IllegalArgumentException("Mismatched parenthesis in query!");
    		}
    		
    		// if brackets is not zero, num of brackets is unbalanced
    		if (brackets != 0)
    			throw new IllegalArgumentException("Number of brackets must be balanced!");
    		
    		// maps what characters cannot follow each other 
    		// regex are (, ), &, |, word
    		HashMap<String, List<String>> cantFollow = new HashMap<String, List<String>>();
    		cantFollow.put("\\(", Arrays.asList("&", "|"));
    		cantFollow.put("\\)", Arrays.asList());
    		cantFollow.put("&", Arrays.asList(")", "&", "|"));
    		cantFollow.put("\\|", Arrays.asList(")", "&", "|"));
    		
    		String[] tokens = query.split(" ");
    		for (int i = 0; i < tokens.length-1; i++) {
    			List<String> noFollow = new ArrayList<String>();
    			for (String s : cantFollow.keySet()) {
    				if (tokens[i].matches(s))
    					noFollow = cantFollow.get(s);
    			}
    			
    			if (noFollow.contains(tokens[i+1]))
    				throw new IllegalArgumentException("'" + tokens[i+1] + "' cannot follow follow '" + tokens[i] +"' token!");
    		}
    }
    
    
    /**
     * Convertes an infix Query to a postfix tree using
     * Shunting-yard algorithm.
     * @return
     */
    public Queue<String> convertToPostfix(String[] query) {
    		// intialize terms and operators queue/stack
    		Queue<String> output = new LinkedList<String>();
    		Stack<String> operators = new Stack<String>();

    		for (int i = 0; i < query.length; i++) {
    			// if word, move to output queue
    			if (WebIndex.isWord(query[i])) {
    				output.add(query[i]);
    			} else if (query[i].equals("(")) {
    				operators.push(query[i]);
    			} else if (query[i].equals(")")) {
    				String tempOp;
    				while (!(tempOp = operators.pop()).equals("(")) {
    					output.add(tempOp);
    				}
    			} else {
    				// we have an operator
    				// peek at top of operator stack
    				while (!operators.isEmpty() && 
    						((query[i].equals("|")) || (query[i].equals("&") && operators.peek().equals("&"))) &&
    						!operators.peek().equals("(")) {
    					output.add(operators.pop());
    				}
    				operators.push(query[i]);
    			}
    		}
    		
    		// while there are still operators on the stack
    		while (!operators.isEmpty()) {
    			output.add(operators.pop());
    		}
    		return output;
    }
}
