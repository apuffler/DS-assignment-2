package dslab.bigtest;

import dslab.*;
import dslab.basic.ThreadedCommunication;
import dslab.mailbox.IMailboxServer;
import dslab.monitoring.IMonitoringServer;
import dslab.transfer.ITransferServer;
import dslab.util.Config;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

public class BigTest1 extends TestBase {

    private static final Log LOG = LogFactory.getLog(BigTest1.class);

    private String mailbox = "mailbox-earth-planet";
    private String transfer1 = "transfer-1";
    private String transfer2 = "transfer-2";
    private String monitoring = "monitoring";

    private IMailboxServer mailboxcomp;
    private ITransferServer t1, t2;
    private IMonitoringServer monitoringcomp;

    private Thread tmb, tt1, tt2, tmon;

    private int mb_tp, mb_ap, t1p, t2p, mp;

    private TestInputStream mb_in, t1_in, t2_in, mon_in;
    private TestOutputStream mb_out, t1_out, t2_out, mon_out;


    @Before
    public void setUp() throws Exception {
        mb_in = new TestInputStream();
        t1_in = new TestInputStream();
        t2_in = new TestInputStream();
        mon_in = new TestInputStream();

        mb_out = new TestOutputStream();
        t1_out = new TestOutputStream();
        t2_out = new TestOutputStream();
        mon_out = new TestOutputStream();

        mailboxcomp = ComponentFactory.createMailboxServer(mailbox, mb_in, mb_out);
        t1 = ComponentFactory.createTransferServer(transfer1, t1_in, t1_out);
        t2 = ComponentFactory.createTransferServer(transfer2, t2_in, t2_out);
        monitoringcomp = ComponentFactory.createMonitoringServer(monitoring, mon_in, mon_out);

        tmb = new Thread(mailboxcomp);
        tt1 = new Thread(t1);
        tt2 = new Thread(t2);
        tmon = new Thread(monitoringcomp);

        tmb.start();
        tt1.start();
        tt2.start();
        tmon.start();

        mb_ap = new Config(mailbox).getInt("dmap.tcp.port");
        mb_tp = new Config(mailbox).getInt("dmtp.tcp.port");

        t1p = new Config(transfer1).getInt("tcp.port");
        t2p = new Config(transfer2).getInt("tcp.port");

        mp = new Config(monitoring).getInt("udp.port");


        LOG.info("Waiting for server sockets to appear");
        Sockets.waitForSocket("localhost", mb_ap, Constants.COMPONENT_STARTUP_WAIT);
        Sockets.waitForSocket("localhost", mb_tp, Constants.COMPONENT_STARTUP_WAIT);
        Sockets.waitForSocket("localhost", t1p, Constants.COMPONENT_STARTUP_WAIT);
        Sockets.waitForSocket("localhost", t2p, Constants.COMPONENT_STARTUP_WAIT);
        //Sockets.waitForSocket("localhost", mp, Constants.COMPONENT_STARTUP_WAIT);
    }

    @After
    public void tearDown() throws Exception {
        mb_in.addLine("shutdown"); // send "shutdown" command to command line
        t1_in.addLine("shutdown"); // send "shutdown" command to command line
        t2_in.addLine("shutdown"); // send "shutdown" command to command line
        mon_in.addLine("shutdown"); // send "shutdown" command to command line
        Thread.sleep(Constants.COMPONENT_TEARDOWN_WAIT);
    }

