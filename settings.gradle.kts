plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
rootProject.name = "OmniSentry"
include("os-main-backend")
include("os-shared-core")
include("os-gateway")
include("os-authenticator")