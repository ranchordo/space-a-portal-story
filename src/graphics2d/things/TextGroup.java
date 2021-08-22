package graphics2d.things;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import javax.vecmath.Vector4f;

import game.Main;
import graphics2d.util.FontAtlas;
import graphics2d.util.Glyph;
import graphics2d.util.InstancedRenderConfig2d;
import graphics2d.util.InstancedRenderer2d;
import lepton.engine.rendering.InstanceAccumulator;
import lepton.engine.rendering.Shader;

public class TextGroup extends Thing2d {
	public static Shader textShader;
	public ArrayList<GenericInstancedThing2d> characters=new ArrayList<GenericInstancedThing2d>();
	private String s;
	private FontAtlas f;
	public float height;
	public float r;
	public float g;
	public float b;
	private InstanceAccumulator ia;
	public TextGroup(String str, FontAtlas font, float x, float y, float height, float r, float g, float b, InstancedRenderer2d sl) {
		f=font;
		s=str;
		refreshString=true;
		this.x=x;
		this.y=y;
		this.height=height;
		if(textShader==null) {
			textShader=Main.shaderLoader.load("specific/textChar");
		}
		ia=sl.loadConfiguration(textShader,f.textureImage,Thing2d.genericSquare,12,"info_buffer").instanceAccumulator;
		this.r=r;
		this.g=g;
		this.b=b;
	}
	private boolean refreshString=false;
	public void setString(String s) {
		this.s=s;
		refreshString=true;
	}
	@Override
	public void logic() {
		float pix2ui=height/f.getHeight();
		if(refreshString) {
			int len=0;
			for(int i=0;i<s.length();i++) {
				if(f.get(s.charAt(i))!=null) {
					len++;
				}
			}
			while(len!=characters.size()) {
				if(len<characters.size()) {
					characters.remove(characters.size()-1);
				} else {
					characters.add((GenericInstancedThing2d)new GenericInstancedThing2d(ia).setParent(parent));
				}
			}
			Glyph a=f.get('I');
			int spacew=a.end-a.start;
			int offset=0;
			int diff=0;
			for(int i=0;i<s.length();i++) {
				Glyph g=f.get(s.charAt(i));
				if(g!=null) {
					GenericInstancedThing2d c=characters.get(i+diff);
					c.x=(offset*pix2ui)+x;
					c.y=y;
					c.height=height;
					c.width=pix2ui*(g.end-g.start);
					c.objectSize=GenericInstancedThing2d.defaultObjectSize+4;
//					c.image=f.textureImage;
					c.texX=g.start/(float)f.getWidth();
					c.texY=0;
					c.texW=(g.end-g.start)/(float)f.getWidth();
					c.texH=1;
					c.renderingShader=textShader;
					c.refreshDataLength();
					c.additionalData[0]=this.r;
					c.additionalData[1]=this.g;
					c.additionalData[2]=this.b;
					c.additionalData[3]=0;
				} else {
					diff--;
				}
				int w=g==null?spacew:g.end-g.start;
				offset+=w;
				offset+=f.getSpacing();
			}
			refreshString=false;
		}
	}
	@Override
	public void render() {
		for(GenericInstancedThing2d t : characters) {
			t.render();
		}
	}
	private Vector4f a=new Vector4f();
	@Override
	public Vector4f getBoundingBox() {
		if(characters.size()>0) {
			a.set(x,y,characters.get(characters.size()-1).getBoundingBox().z,height+y);
			return a;
		}
		return null;
	}
	@Override
	public Thing2d setParent(Thing2d parent) {
		for(GenericInstancedThing2d c : characters) {
			c.setParent(parent);
		}
		this.parent=parent;
		return this;
	}
}
