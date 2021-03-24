package pooling;

public class ObjectPool<T> extends AbstractObjectPool<T> { //Object pool with default behavior.

	public ObjectPool(String type, PoolInitCreator<T> p) {
		super(type, p);
	}

}
