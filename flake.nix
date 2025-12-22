{
  description = "InvOverstack - Minecraft 1.21.x Fabric Mod";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = nixpkgs.legacyPackages.${system};
      in
      {
        devShells.default = pkgs.mkShell {
          buildInputs = with pkgs; [
            # Java 21 for Minecraft 1.21.10
            jdk21

            # Gradle wrapper will be used, but having gradle available is useful
            gradle

            # Development tools
            git
          ];

          shellHook = ''
            echo "InvOverstack Development Environment"
            echo "Java version: $(java -version 2>&1 | head -n 1)"
            echo "Gradle version: $(gradle --version | grep Gradle | head -n 1)"
            echo ""
            echo "To build the mod: ./gradlew build"
            echo "To run the client: ./gradlew runClient"
            echo "To run the server: ./gradlew runServer"
            echo ""

            # Set JAVA_HOME for Gradle
            export JAVA_HOME="${pkgs.jdk21}"

            # Ensure gradlew is executable
            if [ -f ./gradlew ]; then
              chmod +x ./gradlew
            fi
          '';

          # Set environment variables for the shell
          JAVA_HOME = "${pkgs.jdk21}";

          # Optional: Set gradle properties for better performance on Linux
          GRADLE_OPTS = "-Dorg.gradle.daemon=true -Dorg.gradle.parallel=true";
        };
      }
    );
}

