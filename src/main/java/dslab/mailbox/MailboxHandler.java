package dslab.mailbox;

import dslab.basic.Email;
import dslab.protocols.*;
import dslab.protocols.dmap.MessageAccessProtocolServer;
import dslab.protocols.dmtp.MessageTransferProtocolServer;
import dslab.util.Config;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MailboxHandler implements MessageAccessProtocolServer, MessageTransferProtocolServer {

    private Map<String, Map<Integer, Message>> mailboxes;
    private Config users;
    private String domain;
    private int idcount;

    public MailboxHandler(String domain, String userfile){
        this.domain = domain;
        this.idcount = 1;

        this.mailboxes = new ConcurrentHashMap<String, Map<Integer, Message>>();

        this.users = new Config(userfile);
    }


    @Override
    public void login(String username, String password) throws ProtocolException {
        if(username == null || password == null || username == "" || password == ""){
            throw new ProtocolException("error Username or Password empty!");
        }

        synchronized (this.users) {

            if (this.users.containsKey(username)) {
                String pwd = this.users.getString(username);
                if (!password.equals(pwd)) {
                    throw new ProtocolException("error wrong Password!");
                }
            } else {
                throw new ProtocolException("error User not found!");
            }
        }
    }

    @Override
    public Message[] list(String username) throws ProtocolException {
        if(username == null){
            throw new ProtocolException("not logged in");
        }
        if(!this.users.containsKey(username)){
            throw new ProtocolException("Username not found");
        }
        if(!this.mailboxes.containsKey(username)){
            throw new ProtocolException("no Mailbox found!");
        }
        Map<Integer,Message> tmp = this.mailboxes.get(username);
        return tmp.values().toArray(new Message[0]);
    }

    @Override
    public Message show(String username, int msgid) throws ProtocolException {
        if(username == null){
            throw new ProtocolException("not logged in");
        }
        if(!this.users.containsKey(username)){
            throw new ProtocolException("Username not found");
        }
        if(!this.mailboxes.containsKey(username)){
            throw new ProtocolException("no Mailbox found!");
        }
        Message m = this.mailboxes.get(username).get(msgid);
        if(m == null){
            throw new ProtocolException("no Message found!");
        }
        return m;
    }

    @Override
    public void delete(String username, int msgid) throws ProtocolException {
        if(username == null){
            throw new ProtocolException("not logged in");
        }
        if(!this.users.containsKey(username)){
            throw new ProtocolException("Username not found");
        }
        if(!this.mailboxes.containsKey(username)){
            throw new ProtocolException("no Mailbox found!");
        }
        this.mailboxes.get(username).remove(msgid);
    }

    @Override
    public int checkAddresses(String addresses) throws ProtocolException {
        List<String> found = this.filterAddresses(addresses);

        return found.size();
    }

    private List<String> filterAddresses(String addresses) throws ProtocolException{
        List<String> found = new ArrayList<String>();
        String[] addrs = addresses.split(",");
        String error = "", invalid = "";
        for (String addr : addrs){
            if(addr != null && addr != ""){
                if(addr.endsWith("@" + this.domain)){
                    if(Email.validateEmail(addr)){
                        String user = Email.getUsername(addr);
                        if(this.users.containsKey(user)){
                            found.add(addr);
                        }else{
                            error += "," + addr;
                        }
                    }else{
                        invalid += "," + addr;
                    }
                }
            }
        }

        if(!"".equals(invalid)){
            throw new ProtocolException("Invalid Addresses: " + invalid.substring(1), found);
        }
        if(!"".equals(error)){
            throw new ProtocolException("unknown Addresses: " + error.substring(1), found);
        }
        return found;
    }

    @Override
    public void send(Message m) throws ProtocolException{
        List<String> found = null;
        try {
            found = this.filterAddresses(m.getTo());
        }catch (ProtocolException e){
            Object o = e.getData();
            if ( o != null){
                if ( o instanceof List){
                    found = (List)o;
                }
            }

            if(found == null)
                throw e;
        }



        for(String addr : found){
            String user = Email.getUsername(addr);
            Map<Integer, Message> mailbox = this.mailboxes.get(user);
            if(mailbox == null){
                if(this.users.containsKey(user)){
                    mailbox = Collections.synchronizedMap(new HashMap<Integer, Message>());
                    this.mailboxes.put(user, mailbox);
                }else{
                    throw new ProtocolException("User " + user + " not found!");
                }
            }
            Message newMessage = m.clone();
            newMessage.setID(this.generateID());
            mailbox.put(newMessage.getID(), newMessage);
        }
    }

    private int generateID(){
        return this.idcount ++;
    }
}
