package dslab.client;

import dslab.basic.ThreadedCommunication;
import dslab.protocols.DMessage;
import dslab.protocols.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.nio.Buffer;
import java.util.*;

public class DMAPConnectionOLD implements Runnable {
    private String host;
    private int port;
    private String user, pwd;

    private Thread thread;

    private boolean ready = false;

    private Socket socket;
    private BufferedReader in;
    private PrintStream out;

    private String lastCMD;
    private Map<Integer,Message> inbox;
    private Stack<Integer> ids;
    private Message current_Message;

    public DMAPConnectionOLD(String host, int port, String user, String pwd){
        this.host = host;
        this.port = port;
        this.user = user;
        this.pwd = pwd;
    }

    public void start() throws IOException {
        this.socket = new Socket(this.host, this.port);

        this.in = new BufferedReader( new InputStreamReader(this.socket.getInputStream()));
        this.out = new PrintStream(this.socket.getOutputStream());

        this.thread = new Thread(this);
        this.thread.start();

        this.startTSL();
    }

    private void startTSL(){
        this.lastCMD = "TSL";
    }

    private void login(){
        this.lastCMD = "login";
        this.out.printf("login %s %s%n", this.user, this.pwd);
    }

    public Collection<Message> inbox(){
        this.lastCMD = "INBOX1";
        this.inbox = new HashMap<Integer, Message>();
        this.ids = new Stack<Integer>();
        this.out.println("list");
        try {
            while  ( this.lastCMD != null){

                    Thread.sleep(10);

            }
            return this.inbox.values();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }



    @Override
    public void run() {

        try{
            while(!this.thread.isInterrupted()){

                while(!this.in.ready()){
                    try {
                        Thread.sleep(10);
                    }catch(InterruptedException e){}
                }
                processLine(this.in.readLine());
            }
        }catch(IOException e){
            //e.printStackTrace();
        }
    }

    private void processLine(String line){
        if(lastCMD == null)
            return;
        line = line.replaceAll("S> ", "").replaceAll("C> ", "").trim();
        System.out.printf("LINE >%s<%n", line);
        switch (this.lastCMD){
            case "TSL":
                processTSL(line);
                break;
            case "login":
                processLogin(line);
                break;
            case "INBOX1":

                processINBOX1(line);
                break;
            case "INBOX2":
                processINBOX2(line);
        }
    }

    private void processTSL(String line){
        if (line.contains("ok")){
            this.lastCMD = null;
            this.login();

        }
    }

    private void processLogin(String line){
        if ("ok".equals(line)){
            this.lastCMD = null;
        }else{
            //TODO Fehler
        }
    }

    private void processINBOX1(String line){
        if("ok".equals(line.trim())){
            this.lastCMD = "INBOX2";
            processINBOX2(line);
            return;
        }

        int index = line.indexOf(' ');
        if (index != -1){
            try{
                String arg = line.substring(0,index).trim();
                int id = Integer.parseInt(arg);
                this.ids.push(id);
            }catch(NumberFormatException e){
                e.printStackTrace();
            }
        }
    }

    private void processINBOX2(String line){
        if("ok".equals(line.trim())){
            if(!this.ids.empty()){
                this.current_Message = new DMessage();
                this.current_Message.setID(this.ids.pop());
                this.inbox.put(this.current_Message.getID(), this.current_Message);
                this.out.printf("show %s%n", this.current_Message.getID());
            }else {
                this.lastCMD = null;
            }
        }
        int index = line.indexOf(' ');
        if (index != -1){
            String cmd = line.substring(0,index).trim();
            String arg = line.substring(index).trim();
            System.out.printf("cmd >%s<%narg >%s<%n", cmd, arg);
            switch (cmd.toLowerCase()){
                case "from":
                    this.current_Message.setFrom(arg);
                    break;
                case "to":
                    this.current_Message.setTo(arg);
                case "subject":
                    this.current_Message.setSubject(arg);
                    break;
                case "data":
                    this.current_Message.setData(arg);
                    break;
            }
        }
    }
}
