package org.example.network.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.network.rpc.BasketballJsonWorker;
import org.example.service.AllServices;

import java.net.Socket;

public class BasketballJsonServer extends ConcurrentServer {
    private final AllServices services;
    private static final Logger logger = LogManager.getLogger(BasketballJsonServer.class);

    public BasketballJsonServer(int port, AllServices services) {
        super(port);
        this.services = services;
        logger.info("Basketball JSON Server initialized");
    }

    @Override
    protected Thread createWorker(Socket client) {
        BasketballJsonWorker worker = new BasketballJsonWorker(services, client);
        return new Thread(worker);
    }
}