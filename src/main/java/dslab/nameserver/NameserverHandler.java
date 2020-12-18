package dslab.nameserver;

import dslab.ComponentFactory;
import dslab.common.Domain;
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


public class NameserverHandler implements INameserverRemote {

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

    //Store mailbox names and ip addresses + port
    ConcurrentSkipListMap<String, String> mailBoxMap;

    //Store associated domain and remote references to other known Nameserver remote objects
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
        print("registering " + domain + " @ " + this.domain);
        Domain parsedDomain = new Domain(domain);
        print("registerNameserver called with: domain:" + domain);
        //TODO: Possible refactor with getNameServer

        if(parsedDomain.isFullyResolved())
        {
            //If this domain is already registered here, throw exception.
            if(this.nameServerMap.containsKey(domain))
            {
                throw new AlreadyRegisteredException("Domain " + domain + "already registered at" + this.componentId);
            }

            //Leaf-zone reached, nameserver will be registered here
            this.nameServerMap.put(parsedDomain.getDomain(), nameserver);
        }
        else
        {
            if(this.nameServerMap.containsKey(parsedDomain.getTLD()))
            {
                //TODO: What to do when exception (RemoteException, AlreadyRegisteredException, InvalidDomainException)
                //TODO: gets thrown? (especially RemoteException) retry in a couple milliseconds?
                //Domain not fully resolved but next nameserver found, forward call
                this.nameServerMap.get(parsedDomain.getTLD()).registerNameserver(parsedDomain.getSubdomains(),nameserver);
            }
            else
            {   //Resolving domain fully impossible, intermediate nameserver not found
                throw new RemoteException("Domain" + domain + "not fully resolved, intermediate nameserver not found");
            }
        }

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
        print("registering mailbox" + domain + " @ " + this.domain);
        Domain parsedDomain = new Domain(domain);
        if(parsedDomain.isFullyResolved())
        {
            //If this domain is already registered here, throw exception.
            if(this.mailBoxMap.containsKey(address))
            {
                throw new AlreadyRegisteredException("Mailbox " + address + "already registered at" + this.componentId);
            }

            //Leaf-zone reached, nameserver will be registered here
            this.mailBoxMap.put(parsedDomain.getDomain(), address);
        }
        else
        {
            if(this.nameServerMap.containsKey(parsedDomain.getTLD()))
            {
                this.nameServerMap.get(parsedDomain.getTLD()).registerMailboxServer(parsedDomain.getSubdomains(), address);
            }
            else
            {   //Resolving domain fully impossible, intermediate nameserver not found
                throw new RemoteException("Domain" + domain + "not fully resolved, intermediate nameserver not found");
            }
        }
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
        try {
            Domain parsedDomain = new Domain(zone);

            //Check if TLD in one of stored references
            if(this.nameServerMap.containsKey(parsedDomain.getTLD()))
            {
                //TODO: What to do when exception (RemoteException, AlreadyRegisteredException, InvalidDomainException)
                //TODO: gets thrown? (especially RemoteException) retry in a couple milliseconds?
                return this.nameServerMap.get(parsedDomain.getTLD());
            }
            else
            {
                throw new RemoteException("Domain" + zone + "not fully resolved, intermediate nameserver not found");
            }
        } catch (InvalidDomainException e) {
            throw new RemoteException(e.toString());
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
        try {
            UnicastRemoteObject.unexportObject(this, true);
        } catch (NoSuchObjectException e) {
            e.printStackTrace();
        }

    }

}
