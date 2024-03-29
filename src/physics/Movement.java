package physics;

import static org.lwjgl.glfw.GLFW.*;

import java.nio.DoubleBuffer;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import org.lwjgl.BufferUtils;

import com.bulletphysics.linearmath.Transform;

import game.Main;
import game.PlayerInitializer;
import graphics.Camera;

import static game.Main.in;
import lepton.util.LeptonUtil;
import objects.Player;

public class Movement {
	private static long win;
	public static void initHandler(long win) {
		Movement.win=win;
	}

	static double msY;
	static double msX;
	public static void initMovement() {
		DoubleBuffer mX=BufferUtils.createDoubleBuffer(1);
		DoubleBuffer mY=BufferUtils.createDoubleBuffer(1);
		glfwGetCursorPos(win,mX,mY);
		msX=mX.get(0);
		msY=mY.get(0);
	}
	public static Vector3f force(Vector3f targ, Vector3f curr, float scale, Vector3f ret) {
		ret.set(targ);
		ret.sub(curr);
		ret.scale(PlayerInitializer.player.geo.p.mass*scale);
		return ret;
	}
	public static float sensitivity=0.2f;
	public static void rotate(float x, float y) {
		msX-=sensitivity*x;
		msY-=sensitivity*y;
	}
	private static float proty=0;
	public static Matrix4f post=new Matrix4f(LeptonUtil.noPool(LeptonUtil.AxisAngle(new AxisAngle4f(1,0,0,0))),new Vector3f(0,0,0),1.0f);
	private static DoubleBuffer mX;
	private static DoubleBuffer mY;
	private static Transform player=new Transform();
	private static Matrix3f player_transform=new Matrix3f();
	
