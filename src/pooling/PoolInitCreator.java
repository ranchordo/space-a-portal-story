package pooling;

public interface PoolInitCreator<T> {
	public T allocateInitValue(); //Create a new object that we can use
}
