import 'dart:async';

import 'package:flutter/services.dart';
import 'package:nearby_plugin/src/nearby_message.dart';

/// Defines the method channel and invoke the Java methods.
class TransferManager {
  static const String _methodName = 'naerby.lib/method.channel';
  static const String _eventName = 'nearby.lib/event.channel';

  static const MethodChannel _methodCh = MethodChannel(_methodName);
  static const EventChannel _eventCh = EventChannel(_eventName);

  late StreamController<NearbyMessage> _controller;

  TransferManager() {
    _controller = StreamController<NearbyMessage>.broadcast();

    _eventCh.receiveBroadcastStream().listen((event) {
      var msg = NearbyMessage.fromMap(event);
      _controller.add(msg);
    });
  }

  Stream get eventStream => _controller.stream;

  void enable() {
    _methodCh.invokeMethod('startAdvertising');
  }

  void disable() {
    _methodCh.invokeMethod('stopAdvertising');
  }

  void broadcast(Object data) {
    _methodCh.invokeMethod('broadcast', data);
  }

  void discovery(int ms) {
    _methodCh.invokeMethod('startDiscovery');
    Future.delayed(Duration(milliseconds: ms), () {
      _methodCh.invokeMethod('stopDiscovery');
    });
  }

  void connect(String endpoint, String name) {
    _methodCh.invokeMethod('connect', [endpoint, name]);
  }

  void disconnectAll() {
    _methodCh.invokeMethod('disconnectAll');
  }
}
