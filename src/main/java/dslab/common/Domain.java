package dslab.common;

import dslab.nameserver.InvalidDomainException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Domain {

    private String domain;
    private String tld;
    private String subdomains;

    public Domain(String domain) throws InvalidDomainException {
        this.domain = domain;
        this.tld = null;
        this.subdomains = null;
        this.categorizeDomain();
    }

    public String getTLD(){return this.tld;}
    public String getDomain(){return this.domain;}
    public String getSubdomains(){return this.subdomains;}

    public boolean isFullyResolved()
    {
        return !this.domain.contains(".");
    }

    private boolean isAlphanumerical(String s)
    {
        String pattern = "^(\\w+)$";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(s);
        return m.find();
    }


    private boolean isValid()
    {
        return this.isAlphanumerical(this.domain.replace(".",""));
    }

    public ArrayList<String> splitDomain()
    {
        ArrayList<String> splitDomains = new ArrayList<String>();
        splitDomains.addAll(Arrays.asList(this.domain.split("\\.")));
        return splitDomains;
    }

    private void categorizeDomain() throws InvalidDomainException
    {
        if(!this.isValid())
        {
            throw new InvalidDomainException("Not a valid domain: " + this.domain);
        }

        if (this.isFullyResolved())
        {
            //Resolved domain
            this.tld = this.domain;
            this.subdomains = "";
        }
        else {
            //Unresolved domain
            String pattern = "^(.+)\\.(\\w+)$";
            Pattern r = Pattern.compile(pattern);
            Matcher m = r.matcher(this.domain);
            if (!m.find()) {
                throw new InvalidDomainException("Not a valid domain: " + this.domain);
            }

            this.tld = m.group(2);
            this.subdomains = m.group(1);

        }
    }



}
