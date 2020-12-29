package dslab.client.commands;

import dslab.client.INBOXManager;
import dslab.client.MessageVerifier;
import dslab.client.connection.Connection;
import dslab.protocols.Message;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class VerifyCommand implements Command{
    private Connection con;
    private INBOXManager manager;

    private int verify_id;

    public VerifyCommand(int verify_id){
        this.verify_id = verify_id;
    }

    @Override
    public void execute(Connection con, INBOXManager manager) {
        this.con = con;
        this.manager = manager;
        this.run();
    }

    @Override
    public void run() {
        Message msg = this.manager.getMsg(this.verify_id);

        try {
            if(MessageVerifier.getInstance().verifyMessage(msg)){
                this.con.console().println("ok");
            }else{
                this.con.console().println("error");
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
