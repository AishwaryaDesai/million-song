package trifonov.stanislav.msd;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.mahout.cf.taste.common.Refreshable;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.recommender.AbstractRecommender;
import org.apache.mahout.cf.taste.impl.recommender.GenericRecommendedItem;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.recommender.IDRescorer;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;

class MSDPopularRecommender extends AbstractRecommender {
	
	private final List<Integer> _popularSongs;
	private final Map<Integer, Integer> _popularSongIndices;
	

	protected MSDPopularRecommender(DataModel dataModel, List<Integer> popularSongs) {
		super(dataModel);
		_popularSongs = popularSongs;
		_popularSongIndices = new HashMap<>(_popularSongs.size());
		
		for(int index=0; index < _popularSongs.size(); ++index)
			_popularSongIndices.put( _popularSongs.get(index), index );
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
		Integer songIndex = _popularSongIndices.get((int)itemID);
		if( songIndex == null || songIndex.intValue() >= MillionSong.RECOMMENDATIONS_COUNT)
			return 0f;
		
		return 1f;
	}

	@Override
	public void refresh(Collection<Refreshable> alreadyRefreshed) {
		
	}
}
