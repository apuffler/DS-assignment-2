package dslab.basic;

import java.util.regex.Pattern;

public class Email {

    private static final String regex = "^(.+)@(.+)";

    public static boolean validateEmail(String email){
        Pattern emailchecker = Pattern.compile(Email.regex);
        return emailchecker.matcher(email).matches();
    }

    public static String getUsername(String email){
        return email.substring(0,email.indexOf('@'));
    }

    public static String getDomain(String email){
        return email.substring(email.indexOf('@') + 1);
    }
}
