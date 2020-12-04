package dslab.transfer;

public interface DNSService {

    String lookup(String domian);

    String getLoacalAddress();

    String getName();
}
