enum NearbyMessageType {
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

NearbyMessageType? getNearbyMessageTypeFromString(String typeString) {
  if (typeString == "onDiscoveryStarted") {
    return NearbyMessageType.onDiscoveryStarted;
  } else if (typeString == "onEndpointDiscovered") {
    return NearbyMessageType.onEndpointDiscovered;
  } else if (typeString == "onEndpointLost") {
    return NearbyMessageType.onEndpointLost;
  } else if (typeString == "onDiscoveryEnded") {
    return NearbyMessageType.onDiscoveryEnded;
  } else if (typeString == "onConnectionRequested") {
    return NearbyMessageType.onConnectionRequested;
  } else if (typeString == "onConnectionRequestFailed") {
    return NearbyMessageType.onConnectionRequestFailed;
  } else if (typeString == "onConnectionAccepted") {
    return NearbyMessageType.onConnectionAccepted;
  } else if (typeString == "onConnectionRejected") {
    return NearbyMessageType.onConnectionRejected;
  } else if (typeString == "onConnectionEnded") {
    return NearbyMessageType.onConnectionEnded;
  } else if (typeString == "onPayloadReceived") {
    return NearbyMessageType.onPayloadReceived;
  } else {
    return null;
  }
}
