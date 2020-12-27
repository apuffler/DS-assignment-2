package dslab.client.commands;

import dslab.client.INBOXManager;
import dslab.client.connection.Connection;
import dslab.protocols.Message;

public class MsgCommand implements Command{
    private Connection con;
    private INBOXManager manager;

    private Message msg;

    public MsgCommand(Message msg){
        this.msg = msg;
    }

    @Override
    public void execute(Connection con, INBOXManager manager) {
        this.con = con;
        this.manager = manager;
        this.run();
    }

    @Override
    public void run() {
        this.con.writeCon("begin");

        String line = this.con.readCon();
        if(!"ok".equals(line)){
            this.con.console().printf("%s: %s%n", this.getClass().getName(),line);
            return;
        }

        for( String cmd : this.msg.generateCommands()){
            this.con.writeCon(cmd);
            line = this.con.readCon();
            if(!line.contains("ok")){
                this.con.console().printf("%s: %s%n", this.getClass().getName(),line);
                return;
            }
        }

        this.con.writeCon("send");
        line = this.con.readCon();
        if("ok".equals(line)){
            this.con.console().printf("%s%n", line);
        }else{
            this.con.console().printf("%s: %s%n", this.getClass().getName(),line);
            return;
        }
    }
}
