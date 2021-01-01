package dslab.client.connection;

import dslab.client.util.EncryptionManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

public class DMTPConnection implements Connection {
    private String host;
    private int port;
    private PrintStream console;

    private Socket socket;
    private BufferedReader in;
    private PrintStream out;

    public DMTPConnection(String host, int port, PrintStream console) throws IOException {
        this.host = host;
        this.port = port;
        this.console = console;
        start();
    }

    @Override
    public void start() throws IOException {
        this.socket = new Socket(this.host, this.port);

        this.in = new BufferedReader( new InputStreamReader(this.socket.getInputStream()));
        this.out = new PrintStream(this.socket.getOutputStream());

        readCon();
    }

    @Override
    public String readCon() {
        try{
            while(!this.in.ready()){
                Thread.sleep(10);
            }
        }catch(IOException e){
            e.printStackTrace();
        }catch(InterruptedException e){
            e.printStackTrace();
        }

        String line = null;

        try{
            line = this.in.readLine();
            line = line.replaceAll("S> ", "").replaceAll("C> ", "").trim();
        }catch(IOException e){
            e.printStackTrace();
        }

        return line;
    }

    @Override
    public EncryptionManager getEncryptionManager() {
        return null;
    }

    @Override
    public void writeCon(String msg) {
        this.out.println(msg);
    }

    @Override
    public PrintStream console() {
        return this.console;
    }

    @Override
    public void stop() {
        this.writeCon("quit");
        if(this.out != null){
            this.out.close();
        }

        if(this.in != null){
            try{
                this.in.close();
            }catch(IOException e){
                e.printStackTrace();
            }
        }

        if(this.socket != null){
            try{
                this.socket.close();
            }catch(IOException e){
                e.printStackTrace();
            }
        }
    }
}
