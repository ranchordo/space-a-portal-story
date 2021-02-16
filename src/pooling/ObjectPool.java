package pooling;

public class ObjectPool<T> extends AbstractObjectPool<T> {

	public ObjectPool(String type, PoolInitCreator<T> p) {
		super(type, p);
	}

}
