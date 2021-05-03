package util;

import static org.lwjgl.opengl.GL46.*;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Random;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Quat4f;
import javax.vecmath.Tuple3f;
import javax.vecmath.Vector3f;

import org.lwjgl.BufferUtils;

import com.bulletphysics.linearmath.Transform;

import game.Main;
import logger.Logger;
import pooling.PoolElement;
import pooling.Pools;

public class Util {
	public static void initUtil() {
	}
	public static FloatBuffer asFloatBuffer(float[] values, FloatBuffer buffer) {
		buffer.clear();
		buffer.put(values);
		buffer.flip();
		return buffer;
	}
	public static IntBuffer asIntBuffer(int[] values, IntBuffer buffer) {
		//IntBuffer buffer=BufferUtils.createIntBuffer(values.length);
		buffer.clear();
		buffer.put(values);
		buffer.flip();
		return buffer;
	}
	public static IntBuffer asIntBuffer_badgc(int[] values) {
		IntBuffer ret=BufferUtils.createIntBuffer(values.length);
		asIntBuffer(values,ret);
		return ret;
	}
	private static Random rand=new Random();
	public static int randint(int max) {
		return rand.nextInt(max);
	}
	public static float mod(float m, float n) {
		while(m>=n) {m=m-n;}
		while(m<0) {m=m+n;}
		return m;
	}
	public static int indexMax(float... v) {
		int ret=0;
		float max=v[0];
		for(int i=1;i<v.length;i++) {
			if(v[i]>max) {
				ret=i;
				max=v[i];
			}
		}
		return ret;
	}
	public static int indexMin(float... v) {
		int ret=0;
		float max=v[0];
		for(int i=1;i<v.length;i++) {
			if(v[i]<max) {
				ret=i;
				max=v[i];
			}
		}
		return ret;
	}
	public static float distance(Vector3f a, Vector3f b) {
		return (float)Math.sqrt(Math.pow(b.x-a.x,2)+Math.pow(b.y-a.y,2)+Math.pow(b.z-a.z,2));
	}
	public static int mod(int m, int n) {
		while(m>=n) {m=m-n;}
		while(m<0) {m=m+n;}
		return m;
	}
	public static Quat4f AxisAngle_np(AxisAngle4f i) {
		return noPool(AxisAngle(i));
	}
	public static PoolElement<Quat4f> AxisAngle(AxisAngle4f i) {
		PoolElement<Vector3f> v=Pools.vector3f.alloc();
		v.o().set(i.x,i.y,i.z);
		v.o().normalize();
		float f=(float)Math.sin(i.angle/2.0);
		PoolElement<Quat4f> ret=Pools.quat4f.alloc();
		ret.o().set(v.o().x*f,v.o().y*f,v.o().z*f,(float)Math.cos(i.angle/2.0));
		v.free();
		return ret;
	}
//	public static Matrix4f setPerspective(float fovy, float aspect, float zNear, float zFar) {
//		float h = (float) Math.tan(fovy * 0.5f);
//		Matrix4f ret=new Matrix4f();
//		ret.m00=(1.0f / (h * aspect));
//		ret.m11=(1.0f / h);
//		ret.m22=((zFar + zNear) / (zNear - zFar));
//		ret.m32=((zFar + zFar) * zNear / (zNear - zFar));
//		ret.m23=(-1.0f);
//		//ret.m33=1.0f;
//		return ret;
//	}
	public static void openGLMatrix(Matrix4f in, float[] put) {
		put[0]=in.m00;put[1]=in.m01;put[2]=in.m02;put[3]=in.m03;
		put[4]=in.m10;put[5]=in.m11;put[6]=in.m12;put[7]=in.m13;
		put[8]=in.m20;put[9]=in.m21;put[10]=in.m22;put[11]=in.m23;
		put[12]=in.m30;put[13]=in.m31;put[14]=in.m32;put[15]=in.m33;
	}
	public static Tuple3f mul(Tuple3f v, float s) {
		return new Vector3f(v.x*s,v.y*s,v.z*s);
	}
	public static Tuple3f sub(Tuple3f a, Tuple3f b) {
		return new Vector3f(a.x-b.x,a.y-b.y,a.z-b.z);
	}
	public static Tuple3f add(Tuple3f a, Tuple3f b) {
		return new Vector3f(a.x+b.x,a.y+b.y,a.z+b.z);
	}
	public static AxisAngle4f Quat_np(Quat4f q) {
		return noPool(Quat(q));
	}
	public static PoolElement<AxisAngle4f> Quat(Quat4f q) {
		PoolElement<AxisAngle4f> a=Pools.axisAngle4f.alloc();
		a.o().angle=2.0f*(float)Math.acos(q.w);
		float b=(float)Math.sqrt(1-q.w*q.w);
		if(b==0.0f) {
			a.o().x=1;
			a.o().y=0;
			a.o().z=0;
			return a;
		}
		a.o().x=q.x/b;
		a.o().y=q.y/b;
		a.o().z=q.z/b;
		return a;
	}
	private static Matrix4f mat=new Matrix4f();
	public static float getAvgScale_tr(Transform in) {
		in.getMatrix(mat);
		return getAvgScale_4f(mat);
	}
	private static Matrix3f rs=new Matrix3f();
	public static float getAvgScale_4f(Matrix4f in) {
		in.getRotationScale(rs);
		return getAvgScale(rs);
	}
	private static Vector3f col1=new Vector3f();
	private static Vector3f col2=new Vector3f();
	private static Vector3f col3=new Vector3f();
	public static float getAvgScale(Matrix3f in) {
		col1.set(in.m00,in.m10,in.m20);
		col2.set(in.m01,in.m11,in.m21);
		col3.set(in.m02,in.m12,in.m22);
		return (col1.length()+col2.length()+col3.length())/3.0f;
	}
	@SuppressWarnings("unchecked")
	public static <T extends Cloneable> T cloneObject(T i) {
		try {
			return (T)i.getClass().getMethod("clone").invoke(i);
		} catch (Exception e) {
			Logger.log(4,e.toString(),e);
			return null;
		}
	}
	public static <T extends Cloneable> T noPool(PoolElement<T> q) {
		T ret=cloneObject(q.o());
		q.free();
		return ret;
	}
	public static FloatBuffer getMatrix() {
		FloatBuffer b=BufferUtils.createFloatBuffer(16);
		glGetFloatv(GL_MODELVIEW_MATRIX,b);
		b.rewind();
		return b;
	}
	public static String getExternalPath() {
		File jarpath=new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath());
		String externalPath=jarpath.getParentFile().getAbsolutePath().replace("\\", "/").replace("%20", " ");
		return externalPath;
	}
	public static int gcd2(int a, int b) {
		if(a==0) {return b;}
		return gcd2(b%a,a);
	}
	public static int gcd(Iterable<Integer> v) {
		int result=0;
		for(int a : v) {
			result=gcd2(result,a);
		}
		return result;
	}
}
