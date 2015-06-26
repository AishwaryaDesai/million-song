package trifonov.stanislav.msd;

import java.io.File;
import java.util.List;
import java.util.Map;

public class MillionSong {

	final static String DIRNAME_DATA = "../data";
	final static String DIRNAME_FULL_HISTORY = "/Volumes/storage";
	
	final static String FILENAME_USERS = "kaggle_users.txt";
	final static String FILENAME_SONGS = "kaggle_songs.txt";
	final static String FILENAME_VISIBLE_HISTORY = "kaggle_visible_evaluation_triplets.txt";
	final static String FILENAME_FULL_HISTORY = "train_triplets.txt";
	
	public static void main(String[] args) {

		File trainingFile = new File(DIRNAME_FULL_HISTORY, FILENAME_FULL_HISTORY);
		File usersFile = new File(DIRNAME_DATA, FILENAME_USERS);
		File songsFile = new File(DIRNAME_DATA, FILENAME_SONGS);
		File evaluationFile = new File(DIRNAME_DATA, FILENAME_VISIBLE_HISTORY);
		
		System.out.println("loading training data ");
		Map<String, List<Integer>> songUsersTraining = MSDUtils.songUsers(trainingFile);//TODO use the training file
		
		
		System.out.print("done");
	}

}
