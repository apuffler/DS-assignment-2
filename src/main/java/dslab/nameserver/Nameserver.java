package dslab.nameserver;

import java.io.InputStream;
import java.io.PrintStream;

import dslab.ComponentFactory;
import dslab.common.Log;
import dslab.util.Config;

import at.ac.tuwien.dsg.orvell.Shell;
import at.ac.tuwien.dsg.orvell.StopShellException;
import at.ac.tuwien.dsg.orvell.annotation.Command;

import java.rmi.AlreadyBoundException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import java.util.ArrayList;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;



public class Nameserver implements INameserver{

    /**
     * Creates a new server instance.
     *
     * @param componentId the id of the component that corresponds to the Config resource
     * @param config the component config
     * @param in the input stream to read console input from
     * @param out the output stream to write console output to
     */
    private final String componentId;
    private final Config config;
    private final InputStream in;
    private final PrintStream out;
    private final Shell shell;
    private final Log log;

    //Store mailbox domain and ip addresses + port
    // Mailbox name (e.g vienna) , DMTP Socket address of mailbox server (127.0.0.1:16503)
    ConcurrentSkipListMap<String, String> mailBoxMap = new ConcurrentSkipListMap<String, String>(String.CASE_INSENSITIVE_ORDER);
    //Store (remote?) references to other known Nameservers
    // Domain name (e.g. 'planet' or 'earth.planet'), Remote Object of Nameserver
    ConcurrentSkipListMap<String, INameserverRemote> nameServerMap = new ConcurrentSkipListMap<String, INameserverRemote>(String.CASE_INSENSITIVE_ORDER);

    //Nameserver Handler
    private NameserverHandler handler;

    //Store registry (rootserver only)
    private Registry registry = null;

    private final String registryHost;
    private final int registryPort;
    private final String rootNameserverBindingName;

    //Root Nameserver: domain == null, zone nameserver: domain != null
    private String domain = null;

    public Nameserver(String componentId, Config config, InputStream in, PrintStream out) {
        this.componentId = componentId;
        this.config = config;
        this.in = in;
        this.out = out;
        this.shell = new Shell(this.in,this.out);

        this.registryHost = this.config.getString("registry.host");
        this.registryPort = this.config.getInt("registry.port");

        this.rootNameserverBindingName = this.config.getString("root_id");

        //Load config, determine if root nameserver or zone nameserver
        if (this.config.containsKey("domain")) {
            //Domain to manage - therefore regular Nameserver
            this.domain = this.config.getString("domain");
        }

        this.log = new Log(this.shell);

    }

    private INameserverRemote registryLookup(String name) throws RemoteException, NotBoundException
    {
            Registry remoteRegistry = LocateRegistry.getRegistry(this.registryHost,this.registryPort);
            return (INameserverRemote) remoteRegistry.lookup(name);
    }

    private boolean isRootNameserver()
    {
        return this.domain == null;
    }



    @Override
    public void run() {
        //Setting up shell
        this.shell.setPrompt(this.componentId + "> ");
        this.shell.register(this);

        //Instancing handler
        this.handler = new NameserverHandler(this.componentId, this.config, this.shell,
                this.mailBoxMap,
                this.nameServerMap);


        if(isRootNameserver())
        {
            //IS ROOTSERVER
            //Create registry, export root NameserverHandler, bind root NameserverHandler in registry
            this.log.log("Starting up root nameserver");
            try {

                this.registry = LocateRegistry.createRegistry(this.registryPort);

                //instantiate handler and export, then bind
                int randomPort = 0;
                INameserverRemote remoteHandler = (INameserverRemote) UnicastRemoteObject.exportObject(this.handler, randomPort);

                this.registry.bind(this.rootNameserverBindingName, this.handler);

            } catch (RemoteException | AlreadyBoundException e) {
                e.printStackTrace();
            }
        }
        else
        {
            //IS ZONESERVER
            //Export zone NameserverHandler, lookup root NameserverHandler, register Nameserver
            this.log.log("Starting up zone nameserver: " + this.domain);
            try {
                int randomPort = 0;
                //instantiate handler and export, then bind+
                INameserverRemote remoteHandler = (INameserverRemote) UnicastRemoteObject.exportObject(this.handler, randomPort);

                //Lookup rootNameserver
                INameserverRemote rootNS = this.registryLookup(this.rootNameserverBindingName);
                //Register
                rootNS.registerNameserver(this.domain, this.handler);
            } catch (RemoteException | AlreadyRegisteredException | InvalidDomainException | NotBoundException e) {
                e.printStackTrace();
            }
        }

        //run Shell
        this.shell.run();
    }



    private void printEnumeratedStrings(SortedSet<String> stringSet)
    {
        int i = 1;
        for (String s : stringSet)
        {
            this.shell.out().printf("%d. %s %n", i, s);
            i++;
        }
    }

    @Command
    @Override
    public void nameservers() {
        this.printEnumeratedStrings(this.nameServerMap.descendingKeySet());
    }

    @Command
    @Override
    public void addresses() {
        ArrayList<String> domainAddressList = new ArrayList<String>();
        for(String domain : this.mailBoxMap.descendingKeySet())
        {
            domainAddressList.add(domain + " " + this.mailBoxMap.get(domain));
        }
        //TODO: Check this raw assignment/conversion of ConcurrentSkipListSet to SortedSet
        this.printEnumeratedStrings(new ConcurrentSkipListSet(domainAddressList));
    }


    @Command
    @Override
    public void shutdown() {

        this.log.log("Shutting down nameserver: " + (this.isRootNameserver() ? "root" : this.domain));

        //Shutdown handler (it unregisters itself)
        this.handler.shutdown();

        //Shutdown registry by unregistering
        if(this.isRootNameserver()) {
            try {
                UnicastRemoteObject.unexportObject(this.registry, true);
            } catch (NoSuchObjectException e) {
                e.printStackTrace();
            }
        }

        this.log.log("Nameserver stopped.");
        //Shell beenden
        throw new StopShellException();
    }

    public static void main(String[] args) throws Exception {
        INameserver component = ComponentFactory.createNameserver(args[0], System.in, System.out);
        component.run();
    }

}
