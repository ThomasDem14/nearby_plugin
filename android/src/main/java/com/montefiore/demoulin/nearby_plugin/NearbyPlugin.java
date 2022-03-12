package com.montefiore.demoulin.nearby_plugin;

import androidx.annotation.NonNull;

import java.util.List;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

/// Defines the method & event channels and receives the method calls.
/// Dispatch them to the NearbyManager.
public class NearbyPlugin implements FlutterPlugin, MethodCallHandler {
  private static final String METHOD_NAME = "nearby.lib/method.channel";
  private static final String EVENT_NAME = "nearby.lib/event.channel";

  private MethodChannel channel;
  private EventChannel eventChannel;
  private EventChannel.EventSink eventSink;

  private NearbyManager nearbyManager;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    BinaryMessenger messenger = flutterPluginBinding.getBinaryMessenger();

    channel = new MethodChannel(messenger, METHOD_NAME);
    channel.setMethodCallHandler(this);

    eventChannel = new EventChannel(messenger, EVENT_NAME);
    eventChannel.setStreamHandler(new EventChannel.StreamHandler() {
      @Override
      public void onListen(Object arguments, EventChannel.EventSink events) {
        eventSink = events;
      }
      @Override
      public void onCancel(Object arguments) {
        eventSink = null;
        eventChannel.setStreamHandler(null);
        eventChannel = null;
      }
    });

    nearbyManager = new NearbyManager(flutterPluginBinding.getApplicationContext(), eventSink);
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    switch (call.method) {
      case "startAdvertising":
        final String name = call.arguments();
        nearbyManager.startAdvertising(name);
      case "stopAdvertising":
        nearbyManager.stopAdvertising();
      case "startDiscovery":
        nearbyManager.startDiscovery();
        break;
      case "stopDiscovery":
        nearbyManager.stopDiscovery();
        break;
      case "connect":
        final String[] args = call.arguments();
        nearbyManager.connect(args[0], args[1]);
        break;
      case "disconnectAll":
        nearbyManager.disconnectAll();
        break;
      case "sendMessage":
        final Object[] data = call.arguments();
        nearbyManager.sendMessage((String) data[0], (byte[]) data[1]);
      case "broadcast":
        final Object[] msg = call.arguments();
        nearbyManager.broadcast((List<String>) msg[0], (byte[]) msg[1]);
      default:
        result.notImplemented();
        break;
    }
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    //nearbyManager.close();
    channel.setMethodCallHandler(null);
  }
}
