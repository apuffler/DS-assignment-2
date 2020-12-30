package dslab.common;

import at.ac.tuwien.dsg.orvell.Shell;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Log {
    private Shell shell;

    public Log(Shell shell)
    {
        this.shell = shell;
    }

    public void log(String s)
    {
        LocalTime time = LocalTime.now();
        String timeColonPattern = "HH:mm:ss";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(timeColonPattern);
        String formattedTime = time.format(formatter);
        String logline = "%s : %s";
        this.shell.out().println(String.format(logline, formattedTime, s));
        this.shell.out().flush();
    }
}
