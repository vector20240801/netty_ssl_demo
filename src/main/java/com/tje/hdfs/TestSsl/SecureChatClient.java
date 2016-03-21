package com.tje.hdfs.TestSsl;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.ssl.SslHandler;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;

import javax.net.ssl.SSLEngine;

public final class SecureChatClient {

    static final String HOST = System.getProperty("host", "127.0.0.1");
    static final int PORT = Integer.parseInt(System.getProperty("port", "8992"));

    public static void main(String[] args) throws Exception {

        SSLEngine engine = SslContextFactory.getClientContext().createSSLEngine();
        engine.setUseClientMode(true);
        engine.setWantClientAuth(false);

        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new SecureChatClientInitializer(engine));

            // Start the connection attempt.
            Channel ch = b.connect(HOST, PORT).sync().channel();

            // Read commands from the stdin.
            ChannelFuture lastWriteFuture = null;
			BufferedReader in = new BufferedReader(new InputStreamReader(
					new ByteArrayInputStream(
							"几把几把吧唧吧唧啊我我\r\n几个IE我及覅威风威风访问发\r\n".getBytes())));
            for (; ; ) {
                String line = in.readLine();
                if (line == null) {
                    break;
                }

                // Sends the received line to the server.
                lastWriteFuture = ch.writeAndFlush(line + "\r\n");

                // If user typed the 'bye' command, wait until the server closes
                // the connection.
                if ("bye".equals(line.toLowerCase())) {
                    ch.closeFuture().sync();
                    break;
                }
            }

            // Wait until all messages are flushed before closing the channel.
            if (lastWriteFuture != null) {
                lastWriteFuture.sync();
            }
        } finally {
            // The connection is closed automatically on shutdown.
            group.shutdownGracefully();
        }
    }

    private static class SecureChatClientInitializer extends ChannelInitializer<SocketChannel> {

        private final SSLEngine sslCtx;

        public SecureChatClientInitializer(SSLEngine sslCtx) {
            this.sslCtx = sslCtx;
        }

        @Override
        public void initChannel(SocketChannel ch) throws Exception {
            ChannelPipeline pipeline = ch.pipeline();

            // Add SSL handler first to encrypt and decrypt everything.
            // In this example, we use a bogus certificate in the server side
            // and accept any invalid certificates in the client side.
            // You will need something more complicated to identify both
            // and server in the real world.
            //pipeline.addLast(sslCtx.newHandler(ch.alloc(), SecureChatClient.HOST, SecureChatClient.PORT));
            pipeline.addLast(new SslHandler(sslCtx));

            // On top of the SSL handler, add the text line codec.
            pipeline.addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
            pipeline.addLast(new StringDecoder());
            pipeline.addLast(new StringEncoder());
            //pipeline.addLast(new LoggingHandler(LogLevel.INFO));


            // and then business logic.
            pipeline.addLast(new SecureChatClientHandler());
        }
    }

    private static class SecureChatClientHandler extends SimpleChannelInboundHandler<String> {

        @Override
        public void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
            System.out.println(msg);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
        }
    }
}
