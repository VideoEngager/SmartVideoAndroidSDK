VideoEngager - Client SDK
===

This is the VideoEngager Client SDK for Android. Embedding it into your application will allow your users to call an agent.

# Usage

## Install

Add the necessary libraries into your `build.gradle` file:

```
buildTypes {
    ...
    packagingOptions {
        exclude 'lib/arm64-v8a/*'
        exclude 'lib/armeabi/*'
        exclude 'lib/mips64/*'
        exclude 'lib/mips/*'
        exclude 'lib/x86_64/*'
    }
}
```


```
repositories {
    ...
    flatDir {
        dirs 'libs'
    }
}
...
dependencies {
    ...
    compile(name:'core-sdk-release', ext:'aar')
    compile(name:'videolibrary-release', ext:'aar')
    compile(name:'clientsdk-release', ext:'aar')

    compile 'com.android.support:appcompat-v7:24.+'
    compile 'com.android.support:design:24.2.1'
    compile 'com.squareup.retrofit2:retrofit:2.3.0'
    compile 'org.webrtc:google-webrtc:1.0.20371'
    compile 'com.squareup.picasso:picasso:2.5.2'
    compile 'com.android.support:recyclerview-v7:24.2.1'
}
```

**Note**: `minSdkVersion` for the SDK is 19.

## Permissions

These permissions should be added to the application's AndroidManifext.xml file:

```
    <uses-permission android:name="android.permission.INTERNET"/>
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE"/>
    <uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS"/>
    <uses-permission android:name="android.permission.RECORD_AUDIO"/>
    <uses-permission android:name="android.permission.READ_PHONE_STATE"/>
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
    <uses-permission android:name="android.permission.WAKE_LOCK"/>
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
    <uses-permission android:name="android.permission.VIBRATE"/>
    <uses-permission android:name="android.permission.CHANGE_NETWORK_STATE"/>
    <uses-permission android:name="android.permission.CAMERA"/>
    <uses-permission android:name="android.permission.BLUETOOTH"/>
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>

    <uses-feature android:name="android.hardware.telephony" android:required="false"/>
    <uses-feature android:name="android.hardware.camera"/>
    <uses-feature android:name="android.hardware.camera.autofocus" android:required="false"/>
    <uses-feature android:name="android.hardware.camera.front" android:required="false"/>
    <uses-feature android:glEsVersion="0x00020000" android:required="true"/>
```

**Note**: the application is responsible for requesting the dangerous permissions.

## Other AndroidManifest changes

The UI of the SDK is implemented inside an activity - VideoActivity.

```
<activity
    android:theme="@style/AppTheme"
    android:name="com.videoengager.clientsdk.VideoActivity"
    android:configChanges="keyboardHidden|orientation|screenSize|screenLayout"
    android:launchMode="singleTask"
    android:screenOrientation="fullSensor"
    android:showOnLockScreen="true">
</activity>
```

## Obfuscating

These lines should be added to the ProGuard Rules file:
```
-keep class net.sqlcipher.** {
    *;
}

-keep class net.sqlcipher.database.** {
    *;
}
```

## SDK API

### Obtain instance

The SDK is implemented as an singleton:

```
VideoClient videoClient = VideoClient.getInstance(appContext);
```

### Init

Call the init method to connect to an agent:
```
videoClient.init(<agent-path>, <client-display-name>, <client-email>, <client-phone>);
```

The last 3 parameters (the client info) are not mandatory.

### Call the agent

To initiate a call:

```
videoClient.callAgent(false, false);
```
The first parameter is whether to initiate audio only call (disable video).
The second parameter is whether to initiate only a chat session (the first parameter is ignored).

**Note:** Make sure the SDK is connected before performing this API call (see [Listen for events](#listen-for-events) below).

### Logout

To disconnect the SDK:
```
videoClient.logout();
```

### Set background mode

While connected the SDK keeps a permanent connection to the VideoEngager backend.
To avoid battery drain while the calling application is not on foreground, set the SDK in background too:
```
videoClient.setInBackground(true);
```

### Check availability: audio/video

After the SDK is connected and before calling the agent, check if the agent is available for audio/video calls: `videoClient.isVideoAudioAvailable()`

### Check availability: chat

After the SDK is connected, check if the agent is available for chat: `videoClient.isChatAvailable()`

### Listen for events
To subscribe for the events call:
```
videoClient.setEventsListener(new VideoClient.VideoClientEventsListener() {
    @Override
    public void onInitResult(boolean success) {
    }

    @Override
    public void onAudioVideoCallStateChanged(VideoClient.CallState newCallState, boolean incoming) {
    }

    @Override
    public void onChatCallStateChanged(VideoClient.CallState newCallState, boolean incoming) {
    }

    @Override
    public void onNewChatMessage() {
    }

    @Override
    public void onAgentStatusUpdate(boolean availableForCalls, boolean availableForChat) {
    }
});
```

### Genesys flow

The SDK supports video calls to the Genesys workspace (which should has the VideoEngager integrated). 

#### Init

```
videoClient.initGenesysClient(<genesys-url>, <agent-url>, <client-first-name>, <client-last-name>, <client-email>, <subject>);
```

#### Call the agent

Upon receiving the event for successful init the client can call the agent. Only video calls are supported.
```
videoClient.callGenesysAgent(<is-audio-only>);
```
