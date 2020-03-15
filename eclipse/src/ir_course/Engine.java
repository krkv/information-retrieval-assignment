package ir_course;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

public class Engine {
	
	Integer ranking;
	Integer stemmer;
	Integer stopwords;
	int totalDocs;
	int relevantDocs;
	int totalRet;
	int relevantRet;

	
	
	/**
	 * Constructor uses predefined code
	 * to create a search engine.
	 */
	public Engine(String code) {				
		this.ranking = Integer.parseInt(code.charAt(0)+"");
		this.stemmer = Integer.parseInt(code.charAt(1)+"");
		this.stopwords = Integer.parseInt(code.charAt(2)+"");
		this.totalDocs = 0;
		this.relevantDocs = 0;
		this.totalRet = 0;
		this.relevantRet = 0;
	}
	
	
	
	/**
	 * Create an Analyzer according to engine
	 * configuration.
	 * 
	 * Return Analyzer.
	 */
	public Analyzer getAnalyzer() {
		if(stemmer == 1) {
			if(stopwords == 1) {
				return new EnglishAnalyzer();
			} else {
				return new EnglishAnalyzer(CharArraySet.EMPTY_SET);
			}
		} else {
			if(stopwords == 1) {
				return new StandardAnalyzer();
			} else {
				return new StandardAnalyzer(CharArraySet.EMPTY_SET);
			}
		}
	}
	
	
	
	/**
	 * Create an IndexWriterConfig according to
	 * engine configuration and using Analyzer.
	 * 
	 * Return IndexWriterConfig.
	 */
	public IndexWriterConfig getConfig(Analyzer analyzer) {
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		if(ranking == 1) {
			config.setSimilarity(new ClassicSimilarity());
		} else {
			config.setSimilarity(new BM25Similarity());
		}
		return config;		
	}
	
	
	
	/**
	 * Create a RAMDirectory using IndexWriterConfig and
	 * a collection of documents.
	 * 
	 * Return RAMDirectory.
	 */
	public Directory getDirectory(IndexWriterConfig config,
			List<DocumentInCollection> dics) throws IOException {
		
		Directory directory = new RAMDirectory();		
		IndexWriter writer = new IndexWriter(directory, config);
		
		for (DocumentInCollection dic: dics) {
			
			Document doc = new Document();
			doc.add(new TextField("title", dic.getTitle(), Field.Store.YES));
            doc.add(new TextField("abstract_text", dic.getAbstractText(), Field.Store.YES));
            
            // write ALL documents to directory,
            // but only relevant documents for task 5 are considered relevant
            if(dic.getSearchTaskNumber() == 5) {
            	doc.add(new TextField("relevance", Boolean.toString(dic.isRelevant()), Field.Store.YES));
            } else {
            	doc.add(new TextField("relevance", "false", Field.Store.YES));
            }
            writer.addDocument(doc);
            totalDocs++;
            if (dic.isRelevant() && dic.getSearchTaskNumber() == 5) relevantDocs++;
			
		}
		writer.close();
		return directory;
	}
	
	
	
	/**
	 * Parse a String query.
	 * 
	 * Return parsed Query.
	 */
	public Query getQuery(String query, Analyzer analyzer) throws ParseException {
		QueryParser parser = new QueryParser("abstract_text", analyzer);
		Query parsedQuery = parser.parse(query);
		return parsedQuery;
	}
	
	
	
	/**
	 * Perform search of Query in Directory.
	 * 
	 * Apply correct ranking according to engine configuration.
	 * 
	 * Retrieve top 1000 documents from returned results.
	 * 
	 * Return a SearchResult object.
	 */
	public SearchResult search(Query query, Directory directory) throws IOException {
		
		List<String> stringResults = new LinkedList<String>();
		List<Integer> integerResults = new LinkedList<Integer>();		
		
		IndexReader reader = DirectoryReader.open(directory);
		IndexSearcher searcher = new IndexSearcher(reader);
		
		// apply ranking
		if (ranking == 1) {
			searcher.setSimilarity(new ClassicSimilarity());		
		} else {
			searcher.setSimilarity(new BM25Similarity());
		}
		
		// take 1000 top documents
		TopDocs topDocs = searcher.search(query, 1000);
		ScoreDoc[] scoredDocs = topDocs.scoreDocs;
		
		// save results to variables
		for (ScoreDoc sDoc : scoredDocs) {
			Document doc = searcher.doc(sDoc.doc);
			totalRet++;
			if (doc.get("relevance").equals("true")) {
                relevantRet++;
                integerResults.add(1);
                stringResults.add("+ " + doc.get("title") + " | relevant: " + doc.get("relevance") + " | score: " + sDoc.score);
            } else {
            	integerResults.add(0);
                stringResults.add("- " + doc.get("title") + " | relevant: " + doc.get("relevance") + " | score: " + sDoc.score);
            }
		}
		
		// create a SearchRelut object
		// populated with obtained data
		SearchResult sr = new SearchResult(
				ranking,
				stemmer,
				stopwords,
				query.toString(),
				totalDocs,
				relevantDocs,
				totalRet,
				relevantRet,
				stringResults,
				integerResults);
		
		return sr;
		
	}

	
	
	/**
	 * Main helper function.
	 * 
	 * Performs all required engine actions to find search results
	 * given String Query in DocumentInCollection List.
	 * 
	 * Returns a SearchResult object.
	 */
	public SearchResult doSearch(String originalQuery, List<DocumentInCollection> dics) throws Exception {
		Analyzer analyzer = getAnalyzer();
		IndexWriterConfig config = getConfig(analyzer);
		Directory directory = getDirectory(config, dics);
		Query query = getQuery(originalQuery, analyzer);
		SearchResult sr = search(query, directory);
		//System.out.println("Original query: " + originalQuery);
		//System.out.println("Total: " + totalDocs);
		//System.out.println("Relevant: " + relevantDocs);
		//System.out.println("Total retrieved: " + totalRet);
		//System.out.println("Relevant retrieved: " + relevantRet);
		//System.out.println("Recall = " + ((float)relevantRet / relevantDocs));
		//System.out.println("Precision = " + ((float)relevantRet / totalRet));
		//printResults(sr.stringResults);
		return sr;
	}
	
}