import 'dart:async';
import 'dart:convert';

import 'package:flutter/services.dart';
import 'package:nearby_plugin/src/nearby_message.dart';
import 'package:nearby_plugin/src/nearby_strategy.dart';
import 'package:nearby_plugin/src/permission_manager.dart';

/// Defines the method channel and invoke the Java methods.
class TransferManager {
  static const String _methodName = 'nearby.lib/method.channel';
  static const String _eventName = 'nearby.lib/event.channel';

  static const MethodChannel _methodCh = MethodChannel(_methodName);
  static const EventChannel _eventCh = EventChannel(_eventName);

  late StreamController<NearbyMessage> _controller;
  final NearbyStrategy _strategy;

  TransferManager(this._strategy) {
    _controller = StreamController<NearbyMessage>.broadcast();

    _eventCh.receiveBroadcastStream().listen((event) {
      var msg = NearbyMessage.fromMap(
          jsonDecode(jsonEncode(event).toString()) as Map<String, dynamic>);
      _controller.add(msg);
    });
  }

  Stream<NearbyMessage> get eventStream => _controller.stream;

  /// Enable the plugin.
  Future<bool> enable(String name) async {
    var granted = await PermissionManager.requestPermissions();
    if (granted) {
      _methodCh.invokeMethod('startAdvertising', <String, dynamic>{
        'name': name,
        'strategy': _strategy.index,
      });
    }
    return granted;
  }

  /// Disable the plugin.
  void disable() {
    _methodCh.invokeMethod('stopAdvertising');
  }

  /// Send data to a destination.
  /// Supports large data (more than 1MB).
  void sendPayload(Object data, String destination) {
    var encoded = const Utf8Encoder().convert(const JsonCodec().encode(data));
    _methodCh.invokeMethod('sendPayload', <String, dynamic>{
      'data': encoded,
      'destination': destination,
    });
  }

  /// Send data to all connected devices.
  /// Supports data smaller than 1MB.
  void broadcast(Object data) {
    var encoded = const JsonCodec().encode(data);
    _methodCh.invokeMethod('broadcast', encoded);
  }

  /// Send data to all connected devices except the ones specified in the list.
  /// Supports data smaller than 1MB.
  void broadcastExcept(Object data, List<String> endpoints) {
    var encoded = const JsonCodec().encode(data);
    _methodCh.invokeMethod('broadcastExcept', <String, dynamic>{
      'data': encoded,
      'endpoints': endpoints,
    });
  }

  /// Start a discovery process for a certain amount of time (in ms).
  void discovery(int ms) {
    _methodCh.invokeMethod('startDiscovery');
    Future.delayed(Duration(milliseconds: ms), () {
      _methodCh.invokeMethod('stopDiscovery');
    });
  }

  /// Establish a connection with the given endpoint.
  void connect(String endpoint) {
    _methodCh.invokeMethod('connect', endpoint);
  }

  /// Disconnect from all connected devices.
  void disconnectAll() {
    _methodCh.invokeMethod('disconnectAll');
  }
}
