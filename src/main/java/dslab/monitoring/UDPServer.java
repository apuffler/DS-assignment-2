package dslab.monitoring;

import dslab.basic.ThreadedCommunication;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class UDPServer implements ThreadedCommunication {
    private static final int bufferlength = 1024;
    private int port;
    private Thread thread;
    private DatagramSocket socket;
    private IMonitoringServer server;

    public UDPServer(int port, IMonitoringServer server){
        this.port = port;
        this.server = server;
    }

    @Override
    public void start() {
        this.thread = new Thread(this);
        this.thread.start();
    }

    @Override
    public void shutdown() {

        this.thread.interrupt();
        this.socket.close();
    }

    @Override
    public void run() {
        byte[] buffer;
        DatagramPacket packet;
        try {
            this.socket = new DatagramSocket(this.port);
            while(!this.thread.isInterrupted()){
                buffer = new byte[UDPServer.bufferlength];

                packet = new DatagramPacket(buffer,buffer.length);


                this.socket.receive(packet);

                this.server.processData(new String(packet.getData()));
            }
        } catch (SocketException e) {
            if(!e.getMessage().startsWith("Socket closed"))
                e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally{
            if(this.socket != null && !this.socket.isClosed()){
                this.socket.close();
            }
        }
    }

    @Override
    public String getLocalAddress() {
        return this.socket.getInetAddress() + ":" + this.socket.getLocalPort();
    }
}
