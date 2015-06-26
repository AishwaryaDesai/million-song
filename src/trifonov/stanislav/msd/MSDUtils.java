package trifonov.stanislav.msd;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;


/**
 * Various utility functions
 * @author stan0
 *
 */
public class MSDUtils {
	
	static boolean shouldLog = true;
	
	static final long ONE_GB = 1024 * 1024 * 1024;
	
	private interface LineConsumer extends Consumer<String[]>{};

	private static void fileRead(File f, Consumer<String[]> consumer) {
		FileInputStream is = null;
		
		try {
			
			is = new FileInputStream(f);
			FileChannel channel = is.getChannel();
			MappedByteBuffer buffer = channel.map(MapMode.READ_ONLY, 0, Math.min(channel.size(), Integer.MAX_VALUE));
			buffer.order(ByteOrder.LITTLE_ENDIAN);
			int bufferCount = 1;
			
			StringBuilder lineBuilder = new StringBuilder();
			String line = null;
			String[] columns = null;
//			int linesCount = 0;
			char c;
			
			
			while(buffer.hasRemaining()) {
				lineBuilder.setLength(0);
				c = (char) buffer.get();
				while (c != '\n') {
					lineBuilder.append(c);
					c = (char) buffer.get();
				}
				
				line = lineBuilder.toString();
				
//				if(shouldLog) {
//					++linesCount;
//					if(linesCount % 5000000l == 0)
//						System.out.println(linesCount + " lines read");
//				}
				
				columns = line.split("\t");
				consumer.accept( columns );
				
				if(buffer.position() > ONE_GB) {
					int newPosition = (int)(buffer.position() - ONE_GB);
					long size = Math.min(channel.size() - ONE_GB*bufferCount, Integer.MAX_VALUE);
//					System.out.println(
//							String.format(
//									"Mapping %.2f GB at position %d",
//									size / (double)ONE_GB,
//									ONE_GB*bufferCount));
					
					buffer = channel.map(MapMode.READ_ONLY, ONE_GB*bufferCount, size);
					buffer.order(ByteOrder.LITTLE_ENDIAN);
					buffer.position(newPosition);
					bufferCount += 1;
				}
			}
			
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
	}
	
	
	public static Map<String, Integer> songIndices(File f) {
		final Map<String, Integer> songIndices = new HashMap<String, Integer>();
		
		fileRead(f, new LineConsumer() {
			public void accept(String[] columns) {
				String[] tokens = columns[0].split(" ");
				songIndices.put( tokens[0], Integer.valueOf(tokens[1]) );
			}
		});
		
		if(shouldLog)
			System.out.println(songIndices.size() + " uniques songs found");
		
		return songIndices;
	}
	
	private static Map<String, Integer> uniqueUsers(File f) {
		final Map<String, Integer> usersToIndices = new HashMap<String, Integer>();
		
		fileRead(f, new LineConsumer() {
			public void accept(String[] columns) {
				usersToIndices.put( columns[0], usersToIndices.size()+1);
			}
		});
		if(shouldLog)
			System.out.println( usersToIndices.size() + " unique users found");
		
		return usersToIndices;
	}
	
	static Map<Integer, List<Integer>> songUsers(File f, final Map<String, Integer> songIndices) {
		final Map<String, Integer> userIndices = uniqueUsers(f);
		
		final Map<Integer, List<Integer>> songUsers = new HashMap<Integer, List<Integer>>();
		
		LineConsumer lineConsumer = new LineConsumer() {
			public void accept(String[] columns) {
				//t = [user, song, playCount];
				int userIndex = userIndices.get(columns[0]);
				int songIndex = songIndices.get(columns[1]);
				List<Integer> users = songUsers.get(songIndex);
				if(users == null) {
					users = new ArrayList<Integer>();
					songUsers.put(songIndex, users);
				}
				users.add(userIndex);
			}
		};
		
		fileRead(f, lineConsumer);

		if(shouldLog) {
			int minUsers = Integer.MAX_VALUE;
			int maxUsers = Integer.MIN_VALUE;
			long totalUsers = 0;
			for(Map.Entry<Integer, List<Integer>> entry : songUsers.entrySet()) {
				int listenersCount = entry.getValue().size();
				if(  listenersCount < minUsers)
					minUsers = listenersCount;
				if( listenersCount > maxUsers)
					maxUsers = listenersCount;
				totalUsers += listenersCount;
			}
			
			System.out.println(
					String.format(
							"%d songs found. minUsers: %d maxUsers: %d avgUsers: %.3f",
							songUsers.size(),
							minUsers, maxUsers, (totalUsers/(double)songUsers.size()) ));
		}
		
		return songUsers;
	}
}
