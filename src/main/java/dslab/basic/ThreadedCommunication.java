package dslab.basic;

public interface ThreadedCommunication extends Runnable{

    void start();
    void shutdown();

    String getLocalAddress();

    @Override
    void run();
}
