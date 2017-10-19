#! /bin/bash

# Copyright (c) 2017 Intel Corporation
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

cd $HOME
echo "------------------------------------------------------------------------------"
echo " Switched to User home dir: $HOME"
echo "------------------------------------------------------------------------------"

# #############################################################
# FPC & Environment Dependencies settings
# #######################################
FPC_DIR=fpc
FPC_GIT="https://github.com/sprintlabs/fpc.git"
FPC_BRANCH=dev-stable
MVN_GET_SETTING="https://raw.githubusercontent.com/opendaylight/odlparent/master/settings.xml"
ZMQ_DOWNLOAD="https://github.com/zeromq/libzmq/releases/download/v4.2.2/zeromq-4.2.2.tar.gz"
ZMQ_PKG=zeromq-4.2.2.tar.gz
ZMQ_DIR=zeromq-4.2.2

echo -e "\n*************************************************"
echo "Verify FPC & Environment Dependencies settings..."
echo "*************************************************"
echo -e "FPC_DIR:\t\t" $FPC_DIR
echo -e "FPC_GIT:\t\t" $FPC_GIT
echo -e "FPC_BRANCH:\t\t" $FPC_BRANCH
echo -e "MVN_GET_SETTING:\t" $MVN_GET_SETTING
echo -e "ZMQ_DOWNLOAD:\t\t" $ZMQ_DOWNLOAD
echo -e "ZMQ_DIR:\t\t" $ZMQ_DIR

read -p "OK FPC environment settings? y/n: " Answer
	if [[ "${Answer}" = "n" || "${Answer}" = "N" ]]
	then
		set -x
		echo -e "Please correct FPC environment settings!!!"
		exit 1
		set +x
	fi

#
# Sets QUIT variable so script will finish.
#
quit()
{
	QUIT=$1
}

# Shortcut for quit.
q()
{
	quit
}

setup_http_proxy()
{
	while true; do
		echo
		read -p "Enter Proxy : " proxy
		export http_proxy=$proxy
		export https_proxy=$proxy
		echo "Acquire::http::proxy \"$http_proxy\";" | sudo tee -a /etc/apt/apt.conf > /dev/null
		echo "Acquire::https::proxy \"$http_proxy\";" | sudo tee -a /etc/apt/apt.conf > /dev/null

		wget -T 20 -t 3 --spider http://www.google.com
		if [ "$?" != 0 ]; then
		  echo -e "No Internet connection. Proxy incorrect? Try again"
		  echo -e "eg: http://<proxy>:<port>"
		  exit 1
		fi
	return
	done
}

step_1()
{
        TITLE="FPC Environment setup."
        CONFIG_NUM=1
        TEXT[1]="Check OS and network connection"
        FUNC[1]="setup_env"
}
setup_env()
{
	# a. Check for OS dependencies
	source /etc/os-release
	if [[ $VERSION_ID != "16.04" ]] ; then
		echo "WARNING: It is recommended to use Ubuntu 16.04..Your version is "$VERSION_ID
		echo "The libboost 1.58 dependency is not met by official Ubuntu PPA. Either attempt"
		echo "to find/compile boost 1.58 or upgrade your distribution by performing 'sudo do-release-upgrade'"
	else
		echo "Ubuntu 16.04 OS requirement met..."
	fi
	echo
	echo "Checking network connectivity..."
	# b. Check for internet connections
	wget -T 20 -t 3 --spider http://www.google.com
	if [ "$?" != 0 ]; then
		while true; do
			read -p "No Internet connection. Are you behind a proxy (y/n)? " yn
			case $yn in
				[Yy]* ) $SETUP_PROXY ; return;;
				[Nn]* ) echo "Please check your internet connection..." ; exit;;
				* ) "Please answer yes or no.";;
			esac
		done
	fi
}

step_2()
{
	TITLE="Download and Install"
	CONFIG_NUM=1
	TEXT[1]="Agree to download"
	FUNC[1]="get_agreement_download"
	TEXT[2]="Download packages"
	FUNC[2]="install_libs"
	TEXT[3]="Download & install Java JDK for ODL"
	FUNC[3]="install_jdk"
	TEXT[4]="Download & install maven POM for ODL"
	FUNC[4]="install_maven"
	TEXT[5]="Download & install zeromq"
	FUNC[5]="install_zeromq"
}
get_agreement_download()
{
	echo
	echo "List of packages needed for FPC build and installation:"
	echo "-------------------------------------------------------"
	echo "1.  Java JDK for ODL"
	echo "2.  maven"
	echo "3.  zeromq"
	echo "4.  python zeromq"
	echo "5.  build-essential"
	echo "6.  linux-headers-generic"
	echo "7.  git"
	echo "8.  unzip"
	echo "9.  libpcap-dev"
	echo "10. make"
	echo "11. and other library dependencies"
	while true; do
		read -p "We need download above mentioned package. Press (y/n) to continue? " yn
		case $yn in
			[Yy]* )
				touch .agree
				return;;
			[Nn]* ) exit;;
			* ) "Please answer yes or no.";;
		esac
	done
}

