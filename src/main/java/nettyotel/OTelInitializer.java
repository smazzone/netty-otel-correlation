package nettyotel;


import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;


public class OTelInitializer {

    private static boolean initialized = false;
    private static SdkTracerProvider tracerProvider;
    public static void initialize() {
        if (initialized) {
                System.out.println("OpenTelemetry is already initialized.");
                return;
            }

        String exporterType = System.getenv().getOrDefault("OTEL_EXPORTER_TYPE", "grpc");
        System.out.println("Using exporter type: " + exporterType);
        BatchSpanProcessor spanProcessor;

        if ("http".equalsIgnoreCase(exporterType)) {
            OtlpHttpSpanExporter httpExporter = OtlpHttpSpanExporter.builder()
                    .setEndpoint(System.getenv().getOrDefault("OTEL_EXPORTER_OTLP_HTTP_ENDPOINT", "http://collector:4318"))
                    .build();
            spanProcessor = BatchSpanProcessor.builder(httpExporter).build();
        } else {
            OtlpGrpcSpanExporter grpcExporter = OtlpGrpcSpanExporter.builder()
                    .setEndpoint(System.getenv().getOrDefault("OTEL_EXPORTER_OTLP_ENDPOINT", "http://collector:4317"))
                    .build();
            spanProcessor = BatchSpanProcessor.builder(grpcExporter).build();
        }
    
        tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(spanProcessor)
                .setSampler(Sampler.alwaysOn())
                .setResource(Resource.getDefault().toBuilder()
                        .build())
                .build();
    
        OpenTelemetrySdk.builder()
        .setTracerProvider(tracerProvider)
        .setPropagators(ContextPropagators.create(
                TextMapPropagator.composite(
                                W3CTraceContextPropagator.getInstance(),
                                W3CBaggagePropagator.getInstance()
                )
        ))
        .buildAndRegisterGlobal();

        initialized = true;
        System.out.println("OpenTelemetry initialized.");
    }

    public static void shutdown() {
        if (tracerProvider != null) {
            System.out.println("Flushing telemetry data...");
            tracerProvider.forceFlush().join(10, java.util.concurrent.TimeUnit.SECONDS);
            System.out.println("Telemetry flushed.");

            System.out.println("Shutting down OpenTelemetry...");
            tracerProvider.shutdown().join(10, java.util.concurrent.TimeUnit.SECONDS);
            System.out.println("OpenTelemetry shutdown completed.");
        } else {
            System.out.println("No tracer provider to shutdown.");
        }
    }    
    


}


