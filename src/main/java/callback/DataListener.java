package callback;

public interface DataListener<T> {
    //TODO: implement what promise is like: a callback but it can fail.
    void call(T data);
}
