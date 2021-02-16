package util;

import java.awt.image.BufferedImage;
import java.util.HashMap;

import logger.Logger;

public class PrefetchedImages {
	//This is basically just a convenient String,BufferedImage HashMap wrapper
	HashMap<String, BufferedImage> images=new HashMap<String,BufferedImage>();
	public PrefetchedImages() {}
	public void add(String key, BufferedImage img) {
		images.put(key,img);
		Logger.log(0,"PrefetchedImages BuIm add routine: "+key);
	}
	public void add(String key, String fname) {
		images.put(key,ImageUtil.getImage_handleNotFound(fname));
		Logger.log(0,"PrefetchedImages File add routine: "+key+", "+fname);
	}
	public void replace(String key, BufferedImage img) {
		images.remove(key);
		this.add(key,img);
	}
	public BufferedImage get(String key) {
		return images.get(key);
	}
}