	private static Vector3f targ=new Vector3f();
	private static Vector3f target=new Vector3f();
	private static Vector3f linvel=new Vector3f();
	private static Vector3f dot2=new Vector3f();
	private static Vector3f appImpulse=new Vector3f();
	private static Vector3f frcout=new Vector3f();
	public static void movement() {
		Camera camera=Main.camera;
		Player player_d=PlayerInitializer.player;
		player_d.updateOnFloor();
//		player_d.onFloor=true;
		float df=4.0f;
		float frc_scale=7.0f;
		boolean inputPressed=false;
		player=PlayerInitializer.player.geo.p.getTransform();
		player.getMatrix(new Matrix4f()).getRotationScale(player_transform);
		if(player_d.onFloor && !player_d.isInGodMode()) {
			targ.set(0,0,0);
			if(in.i(GLFW_KEY_W)) {
				inputPressed=true;
				target.set(0,0,-1);
				player_transform.transform(target);
				targ.add(target);
			}
			if(in.i(GLFW_KEY_S)) {
				inputPressed=true;
				target.set(0,0,1);
				player_transform.transform(target);
				targ.add(target);
			}
			if(in.i(GLFW_KEY_A)) {
				inputPressed=true;
				target.set(-1,0,0);
				player_transform.transform(target);
				targ.add(target);
			}
			if(in.i(GLFW_KEY_D)) {
				inputPressed=true;
				target.set(1,0,0);
				player_transform.transform(target);
				targ.add(target);
			}
			if(in.i(GLFW_KEY_SPACE)) {
				Vector3f cd=player_d.getCurrDown();
				float mul=5.0f*PlayerInitializer.player.geo.p.mass;
				dot2.set(-cd.x,-cd.y,-cd.z);
				appImpulse.set(-cd.x*mul,-cd.y*mul,-cd.z*mul);
				if((PlayerInitializer.player.geo.p.body.getLinearVelocity(linvel).dot(dot2))<=0.1f) {
					PlayerInitializer.player.geo.p.body.applyCentralImpulse(
							appImpulse);
				}
			}
			targ.scale(df);
			PlayerInitializer.player.geo.p.body.applyCentralForce(
					Movement.force(targ, //Target
							PlayerInitializer.player.geo.p.body.getLinearVelocity(new Vector3f()),frc_scale,frcout)); //Current
		} else if(!player_d.onFloor && !player_d.isInGodMode()) {
			df=120.0f;
			if(in.i(GLFW_KEY_W)) {
				inputPressed=true;
				target.set(0,0,-df);
				player_transform.transform(target);
				PlayerInitializer.player.geo.p.body.applyCentralForce(target);
			}
			if(in.i(GLFW_KEY_S)) {
				inputPressed=true;
				target.set(0,0,df);
				player_transform.transform(target);
				PlayerInitializer.player.geo.p.body.applyCentralForce(target);
			}
			if(in.i(GLFW_KEY_A)) {
				inputPressed=true;
				target.set(-df,0,0);
				player_transform.transform(target);
				PlayerInitializer.player.geo.p.body.applyCentralForce(target);
			}
			if(in.i(GLFW_KEY_D)) {
				inputPressed=true;
				target.set(df,0,0);
				player_transform.transform(target);
				PlayerInitializer.player.geo.p.body.applyCentralForce(target);
			}
			
			
			
			
		} else if(player_d.isInGodMode()) {
			df=6.0f;
			if(in.i(GLFW_KEY_LEFT_CONTROL)) {
				df=15.0f;
			}
			if(in.i(GLFW_KEY_W)) {
				inputPressed=true;
				target.set(0,0,-df);
				player_transform.transform(target);
				PlayerInitializer.player.geo.p.body.applyCentralForce(
						Movement.force(target, //Target
								PlayerInitializer.player.geo.p.body.getLinearVelocity(linvel),frc_scale,frcout)); //Current
			}
			if(in.i(GLFW_KEY_S)) {
				inputPressed=true;
				target.set(0,0,df);
				player_transform.transform(target);
				PlayerInitializer.player.geo.p.body.applyCentralForce(
						Movement.force(target, //Target
								PlayerInitializer.player.geo.p.body.getLinearVelocity(linvel),frc_scale,frcout)); //Current
			}
			if(in.i(GLFW_KEY_A)) {
				inputPressed=true;
				target.set(-df,0,0);
				player_transform.transform(target);
				PlayerInitializer.player.geo.p.body.applyCentralForce(
						Movement.force(target, //Target
								PlayerInitializer.player.geo.p.body.getLinearVelocity(linvel),frc_scale,frcout)); //Current
			}
			if(in.i(GLFW_KEY_D)) {
				inputPressed=true;
				target.set(df,0,0);
				player_transform.transform(target);
				PlayerInitializer.player.geo.p.body.applyCentralForce(
						Movement.force(target, //Target
								PlayerInitializer.player.geo.p.body.getLinearVelocity(linvel),frc_scale,frcout)); //Current
			}
			if(in.i(GLFW_KEY_SPACE)) {
				inputPressed=true;
				target.set(0,df,0);
				player_transform.transform(target);
				PlayerInitializer.player.geo.p.body.applyCentralForce(
						Movement.force(target, //Target
								PlayerInitializer.player.geo.p.body.getLinearVelocity(linvel),frc_scale,frcout)); //Current
			}
			if(in.i(GLFW_KEY_LEFT_SHIFT)) {
				inputPressed=true;
				target.set(0,-df,0);
				player_transform.transform(target);
				PlayerInitializer.player.geo.p.body.applyCentralForce(
						Movement.force(target, //Target
								PlayerInitializer.player.geo.p.body.getLinearVelocity(linvel),frc_scale,frcout)); //Current
			}
			if(!inputPressed) {
				PlayerInitializer.player.geo.p.body.applyCentralForce(
						Movement.force(new Vector3f(0.0f,0.0f,0.0f), //Target
								PlayerInitializer.player.geo.p.body.getLinearVelocity(linvel),frc_scale,frcout)); //Current
			}
		}
		if(mX==null) {mX=BufferUtils.createDoubleBuffer(1);}
		if(mY==null) {mY=BufferUtils.createDoubleBuffer(1);}
		mX.clear();
		mY.clear();
		glfwGetCursorPos(win,mX,mY);
		float nx=LeptonUtil.mod(sensitivity*(float)(mY.get(0)-msY),360);
		if(nx<270 && nx>180) {
			msY=-((-90.0f/sensitivity)-mY.get(0));
			nx=sensitivity*(float)(mY.get(0)-msY);
		}
		if(nx>90 && nx<180) {
			msY=-((90.0f/sensitivity)-mY.get(0));
			nx=sensitivity*(float)(mY.get(0)-msY);
		}
		float nroty=-(float)Math.toRadians(sensitivity*(float)(mX.get(0)-msX));

		if(player_d.bindToObject) {player_d.rotY(nroty-proty);}

		proty=nroty;
		if(player_d.bindToObject) {
			Transform tr=PlayerInitializer.player.geo.p.getTransform();
			AxisAngle4f axisAngle=LeptonUtil.noPool(LeptonUtil.Quat(tr.getRotation(new Quat4f())));
			Vector3f p=player_d.getCameraPositioning();
			camera.position(p.x,p.y,p.z);
			camera.rotation(new AxisAngle4f(axisAngle.x,axisAngle.y,axisAngle.z,axisAngle.angle));
		}
		camera.transformRotation(new Matrix4f(LeptonUtil.noPool(LeptonUtil.AxisAngle(new AxisAngle4f(1,0,0,(float)Math.toRadians(-nx)))),new Vector3f(0,0,0),1.0f));
	}
}
