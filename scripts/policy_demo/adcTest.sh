cd ..;
./bindclient.sh;
sleep 1;
cd policy_demo;
./create_port_$1.sh;
sleep 1;
cd ..;
./context_create.sh;
