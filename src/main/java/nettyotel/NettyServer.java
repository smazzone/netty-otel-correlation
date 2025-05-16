package nettyotel;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;

import java.util.HashMap;
import java.util.Map;

public class NettyServer {
    
    public static void main(String[] args) throws Exception {
        // Initialize OpenTelemetry
        OTelInitializer.initialize(); // Ensure this method uses OpenTelemetrySdkBuilder.buildAndRegisterGlobal
        Tracer tracer = GlobalOpenTelemetry.getTracer("tcp-server");

        System.out.println("Starting Netty Server...");
        EventLoopGroup boss = new NioEventLoopGroup(1);
        EventLoopGroup worker = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            System.out.println("Configuring server bootstrap...");
            b.group(boss, worker)
             .channel(NioServerSocketChannel.class)
             .childHandler(new ChannelInitializer<SocketChannel>() {
                 protected void initChannel(SocketChannel ch) {
                     System.out.println("Initializing channel...");
                     ch.pipeline().addLast(new io.netty.handler.codec.string.StringDecoder());
                     ch.pipeline().addLast(new SimpleChannelInboundHandler<String>() {
                         @Override
                         protected void channelRead0(ChannelHandlerContext ctx, String msg) {
                             System.out.println("Received a message: " + msg);
                             Map<String, String> headers = new HashMap<>();
                             String[] parts = msg.split("\n\n", 2);
                             if (parts.length == 2) {
                                 for (String line : parts[0].split("\n")) {
                                     String[] kv = line.split(":", 2);
                                     if (kv.length == 2) {
                                         headers.put(kv[0].trim(), kv[1].trim());
                                     }
                                 }
                             }

                             System.out.println("Extracting context from headers...");
                             System.out.println("Headers: " + headers);
                             Context context = ContextPropagatorHelper.extract(headers);

                             // Debug print of extracted context
                             Span extractedSpan = Span.fromContext(context);
                             System.out.println("Extracted context - TraceId: " + extractedSpan.getSpanContext().getTraceId() +
                                             ", SpanId: " + extractedSpan.getSpanContext().getSpanId() +
                                             ", Valid: " + extractedSpan.getSpanContext().isValid());

                             Span span = tracer.spanBuilder("server-receive")
                                     .setSpanKind(io.opentelemetry.api.trace.SpanKind.SERVER)
                                     .setParent(context)
                                     .setAttribute("service.name", "server")
                                     .setAttribute("deployment.environment", "nettytest")
                                     .setAttribute("version", "1.0.0")
                                     .startSpan();
                             try (var scope = span.makeCurrent()) {
                                 if (parts.length > 1) {
                                     System.out.println("Processing message payload: " + parts[1]);
                                 } else {
                                     System.out.println("Message payload is missing.");
                                 }
                             } finally {
                                 System.out.println("Ending span...");
                                 span.end();
                             }
                         }

                         @Override
                         public void channelReadComplete(ChannelHandlerContext ctx) {
                             System.out.println("Read complete, flushing context...");
                             ctx.flush();
                         }
                     });
                 }
             });

            System.out.println("Binding server to port 8080...");
            b.bind(8080).sync().channel().closeFuture().sync();
        } finally {
            System.out.println("Shutting down event loop groups...");
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }
}
