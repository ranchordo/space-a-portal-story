package graphics;

import java.io.FileNotFoundException;
import java.util.ArrayList;

import logger.Logger;
import util.ImageUtil;

public class TextureArray {
	//Actual textures!
	public static final int COLOR=0;
	public static final int BUMP=1;
	public static final int NORMAL=2;
	
	public String name="";
	public ArrayList<Boolean> colorLoaded=new ArrayList<Boolean>();
	public ArrayList<Boolean> bumpLoaded=new ArrayList<Boolean>();
	public ArrayList<Boolean> normLoaded=new ArrayList<Boolean>();
	TextureImageArray color; //Binding 0
	TextureImageArray bump; //Binding 1
	TextureImageArray norm; //Binding 2
	public TextureArray(TextureArray in) {
		name=in.name;
		colorLoaded=in.colorLoaded;
		bumpLoaded=in.bumpLoaded;
		normLoaded=in.normLoaded;
		color=in.color;
		bump=in.bump;
		norm=in.norm;
	}
	public TextureArray() {
		color=new TextureImageArray(0);
		bump=new TextureImageArray(1);
		norm=new TextureImageArray(2);
	}
	public TextureImageArray get(int id) {
		switch(id) {
		case COLOR:
			return color;
		case BUMP:
			return bump;
		case NORMAL:
			return norm;
		default:
			return null;
		}
	}
	private void reset(int id) {
		switch(id) {
		case COLOR:
			color=new TextureImageArray(0);
		case BUMP:
			bump=new TextureImageArray(1);
		case NORMAL:
			norm=new TextureImageArray(2);
		}
	}
	public void runCheck() {
		if(color.depth!=bump.depth || bump.depth!=norm.depth || norm.depth!=colorLoaded.size() || colorLoaded.size()!=bumpLoaded.size() || bumpLoaded.size()!=normLoaded.size()) {
			Logger.log(3,String.valueOf(color.depth));
			Logger.log(3,String.valueOf(bump.depth));
			Logger.log(3,String.valueOf(norm.depth));
			Logger.log(3,String.valueOf(colorLoaded.size()));
			Logger.log(3,String.valueOf(bumpLoaded.size()));
			Logger.log(3,String.valueOf(normLoaded.size()));
			
			throw new AssertionError("Unequal texture array sizes.");
		}
	}
	public int create_color(String fname) throws FileNotFoundException {
		if(fname!=null) {color.addSubImage(ImageUtil.getImage(fname));}
		else {color.addNullSubImage();}
		colorLoaded.add(fname!=null);
		return color.depth-1;
	}
	public int create_bump(String fname) throws FileNotFoundException {
		if(fname!=null) {bump.addSubImage(ImageUtil.getImage(fname));}
		else {bump.addNullSubImage();}
		bumpLoaded.add(fname!=null);
		Logger.log(0,"Found bumpmap "+fname);
		return bump.depth-1;
	}
	public int create_norm(String fname) throws FileNotFoundException {
		if(fname!=null) {norm.addSubImage(ImageUtil.getImage(fname));}
		else {norm.addNullSubImage();}
		normLoaded.add(fname!=null);
		Logger.log(0,"Found normal map "+fname);
		return norm.depth-1;
	}
//	public void upload() {
//		color.upload();
//		if(bumpLoaded && normLoaded) {norm.upload();}// bump.upload();}
//	}
	public void bind() {
		color.bind();
		norm.bind();
	}
}
