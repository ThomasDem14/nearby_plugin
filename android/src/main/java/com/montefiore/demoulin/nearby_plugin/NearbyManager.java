package com.montefiore.demoulin.nearby_plugin;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.flutter.plugin.common.EventChannel;

/// Uses the Nearby API functions
public class NearbyManager {
    private final String TAG = "[NearbyManager]";
    private final Strategy STRATEGY = Strategy.P2P_CLUSTER;
    private final String SERVICE_ID = "com.montefiore.demoulin.nearby_plugin";

    private final Context context;
    private final EventChannel.EventSink eventSink;

    private final List<String> connectedEndpoints = new ArrayList<>();
    private String name;

    NearbyManager(Context context, EventChannel.EventSink eventSink) {
        this.context = context;
        this.eventSink = eventSink;
    }

    public void startAdvertising(@NonNull String name) {
        this.name = name;

        AdvertisingOptions advertisingOptions =
                new AdvertisingOptions.Builder().setStrategy(STRATEGY).build();
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
                new DiscoveryOptions.Builder().setStrategy(STRATEGY).build();
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
        for(String endpoint : connectedEndpoints) {
            Nearby.getConnectionsClient(context).disconnectFromEndpoint(endpoint);
        }
    }

    public void broadcast(@NonNull String payload) {
        Log.i(TAG, "Broadcast message");
        Nearby.getConnectionsClient(context)
                .sendPayload(connectedEndpoints, Payload.fromBytes(payload.getBytes()));
    }

    public void broadcastExcept(@NonNull String payload, @NonNull List<String> exceptList) {
        Log.i(TAG, "Broadcast message except " + exceptList.size());
        for (String endpoint : connectedEndpoints) {
            if (!exceptList.contains(endpoint)) {
                Nearby.getConnectionsClient(context)
                        .sendPayload(endpoint, Payload.fromBytes(payload.getBytes()));
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
                }

                @Override
                public void onConnectionResult(@NonNull String endpointId, ConnectionResolution result) {
                    HashMap<String, Object> mapInfoValue = new HashMap<>();
                    switch (result.getStatus().getStatusCode()) {
                        case ConnectionsStatusCodes.STATUS_OK:
                            Log.i(TAG, "Connected successful with: " + endpointId);
                            // Save the endpoint in the list of connected devices
                            connectedEndpoints.add(endpointId);

                            mapInfoValue.put("type", MessageType.onConnectionAccepted.toString());
                            mapInfoValue.put("endpointId", endpointId);
                            eventSink.success(mapInfoValue);
                            break;
                        case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                            Log.i(TAG, "Connection rejected with: " + endpointId);
                            mapInfoValue.put("type", MessageType.onConnectionRejected.toString());
                            mapInfoValue.put("endpointId", endpointId);
                            eventSink.success(mapInfoValue);
                            break;
                        case ConnectionsStatusCodes.STATUS_ERROR:
                            Log.i(TAG, "Connection broke with: " + endpointId);
                            mapInfoValue.put("type", MessageType.onConnectionEnded.toString());
                            mapInfoValue.put("endpointId", endpointId);
                            eventSink.success(mapInfoValue);
                            break;
                        default:
                            // Unknown status code
                    }
                }

                @Override
                public void onDisconnected(@NonNull String endpointId) {
                    HashMap<String, Object> mapInfoValue = new HashMap<>();
                    Log.i(TAG, "Connection ended with: " + endpointId);
                    // Remove the endpoint from the list of connected devices
                    connectedEndpoints.remove(endpointId);

                    mapInfoValue.put("type", MessageType.onConnectionEnded.toString());
                    mapInfoValue.put("endpointId", endpointId);
                    eventSink.success(mapInfoValue);
                }
            };

    private final PayloadCallback payloadCallback =
            new PayloadCallback() {
                @Override
                public void onPayloadReceived(@NonNull String endpointId, @NonNull Payload payload) {
                    HashMap<String, Object> mapInfoValue = new HashMap<>();
                    mapInfoValue.put("type", MessageType.onPayloadReceived.toString());
                    mapInfoValue.put("endpointId", endpointId);
                    mapInfoValue.put("payloadId", payload.getId());
                    mapInfoValue.put("payload", new String(payload.asBytes()));
                    eventSink.success(mapInfoValue);
                }

                @Override
                public void onPayloadTransferUpdate(@NonNull String endpointId, @NonNull PayloadTransferUpdate update) {
                    HashMap<String, Object> mapInfoValue = new HashMap<>();
                    mapInfoValue.put("type", MessageType.onPayloadTransferred.toString());
                    mapInfoValue.put("endpointId", endpointId);
                    mapInfoValue.put("payloadId", update.getPayloadId());
                    mapInfoValue.put("payloadStatus", update.getStatus());
                    eventSink.success(mapInfoValue);
                }
            };
}