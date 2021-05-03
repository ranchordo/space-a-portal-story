package objectTypes;

import static org.lwjgl.opengl.GL43.*;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;

import org.lwjgl.BufferUtils;

import com.bulletphysics.collision.shapes.BvhTriangleMeshShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.shapes.TriangleIndexVertexArray;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.MotionState;
import com.bulletphysics.linearmath.Transform;

import anim.Animator;
import graphics.GraphicsInit;
import graphics.RenderUtils;
import graphics.Renderer;
import graphics.Shader;
import graphics.Texture;
import graphics.Tri;
import graphics.VertexMap;
import logger.Logger;
import physics.Physics;
import pooling.PoolElement;
import pooling.Pools;
import util.OBJReturnObject;
import util.Util;

public class GObject {
	//Data types for VBO copying
	public static final int VERTEX_DATA=0;
	public static final int TEXTURE_DATA=1;
	public static final int COLOR_DATA=2;
	public static final int NORMAL_DATA=3;
	public static final int TANGENT_DATA=4;
	public static final int MATERIAL_DATA=5;
	
	public boolean useTex=false;
	public boolean useBump=false;
	private ArrayList<Tri> tris=new ArrayList<Tri>();
	public VertexMap vmap=new VertexMap();
	public boolean fromOBJ=false;
	
	private Shader renderingShader=null;
	
	public boolean useLighting=true;
	public boolean wireframe=false;
	
	private boolean trisLocked=false;
	
	public boolean useCulling=true;
	public boolean hasAlpha=false;
	
	public Vector3f scale=new Vector3f(1.0f,1.0f,1.0f);
	
	private int v_id;
	private int t_id;
	private int c_id;
	private int n_id;
	private int m_id;
	
	private int tan_id;
	private int bit_id;
	
	public int instances=-1;
	
//	protected Vector2f atlas_transform(Vector2f prop_texcoord) {
//		Vector2f texcoord_pix_rel=new Vector2f(prop_texcoord.x*vmap.tex.img.getWidth(),(1-prop_texcoord.y)*vmap.tex.img.getHeight());
//		Vector2f texcoord_pix=new Vector2f(texcoord_pix_rel.x+vmap.tex.atlasX,texcoord_pix_rel.y+vmap.tex.atlasY);
//		Vector2f texcoord=new Vector2f(texcoord_pix.x/Renderer.activeAtlas.atlas.getWidth(),texcoord_pix.y/Renderer.activeAtlas.atlas.getHeight());
//		return texcoord;
//	}
	
	public void addTri(Tri toAdd) {
		if(!trisLocked) {tris.add(toAdd);}
		else {
			throw new SecurityException("addTri: Tris are locked, you idiot!");
		}
	}
	public ArrayList<Tri> getTris() {
		return tris;
	}
	public void clean() {
		clearVBOs();
		trisLocked=false;
		clearTris();
	}
	public void clearTris() {
		if(!trisLocked) {tris.clear();}
		else {
			throw new SecurityException("clearTris: Tris are locked, you idiot!");
		}
	}
	public boolean getLocked() {return trisLocked;}
	public void lock() {trisLocked=true;}
	public void unlock() {trisLocked=false;}
	
