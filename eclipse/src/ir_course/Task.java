package ir_course;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;



/**
 * Contains methods that utilises Engine objects
 * and SearchResult objects in order to perform search
 * and to compute 11 average interpolated precision
 * values for every Engine configuration.
 */
public class Task {

	
	
	/**
	 * Search through a list of all recall values
	 * to find the one closest to a target recall value.
	 */
	public static int findNearest(float target, List<Float> values)
	{
		float minSeen = Float.MAX_VALUE;
		int minSeenIndex = 0;
		int indexer = 0;
		for (Float v : values)
		{
			float diff = Math.abs(v - target);
			if (diff < minSeen)
			{
				minSeen = diff;
				minSeenIndex = indexer;
			}
			indexer++;
		}
		return minSeenIndex;
	}

	
	
	/**
	 * Computes precision and recall values for all depth
	 * levels of search results.
	 * 
	 * Computes and returns 11 precision steps for 11-step
	 * precision-recall curves.
	 * 
	 * Not interpolated precision values! For debugging.
	 */
	public static List<Float> computeOriginalPrecisions(SearchResult result) {		
		
		List<Integer> relevances = result.relevances;				
		int relevantDocuments = result.relevantDocuments;		
		
		// all recall and precision values
		List<Float> allRecalls = new LinkedList<Float>();
		List<Float> allPrecisions = new LinkedList<Float>();

		// 11 recall and precision values for the curves
		List<Float> recalls11 = new LinkedList<Float>();
		List<Float> precisions11 = new LinkedList<Float>();

		// fill recalls11 with values required for
		// 11-step precision-recall curve:
		// 0.0, 0.1, 0.2, 0.3, 0.4, 0.5,
		// 0.6, 0.7, 0.8, 0.9, 1.0
		for(int i = 0; i < 11; i++) {
			recalls11.add((float)i/10);
		}

		// compute all recall and precision values
		int currentTotal = 0;
		int currentRelevant = 0;
		for(int i = 0; i < relevances.size(); i++) {
			currentTotal++;
			if(relevances.get(i) == 1) currentRelevant++;
			float currentPrecision = (float)currentRelevant/currentTotal;
			float currentRecall = (float)currentRelevant/relevantDocuments;
			allPrecisions.add(i, currentPrecision);
			allRecalls.add(i, currentRecall);
		}

		// find precision values in 11 recall steps
		// that are closest to required recall levels
		for(int i = 0; i < 11; i++) {
			float goal = recalls11.get(i);			
			int closest = findNearest(goal, allRecalls);
			precisions11.add(allPrecisions.get(closest));
		}

		return precisions11;		
	}	

	
	
	/**
	 * Computes precision and recall values for all depth
	 * levels of search results.
	 * 
	 * Computes and returns 11 precision steps for 11-step
	 * precision-recall curves.
	 * 
	 * Interpolates precision values. Values are suitable to use
	 * for 11-step interpolated precision-recall curves.
	 */
	public static List<Float> computeInterpolatedPrecisions(SearchResult result) {

		List<Integer> relevances = result.relevances;
		int relevantDocuments = result.relevantDocuments;

		// all recall and precision values
		float[] allPrecisions = new float[relevances.size()];
		List<Float> allRecalls = new LinkedList<Float>();
		
		// 11 recall and precision values for the curves
		List<Float> precisions11 = new LinkedList<Float>();
		List<Float> recalls11 = new LinkedList<Float>();

		// compute all recall and precision values
		int currentTotal = 0;
		int currentRelevant = 0;
		for(int i = 0; i < relevances.size(); i++) {
			currentTotal++;
			if(relevances.get(i) == 1) currentRelevant++;
			float currentPrecision = (float)currentRelevant/currentTotal;
			float currentRecall = (float)currentRelevant/relevantDocuments;
			//System.out.println(currentPrecision + " " + currentRecall);
			allPrecisions[i] = currentPrecision;
			allRecalls.add(currentRecall);
		}

		// fill recalls11 with values required for
		// 11-step precision-recall curve:
		// 0.0, 0.1, 0.2, 0.3, 0.4, 0.5,
		// 0.6, 0.7, 0.8, 0.9, 1.0
		for(int i = 0; i < 11; i++) {
			recalls11.add((float)i/10);
		}
		
		// find precision values in 11 recall steps
		// that are closest to required recall levels
		//
		// interpolate discovered precision values
		for(int i = 0; i < 11; i++) {
			float goal = recalls11.get(i);			
			int closest = findNearest(goal, allRecalls);
			
			float notInterpolated = allPrecisions[closest];
			float[] leftoverPrecisions = Arrays.copyOfRange(allPrecisions, closest, allPrecisions.length);

			float maxSeen = notInterpolated;
			for(int k = 0; k < leftoverPrecisions.length; k++) {
				if(leftoverPrecisions[k] > maxSeen) {
					maxSeen = leftoverPrecisions[k];
				}
			}
			float interpolated = maxSeen;
			precisions11.add(interpolated);
		}

		return precisions11;

	}

	
	
