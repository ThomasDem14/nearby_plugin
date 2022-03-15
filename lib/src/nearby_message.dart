import 'dart:convert';

import 'package:nearby_plugin/nearby_plugin.dart';

class NearbyMessage {
  /// Type of the message.
  NearbyMessageType type;

  /// Endpoint id.
  String? endpointId;

  /// Source of the message.
  String? endpoint;

  /// Payload id.
  String? payloadId;

  /// Payload of the message.
  String? payload;

  int? payloadStatus;

  NearbyMessage(this.type, this.endpointId, this.endpoint, this.payloadId,
      this.payload, this.payloadStatus);

  factory NearbyMessage.fromMap(Map<String, dynamic> from) {
    return NearbyMessage(
      getNearbyMessageTypeFromString(from['type'])!,
      from['endpointId'].toString(),
      from['endpoint'].toString(),
      from['payloadId'].toString(),
      from['payload'].toString(),
      from['payloadStatus'],
    );
  }
}
