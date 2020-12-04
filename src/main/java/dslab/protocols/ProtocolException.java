package dslab.protocols;

public class ProtocolException extends Exception{
    private Object data;
    public ProtocolException(){
        this("Generic Error", null);
    }

    public ProtocolException(String message){
        this(message,null);
    }

    public ProtocolException(String message, Object data){
        super(message);
        this.data = data;
    }

    public Object getData(){
        return this.data;
    }
}
