package com.pioteks.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.session.ExpiringSessionRecycler;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.DatagramSessionConfig;
import org.apache.mina.transport.socket.nio.NioDatagramAcceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pioteks.handler.NBServerHandler;
import com.pioteks.main.Main;

public class NBServer {
	private static final Logger logger = (Logger) LoggerFactory.getLogger(NBServer.class);
	
	
	public static void startNBServer(int port) throws IOException {
		// ** Acceptor����
        NioDatagramAcceptor acceptor = new NioDatagramAcceptor();
        // ���д���������ĳ���������������10��
        DefaultIoFilterChainBuilder chain = acceptor.getFilterChain();
        chain.addLast("threadPool",new ExecutorFilter(Executors.newCachedThreadPool()));
        chain.addLast("logger", new LoggingFilter());
        // ����MINA2��IoHandlerʵ����
        acceptor.setHandler(new NBServerHandler());
        // ���ûỰ��ʱʱ�䣨��λ�����룩����������Ĭ����10�룬�밴������
        acceptor.setSessionRecycler(new ExpiringSessionRecycler(15 * 1000));

        // ** UDPͨ������ �����Ƿ����õ�ַ��Ҳ����ÿ����������udp��Ϣ����һ����ַ��
        DatagramSessionConfig dcfg = acceptor.getSessionConfig();
        dcfg.setReuseAddress(true);
        // �������뻺�����Ĵ�С��ѹ�����Ա�����������2048�����ܷ�������
        dcfg.setReceiveBufferSize(1024);
        // ��������������Ĵ�С��ѹ�����Ա�����������2048�����ܷ�������
        dcfg.setSendBufferSize(1024);

        // ** UDP����˿�ʼ����
        acceptor.bind(new InetSocketAddress(port));

        System.out.println("NBServer start in " + port + " ..");
        logger.info("NBServer start in " + port + " ..");
	}
}
