package com.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.*;

/**
 * Hello world!
 *
 */
public class App 
{
    static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main( String[] args ) throws InterruptedException, ConfigError {
//        App.class.getResourceAsStream("hoge");

        if (args.length != 1) return;

        String fileName = args[0];

        logger.warn( "file name is ... " + fileName );

        SessionSettings settings = new SessionSettings(fileName);
        FixApplication application = new FixApplication(settings);
        MessageStoreFactory messageStoreFactory = new FileStoreFactory(settings);
        FileLogFactory logFactory = new FileLogFactory(settings);

        DefaultMessageFactory messageFactory = new DefaultMessageFactory();

        final SocketInitiator initiator = new SocketInitiator(application, messageStoreFactory, settings, logFactory, messageFactory);
        initiator.start();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                super.run();
                initiator.stop();
                logger.warn( "initiator is stopped." );
            }
        });

        while (true) {
            Thread.sleep(1000L);
        }
    }
}
