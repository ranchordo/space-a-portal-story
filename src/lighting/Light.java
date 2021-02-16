package lighting;

import java.io.Serializable;

import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;

public class Light implements Serializable {
	private static final long serialVersionUID = -2091904905510317114L;
	public static final int LIGHT_NULL=0;
	public static final int LIGHT_AMBIENT=1;
	public static final int LIGHT_DIRECTION=2;
	public static final int LIGHT_POSITION=3;
	public Light(int type_, float x, float y, float z, float r, float g, float b, float a) {
		this.intensity=new Vector4f(r,g,b,a);
		this.prop=new Vector3f(x,y,z);
		if(type_==LIGHT_DIRECTION) {
			this.prop.normalize();
		}
		this.type=type_;
	}
	public Light(int type_, Vector3f prop_, Vector4f intensity_) {
		this.intensity=intensity_;
		this.prop=prop_;
		this.type=type_;
	}
	int type=0;
	public Vector4f intensity;
	public Vector3f prop;
	public int propPointer=0;
	public int intensityPointer=0;
}
