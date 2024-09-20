package callback;

public interface HasDataCallbackListeners<T> {
    void addDataListener(DataListener<T> dataListener);
    void removeDataListener(DataListener<T> dataListener);
}
