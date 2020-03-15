/**
 * Created by anton on 25/03/16.
 */

package ir_course;


import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class ClirSearch {

    String analyzer;
    Boolean stemmer;
    Boolean stopwords;
    int totalDocs = 0;
    int relevantDocs = 0;
    int relevantRetrieved = 0;
    int retrieved = 0;
    List<Float> precision = new ArrayList<>();
    List<Float> recall = new ArrayList<>();
    Directory corpus = new RAMDirectory();

    public ClirSearch(String analyzer, Boolean stemmer, Boolean stopwords) 
    {
        this.analyzer = analyzer;
        this.stemmer = stemmer;
        this.stopwords = stopwords;

    }

    public IndexWriterConfig index(List<DocumentInCollection> docs) throws IOException 
    {

        Analyzer analyz;
        IndexWriterConfig config;

        if (analyzer.equals("vsm") && stopwords && stemmer) 
        {
            //VSM cosine similarity with TFIDF + stopwords + stemmer
            analyz = new EnglishAnalyzer();
            config = new IndexWriterConfig(analyz);
            config.setSimilarity(new ClassicSimilarity());
        } 
        else if (analyzer.equals("vsm") && !stopwords && stemmer) 
        {
            //VSM cosine similarity with TFIDF - stopwords + stemmer
            analyz = new EnglishAnalyzer(CharArraySet.EMPTY_SET);
            config = new IndexWriterConfig(analyz);
            config.setSimilarity(new ClassicSimilarity());
        } 
        else if (analyzer.equals("vsm") && stopwords && !stemmer) 
        {
            //VSM cosine similarity with TFIDF - stopwords - stemmer
            analyz = new StandardAnalyzer();
            config = new IndexWriterConfig(analyz);
            config.setSimilarity(new ClassicSimilarity());
        } 
        else if (analyzer.equals("bm25") && stopwords && stemmer) 
        {
            //Analyzer + stopwords + stemmer
            analyz = new EnglishAnalyzer();
            config = new IndexWriterConfig(analyz);
            //BM25 ranking method
            config.setSimilarity(new BM25Similarity());
        } 
        else if (analyzer.equals("bm25") && !stopwords && stemmer) 
        {
            //Analyzer - stopwords + stemmer
            analyz = new EnglishAnalyzer(CharArraySet.EMPTY_SET);
            config = new IndexWriterConfig(analyz);
            //BM25 ranking method
            config.setSimilarity(new BM25Similarity());
        } 
        else if (analyzer.equals("bm25") && stopwords && !stemmer) 
        {
            //Analyzer + stopwords - stemmer
            analyz = new StandardAnalyzer();
            config = new IndexWriterConfig(analyz);
            //BM25 ranking method
            config.setSimilarity(new BM25Similarity());
        } 
        else 
        {
            //some default
            analyz = new StandardAnalyzer();
            config = new IndexWriterConfig(analyz);
            config.setSimilarity(new ClassicSimilarity());
        }


        IndexWriter w = new IndexWriter(corpus, config);

        //total 153 documents with group 5
        for (DocumentInCollection doc1 : docs) {
            if (doc1.getSearchTaskNumber() == 5) {
                Document doc = new Document();
                doc.add(new TextField("title", doc1.getTitle(), Field.Store.YES));
                doc.add(new TextField("abstract_text", doc1.getAbstractText(), Field.Store.YES));
                doc.add(new TextField("relevance", Boolean.toString(doc1.isRelevant()), Field.Store.YES));
                w.addDocument(doc);
                totalDocs++;
                if (doc1.isRelevant()) relevantDocs++;
            }

        }

        w.close();

        return config;
    }

    public List<String> search(String searchQuery, IndexWriterConfig cf) throws IOException {

        printQuery(searchQuery);

        List<String> results = new LinkedList<String>();


        //Constructing QueryParser to stem search query
        QueryParser qp = new QueryParser("abstract_text", cf.getAnalyzer());
        Query parsedQuery = null;

        try {
            parsedQuery = qp.parse(searchQuery);
            System.out.println(parsedQuery);
        } catch (ParseException e) {
            e.printStackTrace();
        }



        // opening directory for search
        IndexReader reader = DirectoryReader.open(corpus);
        // implementing search over IndexReader
        IndexSearcher searcher = new IndexSearcher(reader);

        searcher.setSimilarity(cf.getSimilarity());

        // finding top totalDocs documents qualifying the search
        TopDocs docs = searcher.search(parsedQuery, totalDocs);


        // representing array of hits from TopDocs
        ScoreDoc[] scored = docs.scoreDocs;


        // adding matched doc titles to results
        for (ScoreDoc aDoc : scored) {
            Document d = searcher.doc(aDoc.doc);
            retrieved++;
            //relevance and score are printed out for debug purposes
            if (d.get("relevance").equals("true")) {
                relevantRetrieved++;
                results.add("+ " + d.get("title") + " | relevant: " + d.get("relevance") + " | score: " + aDoc.score);
            } else {
                results.add("- " + d.get("title") + " | relevant: " + d.get("relevance") + " | score: " + aDoc.score);
            }
            precision.add((float)relevantRetrieved/retrieved);
            recall.add((float)relevantRetrieved/relevantDocs);
        }


        return results;
    }

    public void printQuery(String searchQuery) {
        System.out.print("Search (");
        if (searchQuery != null) {
            System.out.print("Search query: " + searchQuery);
        }
        System.out.println("):");
    }

    public void printResults(List<String> results) {
        if (results.size() > 0) {
            for (int i=0; i<results.size(); i++)
                System.out.println(" " + (i+1) + ". " + results.get(i));
        }
        else
            System.out.println(" no results");
    }

    public void startSearch(List<DocumentInCollection> docs, String searchQuery) {
        try {
        	IndexWriterConfig cf = index(docs);
            
            List<String> results = search(searchQuery, cf);
            //printResults(results);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public int nearest(float of, List<Float> in) {
        float min = Float.MAX_VALUE;
        int indexer = 0;
        int theIndexToReturn = 0;
        for (Float v : in)
        {
            float diff = Math.abs(v - of);

            if (diff < min) {
                min = diff;
                theIndexToReturn = indexer;
            }
            indexer++;
        }
        return theIndexToReturn;
    }

    public void engineSearchPrintout() {
        /*
        System.out.println("Total docs in the collection: " + totalDocs +
                "\t Total relevant docs in the collection: " + relevantDocs);
        System.out.println("Retrieved docs: " + retrieved + "\t " +
                "Relevant in retrieved: " + relevantRetrieved);
        System.out.println("Precision: " + (float)relevantRetrieved / retrieved);
        System.out.println("Recall: " + (float)relevantRetrieved/relevantDocs);
        */
        int[] arr = new int[0];
        for (int i=0;i<11;i++) {
            int index = nearest(i/10f, recall);
            float max = precision.get(index);
            for (int j=index;j<precision.size();j++) {
                if (precision.get(j) > max) {
                    max = precision.get(j);
                }
            }
            System.out.println(recall.get(index) + "," + max);
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length > 0) {
            //getting document collection
            DocumentCollectionParser parser = new DocumentCollectionParser();
            parser.parse(args[0]);
            List<DocumentInCollection> docs = parser.getDocuments();

            //creating new search engine VSM with Porter stemmer and stopwords
            System.out.println("VSM with Porter stemmer and stopwords");
            ClirSearch engine = new ClirSearch("vsm", true, true);
            //doing search
            engine.startSearch(docs, "cross-language information retrieval");
            System.out.println(">>> INTERPOLATED <<<");
            engine.engineSearchPrintout();
            System.out.println(">>> ALL <<<");
            for (int i=0; i<engine.recall.size();i++) {
                System.out.println(engine.recall.get(i) + "," + engine.precision.get(i));
            }

/*
            //VSM with Porter stemmer and no stopwords
            System.out.println("\n\nVSM with Porter stemmer and no stopwords");
            ClirSearch engine2 = new ClirSearch("vsm", true, false);
            engine2.startSearch(docs, "cross-language information retrieval");
            engine2.engineSearchPrintout();


            //VSM with no Porter stemmer and stopwords
            System.out.println("\n\nVSM with no Porter stemmer and stopwords");
            ClirSearch engine3 = new ClirSearch("vsm", false, true);
            engine3.startSearch(docs, "cross-language information retrieval");
            engine3.engineSearchPrintout();

            //BM25 with Porter stemmer and stopwords
            System.out.println("\n\nBM25 with Porter stemmer and stopwords");
            ClirSearch engine4 = new ClirSearch("bm25", true, true);
            engine4.startSearch(docs, "cross-language information retrieval");
            engine4.engineSearchPrintout();

            //BM25 with Porter stemmer and no stopwords
            System.out.println("\n\nBM25 with Porter stemmer and no stopwords");
            ClirSearch engine5 = new ClirSearch("bm25", true, false);
            engine5.startSearch(docs, "cross-language information retrieval");
            engine5.engineSearchPrintout();

            //BM25 with no Porter stemmer and stopwords
            System.out.println("\n\nBM25 with no Porter stemmer and stopwords");
            ClirSearch engine6 = new ClirSearch("bm25", false, true);
            engine6.startSearch(docs, "cross-language information retrieval");
            engine6.engineSearchPrintout();

            //creating new search engine VSM with Porter stemmer and stopwords
            System.out.println("VSM with Porter stemmer and stopwords");
            ClirSearch engine7 = new ClirSearch("vsm", true, true);
            //doing search
            engine7.startSearch(docs, "translingual information retrieval");
            engine7.engineSearchPrintout();

            //VSM with Porter stemmer and no stopwords
            System.out.println("\n\nVSM with Porter stemmer and no stopwords");
            ClirSearch engine8 = new ClirSearch("vsm", true, false);
            engine8.startSearch(docs, "translingual information retrieval");
            engine8.engineSearchPrintout();


            //VSM with no Porter stemmer and stopwords
            System.out.println("\n\nVSM with no Porter stemmer and stopwords");
            ClirSearch engine9 = new ClirSearch("vsm", false, true);
            engine9.startSearch(docs, "translingual information retrieval");
            engine9.engineSearchPrintout();

            //BM25 with Porter stemmer and stopwords
            System.out.println("\n\nBM25 with Porter stemmer and stopwords");
            ClirSearch engine10 = new ClirSearch("bm25", true, true);
            engine10.startSearch(docs, "translingual information retrieval");
            engine10.engineSearchPrintout();

            //BM25 with Porter stemmer and no stopwords
            System.out.println("\n\nBM25 with Porter stemmer and no stopwords");
            ClirSearch engine11 = new ClirSearch("bm25", true, false);
            engine11.startSearch(docs, "translingual information retrieval");
            engine11.engineSearchPrintout();

            //BM25 with no Porter stemmer and stopwords
            System.out.println("\n\nBM25 with no Porter stemmer and stopwords");
            ClirSearch engine12 = new ClirSearch("bm25", false, true);
            engine12.startSearch(docs, "translingual information retrieval");
            engine12.engineSearchPrintout();
            */


        }
        else
            System.out.println("ERROR: the path of a RSS Feed file has to be passed as a command line argument.");
    }
}


