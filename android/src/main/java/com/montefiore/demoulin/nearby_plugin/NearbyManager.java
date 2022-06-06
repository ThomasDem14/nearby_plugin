package com.montefiore.demoulin.nearby_plugin;

import android.content.Context;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.collection.SimpleArrayMap;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import io.flutter.plugin.common.EventChannel;

/// Uses the Nearby API functions
public class NearbyManager {
    private final String TAG = "[NearbyManager]";
    private final String SERVICE_ID = "com.montefiore.demoulin.nearby_plugin";

    private final Context context;
    private final EventChannel.EventSink eventSink;

    private final SimpleArrayMap<Long, ByteArrayOutputStream> incomingPayloads = new SimpleArrayMap<>();
    private final SimpleArrayMap<Long, Thread> backgroundThreads = new SimpleArrayMap<>();
    private static final long READ_STREAM_IN_BG_TIMEOUT = 5000;

    private static class Endpoint {
        String id;
        String name;

        Endpoint(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    private final List<Endpoint> initiatedEndpoints = new ArrayList<>();
    private final List<Endpoint> connectedEndpoints = new ArrayList<>();

    private String name;
    private Strategy strategy;

    NearbyManager(Context context, EventChannel.EventSink eventSink) {
        this.context = context;
        this.eventSink = eventSink;
    }

    public void startAdvertising(@NonNull String name, @NonNull Strategy strategy) {
        this.name = name;
        this.strategy = strategy;

        AdvertisingOptions advertisingOptions =
                new AdvertisingOptions.Builder().setStrategy(strategy).build();
        Nearby.getConnectionsClient(context)
                .startAdvertising(
                        name, SERVICE_ID, connectionLifecycleCallback, advertisingOptions)
                .addOnSuccessListener(
                        (Void unused) -> {
                            Log.i(TAG, "Start advertising");
                        })
                .addOnFailureListener(
                        (Exception e) -> {
                            Log.i(TAG, "Failed to start advertising: " + e.getMessage());
                        });
    }

    public void stopAdvertising() {
        Log.i(TAG, "Stop advertising");
        Nearby.getConnectionsClient(context).stopAdvertising();
    }

    public void startDiscovery() {
        DiscoveryOptions discoveryOptions =
                new DiscoveryOptions.Builder().setStrategy(this.strategy).build();
        Nearby.getConnectionsClient(context)
                .startDiscovery(SERVICE_ID, endpointDiscoveryCallback, discoveryOptions)
                .addOnSuccessListener(
                        (Void unused) -> {
                            Log.i(TAG, "Start discovery");
                            HashMap<String, Object> mapInfoValue = new HashMap<>();
                            mapInfoValue.put("type", MessageType.onDiscoveryStarted.toString());
                            eventSink.success(mapInfoValue);
                        })
                .addOnFailureListener(
                        (Exception e) -> {
                            Log.i(TAG, "Failed to start discovery: " + e.getMessage());
                        });
    }

    public void stopDiscovery() {
        Log.i(TAG, "Stop discovery");
        Nearby.getConnectionsClient(context).stopDiscovery();
        HashMap<String, Object> mapInfoValue = new HashMap<>();
        mapInfoValue.put("type", MessageType.onDiscoveryEnded.toString());
        eventSink.success(mapInfoValue);
    }

    public void connect(@NonNull String endpointId) {
        Nearby.getConnectionsClient(context)
                .requestConnection(this.name, endpointId, connectionLifecycleCallback)
                .addOnSuccessListener(
                        (Void unused) -> {
                            Log.i(TAG, "Connection request to " + endpointId);
                            HashMap<String, Object> mapInfoValue = new HashMap<>();
                            mapInfoValue.put("type", MessageType.onConnectionRequested.toString());
                            mapInfoValue.put("endpointId", endpointId);
                            eventSink.success(mapInfoValue);
                        })
                .addOnFailureListener(
                        (Exception e) -> {
                            Log.i(TAG, "Failed to request connection: " + e.getMessage());
                            HashMap<String, Object> mapInfoValue = new HashMap<>();
                            mapInfoValue.put("type", MessageType.onConnectionRequestFailed.toString());
                            mapInfoValue.put("endpointId", endpointId);
                            eventSink.success(mapInfoValue);
                        });
    }

    public void disconnectAll() {
        Log.i(TAG, "Disconnect all");
        for(Endpoint endpoint : connectedEndpoints) {
            Nearby.getConnectionsClient(context).disconnectFromEndpoint(endpoint.id);
        }
    }

    public void sendPayload(@NonNull byte[] payload, @NonNull String destination) {
        Log.i(TAG, "Send message to " + destination);

        if (payload.length < ConnectionsClient.MAX_BYTES_DATA_SIZE) {
            Payload bytePayload = Payload.fromBytes(payload);
            Nearby.getConnectionsClient(context).sendPayload(destination, bytePayload);
        }
        else {
            try {
                ParcelFileDescriptor[] payloadPipe = ParcelFileDescriptor.createPipe();

                // Send read part to destination
                Nearby.getConnectionsClient(context)
                        .sendPayload(destination, Payload.fromStream(payloadPipe[0]));

                // Write payload on write part
                OutputStream os = new ParcelFileDescriptor.AutoCloseOutputStream(payloadPipe[1]);
                InputStream is = new ByteArrayInputStream(payload);
                // By bunch of 32KB
                int nRead;
                byte[] data = new byte[32768];
                while ((nRead = is.read(data, 0, data.length)) != -1) {
                    os.write(data, 0, nRead);
                }
                os.close();
            } catch (IOException e) {
                Log.e(TAG, "Failed to create payload: ", e);
            }
        }
    }

    public void broadcast(@NonNull String payload) {
        Log.i(TAG, "Broadcast message");

        List<String> list = new ArrayList<>();
        for (Endpoint e : connectedEndpoints) {
            list.add(e.id);
        }

        Payload bytePayload = Payload.fromBytes(payload.getBytes());
        Nearby.getConnectionsClient(context).sendPayload(list, bytePayload);
    }

    public void broadcastExcept(@NonNull String payload, @NonNull List<String> exceptList) {
        Log.i(TAG, "Broadcast message except " + exceptList.size());

        Payload bytePayload  = Payload.fromBytes(payload.getBytes());
        for (Endpoint endpoint : connectedEndpoints) {
            if (!exceptList.contains(endpoint.id)) {
                Nearby.getConnectionsClient(context).sendPayload(endpoint.id, bytePayload);
            }
        }
    }

    private final EndpointDiscoveryCallback endpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(@NonNull String endpointId, @NonNull DiscoveredEndpointInfo info) {
                    Log.i(TAG, "Endpoint found: " + endpointId);
                    HashMap<String, Object> mapInfoValue = new HashMap<>();
                    mapInfoValue.put("type", MessageType.onEndpointDiscovered.toString());
                    mapInfoValue.put("endpoint", info.getEndpointName());
                    mapInfoValue.put("endpointId", endpointId);
                    eventSink.success(mapInfoValue);
                }

                @Override
                public void onEndpointLost(@NonNull String endpointId) {
                    Log.i(TAG, "Endpoint lost: " + endpointId);
                    HashMap<String, Object> mapInfoValue = new HashMap<>();
                    mapInfoValue.put("type", MessageType.onEndpointLost.toString());
                    mapInfoValue.put("endpointId", endpointId);
                    eventSink.success(mapInfoValue);
                }
            };

