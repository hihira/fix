package io.github.hihira;

import org.quickfixj.jmx.JmxExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.*;

import javax.management.JMException;
import java.io.*;

/**
 * Hello world!
 */
public class App {
    static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) throws InterruptedException, ConfigError {
        InputStream inputStream = null;
        if (args.length == 0) {
            inputStream = App.class.getResourceAsStream("client.cfg");
        } else if (args.length == 1) {
            try {
                inputStream = new FileInputStream(args[0]);
            } catch (FileNotFoundException e) {
            }
        }

        if (inputStream == null) {
            System.out.println("usage: " + App.class.getName() + " [configFile].");
            return;
        }

        SessionSettings settings = new SessionSettings(inputStream);
        FixApplication application = new FixApplication();
        MessageStoreFactory messageStoreFactory = new FileStoreFactory(settings);
        FileLogFactory logFactory = new FileLogFactory(settings);

        DefaultMessageFactory messageFactory = new DefaultMessageFactory();

        final SocketInitiator initiator = new SocketInitiator(application, messageStoreFactory, settings, logFactory, messageFactory);

        JmxExporter exporter = null;
        try {
            exporter = new JmxExporter();
        } catch (JMException e) {
            e.printStackTrace();
        }
        exporter.register(initiator);

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

        initiator.start();

        label:
        while (true) {
            System.out.println("type #quit to quit");
            String value = null;
            try {
                value = in.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (value != null) {
                switch (value) {
                    case "#quit":
                        break label;
                    default:
                        System.out.println("");
                        break;
                }
            }
        }

        initiator.stop();
    }
}
