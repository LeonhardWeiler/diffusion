{
  description = "Dev shell for building Diffusion on NixOS";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
  };

  outputs =
    { self, nixpkgs }:
    let
      system = "x86_64-linux";
      overlays = [
        # The android build tools list 32 bit libs for autoPatchelf. All of them
        # come from the binary cache, except ncurses5, which is an override and
        # would have to be built locally, which needs 32 bit kernel support.
        # Nothing shipped in build tools 36/37 is 32 bit, so plain ncurses does.
        (_final: prev: prev.lib.optionalAttrs prev.stdenv.hostPlatform.is32bit {
          ncurses5 = prev.ncurses;
        })
      ];
      pkgs = import nixpkgs {
        inherit system overlays;
        config.allowUnfree = true;
        config.android_sdk.accept_license = true;
      };

      # Match compileSdk in app/build.gradle.kts. There is no NDK here and no
      # cmake: everything this app is made of is jvm code.
      android = pkgs.androidenv.composeAndroidPackages {
        platformVersions = [ "37" ];
        buildToolsVersions = [
          "36.0.0"
          "37.0.0"
        ];
        includeEmulator = false;
        includeSources = false;
        includeSystemImages = false;
        includeNDK = false;
      };

      jdk = pkgs.jdk21;
    in
    {
      devShells.${system}.default = pkgs.mkShell {
        packages = [
          android.androidsdk
          jdk
          pkgs.gradle_9
          pkgs.git
          pkgs.just
        ];

        JAVA_HOME = "${jdk}/lib/openjdk";

        shellHook = ''
          build_tools_version="37.0.0"
          sdk_root="${android.androidsdk}/libexec/android-sdk"

          export ANDROID_SDK_ROOT="$sdk_root"
          export ANDROID_HOME="$sdk_root"

          export PATH="$ANDROID_SDK_ROOT/platform-tools:$ANDROID_SDK_ROOT/build-tools/$build_tools_version:$PATH"
          export GRADLE_OPTS="-Dorg.gradle.project.android.aapt2FromMavenOverride=$ANDROID_SDK_ROOT/build-tools/$build_tools_version/aapt2"

          echo "ANDROID_SDK_ROOT=$ANDROID_SDK_ROOT"
        '';
      };
    };
}