	FloatBuffer vertex_data;
	FloatBuffer texture_data;
	FloatBuffer color_data;
	FloatBuffer normal_data;
	FloatBuffer material_data;
	FloatBuffer tangent_data;
	FloatBuffer bitangent_data;
	private FloatBuffer handleBufferCreation(FloatBuffer buffer, float[] data) {
		if(buffer==null || buffer.capacity()<data.length) {
			buffer=BufferUtils.createFloatBuffer(data.length+12);
		}
		Util.asFloatBuffer(data,buffer);
		return buffer;
	}
	public void copyData(int type, int mode) {
		switch(type) {
		case VERTEX_DATA:
			glBindBuffer(GL_ARRAY_BUFFER,v_id);
			vertex_data=handleBufferCreation(vertex_data,glvertices());
			glBufferData(GL_ARRAY_BUFFER,vertex_data,mode);
			glBindBuffer(GL_ARRAY_BUFFER,0);
			break;
		case TEXTURE_DATA:
			glBindBuffer(GL_ARRAY_BUFFER,t_id);
			texture_data=handleBufferCreation(texture_data,gltexcoords());
			glBufferData(GL_ARRAY_BUFFER,texture_data,mode);
			glBindBuffer(GL_ARRAY_BUFFER,0);
			break;
		case COLOR_DATA:
			glBindBuffer(GL_ARRAY_BUFFER,c_id);
			color_data=handleBufferCreation(color_data,glcolors());
			glBufferData(GL_ARRAY_BUFFER,color_data,mode);
			glBindBuffer(GL_ARRAY_BUFFER,0);
			break;
		case NORMAL_DATA:
			glBindBuffer(GL_ARRAY_BUFFER,n_id);
			normal_data=handleBufferCreation(normal_data,glnormals());
			glBufferData(GL_ARRAY_BUFFER,normal_data,mode);
			glBindBuffer(GL_ARRAY_BUFFER,0);
			break;
		case MATERIAL_DATA:
			glBindBuffer(GL_ARRAY_BUFFER,m_id);
			material_data=handleBufferCreation(material_data,glmatdata());
			glBufferData(GL_ARRAY_BUFFER,material_data,mode);
			glBindBuffer(GL_ARRAY_BUFFER,0);
			break;
		case TANGENT_DATA:
			Vector3f[][] tbi=tangentBitangent();
			glBindBuffer(GL_ARRAY_BUFFER,tan_id);
			tangent_data=handleBufferCreation(tangent_data,GObject.toFloats(tbi[0]));
			glBufferData(GL_ARRAY_BUFFER,tangent_data,mode);
			glBindBuffer(GL_ARRAY_BUFFER,bit_id);
			bitangent_data=handleBufferCreation(bitangent_data,GObject.toFloats(tbi[1]));
			glBufferData(GL_ARRAY_BUFFER,bitangent_data,mode);
			glBindBuffer(GL_ARRAY_BUFFER,0);
			break;
		}
	}
	public void initVBO() {
		v_id=glGenBuffers();
		t_id=glGenBuffers();
		c_id=glGenBuffers();
		n_id=glGenBuffers();
		m_id=glGenBuffers();
		
		tan_id=glGenBuffers();
		bit_id=glGenBuffers();
	}
	public boolean rayTest(Vector3f a, Vector3f b, Transform tr) {
		for(Tri t : tris) {
			if(t.rayTest(a,b,vmap,tr)) {
				return true;
			}
		}
		return false;
	}
	public ArrayList<Tri> rayTest_list(Vector3f a, Vector3f b, Transform tr) {
		ArrayList<Tri> result=new ArrayList<Tri>();
		for(Tri t : tris) {
			if(t.rayTest(a,b,vmap,tr)) {
				result.add(t);
			}
		}
		return result;
	}
	public Tri rayTest_closest(Vector3f a, Vector3f b, Transform tr) {
		float minDistance=Tri.CLIP_DISTANCE+2;
		Tri winner=null;
		for(Tri t : tris) {
			float t_val=t.rayTest_back(a, b, vmap, tr);
			if(t_val>=-0.1) {
				if(t_val<minDistance) {
					minDistance=t_val;
					winner=t;
				}
			}
		}
		if(winner!=null) {
		}
		return winner;
	}
	public float rayTest_distance(Vector3f a, Vector3f b, Transform tr) {
		float minDistance=Tri.CLIP_DISTANCE+2;
		for(Tri t : tris) {
			float t_val=t.rayTest_back(a, b, vmap, tr);
			if(t_val>=-0.1) {
				if(t_val<minDistance) {
					minDistance=t_val;
				}
			}
		}
		if(minDistance>=Tri.CLIP_DISTANCE+1) {
			return -1;
		}
		return minDistance;
	}
	IntBuffer bufferDel=BufferUtils.createIntBuffer(7);
	public void clearVBOs() {
		glDeleteBuffers(Util.asIntBuffer(new int[] {v_id, t_id, c_id, n_id, m_id, tan_id, bit_id},bufferDel));
	}
	public void refresh() {
		if(true) {//dynamic) {
			this.copyData(VERTEX_DATA,GL_DYNAMIC_DRAW);
			this.copyData(NORMAL_DATA,GL_DYNAMIC_DRAW);
			if(texAV()) {this.copyData(TEXTURE_DATA,GL_DYNAMIC_DRAW);}
			this.copyData(COLOR_DATA,GL_DYNAMIC_DRAW);
			this.copyData(MATERIAL_DATA,GL_DYNAMIC_DRAW);
			if(this.useBump && vmap.tex.normLoaded) {
				this.copyData(TANGENT_DATA,GL_DYNAMIC_DRAW);
			}
		}
	}
	private float[] toFloatArray(Matrix3f m,float[] put) {
		put[0]=m.m00; put[1]=m.m10; put[2]=m.m20;
		put[3]=m.m01; put[4]=m.m11; put[5]=m.m21;
		put[6]=m.m02; put[7]=m.m12; put[8]=m.m22;
		return put;
	}
	Transform mmc=new Transform();
	Matrix3f m=new Matrix3f();
	float[] mma=new float[16];
	FloatBuffer fm=BufferUtils.createFloatBuffer(16);
	FloatBuffer f=BufferUtils.createFloatBuffer(9);
	
