# IPHub (RetroMC)
A flexible VPN blocking plugin for Minecraft b1.7.3 (Poseidon)

Modified to support Beta Evolutions VPN bypass.

# Features
* Commercial IP blocking
* Passthrough list for both usernames & IP addresses
* Customizable messages for staff & player kicks.
* Backup API key for free users (like us) who want an extra 1k requests per day.

# Configuration
You will find the configuration inside <code>plugins/IPHub/config.yml</code>

Here is an example of what the configuration should look like:
```
settings:
  passthrough:
      enabled: true
      ipList: 0.0.0.0,127.0.0.1,192.168.1.1
      nameList: Steve,Notch,Jeb_
  logging:
      enabled: true
      msgFormat: '&c{player}: {cnCode} {ip}, {isp} ({asn})'
  messages:
      vpnDetected: '&cVPN detected'
      vpnDetectedNotif: '&cKICKED: &e{player} &cdetected with VPN'
      notChecked: '&cFailed to check IP'
      vpnPossible: '&e{player} might have a VPN'
      checkingError: '&cError while checking {player} for VPN'
  developer:
      debug: false
      disclaimer: ONLY ENABLE THIS SETTING IF YOU KNOW WHAT YOU ARE DOING
  api:
      key: YOUR_API_KEY_HERE
      backupKey: YOUR_BACKUP_API_KEY_HERE___PLEASE_USE_THIS
```
