package callback;

public interface HasSimpleCallbackListeners {
    void addSimpleListener(SimpleListener simpleListener);
    void removeSimpleListener(SimpleListener simpleListener);
}
