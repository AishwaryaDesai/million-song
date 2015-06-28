package trifonov.stanislav.msd;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.mahout.cf.taste.common.Refreshable;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.recommender.AbstractRecommender;
import org.apache.mahout.cf.taste.impl.recommender.GenericRecommendedItem;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.recommender.IDRescorer;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;

public class MSDRandomRecommender extends AbstractRecommender {

	private final List<Integer> _songs;
	
	protected MSDRandomRecommender(DataModel dataModel, List<Integer> songs) {
		super(dataModel);
		_songs = songs;
	}

	@Override
	public List<RecommendedItem> recommend(long userID, int howMany, IDRescorer rescorer, boolean includeKnownItems) throws TasteException {
		List<RecommendedItem> result = new ArrayList<>();
		
		for(int i=0; i<howMany; ++i) {
			int itemID = (int) (Math.random() * (double)_songs.size());
			result.add( new GenericRecommendedItem(itemID, estimatePreference(userID, itemID)) );
		}
		
		return result;
	}

	@Override
	public float estimatePreference(long userID, long itemID) throws TasteException {
		int songPseudoIndex = (int) (Math.random() * (double)_songs.size());
		if(songPseudoIndex < MillionSong.RECOMMENDATIONS_COUNT)
			return 1f;
		else
			return 0f;
	}

	@Override
	public void refresh(Collection<Refreshable> alreadyRefreshed) {

	}

}
