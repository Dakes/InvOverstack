# InvOverstack

A Minecraft Fabric mod that increases stack sizes ONLY in player inventory with automatic split on container transfer.

## Features

- Increase item stack sizes in player inventory (configurable, up to 4096+)
- Automatic stack splitting when transferring to containers
- Server-side authoritative with optional client enhancements
- Works server-side only with degraded UX (wrong count display >99)
- With client mod installed: supports proper display of large stack counts

## Development

### Prerequisites

- Java 21
- Gradle (wrapper included)
- NixOS users: use the provided `flake.nix`

### Building

On NixOS:
```bash
nix develop
./gradlew build
```

On other systems:
```bash
./gradlew build
```

### Running

Client:
```bash
./gradlew runClient
```

Server:
```bash
./gradlew runServer
```

## Project Status

Currently in Phase 1: Project Setup (Complete)

See [TODO.md](TODO.md) for full implementation roadmap.

## Technical Details

- **Minecraft Version:** 1.21.11 (targeting 1.21.x compatibility)
- **Fabric Loader:** 0.18.2+
- **Fabric API:** 0.139.4+1.21.11
- **Java:** 21

## Author

Dakes - [GitHub](https://github.com/dakes)
