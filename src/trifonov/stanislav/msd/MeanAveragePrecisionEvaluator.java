package trifonov.stanislav.msd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.mahout.cf.taste.impl.eval.AbstractDifferenceRecommenderEvaluator;
import org.apache.mahout.cf.taste.model.Preference;

public class MeanAveragePrecisionEvaluator extends AbstractDifferenceRecommenderEvaluator {

	private final Map<Integer, List<Integer>> _userSongs;
	private final Map<Integer, List<Integer>> _userRecommendations = new HashMap<>();
	
	public MeanAveragePrecisionEvaluator(Map<Integer, List<Integer>> userSongs) {
		_userSongs = userSongs;
	}
	
	@Override
	protected void reset() {
		_userRecommendations.clear();
	}

	@Override
	protected void processOneEstimate(float estimatedPreference, Preference realPref) {

		Integer user = (int)realPref.getUserID();
		Integer item = (int)realPref.getItemID();
		
		List<Integer> recommendations = _userRecommendations.get(user);
		if(recommendations == null) {
			recommendations = new ArrayList<>( MillionSong.RECOMMENDATIONS_COUNT );
			_userRecommendations.put(user, recommendations);
		}
		if(recommendations.size() <= MillionSong.RECOMMENDATIONS_COUNT)
			recommendations.add(item);
	}

	// Check the winner's document for the complete formula
	@Override
	protected double computeFinalEvaluation() {
		double mAP = 0;
		
		for(Map.Entry<Integer, List<Integer>> userEntry : _userSongs.entrySet()) {
			List<Integer> userSongs = _userSongs.get(userEntry.getKey());
			if(userSongs == null || userSongs.size()==0)
				continue;
			
			double AP = 0;
			
			List<Integer> recommendations = _userRecommendations.get(userEntry.getKey());
			if(recommendations != null) {
				for(int k=0; k<recommendations.size(); ++k) {
					if(k >= MillionSong.RECOMMENDATIONS_COUNT)
						break;
					
					double pk = 0;
					for(int p=0; p<=k; ++p) {
						int yp = recommendations.get(p);
						if(userSongs.contains(yp))
							pk += 1.0;
					}
					pk /= (double) (k+1);
					AP += pk * (userSongs.contains(recommendations.get(k)) ? 1.0 : 0.0);
				}
			}
			
			AP /= Math.min(userSongs.size(), MillionSong.RECOMMENDATIONS_COUNT);//number of relevant songs
			mAP += AP;
		}
		
		return mAP / (double)_userSongs.size();
	}

}
