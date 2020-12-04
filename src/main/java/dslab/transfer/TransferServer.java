package dslab.transfer;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import at.ac.tuwien.dsg.orvell.Shell;
import at.ac.tuwien.dsg.orvell.StopShellException;
import at.ac.tuwien.dsg.orvell.annotation.Command;
import dslab.ComponentFactory;
import dslab.basic.BasicServer;
import dslab.basic.Email;
import dslab.basic.TCPServer;
import dslab.basic.ThreadedCommunication;
import dslab.protocols.Message;
import dslab.protocols.ProtocolException;
import dslab.protocols.dmtp.DMTPClient;
import dslab.protocols.dmtp.MessageTransferProtocolServer;
import dslab.util.Config;

public class TransferServer implements ITransferServer, Runnable, BasicServer, MessageTransferProtocolServer, DNSService {

    private String componentId;
    private Shell shell;
    private Config config;

    private List<ThreadedCommunication> clients;
    private ThreadedCommunication dmtpserver;

    private DeliveryManager delivery;
    private Config dns;

    /**
     * Creates a new server instance.
     *
     * @param componentId the id of the component that corresponds to the Config resource
     * @param config the component config
     * @param in the input stream to read console input from
     * @param out the output stream to write console output to
     */
    public TransferServer(String componentId, Config config, InputStream in, PrintStream out) {
        // TODO
        this.componentId = componentId;
        this.config = config;

        this.shell = new Shell(in,out);
        this.shell.register(this);
        this.shell.setPrompt("");

        this.clients = Collections.synchronizedList( new ArrayList<ThreadedCommunication>());

        this.delivery = new DeliveryManager(this, this.config.getString("monitoring.host"), this.config.getInt("monitoring.port"));
        this.dns = new Config("domains.properties");
    }

    @Override
    public void run() {
        // TODO
        int port = this.config.getInt("tcp.port");

        try{
            this.dmtpserver = new TCPServer(componentId,port, this);

            this.dmtpserver.start();

            this.shell.out().printf("%s> started!%n",this.componentId);
            this.shell.run();

        }catch(IOException e){
            e.printStackTrace();
        }
        System.out.println("Exiting the shell, bye!");
    }

    @Override
    @Command
    public void shutdown() {

        this.shell.out().printf("%s> Server stopping...%n", this.componentId);

        this.dmtpserver.shutdown();
        synchronized (this.clients) {
            for (ThreadedCommunication c : this.clients) {
                c.shutdown();
            }
        }
        this.shell.out().printf("%s> Server stoped!%n", this.componentId);

        this.delivery.shutdown();

        throw new StopShellException();
    }

    @Override
    public void addClient(String name, Socket socket) {
        try {
            ThreadedCommunication c = new DMTPClient(socket, this, this, "DMTP");;
            if(c != null){
                c.start();

                synchronized (this.clients) {
                    this.clients.add(c);
                }
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    @Override
    public void removeClient(ThreadedCommunication socket) {
        synchronized (this.clients) {
            this.clients.remove(socket);
        }
    }

    @Override
    public PrintStream console() {
        return this.shell.out();
    }

    @Override
    public int checkAddresses(String addresses) throws ProtocolException {
        String[] split = addresses.split(",");
        int count = 0;
        String error = "";
        for(String addr: split){
            if(Email.validateEmail(addr)){
                count ++;
            }else{
                error += "," + addr;
            }
        }
        if(!"".equals(error)){
            throw new ProtocolException("invalid Addresses " + error.substring(1));
        }
        return count;
    }

    @Override
    public void send(Message m) throws ProtocolException {
        this.delivery.sendMessage(m);
    }

    @Override
    public String lookup(String domian) {
        return this.dns.getString(domian);
    }

    @Override
    public String getLoacalAddress() {
        return this.dmtpserver.getLocalAddress();
    }

    @Override
    public String getName() {
        return this.componentId;
    }

    public static void main(String[] args) throws Exception {
        ITransferServer server = ComponentFactory.createTransferServer(args[0], System.in, System.out);
        server.run();
    }


}
