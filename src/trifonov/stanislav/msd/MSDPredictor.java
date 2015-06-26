package trifonov.stanislav.msd;

import java.util.List;

abstract class MSDPredictor {

	abstract public void score(List<String> userSongs, List<String> allSongs);
}
