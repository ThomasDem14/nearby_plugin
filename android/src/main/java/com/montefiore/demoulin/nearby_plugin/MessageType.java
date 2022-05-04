package com.montefiore.demoulin.nearby_plugin;

public enum MessageType {
    onDiscoveryStarted,
    onEndpointDiscovered,
    onEndpointLost,
    onDiscoveryEnded,

    onConnectionRequested,
    onConnectionRequestFailed,
    onConnectionAccepted,
    onConnectionRejected,
    onConnectionEnded,

    onPayloadReceived,
}
