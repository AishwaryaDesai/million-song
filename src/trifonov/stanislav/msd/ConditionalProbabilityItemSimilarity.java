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
	
	private final Map<Integer, List<Integer>> _songUsers;
	public ConditionalProbabilityItemSimilarity(Map<Integer, List<Integer>> songUsers) {
		_songUsers = songUsers;
	}
	
	public void refresh(Collection<Refreshable> alreadyRefreshed) {
		
	}

	public double itemSimilarity(long itemID1, long itemID2) throws TasteException {
		int song1Index = (int)itemID1;
		int song2Index = (int)itemID2;
		List<Integer> item1UserList = _songUsers.get( Integer.valueOf(song1Index) );
		List<Integer> item2UserList = _songUsers.get( Integer.valueOf(song2Index) );
		if(item1UserList == null || item2UserList == null)
			return 0.0;
		
		Set<Integer> item1Users = new HashSet<Integer>( item1UserList );
		Set<Integer> item2Users = new HashSet<Integer>( item2UserList );
		Set<Integer> commonItems = new HashSet<Integer>();
		for(Integer user : item1Users)
			if(item2Users.contains(user))
				commonItems.add(user);
		
		
		double value = commonItems.size() /
				( Math.pow(item1Users.size(), ALPHA) * Math.pow(item2Users.size(), (1-ALPHA)) );
		value = value * 2 - 1;// from [0,1] to [-1, 1] for the purpose of ItemItemSimilarity
		value = Math.max(value, -1.0);
		value = Math.min(value, 1.0);
		
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
