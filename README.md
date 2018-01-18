# WebCrawler
As a Turing Scholar, our final project in Data Structures was to create an HTML WebCrawler to index a portion of the web and then build a search engine to parse queries for text on the page and return relevant webpages. The queries supported the &, |, and ! operators as well as parenthesis.

## Building the Index
The crawler parsed the HTML using the [Attoparser](http://www.attoparser.org) library. Given a starting page, the crawler finds all the links on that page and adds them to a queue of links to index next. The parser takes every word on the page and hashes the current URL with that word, so the list of URLs associated with a given word can be retrieved in constant time. The index is then stored into the `index.db` file.

## Search Engine
The search engine loads the index and accepts word queries. The queries are then validated and converted to postfix using the [Shunting-Yard algorithim](https://en.wikipedia.org/wiki/Shunting-yard_algorithm). From there, the &, |, and ! operations are executed using set difference and interesection on the list of URLs returned by the index. Finally, the list of relevant pages is returned.

## Some Images
![example search query](images/img1.png)
![example search results](images/img2.png)
