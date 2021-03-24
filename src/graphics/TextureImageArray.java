package graphics;

import static org.lwjgl.opengl.GL46.*;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;

public class TextureImageArray {
	//Backend openGL texture image array thing.
	private int id;
	public int binding;
	public int width;
	public int height;
	public int depth;
	private ByteBuffer pixels;
	
	public TextureImageArray(int b) {
		id=glGenTextures();
		binding=b;
		
		width=0;
		height=0;
		depth=0;
	}
	public void addNullSubImage() {
		depth++;
		ByteBuffer newPixels=BufferUtils.createByteBuffer(width*depth*height*4);
		for(int i=0;i<pixels.capacity();i++) {
			newPixels.put(pixels.get(i));
		}
		for(int i=0;i<width;i++) {
			for(int j=0;j<height;j++) {
				pixels.put((byte)0);
				pixels.put((byte)0);
				pixels.put((byte)0);
				pixels.put((byte)0);
			}
		}
	}
	public void addSubImage(BufferedImage bi) { //Convert the internal BI to a bytebuffer and upload
		width=Math.max(bi.getWidth(),width);
		height=Math.max(bi.getHeight(),height);
		depth++;
		int[] pixels_raw=new int[width*height*4];
		pixels_raw=bi.getRGB(0, 0, bi.getWidth(), bi.getHeight(), null, 0, width);
		ByteBuffer newPixels=BufferUtils.createByteBuffer(width*depth*height*4);
		for(int i=0;i<pixels.capacity();i++) {
			newPixels.put(pixels.get(i));
		}
		
		for(int i=0;i<width;i++) {
			for(int j=0;j<height;j++) {
				int pixel=(j>bi.getHeight() || i>bi.getWidth()) ? 0 : pixels_raw[i*bi.getHeight()+j];
				pixels.put((byte)((pixel >>16)&0xFF));
				pixels.put((byte)((pixel >> 8)&0xFF));
				pixels.put((byte)((pixel     )&0xFF));
				pixels.put((byte)((pixel >>24)&0xFF));
			}
		}
		pixels=newPixels;
	}
	private void upload() {
		pixels.flip();
		glActiveTexture(GL_TEXTURE0+binding);
		glBindTexture(GL_TEXTURE_2D_ARRAY,id);
		glTexImage3D(GL_TEXTURE_2D_ARRAY,0,GL_RGBA,width,height,depth,0,GL_RGBA,GL_UNSIGNED_BYTE,pixels);
		glTexParameteri(GL_TEXTURE_2D_ARRAY,GL_TEXTURE_MIN_FILTER,GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D_ARRAY,GL_TEXTURE_MIN_FILTER,GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D_ARRAY,GL_TEXTURE_MAG_FILTER,GL_LINEAR);
		glBindTexture(GL_TEXTURE_2D_ARRAY,0);
	}
	public void bind() { //Bind it!
		glActiveTexture(GL_TEXTURE0+binding);
		glBindTexture(GL_TEXTURE_2D_ARRAY,id);
	}
}
