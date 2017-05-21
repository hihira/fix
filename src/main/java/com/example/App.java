package com.example;

import org.quickfixj.jmx.JmxExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.*;

import javax.management.JMException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Hello world!
 */
public class App {
    static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) throws InterruptedException, ConfigError {
//        App.class.getResourceAsStream("hoge");

        if (args.length != 1) return;

        String fileName = args[0];

        logger.warn("file name is ... " + fileName);

        SessionSettings settings = new SessionSettings(fileName);
        FixApplication application = new FixApplication(settings);
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
