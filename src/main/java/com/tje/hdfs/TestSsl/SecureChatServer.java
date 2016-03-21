package com.tje.hdfs.TestSsl;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.net.InetAddress;

import javax.net.ssl.SSLEngine;

public final class SecureChatServer {

    static final int PORT = Integer.parseInt(System.getProperty("port", "8992"));

    public static void main(String[] args) throws Exception {

        SSLEngine sslEngine = SslContextFactory.getServerContext().createSSLEngine();
        sslEngine.setUseClientMode(false);
        sslEngine.setWantClientAuth(false);

        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup(3);

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new SecureChatServerInitializer(sslEngine));

            b.bind(PORT).sync();
        }catch(Exception e){
        	e.printStackTrace();
        }
    }

    private static class SecureChatServerInitializer extends ChannelInitializer<SocketChannel> {

        private final SSLEngine sslCtx;

        public SecureChatServerInitializer(SSLEngine sslCtx) {
            this.sslCtx = sslCtx;
        }

        @Override
        public void initChannel(SocketChannel ch) throws Exception {
            ChannelPipeline pipeline = ch.pipeline();

            pipeline.addLast(new SslHandler(sslCtx));
            pipeline.addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
            pipeline.addLast(new StringDecoder());
            pipeline.addLast(new StringEncoder());

            // and then business logic.
            pipeline.addLast(new SecureChatServerHandler());
        }
    }

    private static class SecureChatServerHandler extends SimpleChannelInboundHandler<String> {

        static final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

        @Override
        public void channelActive(final ChannelHandlerContext ctx) {
            // Once session is secured, send a greeting and register the channel to the global channel
            // list so the channel received the messages from others.
            ctx.pipeline().get(SslHandler.class).handshakeFuture().addListener(
                    new GenericFutureListener<Future<Channel>>() {
                        @Override
                        public void operationComplete(Future<Channel> future) throws Exception {
                            ctx.writeAndFlush(
                                    "Welcome to " + InetAddress.getLocalHost().getHostName() + " secure chat service!\n");
                            ctx.writeAndFlush(
                                    "Your session is protected by " +
                                            ctx.pipeline().get(SslHandler.class).engine().getSession().getCipherSuite() +
                                            " cipher suite.\n");

                            channels.add(ctx.channel());
                        }
                    });
        }

        @Override
        public void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        	System.out.println("receive" + msg);
            // Send the received message to all channels but the current one.
            for (Channel c : channels) {
                if (c != ctx.channel()) {
                    c.writeAndFlush("[" + ctx.channel().remoteAddress() + "] " + msg + '\n');
                } else {
                    c.writeAndFlush("[you] " + msg + '\n');
                }
            }

            // Close the connection if the client has sent 'bye'.
            if ("bye".equals(msg.toLowerCase())) {
                ctx.close();
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
        }
    }
}