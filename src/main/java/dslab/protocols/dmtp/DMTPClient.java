package dslab.protocols.dmtp;

import at.ac.tuwien.dsg.orvell.StopShellException;
import dslab.basic.BasicServer;
import dslab.basic.TCPClient;
import dslab.protocols.*;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

public class DMTPClient extends TCPClient implements MessageTransferProtocol {


    private final MessageTransferProtocolServer protocol;


    private Message message;

    public DMTPClient(Socket socket, BasicServer server, MessageTransferProtocolServer protocol, String protocolname) throws IOException {
        super(socket, server,protocolname);
        this.protocol = protocol;
        this.message = null;
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

        switch(cmd){
            case "begin":
                begin();
                break;
            case "to":
                to(args);
                break;
            case "from":
                from(args);
                break;
            case "subject":
                subject(args);
                break;
            case "data":
                data(args);
                break;
            case "hash":
                hash(args);
                break;
            case "send":
                send();
                break;
            default:
                super.processLine(line);
        }
        this.out().print("C> ");
    }



    @Override
    public void begin() {
        this.message = new DMessage();
        this.ok();
    }

    @Override
    public void to(String address) {
        if(this.message == null){
            this.out().println("S> error No Message");
            return;
        }
        try {
            int count = this.protocol.checkAddresses(address);
            if(count > 0) {
                this.message.setTo(address);
                this.out().printf("S> ok %d%n", count);
            }else{
                this.out().println("S> error no valid Addresses found (" + address + ")");
            }
        }catch(ProtocolException e){
            Object o = e.getData();
            this.out().print("S> ");
            if(o != null){
                if( o instanceof List){
                    int count = ((List)o).size();
                    this.message.setTo(address);
                    this.out().printf("ok %d - ", count);
                }
            }
            this.out().printf("error %s%n", e.getMessage());
        }
    }

    @Override
    public void from(String address) {
        if(this.message == null){
            this.out().println("S> error No Message");
            return;
        }
        this.message.setFrom(address);
        this.ok();
    }


    @Override
    public void subject(String subject) {
        if(this.message == null){
            this.out().println("S> error No Message");
            return;
        }
        this.message.setSubject(subject);
        this.ok();
    }

    @Override
    public void data(String data) {
        if(this.message == null){
            this.out().println("S> error No Message");
            return;
        }

        this.message.setData(data);
        this.ok();

    }

    @Override
    public void hash(String hash) {
        if(this.message == null){
            this.out().println("S> error No Message");
            return;
        }

        this.message.setHash(hash);
        this.ok();
    }

    @Override
    public void send() {
        if(this.message == null){
            this.out().println("S> error No Message");
            return;
        }
        if(!this.message.validate()){
            this.out().println("S> error Message incomplete");
            return;
        }
        try {
            this.protocol.send(this.message);
            this.ok();
            this.message = null;
        }catch(ProtocolException e){
            this.out().printf("S> error %s%n", e.getMessage());
        }
    }

    @Override
    public void quit() {
        this.out().println("S> ok bye");
        this.shutdown();
        throw new StopShellException();
    }

    private void ok(){
        this.out().println("S> ok");
    }
}
