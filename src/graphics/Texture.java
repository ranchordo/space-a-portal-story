package graphics;

import java.io.FileNotFoundException;

import logger.Logger;
import util.ImageUtil;

public class Texture {
	//Actual textures!
	public static final int COLOR=0;
	public static final int BUMP=1;
	public static final int NORMAL=2;
	
	public String name="";
	public boolean colorLoaded=false;
	public boolean bumpLoaded=false;
	public boolean normLoaded=false;
	TextureImage color; //Binding 0
	TextureImage bump; //Binding 1
	TextureImage norm; //Binding 2
	public Texture(Texture in) {
		name=in.name;
		colorLoaded=in.colorLoaded;
		bumpLoaded=in.bumpLoaded;
		normLoaded=in.normLoaded;
		color=in.color;
		bump=in.bump;
		norm=in.norm;
	}
	public Texture() {
		color=new TextureImage(0);
		bump=new TextureImage(1);
		norm=new TextureImage(2);
	}
	public TextureImage get(int id) {
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
	public void reset(int id) {
		switch(id) {
		case COLOR:
			color=new TextureImage(0);
		case BUMP:
			bump=new TextureImage(1);
		case NORMAL:
			norm=new TextureImage(2);
		}
	}
	public void create_color(String fname) throws FileNotFoundException {
		color.create(ImageUtil.getImage(fname));
	}
	public void create_bump(String fname) throws FileNotFoundException {
		bump.create(ImageUtil.getImage(fname));
		bumpLoaded=true;
		Logger.log(0,"Found bumpmap "+fname);
	}
	public void create_norm(String fname) throws FileNotFoundException {
		norm.create(ImageUtil.getImage(fname));
		normLoaded=true;
		Logger.log(0,"Found normal map "+fname);
	}
//	private void upload() {
//		color.upload();
//		if(bumpLoaded && normLoaded) {norm.upload();}// bump.upload();}
//	}
	public void bind() {
		color.bind();
		if(bumpLoaded && normLoaded) {norm.bind();}// bump.bind();}
	}
}
