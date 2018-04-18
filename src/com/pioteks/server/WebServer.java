package com.pioteks.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.session.IoSessionConfig;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pioteks.handler.WebServerHandler;

public class WebServer {

	private static final Logger logger = (Logger) LoggerFactory.getLogger(WebServer.class);
	
	public static void startWebServer(int port) throws IOException {
		// ** Acceptor����
		IoAcceptor acceptor = new NioSocketAcceptor();
        // ���д���������ĳ���������������10��
        DefaultIoFilterChainBuilder chain = acceptor.getFilterChain();
        chain.addLast("threadPool",new ExecutorFilter(Executors.newCachedThreadPool()));
        chain.addLast("logger", new LoggingFilter());
        // ����MINA2��IoHandlerʵ����
        acceptor.setHandler(new WebServerHandler());

        acceptor.getSessionConfig().setReadBufferSize(2048);

        // ** TCP����˿�ʼ����
        acceptor.bind(new InetSocketAddress(port));

        System.out.println("WebServer start in " + port + " ..");
        logger.info("WebServer start in " + port + " ..");
	}
}
