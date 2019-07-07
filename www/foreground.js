var exec = require('cordova/exec');

module.exports = {
  start: function(title, text, icon, importance, notificationId) {
    exec(null, null, "KHForegroundPlugin", "start", [title || "", text || "", icon || "", importance || "1", notificationId || ""]);
  },
  check: function(success, failure) {
      exec(success, failure, "KHForegroundPlugin", "checkUnlock",[]);
    },
  stop: function() {
    exec(null, null, "KHForegroundPlugin", "stop", []);
  }
};
