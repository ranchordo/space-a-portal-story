package util;

import javax.vecmath.Point3f;
import javax.vecmath.Vector2f;

import lepton.optim.objpoollib.ObjectPool;
import lepton.optim.objpoollib.PoolInitCreator_clone;

public class AdditionalVecmathPools {
	public static ObjectPool<Point3f> point3f=new ObjectPool<Point3f>("Point3f",new PoolInitCreator_clone<Point3f>(new Point3f()));
	public static ObjectPool<Vector2f> vector2f=new ObjectPool<Vector2f>("Vector2f",new PoolInitCreator_clone<Vector2f>(new Vector2f()));
}
