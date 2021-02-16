package audio;

import java.util.HashMap;

public class Soundtrack {
	public HashMap<String, Sound> sounds=new HashMap<String, Sound>();
	public void put(String key, String fname) {
		Sound s=new Sound();
		s.getFile(fname);
		sounds.put(key,s);
	}
	public Sound get(String key) {
		return sounds.get(key);
	}
}