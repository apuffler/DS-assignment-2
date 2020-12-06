package dslab.transfer;

import dslab.protocols.Message;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class DeliveryManager{
    private DNSService service;
    private ThreadPoolExecutor executorService;

    private String statistic_address;
    private int statistic_port;


    public DeliveryManager(DNSService service, String statistic_address, int statistic_port){
        this.service = service;
        executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(5);
        this.statistic_address = statistic_address;
        this.statistic_port = statistic_port;

    }

    public void sendMessage(Message message){

        DeliveryTask task = new DeliveryTask(message, this.service,this);
        executorService.execute(task);

    }

    public void sendStatistic(String sender){
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket();

            String send = this.service.getLocalAddress() + " " + sender;
            byte[] buffer = send.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(this.statistic_address), this.statistic_port);
            socket.send(packet);
        } catch (UnknownHostException e) {
            System.out.println("Cannot connect to host: " + e.getMessage());
        } catch (SocketException e) {
            System.out.println("SocketException: " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(socket != null && !socket.isClosed()){
                socket.close();
            }
        }
    }

    public void shutdown(){
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }
}
