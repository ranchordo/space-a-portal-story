package graphics.handlers;

import java.nio.FloatBuffer;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;

import org.lwjgl.BufferUtils;

import com.bulletphysics.linearmath.Transform;

import graphics.GObject;
import graphics.RenderUtils;
import graphics.Renderer;
import logger.Logger;
import objects.Thing;
import util.ComputeShader;
import util.SSBO;
import util.Util;

public class RTXDataCoordinator {
	public static final int NArray_size=16;
	private static FloatBuffer handleBufferCreation(FloatBuffer buffer, int size, SSBO ssbo) {
		if(ssbo!=null) {ssbo.size_desynced=false;}
		if(buffer==null || buffer.capacity()!=size) {
			if(buffer!=null) {
				Logger.log(1,"RTXDataCoordinator: Needing to allocate a new buffer. Stop shifting vertex data size. If you're seeing this every frame, your performance is gonna be crap. Capacity was "+buffer.capacity()+", in was "+size);
			}
			buffer=BufferUtils.createFloatBuffer(size);
			onNewFloatBufferAllocation(buffer, ssbo);
		}
		buffer.clear();
		return buffer;
	}
	private static void onNewFloatBufferAllocation(FloatBuffer newbuffer, SSBO ssbo) {
		if(ssbo!=null) {ssbo.size_desynced=true;}
	}
	private static Matrix4f trmat=new Matrix4f();
	private static FloatBuffer geoData;
	private static FloatBuffer matData;
	private static FloatBuffer nonArray;
	private static float[] mat=new float[16];
	public static void handleSSBOData(ComputeShader sh) {
		int alloc=0;
		for(Thing thing : Renderer.things) {
			for(GObject geo : thing.getResources()) {
				alloc+=geo.getNumTris();
			}
		}
		SSBO geo_data=sh.getSSBOMappings().get("geo_data");
		SSBO mat_data=sh.getSSBOMappings().get("mat_data");
		SSBO nar_data=sh.getSSBOMappings().get("narray_data");
		geoData=handleBufferCreation(geoData,alloc*GObject.RTXVertexSize, geo_data);
		matData=handleBufferCreation(matData,alloc*16, mat_data);
		nonArray=handleBufferCreation(nonArray,NArray_size, nar_data);
		int i=0;
		for(Thing thing : Renderer.things) {
			for(GObject geo : thing.getResources()) {
				Transform tr=geo.getTransform();
				if(tr==null || thing.type.equals("Player")) {continue;}
				tr.getMatrix(trmat);
				geo.RTXAddVertexData(geoData,i);
				i++;
				trmat.transpose();
				matData.put(trmat.m00);
				matData.put(trmat.m01);
				matData.put(trmat.m02);
				matData.put(trmat.m03);
				
				matData.put(trmat.m10);
				matData.put(trmat.m11);
				matData.put(trmat.m12);
				matData.put(trmat.m13);
				
				matData.put(trmat.m20);
				matData.put(trmat.m21);
				matData.put(trmat.m22);
				matData.put(trmat.m23);
				
				matData.put(trmat.m30);
				matData.put(trmat.m31);
				matData.put(trmat.m32);
				matData.put(trmat.m33);
			}
		}
		Renderer.camtr.getOpenGLMatrix(mat);
		Util.asFloatBuffer(mat,nonArray);
		geoData.flip();
		matData.flip();
		
		geoData.limit(geoData.capacity());
		matData.limit(matData.capacity());
		nonArray.limit(nonArray.capacity());
		if(geo_data!=null) {if(geo_data.size_desynced) {sh.initSSBOData(geoData.capacity()*4,geo_data);}}
		if(mat_data!=null) {if(mat_data.size_desynced) {sh.initSSBOData(matData.capacity()*4,mat_data);}}
		if(nar_data!=null) {if(nar_data.size_desynced) {sh.initSSBOData(nonArray.capacity()*4,nar_data);}}
		if(geo_data==null) {geo_data=sh.generateNewSSBO("geo_data", geoData.capacity()*4);}
		if(mat_data==null) {mat_data=sh.generateNewSSBO("mat_data", matData.capacity()*4);}
		if(nar_data==null) {nar_data=sh.generateNewSSBO("narray_data", nonArray.capacity()*4);}
		sh.updateSSBOData(geoData,geo_data);
		sh.updateSSBOData(matData,mat_data);
		sh.updateSSBOData(nonArray,nar_data);
	}
}
