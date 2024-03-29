package dslab.client.commands;

import dslab.client.INBOXManager;
import dslab.client.connection.Connection;
import dslab.protocols.DMessage;
import dslab.protocols.Message;

import java.util.HashMap;
import java.util.Stack;

public class InboxCommand implements Command{

    private Connection con;
    private INBOXManager manager;

    @Override
    public void execute(Connection con, INBOXManager manager) {
        this.con = con;
        this.manager = manager;
        this.run();
    }

    @Override
    public void run() {
        this.con.writeCon("list");

        Stack<Integer> ids = new Stack<Integer>();

        //Laden aller Mail mit List Command
        do{
            String line = this.con.readCon();

            if(line.toUpperCase().contains("ERROR") && !line.contains(("ERROR Domain not found"))){
                this.con.console().println(line);
                return;
            }
            if(line.toUpperCase().contains("NO MAILBOX FOUND!")){
                this.con.console().printf("NO Messages!%n");
                break;
            }

            if("ok".equals(line)){
                break;
            }

            try{ // TODO: Fix this with line 'no mailbox found!'
                int pos = line.indexOf(' ');
                int id = Integer.parseInt(line.substring(0,pos).trim());
                ids.push(id);
            }catch(NumberFormatException e){ e.printStackTrace(); }

        }while(true);
        HashMap<Integer, Message> inbox = new HashMap<Integer, Message>();
        //Laden der einzelnen Mails
        try {
            for (int id : ids) {
                Message msg = readMessage(id);
                msg.setID(id);
                printMessage(msg);
                inbox.put(id, msg);
            }
        }catch(Exception e){
            e.printStackTrace();
        }

        if(inbox.size() == 0){
            this.con.console().printf("NO Messages!%n");
        }

        this.manager.updateINBOX(inbox);
    }

    private void printMessage(Message msg){
        String txt = "id: %s%nsender: %s%nrecipients: %s%nsubject: %s%ndata: %s%n%s%n";
        this.con.console().printf(txt,msg.getID(), msg.getFrom(), msg.getTo(), msg.getSubject(),msg.getData(),"#".repeat(100));
        //this.con.console().printf("HASH: %s%n",msg.getHash());
    }

    private Message readMessage(int id){
        this.con.writeCon("show " + id);

        Message msg = new DMessage();

        do{
            String line = this.con.readCon();

            if(line.toUpperCase().contains("ERROR") && !line.contains(("ERROR Domain not found"))){
                this.con.console().println(line);
                return null;
            }

            if("ok".equals(line)){
                break;
            }

            int pos = line.indexOf(' ');
            String cmd = line.substring(0,pos).trim();
            String arg = line.substring(pos).trim();

            switch (cmd.toLowerCase()){
                case "from":
                    msg.setFrom(arg);
                    break;
                case "to":
                    msg.setTo(arg);
                case "subject":
                    msg.setSubject(arg);
                    break;
                case "data":
                    msg.setData(arg);
                    break;
                case "hash":
                    msg.setHash(arg);
                    break;
            }
        }while(true);
        return msg;
    }
}
