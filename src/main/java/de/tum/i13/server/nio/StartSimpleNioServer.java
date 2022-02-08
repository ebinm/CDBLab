package de.tum.i13.server.nio;

import de.tum.i13.server.echo.EchoLogic;
import de.tum.i13.shared.CommandProcessor;
import de.tum.i13.shared.Config;

import java.io.IOException;
import java.util.logging.Logger;

import static de.tum.i13.shared.Config.parseCommandlineArgs;
import static de.tum.i13.shared.LogSetup.setupLogging;

public class StartSimpleNioServer {

    public static Logger logger = Logger.getLogger(StartSimpleNioServer.class.getName());

    public static void main(String[] args) throws IOException {
//        String port = "5162";
//        String test = "Demonstration/KVServer1";
//        String test = "Demonstration/KVServer2_ToBeTransformed";
//        String test = "Demonstration/KVServer3";
//        String test = "Demonstration/KVServer4";
        Config cfg = parseCommandlineArgs(/*new String[]{"-b 127.0.0.1:5153", "-p"+port, "-d"+test}*/args);  //Do not change this
        setupLogging(cfg.logfile);
        logger.info("Config: " + cfg.toString());

        logger.info("Starting server");

        //Replace with your Key Value command processor
        CommandProcessor echoLogic = new EchoLogic(cfg);

        SimpleNioServer sn = new SimpleNioServer(echoLogic);
        sn.bindSockets(cfg.listenaddr, cfg.port);
        sn.start();
    }
}
