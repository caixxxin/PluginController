export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export ANDROID_HOME=/root/Android/Sdk

rm -f sign.apk

./gradlew assembleRelease -PIS_SO_BUILD=false

/root/Android/Sdk/build-tools/34.0.0/apksigner sign --ks caixxxin.keystore --ks-pass pass:"12345678" --in app/build/outputs/apk/release/app-release-unsigned.apk --out sign.apk

rm -f sign.apk.idsig