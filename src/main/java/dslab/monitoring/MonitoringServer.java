package dslab.monitoring;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashMap;

import at.ac.tuwien.dsg.orvell.Shell;
import at.ac.tuwien.dsg.orvell.StopShellException;
import at.ac.tuwien.dsg.orvell.annotation.Command;
import dslab.ComponentFactory;
import dslab.basic.ThreadedCommunication;
import dslab.util.Config;

public class MonitoringServer implements IMonitoringServer {

    private HashMap<String,Integer> addresses, servers;

    private Shell shell;
    private Config config;
    private String name;

    private ThreadedCommunication server;
    /**
     * Creates a new server instance.
     *
     * @param componentId the id of the component that corresponds to the Config resource
     * @param config the component config
     * @param in the input stream to read console input from
     * @param out the output stream to write console output to
     */
    public MonitoringServer(String componentId, Config config, InputStream in, PrintStream out) {
        this.name = componentId;
        this.config = config;
        this.shell = new Shell(in,out);
    }

    @Override
    public void run() {
        // Setup Shell
        this.shell.setPrompt(this.name + "> ");
        this.shell.register(this);

        //Setup Data Handling
        this.addresses = new HashMap<String,Integer>();
        this.servers = new HashMap<String,Integer>();

        //Setup UDPServer
        this.server = new UDPServer(this.config.getInt("udp.port"),this);
        this.server.start();

        //run Shell
        this.shell.run();
    }

    @Override
    public void processData(String data) {
        String[] parts = data.trim().split(" ");
        if(parts.length == 2){
            updateMap(this.servers, parts[0]);
            updateMap(this.addresses, parts[1]);
        }
    }

    private void updateMap(HashMap<String,Integer> map, String key){
        Integer count = 0;
        if(map.containsKey(key)) {
            count = map.get(key);
        }
        map.put(key, count + 1);
    }

    @Override
    @Command
    public void addresses() {
        printMap(this.addresses);
    }

    @Override
    @Command
    public void servers() {
        printMap(this.servers);
    }

    private void printMap(HashMap<String, Integer> map){
        for( String key : map.keySet()){
            if(map.containsKey(key)){
                this.shell.out().printf("%s %d%n", key, map.get(key));
            }
        }
    }

    @Override
    @Command
    public void shutdown() {
        this.shell.out().printf("%s> Server stopping...%n", this.name);

        this.server.shutdown();

        this.shell.out().printf("%s> Server stoped!%n", this.name);
        //Shell beenden
        throw new StopShellException();
    }

    public static void main(String[] args) throws Exception {
        IMonitoringServer server = ComponentFactory.createMonitoringServer(args[0], System.in, System.out);
        server.run();
    }

}
