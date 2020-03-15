package ir_course;

import java.util.List;



/**
 * Represents a search result.
 *
 * Contains all information about search engine,
 * search query and search results.
 */
public class SearchResult {
	
	Integer ranking;
	Integer stemmer;
	Integer stopwords;	
	String query;
	
	int totalDocuments;
	int relevantDocuments;
	int totalRetrieved;
	int relevantRetrieved;
	
	List<String> stringResults;
	List<Integer> relevances;
	
	
	
	/**
	 * Constructor.
	 */
	public SearchResult(
			Integer ranking,
			Integer stemmer,
			Integer stopwords,
			String query,
			int totalDocuments,
			int relevantDocuments,
			int totalRetrieved,
			int relevantRetrieved,
			List<String> stringResults,
			List<Integer> relevances) {
		this.ranking = ranking;
		this.stemmer = stemmer;
		this.stopwords = stopwords;
		this.query = query;
		this.totalDocuments = totalDocuments;
		this.relevantDocuments = relevantDocuments;
		this.totalRetrieved = totalRetrieved;
		this.relevantRetrieved = relevantRetrieved;
		this.stringResults = stringResults;
		this.relevances = relevances;
	}
	
	
	
	/**
	 * Used to print out all information about the search,
	 * including engine configuration, query, document count
	 * and top results.
	 */
	public void report() {
		String c1 = "";
		String c2 = "";
		String c3 = "";
		
		if(ranking == 1){
			c1 = "ranking:VSM";
		} else if(ranking == 2) {
			c1 = "ranking:BM25";
		}
		
		if(stemmer == 0){
			c2 = "stemming:NO";
		} else if(stemmer == 1) {
			c2 = "stemming:YES";
		}
		
		if(stopwords == 0){
			c3 = "stopwords:NO";
		} else if(stopwords == 1) {
			c3 = "stopwords:YES";
		}
		
		System.out.println("Engine configuration: " + c1 + ", " + c2 + ", " + c3);
		System.out.println(query);
		System.out.println("Documents total: " + totalDocuments + " (" + relevantDocuments + " relevant)");
		System.out.println("Documents retrieved: " + totalRetrieved + " (" + relevantRetrieved + " relevant)");
		System.out.println("Top 25 results: ");
		for(int i = 0; i < 25; i++){
			System.out.println(i+1 + ". " + stringResults.get(i));
		}
		System.out.println("");
	}

}
