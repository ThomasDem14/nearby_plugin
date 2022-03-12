import 'dart:typed_data';

import 'package:nearby_plugin/nearby_plugin.dart';

class NearbyMessage {
  NearbyMessageType type;
  String? endpoint;
  String? id;
  Uint8List? payload;

  NearbyMessage(this.type, this.endpoint, this.id, this.payload);

  factory NearbyMessage.fromMap(Map<String, dynamic> from) {
    return NearbyMessage(
      from['uuid'],
      from['endpoint'],
      from['id'],
      from['payload'],
    );
  }
}
