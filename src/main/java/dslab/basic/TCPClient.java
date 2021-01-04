package dslab.basic;

import java.io.*;
import java.net.Socket;

public class TCPClient implements ThreadedCommunication{
    private Socket socket;
    protected BasicServer server;
    private String protocolname;

    private Thread thread;

    private BufferedReader in;
    private PrintStream out;

    public TCPClient(Socket socket, BasicServer server, String protocolname) throws IOException {
        this.socket = socket;
        this.server = server;
        this.protocolname = protocolname;

        this.in = new BufferedReader( new InputStreamReader(this.socket.getInputStream()));
        this.out = new PrintStream(this.socket.getOutputStream());


        this.thread = new Thread(this, "Client-" + this.protocolname + "-" + this.socket.getPort());
    }

    public PrintStream out(){
        return this.out;
    }

    @Override
    public void start() {
        this.server.console().println(this.thread.getName() + "> starting communication...");
        this.thread.start();
    }



    @Override
    public void shutdown() {
        this.thread.interrupt();
        try{
            if(this.in != null)
                this.in.close();
        }catch(IOException e){
            e.printStackTrace();
        }
        if(this.out != null)
            this.out.close();
        if ( !(this.socket.isClosed() ) ) {
            try{
                this.socket.close();
            }catch(IOException e){
                e.printStackTrace();
            }
        }
    }

    @Override
    public void run() {
        println("ok " + this.protocolname);
        print("C> ");
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

        try {
            this.in.close();
            this.out.close();
            this.socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.server.removeClient(this);
        this.server.console().printf("%s> stoped communication!%n",this.thread.getName());
    }

    @Override
    public String getLocalAddress() {
        return this.socket.getInetAddress() + ":" + this.socket.getLocalPort();
    }

    public void processLine(String line){
        if(line.startsWith("quit")){
            println("ok bye");
            this.shutdown();
        }else{
            println("Wrong Command!");
            this.shutdown();
        }
    }

    void println(String line) {
        this.out.println(line);
    }

    void print(String line) {
        this.out.print(line);
    }
}
