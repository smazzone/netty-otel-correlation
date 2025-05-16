package nettyotel;

import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.context.Context;

import java.util.Map;


public class ContextPropagatorHelper {
    public static final TextMapGetter<Map<String, String>> getter = new TextMapGetter<>() {
        public Iterable<String> keys(Map<String, String> carrier) {
            return carrier.keySet();
        }

        public String get(Map<String, String> carrier, String key) {
            return carrier.get(key);
        }
    };

    public static final TextMapSetter<Map<String, String>> setter = Map::put;

    public static void inject(Context context, Map<String, String> carrier) {
        ensureInitialized();
        GlobalOpenTelemetry.getPropagators().getTextMapPropagator().inject(context, carrier, setter);
    }

    public static Context extract(Map<String, String> carrier) {
        ensureInitialized();
        return GlobalOpenTelemetry.getPropagators().getTextMapPropagator().extract(Context.current(), carrier, getter);
    }

    private static void ensureInitialized() {
        if (GlobalOpenTelemetry.get() == null) {
            throw new IllegalStateException("GlobalOpenTelemetry is not initialized. Call OTelInitializer.initialize() first.");
        }
    }
}