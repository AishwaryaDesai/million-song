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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;
import org.apache.mahout.cf.taste.impl.recommender.CachingRecommender;
import org.apache.mahout.cf.taste.impl.recommender.GenericBooleanPrefItemBasedRecommender;
import org.apache.mahout.cf.taste.impl.recommender.GenericItemBasedRecommender;
import org.apache.mahout.cf.taste.impl.recommender.RandomRecommender;
import org.apache.mahout.cf.taste.impl.similarity.GenericItemSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.PearsonCorrelationSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.UncenteredCosineSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.GenericItemSimilarity.ItemItemSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.cf.taste.similarity.ItemSimilarity;

public class MillionSong {

	final static String DIRNAME_DATA = "../data";
	final static String DIRNAME_FULL_HISTORY = "../full_history";
	
	final static String FILENAME_USERS = "kaggle_users.txt";
	final static String FILENAME_SONGS = "kaggle_songs.txt";
	final static String FILENAME_VISIBLE_HISTORY = "kaggle_visible_evaluation_triplets.txt";
	final static String FILENAME_VISIBLE_HISTORY_CSV = "kaggle_visible_evaluation_triplets_csv.txt";
	final static String FILENAME_FULL_HISTORY = "train_triplets.txt";
	final static String FILENAME_FULL_HISTORY_CSV = "train_triplets_csv.txt";
	final static String FILENAME_RECOMMENDATIONS_OUTPUT = "rec_output.txt";
	
	final static int RECOMMENDATIONS_COUNT = 500;
	
	
	public static void main(String[] args) throws IOException, TasteException {

		File trainingFile = new File(DIRNAME_FULL_HISTORY, FILENAME_FULL_HISTORY);
		File trainingCSVFile = new File(DIRNAME_FULL_HISTORY, FILENAME_FULL_HISTORY_CSV);
		File usersFile = new File(DIRNAME_DATA, FILENAME_USERS);
		File songsFile = new File(DIRNAME_DATA, FILENAME_SONGS);
		File evaluationFile = new File(DIRNAME_DATA, FILENAME_VISIBLE_HISTORY);
		File evaluationCSVFile = new File(DIRNAME_DATA, FILENAME_VISIBLE_HISTORY_CSV);
		File recommendationsOutputFile = new File(DIRNAME_DATA, FILENAME_RECOMMENDATIONS_OUTPUT);
		

		System.out.println("processing data...");

		Map<String, Integer> songIndices = MSDUtils.songIndices(songsFile);
		Map<String, Integer> evaluationUsers = MSDUtils.uniqueUsers(usersFile);
		
		if( !trainingCSVFile.exists() )
			MSDUtils.exportToCSV( trainingFile, trainingCSVFile, MSDUtils.uniqueUsers(trainingFile), songIndices );
		if( !evaluationCSVFile.exists() )
			MSDUtils.exportToCSV( evaluationFile, evaluationCSVFile, evaluationUsers, songIndices );

		System.out.println("data is ready");

		
		//train
		System.out.println("training...");
		long start = System.currentTimeMillis();
		DataModel dataModel = new FileDataModel(evaluationCSVFile);
		List<Integer> popularSongs = MSDUtils.songByPopularityFromCSV(trainingCSVFile);
		Map<Integer, List<Integer>> songUsersTraining = MSDUtils.songUsersFromCSV(trainingCSVFile);
		ItemSimilarity itemSimilarity = new ConditionalProbabilityItemSimilarity(songUsersTraining);
		Recommender cachingRecommender = new MSDRecommender(dataModel, popularSongs);// CachingRecommender( new GenericItemBasedRecommender(dataModel, itemSimilarity) );

		long end = System.currentTimeMillis();
		System.out.println("Training took " + (end-start) + "ms.");
		
		
		
		//recommend and export
		Collection<Integer> values = evaluationUsers.values();
		Integer[] users = values.toArray( new Integer[values.size()] );
		Arrays.sort(users);

		Set<OpenOption> options = new HashSet<OpenOption>();
	    options.add(StandardOpenOption.APPEND);
	    options.add(StandardOpenOption.CREATE);
		
	    if(recommendationsOutputFile.exists())
	    	recommendationsOutputFile.delete();
	    
	    final SeekableByteChannel sbc = Files.newByteChannel( Paths.get(recommendationsOutputFile.getAbsolutePath()), options );
	    
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
			
//			if(userRecommendations.size() < RECOMMENDATIONS_COUNT) {
//				for(Integer popularSong : popularSongs) {
//					if( !recommendedItems.contains(popularSong) ) {
//						recommendedItems.add(popularSong);
//						lineBuider.append( popularSong );
//						if(recommendedItems.size() >= RECOMMENDATIONS_COUNT)
//							break;
//						else
//							lineBuider.append(' ');
//					}
//				}
//			}
			
			lineBuider.append('\n');
			
			byte data[] = lineBuider.toString().getBytes();
		    ByteBuffer bb = ByteBuffer.wrap(data);
			sbc.write(bb);
		}
		
		sbc.close();
		
		if(MSDUtils.shouldLog)
			System.out.println("Recommending took " + ((System.currentTimeMillis()-start)) + "ms.");
		
		
		if(MSDUtils.shouldLog)
			System.out.println("done");
	}
	
}
