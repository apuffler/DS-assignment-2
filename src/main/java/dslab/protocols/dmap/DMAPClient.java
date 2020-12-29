package dslab.protocols.dmap;

import at.ac.tuwien.dsg.orvell.StopShellException;
import dslab.basic.BasicServer;
import dslab.basic.TCPClient;
import dslab.protocols.Message;
import dslab.protocols.ProtocolException;

import java.io.IOException;
import java.net.Socket;

public class DMAPClient extends TCPClient implements MessageAccessProtocol {


    private final MessageAccessProtocolServer protocol;

    private String username;


    public DMAPClient(Socket socket, BasicServer server, MessageAccessProtocolServer protocol, String protocolname) throws IOException {
        super(socket, server, protocolname);
        this.protocol = protocol;

        this.username = null;
    }

    @Override
    public void processLine(String line){

        int pos = line.indexOf(' ');
        String cmd = line.trim();
        String args = "";
        if(pos != -1) {
            cmd = line.substring(0, pos).trim();
            args = line.substring(pos).trim();
        }

        switch (cmd){
            case "list":
                list();
                break;
            case "show":
            case "delete":
                int id = -1;
                try {
                    id = Integer.parseInt(args);
                    if("show".equals(cmd)){
                        show(id);
                    }else {
                        delete(id);
                    }
                }catch(NumberFormatException e){
                    this.out().println("S> No Number!");
                }
                break;
            case "login":
                pos = args.indexOf(' ');
                if ( pos != -1){
                    this.login(args.substring(0,pos).trim(), args.substring(pos).trim());
                }else{
                    this.out().println("S> Username or Password missing!");
                }
                break;
            case "logout":
                this.logout();
                break;
            default:
                super.processLine(line);

        }
        this.out().print("C> ");

    }

    @Override
    public void login(String username, String password) {
        try{
            this.protocol.login(username, password);
            this.username = username;
            this.out().println("S> ok");
        }catch(ProtocolException e){
            this.out().println("S> " + e.getMessage());
        }
    }

    @Override
    public void list() {
        if(this.username == null){
            this.out().println("S> error not logged in");
            return;
        }
        try{
            Message[] messages = this.protocol.list(this.username);
            for(Message m : messages){
                this.out().printf("S> %d %s %s%n", m.getID(),m.getFrom(), m.getSubject());
            }
            this.out().println("S> ok");
        }catch(ProtocolException e){
            this.out().println("S> " + e.getMessage());
        }
    }

    @Override
    public void show(int msgid) {
        if(this.username == null){
            this.out().println("S> error not logged in");
            return;
        }
        try{
            Message m = this.protocol.show(this.username,msgid);
            this.out().printf("S> from %s%n",m.getFrom());
            this.out().printf("S> to %s%n",m.getTo());
            this.out().printf("S> subject %s%n",m.getSubject());
            this.out().printf("S> data %s%n",m.getData());
            this.out().printf("S> hash %s%n",m.getHash());
            this.out().println("S> ok");
        }catch(ProtocolException e){
            this.out().printf("S> %s%n" , e.getMessage());
        }
    }

    @Override
    public void delete(int msgid) {
        if(this.username == null){
            this.out().println("S> error not logged in");
            return;
        }
        try{
            this.protocol.delete(this.username, msgid);
            this.out().println("S> ok");
        }catch(ProtocolException e){
            this.out().println("S> " + e.getMessage());
        }
    }

    @Override
    public void logout() {
        if(this.username == null){
            this.out().println("S> error not logged in");
            return;
        }
        this.username = null;
        this.out().printf("S> ok%n");
    }

    @Override
    public void quit() {
        this.out().println("S> ok bye");
        this.shutdown();
        throw new StopShellException();
    }
}
