package trifonov.stanislav.msd;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.eval.RecommenderBuilder;
import org.apache.mahout.cf.taste.impl.eval.AbstractDifferenceRecommenderEvaluator;
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;
import org.apache.mahout.cf.taste.impl.recommender.GenericBooleanPrefItemBasedRecommender;
import org.apache.mahout.cf.taste.impl.recommender.ItemAverageRecommender;
import org.apache.mahout.cf.taste.impl.recommender.svd.SVDPlusPlusFactorizer;
import org.apache.mahout.cf.taste.impl.recommender.svd.SVDRecommender;
import org.apache.mahout.cf.taste.impl.similarity.LogLikelihoodSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.TanimotoCoefficientSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.cf.taste.similarity.ItemSimilarity;

public class MillionSong {

	final static String DIRNAME_DATA = "../data";
	final static String DIRNAME_FULL_HISTORY = "../full_history";
	final static String DIRNAME_EVAL_DATA = "../EvalDataYear1MSDWebsite";
	
	final static String FILENAME_USERS = "kaggle_users.txt";
	final static String FILENAME_SONGS = "kaggle_songs.txt";
	final static String FILENAME_VISIBLE_HISTORY = "kaggle_visible_evaluation_triplets.txt";
	final static String FILENAME_VISIBLE_HISTORY_CSV = "kaggle_visible_evaluation_triplets_csv.txt";
	final static String FILENAME_FULL_HISTORY = "train_triplets.txt";
	final static String FILENAME_FULL_HISTORY_CSV = "train_triplets_csv.txt";
	final static String FILENAME_RECOMMENDATIONS_OUTPUT = "rec_output.txt";
	
	final static String FILENAME_EVAL_VALIDATION_VISIBLE = "year1_valid_triplets_visible.txt";
	final static String FILENAME_EVAL_VALIDATION_HIDDEN = "year1_valid_triplets_hidden.txt";
	final static String FILENAME_EVAL_TEST_VISIBLE = "year1_test_triplets_visible.txt";
	final static String FILENAME_EVAL_TEST_HIDDEN = "year1_test_triplets_hidden.txt";
	
	
	final static int RECOMMENDATIONS_COUNT = 500;
	
	
	public static void main(String[] args) throws IOException, TasteException {

		MillionSong ms = new MillionSong();
		ms.prepareData();
		ms.train();
//		ms.recommendAndExport();
		
		ms.testRecommenders();
		
		System.out.println("done");
	}
	
	
	private final File _trainingFile = new File(DIRNAME_FULL_HISTORY, FILENAME_FULL_HISTORY);
	private final File _trainingCSVFile = new File(DIRNAME_FULL_HISTORY, FILENAME_FULL_HISTORY_CSV);
	private final File _usersFile = new File(DIRNAME_DATA, FILENAME_USERS);
	private final File _songsFile = new File(DIRNAME_DATA, FILENAME_SONGS);
	private final File _evaluationFile = new File(DIRNAME_DATA, FILENAME_VISIBLE_HISTORY);
	private final File _evaluationCSVFile = new File(DIRNAME_DATA, FILENAME_VISIBLE_HISTORY_CSV);
	private final File _recommendationsOutputFile = new File(DIRNAME_DATA, FILENAME_RECOMMENDATIONS_OUTPUT);
	private final File _evaluationTestHidden = new File(DIRNAME_EVAL_DATA, FILENAME_EVAL_TEST_HIDDEN);
	private final File _evaluationTestVisbile = new File(DIRNAME_EVAL_DATA, FILENAME_EVAL_TEST_VISIBLE);
	private final File _evaluationValidationHidden = new File(DIRNAME_EVAL_DATA, FILENAME_EVAL_VALIDATION_HIDDEN);
	private final File _evaluationValidationVisible = new File(DIRNAME_EVAL_DATA, FILENAME_EVAL_VALIDATION_VISIBLE);
	
	private Map<String, Integer> _songIndices;
	private Map<String, Integer> _evaluationUserIndices;
	private List<Integer> _popularSongs;
	private Map<Integer, List<Integer>> _userSongs;
	private Map<Integer, List<Integer>> _songUsersTraining;
	
	
	public MillionSong() {
		
	}
	
