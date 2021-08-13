package util;

import java.util.List;

import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;

import com.bulletphysics.linearmath.Transform;

import lepton.optim.objpoollib.PoolElement;

public class Util {
	public static Transform nothing=new Transform(new Matrix4f(1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1));
	public static float distance(Vector3f a, Vector3f b) {
		return (float)Math.sqrt(Math.pow(b.x-a.x,2)+Math.pow(b.y-a.y,2)+Math.pow(b.z-a.z,2));
	}
	public static int mod(int m, int n) {
		while(m>=n) {m=m-n;}
		while(m<0) {m=m+n;}
		return m;
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
	public static int sign(float i) {
		return (i>0)?1:((i<0)?-1:0);
	}
	public static void clear(Matrix4f i) {
		i.m00=0; i.m01=0; i.m02=0; i.m03=0;
		i.m10=0; i.m11=0; i.m12=0; i.m13=0;
		i.m20=0; i.m21=0; i.m22=0; i.m23=0;
		i.m30=0; i.m31=0; i.m32=0; i.m33=0;
	}
	public static <T> void clearsafe(List<PoolElement<T>> l) {
		for(int i=l.size()-1;i>=0;i--) {
			l.get(i).free();
			l.remove(i);
		}
	}
}
