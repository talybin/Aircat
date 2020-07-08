# Aircat

WPA cracking tool for Android. Ugly but functional. Currently **requires root**.

### What is special about this app?

Unlike other similar tools this app do not require monitor mode support for wireless card. This makes it able to run on any Android device.

### How does it work?

Aircat targets the management frames used during roaming to obtain the PMKID (the unique key identifier used by the AP to keep track of the PMK being used for the client). While not all AP has support for this, many do.
Aircat establish a connection to selected AP with a simple password and listens for management response frame. If response contains a PMKID it is added as a job. Providing a word list, the password can be later found by running a job.

### Supported word lists

A word list file should contain a list of passwords separated by a white space (tab, next line, etc...). File may be a plain text or compressed by gzip or zip.
Note that a WPA password has length between 8 and 64. While this is not requirement for Aircat, removing all words less than 8 characters may slightly speed up the process.

```sh
# Remove lines that are less than 8 characters long
sed -r '/^.{,7}$/d' filename

# Remove duplicates
awk '!(count[$0]++)' filename
```

Aircat has a built-in password list, rockyou.txt.gz. It contains most used passwords and prepared for WPA password length.

### Build

Use [Android Studio](https://developer.android.com/studio) to build the project.

### Latest release

[Aircat version 1.0.](https://github.com/talybin/Aircat/releases/download/v1.0/aircat-1.0.apk)
