package dslab.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashMap;

import at.ac.tuwien.dsg.orvell.Shell;
import at.ac.tuwien.dsg.orvell.StopShellException;
import at.ac.tuwien.dsg.orvell.annotation.Command;
import dslab.ComponentFactory;
import dslab.client.commands.*;
import dslab.client.connection.Connection;
import dslab.client.connection.DMAPConnection;
import dslab.client.connection.DMTPConnection;
import dslab.client.util.EncryptionManager;
import dslab.protocols.DMessage;
import dslab.protocols.Message;
import dslab.util.Config;

public class MessageClient implements IMessageClient, Runnable, INBOXManager {
    private String componentId;
    private Config config;
    private Shell shell;

    private Connection mailbox;
    private EncryptionManager encryptionManager;

    private HashMap<Integer, Message> inbox;

    /**
     * Creates a new client instance.
     *
     * @param componentId the id of the component that corresponds to the Config resource
     * @param config the component config
     * @param in the input stream to read console input from
     * @param out the output stream to write console output to
     */
    public MessageClient(String componentId, Config config, InputStream in, PrintStream out) {
        this.componentId = componentId;
        this.config = config;
        this.shell = new Shell(in,out);
        this.inbox = new HashMap<Integer, Message>();
        this.encryptionManager = new EncryptionManager();
    }

    @Override
    public void run() {
        //Startup connections
        this.mailbox = new DMAPConnection(this.config.getString("mailbox.host"), this.config.getInt("mailbox.port"), this.shell.out(),encryptionManager);
        try {
            this.mailbox.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.mailbox.readCon();

        //Establish secure connection before login
        new StartSecureCommand().execute(this.mailbox, this);
        //Login
        new LoginCommand().execute(this.mailbox, this);

        this.shell.register(this);
        this.shell.setPrompt("");
        this.shell.out().println("Ready " + this.componentId);
        this.shell.run();
    }

    @Override
    @Command
    public void inbox() {
        this.shell.out().println("INBOX");
        new InboxCommand().execute(this.mailbox,this);
    }

    @Override
    @Command
    public void delete(String id) {
        this.shell.out().println("Delete " + id);
        try {
            new DeleteCommand(Integer.parseInt(id)).execute(this.mailbox, this);
        }catch(NumberFormatException e){
            e.printStackTrace();
        }
    }

    @Override
    @Command
    public void verify(String id) {
        this.shell.out().println("verify " + id );
        try {
            new VerifyCommand(Integer.parseInt(id)).execute(this.mailbox, this);
        }catch(NumberFormatException e){
            e.printStackTrace();
        }
    }

    @Override
    @Command
    public void msg(String to, String subject, String data) {
        this.shell.out().println("MSG");
        this.shell.out().println(to);
        this.shell.out().println(subject);
        this.shell.out().println(data);
        Message msg = new DMessage(this.config.getString("transfer.email"), to, subject, data);
        try {
            // TODO: fix connection refused error!
            Connection con = new DMTPConnection(this.config.getString("transfer.host"), this.config.getInt("transfer.port"), this.shell.out());
            new MsgCommand(msg).execute(con, this);
            con.stop();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    @Command
    public void shutdown() {
        new LogoutCommand().execute(this.mailbox, this);
        this.mailbox.stop();
        throw new StopShellException();
    }

    @Command
    public void startsecure() {
        if(!encryptionManager.aesActive()) {
            new StartSecureCommand().execute(this.mailbox, this);
        } else {
            this.shell.out().println("Encryption already active!");
        }
    }

    @Override
    public void updateINBOX(HashMap<Integer, Message> inbox) {
        this.inbox = inbox;
    }

    @Override
    public Message getMsg(int id) {
        return this.inbox.get(id);
    }

    @Override
    public void removeMsg(int id) {
        this.inbox.remove(id);
    }

    @Override
    public String getLogin() {
        return this.config.getString("mailbox.user") + " " +  this.config.getString("mailbox.password");
    }

    public static void main(String[] args) throws Exception {
        IMessageClient client = ComponentFactory.createMessageClient(args[0], System.in, System.out);
        client.run();
    }
}
