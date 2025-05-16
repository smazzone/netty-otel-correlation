package nettyotel;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class NettyClient {
    // Tracer will be initialized locally in the main method

    public static void main(String[] args) throws Exception {
        // Initialize OpenTelemetry
        OTelInitializer.initialize(); // Ensure this method uses OpenTelemetrySdkBuilder.buildAndRegisterGlobal
        System.out.println("OpenTelemetry initialized.");
        Tracer tracer = GlobalOpenTelemetry.getTracer("tcp-client");

        System.out.println("Tracer initialized.");
        System.out.println("Starting NettyClient and creating a span...");
        // Create a new span for the client
        Span span = tracer.spanBuilder("client-send")
              .setSpanKind(io.opentelemetry.api.trace.SpanKind.CLIENT)
              .setAttribute("service.name", "client")
              .setAttribute("deployment.environment", "nettytest")
              .setAttribute("version", "1.0.0")
              .startSpan();
        try (var scope = span.makeCurrent()) {
            System.out.println("Span started and made current.");

            // Inject OpenTelemetry context
            Map<String, String> headers = new HashMap<>();

            ContextPropagatorHelper.inject(Context.current(), headers);
            System.out.println("Context injected into headers.");

            // Print headers for debugging
            System.out.println("Headers before sending:");
 
            System.out.println("Injected context - TraceId: " + span.getSpanContext().getTraceId());
            System.out.println("SpanId: " + span.getSpanContext().getSpanId());
            System.out.println("TraceFlags: " + span.getSpanContext().getTraceFlags());            
             // Build message with headers followed by double newline
            StringBuilder message = new StringBuilder();
            headers.forEach((k, v) -> {
                message.append(k).append(":").append(v).append("\n");
            });
            message.append("\nHello from the Client");
            System.out.println("Message built: " + message);

            EventLoopGroup group = new NioEventLoopGroup();
            try {
                System.out.println("Initializing Bootstrap...");
                Bootstrap b = new Bootstrap();
                b.group(group)
                 .channel(NioSocketChannel.class)
                 .handler(new ChannelInitializer<>() {
                     @Override
                     protected void initChannel(Channel ch) {
                         System.out.println("Initializing channel...");
                         ch.pipeline().addLast(new SimpleChannelInboundHandler<String>() {
                             @Override
                             protected void channelRead0(ChannelHandlerContext ctx, String msg) {
                                 System.out.println("Received message from server: " + msg);
                                 // handle server response if needed
                             }

                             @Override
                             public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                 System.out.println("Exception caught in channel: " + cause.getMessage());
                                 cause.printStackTrace();
                                 ctx.close();
                             }
                         });
                     }
                 });

                // Connect and send ByteBuf message
                System.out.println("Connecting to server...");
                //to run standalone, use localhost and port 8080
                //ChannelFuture f = b.connect("localhost", 8080).sync();
                //to run in docker use the container name and port 8080
                ChannelFuture f = b.connect("server", 8080).sync();
                System.out.println("Connected to server. Sending message...");
                // f.channel().writeAndFlush(Unpooled.copiedBuffer(message.toString(), StandardCharsets.UTF_8)).sync();
                // System.out.println("Message: " + message.toString() + " has been sent to Server. \nWaiting for channel to close...");
                // f.channel().closeFuture().sync();
                f.channel().writeAndFlush(Unpooled.copiedBuffer(message.toString(), StandardCharsets.UTF_8)).sync();
                System.out.println("Message: " + message.toString() + " has been sent to Server. \nClosing channel manually...");
                f.channel().close().sync();                
                System.out.println("Channel closed.");                
            } finally {
                System.out.println("Shutting down event loop group...");
                group.shutdownGracefully();
            }
        } finally {
            System.out.println("Ending span...");
            span.end();
            System.out.println("Span ended.");
            // Shutdown OpenTelemetry
            OTelInitializer.shutdown(); // Ensure this method uses OpenTelemetrySdkBuilder.shutdown()
            System.out.println("OpenTelemetry shutdown completed.");

        }
        System.out.println("NettyClient execution completed.");
    }
}
