package graphics;
import static org.lwjgl.opengl.GL46.*;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;

import logger.Logger;
import util.Util;

public class FrameBuffer {
	//All framebuffers using this class will implement rtt
	public static final int FRAMEBUFFER=0, RENDERBUFFER=1, TEXTUREBUFFER=2;
	private int fbo; //Framebuffer object
	private int[] tbo; //Texture buffer object
	private int rbo; //Renderbuffer object
	private int multiSample=-1;
	private IntBuffer ib;
	public int getID(int id, int secID) {
		switch(id) {
		case FRAMEBUFFER:
			return fbo;
		case RENDERBUFFER:
			return rbo;
		case TEXTUREBUFFER:
			return tbo[secID];
		default:
			throw new IllegalArgumentException("Please just use a valid buffer id.");
		}
	}
	public FrameBuffer(int ms) {
		this(ms,1,GL_RGBA16F);
	}
	public FrameBuffer(int ms, int ntbo, int format) {
		if(ntbo<1) {
			throw new IllegalArgumentException("What the hell were you thinking?");
		}
		IntBuffer m=Renderer.stack.mallocInt(1);
		glGetIntegerv(GL_MAX_COLOR_ATTACHMENTS,m);
		if(ntbo>m.get(0)) {
			throw new IllegalArgumentException("You tried to init a framebuffer with "+ntbo+" attachments. Uh... Just so you know, the max is "+m.get(0));
		}
		multiSample=ms;
		int texParam=(ms>0)?GL_TEXTURE_2D_MULTISAMPLE:GL_TEXTURE_2D;
		fbo=glGenFramebuffers();
		glBindFramebuffer(GL_FRAMEBUFFER,fbo);
		tbo=new int[ntbo];
		int[] attachList=new int[ntbo];
		for(int i=0;i<ntbo;i++) {
			tbo[i]=glGenTextures();
			glBindTexture(texParam,tbo[i]);
			if(ms>0) {
				glTexImage2DMultisample(texParam,ms,format,RenderUtils.winW,RenderUtils.winH,true);
			} else {
				glTexImage2D(texParam,0,format,RenderUtils.winW,RenderUtils.winH,0,GL_RGBA,GL_FLOAT,(FloatBuffer)null);
			}
			glTexParameteri(texParam,GL_TEXTURE_MIN_FILTER,GL_LINEAR);
			glTexParameteri(texParam,GL_TEXTURE_MAG_FILTER,GL_LINEAR);
			glTexParameteri(texParam,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE);
		    glTexParameteri(texParam,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE);
			
			glFramebufferTexture2D(GL_FRAMEBUFFER,GL_COLOR_ATTACHMENT0+i,texParam,tbo[i],0);
			attachList[i]=GL_COLOR_ATTACHMENT0+i;
		}
		ib=BufferUtils.createIntBuffer(tbo.length);
		Util.asIntBuffer(attachList,ib);
		rbo=glGenRenderbuffers();
		glBindRenderbuffer(GL_RENDERBUFFER,rbo); 
		if(ms>0) {
			glRenderbufferStorageMultisample(GL_RENDERBUFFER,ms,GL_DEPTH24_STENCIL8,RenderUtils.winW,RenderUtils.winH);
		} else {
			glRenderbufferStorage(GL_RENDERBUFFER,GL_DEPTH24_STENCIL8,RenderUtils.winW,RenderUtils.winH);
		}
		
		glFramebufferRenderbuffer(GL_FRAMEBUFFER,GL_DEPTH_STENCIL_ATTACHMENT,GL_RENDERBUFFER,rbo);
		
//		rbo=glGenTextures();
//		glBindTexture(GL_TEXTURE_2D,tbo);
//		
//		glTexImage2D(GL_TEXTURE_2D,0,GL_DEPTH24_STENCIL8,RenderUtils.winW,RenderUtils.winH,0,GL_DEPTH_STENCIL,GL_UNSIGNED_INT_24_8,(FloatBuffer)null);
		
		if(glCheckFramebufferStatus(GL_FRAMEBUFFER)==GL_FRAMEBUFFER_COMPLETE) {
			Logger.log(0,"Framebuffer initialized successfully.");
		} else {
			Logger.log(4,"Framebuffer initiation did not result in a FRAMEBUFFER_COMPLETE flag.");
		}
		glBindTexture(texParam,0);
		glBindFramebuffer(GL_FRAMEBUFFER,0);
		glBindRenderbuffer(GL_RENDERBUFFER,0);
	}
	public void delete() {
		glDeleteFramebuffers(fbo);
		glDeleteTextures(tbo);
		glDeleteRenderbuffers(rbo);
	}
	public void bindTexture(int id) {
		if(multiSample>0) {
			throw new IllegalStateException("Cannot bind MSAA framebuffer to texture.");
		}
		glActiveTexture(GL_TEXTURE0);
		bindTexture(id,0);
	}
	public void bindTexture(int id, int binding) {
		if(multiSample>0) {
			throw new IllegalStateException("Cannot bind MSAA framebuffer to texture.");
		}
		glActiveTexture(GL_TEXTURE0+binding);
		glBindTexture(GL_TEXTURE_2D,tbo[id]);
	}
	public void blitTo(FrameBuffer buffer) {
		blitTo(buffer,0);
	}
	public void blitTo(FrameBuffer buffer, int id) {
		glBindFramebuffer(GL_READ_FRAMEBUFFER, fbo);
		glBindFramebuffer(GL_DRAW_FRAMEBUFFER, buffer.fbo);
		glDrawBuffers(buffer.ib);
		glReadBuffer(GL_COLOR_ATTACHMENT0+id);
		glBlitFramebuffer(0,0,RenderUtils.winW,RenderUtils.winH,0,0,RenderUtils.winW,RenderUtils.winH,GL_COLOR_BUFFER_BIT,GL_NEAREST);
		glBindFramebuffer(GL_READ_FRAMEBUFFER, 0);
		glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
	}
	public void bind() {
		glBindFramebuffer(GL_FRAMEBUFFER,fbo);
		glDrawBuffers(ib);
	}
	public static void unbind_all() {
		glBindFramebuffer(GL_FRAMEBUFFER,0);
	}
	public void unbind() {
		unbind_all();
	}
	
}
