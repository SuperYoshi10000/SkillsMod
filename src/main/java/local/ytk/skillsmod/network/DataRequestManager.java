package local.ytk.skillsmod.network;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Deprecated(forRemoval = true)
public class DataRequestManager<P extends PlayerEntity> {
    final BiConsumer<P, RequestDataPayload> sendFunction;
    final Map<String, Request<?>> DATA_REQUESTS = new HashMap<>();
    public DataRequestManager(BiConsumer<P, RequestDataPayload> sendFunction) {
        this.sendFunction = sendFunction;
    }
    
    public <T> void sendRequest(P player, List<String> keys, ProvideDataPayload.Template<T> template, Consumer<Map<String, T>> callback) {
        String id;
        do id = UUID.randomUUID().toString(); while (DATA_REQUESTS.containsKey(id));
        Request<T> request = new Request<>(id, template, callback);
        DATA_REQUESTS.put(id, request);
        // Send the request to the server
        RequestDataPayload payload = new RequestDataPayload(id, keys);
        sendFunction.accept(player, payload);
    }
    public <T> Optional<Map<String, T>> receiveResult(ProvideDataPayload<T> payload) {
        String id = payload.id();
        @SuppressWarnings("unchecked")
        Request<T> request = (Request<T>) DATA_REQUESTS.remove(id);
        // Handle unknown request
        if (request == null) return Optional.empty();
        // Get the data from the payload
        Map<String, T> result = request.template().decode(payload);
        // Call the callback with the payload
        request.callback.accept(result);
        // Return the result
        return Optional.of(result);
    }
    
    record Request<T>(String key, ProvideDataPayload.Template<T> template, Consumer<Map<String, T>> callback) {}
}
