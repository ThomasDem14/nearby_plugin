import 'dart:async';
import 'dart:convert';
import 'dart:math';

import 'package:flutter/services.dart';
import 'package:nearby_plugin/src/nearby_message.dart';
import 'package:nearby_plugin/src/permission_manager.dart';

/// Defines the method channel and invoke the Java methods.
class TransferManager {
  static const String _methodName = 'nearby.lib/method.channel';
  static const String _eventName = 'nearby.lib/event.channel';

  static const MethodChannel _methodCh = MethodChannel(_methodName);
  static const EventChannel _eventCh = EventChannel(_eventName);

  late StreamController<NearbyMessage> _controller;

  TransferManager() {
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
      _methodCh.invokeMethod('startAdvertising', _uniqueName(name));
    }
    return granted;
  }

  /// Transfrorm a name into a unique one by adding 4 random digits.
  String _uniqueName(String name) {
    var rng = Random();
    var code = rng.nextInt(9000) + 1000;
    return name + "#" + code.toString();
  }

  /// Disable the plugin.
  void disable() {
    _methodCh.invokeMethod('stopAdvertising');
  }

  /// Send data to all connected devices.
  void broadcast(String data) {
    _methodCh.invokeMethod('broadcast', data);
  }

  /// Send data to all connected devices except the ones specified in the list.
  void broadcastExcept(String data, List<String> endpoints) {
    _methodCh.invokeMethod('broadcast', <String, dynamic>{
      'data': data,
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
