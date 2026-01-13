###########################
# 安装基础环境  基于ubuntu22.04 LTS (可以替换国内apt源，未自动替换)
###########################
# sed -i 's@//.*archive.ubuntu.com@//mirrors.ustc.edu.cn@g' /etc/apt/sources.list
# sed -i 's/security.ubuntu.com/mirrors.ustc.edu.cn/g' /etc/apt/sources.list
cd /root
apt update
apt install -y git yasm curl wget gcc make cmake unzip build-essential
add-apt-repository -y ppa:openjdk-r/ppa
apt install -y openjdk-8-jdk openjdk-17-jdk

###########################
# 下载Android-Studio 并安装sdk25和build tool 25.0.3 (可以单独安装commandlinetools)
###########################
#cd /root
#wget https://redirector.gvt1.com/edgedl/android/studio/ide-zips/2021.2.1.3/android-studio-2021.2.1.3-linux.tar.gz
#tar zxvf android-studio-2021.2.1.3-linux.tar.gz
#cd /root/android-studio

###########################
# 下载安装sdk25和build tool 25.0.3
###########################
cd /root
wget https://dl.google.com/android/repository/commandlinetools-linux-9123335_latest.zip
unzip commandlinetools-linux-9123335_latest.zip
mkdir -p /root/Android/Sdk/cmdline-tools
mv /root/cmdline-tools /root/Android/Sdk/cmdline-tools/latest
cd /root/Android/Sdk/cmdline-tools/latest/bin
yes | ./sdkmanager --licenses

###########################
# 下载安装sdk34和build tool 34.0.0
###########################
cd /root/Android/Sdk/cmdline-tools/latest/bin
./sdkmanager --install platforms\;android-34
./sdkmanager --install build-tools\;34.0.0
./sdkmanager --install sources\;android-34
./sdkmanager --install cmake\;3.22.1


###########################
# 下载并安装ndk
###########################
cd /root
wget https://dl.google.com/android/repository/android-ndk-r10e-linux-x86_64.zip
unzip android-ndk-r10e-linux-x86_64.zip
mkdir -p /root/Android/Sdk/ndk
mv android-ndk-r10e /root/Android/Sdk/ndk/


##################
# 下载代码
##################
#git config --global http.proxy http://192.168.2.46:7897
#git config --global https.proxy http://192.168.2.46:7897

cd /root
git clone https://github.com/caixxxin/PluginController.git



#### 
# 测试工具 不参与编译
####

apt install python3-pip
pip3 config set global.index-url https://mirrors.aliyun.com/pypi/simple
pip3 config set install.trusted-host mirrors.aliyun.com
pip3 install python-miio


export HTTP_PROXY="http://192.168.3.40:7897"
export HTTPS_PROXY="http://192.168.3.40:7897"

bash <(curl -L https://github.com/PiotrMachowski/Xiaomi-cloud-tokens-extractor/raw/master/run.sh)


unset HTTP_PROXY
unset HTTPS_PROXY

miiocli chuangmiplug --ip 192.168.3.49 --token xxx --model chuangmi.plug.m1 on

miiocli chuangmiplug --ip 192.168.3.49 --token xxx --model chuangmi.plug.m1 off

miiocli chuangmiplug --ip 192.168.3.47 --token xxx --model chuangmi.plug.v1 usb_on

miiocli chuangmiplug --ip 192.168.3.49 --token xxx --model chuangmi.plug.m1 status

miiocli chuangmiplug --ip 192.168.3.49 --token xxx --model chuangmi.plug.m1 info