install_libs()
{
	echo "Install libs needed to build and run FPC..."
	file_name=".agree"
	if [ ! -e "$file_name" ]; then
		echo "Please choose option '2.Agree to download' first"
		return
	fi
	file_name=".download"
	if [ -e "$file_name" ]; then
		clear
		return
	fi
	sudo apt-get update
	sudo apt-get -y install curl build-essential linux-headers-$(uname -r) git unzip libpcap0.8-dev gcc libjson0-dev\
		make libc6 libc6-dev g++-multilib libzmq3-dev libcurl4-openssl-dev
	apt-get -y install python-pip
	pip install pyzmq
	pip install --upgrade pip
	touch .download
}

install_jdk()
{
	echo "Install Java JDK"
	file_name=".agree"
	if [ ! -e "$file_name" ]; then
		echo "Please choose option '2.Agree to download' first"
		return
	fi
	apt-get install openjdk-8-jdk
	if [ $? -ne 0 ] ; then
		echo "Failed to install Java jdk."
	fi
}

install_maven()
{
	echo "Install maven"
	file_name=".agree"
	if [ ! -e "$file_name" ]; then
		echo "Please choose option '2.Agree to download' first"
		return
	fi
	apt-get install maven
	mkdir -p ~/.m2
	cp /etc/maven/settings.xml ~/.m2/
	cp -n ~/.m2/settings.xml{,.orig}
	ls -ahl ~/.m2
	read -p "OK ~/.m2/settings.xml? y/n: " Answer
	if [[ "${Answer}" = "y" || "${Answer}" = "Y" ]]
	then
		echo "Update maven settings.xml..."
#		wget -O - https://raw.githubusercontent.com/opendaylight/odlparent/master/settings.xml > ~/.m2/settings.xml
		wget -O - $MVN_GET_SETTING > ~/.m2/settings.xml
	else
		echo "Aborting update maven settings.xml!!!"
		exit 1
	fi
}

install_zeromq()
{
	echo "Download zermoq zip"
	file_name=".agree"
	if [ ! -e "$file_name" ]; then
		echo "Please choose option '2.Agree to download' first"
		return
	fi
	wget ${ZMQ_DOWNLOAD}
	tar -xvzf $ZMQ_PKG
	pushd $ZMQ_DIR
	./configure
	make
	make install
	popd
}

step_3()
{
        TITLE="Build FPC"
        CONFIG_NUM=1
        TEXT[1]="Build FPC"
        FUNC[1]="build_fpc"
}
build_fpc()
{
#	git clone https://github.com/sprintlabs/fpc.git
	git clone $FPC_GIT
	pushd $FPC_DIR
	git checkout $FPC_BRANCH
	./build.sh
	popd
}

SETUP_PROXY="setup_http_proxy"
STEPS[1]="step_1"
STEPS[2]="step_2"
STEPS[3]="step_3"

QUIT=0

clear

echo -n "Checking for user permission.. "
sudo -n true
if [ $? -ne 0 ]; then
   echo "Password-less sudo user must run this script" 1>&2
   exit 1
fi
echo "Done"
clear

while [ "$QUIT" == "0" ]; do
        OPTION_NUM=1
        for s in $(seq ${#STEPS[@]}) ; do
                ${STEPS[s]}

                echo "----------------------------------------------------------"
                echo " Step $s: ${TITLE}"
                echo "----------------------------------------------------------"

                for i in $(seq ${#TEXT[@]}) ; do
                        echo "[$OPTION_NUM] ${TEXT[i]}"
                        OPTIONS[$OPTION_NUM]=${FUNC[i]}
                        let "OPTION_NUM+=1"
                done

                # Clear TEXT and FUNC arrays before next step
                unset TEXT
                unset FUNC

                echo ""
        done

        echo "[$OPTION_NUM] Exit Script"
        OPTIONS[$OPTION_NUM]="quit"
        echo ""
        echo -n "Option: "
        read our_entry
        echo ""
        ${OPTIONS[our_entry]} ${our_entry}

        if [ "$QUIT" == "0" ] ; then
                echo
                echo -n "Press enter to continue ..."; read
                clear
                continue
                exit
        fi
        echo "Installation complete. Please refer to README.MD for more information"
done

