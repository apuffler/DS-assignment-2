package dslab.nameserver;

import at.ac.tuwien.dsg.orvell.Shell;

import dslab.common.Domain;
import dslab.common.Log;
import dslab.util.Config;

import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import java.util.concurrent.ConcurrentSkipListMap;



public class NameserverHandler implements INameserverRemote {

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
    private final Shell shell;
    private final Log log;
    //Store mailbox names and ip addresses + port
    private final ConcurrentSkipListMap<String, String> mailBoxMap;

    //Store associated domain and remote references to other known Nameserver remote objects
    private final ConcurrentSkipListMap<String, INameserverRemote> nameServerMap;

    //Root Nameserver: domain == null, zone nameserver: domain != null
    private String domain = null;

    public NameserverHandler(String componentId, Config config, Shell shell,
                             ConcurrentSkipListMap<String, String> mailBoxMap,
                             ConcurrentSkipListMap<String, INameserverRemote> nameServerMap) {
        this.mailBoxMap = mailBoxMap;
        this.nameServerMap = nameServerMap;
        this.componentId = componentId;
        this.config = config;
        this.shell = shell;
        this.log = new Log(this.shell);

        //Load config, determine if root nameserver or zone nameserver
        if (this.config.containsKey("domain")) {
            //Domain to manage - therefore regular Nameserver
            this.domain = this.config.getString("domain");
        }
    }

    private boolean isRootNameserver() {
        return this.domain == null;
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
        Domain parsedDomain = new Domain(domain);
        if(parsedDomain.isFullyResolved())
        {
            synchronized (this.nameServerMap) {
                //If this domain is already registered here, throw exception.
                if (this.nameServerMap.containsKey(domain)) {
                    throw new AlreadyRegisteredException("Domain " + domain + "already registered at" + this.componentId);
                }

                //Leaf-zone reached, nameserver will be registered here
                this.nameServerMap.put(parsedDomain.getDomain(), nameserver);
                this.log.log("Registering nameserver for zone " + parsedDomain.getDomain());
            }
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
        Domain parsedDomain = new Domain(domain);
        if(parsedDomain.isFullyResolved())
        {
            synchronized (this.mailBoxMap) {
                //If this domain is already registered here, throw exception.
                if (this.mailBoxMap.containsKey(address)) {
                    throw new AlreadyRegisteredException("Mailbox " + address + "already registered at" + this.componentId);
                }

                //Leaf-zone reached, nameserver will be registered here
                this.mailBoxMap.put(parsedDomain.getDomain(), address);
                this.log.log("Registering mailboxserver for zone"  + parsedDomain.getDomain());
            }
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
        this.log.log("Nameserver for " + zone + " requested");
        try {
            Domain parsedDomain = new Domain(zone);

            //Check if TLD in one of stored references
            if(this.nameServerMap.containsKey(parsedDomain.getTLD()))
            {
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
        this.log.log("Looking up " + username + " at nameserver " + this.domain);
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
            this.log.log("NameserverHandler" + this.componentId + " shutting down!");
            UnicastRemoteObject.unexportObject(this, true);
        } catch (NoSuchObjectException e) {
            e.printStackTrace();
        }

    }

}
