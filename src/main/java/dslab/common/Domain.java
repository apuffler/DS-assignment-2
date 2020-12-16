package dslab.common;

import dslab.nameserver.InvalidDomainException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Domain {

    private String domainString;
    private String tld;
    private String subdomains;

    public Domain(String domainString) throws InvalidDomainException {
        this.domainString = domainString;
        this.tld = null;
        this.subdomains = null;
        this.categorizeDomain();
    }

    public String getTLD(){return this.tld;}
    public String getDomain(){return this.domainString;}
    public String getSubdomains(){return this.subdomains;}

    public boolean isFullyResolved()
    {
        return !this.domainString.contains(".");
    }

    public boolean isAlphanumerical()
    {
        String pattern = "^(\\w+)$";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(this.domainString);
        return m.find();
    }

    /*
    private boolean isValid()
    {
        return (!this.isAlphanumerical() && !this.isFullyResolved()) || (this.isAlphanumerical() && this.isFullyResolved());
    }
    
     */

    private void categorizeDomain() throws InvalidDomainException
    {
        //Fully resolved but domainname itself is not alphanumerical, therefore not valid.
        if(!this.isAlphanumerical() && this.isFullyResolved())
        {
            throw new InvalidDomainException("Not a valid domain: " + this.domainString);
        }

        //Fully resolved and valid
        if (this.isAlphanumerical() && this.isFullyResolved())
        {
            this.tld = "";
            this.subdomains = "";
            return;
        }

        //Unresolved
        String pattern = "^(.+)\\.(\\w+)$";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(this.domainString);
        if(!m.find())
        {
            throw new InvalidDomainException("Not a valid domain: " + this.domainString);
        }

        this.tld = m.group(1);
        this.subdomains = m.group(2);

    }



}
