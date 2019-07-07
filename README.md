# cordova-plugin-kh-foreground-service 
---

This plugin allows for android devices to continue running services in the background, using a foreground ongoing notification. This is targeted towards use with plugins such as 'cordova-geolocation' that will not run while the app is in the background on android API 21+.

---

## Requirements

- com.nks.kh >= 1.0.0
- cordova version >= 8.0.0
- cordova-android version >= 6.0.0
- android-sdk api >= 21

---

## Setup and Usage

### Install

```shell
cordova plugin add cordova-plugin-kh-foreground-service

#or

cordova plugin add https://github.com/orgixmh/cordova-plugin-kh-foreground-service
```

### Start Method

To enable the foreground service, call the `start` method:

```javascript

//Start service
cordova.plugins.KhforegroundService.start('Foreground Service Running Message', 'Device found message');

//Check for pending notification requests
cordova.plugins.KhforegroundService.check(SUCCESS_CALLBACK, FAILED_CALLBACK);

```

### Stop Method

To disable the foreground service, call the `stop` method:

```javascript
cordova.plugins.KhforegroundService.stop();
```

---

Nikas Konstantinos (2019)
