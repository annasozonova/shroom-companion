# Shroom companion

Shroom companion is a lightweight Java desktop application featuring an animated pixel-art mushroom that reacts to user activity. It tracks mouse clicks and keyboard presses, unlocking new skins based on user engagement.

The project was inspired by the behavior and interactivity of the [Bongo Cat](https://store.steampowered.com/app/3419430/Bongo_Cat/) desktop application available on Steam.

## Features

- Animated mushroom character reacting to user input
- Click and keyboard tracking
- Unlockable skins system based on usage
- Custom wardrobe UI and themed scrollbar
- Minimal always-on-top window
- Local preferences-based storage (per user session)

## Technologies Used

- Java 17
- [JNativeHook](https://github.com/kwhat/jnativehook) for global keyboard and mouse listening
- Swing for UI rendering

## How to Run

### Option 1: Windows `.exe` (no Java required)

1. Download the latest release from the [Releases page](https://github.com/annasozonova/shroom-companion/releases)
2. Extract the ZIP archive
3. Run `shroom-companion.exe`

### Option 2: From source (Java 17+ and Maven required)

```bash
git clone https://github.com/your-username/shroom-companion.git
cd shroom-companion
mvn clean package
java -jar target/shroom-companion-1.0-SNAPSHOT-jar-with-dependencies.jar
```
## Project Structure

```plaintext
src/
├── main/
│   ├── java/                       
│   │   └── org/
│   │       └── example/
│   │           └── ClickCharacterApp.java       # Entry point and application logic
│   └── resources/                               # Sprite images and UI assets
│       ├── hanger.png
│       ├── mushroom_calm.png
│       ├── mushroom_left.png
│       ├── mushroom_right.png
│       ├── mushroom_without_tail.png
│       ├── mini_red.png
│       ├── mini_blue.png
│       └── ...                                  # Additional sprite thumbnails and frames
pom.xml                                          # Maven configuration file
README.md                                        # Project documentation
LICENSE                                          # License information
```
## Packaging

This application is distributed as a standalone `.exe` using [Launch4j](http://launch4j.sourceforge.net/), which wraps the executable around the compiled JAR file. Optionally, the executable can include a bundled Java Runtime Environment (JRE) to ensure consistent behavior across systems without requiring Java to be installed separately.



