package trifonov.stanislav.msd;

import java.util.ArrayList;
import java.util.List;

class MSDRecommender {
	
	final int RECOMMENDATIONS_COUNT = 500; // number of songs to recommend to a user
	
	private final List<MSDPredictor> _predictors = new ArrayList<MSDPredictor>();
	private final List<String> _allSongs;
	
	
	public MSDRecommender(List<String> allSongs) {
		_allSongs = allSongs;
	}
	
	public void addPredictor(MSDPredictor predictor) {
		_predictors.add(predictor);
	}
	
	

}
