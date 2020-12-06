package dslab.transfer;

public interface DNSService {

    String lookup(String domain);

    String getLocalAddress();

    String getName();
}
