package pooling;

import util.Util;

public class PoolInitCreator_clone<T extends Cloneable> implements PoolInitCreator<T> {
	T prototype;
	public PoolInitCreator_clone(T p) {
		prototype=p;
	}
	@Override
	public T allocateInitValue() {
		return Util.cloneObject(prototype);
	}
	
}
