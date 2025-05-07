package org.example.network.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.Socket;

public abstract class ConcurrentServer extends AbstractServer {
    private static final Logger logger = LogManager.getLogger(ConcurrentServer.class);

    public ConcurrentServer(int port) {
        super(port);
        logger.info("Concurrent server initialized on port {}", port);
    }

    @Override
    protected void processRequest(Socket client) {
        Thread worker = createWorker(client);
        worker.start();
    }

    protected abstract Thread createWorker(Socket client);
}