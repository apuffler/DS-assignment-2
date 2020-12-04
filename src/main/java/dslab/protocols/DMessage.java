package dslab.protocols;

import java.util.ArrayList;
import java.util.List;

public class DMessage implements Message {

    private Integer id;
    private String from, to, subject, data;

    public DMessage(){
        this(null,null,null,null);
    }

    public DMessage(DMessage m){
        this(m.from, m.to,m.subject,m.data);
    }

    public DMessage(String from, String to, String subject, String data){
        this.id = null;
        this.from = from;
        this.to = to;
        this.subject = subject;
        this.data = data;
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

        return list;
    }

    private static boolean check(String str){
        return (str != null && str != "");
    }
}