    private final ConnectionLifecycleCallback connectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(@NonNull String endpointId, @NonNull ConnectionInfo connectionInfo) {
                    // Automatically accept the connection on both sides.
                    Nearby.getConnectionsClient(context).acceptConnection(endpointId, payloadCallback);
                    // Save the endpoint id and name in the initiated list
                    initiatedEndpoints.add(new Endpoint(endpointId, connectionInfo.getEndpointName()));
                }

                @Override
                public void onConnectionResult(@NonNull String endpointId, @NonNull ConnectionResolution result) {
                    HashMap<String, Object> mapInfoValue = new HashMap<>();

                    // Get the initiated endpoint associated to the given endpointId
                    Endpoint endpoint = null;
                    for (Endpoint e : initiatedEndpoints) {
                        if (e.id.equals(endpointId)) {
                            endpoint = e;
                        }
                    }
                    if (endpoint == null) {
                        endpoint = new Endpoint(endpointId, null);
                    }

                    switch (result.getStatus().getStatusCode()) {
                        case ConnectionsStatusCodes.STATUS_OK:
                            Log.i(TAG, "Connection successful with: " + endpoint.id);
                            // Save the endpoint in the list of connected devices
                            connectedEndpoints.add(endpoint);
                            // Notify
                            mapInfoValue.put("type", MessageType.onConnectionAccepted.toString());
                            mapInfoValue.put("endpointId", endpoint.id);
                            mapInfoValue.put("endpoint", endpoint.name);
                            eventSink.success(mapInfoValue);
                            break;
                        case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                        case ConnectionsStatusCodes.STATUS_ERROR:
                            Log.i(TAG, "Connection rejected/broke with: " + endpoint.id);
                            // Notify
                            mapInfoValue.put("type", MessageType.onConnectionRejected.toString());
                            mapInfoValue.put("endpointId", endpointId);
                            mapInfoValue.put("endpoint", endpoint.name);
                            eventSink.success(mapInfoValue);
                            break;
                        default:
                            // Unknown status code
                    }

                    // Remove the endpoint from the list of initiated devices
                    Iterator<Endpoint> iterator = initiatedEndpoints.iterator();
                    while (iterator.hasNext()) {
                        Endpoint e = iterator.next();
                        if (e.id.equals(endpointId)) {
                            iterator.remove();
                        }
                    }
                }