	Matrix4f m4_=new Matrix4f();
	float[] m3_=new float[9];
	float sc;
	public void highRender_noPushPop_customTransform(Transform mm) {
		if(renderingShader==null) {
			Renderer.main.bind();
		} else {
			renderingShader.bind();
		}
		if(mm!=null) {
			mm.getMatrix(m4_).getRotationScale(m);
			sc=Util.getAvgScale(m);
			f=Util.asFloatBuffer(toFloatArray(m,m3_),f);
			//mma=new float[16];
			mm.getOpenGLMatrix(mma);
			fm=Util.asFloatBuffer(mma,fm);
			Renderer.activeShader.setUniformMatrix4fv("master_matrix", fm);
		}
		
		mmc.set(Renderer.camtr);
		if(Renderer.activePortalTransform!=0) {
			int usePortal=Util.mod(Renderer.activePortalTransform-1,2)+1;
			int loop=Math.round((Renderer.activePortalTransform-usePortal)/2.0f)+1;
			Transform comp=(usePortal==1)?GraphicsInit.player.portalPair.difference():GraphicsInit.player.portalPair.difference_inv();
			for(int i=0;i<loop;i++) {
				mmc.mul(mmc,comp);
			}
		}
		mmc.getOpenGLMatrix(mma);
		fm=Util.asFloatBuffer(mma,fm);
		Renderer.activeShader.setUniformMatrix4fv("world2view",fm);
		
		Util.openGLMatrix(RenderUtils.proj_matrix,mma);
		fm=Util.asFloatBuffer(mma,fm);
		Renderer.activeShader.setUniformMatrix4fv("proj_matrix",fm);
		
		Renderer.activeShader.setUniform1i("millis",(int)(RenderUtils.micros()));
		
		Renderer.activeShader.setUniform1f("useTextures", (useTex && this.vmap.tex.colorLoaded && Renderer.useGraphics) ? 2 : 0);
		Renderer.activeShader.setUniform1f("useLighting", (this.useLighting && Renderer.useGraphics) ? 2 : 0);
		Renderer.activeShader.setUniform1f("useDetail", (this.vmap.tex.normLoaded && useBump && Renderer.useGraphics) ? (2) : 0);
		
		Renderer.activeShader.applyAllSSBOs();
		if(useTex && this.vmap.tex.colorLoaded && Renderer.useGraphics) {this.vmap.tex.bind();}
		//glScalef(scale.x,scale.y,scale.z);
		//glScalef(sc,sc,sc);
		this.render();
	}
//	private static int print(String st) { //DEBGUG
//		System.out.println(st);
//		return 0;
//	}
	public void highRender(PhysicsObject obj) {
		highRender_noPushPop_customTransform(obj.getTransform());
	}