    @Test(timeout = 15000)
    public void bigTest1() throws Exception {
        try (JunitSocketClient client = new JunitSocketClient(t1p, err)) {
            client.verify("ok DMTP");
            client.sendAndVerify("begin", "ok");
            client.sendAndVerify("from dbac@lva.at", "ok");
            client.sendAndVerify("to trillian@earth.planet", "ok 1");
            client.sendAndVerify("subject test1", "ok");
            client.sendAndVerify("data test data", "ok");
            client.sendAndVerify("send", "ok");
            client.sendAndVerify("quit","ok bye");
        }

        try (JunitSocketClient client = new JunitSocketClient(t2p, err)) {
            client.verify("ok DMTP");
            client.sendAndVerify("begin", "ok");
            client.sendAndVerify("from dbac@lva.at", "ok");
            client.sendAndVerify("to trillian@earth.planet", "ok 1");
            client.sendAndVerify("subject test2", "ok");
            client.sendAndVerify("data test data", "ok");
            client.sendAndVerify("send", "ok");
            client.sendAndVerify("quit","ok bye");
        }

        try (JunitSocketClient client = new JunitSocketClient(t1p, err)) {
            client.verify("ok DMTP");
            client.sendAndVerify("begin", "ok");
            client.sendAndVerify("from gu@lva.at", "ok");
            client.sendAndVerify("to trillian@earth.planet", "ok 1");
            client.sendAndVerify("subject test3", "ok");
            client.sendAndVerify("data test data", "ok");
            client.sendAndVerify("send", "ok");
            client.sendAndVerify("quit","ok bye");
        }

        Thread.sleep(2500);

        try (JunitSocketClient client = new JunitSocketClient(mb_ap, err)) {
            client.verify("ok DMAP");
            client.sendAndVerify("login trillian 12345", "ok");

            client.send("list");
            String listResult = client.listen();
            err.checkThat(listResult, containsString("dbac@lva.at test1"));
            err.checkThat(listResult, containsString("dbac@lva.at test2"));
            err.checkThat(listResult, containsString("gu@lva.at test3"));

            client.sendAndVerify("logout", "ok");
            client.sendAndVerify("quit", "ok bye");
        }

        Thread.sleep(2500);
        mon_in.addLine("addresses"); // send "addresses" command to command line
        Thread.sleep(2500);
        String output = String.join(",", mon_out.getLines());
        assertThat(output, containsString("dbac@lva.at 2"));
        assertThat(output, containsString("gu@lva.at 1"));

        mon_in.addLine("servers"); // send "addresses" command to command line
        Thread.sleep(2500);
        output = String.join(",", mon_out.getLines());
        assertThat(output, containsString(":12710 2"));
        assertThat(output, containsString(":12711 1"));


    }

    @Test(timeout = 15000)
    public void bigTest2() throws Exception {
        try (JunitSocketClient client = new JunitSocketClient(t1p, err)) {
            client.verify("ok DMTP");
            client.sendAndVerify("begin", "ok");
            client.sendAndVerify("from trillian@earth.planet", "ok");
            client.sendAndVerify("to arthur@earth.planet,abc@earth.planet", "ok 2");
            client.sendAndVerify("subject test1", "ok");
            client.sendAndVerify("data test data", "ok");
            client.sendAndVerify("send", "ok");
            client.sendAndVerify("quit","ok bye");
        }

        try (JunitSocketClient client = new JunitSocketClient(t2p, err)) {
            client.verify("ok DMTP");
            client.sendAndVerify("begin", "ok");
            client.sendAndVerify("from arthur@earth.planet", "ok");
            client.sendAndVerify("to trillian@earth.planet", "ok 1");
            client.sendAndVerify("subject test2", "ok");
            client.sendAndVerify("data test data", "ok");
            client.sendAndVerify("send", "ok");
            client.sendAndVerify("quit","ok bye");
        }

        Thread.sleep(2500);

        try (JunitSocketClient client = new JunitSocketClient(mb_ap, err)) {
            client.verify("ok DMAP");
            client.sendAndVerify("login trillian 12345", "ok");

            client.send("list");
            String listResult = client.listen();
            err.checkThat(listResult, containsString("arthur@earth.planet test2"));
            err.checkThat(listResult, containsString("delivery@transfer-1.service ERROR Delivery"));

            client.sendAndVerify("logout", "ok");


            client.sendAndVerify("login arthur 23456", "ok");
            client.send("list");
            listResult = client.listen();
            err.checkThat(listResult, containsString("trillian@earth.planet test1"));



            client.sendAndVerify("quit", "ok bye");
        }

        Thread.sleep(2500);
        mon_in.addLine("addresses"); // send "addresses" command to command line
        Thread.sleep(2500);
        String output = String.join(",", mon_out.getLines());
        assertThat(output, containsString("trillian@earth.planet 1"));
        assertThat(output, containsString("arthur@earth.planet 1"));

        mon_in.addLine("servers"); // send "addresses" command to command line
        Thread.sleep(2500);
        output = String.join(",", mon_out.getLines());
        assertThat(output, containsString(":12710 2"));
        assertThat(output, containsString(":12711 1"));


    }

}
