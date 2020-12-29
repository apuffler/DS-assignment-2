package dslab.protocols;

import java.util.ArrayList;
import java.util.List;

public class DMessage implements Message {

    private Integer id;
    private String from, to, subject, data, hash;

    public DMessage(){
        this(null,null,null,null, null);
    }

    public DMessage(DMessage m){
        this(m.from, m.to,m.subject,m.data, m.hash);
    }

    public DMessage(String from, String to, String subject, String data, String hash){
        this.id = null;
        this.from = from;
        this.to = to;
        this.subject = subject;
        this.data = data;
        this.hash = hash;
    }

    @Override
    public Integer getID() {
        return this.id;
    }

    @Override
    public void setID(int id) {
        if(this.id == null){
            this.id = id;
        }
    }

    @Override
    public String getFrom() {
        return this.from;
    }

    @Override
    public void setFrom(String from) {
        this.from = from;
    }

    @Override
    public String getSubject() {
        return this.subject;
    }

    @Override
    public void setSubject(String subject) {
        this.subject = subject;
    }

    @Override
    public String getTo() {
        return this.to;
    }

    @Override
    public void setTo(String to) {
        this.to = to;
    }

    @Override
    public String getData() {
        return this.data;
    }

    @Override
    public void setData(String data) {
        this.data = data;
    }

    @Override
    public String getHash() {
        return this.hash;
    }

    @Override
    public void setHash(String hash) {
        this.hash = hash;
    }

    @Override
    public Message clone(){
        return new DMessage(this);
    }

    @Override
    public boolean validate() {
        return DMessage.check(this.from) && DMessage.check(this.to);
    }

    @Override
    public List<String> generateCommands(){
        List<String> list = new ArrayList<String>();

        list.add("from " + this.getFrom());
        list.add("to " + this.getTo());
        list.add("subject " + this.getSubject());
        list.add("data " + this.getData());
        list.add("hash " + this.getHash());

        return list;
    }

    private static boolean check(String str){
        return (str != null && str != "");
    }
}