                @Override
                public void onDisconnected(@NonNull String endpointId) {
                    HashMap<String, Object> mapInfoValue = new HashMap<>();
                    Log.i(TAG, "Connection ended with: " + endpointId);
                    // Remove the endpoint from the list of connected devices
                    Iterator<Endpoint> iterator = connectedEndpoints.iterator();
                    while (iterator.hasNext()) {
                        Endpoint e = iterator.next();
                        if (e.id.equals(endpointId)) {
                            iterator.remove();
                        }
                    }

                    mapInfoValue.put("type", MessageType.onConnectionEnded.toString());
                    mapInfoValue.put("endpointId", endpointId);
                    eventSink.success(mapInfoValue);
                }
            };

    private final PayloadCallback payloadCallback =
            new PayloadCallback() {
                @Override
                public void onPayloadReceived(@NonNull String endpointId, @NonNull Payload payload) {
                    if (payload.getType() == Payload.Type.BYTES) {
                        HashMap<String, Object> mapInfoValue = new HashMap<>();
                        mapInfoValue.put("type", MessageType.onPayloadReceived.toString());
                        mapInfoValue.put("endpointId", endpointId);
                        mapInfoValue.put("payloadId", payload.getId());
                        mapInfoValue.put("payload", new String(payload.asBytes()));
                        eventSink.success(mapInfoValue);
                    }
                    else if (payload.getType() == Payload.Type.STREAM) {
                        Thread backgroundThread = new Thread() {
                            @Override
                            public void run() {
                                InputStream inputStream = Objects.requireNonNull(payload.asStream()).asInputStream();
                                ByteArrayOutputStream outputStream = Objects.requireNonNull(incomingPayloads.get(payload.getId()));
                                long lastRead = SystemClock.elapsedRealtime();
                                while (!Thread.interrupted()) {
                                    if ((SystemClock.elapsedRealtime() - lastRead) >= READ_STREAM_IN_BG_TIMEOUT) {
                                        Log.e(TAG, "Read data from stream but timed out.");
                                        break;
                                    }
                                    try {
                                        int availableBytes = inputStream.available();
                                        Log.i(TAG, "Read " + availableBytes + " bytes");
                                        if (availableBytes > 0) {
                                            byte[] bytes = new byte[availableBytes];
                                            if (inputStream.read(bytes) == availableBytes) {
                                                lastRead = SystemClock.elapsedRealtime();
                                                outputStream.write(bytes, 0, availableBytes);
                                            }
                                        }
                                    } catch (IOException e) {
                                        Log.e(TAG, "Failed to read bytes from InputStream.", e);
                                        break;
                                    } // try-catch
                                } // while
                            }
                        };
                        incomingPayloads.put(payload.getId(), new ByteArrayOutputStream());
                        backgroundThread.start();
                        backgroundThreads.put(payload.getId(), backgroundThread);
                    }
                }

                @Override
                public void onPayloadTransferUpdate(@NonNull String endpointId, @NonNull PayloadTransferUpdate update) {
                    if (incomingPayloads.containsKey(update.getPayloadId())
                            && update.getStatus() != PayloadTransferUpdate.Status.IN_PROGRESS) {

                        Objects.requireNonNull(backgroundThreads.get(update.getPayloadId())).interrupt();

                        ByteArrayOutputStream buffer =
                                Objects.requireNonNull(incomingPayloads.remove(update.getPayloadId()));

                        // Send payload to upper layer
                        HashMap<String, Object> mapInfoValue = new HashMap<>();
                        mapInfoValue.put("type", MessageType.onPayloadReceived.toString());
                        mapInfoValue.put("endpointId", endpointId);
                        mapInfoValue.put("payloadId", update.getPayloadId());
                        mapInfoValue.put("payload", buffer.toString());
                        eventSink.success(mapInfoValue);
                    }
                }
            };
}