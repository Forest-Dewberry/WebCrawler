package assignment;
import java.net.URL;
import java.util.*;

/**
 * A query engine which holds an underlying web index and can answer textual queries with a
 * collection of relevant pages.
 *
 * TODO: Implement this!
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
     * Returns a WebQueryEngine that uses the given Index to constructe answers to queries.
     *
     * @param index The WebIndex this WebQueryEngine should use.
     * @return A WebQueryEngine ready to be queried.
     */
    public static WebQueryEngine fromIndex(WebIndex i) {
        return new WebQueryEngine(i);
    }

    /**
     * Returns a Collection of URLs (as Strings) of web pages satisfying the query expression.
     *
     * @param query A query expression.
     * @return A collection of web pages satisfying the query.
     */
    public Collection<Page> query(String query) {
    		Queue<String> postfix = getPostFix(query);

    		// return null if input query is invalid
    		if (postfix == null)
    			return null;

    		Queue<Object> terms = new LinkedList<Object>();
    		
    		for (String token : postfix) {
    			if (token.matches("!?[A-Za-z-]+") || token.matches("((?:[A-Za-z-]+\\+)+[A-Za-z-]+)")) {
    				terms.add(token);
    			}
    			else {
    				if (token.equals("&")) {
    					assert terms.size() >= 2 : "WebQueryIndex : postfix notation broken!";
    					
    					Object arg1 = terms.poll();
    					Object arg2 = terms.poll();
    					
    					// four cases: Str-str, str-list, list-list, list-str
    					// str/str
    					if (arg1 instanceof String && arg2 instanceof String) {
    						Set<Page> result = new HashSet<Page>();
    						result = index.getUrls((String) arg1);
    						result = index.getUrls((String) arg2, result);
    						terms.add(result);
    					} else if (arg1 instanceof Set && arg2 instanceof String) {
    						arg1 = index.getUrls((String) arg2, (Set<Page>) arg1);
    						terms.add(arg1);
    					} else if (arg1 instanceof String && arg2 instanceof Set) {
    						arg2 = index.getUrls((String) arg1, (Set<Page>) arg2);
    						terms.add(arg2);
    					} else if (arg1 instanceof Set && arg2 instanceof Set) {
    						terms.add(((Set<Page>) arg1).retainAll((Set<Page>) arg2));
    					} else {
    						// error
    						System.err.println("WebQueryIndex: Fatal error at &!");
    						System.exit(1);
    					}
    					
    				} else if (token.equals("|")) {
    					assert terms.size() >= 2 : "WebQueryIndex : postfix notation broken!";

    					Object arg1 = terms.poll();
    					Object arg2 = terms.poll();
    					
    					// four cases: Str-str, str-list, list-list, list-str
    					// str/str
    					if (arg1 instanceof String && arg2 instanceof String) {
    						Set<Page> result = new HashSet<Page>();
    						result = index.getUrls((String) arg1);
    						result.addAll(index.getUrls((String) arg2));
    						terms.add(result);
    					} else if (arg1 instanceof Set && arg2 instanceof String) {
    						Set<Page> arg2Set = index.getUrls((String) arg2);
    						arg2Set.addAll((Set<Page>) arg1);
    						terms.add(arg2Set);
    					} else if (arg1 instanceof String && arg2 instanceof Set) {
    						Set<Page> arg1Set = index.getUrls((String) arg1);
    						arg1Set.addAll((Set<Page>) arg2);
    						terms.add(arg1Set);
    					} else if (arg1 instanceof Set && arg2 instanceof Set) {
    						terms.add(((Set<Page>) arg1).addAll((Set<Page>) arg2));
    					} else {
    						// error
    						System.err.println("WebQueryIndex: Fatal error at |!");
    						System.exit(1);
    					}
    				}
    			}
    		}
    		
    		assert terms.size() == 1 : "WebQueryEngine : not all terms processed!";
    		if (terms.peek() instanceof String) {
    			return index.getUrls((String) terms.poll());
    		}
    		return (Set<Page>) terms.poll();
    }
    
    /**
     * Does all formatting of Query and returns a
     * postfix queue of String or null if the query is
     * invalid.
     */
    public Queue<String> getPostFix(String query) {
    		query = query.toLowerCase();
    		
    		// first fix spacing such that each token is 1-space separated
    		query = fixSpacing(query);
    		
    		// change phrases from "hello my" to hello+my
    		query = fixPhrases(query);
    		
    		// validate given query
    		if (!isValidQuery(query)) {
    			return null;
    		}

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
    				tokens.add(i, concat.substring(0, concat.length()-1));
    			}
    		}
    		return String.join(" ",tokens);
    }

    /**
     * Adds the explicit and between any two adjacent strings.
     */
    public String[] addExplicitAND(String[] query) {
    	ArrayList<String> tokens = new ArrayList<String>(Arrays.asList(query));
		
    		Set<String> operators = new HashSet<String>(Arrays.asList("&", "|"));
		// add implicit AND's
		for (int i = 0; i < tokens.size()-1; i++) {
			// add &'s between two non-operators
			if (!operators.contains(tokens.get(i)) && !operators.contains(tokens.get(i+1))) {
				// filter out case ), ) and (, (
				if (!tokens.get(i).equals(tokens.get(i+1)) &&
						!tokens.get(i).equals("(") && !tokens.get(i+1).equals(")")) {
					tokens.add(i+1, "&");
				}
			}
		}
		
		return tokens.toArray(new String[0]);
    }
    
    /**
     * Given a query of 1-space separated tokens
     * returns whether or not it is a valid query.
     */
    public boolean isValidQuery(String query) {
    		// make sure every token is either operator or word
    		for (String token : query.split(" ")) {
    			if (!token.matches("!?[A-Za-z-]+") && !token.equals("(") && !token.equals(")")
    					&& !token.equals("&") && !token.equals("|")
    					&& !token.matches("((?:[A-Za-z-]+\\+)+[A-Za-z-]+)"))
    				return false;
    		}

    		// check for equal amount of open and close brackets
    		int brackets = 0;
    		for (int i = 0; i < query.length(); i++) {
    			if (query.charAt(i) == '(')
    				brackets++;
    			else if (query.charAt(i) == ')')
    				brackets--;
    		}
    		
    		// if brackets is not zero, num of brackets is unbalanced
    		if (brackets != 0)
    			return false;
    		
    		return true;
    }
    
    
    /**
     * Convertes an infix Query to a postfix tree using
     * Shunting-yard algorithm.
     * @return
     */
    public Queue<String> convertToPostfix(String[] query) {
    		Queue<String> output = new LinkedList<String>();
    		Stack<String> operators = new Stack<String>();
    		for (int i = 0; i < query.length; i++) {
    			// if word, move to output queue
    			if (query[i].matches("!?[A-Za-z-]+") || query[i].matches("((?:[A-Za-z-]+\\+)+[A-Za-z-]+)")) {
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
