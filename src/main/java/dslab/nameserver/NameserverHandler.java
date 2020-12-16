package dslab.nameserver;

import dslab.ComponentFactory;
import dslab.util.Config;

import java.io.InputStream;
import java.io.PrintStream;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentSkipListMap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class NameserverHandler implements INameserverRemote, Runnable {

    /**
     * Creates a new server instance.
     *
     * @param componentId the id of the component that corresponds to the Config resource
     * @param config the component config
     * @param in the input stream to read console input from
     * @param out the output stream to write console output to
     */
    private String componentId;
    private Config config;
    private InputStream in;
    private PrintStream out;
    //private Shell shell;

    //Store mailbox names and ip addresses + port

    //Store (remote?) references to other known Nameservers
    ConcurrentSkipListMap<String, String> mailBoxMap;
    ConcurrentSkipListMap<String, INameserverRemote> nameServerMap;
    //Store registry (rootserver only)
    private Registry registry = null;

    private String registryHost;
    private int registryPort;
    private String rootNameserverBindingName;

    //Root Nameserver: domain == null, zone nameserver: domain != null
    private String domain = null;

    public NameserverHandler(String componentId, Config config, InputStream in, PrintStream out,
                             ConcurrentSkipListMap<String, String> mailBoxMap,
                             ConcurrentSkipListMap<String, INameserverRemote> nameServerMap) {
        this.mailBoxMap = mailBoxMap;
        this.nameServerMap = nameServerMap;
        this.componentId = componentId;
        this.config = config;
        this.in = in;
        this.out = out;
        //this.shell = new Shell(this.in,this.out);

        this.registryHost = this.config.getString("registry.host");
        this.registryPort = this.config.getInt("registry.port");

        //Load config, determine if root nameserver or zone nameserver
        if (this.config.containsKey("domain")) {
            //Domain to manage - therefore regular Nameserver
            this.domain = this.config.getString("domain");
        }
        this.rootNameserverBindingName = this.config.getString("root_id");

    }



    private boolean isRootNameserver() {
        return this.domain == null;
    }

    @Override
    public void run() {
        //TODO
        // Setup Shell

        //Other startup stuff

        //run Shell

    }

    private ArrayList<String> extractTLD(String domain)
    {
        //Captures top level domain, seperates from subdomains
        //group 1: top level domain
        //group 2: subdomains
        String pattern = "^(.+)\\.(\\w+)$";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(domain);
        if(!m.find())
        {
            return null;
        }
        return new ArrayList<String>(Arrays.asList(m.group(1),m.group(2)));
    }

    private boolean isAlphanumerical(String s)
    {
        String pattern = "^(\\w+)$";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(s);
        return m.find();
    }

    //TODO HELPER FUNCTION, REMOVE
    public void print(String s)
    {
        System.out.println(this.componentId + "-handler: " + s);
        System.out.flush();
    }

    /**
     * Registers a mailbox server with the given address for the given domain. For example, when registering a
     * nameserver for the domain 'earth.planet', the new nameserver first calls the root nameserver with the argument
     * 'earth.planet'. The root nameserver locates the nameserver for 'planet' via its child-nameserver references, and
     * invokes this method with the remainder of the domain (i.e., 'earth'). Because 'earth' is then the leaf zone, the
     * current nameserver ('planet') stores the reference in its child-nameserver references.
     *
     * @param domain the domain
     * @param nameserver the nameserver's remote object
     * @throws RemoteException RMI exception (declaration required by RMI)
     * @throws AlreadyRegisteredException if the given domain is already registered
     * @throws InvalidDomainException if the domain is invalid (e.g., due to a syntax error, or a required intermediary
     * nameserver was not found)
     */
    public void registerNameserver(String domain, INameserverRemote nameserver) throws RemoteException, AlreadyRegisteredException, InvalidDomainException
    {
        print("registerNameserver called with: domain:" + domain);
        //TODO: Possible refactor with getNameServer
        //If domain not fully resolved, check domainname for splitting, then forward the registration
        if(domain.contains("."))
        {
            ArrayList<String> splitDomain = extractTLD(domain);
            if(splitDomain == null)
            {
                throw new InvalidDomainException("No valid domain: " + domain);
            }
            String tld = splitDomain.get(0);
            String lowerLevelDomains = splitDomain.get(1);

            //Check if TLD in one of stored references
            if(this.nameServerMap.containsKey(tld))
            {
                //TODO: What to do when exception (RemoteException, AlreadyRegisteredException, InvalidDomainException)
                //TODO: gets thrown? (especially RemoteException) retry in a couple milliseconds?
                this.nameServerMap.get(tld).registerNameserver(lowerLevelDomains,nameserver);
            }
            else
            {
                throw new InvalidDomainException("Domain" + domain + "not fully resolved, intermediate nameserver not found");
            }
        }

        //If Domain fully resolved, check if name is acceptable, then register
        if (!this.isAlphanumerical(domain))
        {
            throw new InvalidDomainException("Domain not alphanumerical: " + domain);
        }
        if(this.nameServerMap.containsKey(domain))
        {
            throw new AlreadyRegisteredException("Domain " + domain + "already registered at" + this.componentId);
        }
        //Leaf-zone reached, domainname alphanumerical, nameserver will be registered here
        this.nameServerMap.put(domain, nameserver);

    }

    /**
     * Registers a mailbox server with the given address for the given domain.
     *
     * @param domain the mail domain, e.g. <code>vienna.earth.planet</code>
     * @param address the socket address of the mailbox server's DMTP socket, e.g., <code>127.0.0.1:16503</code>
     * @throws RemoteException RMI exception (declaration required by RMI)
     * @throws AlreadyRegisteredException if the given domain is already in use
     * @throws InvalidDomainException if the domain is invalid (e.g., due to a syntax error, or the responsible
     * nameserver was not found)
     */
    public void registerMailboxServer(String domain, String address) throws RemoteException, AlreadyRegisteredException, InvalidDomainException
    {
        //TODO
    }

    /**
     * Returns a reference to the remote object of the ns for the given zone. For example, if called with the argument
     * 'earth' on the remote object of zone 'planet', the call returns the reference to the nameserver of the zone
     * 'earth.planet'.
     *
     * @param zone the child zone, e.g. <code>earth</code>
     * @return the remote object reference of the given zone, or <code>null</code> if it does not exist
     * @throws RemoteException RMI exception (declaration required by RMI)
     */
    public INameserverRemote getNameserver(String zone) throws RemoteException
    {
        //TODO: Remove prints, put in proper logging, refactor with registerNameServer
        print("getNameserver called with: zone:" + zone);
        print("current nameservermap " + this.nameServerMap.keySet().toString());
        String tld;
        if(zone.contains(".")) {
            ArrayList<String> splitDomain = extractTLD(zone);
            if (splitDomain == null) {
                throw new RemoteException("No valid domain: " + zone);
            }
            tld = splitDomain.get(0);
        }
        else
        {
            tld = zone;
        }

        //Check if TLD in one of stored references
        if(this.nameServerMap.containsKey(tld))
        {
            //TODO: What to do when exception (RemoteException, AlreadyRegisteredException, InvalidDomainException)
            //TODO: gets thrown? (especially RemoteException) retry in a couple milliseconds?
            return this.nameServerMap.get(tld);
        }
        else
        {
            throw new RemoteException("Domain" + zone + "not fully resolved, intermediate nameserver not found");
        }

    }


    public String lookup(String username) throws RemoteException
    {
        if (this.mailBoxMap.containsKey(username))
        {
            //Returns IP and port of mailbox
            return this.mailBoxMap.get(username);
        }
        else
        {
            throw new RemoteException("No user " + username + " at " + (this.isRootNameserver() ? "rootNameserver" : this.domain));
        }
    }


    public void shutdown() {
        // TODO
        /*
        When shutting down the nameserver, do not forget to unexport its remote object using
        the static method UnicastRemoteObject.unexportObject(Remote obj, boolean force) and, in the
        case of the root nameserver, also unregister the remote object and close the registry by invoking the
        before mentioned static unexportObject method and registry reference as parameter. Otherwise the
        application may not stop.
        * */
        /*
        try {
            UnicastRemoteObject.unexportObject(this, true);
        } catch (NoSuchObjectException e) {
            e.printStackTrace();
        }

         */

        try {
            UnicastRemoteObject.unexportObject(this, true);
        } catch (NoSuchObjectException e) {
            e.printStackTrace();
        }


        /*
        for (String k : this.nameServerMap.keySet())
        {
            try {
                UnicastRemoteObject.unexportObject(this.nameServerMap.get(k), true);
            } catch (NoSuchObjectException e) {
                e.printStackTrace();
            }
        }
        */


    }


    /*
    public static void main(String[] args) throws Exception {
        INameserver component = ComponentFactory.createNameserver(args[0], System.in, System.out);
        component.run();
    }

     */

}
