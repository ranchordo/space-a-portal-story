package pooling;

public interface PoolInitCreator<T> {
	public T allocateInitValue();
}
