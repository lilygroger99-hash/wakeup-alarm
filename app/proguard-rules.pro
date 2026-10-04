# R8 / ProGuard rules for WakeUp.
# Room, DataStore, Compose and Navigation ship their own consumer rules, so nothing special is needed.
# Manifest components (activities, services, receivers) are kept automatically by AAPT2.

# Keep readable stack traces in Play Console crash reports (upload the mapping file with the release).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
