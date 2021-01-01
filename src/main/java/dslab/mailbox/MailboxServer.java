package dslab.mailbox;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import at.ac.tuwien.dsg.orvell.Shell;
import at.ac.tuwien.dsg.orvell.StopShellException;
import at.ac.tuwien.dsg.orvell.annotation.Command;
import dslab.ComponentFactory;
import dslab.basic.BasicServer;
import dslab.basic.TCPServer;
import dslab.basic.ThreadedCommunication;
import dslab.protocols.*;
import dslab.protocols.dmap.DMAPClient;
import dslab.protocols.dmtp.DMTPClient;
import dslab.util.Config;

public class MailboxServer implements IMailboxServer, Runnable, BasicServer {

    private static final String dmap_name = "DMAP2.0", dmtp_name = "DMTP";

    private final String name;
    private final Config config;
    private final Shell shell;

    //private Config users;
    private final List<ThreadedCommunication> clients;
    private ThreadedCommunication dmapServer, dmtpServer;

    private MailboxHandler mailbox;

    /**
     * Creates a new server instance.
     *
     * @param componentId the id of the component that corresponds to the Config resource
     * @param config the component config
     * @param in the input stream to read console input from
     * @param out the output stream to write console output to
     */
    public MailboxServer(String componentId, Config config, InputStream in, PrintStream out) {
        this.config = config;

        this.name = this.config.getString("domain");

        this.shell = new Shell(in,out);


        this.clients = new ArrayList<ThreadedCommunication>();

    }

    @Override
    public void run() {

        this.shell.register(this);
        this.shell.setPrompt("");

        this.shell.out().printf("%s> starting...%n",this.name);
        this.mailbox = new MailboxHandler(this.name, this.config.getString("users.config"));


        int dmapPort = this.config.getInt("dmap.tcp.port");
        int dmtpPort = this.config.getInt("dmtp.tcp.port");


        //this.users = new Config(this.config.getString("users.config"));

        try {
            this.dmapServer = new TCPServer(MailboxServer.dmap_name,dmapPort,this);
            this.dmtpServer = new TCPServer(MailboxServer.dmtp_name,dmtpPort,this);

            this.dmapServer.start();
            this.dmtpServer.start();

            this.shell.out().printf("%s> started!%n",this.name);
            this.shell.run();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Exiting the shell, bye!");
    }

    @Override
    @Command
    public void shutdown() {
        // TODO
        this.shell.out().printf("%s> Server stopping...%n", this.name);
        this.dmtpServer.shutdown();
        this.dmapServer.shutdown();

        synchronized (this.clients) {
            for (ThreadedCommunication c : this.clients) {
                c.shutdown();
            }
        }
        this.shell.out().printf("%s> Server stoped!%n", this.name);
        //Shell beenden
        throw new StopShellException();
    }

    @Override
    public void addClient(String name, Socket socket) {
        try {
            ThreadedCommunication c = null;
            if (MailboxServer.dmap_name.equals(name)) {
                c = new DMAPClient(socket, this, this.mailbox, MailboxServer.dmap_name);
            }
            if (MailboxServer.dmtp_name.equals(name)) {
                c = new DMTPClient(socket, this, this.mailbox, MailboxServer.dmtp_name);
            }
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
    public void removeClient( ThreadedCommunication socket) {
        synchronized (this.clients) {
            this.clients.remove(socket);
        }
    }

    @Override
    public PrintStream console() {
        return this.shell.out();
    }


    public static void main(String[] args) throws Exception {
        IMailboxServer server = ComponentFactory.createMailboxServer(args[0], System.in, System.out);
        server.run();
    }

    public String getName() {
      return name;
    }
}
