-keepattributes Signature

# Keep constructors of Device subclasses
-keepclassmembers class * extends com.neofect.communicator.Device {
    public <init>(com.neofect.communicator.Connection);
}
