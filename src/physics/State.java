package physics;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import com.bulletphysics.linearmath.Transform;
import com.sun.javafx.geom.transform.SingularMatrixException;

import logger.Logger;
import util.Util;

import static org.lwjgl.opengl.GL46.*;

public class State {
	private Vector3f pos=new Vector3f(0,0,0);
	private Vector3f vel=new Vector3f(0,0,0);
	private AxisAngle4f rot=new AxisAngle4f();
	
	public Vector3f pos_out=new Vector3f(0,0,0);
	public Vector3f vel_out=new Vector3f(0,0,0);
	public AxisAngle4f rot_out_aa=new AxisAngle4f();
	public Vector3f rot_out=new Vector3f(0,0,0);
	
	public void rotation(Float p_, Float y_, Float r_) {
		Quat4f rotq=Util.noPool(Util.AxisAngle(new AxisAngle4f()));
		Matrix4f rotm=new Matrix4f(rotq,new Vector3f(0,0,0),1.0f);
		if(p_!=null) {rotm.mul(new Matrix4f(Util.noPool(Util.AxisAngle(new AxisAngle4f(1,0,0,p_))),new Vector3f(0,0,0),1.0f));}
		if(y_!=null) {rotm.mul(new Matrix4f(Util.noPool(Util.AxisAngle(new AxisAngle4f(0,1,0,y_))),new Vector3f(0,0,0),1.0f));}
		if(r_!=null) {rotm.mul(new Matrix4f(Util.noPool(Util.AxisAngle(new AxisAngle4f(0,0,1,r_))),new Vector3f(0,0,0),1.0f));}
		rot=Util.noPool(Util.Quat(new Transform(rotm).getRotation(new Quat4f())));
		rotmod();
		this.rot_out_aa=new AxisAngle4f(this.rot.x,this.rot.y,this.rot.z,this.rot.angle);
		updateEuler();
	}
	public void rotation(AxisAngle4f in) {
		rot=in;
		rotmod();
		this.rot_out_aa=new AxisAngle4f(this.rot.x,this.rot.y,this.rot.z,this.rot.angle);
		updateEuler();
	}
	public void transformRotation(Matrix4f tr) {
		Quat4f rotq=Util.noPool(Util.AxisAngle(rot));
		Matrix4f rotm=new Matrix4f();
		rotm.mul(new Matrix4f(rotq,new Vector3f(0,0,0),1.0f),tr);
		rot=Util.noPool(Util.Quat(new Transform(rotm).getRotation(new Quat4f())));
		rotmod();
		this.rot_out_aa.set(this.rot.x,this.rot.y,this.rot.z,this.rot.angle);
		updateEuler();
	}
	public void velocity(Float vx_, Float vy_, Float vz_) {
		if(vx_!=null) {vel.x=vx_;}
		if(vy_!=null) {vel.y=vy_;}
		if(vz_!=null) {vel.z=vz_;}
		this.vel_out.set(this.vel.x,this.vel.y,this.vel.z);
	}
	public void position(Float x_, Float y_, Float z_) {
		if(x_!=null) {pos.x=x_;}
		if(y_!=null) {pos.y=y_;}
		if(z_!=null) {pos.z=z_;}
		this.pos_out.set(this.pos.x,this.pos.y,this.pos.z);
	}
	
	public void updateEuler() {
		
		float yaw=(float)Math.toDegrees(Math.atan2(rot.y*Math.sin(rot.angle)-
				rot.x*rot.z*(1-Math.cos(rot.angle)),
				1-(Math.pow(rot.y,2)+Math.pow(rot.z,2))*(1-Math.cos(rot.angle))));
		float roll=(float)Math.toDegrees(Math.asin(rot.x*rot.y*(1-Math.cos(rot.angle))+rot.z*Math.sin(rot.angle)));
		float pitch=(float)Math.toDegrees(Math.atan2(rot.x*Math.sin(rot.angle)-rot.y*rot.z*(1-Math.cos(rot.angle))
				,1-(Math.pow(rot.x,2)+Math.pow(rot.z,2))*(1-Math.cos(rot.angle))));
		rot_out.x=pitch;
		rot_out.y=yaw;
		rot_out.z=roll;
	}
	private Transform ret=new Transform();
	public Transform getTransform() {
		ret.set(new Matrix4f(Util.noPool(Util.AxisAngle(rot)),pos,1.0f));
		return ret;
	}
	private Matrix4f tempmat=new Matrix4f();
	public Transform getInvTransform() {
		getTransform();
		ret.getMatrix(tempmat);
		try {
			tempmat.invert();
		} catch (Exception e) {
			System.out.println(tempmat);
			Logger.log(4,e.toString());
		}
		ret.set(tempmat);
		return ret;
	}
	public void rotate(Float p_, Float y_, Float r_) {
		Quat4f rotq=Util.noPool(Util.AxisAngle(rot));
		Matrix4f rotm=new Matrix4f(rotq,new Vector3f(0,0,0),1.0f);
		if(p_!=null) {rotm.mul(new Matrix4f(Util.noPool(Util.AxisAngle(new AxisAngle4f(1,0,0,p_))),new Vector3f(0,0,0),1.0f));}
		if(y_!=null) {rotm.mul(new Matrix4f(Util.noPool(Util.AxisAngle(new AxisAngle4f(0,1,0,y_))),new Vector3f(0,0,0),1.0f));}
		if(r_!=null) {rotm.mul(new Matrix4f(Util.noPool(Util.AxisAngle(new AxisAngle4f(0,0,1,r_))),new Vector3f(0,0,0),1.0f));}
		rotm.mul(rotm,new Matrix4f(Util.noPool(Util.AxisAngle(rot)),new Vector3f(0,0,0),1.0f));
		rot=Util.noPool(Util.Quat(new Transform(rotm).getRotation(new Quat4f())));
		rotmod();
		this.rot_out_aa.set(this.rot.x,this.rot.y,this.rot.z,this.rot.angle);
		updateEuler();
	}
	public void accelerate(Float vx_, Float vy_, Float vz_) {
		if(vx_!=null) {vel.x+=vx_;}
		if(vy_!=null) {vel.y+=vy_;}
		if(vz_!=null) {vel.z+=vz_;}
		this.vel_out=new Vector3f(this.vel.x,this.vel.y,this.vel.z);
	}
	public void translate(Float x_, Float y_, Float z_) {
		if(x_!=null) {pos.x+=x_;}
		if(y_!=null) {pos.y+=y_;}
		if(z_!=null) {pos.z+=z_;}
		this.pos_out.set(this.pos.x,this.pos.y,this.pos.z);
	}
	private void rotmod() {
		Vector3f rotv=new Vector3f(rot.x,rot.y,rot.z);
		float rotm=rotv.length();
		rot.set(this.rot.x/rotm,this.rot.y/rotm,this.rot.z/rotm,Util.mod(this.rot.angle,(float)Math.PI*2.0f));
	}
	public void grender(boolean isCamera) {
		if(!isCamera) {
			glTranslatef(pos.x,pos.y,pos.z);
			glRotatef((float)Math.toDegrees(rot.angle),rot.x,rot.y,rot.z);
		}
		
		if(isCamera) {
			glRotatef((float)Math.toDegrees(rot.angle),rot.x,rot.y,rot.z);
			glTranslatef(-pos.x,-pos.y,-pos.z);
		}
	}
}
