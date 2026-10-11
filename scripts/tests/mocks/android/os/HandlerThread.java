package android.os;

/** Small host-side stand-in for the Android worker thread used by EventBus. */
public class HandlerThread extends Thread {
    public static final int NORM_PRIORITY = Thread.NORM_PRIORITY;
    private final Looper looper = Looper.getMainLooper();

    public HandlerThread(String name, int priority) {
        super(name);
        setPriority(priority);
    }

    @Override
    public synchronized void start() {
        // E2E tests execute deterministically on the calling thread.
    }

    public Looper getLooper() {
        return looper;
    }
}
