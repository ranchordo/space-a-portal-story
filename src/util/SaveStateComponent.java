package util;

import java.io.Serializable;

import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;

public class SaveStateComponent implements Serializable {
	private static final long serialVersionUID = -5693590368115420481L;
	public Matrix4f transform;
	public Vector3f velocity;
}