	/**
	 * Takes three lists of 11 (interpolated) precision values
	 * and computes one list of 11 average precisions.
	 * 
	 * Required to find average interpolated precision
	 * over three different search queries. 
	 */
	public static List<Float> averagePrecisions(List<List<Float>> precisions) {		
		
		// create an empty list with 11 float zeroes
		List<Float> averagedPrecisions = new LinkedList<Float>();
		for(int i = 0; i < 11; i++) {
			averagedPrecisions.add(Float.valueOf(0));
		}	
		
		// populate the list with sum of three given precision values
		for(int i = 0; i < 3; i++) {
			for(int j = 0; j < 11; j++) {
				float currentValue = averagedPrecisions.get(j);
				float newValue = currentValue + precisions.get(i).get(j);
				averagedPrecisions.set(j, newValue);
			}
		}
		
		// divide each element of the list by 3 to get an average
		for(int i = 0; i < 11; i++) {
			Float currentValue = averagedPrecisions.get(i);
			Float newValue = new Float(currentValue/3.0);
			averagedPrecisions.set(i, newValue);
		}

		return averagedPrecisions;
	}
	
	

	/**
	 * Creates a list of documents in collection.
	 * 
	 * Creates six search engines with different configurations.
	 * 
	 * Executes three searches for every engine.
	 * 
	 * Records 11 step interpolated precision values for every search.
	 * 
	 * Averages 11 step interpolated precision values for every engine.
	 * 
	 * The resulting list is used to plot 11-step interpolated
	 * precision-recall curves.
	 */
	public static void main(String[] args) throws Exception {

		// prepare document collection
		DocumentCollectionParser parser = new DocumentCollectionParser();
		parser.parse(args[0]);
		List<DocumentInCollection> dics = parser.getDocuments();

		// our three search queries
		String[] qs = new String[3];
		qs[0] = "translingual information retrieval";
		qs[1] = "polyglot information searching";
		qs[2] = "cross-language information retrieval";

		// final precision values for curves
		List<List<Float>> finalOriginal = new LinkedList<List<Float>>();
		List<List<Float>> finalInterpolated = new LinkedList<List<Float>>();

		// Engine configurations.
		// First digit = ranking method VSM (1) or BM25 (2)
		// Second digit = stemmer on (1) or off (0)
		// Third digit = stop words on (1) or off (0)
		List<String> engines = new LinkedList<String>();
		engines.addAll(Arrays.asList("111","110","101","211", "210", "201"));

		// compute non-interpolated and interpolated precision values
		// for plotting the curves
		for(String e : engines) {
			List<List<Float>> originalPrecisions = new LinkedList<List<Float>>();
			List<List<Float>> interpolatedPrecisions = new LinkedList<List<Float>>();		
			for (int i = 0; i < 3; i++) {
				Engine engine = new Engine(e);
				SearchResult sr = engine.doSearch(qs[i], dics);
				sr.report();
				originalPrecisions.add(computeOriginalPrecisions(sr));
				interpolatedPrecisions.add(computeInterpolatedPrecisions(sr));
			}
			List<Float> averageOriginalPrecisions = averagePrecisions(originalPrecisions);
			List<Float> averageInterpolatedPrecisions = averagePrecisions(interpolatedPrecisions);
			finalOriginal.add(averageOriginalPrecisions);
			finalInterpolated.add(averageInterpolatedPrecisions);
		}		

		// Print final precision values
		System.out.println("Average precision values (interpolated) for all engines:");
		for(List<Float> r : finalInterpolated) {
			System.out.println(r);
		}


	}

}
