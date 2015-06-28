package trifonov.stanislav.msd;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.mahout.cf.taste.common.Refreshable;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.similarity.ItemSimilarity;

public class ConditionalProbabilityItemSimilarity implements ItemSimilarity {

	private static final double ALPHA = 0.15;
	private List<Integer> _item1UserList;
	private List<Integer> _item2UserList;
	private Set<Integer> _item1UserSet = new HashSet<Integer>();
	private Set<Integer> _item2UserSet = new HashSet<Integer>();;
	private Set<Integer> _commonItemSet = new HashSet<Integer>();
	private int _song1Index;
	private int _song2Index;
	
	
	private final Map<Integer, List<Integer>> _songUsers;
	public ConditionalProbabilityItemSimilarity(Map<Integer, List<Integer>> songUsers) {
		_songUsers = songUsers;
	}
	
	public void refresh(Collection<Refreshable> alreadyRefreshed) {
		
	}

	public double itemSimilarity(long itemID1, long itemID2) throws TasteException {
		_song1Index = (int)itemID1;
		_song2Index = (int)itemID2;
		_item1UserList = _songUsers.get( Integer.valueOf(_song1Index) );
		_item2UserList = _songUsers.get( Integer.valueOf(_song2Index) );
		if(_item1UserList == null || _item2UserList == null)
			return -1.0;
		
		_item1UserSet.addAll(_item1UserList);
		_item2UserSet.addAll(_item2UserList);
		for(Integer user : _item1UserSet)
			if(_item2UserSet.contains(user))
				_commonItemSet.add(user);
		
		
		double value = _commonItemSet.size() /
				( Math.pow(_item1UserSet.size(), ALPHA) * Math.pow(_item2UserSet.size(), (1-ALPHA)) );
		value = value * 2 - 1;// from [0,1] to [-1, 1] for the purpose of ItemItemSimilarity
		value = Math.max(value, -1.0);
		value = Math.min(value, 1.0);

		//important
		_item1UserSet.clear();
		_item2UserSet.clear();
		_commonItemSet.clear();
		
		return value;
	}

	public double[] itemSimilarities(long itemID1, long[] itemID2s) throws TasteException {
		double[] values = new double[itemID2s.length];
		for(int i=0; i<values.length; ++i)
			values[i] = itemSimilarity(itemID1, itemID2s[i]);
		
		return values;
	}

	public long[] allSimilarItemIDs(long itemID) throws TasteException {
		return null;
	}

}
