package dslab.basic;

import java.io.IOException;
import java.net.*;

public class TCPServer implements ThreadedCommunication {
    private BasicServer server;
    private ServerSocket socket;
    private Thread thread;

    public TCPServer(String name, int port, BasicServer server) throws IOException {
        this.server = server;

        this.thread = new Thread(this,name);
        this.socket = new ServerSocket(port);

    }

    @Override
    public void start() {
        this.server.console().println(this.thread.getName() + "> Server starting... ");
        this.thread.start();
    }

    @Override
    public void shutdown() {
        this.thread.interrupt();
        if(!this.socket.isClosed()) {
            try {
                this.socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String getLocalAddress() {
        try {
            return Inet4Address.getLocalHost().getHostAddress() + ":" + this.socket.getLocalPort();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void run() {
        try{
            this.server.console().println(this.thread.getName() + "> Server started!, Listen on " + this.socket.getLocalPort());
            while(!this.thread.isInterrupted()){
                //this.server.console().println(this.thread.getName() + "> wating for clients");
                this.server.addClient(this.thread.getName(), this.socket.accept());
            }
        }catch(IOException e){
            if( !(e instanceof SocketException))
                e.printStackTrace();
        }
    }
}
