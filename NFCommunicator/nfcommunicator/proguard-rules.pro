# Keep constructors of Device classes
-keepclassmembers class * extends com.neofect.communicator.Device {
    public <init>(com.neofect.communicator.Connection);
}