	public int getNumTris() {
		return tris.size();
	}
	public static final int RTXVertexSize=16;
	public FloatBuffer RTXAddVertexData(FloatBuffer in, int modelMatrixID) {
		for(int i=0;i<tris.size();i++) {
			Tri t=tris.get(i);
			Vector3f v0=vmap.vertices.get(t.vertices[0]);
			Vector3f v1=vmap.vertices.get(t.vertices[1]);
			Vector3f v2=vmap.vertices.get(t.vertices[2]);
			in.put(v0.x); in.put(v0.y); in.put(v0.z); in.put(2);
			in.put(v1.x); in.put(v1.y); in.put(v1.z); in.put(2);
			in.put(v2.x); in.put(v2.y); in.put(v2.z); in.put(modelMatrixID);
			in.put(modelMatrixID);
			in.put(1);
			in.put(1);
			in.put(1);
		}
		return in;
	}
	public boolean texUAL() {return useTex && vmap.tex.colorLoaded;}
	public boolean texAV() {return vmap.texcoords.size()>0;}
	public boolean bumpUAL() {
		return useBump && vmap.tex.normLoaded;
	}
	private void drawArrays() {
		if(instances<1) {
			glDrawArrays(GL_TRIANGLES,0,tris.size()*3);
		} else {
			glDrawArraysInstanced(GL_TRIANGLES,0,tris.size()*3,instances);
		}
	}
	public void render() {
		if(!useCulling) {glDisable(GL_CULL_FACE);}
		if(wireframe) {glPolygonMode(GL_FRONT,GL_LINE);}
		glEnableVertexAttribArray(0);
		if(texAV()) {glEnableVertexAttribArray(8);}
		glEnableVertexAttribArray(2);
		glEnableVertexAttribArray(3);
		if(bumpUAL()) {glEnableVertexAttribArray(14);}
		if(bumpUAL()) {glEnableVertexAttribArray(15);}
		glEnableVertexAttribArray(13);
		
		
		glBindBuffer(GL_ARRAY_BUFFER,v_id);
		glVertexAttribPointer(0,3,GL_FLOAT,false,0,0);
		
		if(texAV()) {
			glBindBuffer(GL_ARRAY_BUFFER,t_id);
			glVertexAttribPointer(8,2,GL_FLOAT,false,0,0);
		}
		
		glBindBuffer(GL_ARRAY_BUFFER,n_id);
		glVertexAttribPointer(2,3,GL_FLOAT,false,0,0);
		
		glBindBuffer(GL_ARRAY_BUFFER,c_id);
		glVertexAttribPointer(3,4,GL_FLOAT,false,0,0);
		
		if(bumpUAL()) {
			glBindBuffer(GL_ARRAY_BUFFER,tan_id);
			glVertexAttribPointer(14,3,GL_FLOAT,false,0,0);
			
			glBindBuffer(GL_ARRAY_BUFFER,bit_id);
			glVertexAttribPointer(15,3,GL_FLOAT,false,0,0);
		}
		glBindBuffer(GL_ARRAY_BUFFER,m_id);
		glVertexAttribPointer(13,4,GL_FLOAT,false,0,0);
		
		
		drawArrays();
		glBindBuffer(GL_ARRAY_BUFFER,0);
		
		glDisableVertexAttribArray(0);
		if(texAV()) {glDisableVertexAttribArray(8);}
		glDisableVertexAttribArray(2);
		glDisableVertexAttribArray(3);
		if(wireframe) {glPolygonMode(GL_FRONT,GL_FILL);}
		if(bumpUAL()) {glDisableVertexAttribArray(14);}
		if(bumpUAL()) {glDisableVertexAttribArray(15);}
		glDisableVertexAttribArray(13);
		if(!useCulling) {glEnable(GL_CULL_FACE);}
	}
	public void setColor(float r, float g, float b) {
		Tri t0=tris.get(0);
		if(t0.color[0]==r && t0.color[1]==g && t0.color[2]==b) {
			return;
		}
		for(int i=0;i<tris.size();i++) {
			tris.get(i).setColor(r, g, b);
		}
	}
	public void setMaterial(float spec, float rough, float mat2, float mat3) {
		for(int i=0;i<tris.size();i++) {
			tris.get(i).setMaterial(spec,rough,mat2,mat3);
		}
	}
	public void setColor(float r, float g, float b, float a) {
		Tri t0=tris.get(0);
		if(t0.color[0]==r && t0.color[1]==g && t0.color[2]==b && t0.color[3]==a) {
			return;
		}
		for(int i=0;i<tris.size();i++) {
			tris.get(i).setColor(r, g, b);
			tris.get(i).setAlpha(a);
		}
	}
	public void scale(float s) {
		this.vmap.scale(s);
	}
	public void scale(float x, float y, float z) {
		this.vmap.scale(x,y,z);
	}
//	public GObject scale(float s) {
//		for(Tri t : tris) {
//			for(float[] vert : t.vertices) {
//				for(int i=0;i<vert.length;i++) {
//					vert[i]*=s;
//				}
//			}
//		}
//		return this;
//	}
	private float[] glvertices_last;
	public float[] glvertices() {
		if(glvertices_last!=null && trisLocked) {
			return glvertices_last;
		}
		glvertices_last=new float[tris.size()*3*3];
		for(int i=0;i<tris.size();i++) {
			Vector3f vertex1=vmap.vertices.get(tris.get(i).vertices[0]);
			Vector3f vertex2=vmap.vertices.get(tris.get(i).vertices[1]);
			Vector3f vertex3=vmap.vertices.get(tris.get(i).vertices[2]);
			glvertices_last[i*3*3+0]=vertex1.x;
			glvertices_last[i*3*3+1]=vertex1.y;
			glvertices_last[i*3*3+2]=vertex1.z;
			
			glvertices_last[i*3*3+3]=vertex2.x;
			glvertices_last[i*3*3+4]=vertex2.y;
			glvertices_last[i*3*3+5]=vertex2.z;
			
			glvertices_last[i*3*3+6]=vertex3.x;
			glvertices_last[i*3*3+7]=vertex3.y;
			glvertices_last[i*3*3+8]=vertex3.z;
		}
		return glvertices_last;
	}
	private float[] glnormals_last;
	public float[] glnormals() {
		if(glnormals_last!=null && trisLocked) {
			return glnormals_last;
		}
		glnormals_last=new float[tris.size()*3*3];
		for(int i=0;i<tris.size();i++) {
			Vector3f vertex1=vmap.normals.get(tris.get(i).normals[0]);
			Vector3f vertex2=vmap.normals.get(tris.get(i).normals[1]);
			Vector3f vertex3=vmap.normals.get(tris.get(i).normals[2]);
			glnormals_last[i*3*3+0]=vertex1.x;
			glnormals_last[i*3*3+1]=vertex1.y;
			glnormals_last[i*3*3+2]=vertex1.z;
			
			glnormals_last[i*3*3+3]=vertex2.x;
			glnormals_last[i*3*3+4]=vertex2.y;
			glnormals_last[i*3*3+5]=vertex2.z;
			
			glnormals_last[i*3*3+6]=vertex3.x;
			glnormals_last[i*3*3+7]=vertex3.y;
			glnormals_last[i*3*3+8]=vertex3.z;
		}
		return glnormals_last;
	}
	private float[] glcolors_last;
	public float[] glcolors() {
		if(glcolors_last==null || !trisLocked) {
			glcolors_last=new float[tris.size()*3*4];
		}
		for(int i=0;i<tris.size();i++) {
			PoolElement<Vector4f> vertex1_pe=Pools.vector4f.alloc();
			vertex1_pe.o().set(tris.get(i).color[0],tris.get(i).color[1],tris.get(i).color[2],tris.get(i).color[3]);
			Vector4f vertex1=vertex1_pe.o();
			glcolors_last[i*3*4+0]=vertex1.x;
			glcolors_last[i*3*4+1]=vertex1.y;
			glcolors_last[i*3*4+2]=vertex1.z;
			glcolors_last[i*3*4+3]=vertex1.w;
			
			glcolors_last[i*3*4+4]=vertex1.x;
			glcolors_last[i*3*4+5]=vertex1.y;
			glcolors_last[i*3*4+6]=vertex1.z;
			glcolors_last[i*3*4+7]=vertex1.w;
			
			glcolors_last[i*3*4+8]=vertex1.x;
			glcolors_last[i*3*4+9]=vertex1.y;
			glcolors_last[i*3*4+10]=vertex1.z;
			glcolors_last[i*3*4+11]=vertex1.w;
			vertex1_pe.free();
		}
		return glcolors_last;
	}
	private float[] glmatdata_last;
	public float[] glmatdata() {
		if(glmatdata_last==null || !trisLocked) {
			glmatdata_last=new float[tris.size()*3*4];
		}
		for(int i=0;i<tris.size();i++) {
			PoolElement<Vector4f> vertex1_pe=Pools.vector4f.alloc();
			vertex1_pe.o().set(tris.get(i).material[0],tris.get(i).material[1],tris.get(i).material[2],tris.get(i).material[3]);
			Vector4f vertex1=vertex1_pe.o();
			glmatdata_last[i*3*4+0]=vertex1.x;
			glmatdata_last[i*3*4+1]=vertex1.y;
			glmatdata_last[i*3*4+2]=vertex1.z;
			glmatdata_last[i*3*4+3]=vertex1.w;
			
			glmatdata_last[i*3*4+4]=vertex1.x;
			glmatdata_last[i*3*4+5]=vertex1.y;
			glmatdata_last[i*3*4+6]=vertex1.z;
			glmatdata_last[i*3*4+7]=vertex1.w;
			
			glmatdata_last[i*3*4+8]=vertex1.x;
			glmatdata_last[i*3*4+9]=vertex1.y;
			glmatdata_last[i*3*4+10]=vertex1.z;
			glmatdata_last[i*3*4+11]=vertex1.w;
			vertex1_pe.free();
		}
		return glmatdata_last;
	}
	private float[] gltexcoords_last;
	public float[] gltexcoords() {
		if(gltexcoords_last!=null && trisLocked) {
			return gltexcoords_last;
		}
		gltexcoords_last=new float[tris.size()*3*2];
		for(int i=0;i<tris.size();i++) {
			Vector2f vertex1=vmap.texcoords.get(tris.get(i).texcoords[0]);
			Vector2f vertex2=vmap.texcoords.get(tris.get(i).texcoords[1]);
			Vector2f vertex3=vmap.texcoords.get(tris.get(i).texcoords[2]);
			gltexcoords_last[i*3*2+0]=vertex1.x;
			gltexcoords_last[i*3*2+1]=1-vertex1.y;
			
			gltexcoords_last[i*3*2+2]=vertex2.x;
			gltexcoords_last[i*3*2+3]=1-vertex2.y;
			
			gltexcoords_last[i*3*2+4]=vertex3.x;
			gltexcoords_last[i*3*2+5]=1-vertex3.y;
		}
		return gltexcoords_last;
	}
	private static float[] toFloats(Vector3f[] vs) {
		float[] ret=new float[vs.length*3];
		for(int i=0;i<vs.length;i++) {
			ret[3*i+0]=vs[i].x;
			ret[3*i+1]=vs[i].y;
			ret[3*i+2]=vs[i].z;
		}
		return ret;
	}
	private Vector3f[][] tangentBitangent_last;
	public Vector3f[][] tangentBitangent() {
		if(tangentBitangent_last!=null && trisLocked) {
			return tangentBitangent_last;
		}
		Vector3f[] tangents=new Vector3f[tris.size()*3];
		Vector3f[] bitangents=new Vector3f[tris.size()*3];
		for(int i=0;i<tris.size();i++) {
			Vector3f v0=vmap.vertices.get(tris.get(i).vertices[0]);
			Vector3f v1=vmap.vertices.get(tris.get(i).vertices[1]);
			Vector3f v2=vmap.vertices.get(tris.get(i).vertices[2]);
			
			Vector2f uv0=vmap.texcoords.get(tris.get(i).texcoords[0]);
			Vector2f uv1=vmap.texcoords.get(tris.get(i).texcoords[1]);
			Vector2f uv2=vmap.texcoords.get(tris.get(i).texcoords[2]);
			
			Vector3f dp1=new Vector3f(); dp1.sub(v1,v0);
			Vector3f dp2=new Vector3f(); dp2.sub(v2,v0);
			
			Vector2f duv1=new Vector2f(); duv1.sub(uv1,uv0);
			Vector2f duv2=new Vector2f(); duv2.sub(uv2,uv0);
			
			/*
			 *  float r = 1.0f / (deltaUV1.x * deltaUV2.y - deltaUV1.y * deltaUV2.x);
        	 *  glm::vec3 tangent = (deltaPos1 * deltaUV2.y   - deltaPos2 * deltaUV1.y)*r;
        	 *  glm::vec3 bitangent = (deltaPos2 * deltaUV1.x   - deltaPos1 * deltaUV2.x)*r;
			 */
			float r=1.0f / (duv1.x*duv2.y - duv1.y*duv2.x);
			Vector3f tangent=new Vector3f();//(Vector3f) Util.mul(Util.sub(Util.mul(dp1,duv2.y),Util.mul(dp2,duv1.y)),r);
			Vector3f bitangent=new Vector3f();//(Vector3f) Util.mul(Util.add(Util.mul(dp1,-duv2.x),Util.mul(dp2,duv1.x)),r);
			tangent.x = r * (duv2.y * dp1.x - duv1.y * dp2.x);
			tangent.y = r * (duv2.y * dp1.y - duv1.y * dp2.y);
			tangent.z = r * (duv2.y * dp1.z - duv1.y * dp2.z);

			bitangent.x = r * (-duv2.x * dp1.x + duv1.x * dp2.x);
			bitangent.y = r * (-duv2.x * dp1.y + duv1.x * dp2.y);
			bitangent.z = r * (-duv2.x * dp1.z + duv1.x * dp2.z);
			tangents[i*3+0]=tangent;
			tangents[i*3+1]=tangent;
			tangents[i*3+2]=tangent;
			bitangents[i*3+0]=bitangent;
			bitangents[i*3+1]=bitangent;
			bitangents[i*3+2]=bitangent;
		}
		Vector3f[][] ret=new Vector3f[][] {tangents,bitangents};
		tangentBitangent_last=ret;
		return ret;
	}
	public GObject loadOBJ(String filename) { //Wavefront file parser "frontend"
		if(trisLocked) {
			throw new SecurityException("LoadOBJ -> Tris are locked. What the hell were you thinking?");
		}
		try {
			tris=loadOBJ_raw(new FileInputStream(Util.getExternalPath()+"/assets/3d/"+filename+".obj"));
			if(vmap.tex.colorLoaded) {
				this.vmap.tex=new Texture();
				boolean texError=true;
				this.vmap.tex.colorLoaded=false;
				for(String ext : new String[] {".png",".jpg"}) {
					String search="3d/"+filename+ext;
					boolean found=false;
					for(Texture p : Renderer.activeCache.cache) {
						if(p.name.equals(search)) {
							this.vmap.tex=p;
							texError=false;
							found=true;
							this.vmap.tex.colorLoaded=true;
							break;
						}
					}
					if(found) {break;}
					try {
						Logger.log(0,"Loading new texture "+"3d/"+filename+ext+". If you see this often, there's a problem.");
						this.vmap.tex.create_color("3d/"+filename+ext);
						try {
							this.vmap.tex.create_bump("3d/"+filename+"_bump"+ext);
						} catch (FileNotFoundException e) {
							Logger.log(0,"GObject "+filename+": No bump map.");
							this.vmap.tex.bumpLoaded=false;
						}
						try {
							this.vmap.tex.create_norm("3d/"+filename+"_normal"+ext);
						} catch (FileNotFoundException e) {
							Logger.log(0,"GObject "+filename+": No normal map.");
							this.vmap.tex.normLoaded=false;
						}
						this.vmap.tex.colorLoaded=true;
						this.vmap.tex.name=search;
						texError=false;
						Renderer.activeCache.cache.add(this.vmap.tex);
						break;
					} catch (FileNotFoundException e) {
					} catch (NullPointerException e) {
					} catch (IllegalArgumentException e) {}
				}
				if(texError) {
					Logger.log(1,"Texture mappings included, but no file found for model "+filename);
				}
			}
		} catch(FileNotFoundException e) {
			e.printStackTrace();
			Logger.log(4,filename+" does not appear to exist.",e);
		} catch(IOException e) {
			e.printStackTrace();
			Logger.log(4,"IOException GObject.loadObj",e);
		}
		return this;
	}
	public void loadTexture(String texname) throws IOException {
		if(vmap.tex.colorLoaded) {
			this.vmap.tex=new Texture();
			this.vmap.tex.colorLoaded=false;
			for(Texture p : Renderer.activeCache.cache) {
				if(p.name.equals(texname)) {
					this.vmap.tex=p;
					this.vmap.tex.colorLoaded=true;
					return;
				}
			}
			try {
				Logger.log(0,"Loading new texture "+texname+". If you see this often, there's a problem.");
				this.vmap.tex.create_color(texname);
				String[] dot=texname.split("\\.");
				dot[dot.length-2]=dot[dot.length-2]+"_bump";
				String bumpname=String.join(".",dot);
				
				dot=texname.split("\\.");
				dot[dot.length-2]=dot[dot.length-2]+"_normal";
				String normname=String.join(".",dot);
				try {
					this.vmap.tex.create_bump(bumpname);
				} catch (FileNotFoundException e) {
					Logger.log(0,"GObject texture "+bumpname+": No bump map.");
					this.vmap.tex.bumpLoaded=false;
				}
				try {
					this.vmap.tex.create_norm(normname);
				} catch (FileNotFoundException e) {
					Logger.log(0,"GObject texture "+normname+": No normal map.");
					this.vmap.tex.normLoaded=false;
				}
				this.vmap.tex.colorLoaded=true;
				this.vmap.tex.name=texname;
				Renderer.activeCache.cache.add(this.vmap.tex);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				System.exit(1);
			} catch (NullPointerException e) {
			} catch (IllegalArgumentException e) {}
		}
	}
	public GObject loadOBJ(String filename, String texname) { //Wavefront file parser "frontend"
		if(trisLocked) {
			throw new SecurityException("LoadOBJ(FN,TN) -> Tris are locked. What the hell were you thinking?");
		}
		try {
			tris=loadOBJ_raw(new FileInputStream(Util.getExternalPath()+"/assets/3d/"+filename+".obj"));
			loadTexture(texname);
		} catch(FileNotFoundException e) {
			e.printStackTrace();
			Logger.log(4,filename+" does not appear to exist.",e);
		} catch(IOException e) {
			e.printStackTrace();
			Logger.log(4,"IOException GObject.loadObj(fname,tname)",e);
		}
		return this;
	}
	public ArrayList<Tri> loadOBJ_raw(InputStream f) throws FileNotFoundException, IOException { //Wavefront file parser "backend"
		if(trisLocked) {
			throw new SecurityException("LoadOBJ_raw -> Tris are locked. What the hell were you thinking?");
		}
		fromOBJ=true;
		BufferedReader reader=new BufferedReader(new InputStreamReader(f));
		String line;
		ArrayList<Vector3f> vertices=new ArrayList<Vector3f>();
		ArrayList<Vector3f> normals=new ArrayList<Vector3f>();
		ArrayList<Vector3f> vertIndices=new ArrayList<Vector3f>();
		ArrayList<Vector3f> normIndices=new ArrayList<Vector3f>();
		ArrayList<Vector3f> texIndices=new ArrayList<Vector3f>();
		while((line=reader.readLine())!=null) {
			if(line.startsWith("v ")) {
				float x=Float.valueOf(line.split(" ")[1]);
				float y=Float.valueOf(line.split(" ")[2]);
				float z=Float.valueOf(line.split(" ")[3]);
				vertices.add(new Vector3f(x,y,z));
			} else if(line.startsWith("vn ")) {
				float x=Float.valueOf(line.split(" ")[1]);
				float y=Float.valueOf(line.split(" ")[2]);
				float z=Float.valueOf(line.split(" ")[3]);
				normals.add(new Vector3f(x,y,z));
			} else if(line.startsWith("f ")) {
				try {
					float x=Float.valueOf(line.split(" ")[1].split("/")[0]);
					float y=Float.valueOf(line.split(" ")[2].split("/")[0]);
					float z=Float.valueOf(line.split(" ")[3].split("/")[0]);
					vertIndices.add(new Vector3f(x,y,z));
					float xn=Float.valueOf(line.split(" ")[1].split("/")[2]);
					float yn=Float.valueOf(line.split(" ")[2].split("/")[2]);
					float zn=Float.valueOf(line.split(" ")[3].split("/")[2]);
					normIndices.add(new Vector3f(xn,yn,zn));
					if(this.vmap.tex.colorLoaded) {
						float xt=Float.valueOf(line.split(" ")[1].split("/")[1]);
						float yt=Float.valueOf(line.split(" ")[2].split("/")[1]);
						float zt=Float.valueOf(line.split(" ")[3].split("/")[1]);
						texIndices.add(new Vector3f(xt,yt,zt));
					}
				} catch (NumberFormatException e) {
					e.printStackTrace();
					System.err.println(line);
					System.exit(1);
				}
			} else if(line.startsWith("vt ")) {
				this.vmap.tex.colorLoaded=true;
				String[] split=line.split(" ");
				Vector2f n=new Vector2f(Float.valueOf(split[1]),Float.valueOf(split[2]));
				this.vmap.texcoords.add(n);
			}
		}
		ArrayList<Tri> obj=new ArrayList<Tri>();
		for(int i=0;i<vertIndices.size();i++) {
			Tri t=new Tri(
					-1+(int)vertIndices.get(i).x,
					-1+(int)vertIndices.get(i).y,
					-1+(int)vertIndices.get(i).z,
					
					-1+(int)normIndices.get(i).x,
					-1+(int)normIndices.get(i).y,
					-1+(int)normIndices.get(i).z);
			if(this.vmap.tex.colorLoaded) { 
				t.texcoords[0]=-1+(int)texIndices.get(i).x;
				t.texcoords[1]=-1+(int)texIndices.get(i).y;
				t.texcoords[2]=-1+(int)texIndices.get(i).z;
			}
			obj.add(t);
		}
		for(int i=0;i<vertices.size();i++) {
			this.vmap.vertices.add(vertices.get(i));
		}
		for(int i=0;i<normals.size();i++) {
			this.vmap.normals.add(normals.get(i));
		}
		reader.close();
		f.close();
		return obj;
	}
	public Shader getRenderingShader() {
		return renderingShader;
	}
	public void setRenderingShader(Shader renderingShader) {
		this.renderingShader = renderingShader;
	}
}
