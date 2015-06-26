package trifonov.stanislav.msd;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;


/**
 * Various utility functions
 * @author stan0
 *
 */
public class MSDUtils {
	
	static boolean shouldLog = true;
	
	private interface LineConsumer extends Consumer<String[]>{};

	private static void fileRead(File f, Consumer<String[]> consumer) {
		BufferedReader br = null;
		
		try {
			
			br = Files.newBufferedReader( Paths.get(f.getAbsolutePath()) );
			String line = null;
			
			while ( (line=br.readLine()) != null )
				consumer.accept( line.split("\t") );
			
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
	}
	
	private static Set<String> uniqueUsers(File f) {
		final Set<String> users = new HashSet<String>();
		
		LineConsumer lineConsumer = new LineConsumer() {
			
			public void accept(String[] t) {
				users.add(t[0]);
			}
		};
		
		fileRead(f, lineConsumer);
		
		if(shouldLog)
			System.out.println( users.size() + " unique users found");
		
		return users;
	}
	
	private static Map<String, Integer> usersToIndices(Collection<String> users) {
		Map<String, Integer> usersToIndices = new HashMap<String, Integer>();
		
		int index = 1;
		for (String user : users)
			usersToIndices.put( user, new Integer(index++) );
		
		return usersToIndices;
	}
	
	static Map<String, List<Integer>> songUsers(File f) {
		final Map<String, Set<String>> songUsers = new HashMap<String, Set<String>>();
		
		LineConsumer lineConsumer = new LineConsumer() {
			public void accept(String[] t) {
				//t = [user, song, playCount];
				if(songUsers.containsKey(t[1])) {
					Set<String> users = songUsers.get(t[1]);
					users.add(t[0]);
				}
				else {
					Set<String> users = new HashSet<String>();
					users.add(t[0]);
					songUsers.put(t[1], users);
				}
			}
		};
		
		fileRead(f, lineConsumer);

		
		Map<String, Integer> userIndices = usersToIndices( uniqueUsers(f) );
		Map<String, List<Integer>> result = new HashMap<String, List<Integer>>();
		
		for(Map.Entry<String, Set<String>> entry : songUsers.entrySet()) {
			List<Integer> indices = new ArrayList<Integer>(entry.getValue().size());
			for(String user : entry.getValue())
				indices.add( userIndices.get(user) );
			
			result.put( entry.getKey(), indices );
		}
		
		if(shouldLog)
			System.out.println(result.size() + " songs found");
		
		return result;
	}
}