	public void prepareData() throws IOException {
		System.out.println("processing data...");

		_songIndices = MSDUtils.songIndices(_songsFile);
		_evaluationUserIndices = MSDUtils.uniqueUsers(_usersFile);
		
		if( !_trainingCSVFile.exists() )
			MSDUtils.exportToCSV( _trainingFile, _trainingCSVFile, MSDUtils.uniqueUsers(_trainingFile), _songIndices );
		if( !_evaluationCSVFile.exists() )
			MSDUtils.exportToCSV( _evaluationFile, _evaluationCSVFile, _evaluationUserIndices, _songIndices );

		System.out.println("data is ready");
	}
	
	
	public void train() {
		System.out.println("training...");
		long start = System.currentTimeMillis();
		_popularSongs = MSDUtils.songByPopularityFromCSV(_trainingCSVFile);
		_songUsersTraining = MSDUtils.songUsersFromCSV(_trainingCSVFile);
		_userSongs = MSDUtils.userSongs( _evaluationValidationHidden, _evaluationUserIndices, _songIndices);
		long end = System.currentTimeMillis();
		System.out.println("Training took " + (end-start) + "ms.");
	}
	
	
	public void testRecommenders() throws IOException, TasteException {
		final DataModel dm = new FileDataModel(_evaluationCSVFile);
		
		Map<String, RecommenderBuilder> recommenderBuilders = new HashMap<>();
		
		recommenderBuilders.put("PopRec", new RecommenderBuilder() {
			@Override
			public Recommender buildRecommender(DataModel dataModel) throws TasteException {
				return new MSDRecommender(dm, _popularSongs);
			}
		});
		
//		recommenderBuilders.put("AvgRec", new RecommenderBuilder() {
//			@Override
//			public Recommender buildRecommender(DataModel dataModel) throws TasteException {
//				return new ItemAverageRecommender(dm);
//			}
//		});
		
//		recommenderBuilders.put("SVDRec", new RecommenderBuilder() {
//			@Override
//			public Recommender buildRecommender(DataModel dataModel) throws TasteException {
//				return new SVDRecommender(dm, new SVDPlusPlusFactorizer(dm, 10, 5));
//			}
//		});

		recommenderBuilders.put("ItemBoolLog", new RecommenderBuilder() {
			@Override
			public Recommender buildRecommender(DataModel dataModel) throws TasteException {
				return new GenericBooleanPrefItemBasedRecommender(dm, new TanimotoCoefficientSimilarity(dm));
			}
		});
		
		
		double trainingPercentage = 0.9;
		double testingPercentage = 0.1;
		
		AbstractDifferenceRecommenderEvaluator recEvaluator = new MeanAveragePrecisionEvaluator(_userSongs);
		
		/*
		 * Using AverageAbsoluteDifferenceRecommenderEvaluator:
		 *  SVDRec recommender scored 0.462 in 45.755 s
		 *  ItemBoolLog recommender scored 0.000 in 0.075 s
		 *  AvgRec recommender scored 0.000 in 3.568 s
		 *  PopRec recommender scored 0.000 in 0.060 s
		 *  
		 * Using RMSRecommenderEvaluator
		 *  SVDRec recommender scored 0.628 in 21.490 s
		 *  ItemBoolLog recommender scored 0.000 in 0.088 s
		 *  AvgRec recommender scored 0.000 in 0.155 s
		 *  PopRec recommender scored 0.000 in 0.069 s
		 */
		
		String logFormat = "%s recommender scored %f in %.3f s";
		for(Map.Entry<String, RecommenderBuilder> entry : recommenderBuilders.entrySet()) {
			long start = System.currentTimeMillis();
			String name = entry.getKey();
			double score = recEvaluator.evaluate(entry.getValue(), null, dm, trainingPercentage, testingPercentage);
			long end = System.currentTimeMillis();
			double time = (end-start) / 1000.0;
			
			System.out.println(String.format(logFormat, name, score, time));
		}
	}
	
	
	
	public void recommendAndExport() throws IOException, TasteException {
		//recommend and export
		long start = System.currentTimeMillis();
		DataModel dataModel = new FileDataModel(_evaluationCSVFile);
		ItemSimilarity itemSimilarity = new ConditionalProbabilityItemSimilarity(_songUsersTraining);
		Recommender cachingRecommender = new MSDRecommender(dataModel, _popularSongs);// CachingRecommender( new GenericItemBasedRecommender(dataModel, itemSimilarity) );
		
		Collection<Integer> values = _evaluationUserIndices.values();
		Integer[] users = values.toArray( new Integer[values.size()] );
		Arrays.sort(users);

		Set<OpenOption> options = new HashSet<OpenOption>();
	    options.add(StandardOpenOption.APPEND);
	    options.add(StandardOpenOption.CREATE);
		
	    if(_recommendationsOutputFile.exists())
	    	_recommendationsOutputFile.delete();
	    
	    final SeekableByteChannel sbc = Files.newByteChannel( Paths.get(_recommendationsOutputFile.getAbsolutePath()), options );
	    
		start = System.currentTimeMillis();
		for(Integer user : users) {
			if(user % 1000 == 0)
				System.out.println(
						String.format(
								"recommending for %d users took %.2f s.", 
								user,
								(System.currentTimeMillis()-start)/1000.0 ));
			List<RecommendedItem> userRecommendations = cachingRecommender.recommend(user, RECOMMENDATIONS_COUNT, false);
			List<Integer> recommendedItems = new ArrayList<>(userRecommendations.size());
			StringBuilder lineBuider = new StringBuilder();
			
			for(int i=0; i<userRecommendations.size(); ++i) {
				RecommendedItem item = userRecommendations.get(i);
				recommendedItems.add( (int)item.getItemID() );
				lineBuider.append( item.getItemID() );
				if( i < userRecommendations.size() - 1 )
					lineBuider.append(' ');
			}
			
//					if(userRecommendations.size() < RECOMMENDATIONS_COUNT) {
//						for(Integer popularSong : popularSongs) {
//							if( !recommendedItems.contains(popularSong) ) {
//								recommendedItems.add(popularSong);
//								lineBuider.append( popularSong );
//								if(recommendedItems.size() >= RECOMMENDATIONS_COUNT)
//									break;
//								else
//									lineBuider.append(' ');
//							}
//						}
//					}
			
			lineBuider.append('\n');
			
			byte data[] = lineBuider.toString().getBytes();
		    ByteBuffer bb = ByteBuffer.wrap(data);
			sbc.write(bb);
		}
		
		sbc.close();
		
		if(MSDUtils.shouldLog)
			System.out.println("Recommending took " + ((System.currentTimeMillis()-start)) + "ms.");
	}
}
