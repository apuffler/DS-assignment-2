package dslab.transfer;

import dslab.basic.Email;
import dslab.basic.ThreadedCommunication;
import dslab.protocols.DMessage;
import dslab.protocols.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class DeliveryTask implements Runnable{

    private final Message message;

    private final DNSService service;
    private final DeliveryManager manager;


    public DeliveryTask(Message message, DNSService service, DeliveryManager manager){
        this.message = message;
        this.service = service;
        this.manager = manager;
    }
    @Override
    public void run() {

        List<String> domains = readDomains();

        if(domains != null){
            for(String ip : domains){
                try {
                    sendMessage(ip);
                }catch(IOException e){

                }catch(InterruptedException e){

                }
            }
        }
    }

    private void sendMessage(String address) throws IOException, InterruptedException {
        List<String> commands = this.message.generateCommands();
        String[] split = address.split(":");

        String ip = split[0];
        int port = Integer.parseInt(split[1]);

        Socket socket = new Socket(ip,port);

        PrintStream out= new PrintStream(socket.getOutputStream());
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        String line = wait(in);
        if(checkOk(line)){

            if(checkOk(command("begin", in,out))){
                for(String cmd : commands){
                    line = command(cmd,in,out);
                    if(checkError(line)){
                        if(cmd.startsWith("to")){
                            String from = "mailer@" + this.service.getLocalAddress();
                            if(!this.message.getFrom().equals(from)) {
                                String data = line.substring(line.toLowerCase().indexOf("error") + 5);
                                Message m = new DMessage(from, this.message.getFrom(), "ERROR Delivery", data);
                                this.manager.sendMessage(m);
                            }
                        }else{
                            System.err.println("ERROR command: " + cmd);
                            System.err.println(line);
                        }
                    }
                }
                line = command("send", in,out);
                if(!checkOk(line)){
                    System.err.println("ERROR send command");
                    System.err.println(line);
                }

                command("quit", in, out);
                this.manager.sendStatistic(this.message.getFrom());
            }
        }
        in.close();
        out.close();
        socket.close();

    }

    private String command( String cmd, BufferedReader in, PrintStream out) throws IOException, InterruptedException{
        out.println(cmd);
        String line = wait(in);
        return line;
    }

    private boolean checkOk(String line){
        return line.toLowerCase().contains("ok");
    }

    private boolean checkError(String line){
        return line.toLowerCase().contains("error");
    }

    private String wait(BufferedReader in) throws IOException, InterruptedException{
        while(!in.ready()){
            Thread.sleep(10);
        }
        return in.readLine();
    }

    private List<String> readDomains(){
        List<String> domains = new ArrayList<String>();
        String found = "";
        String nfound = "";

        for(String addr: this.message.getTo().split(",")){
            String domain = Email.getDomain(addr);
            if(!found.contains(domain)){
                String ip = this.service.lookup(domain);
                if(ip != null && ip != ""){
                    domains.add(ip);
                }else{
                    nfound += "," + addr;
                }
                found += "," + domain;
            }
        }

        if(!"".equals(nfound)){
            Message m = this.message.clone();
            m.setTo(this.message.getFrom());
            String returnAddress = "mailer@" + this.service.getLocalAddress();
            m.setFrom(returnAddress);
            m.setData("ERROR Domain not found: " + nfound.substring(1));
            m.setSubject("ERROR Domain not found");
            this.manager.sendMessage(m);
        }

        return domains;
    }
}
