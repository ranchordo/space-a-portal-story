package util;

import javax.vecmath.Vector3f;

public class Util {
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
}
