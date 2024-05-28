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