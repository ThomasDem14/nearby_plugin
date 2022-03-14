package com.montefiore.demoulin.nearby_plugin;

import android.content.Context;

import androidx.annotation.NonNull;

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
  private Context context;

  private NearbyManager nearbyManager;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    this.context = flutterPluginBinding.getApplicationContext();

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
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    switch (call.method) {
      case "startAdvertising":
        nearbyManager = new NearbyManager(context, eventSink);
        final String name = call.arguments();
        nearbyManager.startAdvertising(name);
        break;
      case "stopAdvertising":
        nearbyManager.stopAdvertising();
        break;
      case "startDiscovery":
        nearbyManager.startDiscovery();
        break;
      case "stopDiscovery":
        nearbyManager.stopDiscovery();
        break;
      case "connect":
        final String endpoint = call.arguments();
        nearbyManager.connect(endpoint);
        break;
      case "disconnectAll":
        nearbyManager.disconnectAll();
        break;
      case "broadcast":
        nearbyManager.broadcast(call.arguments().toString().getBytes());
        break;
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
