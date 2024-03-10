# jWHOIS

Swing desktop application to make quick WHOIS info lookups for domains and IP addresses.

<img src="https://i.imgur.com/EEaXkR5.png" width="700" />

- Recursively WHOIS queries responses from lookup servers until the final result is returned
- Lookup domains for registration info or ip addresses for netrange info
- Configurable initial lookup server for edge cases I may not have handled

### Requirements

- Java 11

### Build and Run

During development, run the following command to build and run the application.

```sh
$ ./gradlew run
```

### Package

Before a release, run the following command to package the files into the `libs/` folder to be zipped up.

```sh
$ ./gradlew packageJar
```