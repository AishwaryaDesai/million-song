package trifonov.stanislav.msd;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
			int linesCount = 0;
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
	
	public static Map<String, Integer> uniqueUsers(File f) {
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
	
	static Map<Integer, List<Integer>> songUsersFromCSV(File csvFile) {
		final Map<Integer, List<Integer>> songUsers = new HashMap<Integer, List<Integer>>();
		
		fileRead(csvFile, new LineConsumer() {
			public void accept(String[] columns) {
				String[] values = columns[0].split(",");
				Integer userIndex = Integer.valueOf(values[0]);
				Integer songIndex = Integer.valueOf(values[1]);
				List<Integer> users = songUsers.get(songIndex);
				if(users == null) {
					users = new ArrayList<Integer>();
					songUsers.put(songIndex, users);
				}
				users.add(userIndex);
			}
		});
		
		return songUsers;
	}
	
	static List<Integer> songByPopularityFromCSV(File csvFile) {
		final Map<Integer, Integer> songPlayCounts = new HashMap<Integer, Integer>();
		
		fileRead(csvFile, new LineConsumer() {
			public void accept(String[] arg0) {
				String[] values = arg0[0].split(",");
				Integer song = Integer.valueOf(values[1]);
				Integer playCount = songPlayCounts.get(song);
				if(playCount == null)
					playCount = Integer.valueOf(0);
				
				songPlayCounts.put(song, playCount+1);
			}
		});
		
		List<Integer> songs = new ArrayList<Integer>( songPlayCounts.keySet() );
		Collections.sort(songs, new Comparator<Integer>() {
			public int compare(Integer arg0, Integer arg1) {
				return songPlayCounts.get(arg0) - songPlayCounts.get(arg1);
			};
		});
		
		return songs;
	}
	

	public static void exportToCSV(File in, File out, final Map<String, Integer> userIndices, final Map<String, Integer> songIndices) throws IOException {
		Set<OpenOption> options = new HashSet<OpenOption>();
	    options.add(StandardOpenOption.APPEND);
	    options.add(StandardOpenOption.CREATE);
		
	    final SeekableByteChannel sbc = Files.newByteChannel(Paths.get(out.getAbsolutePath()), options);
	        
		fileRead(in, new LineConsumer() {
			public void accept(String[] columns) {
				Integer userIndex = userIndices.get(columns[0]);
				Integer songIndex = songIndices.get(columns[1]);
				
				String line = 
						userIndex
						+ "," + songIndex
						+ "," + 1
						+ '\n';
				byte data[] = line.getBytes();
			    ByteBuffer bb = ByteBuffer.wrap(data);
			    try {
					sbc.write(bb);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		
		sbc.close();
	}
}
