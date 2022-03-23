import 'package:permission_handler/permission_handler.dart';

class PermissionManager {
  static Future<bool> requestPermissions() async {
    var statuses = await [
      Permission.bluetooth,
      Permission.bluetoothAdvertise,
      Permission.bluetoothConnect,
      Permission.bluetoothScan,
      Permission.location,
    ].request();
    for (var status in statuses.values) {
      if (status.isDenied) {
        return false;
      }
    }
    return true;
  }
}
