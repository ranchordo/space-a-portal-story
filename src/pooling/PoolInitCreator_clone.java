package pooling;

import util.Util;

public class PoolInitCreator_clone<T extends Cloneable> implements PoolInitCreator<T> {  //PoolInitCreator that just clones stuff
	T prototype;
	public PoolInitCreator_clone(T p) {
		prototype=p;
	}
	@Override
	public T allocateInitValue() {
		return Util.cloneObject(prototype);
	}
//	the cloneObject method from Util is this:
//	@SuppressWarnings("unchecked")
//	public static <T extends Cloneable> T cloneObject(T i) {
//		try {
//			return (T)i.getClass().getMethod("clone").invoke(i);
//		} catch (Exception e) {
//			Logger.log(4,e.toString(),e);
//			return null;
//		}
//	}
	
}
