import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:image_picker/image_picker.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  static const platform = MethodChannel('com.example/my_channel');
  static final GlobalKey<NavigatorState> navigatorKey = GlobalKey<NavigatorState>();

  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  File? _image;

  Future<void> _getImage(bool fromCamera) async {
    try {
      final ImagePicker picker = ImagePicker();
      final XFile? image = await picker.pickImage(
          source: fromCamera ? ImageSource.camera : ImageSource.gallery);

      if (image != null) {
        setState(() {
          _image = File(image.path);
        });
        // Send the thumbnail back to native Kotlin
        await  MyApp.platform.invokeMethod('setThumbnail', image.path);
      }
    } on PlatformException catch (e) {
      print("Failed to pick image: '${e.message}'.");
    }
  }
  @override
  void initState() {
    super.initState();
   MyApp.platform.setMethodCallHandler(_receiveFromNative);
  }

  Future<void> _receiveFromNative(MethodCall call) async {
    if (call.method == "openCamera") {
      print(call.arguments);
      return _getImage(true);
    } else if (call.method == "openGallery") {
      return _getImage(false);
    }
  }

  @override
  Widget build(BuildContext context) {

    return MaterialApp(
      navigatorKey: MyApp.navigatorKey, // Attach the global navigator key to MaterialApp
      home: Scaffold(
        appBar: AppBar(title: const Text('Flutter - Native Button Example')),
        body: Center(
          child: ElevatedButton(
            onPressed: _createNativeButton,
            child: const Text('Create Native Button'),
          ),
        ),
      ),
    );
  }

  void _showAlert() {
    // Use navigatorKey to get the correct context for showing the dialog
    showDialog(
      context: MyApp.navigatorKey.currentState!.overlay!.context, // Correct context
      builder: (_) => AlertDialog(
        title: const Text('Green Button Clicked'),
        content: const Text('The green button was clicked in native Android code and triggered this alert.'),
        actions: [
          TextButton(
            onPressed: () => MyApp.navigatorKey.currentState!.pop(), // Use navigator for dismissal
            child: const Text('OK'),
          ),
        ],
      ),
    );
  }

  Future<void> _createNativeButton() async {
    try {
      await MyApp.platform.invokeMethod('createNativeButton');
    } on PlatformException catch (e) {
      print("Failed to create native button: '${e.message}'.");
    }
  }
}