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

class MSDRecommender extends AbstractRecommender {
	
	private final List<Integer> _popularSongs;
	

	protected MSDRecommender(DataModel dataModel, List<Integer> popularSongs) {
		super(dataModel);
		_popularSongs = popularSongs;
	}
	
	@Override
	public List<RecommendedItem> recommend(long userID, int howMany, IDRescorer rescorer, boolean includeKnownItems) throws TasteException {
		List<RecommendedItem> items = new ArrayList<>();
		for(int i=0; i<howMany; ++i) {
			int itemId = _popularSongs.get(i);
			items.add( new GenericRecommendedItem( itemId, estimatePreference(userID, itemId)) );
		}
		
		return items;
	}

	@Override
	public float estimatePreference(long userID, long itemID) throws TasteException {
		if(_popularSongs.indexOf((int)itemID) < MillionSong.RECOMMENDATIONS_COUNT)
			return 1;
		
		return 0;
	}

	@Override
	public void refresh(Collection<Refreshable> alreadyRefreshed) {
		
	}
}
